package com.hatch.marketplace.data.repository

import com.google.firebase.firestore.FieldValue
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.NotificationItem
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.ReportTicket
import com.hatch.marketplace.data.model.UserReview
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InteractionRepository @Inject constructor(
    private val clients: FirebaseClientsHolder
) {
    fun observeFavoriteIds(userId: String?): Flow<Set<String>> = callbackFlow {
        val db = clients.firestore
        if (db == null || userId.isNullOrBlank()) {
            trySend(emptySet())
            awaitClose {}
            return@callbackFlow
        }
        val reg = db.collection("users_private").document(userId)
            .collection("favorites")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.map { it.id }?.toSet().orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun toggleFavorite(userId: String, productId: String, currentlyFavorite: Boolean): Result<Boolean> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val favRef = db.collection("users_private").document(userId)
            .collection("favorites").document(productId)
        val productRef = db.collection("products").document(productId)

        if (currentlyFavorite) {
            favRef.delete().await()
        } else {
            favRef.set(
                mapOf(
                    "productId" to productId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        runCatching {
            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (snap.exists() && snap.getString("status") == ProductStatus.ACTIVE.wireValue) {
                    val likes = (snap.getLong("likesCount") ?: 0L).toInt()
                    val nextLikes = if (currentlyFavorite) maxOf(0, likes - 1) else likes + 1
                    tx.update(
                        productRef,
                        mapOf(
                            "likesCount" to nextLikes,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
        }

        !currentlyFavorite
    }

    suspend fun recordSwipe(userId: String, productId: String, action: String) {
        val db = clients.firestore ?: return
        if (action !in listOf("left", "right", "superlike")) return
        runCatching {
            db.collection("users_private").document(userId)
                .collection("swipes").document(productId)
                .set(
                    mapOf(
                        "productId" to productId,
                        "action" to action,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }
    }

    fun observeNotifications(userId: String?): Flow<List<NotificationItem>> = callbackFlow {
        val db = clients.firestore
        if (db == null || userId.isNullOrBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val personalMap = linkedMapOf<String, NotificationItem>()
        val broadcastMap = linkedMapOf<String, NotificationItem>()

        fun emitAll() {
            val combined = (personalMap.values + broadcastMap.values)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            trySend(combined)
        }

        val personalReg = db.collection("users_private").document(userId)
            .collection("notifications")
            .addSnapshotListener { snap, _ ->
                personalMap.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                        ?.let { personalMap[it.id] = it }
                }
                emitAll()
            }

        val broadcastReg = db.collection("broadcast_notifications")
            .whereIn("type", listOf("offer", "message", "price_drop", "verification", "system", "alert", "update"))
            .addSnapshotListener { snap, _ ->
                broadcastMap.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                        ?.let { broadcastMap[it.id] = it }
                }
                emitAll()
            }

        awaitClose {
            personalReg.remove()
            broadcastReg.remove()
        }
    }

    suspend fun markPersonalNotificationRead(userId: String, notificationId: String) {
        val db = clients.firestore ?: return
        runCatching {
            db.collection("users_private").document(userId)
                .collection("notifications").document(notificationId)
                .update("read", true)
                .await()
        }
    }

    fun observeReviews(): Flow<List<UserReview>> = callbackFlow {
        val db = clients.firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("reviews")
            .whereGreaterThanOrEqualTo("rating", 1)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(UserReview::class.java)?.copy(id = doc.id)
                }.orEmpty().sortedByDescending { it.createdAt?.seconds ?: 0L }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun submitReview(
        sellerId: String,
        reviewerId: String,
        reviewerName: String,
        reviewerAvatar: String,
        rating: Int,
        comment: String,
        tags: List<String>,
        productId: String?,
        productTitle: String?
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        require(sellerId != reviewerId) { "You cannot review your own account." }
        require(rating in 1..5) { "Rating must be between 1 and 5." }
        val cleanComment = comment.trim().take(600)
        require(cleanComment.length >= 2) { "Review comment must be at least 2 characters." }

        val reviewId = HatchFormatters.sanitizeFirestoreId("rev_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val payload = hashMapOf<String, Any>(
            "id" to reviewId,
            "sellerId" to sellerId,
            "reviewerId" to reviewerId,
            "reviewerName" to reviewerName.take(80),
            "reviewerAvatar" to reviewerAvatar,
            "rating" to rating,
            "comment" to cleanComment,
            "tags" to tags.take(6),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!productId.isNullOrBlank()) payload["productId"] = productId
        if (!productTitle.isNullOrBlank()) payload["productTitle"] = productTitle.take(140)

        db.collection("reviews").document(reviewId).set(payload).await()
    }

    fun observeReports(userId: String?, isVerifiedAdminEmail: Boolean): Flow<List<ReportTicket>> = callbackFlow {
        val db = clients.firestore
        if (db == null || userId.isNullOrBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val query = if (isVerifiedAdminEmail) {
            db.collection("reports")
        } else {
            db.collection("reports").whereEqualTo("reporterId", userId)
        }
        val reg = query.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(ReportTicket::class.java)?.copy(id = doc.id)
            }.orEmpty().sortedByDescending { it.createdAt?.seconds ?: 0L }
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun submitReport(
        reporterId: String,
        reporterName: String,
        reportedUserId: String?,
        reportedUserName: String?,
        reportedProductId: String?,
        reportedProductTitle: String?,
        reason: String,
        message: String,
        flagListing: Boolean
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val cleanMessage = message.trim().take(1500)
        require(cleanMessage.length >= 5) { "Report details must be at least 5 characters." }

        val reportId = HatchFormatters.sanitizeFirestoreId("rep_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val payload = hashMapOf<String, Any>(
            "id" to reportId,
            "reporterId" to reporterId,
            "reporterName" to reporterName.take(80),
            "reason" to reason.take(120),
            "message" to cleanMessage,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!reportedUserId.isNullOrBlank()) payload["reportedUserId"] = reportedUserId
        if (!reportedUserName.isNullOrBlank()) payload["reportedUserName"] = reportedUserName.take(80)
        if (!reportedProductId.isNullOrBlank()) payload["reportedProductId"] = reportedProductId
        if (!reportedProductTitle.isNullOrBlank()) payload["reportedProductTitle"] = reportedProductTitle.take(140)

        db.collection("reports").document(reportId).set(payload).await()

        if (flagListing && !reportedProductId.isNullOrBlank()) {
            runCatching {
                db.collection("products").document(reportedProductId)
                    .update(
                        mapOf(
                            "status" to ProductStatus.FLAGGED.wireValue,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
        }
    }
}
