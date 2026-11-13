package com.hatch.marketplace.data.model

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

enum class AppLanguage(val code: String) {
    AR("ar"),
    EN("en")
}

enum class MainTab {
    SWIPE,
    BROWSE,
    CHAT,
    FAVORITES,
    PROFILE,
    ADMIN
}

enum class ProfileSubTab {
    MY_ADS,
    SOLD_ADS,
    SAVED,
    SAFETY
}

enum class AdminSubTab {
    REPORTS,
    BROADCAST,
    SOLD,
    CONVERSATIONS,
    FLAGGED,
    MONETIZATION,
    USERS
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
    BOOKS_HOBBIES("books_hobbies");

    companion object {
        fun fromWire(value: String): CategoryId =
            entries.firstOrNull { it.wireValue == value } ?: PHONES
    }
}

enum class ProductCondition(val wireValue: String) {
    NEW("new"),
    LIKE_NEW("like_new"),
    GOOD("good"),
    FAIR("fair");

    companion object {
        fun fromWire(value: String): ProductCondition =
            entries.firstOrNull { it.wireValue == value } ?: GOOD
    }
}

enum class PaymentMethod(val wireValue: String) {
    BANKAK("bankak"),
    FAWRY("fawry"),
    CASH("cash"),
    OCASH("ocash");

    companion object {
        fun fromWire(value: String): PaymentMethod =
            entries.firstOrNull { it.wireValue == value } ?: CASH
    }
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
    ALL("all");

    companion object {
        fun fromWire(value: String): AdFormat =
            entries.firstOrNull { it.wireValue == value } ?: BANNER
    }
}

enum class ToastType {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

data class UiToast(
    val id: String,
    val message: String,
    val type: ToastType = ToastType.INFO
)

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
    val productTitle: String? = null,
    val createdAt: Timestamp? = null
)

data class ReportTicket(
    val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedUserId: String? = null,
    val reportedUserName: String? = null,
    val reportedProductId: String? = null,
    val reportedProductTitle: String? = null,
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
    val logoUrl: String? = null,
    val videoUrl: String? = null,
    val mediaType: String = "image",
    val ctaTextAr: String = "",
    val ctaTextEn: String = "",
    val targetUrl: String = "",
    val category: String = "",
    val badgeAr: String = "",
    val badgeEn: String = "",
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
    val admobAppId: String = "",
    val bannerUnitId: String = "",
    val interstitialUnitId: String = "",
    val rewardedUnitId: String = "",
    val nativeUnitId: String = "",
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

data class MarketFilterState(
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val selectedCity: String = "all",
    val selectedCondition: String = "all",
    val minPrice: Double = 0.0,
    val maxPrice: Double = 5_000_000.0,
    val unlimitedPrice: Boolean = true,
    val onlyBankak: Boolean = false,
    val onlyUrgent: Boolean = false,
    val onlyNegotiable: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedCity != "all") count++
            if (selectedCondition != "all") count++
            if (!unlimitedPrice || minPrice > 0.0) count++
            if (onlyBankak) count++
            if (onlyUrgent) count++
            if (onlyNegotiable) count++
            return count
        }
}

data class AdRevenueStats(
    val totalImpressions: Int,
    val totalClicks: Int,
    val ctrPercent: Double,
    val estimatedUsd: Double,
    val estimatedSdg: Double
)

object HatchFormatters {
    fun formatSdg(amount: Double, language: AppLanguage): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
        val formatted = nf.format(amount.roundToInt())
        return if (language == AppLanguage.AR) "$formatted ج.س" else "$formatted SDG"
    }

    fun sanitizeFirestoreId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .trim('_')
            .take(120)
        return cleaned.ifEmpty { "id_${System.currentTimeMillis()}" }
    }

    fun normalizeSudanPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("249") && digits.length >= 12 -> "+$digits"
            digits.startsWith("0") && digits.length == 10 -> "+249${digits.drop(1)}"
            digits.length == 9 -> "+249$digits"
            raw.trim().startsWith("+") -> "+$digits"
            else -> digits
        }
    }

    fun isValidSudanPhone(raw: String): Boolean {
        val normalized = normalizeSudanPhone(raw)
        val digits = normalized.filter { it.isDigit() }
        return digits.length in 9..15
    }
}

object MarketplaceLogic {
    const val AUTO_VERIFY_DEALS_THRESHOLD = 10

    fun shouldAutoVerify(dealsCompleted: Int, reportsCount: Int): Boolean =
        dealsCompleted >= AUTO_VERIFY_DEALS_THRESHOLD && reportsCount == 0

    fun isAdminRole(role: String?, email: String?): Boolean {
        val normalizedEmail = email?.trim()?.lowercase(Locale.ROOT)
        return role == "admin" || normalizedEmail == "batydaishab@gmail.com"
    }

    fun isUserSuspended(
        status: String,
        suspendedUntilIso: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (status != UserStatus.SUSPENDED.wireValue) return false
        if (suspendedUntilIso.isNullOrBlank()) return true
        val expiryMillis = parseIsoTimestampMillis(suspendedUntilIso) ?: return true
        return expiryMillis > nowMillis
    }

    fun buildSuspensionEndIso(days: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val safeDays = days.coerceIn(1, 3650)
        val endMillis = nowMillis + safeDays * 86_400_000L
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date(endMillis))
    }

    fun parseIsoTimestampMillis(iso: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            runCatching {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return fmt.parse(iso)?.time
            }
        }
        return null
    }

    fun filterProducts(
        products: List<Product>,
        filter: MarketFilterState,
        activeOnly: Boolean = true
    ): List<Product> {
        val q = filter.searchQuery.trim().lowercase(Locale.ROOT)
        return products.filter { p ->
            if (activeOnly && p.status != ProductStatus.ACTIVE.wireValue) return@filter false
            if (filter.selectedCategory != "all" && p.category != filter.selectedCategory) return@filter false
            if (filter.selectedCity != "all") {
                val selBase = filter.selectedCity.substringBefore("(").replace(" ", "").trim()
                val prodBase = p.city.substringBefore("(").replace(" ", "").trim()
                val cityMatches = p.city == filter.selectedCity || (selBase.isNotEmpty() && selBase == prodBase)
                if (!cityMatches) return@filter false
            }
            if (filter.selectedCondition != "all" && p.condition != filter.selectedCondition) return@filter false
            if (p.price < filter.minPrice) return@filter false
            if (!filter.unlimitedPrice && p.price > filter.maxPrice) return@filter false
            if (filter.onlyBankak && !p.paymentMethods.contains(PaymentMethod.BANKAK.wireValue)) return@filter false
            if (filter.onlyUrgent && !p.urgentSale) return@filter false
            if (filter.onlyNegotiable && !p.negotiable) return@filter false
            if (q.isNotBlank()) {
                val haystack = listOf(
                    p.titleAr, p.titleEn, p.descriptionAr, p.descriptionEn,
                    p.city, p.areaAr, p.areaEn, p.sellerName
                ).joinToString(" ").lowercase(Locale.ROOT)
                if (!haystack.contains(q)) return@filter false
            }
            true
        }.sortedWith(
            compareByDescending<Product> { it.featured }
                .thenByDescending { it.urgentSale }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )
    }

    fun computeRevenueStats(ads: List<SponsoredAdItem>, config: AdMobConfig): AdRevenueStats {
        val impressions = ads.sumOf { it.impressionsCount }
        val clicks = ads.sumOf { it.clicksCount }
        val ctr = if (impressions > 0) (clicks.toDouble() / impressions.toDouble()) * 100.0 else 0.0
        val estimatedUsd = (impressions / 1000.0) * config.ecpmRate + clicks * config.cpcRate
        val estimatedSdg = estimatedUsd * config.sdgRate
        return AdRevenueStats(
            totalImpressions = impressions,
            totalClicks = clicks,
            ctrPercent = ctr,
            estimatedUsd = estimatedUsd,
            estimatedSdg = estimatedSdg
        )
    }

    private val DEPOSIT_SCAM_KEYWORDS = listOf(
        "عربون", "تحويل مسبق", "حول لي", "بنكك قبل", "دفع مقدم",
        "deposit", "upfront", "send first", "wire first", "prepay"
    )

    fun hasDepositWarningKeywords(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return DEPOSIT_SCAM_KEYWORDS.any { lower.contains(it) }
    }
}
