package com.hatch.marketplace.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.hatch.marketplace.data.model.ChatMessage
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.OfferStatus
import com.hatch.marketplace.data.model.Product
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
    fun observeConversations(userId: String?, isVerifiedAdminEmail: Boolean): Flow<List<Conversation>> = callbackFlow {
        val db = clients.firestore
        if (db == null || userId.isNullOrBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = if (isVerifiedAdminEmail) {
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
        if (db == null || conversationId.isNullOrBlank()) {
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
                "productImage" to product.images.firstOrNull().orEmpty(),
                "buyerId" to buyer.uid,
                "sellerId" to product.sellerId,
                "participantIds" to listOf(buyer.uid, product.sellerId),
                "buyerName" to buyer.name.take(80),
                "buyerAvatar" to buyer.avatar,
                "sellerName" to product.sellerName.take(80),
                "sellerAvatar" to product.sellerAvatar,
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
        require(amount > 0.0) { "Offer amount must be positive." }

        val msgText = buildString {
            append("عرض شراء: ${amount.toLong()} ج.س")
            if (note.isNotBlank()) append(" — $note")
        }.take(2000)

        val msgId = HatchFormatters.sanitizeFirestoreId("msg_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(msgId)

        msgRef.set(
            hashMapOf<String, Any>(
                "id" to msgId,
                "senderId" to senderId,
                "text" to msgText,
                "status" to "sent",
                "isOfferUpdate" to true,
                "offerAmount" to amount,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val senderIsBuyer = senderId == conversation.buyerId
        convRef.update(
            hashMapOf<String, Any>(
                "lastMessage" to msgText.take(120),
                "offerAmount" to amount,
                "offerStatus" to OfferStatus.PENDING.wireValue,
                "unreadByBuyer" to if (senderIsBuyer) 0 else conversation.unreadByBuyer + 1,
                "unreadBySeller" to if (senderIsBuyer) conversation.unreadBySeller + 1 else 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun respondToOffer(
        conversation: Conversation,
        responderId: String,
        newStatus: OfferStatus,
        counterAmount: Double? = null
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val msgId = HatchFormatters.sanitizeFirestoreId("msg_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document(msgId)

        val summary = when (newStatus) {
            OfferStatus.ACCEPTED -> "تم قبول العرض (${conversation.offerAmount?.toLong() ?: 0} ج.س)"
            OfferStatus.DECLINED -> "تم رفض العرض"
            OfferStatus.COUNTERED -> "عرض مضاد: ${counterAmount?.toLong() ?: 0} ج.س"
            else -> "تحديث العرض"
        }

        val msgPayload = hashMapOf<String, Any>(
            "id" to msgId,
            "senderId" to responderId,
            "text" to summary,
            "status" to "sent",
            "isOfferUpdate" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (counterAmount != null && counterAmount > 0.0) {
            msgPayload["offerAmount"] = counterAmount
        } else if (conversation.offerAmount != null && conversation.offerAmount > 0.0) {
            msgPayload["offerAmount"] = conversation.offerAmount
        }
        msgRef.set(msgPayload).await()

        val responderIsBuyer = responderId == conversation.buyerId
        val convUpdates = hashMapOf<String, Any>(
            "lastMessage" to summary.take(120),
            "offerStatus" to newStatus.wireValue,
            "unreadByBuyer" to if (responderIsBuyer) 0 else conversation.unreadByBuyer + 1,
            "unreadBySeller" to if (responderIsBuyer) conversation.unreadBySeller + 1 else 0,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (counterAmount != null && counterAmount > 0.0) {
            convUpdates["counterAmount"] = counterAmount
        }
        convRef.update(convUpdates).await()
    }

    suspend fun markConversationRead(conversation: Conversation, readerUid: String, messages: List<ChatMessage>) {
        val db = clients.firestore ?: return
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

        msgRef.set(payload).await()

        val senderIsBuyer = senderId == conversation.buyerId
        convRef.update(
            mapOf(
                "lastMessage" to lastMessageSummary.take(500),
                "unreadByBuyer" to if (senderIsBuyer) 0 else conversation.unreadByBuyer + 1,
                "unreadBySeller" to if (senderIsBuyer) conversation.unreadBySeller + 1 else 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
