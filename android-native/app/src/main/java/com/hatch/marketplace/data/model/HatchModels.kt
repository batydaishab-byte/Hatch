package com.hatch.marketplace.data.model

import com.google.firebase.Timestamp

enum class AppLanguage(val code: String) {
    AR("ar"),
    EN("en")
}

enum class CategoryId(val wireValue: String) {
    PHONES("phones"),
    ELECTRONICS("electronics"),
    VEHICLES("vehicles"),
    FURNITURE("furniture"),
    FASHION("fashion"),
    APPLIANCES("appliances"),
    SOLAR_ENERGY("solar_energy"),
    REAL_ESTATE("real_estate"),
    BOOKS_HOBBIES("books_hobbies")
}

enum class ProductCondition(val wireValue: String) {
    NEW("new"),
    LIKE_NEW("like_new"),
    GOOD("good"),
    FAIR("fair")
}

enum class PaymentMethod(val wireValue: String) {
    BANKAK("bankak"),
    FAWRY("fawry"),
    CASH("cash"),
    OCASH("ocash")
}

enum class ProductStatus(val wireValue: String) {
    ACTIVE("active"),
    SOLD("sold"),
    UNDER_REVIEW("under_review"),
    FLAGGED("flagged")
}

enum class UserStatus(val wireValue: String) {
    ACTIVE("active"),
    SUSPENDED("suspended")
}

enum class OfferStatus(val wireValue: String) {
    NONE("none"),
    PENDING("pending"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    COUNTERED("countered")
}

enum class AdFormat(val wireValue: String) {
    BANNER("banner"),
    INTERSTITIAL("interstitial"),
    REWARDED("rewarded"),
    NATIVE("native"),
    ALL("all")
}

object SudaneseCities {
    val ALL: List<String> = listOf(
        "الخرطوم (ولاية الخرطوم)",
        "أم درمان (ولاية الخرطوم)",
        "بحري (ولاية الخرطوم)",
        "ودمدني (ولاية الجزيرة)",
        "المناقل (ولاية الجزيرة)",
        "رفاعة (ولاية الجزيرة)",
        "الحصاحيصا (ولاية الجزيرة)",
        "الكاملين (ولاية الجزيرة)",
        "عطبرة (ولاية نهر النيل)",
        "الدامر (ولاية نهر النيل)",
        "شندي (ولاية نهر النيل)",
        "أبو حمد (ولاية نهر النيل)",
        "بربر (ولاية نهر النيل)",
        "دنقلا (الولاية الشمالية)",
        "مروي (الولاية الشمالية)",
        "كريمة (الولاية الشمالية)",
        "وادي حلفا (الولاية الشمالية)",
        "بورتسودان (ولاية البحر الأحمر)",
        "سواكن (ولاية البحر الأحمر)",
        "طوكر (ولاية البحر الأحمر)",
        "حلايب (ولاية البحر الأحمر)",
        "كسلا (ولاية كسلا)",
        "حلفا الجديدة (ولاية كسلا)",
        "القضارف (ولاية القضارف)",
        "الفاو (ولاية القضارف)",
        "الحواتة (ولاية القضارف)",
        "سنجة (ولاية سنار)",
        "سنار (ولاية سنار)",
        "الدندر (ولاية سنار)",
        "كوستي (ولاية النيل الأبيض)",
        "ربك (ولاية النيل الأبيض)",
        "الدويم (ولاية النيل الأبيض)",
        "الجبلين (ولاية النيل الأبيض)",
        "الأبيض (ولاية شمال كردفان)",
        "أم روابة (ولاية شمال كردفان)",
        "الرهد (ولاية شمال كردفان)",
        "كادقلي (ولاية جنوب كردفان)",
        "الدلنج (ولاية جنوب كردفان)",
        "رشاد (ولاية جنوب كردفان)",
        "تلودي (ولاية جنوب كردفان)",
        "الفولة (ولاية غرب كردفان)",
        "النهود (ولاية غرب كردفان)",
        "بابنوسة (ولاية غرب كردفان)",
        "المجلد (ولاية غرب كردفان)",
        "الفاشر (ولاية شمال دارفور)",
        "مليط (ولاية شمال دارفور)",
        "كتم (ولاية شمال دارفور)",
        "أم كدادة (ولاية شمال دارفور)",
        "نيالا (ولاية جنوب دارفور)",
        "برام (ولاية جنوب دارفور)",
        "الجنينة (ولاية غرب دارفور)",
        "زالنجي (ولاية وسط دارفور)",
        "نيرتتي (ولاية وسط دارفور)",
        "الضعين (ولاية شرق دارفور)",
        "الدمازين (ولاية النيل الأزرق)",
        "الرصيرص (ولاية النيل الأزرق)"
    )
}

/**
 * Shared Public User document stored at /users_public/{userId}
 * Contains NO PII (no email or phone).
 */
data class PublicUser(
    val uid: String = "",
    val name: String = "",
    val avatar: String = "",
    val city: String = "",
    val rating: Double = 5.0,
    val dealsCompleted: Int = 0,
    val reportsCount: Int = 0,
    val verifiedIdentity: Boolean = false,
    val status: String = UserStatus.ACTIVE.wireValue,
    val suspendedUntil: String? = null,
    val suspensionDays: Int? = null,
    val suspensionReason: String? = null,
    val joinedDate: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Isolated Private User document stored at /users_private/{userId}
 * Readable/writable ONLY by the owner or verified admin.
 */
data class PrivateUser(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "user",
    val rewardCredits: Int = 0,
    val fcmTokens: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Shared Marketplace Product stored at /products/{productId}
 */
data class Product(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val price: Double = 0.0,
    val originalPrice: Double? = null,
    val category: String = CategoryId.PHONES.wireValue,
    val condition: String = ProductCondition.GOOD.wireValue,
    val city: String = "",
    val areaAr: String = "",
    val areaEn: String = "",
    val images: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerAvatar: String = "",
    val sellerRating: Double = 5.0,
    val sellerDealsCount: Int = 0,
    val sellerVerified: Boolean = false,
    val paymentMethods: List<String> = listOf(PaymentMethod.BANKAK.wireValue, PaymentMethod.CASH.wireValue),
    val negotiable: Boolean = true,
    val deliveryAvailable: Boolean = false,
    val urgentSale: Boolean = false,
    val featured: Boolean = false,
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val status: String = ProductStatus.ACTIVE.wireValue,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Conversation(
    val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productPrice: Double = 0.0,
    val productImage: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val participantIds: List<String> = emptyList(),
    val buyerName: String = "",
    val buyerAvatar: String = "",
    val sellerName: String = "",
    val sellerAvatar: String = "",
    val lastMessage: String = "",
    val unreadByBuyer: Int = 0,
    val unreadBySeller: Int = 0,
    val offerAmount: Double? = null,
    val counterAmount: Double? = null,
    val offerStatus: String = OfferStatus.NONE.wireValue,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val status: String = "sent",
    val isOfferUpdate: Boolean = false,
    val offerAmount: Double? = null,
    val audioUrl: String? = null,
    val audioDuration: Int? = null,
    val imageUrl: String? = null,
    val imageTag: String? = null,
    val createdAt: Timestamp? = null
)

data class UserReview(
    val id: String = "",
    val sellerId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewerAvatar: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val tags: List<String> = emptyList(),
    val productId: String? = null,
    val createdAt: Timestamp? = null
)

data class ReportTicket(
    val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedUserId: String? = null,
    val reportedProductId: String? = null,
    val reason: String = "",
    val message: String = "",
    val status: String = "pending",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class NotificationItem(
    val id: String = "",
    val type: String = "system",
    val titleAr: String = "",
    val titleEn: String = "",
    val bodyAr: String = "",
    val bodyEn: String = "",
    val read: Boolean = false,
    val relatedProductId: String? = null,
    val relatedConversationId: String? = null,
    val createdAt: Timestamp? = null
)

data class SponsoredAdItem(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val advertiser: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val imageUrl: String = "",
    val videoUrl: String? = null,
    val mediaType: String = "image",
    val targetUrl: String = "",
    val format: String = AdFormat.BANNER.wireValue,
    val isActive: Boolean = true,
    val durationSeconds: Int = 8,
    val clicksCount: Int = 0,
    val impressionsCount: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class AdMobConfig(
    val isEnabled: Boolean = true,
    val isTestMode: Boolean = true,
    val ecpmRate: Double = 1.85,
    val cpcRate: Double = 0.14,
    val sdgRate: Double = 2650.0,
    val bannerRotationSeconds: Int = 8,
    val interstitialCountdownSeconds: Int = 5,
    val rewardedDurationSeconds: Int = 15,
    val updatedAt: Timestamp? = null
)

data class MediaQuotaStat(
    val creditsUsed: Double = 0.0,
    val creditsLimit: Double = 25.0,
    val usagePercent: Double = 0.0,
    val storageBytes: Long = 0L,
    val bandwidthBytes: Long = 0L,
    val isNearLimit: Boolean = false,
    val videoUploadsEnabled: Boolean = false,
    val updatedAt: Timestamp? = null
)
