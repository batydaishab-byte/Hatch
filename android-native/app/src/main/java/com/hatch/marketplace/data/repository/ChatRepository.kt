package com.hatch.marketplace.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.hatch.marketplace.data.model.ChatMessage
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.OfferStatus
import com.hatch.marketplace.data.model.OfferTransitionAction
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val clients: FirebaseClientsHolder
) {
    fun observeConversations(userId: String?, hasAdminClaim: Boolean): Flow<List<Conversation>> = callbackFlow {
        val db = clients.firestore
        if (db == null || userId.isNullOrBlank() || clients.auth?.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = if (hasAdminClaim) {
            db.collection("conversations")
        } else {
            db.collection("conversations").whereArrayContains("participantIds", userId)
        }

        val reg = query.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(Conversation::class.java)?.copy(id = doc.id)
            }.orEmpty().sortedByDescending { it.updatedAt?.seconds ?: 0L }
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeMessages(conversationId: String?): Flow<List<ChatMessage>> = callbackFlow {
        val db = clients.firestore
        if (db == null || conversationId.isNullOrBlank() || clients.auth?.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val reg = db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun startOrGetConversation(
        product: Product,
        buyer: PublicUser
    ): Result<String> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val authUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(authUid == buyer.uid) { "Authenticated user mismatch." }
        require(buyer.uid != product.sellerId) { "You cannot chat on your own listing." }

        val convId = HatchFormatters.sanitizeFirestoreId("conv_${product.id}_${buyer.uid}_${product.sellerId}")
        val docRef = db.collection("conversations").document(convId)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val payload = hashMapOf<String, Any>(
                "id" to convId,
                "productId" to product.id,
                "productTitle" to product.titleAr.ifBlank { product.titleEn }.take(140),
                "productPrice" to product.price,
                "productImage" to product.images.firstOrNull().orEmpty().take(1000),
                "buyerId" to buyer.uid,
                "sellerId" to product.sellerId,
                "participantIds" to listOf(buyer.uid, product.sellerId),
                "buyerName" to buyer.name.take(80),
                "buyerAvatar" to buyer.avatar.take(1000),
                "sellerName" to product.sellerName.take(80),
                "sellerAvatar" to product.sellerAvatar.take(1000),
                "lastMessage" to "",
                "unreadByBuyer" to 0,
                "unreadBySeller" to 0,
                "offerStatus" to OfferStatus.NONE.wireValue,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
        convId
    }

    suspend fun sendTextMessage(
        conversation: Conversation,
        senderId: String,
        text: String
    ): Result<Unit> = runCatching {
        val cleanText = text.trim().take(2000)
        require(cleanText.isNotEmpty()) { "Message cannot be empty." }
        writeMessageAndTouchConversation(
            conversation = conversation,
            senderId = senderId,
            text = cleanText,
            lastMessageSummary = cleanText.take(120)
        )
    }

    suspend fun sendInspectionImageMessage(
        conversation: Conversation,
        senderId: String,
        imageUrl: String,
        tag: String,
        caption: String
    ): Result<Unit> = runCatching {
        writeMessageAndTouchConversation(
            conversation = conversation,
            senderId = senderId,
            text = caption.ifBlank { tag },
            lastMessageSummary = "[صورة فحص: $tag]",
            imageUrl = imageUrl,
            imageTag = tag.take(80)
        )
    }

    suspend fun sendVoiceMessage(
        conversation: Conversation,
        senderId: String,
        audioUrl: String,
        durationSeconds: Int
    ): Result<Unit> = runCatching {
        val safeDuration = durationSeconds.coerceIn(1, 600)
        writeMessageAndTouchConversation(
            conversation = conversation,
            senderId = senderId,
            text = "رسالة صوتية (${safeDuration}ث)",
            lastMessageSummary = "🎤 رسالة صوتية (${safeDuration}ث)",
            audioUrl = audioUrl,
            audioDuration = safeDuration
        )
    }

    suspend fun sendOffer(
        conversation: Conversation,
        senderId: String,
        amount: Double,
        note: String = ""
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val authUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(authUid == senderId) { "Authenticated user mismatch." }
        require(amount > 0.0) { "Offer amount must be positive." }

        val msgText = buildString {
            append("عرض شراء: ${amount.toLong()} ج.س")
            if (note.isNotBlank()) append(" — $note")
        }.take(2000)

        val msgId = HatchFormatters.sanitizeFirestoreId("msg_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(msgId)
        val productRef = db.collection("products").document(conversation.productId)

        db.runTransaction { tx ->
            val convSnap = tx.get(convRef)
            val prodSnap = tx.get(productRef)
            require(convSnap.exists()) { "Conversation not found." }
            val prodStatus = prodSnap.getString("status") ?: ProductStatus.SOLD.wireValue
            val currentOfferStatus = convSnap.getString("offerStatus") ?: OfferStatus.NONE.wireValue
            val buyerId = convSnap.getString("buyerId").orEmpty()
            val sellerId = convSnap.getString("sellerId").orEmpty()
            val nextStatus = MarketplaceLogic.validateOfferTransition(
                currentStatus = currentOfferStatus,
                actorUid = senderId,
                buyerId = buyerId,
                sellerId = sellerId,
                action = OfferTransitionAction.SUBMIT_OFFER,
                amount = amount,
                productStatus = prodStatus
            ).getOrThrow()

            val unreadBuyer = (convSnap.getLong("unreadByBuyer") ?: 0L).toInt()
            val unreadSeller = (convSnap.getLong("unreadBySeller") ?: 0L).toInt()
            @Suppress("UNCHECKED_CAST")
            val existingHistory = (convSnap.get("offerHistory") as? List<Map<String, Any>>).orEmpty()
            require(existingHistory.size < 30) { "Maximum offer negotiation history reached for this conversation." }
            val newHistoryEntry = mapOf<String, Any>(
                "actorId" to senderId,
                "status" to nextStatus.wireValue,
                "amount" to amount,
                "atMillis" to System.currentTimeMillis()
            )
            val nextHistory = existingHistory + newHistoryEntry

            tx.update(
                convRef,
                hashMapOf<String, Any>(
                    "lastMessage" to msgText.take(120),
                    "offerAmount" to amount,
                    "offerStatus" to nextStatus.wireValue,
                    "offerUpdatedBy" to senderId,
                    "offerHistory" to nextHistory,
                    "unreadByBuyer" to 0,
                    "unreadBySeller" to (unreadSeller + 1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                msgRef,
                hashMapOf<String, Any>(
                    "id" to msgId,
                    "senderId" to senderId,
                    "text" to msgText,
                    "status" to "sent",
                    "isOfferUpdate" to true,
                    "offerAmount" to amount,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun respondToOffer(
        conversation: Conversation,
        responderId: String,
        newStatus: OfferStatus,
        counterAmount: Double? = null
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val authUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(authUid == responderId) { "Authenticated user mismatch." }

        val action = when (newStatus) {
            OfferStatus.ACCEPTED -> OfferTransitionAction.ACCEPT_OFFER
            OfferStatus.DECLINED -> OfferTransitionAction.DECLINE_OFFER
            OfferStatus.COUNTERED -> OfferTransitionAction.COUNTER_OFFER
            else -> error("Invalid offer response action.")
        }

        val msgId = HatchFormatters.sanitizeFirestoreId("msg_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(msgId)
        val productRef = db.collection("products").document(conversation.productId)

        db.runTransaction { tx ->
            val convSnap = tx.get(convRef)
            val prodSnap = tx.get(productRef)
            require(convSnap.exists()) { "Conversation not found." }
            val prodStatus = prodSnap.getString("status") ?: ProductStatus.SOLD.wireValue
            val currentOfferStatus = convSnap.getString("offerStatus") ?: OfferStatus.NONE.wireValue
            val buyerId = convSnap.getString("buyerId").orEmpty()
            val sellerId = convSnap.getString("sellerId").orEmpty()
            val currentOfferAmt = convSnap.getDouble("offerAmount")
            val currentCounterAmt = convSnap.getDouble("counterAmount")

            val validatedStatus = MarketplaceLogic.validateOfferTransition(
                currentStatus = currentOfferStatus,
                actorUid = responderId,
                buyerId = buyerId,
                sellerId = sellerId,
                action = action,
                amount = counterAmount,
                productStatus = prodStatus
            ).getOrThrow()

            val effectiveAmount = when (validatedStatus) {
                OfferStatus.COUNTERED -> counterAmount
                OfferStatus.ACCEPTED -> if (currentOfferStatus == OfferStatus.COUNTERED.wireValue) currentCounterAmt else currentOfferAmt
                else -> currentOfferAmt
            }
            val summary = when (validatedStatus) {
                OfferStatus.ACCEPTED -> "تم قبول العرض (${effectiveAmount?.toLong() ?: 0} ج.س)"
                OfferStatus.DECLINED -> "تم رفض العرض"
                OfferStatus.COUNTERED -> "عرض مضاد: ${counterAmount?.toLong() ?: 0} ج.س"
                else -> "تحديث العرض"
            }

            val unreadBuyer = (convSnap.getLong("unreadByBuyer") ?: 0L).toInt()
            val unreadSeller = (convSnap.getLong("unreadBySeller") ?: 0L).toInt()
            val responderIsBuyer = responderId == buyerId

            @Suppress("UNCHECKED_CAST")
            val existingHistory = (convSnap.get("offerHistory") as? List<Map<String, Any>>).orEmpty()
            require(existingHistory.size < 30) { "Maximum offer negotiation history reached for this conversation." }
            val entry = hashMapOf<String, Any>(
                "actorId" to responderId,
                "status" to validatedStatus.wireValue,
                "atMillis" to System.currentTimeMillis()
            )
            if (effectiveAmount != null && effectiveAmount > 0.0) {
                entry["amount"] = effectiveAmount
            }
            val nextHistory = existingHistory + entry

            val convUpdates = hashMapOf<String, Any>(
                "lastMessage" to summary.take(120),
                "offerStatus" to validatedStatus.wireValue,
                "offerUpdatedBy" to responderId,
                "offerHistory" to nextHistory,
                "unreadByBuyer" to if (responderIsBuyer) 0 else unreadBuyer + 1,
                "unreadBySeller" to if (responderIsBuyer) unreadSeller + 1 else 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (validatedStatus == OfferStatus.COUNTERED && counterAmount != null && counterAmount > 0.0) {
                convUpdates["counterAmount"] = counterAmount
            }
            if (validatedStatus == OfferStatus.ACCEPTED) {
                convUpdates["acceptedBy"] = responderId
                convUpdates["acceptedAt"] = FieldValue.serverTimestamp()
            }

            val msgPayload = hashMapOf<String, Any>(
                "id" to msgId,
                "senderId" to responderId,
                "text" to summary,
                "status" to "sent",
                "isOfferUpdate" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (effectiveAmount != null && effectiveAmount > 0.0) {
                msgPayload["offerAmount"] = effectiveAmount
            }

            tx.update(convRef, convUpdates)
            tx.set(msgRef, msgPayload)
        }.await()
    }

    suspend fun markConversationRead(conversation: Conversation, readerUid: String, messages: List<ChatMessage>) {
        val db = clients.firestore ?: return
        val authUid = clients.auth?.currentUser?.uid ?: return
        if (authUid != readerUid) return
        runCatching {
            val convRef = db.collection("conversations").document(conversation.id)
            val isBuyer = readerUid == conversation.buyerId
            val needsClear = (isBuyer && conversation.unreadByBuyer > 0) ||
                (!isBuyer && conversation.unreadBySeller > 0)
            if (needsClear) {
                convRef.update(
                    mapOf(
                        "unreadByBuyer" to if (isBuyer) 0 else conversation.unreadByBuyer,
                        "unreadBySeller" to if (!isBuyer) 0 else conversation.unreadBySeller,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            messages.filter { it.senderId != readerUid && it.status != "read" }.forEach { msg ->
                convRef.collection("messages").document(msg.id)
                    .update("status", "read")
            }
        }
    }

    private suspend fun writeMessageAndTouchConversation(
        conversation: Conversation,
        senderId: String,
        text: String,
        lastMessageSummary: String,
        imageUrl: String? = null,
        imageTag: String? = null,
        audioUrl: String? = null,
        audioDuration: Int? = null
    ) {
        val db = clients.firestore ?: error("Firestore unavailable")
        val authUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(authUid == senderId) { "Authenticated user mismatch." }
        val msgId = HatchFormatters.sanitizeFirestoreId("msg_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(msgId)

        val payload = hashMapOf<String, Any>(
            "id" to msgId,
            "senderId" to senderId,
            "text" to text.take(2000),
            "status" to "sent",
            "isOfferUpdate" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!imageUrl.isNullOrBlank()) payload["imageUrl"] = imageUrl.take(1000)
        if (!imageTag.isNullOrBlank()) payload["imageTag"] = imageTag.take(80)
        if (!audioUrl.isNullOrBlank()) payload["audioUrl"] = audioUrl.take(1000)
        if (audioDuration != null) payload["audioDuration"] = audioDuration.coerceIn(0, 600)

        db.runTransaction { tx ->
            val convSnap = tx.get(convRef)
            val buyerId = convSnap.getString("buyerId") ?: conversation.buyerId
            val senderIsBuyer = senderId == buyerId
            val unreadBuyer = (convSnap.getLong("unreadByBuyer") ?: 0L).toInt()
            val unreadSeller = (convSnap.getLong("unreadBySeller") ?: 0L).toInt()
            tx.set(msgRef, payload)
            tx.update(
                convRef,
                mapOf(
                    "lastMessage" to lastMessageSummary.take(500),
                    "unreadByBuyer" to if (senderIsBuyer) 0 else unreadBuyer + 1,
                    "unreadBySeller" to if (senderIsBuyer) unreadSeller + 1 else 0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }
}
