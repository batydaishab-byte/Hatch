package com.hatch.marketplace.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.hatch.marketplace.BuildConfig
import com.hatch.marketplace.data.media.CloudinaryMediaService
import com.hatch.marketplace.data.media.ImageWebPCompressor
import com.hatch.marketplace.data.media.MediaUploadPolicy
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clients: FirebaseClientsHolder,
    private val cloudinaryService: CloudinaryMediaService
) {
    fun observeProducts(currentUserId: String?, isVerifiedAdminEmail: Boolean): Flow<List<Product>> = callbackFlow {
        val db = clients.firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val mergedMap = linkedMapOf<String, Product>()
        fun emitSorted() {
            val sorted = mergedMap.values.sortedByDescending { it.createdAt?.seconds ?: 0L }
            trySend(sorted)
        }

        if (isVerifiedAdminEmail) {
            val reg = db.collection("products").addSnapshotListener { snap, _ ->
                mergedMap.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)?.let { mergedMap[it.id] = it }
                }
                emitSorted()
            }
            awaitClose { reg.remove() }
            return@callbackFlow
        }

        val activeSoldReg = db.collection("products")
            .whereIn("status", listOf(ProductStatus.ACTIVE.wireValue, ProductStatus.SOLD.wireValue))
            .addSnapshotListener { snap, _ ->
                val activeSoldIds = mutableSetOf<String>()
                snap?.documents?.forEach { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)?.let { item ->
                        activeSoldIds.add(item.id)
                        mergedMap[item.id] = item
                    }
                }
                val iterator = mergedMap.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val isMine = currentUserId != null && entry.value.sellerId == currentUserId
                    if (!activeSoldIds.contains(entry.key) && !isMine) {
                        iterator.remove()
                    }
                }
                emitSorted()
            }

        val myAdsReg = if (!currentUserId.isNullOrBlank()) {
            db.collection("products")
                .whereEqualTo("sellerId", currentUserId)
                .addSnapshotListener { snap, _ ->
                    snap?.documents?.forEach { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)?.let {
                            mergedMap[it.id] = it
                        }
                    }
                    emitSorted()
                }
        } else null

        awaitClose {
            activeSoldReg.remove()
            myAdsReg?.remove()
        }
    }

    suspend fun createProduct(
        seller: PublicUser,
        titleAr: String,
        titleEn: String,
        descriptionAr: String,
        descriptionEn: String,
        price: Double,
        originalPrice: Double?,
        category: String,
        condition: String,
        city: String,
        areaAr: String,
        areaEn: String,
        localImageUris: List<Uri>,
        remoteImageUrls: List<String>,
        paymentMethods: List<String>,
        negotiable: Boolean,
        deliveryAvailable: Boolean,
        urgentSale: Boolean
    ): Result<String> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val authUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(authUid == seller.uid) { "Seller UID mismatch." }
        require(titleAr.trim().length in 2..120) { "Title must be 2 to 120 characters." }
        require(descriptionAr.trim().length in 5..2000) { "Description must be 5 to 2000 characters." }
        require(price > 0.0 && price <= 10_000_000_000.0) { "Price must be positive." }
        require(paymentMethods.isNotEmpty()) { "Select at least one payment method." }

        val resolvedUrls = mutableListOf<String>()
        var thumbnailUrl: String? = null

        val signerReady = BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")
        if (localImageUris.isNotEmpty()) {
            require(signerReady) {
                "Device image upload requires configuring MEDIA_SIGNER_URL (Cloudflare Worker), or paste direct HTTPS image links."
            }
            for (uri in localImageUris.take(MediaUploadPolicy.MAX_PRODUCT_IMAGES_PER_LISTING)) {
                val compressed = ImageWebPCompressor.compressProductImageAndThumbnail(context, uri).getOrThrow()
                val uploaded = cloudinaryService.uploadProductImageWithThumbnail(compressed).getOrThrow()
                resolvedUrls.add(uploaded.fullImageUrl)
                if (thumbnailUrl == null) thumbnailUrl = uploaded.thumbnailUrl
            }
        }

        remoteImageUrls.map { it.trim() }
            .filter { it.startsWith("https://") || it.startsWith("http://") }
            .forEach { url ->
                if (resolvedUrls.size < MediaUploadPolicy.MAX_PRODUCT_IMAGES_PER_LISTING) {
                    resolvedUrls.add(url)
                }
            }

        require(resolvedUrls.isNotEmpty()) { "Please add at least one valid product photo." }
        if (thumbnailUrl == null) thumbnailUrl = resolvedUrls.first()

        val docId = HatchFormatters.sanitizeFirestoreId("prod_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "titleAr" to titleAr.trim(),
            "titleEn" to titleEn.trim().ifBlank { titleAr.trim() },
            "descriptionAr" to descriptionAr.trim(),
            "descriptionEn" to descriptionEn.trim().ifBlank { descriptionAr.trim() },
            "price" to price,
            "category" to category,
            "condition" to condition,
            "city" to city,
            "areaAr" to areaAr.trim(),
            "areaEn" to areaEn.trim().ifBlank { areaAr.trim() },
            "images" to resolvedUrls.take(5),
            "thumbnailUrl" to thumbnailUrl,
            "sellerId" to seller.uid,
            "sellerName" to seller.name.ifBlank { "تاجر هتش" },
            "sellerAvatar" to seller.avatar,
            "sellerRating" to seller.rating,
            "sellerDealsCount" to seller.dealsCompleted,
            "sellerVerified" to seller.verifiedIdentity,
            "paymentMethods" to paymentMethods.distinct(),
            "negotiable" to negotiable,
            "deliveryAvailable" to deliveryAvailable,
            "urgentSale" to urgentSale,
            "featured" to false,
            "viewsCount" to 0,
            "likesCount" to 0,
            "status" to ProductStatus.ACTIVE.wireValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (originalPrice != null && originalPrice > price) {
            payload["originalPrice"] = originalPrice
        }

        db.collection("products").document(docId).set(payload).await()
        docId
    }

    suspend fun updateProduct(
        productId: String,
        titleAr: String,
        titleEn: String,
        descriptionAr: String,
        descriptionEn: String,
        price: Double,
        originalPrice: Double?,
        category: String,
        condition: String,
        city: String,
        areaAr: String,
        images: List<String>,
        paymentMethods: List<String>,
        negotiable: Boolean,
        deliveryAvailable: Boolean,
        urgentSale: Boolean
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        require(titleAr.trim().length in 2..120) { "Title must be 2 to 120 characters." }
        require(descriptionAr.trim().length in 5..2000) { "Description must be 5 to 2000 characters." }
        require(price > 0.0) { "Price must be positive." }
        require(images.isNotEmpty()) { "At least one image URL is required." }

        val updates = hashMapOf<String, Any>(
            "titleAr" to titleAr.trim(),
            "titleEn" to titleEn.trim().ifBlank { titleAr.trim() },
            "descriptionAr" to descriptionAr.trim(),
            "descriptionEn" to descriptionEn.trim().ifBlank { descriptionAr.trim() },
            "price" to price,
            "originalPrice" to (originalPrice ?: 0.0),
            "category" to category,
            "condition" to condition,
            "city" to city,
            "areaAr" to areaAr.trim(),
            "areaEn" to areaAr.trim(),
            "images" to images.take(5),
            "thumbnailUrl" to images.first(),
            "paymentMethods" to paymentMethods.distinct(),
            "negotiable" to negotiable,
            "deliveryAvailable" to deliveryAvailable,
            "urgentSale" to urgentSale,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("products").document(productId).update(updates).await()
    }

    suspend fun markProductAsSold(productId: String, sellerId: String): Result<Boolean> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val productRef = db.collection("products").document(productId)
        val userRef = db.collection("users_public").document(sellerId)

        productRef.update(
            mapOf(
                "status" to ProductStatus.SOLD.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val currentDeals = (userSnap.getLong("dealsCompleted") ?: 0L).toInt()
            val reportsCount = (userSnap.getLong("reportsCount") ?: 0L).toInt()
            val wasVerified = userSnap.getBoolean("verifiedIdentity") ?: false
            val nextDeals = currentDeals + 1
            val shouldVerify = MarketplaceLogic.shouldAutoVerify(nextDeals, reportsCount)
            tx.update(
                userRef,
                mapOf(
                    "dealsCompleted" to nextDeals,
                    "verifiedIdentity" to shouldVerify,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            !wasVerified && shouldVerify
        }.await()
    }

    suspend fun featureProductWithCredit(productId: String, ownerUid: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val privateRef = db.collection("users_private").document(ownerUid)
        val productRef = db.collection("products").document(productId)

        db.runTransaction { tx ->
            val privateSnap = tx.get(privateRef)
            val credits = (privateSnap.getLong("rewardCredits") ?: 0L).toInt()
            require(credits >= 1) { "You need at least 1 reward credit to feature this listing." }
            tx.update(
                privateRef,
                mapOf(
                    "rewardCredits" to (credits - 1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                productRef,
                mapOf(
                    "featured" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        db.collection("products").document(productId).delete().await()
    }

    suspend fun incrementViews(productId: String) {
        val db = clients.firestore ?: return
        clients.auth?.currentUser ?: return
        runCatching {
            val ref = db.collection("products").document(productId)
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                if (snap.exists() && snap.getString("status") == ProductStatus.ACTIVE.wireValue) {
                    val views = (snap.getLong("viewsCount") ?: 0L).toInt()
                    tx.update(
                        ref,
                        mapOf(
                            "viewsCount" to (views + 1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
        }
    }

    suspend fun uploadSingleInspectionOrAvatarImage(uri: Uri): Result<String> = runCatching {
        require(BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")) {
            "Image upload requires signed worker URL (MEDIA_SIGNER_URL)."
        }
        val compressed = ImageWebPCompressor.compressProductImageAndThumbnail(context, uri).getOrThrow()
        cloudinaryService.uploadProductImageWithThumbnail(compressed).getOrThrow().fullImageUrl
    }
}
