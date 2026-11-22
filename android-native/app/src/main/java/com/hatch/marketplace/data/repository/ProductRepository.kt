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
import com.hatch.marketplace.data.model.resolvedAccountType
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ListingPublishRequest(
    val seller: PublicUser,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val price: Double,
    val originalPrice: Double?,
    val category: String,
    val condition: String,
    val city: String,
    val areaAr: String,
    val areaEn: String,
    val localImageUris: List<Uri>,
    val remoteImageUrls: List<String>,
    val paymentMethods: List<String>,
    val negotiable: Boolean,
    val deliveryAvailable: Boolean,
    val urgentSale: Boolean
)

sealed class ImageUrlValidationResult {
    data class Valid(val normalizedUrl: String) : ImageUrlValidationResult()
    data class Invalid(val messageAr: String, val messageEn: String) : ImageUrlValidationResult()
}

data class ListingDraftValidationResult(
    val isValid: Boolean,
    val resolvedPrice: Double = 0.0,
    val resolvedOriginalPrice: Double? = null,
    val resolvedRemoteUrls: List<String> = emptyList(),
    val errorMessage: String? = null
)

object ListingPublishValidator {
    fun normalizeNumericInput(raw: String): String {
        val sb = StringBuilder(raw.length)
        var hasDecimalPoint = false
        for (ch in raw) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                in '\u0660'..'\u0669' -> sb.append(('0'.code + (ch.code - '\u0660'.code)).toChar())
                in '\u06F0'..'\u06F9' -> sb.append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
                '.', '\u066B' -> {
                    if (!hasDecimalPoint) {
                        sb.append('.')
                        hasDecimalPoint = true
                    }
                }
            }
        }
        return sb.toString()
    }

    fun parsePositivePrice(raw: String): Double? {
        val normalized = normalizeNumericInput(raw)
        if (normalized.isBlank() || normalized == ".") return null
        val parsed = normalized.toDoubleOrNull() ?: return null
        return parsed.takeIf { it.isFinite() && it > 0.0 }
    }

    fun isValidHttpsImageUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.length > 1000) return false
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return false
        if (!trimmed.startsWith("https://", ignoreCase = true)) return false
        val normalized = "https://" + trimmed.substring(8)
        return try {
            val uri = URI(normalized)
            val host = uri.host?.trim().orEmpty()
            uri.scheme.equals("https", ignoreCase = true) &&
                host.isNotEmpty() &&
                host.contains('.') &&
                !host.startsWith('.') &&
                !host.endsWith('.')
        } catch (_: Exception) {
            false
        }
    }

    fun validateManualImageUrl(
        raw: String,
        currentCount: Int,
        existingUrls: List<String> = emptyList(),
        maxCount: Int = MediaUploadPolicy.MAX_PRODUCT_IMAGES_PER_LISTING
    ): ImageUrlValidationResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "أدخل رابط الصورة أولاً ثم اضغط +.",
                messageEn = "Enter an image URL first, then tap +."
            )
        }
        if (currentCount >= maxCount) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "وصلت للحد الأقصى ($maxCount صور للإعلان).",
                messageEn = "Maximum limit reached ($maxCount photos per listing)."
            )
        }
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "رابط الصورة غير صالح: يجب ألا يحتوي على مسافات.",
                messageEn = "Invalid image URL: must not contain spaces."
            )
        }
        if (trimmed.length > 1000) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "رابط الصورة طويل جداً (الحد الأقصى 1000 حرف).",
                messageEn = "Image URL is too long (max 1000 characters)."
            )
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "لأسباب أمنية، يجب أن يبدأ رابط الصورة بـ https:// وليس http://.",
                messageEn = "For security, image URLs must start with https://, not http://."
            )
        }
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "رابط الصورة غير صالح: يجب أن يبدأ بـ https://.",
                messageEn = "Invalid image URL: must start with https://."
            )
        }
        val normalized = "https://" + trimmed.substring(8)
        if (!isValidHttpsImageUrl(normalized)) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "رابط الصورة غير صالح: تأكد من كتابة اسم النطاق كاملاً بعد https://.",
                messageEn = "Invalid image URL: make sure the domain name after https:// is valid."
            )
        }
        if (existingUrls.any { it.equals(normalized, ignoreCase = false) }) {
            return ImageUrlValidationResult.Invalid(
                messageAr = "تمت إضافة رابط الصورة هذا بالفعل.",
                messageEn = "This image URL has already been added."
            )
        }
        return ImageUrlValidationResult.Valid(normalized)
    }

    fun validateDraft(
        title: String,
        description: String,
        priceText: String,
        originalPriceText: String,
        city: String,
        localImageCount: Int,
        remoteUrls: List<String>,
        pendingUrlInput: String,
        paymentMethods: List<String>,
        signerReady: Boolean,
        isArabic: Boolean
    ): ListingDraftValidationResult {
        fun msg(ar: String, en: String) = if (isArabic) ar else en

        val combinedRemoteUrls = remoteUrls.toMutableList()
        val pendingTrimmed = pendingUrlInput.trim()
        if (pendingTrimmed.isNotEmpty()) {
            when (val urlCheck = validateManualImageUrl(
                raw = pendingTrimmed,
                currentCount = localImageCount + combinedRemoteUrls.size,
                existingUrls = combinedRemoteUrls
            )) {
                is ImageUrlValidationResult.Valid -> combinedRemoteUrls.add(urlCheck.normalizedUrl)
                is ImageUrlValidationResult.Invalid -> {
                    return ListingDraftValidationResult(
                        isValid = false,
                        errorMessage = msg(urlCheck.messageAr, urlCheck.messageEn)
                    )
                }
            }
        }

        val totalImages = localImageCount + combinedRemoteUrls.size
        if (totalImages == 0) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "يرجى إضافة صورة واحدة على الأقل للمنتج (أو رابط https:// صحيح).",
                    "Please add at least one product photo (or a valid https:// link)."
                )
            )
        }

        if (localImageCount > 0 && !signerReady) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "رفع صور الهاتف غير مفعّل حالياً (يتطلب إعداد MEDIA_SIGNER_URL). يرجى إزالة صور الهاتف واستخدام رابط صورة يبدأ بـ https://.",
                    "Direct device photo upload is not enabled yet (requires MEDIA_SIGNER_URL). Remove device photos and use an https:// image link instead."
                )
            )
        }

        if (combinedRemoteUrls.any { !isValidHttpsImageUrl(it) }) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "أحد روابط الصور غير صالح أو لا يبدأ بـ https://.",
                    "One of the image links is invalid or does not start with https://."
                )
            )
        }

        val cleanTitle = title.trim()
        if (cleanTitle.length < 2) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "يرجى كتابة عنوان الإعلان (حرفان على الأقل).",
                    "Please enter a listing title (at least 2 characters)."
                )
            )
        }
        if (cleanTitle.length > 120) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "عنوان الإعلان طويل جداً (الحد الأقصى 120 حرفاً).",
                    "Listing title is too long (max 120 characters)."
                )
            )
        }

        val parsedPrice = parsePositivePrice(priceText)
        if (parsedPrice == null) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "يرجى إدخال سعر صحيح أكبر من صفر.",
                    "Please enter a valid price greater than 0."
                )
            )
        }
        if (parsedPrice > 10_000_000_000.0) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "السعر المدخل يتجاوز الحد المسموح به.",
                    "Entered price exceeds the allowed limit."
                )
            )
        }

        val parsedOriginalPrice = parsePositivePrice(originalPriceText)

        if (city.trim().length !in 2..100) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "يرجى اختيار مدينة صحيحة للإعلان.",
                    "Please select a valid city for the listing."
                )
            )
        }

        if (paymentMethods.isEmpty()) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "يرجى اختيار طريقة دفع واحدة على الأقل.",
                    "Please select at least one payment method."
                )
            )
        }

        val cleanDescription = description.trim()
        if (cleanDescription.length < 5) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "يرجى كتابة وصف للسلعة (5 أحرف على الأقل).",
                    "Please enter a product description (at least 5 characters)."
                )
            )
        }
        if (cleanDescription.length > 2000) {
            return ListingDraftValidationResult(
                isValid = false,
                errorMessage = msg(
                    "وصف السلعة طويل جداً (الحد الأقصى 2000 حرف).",
                    "Description is too long (max 2000 characters)."
                )
            )
        }

        return ListingDraftValidationResult(
            isValid = true,
            resolvedPrice = parsedPrice,
            resolvedOriginalPrice = parsedOriginalPrice,
            resolvedRemoteUrls = combinedRemoteUrls,
            errorMessage = null
        )
    }
}

@Singleton
class ProductRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clients: FirebaseClientsHolder,
    private val cloudinaryService: CloudinaryMediaService
) {
    fun observeProducts(currentUserId: String?, hasAdminClaim: Boolean): Flow<List<Product>> = callbackFlow {
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

        if (hasAdminClaim) {
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
        require(price.isFinite() && price > 0.0 && price <= 10_000_000_000.0) { "Price must be positive." }
        require(city.trim().length in 2..100) { "City must be 2 to 100 characters." }
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
            .filter { it.isNotEmpty() }
            .forEach { rawUrl ->
                val normalized = if (rawUrl.startsWith("https://", ignoreCase = true)) {
                    "https://" + rawUrl.substring(8)
                } else {
                    rawUrl
                }
                require(ListingPublishValidator.isValidHttpsImageUrl(normalized)) {
                    "Image URL must be a valid https:// link."
                }
                if (resolvedUrls.size < MediaUploadPolicy.MAX_PRODUCT_IMAGES_PER_LISTING) {
                    resolvedUrls.add(normalized)
                }
            }

        require(resolvedUrls.isNotEmpty()) { "Please add at least one valid product photo." }
        if (thumbnailUrl == null) thumbnailUrl = resolvedUrls.first()

        val brand = MarketplaceLogic.buildSellerBrandSnapshot(
            name = seller.name,
            avatar = seller.avatar,
            businessLogo = seller.businessLogo,
            accountType = seller.resolvedAccountType(),
            businessActivity = seller.businessActivity
        )

        val docId = HatchFormatters.sanitizeFirestoreId("prod_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "titleAr" to titleAr.trim().take(120),
            "titleEn" to titleEn.trim().ifBlank { titleAr.trim() }.take(120),
            "descriptionAr" to descriptionAr.trim().take(2000),
            "descriptionEn" to descriptionEn.trim().ifBlank { descriptionAr.trim() }.take(2000),
            "price" to price,
            "category" to category,
            "condition" to condition,
            "city" to city.trim().take(100),
            "areaAr" to areaAr.trim().take(100),
            "areaEn" to areaEn.trim().ifBlank { areaAr.trim() }.take(100),
            "images" to resolvedUrls.take(5),
            "thumbnailUrl" to thumbnailUrl.take(1000),
            "sellerId" to seller.uid,
            "sellerName" to brand.sellerName,
            "sellerAvatar" to brand.sellerAvatar,
            "sellerRating" to seller.rating,
            "sellerDealsCount" to seller.dealsCompleted,
            "sellerVerified" to seller.verifiedIdentity,
            "sellerAccountType" to brand.sellerAccountType,
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
        if (brand.sellerBusinessActivity.isNotEmpty()) {
            payload["sellerBusinessActivity"] = brand.sellerBusinessActivity
        }
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
        clients.auth?.currentUser ?: error("Authentication required")
        require(titleAr.trim().length in 2..120) { "Title must be 2 to 120 characters." }
        require(descriptionAr.trim().length in 5..2000) { "Description must be 5 to 2000 characters." }
        require(price > 0.0) { "Price must be positive." }
        val cleanImages = images.map { it.trim() }
            .filter { it.startsWith("https://") || it.startsWith("http://") }
            .take(5)
        require(cleanImages.isNotEmpty()) { "At least one valid HTTPS image URL is required." }

        val updates = hashMapOf<String, Any>(
            "titleAr" to titleAr.trim().take(120),
            "titleEn" to titleEn.trim().ifBlank { titleAr.trim() }.take(120),
            "descriptionAr" to descriptionAr.trim().take(2000),
            "descriptionEn" to descriptionEn.trim().ifBlank { descriptionAr.trim() }.take(2000),
            "price" to price,
            "originalPrice" to (originalPrice ?: 0.0),
            "category" to category,
            "condition" to condition,
            "city" to city.trim().take(100),
            "areaAr" to areaAr.trim().take(100),
            "areaEn" to areaAr.trim().take(100),
            "images" to cleanImages,
            "thumbnailUrl" to cleanImages.first().take(1000),
            "paymentMethods" to paymentMethods.distinct(),
            "negotiable" to negotiable,
            "deliveryAvailable" to deliveryAvailable,
            "urgentSale" to urgentSale,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("products").document(productId).update(updates).await()
    }

    suspend fun markProductAsSold(productId: String, sellerId: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val authUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(authUid == sellerId) { "Seller UID mismatch." }
        val productRef = db.collection("products").document(productId)
        val userRef = db.collection("users_public").document(sellerId)
        val commercialRef = db.collection("commercial_profiles").document(sellerId)

        db.runTransaction { tx ->
            val productSnap = tx.get(productRef)
            val userSnap = tx.get(userRef)
            val commercialSnap = tx.get(commercialRef)

            require(productSnap.exists()) { "Product not found." }
            require(productSnap.getString("sellerId") == sellerId) { "Only the seller can mark this product sold." }
            require(productSnap.getString("status") == ProductStatus.ACTIVE.wireValue) { "Only active listings can be marked sold." }

            val currentDeals = (userSnap.getLong("dealsCompleted") ?: 0L).toInt()
            val nextDeals = currentDeals + 1

            tx.update(
                productRef,
                mapOf(
                    "status" to ProductStatus.SOLD.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val dealUpdatePayload = mapOf(
                "dealsCompleted" to nextDeals,
                "lastSoldProductId" to productId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            tx.update(userRef, dealUpdatePayload)
            if (commercialSnap.exists()) {
                tx.update(commercialRef, dealUpdatePayload)
            }
            Unit
        }.await()
    }

    suspend fun featureProductWithCredit(productId: String, ownerUid: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val authUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(authUid == ownerUid) { "Owner UID mismatch." }
        val privateRef = db.collection("users_private").document(ownerUid)
        val productRef = db.collection("products").document(productId)
        val txId = HatchFormatters.sanitizeFirestoreId("ctx_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val txRef = db.collection("credit_transactions").document(txId)

        db.runTransaction { tx ->
            val privateSnap = tx.get(privateRef)
            val productSnap = tx.get(productRef)
            require(privateSnap.exists()) { "Private profile not found." }
            require(productSnap.exists()) { "Listing not found." }
            require(productSnap.getString("sellerId") == ownerUid) { "Only the owner can boost this listing." }
            require(productSnap.getString("status") == ProductStatus.ACTIVE.wireValue) { "Only active listings can be boosted." }
            require(productSnap.getBoolean("featured") != true) { "Listing is already featured." }

            val credits = (privateSnap.getLong("rewardCredits") ?: 0L).toInt()
            require(credits >= 1) { "You need at least 1 boost credit to feature this listing." }

            tx.update(
                privateRef,
                mapOf(
                    "rewardCredits" to (credits - 1),
                    "lastCreditTxId" to txId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                productRef,
                mapOf(
                    "featured" to true,
                    "featuredTxId" to txId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                txRef,
                hashMapOf<String, Any>(
                    "id" to txId,
                    "uid" to ownerUid,
                    "type" to "ad_boost_spend",
                    "amount" to -1,
                    "balanceBefore" to credits,
                    "balanceAfter" to (credits - 1),
                    "productId" to productId,
                    "reason" to "Featured listing boost",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        clients.auth?.currentUser ?: error("Authentication required")
        db.collection("products").document(productId).delete().await()
    }

    suspend fun incrementViews(productId: String) {
        val db = clients.firestore ?: return
        val authUid = clients.auth?.currentUser?.uid ?: return
        runCatching {
            val productRef = db.collection("products").document(productId)
            val viewRef = db.collection("users_private").document(authUid)
                .collection("viewed_products").document(productId)
            db.runTransaction { tx ->
                val viewSnap = tx.get(viewRef)
                if (viewSnap.exists()) return@runTransaction Unit
                val prodSnap = tx.get(productRef)
                if (
                    prodSnap.exists() &&
                    prodSnap.getString("status") == ProductStatus.ACTIVE.wireValue &&
                    prodSnap.getString("sellerId") != authUid
                ) {
                    val views = (prodSnap.getLong("viewsCount") ?: 0L).toInt()
                    tx.set(
                        viewRef,
                        mapOf(
                            "productId" to productId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        productRef,
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
        clients.auth?.currentUser ?: error("Authentication required")
        require(BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")) {
            "Image upload requires signed worker URL (MEDIA_SIGNER_URL)."
        }
        val compressed = ImageWebPCompressor.compressProductImageAndThumbnail(context, uri).getOrThrow()
        cloudinaryService.uploadProductImageWithThumbnail(compressed).getOrThrow().fullImageUrl
    }
}
