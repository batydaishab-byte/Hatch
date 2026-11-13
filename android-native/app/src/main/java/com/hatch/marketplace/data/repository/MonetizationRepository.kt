package com.hatch.marketplace.data.repository

import com.google.firebase.firestore.FieldValue
import com.hatch.marketplace.data.media.CloudinaryMediaService
import com.hatch.marketplace.data.media.CloudinaryQuotaStatus
import com.hatch.marketplace.data.model.AdMobConfig
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.SponsoredAdItem
import com.hatch.marketplace.data.model.UserStatus
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonetizationRepository @Inject constructor(
    private val clients: FirebaseClientsHolder,
    private val cloudinaryService: CloudinaryMediaService
) {
    fun observeSponsoredAds(isVerifiedAdminEmail: Boolean): Flow<List<SponsoredAdItem>> = callbackFlow {
        val db = clients.firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = if (isVerifiedAdminEmail) {
            db.collection("sponsored_ads")
        } else {
            db.collection("sponsored_ads").whereEqualTo("isActive", true)
        }
        val reg = query.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(SponsoredAdItem::class.java)?.copy(id = doc.id)
            }.orEmpty().sortedByDescending { it.createdAt?.seconds ?: 0L }
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAdConfig(isSignedIn: Boolean): Flow<AdMobConfig> = callbackFlow {
        val db = clients.firestore
        if (db == null || !isSignedIn) {
            trySend(AdMobConfig())
            awaitClose {}
            return@callbackFlow
        }
        val reg = db.collection("ad_config").document("default")
            .addSnapshotListener { snap, _ ->
                val cfg = snap?.toObject(AdMobConfig::class.java) ?: AdMobConfig()
                trySend(cfg)
            }
        awaitClose { reg.remove() }
    }

    suspend fun recordImpression(adId: String) {
        val db = clients.firestore ?: return
        clients.auth?.currentUser ?: return
        runCatching {
            val ref = db.collection("sponsored_ads").document(adId)
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                if (snap.exists() && snap.getBoolean("isActive") == true) {
                    val count = (snap.getLong("impressionsCount") ?: 0L).toInt()
                    tx.update(
                        ref,
                        mapOf(
                            "impressionsCount" to (count + 1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
        }
    }

    suspend fun recordClick(adId: String) {
        val db = clients.firestore ?: return
        clients.auth?.currentUser ?: return
        runCatching {
            val ref = db.collection("sponsored_ads").document(adId)
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                if (snap.exists() && snap.getBoolean("isActive") == true) {
                    val count = (snap.getLong("clicksCount") ?: 0L).toInt()
                    tx.update(
                        ref,
                        mapOf(
                            "clicksCount" to (count + 1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
        }
    }

    suspend fun createSponsoredAd(ad: SponsoredAdItem): Result<String> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val id = HatchFormatters.sanitizeFirestoreId("ad_${UUID.randomUUID().toString().replace("-", "").take(14)}")
        val payload = hashMapOf<String, Any>(
            "id" to id,
            "titleAr" to ad.titleAr.trim(),
            "titleEn" to ad.titleEn.trim().ifBlank { ad.titleAr.trim() },
            "advertiser" to ad.advertiser.trim(),
            "descriptionAr" to ad.descriptionAr.trim(),
            "descriptionEn" to ad.descriptionEn.trim(),
            "imageUrl" to ad.imageUrl.trim(),
            "logoUrl" to ad.logoUrl.orEmpty().trim(),
            "mediaType" to ad.mediaType,
            "ctaTextAr" to ad.ctaTextAr.trim(),
            "ctaTextEn" to ad.ctaTextEn.trim(),
            "targetUrl" to ad.targetUrl.trim(),
            "category" to ad.category.trim(),
            "badgeAr" to ad.badgeAr.trim(),
            "badgeEn" to ad.badgeEn.trim(),
            "format" to ad.format,
            "isActive" to ad.isActive,
            "durationSeconds" to ad.durationSeconds.coerceIn(1, 300),
            "clicksCount" to 0,
            "impressionsCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!ad.videoUrl.isNullOrBlank()) payload["videoUrl"] = ad.videoUrl.trim()
        db.collection("sponsored_ads").document(id).set(payload).await()
        id
    }

    suspend fun toggleAdActive(adId: String, active: Boolean): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("sponsored_ads").document(adId).update(
            mapOf(
                "isActive" to active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateAdDuration(adId: String, durationSeconds: Int): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("sponsored_ads").document(adId).update(
            mapOf(
                "durationSeconds" to durationSeconds.coerceIn(1, 300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteAd(adId: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("sponsored_ads").document(adId).delete().await()
    }

    suspend fun updateAdConfig(config: AdMobConfig): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("ad_config").document("default").set(
            hashMapOf<String, Any>(
                "isEnabled" to config.isEnabled,
                "isTestMode" to config.isTestMode,
                "admobAppId" to config.admobAppId.trim(),
                "bannerUnitId" to config.bannerUnitId.trim(),
                "interstitialUnitId" to config.interstitialUnitId.trim(),
                "rewardedUnitId" to config.rewardedUnitId.trim(),
                "nativeUnitId" to config.nativeUnitId.trim(),
                "ecpmRate" to config.ecpmRate.coerceAtLeast(0.0),
                "cpcRate" to config.cpcRate.coerceAtLeast(0.0),
                "sdgRate" to config.sdgRate.coerceAtLeast(1.0),
                "bannerRotationSeconds" to config.bannerRotationSeconds.coerceIn(3, 120),
                "interstitialCountdownSeconds" to config.interstitialCountdownSeconds.coerceIn(1, 60),
                "rewardedDurationSeconds" to config.rewardedDurationSeconds.coerceIn(5, 120),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun resetAllAdStats(ads: List<SponsoredAdItem>): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val batch = db.batch()
        ads.forEach { ad ->
            val ref = db.collection("sponsored_ads").document(ad.id)
            batch.update(
                ref,
                mapOf(
                    "clicksCount" to 0,
                    "impressionsCount" to 0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    suspend fun sendBroadcastNotification(
        type: String,
        titleAr: String,
        titleEn: String,
        bodyAr: String,
        bodyEn: String,
        relatedProductId: String?
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val id = HatchFormatters.sanitizeFirestoreId("bcast_${UUID.randomUUID().toString().replace("-", "").take(14)}")
        val payload = hashMapOf<String, Any>(
            "id" to id,
            "type" to type,
            "titleAr" to titleAr.trim().take(160),
            "titleEn" to titleEn.trim().ifBlank { titleAr.trim() }.take(160),
            "bodyAr" to bodyAr.trim().take(1000),
            "bodyEn" to bodyEn.trim().ifBlank { bodyAr.trim() }.take(1000),
            "read" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!relatedProductId.isNullOrBlank()) payload["relatedProductId"] = relatedProductId
        db.collection("broadcast_notifications").document(id).set(payload).await()
    }

    suspend fun suspendUser(uid: String, days: Int, reason: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val untilIso = MarketplaceLogic.buildSuspensionEndIso(days)
        db.collection("users_public").document(uid).update(
            mapOf(
                "status" to UserStatus.SUSPENDED.wireValue,
                "suspensionDays" to days.coerceIn(1, 3650),
                "suspendedUntil" to untilIso,
                "suspensionReason" to reason.trim().take(500),
                "verifiedIdentity" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val notifId = HatchFormatters.sanitizeFirestoreId("susp_${UUID.randomUUID().toString().replace("-", "").take(12)}")
        db.collection("users_private").document(uid)
            .collection("notifications").document(notifId)
            .set(
                hashMapOf<String, Any>(
                    "id" to notifId,
                    "type" to "alert",
                    "titleAr" to "تم إيقاف حسابك مؤقتاً لمدة $days يوم",
                    "titleEn" to "Account suspended for $days days",
                    "bodyAr" to "السبب: $reason",
                    "bodyEn" to "Reason: $reason",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun unsuspendUser(uid: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("users_public").document(uid).update(
            mapOf(
                "status" to UserStatus.ACTIVE.wireValue,
                "suspensionDays" to 0,
                "suspendedUntil" to "",
                "suspensionReason" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteUserAsAdmin(uid: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("users_public").document(uid).delete().await()
        runCatching { db.collection("users_private").document(uid).delete().await() }
    }

    suspend fun updateReportStatus(reportId: String, newStatus: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("reports").document(reportId).update(
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun approveFlaggedProduct(productId: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("products").document(productId).update(
            mapOf(
                "status" to ProductStatus.ACTIVE.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun fetchAdminQuotaStatus(isAdmin: Boolean): CloudinaryQuotaStatus {
        return cloudinaryService.fetchAdminQuotaStatus(isAdmin)
            .getOrDefault(CloudinaryQuotaStatus())
    }
}
