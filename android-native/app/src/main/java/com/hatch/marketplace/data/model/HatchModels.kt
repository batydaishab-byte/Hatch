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
    VERIFICATION,
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

enum class AccountType(val wireValue: String) {
    PERSONAL("personal"),
    STORE("store"),
    COMPANY("company");

    fun isCommercial(): Boolean = this == STORE || this == COMPANY

    companion object {
        fun fromWire(value: String?): AccountType =
            entries.firstOrNull { it.wireValue == value?.trim()?.lowercase(Locale.ROOT) } ?: PERSONAL
    }
}

enum class GuestAction {
    BROWSE_MARKET,
    BROWSE_SWIPE_DECK,
    VIEW_PRODUCT_DETAIL,
    VIEW_COMMERCIAL_DIRECTORY,
    VIEW_BUSINESS_PROFILE,
    SEARCH_OR_FILTER,
    PUBLISH_LISTING,
    EDIT_OR_DELETE_LISTING,
    MAKE_OFFER_OR_BUY,
    CHAT_OR_MESSAGE,
    DIRECT_CONTACT_OR_CALL,
    TOGGLE_FAVORITE,
    RATE_OR_REVIEW,
    REPORT_VIOLATION,
    WATCH_REWARD_OR_BOOST
}

enum class GuestPromptReason {
    CONTACT_OR_BUY,
    SELL,
    OFFER_NEGOTIATE,
    FAVORITES,
    RATE_OR_REPORT,
    GENERAL
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
    val suspendedUntilTs: Timestamp? = null,
    val suspensionDays: Int? = null,
    val suspensionReason: String? = null,
    val lastSoldProductId: String? = null,
    val accountType: String = AccountType.PERSONAL.wireValue,
    val businessActivity: String? = null,
    val businessLogo: String? = null,
    val joinedDate: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun PublicUser.resolvedAccountType(): AccountType = AccountType.fromWire(accountType)

fun PublicUser.displayLogoOrAvatar(): String =
    businessLogo?.takeIf { it.isNotBlank() } ?: avatar

data class PrivateUser(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "user",
    val rewardCredits: Int = 0,
    val lastCreditTxId: String? = null,
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
    val sellerAccountType: String = AccountType.PERSONAL.wireValue,
    val sellerBusinessActivity: String? = null,
    val paymentMethods: List<String> = listOf(PaymentMethod.BANKAK.wireValue, PaymentMethod.CASH.wireValue),
    val negotiable: Boolean = true,
    val deliveryAvailable: Boolean = false,
    val urgentSale: Boolean = false,
    val featured: Boolean = false,
    val featuredTxId: String? = null,
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val status: String = ProductStatus.ACTIVE.wireValue,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun Product.resolvedSellerAccountType(): AccountType = AccountType.fromWire(sellerAccountType)

enum class OfferTransitionAction {
    SUBMIT_OFFER,
    COUNTER_OFFER,
    ACCEPT_OFFER,
    DECLINE_OFFER
}

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
    val offerUpdatedBy: String? = null,
    val acceptedBy: String? = null,
    val acceptedAt: Timestamp? = null,
    val offerHistory: List<Map<String, Any>> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class VerificationRequest(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val accountType: String = AccountType.PERSONAL.wireValue,
    val city: String = "",
    val note: String = "",
    val documentUrl: String? = null,
    val status: String = "pending",
    val reviewedBy: String? = null,
    val rejectionReason: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val reviewedAt: Timestamp? = null
)

data class CreditTransaction(
    val id: String = "",
    val uid: String = "",
    val type: String = "",
    val amount: Int = 0,
    val balanceBefore: Int = 0,
    val balanceAfter: Int = 0,
    val productId: String? = null,
    val grantedBy: String? = null,
    val reason: String = "",
    val createdAt: Timestamp? = null
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
    val conversationId: String? = null,
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

data class SellerBrandSnapshot(
    val sellerName: String,
    val sellerAvatar: String,
    val sellerAccountType: String,
    val sellerBusinessActivity: String
)

data class CommercialDirectoryEntry(
    val sellerId: String,
    val name: String,
    val logoUrl: String,
    val accountType: AccountType,
    val businessActivity: String,
    val city: String,
    val rating: Double,
    val dealsCompleted: Int,
    val verifiedIdentity: Boolean,
    val products: List<Product>
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
    const val MAX_ATOMIC_SUSPEND_PRODUCTS = 450

    /**
     * Admin UI & query access MUST align with Firestore `allow list` rules (`isAdminClaim()`),
     * which require a verified email and Firebase Auth Custom Claim `admin == true`.
     * Having `/admins/{uid}` (`hasAdminDoc`) without the custom claim cannot execute admin
     * collection queries or media-signer admin APIs, so it must not unlock the Admin UI.
     */
    fun isAdminRole(
        hasAdminClaim: Boolean,
        @Suppress("UNUSED_PARAMETER") hasAdminDoc: Boolean = false
    ): Boolean = hasAdminClaim

    fun canSuspendProductsAtomically(activeProductsCount: Int): Boolean =
        activeProductsCount in 0..MAX_ATOMIC_SUSPEND_PRODUCTS

    fun isUserSuspended(
        status: String,
        suspendedUntilIso: String?,
        suspendedUntilTs: Timestamp? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (status != UserStatus.SUSPENDED.wireValue) return false
        if (suspendedUntilTs != null) {
            return suspendedUntilTs.toDate().time > nowMillis
        }
        if (suspendedUntilIso.isNullOrBlank()) return true
        val expiryMillis = parseIsoTimestampMillis(suspendedUntilIso) ?: return true
        return expiryMillis > nowMillis
    }

    fun buildSuspensionEndTimestamp(days: Int, nowMillis: Long = System.currentTimeMillis()): Timestamp {
        val safeDays = days.coerceIn(1, 3650)
        val endMillis = nowMillis + safeDays * 86_400_000L
        return Timestamp(Date(endMillis))
    }

    fun buildSuspensionEndIso(days: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val safeDays = days.coerceIn(1, 3650)
        val endMillis = nowMillis + safeDays * 86_400_000L
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date(endMillis))
    }

    fun isProductEligibleForOffers(productStatus: String): Boolean =
        productStatus == ProductStatus.ACTIVE.wireValue

    fun validateOfferTransition(
        currentStatus: String,
        actorUid: String,
        buyerId: String,
        sellerId: String,
        action: OfferTransitionAction,
        amount: Double? = null,
        productStatus: String = ProductStatus.ACTIVE.wireValue
    ): Result<OfferStatus> = runCatching {
        require(actorUid == buyerId || actorUid == sellerId) {
            "Only conversation participants can update offers."
        }
        require(isProductEligibleForOffers(productStatus)) {
            "Offers are only allowed on active listings."
        }
        require(currentStatus != OfferStatus.ACCEPTED.wireValue) {
            "Offer has already been accepted and is locked."
        }

        when (action) {
            OfferTransitionAction.SUBMIT_OFFER -> {
                require(actorUid == buyerId) { "Only the buyer can submit an offer." }
                require(amount != null && amount > 0.0) { "Offer amount must be positive." }
                OfferStatus.PENDING
            }
            OfferTransitionAction.COUNTER_OFFER -> {
                require(actorUid == sellerId) { "Only the seller can send a counter offer." }
                require(currentStatus == OfferStatus.PENDING.wireValue) {
                    "Counter offer is only allowed on a pending buyer offer."
                }
                require(amount != null && amount > 0.0) { "Counter amount must be positive." }
                OfferStatus.COUNTERED
            }
            OfferTransitionAction.ACCEPT_OFFER -> {
                val allowed = (actorUid == sellerId && currentStatus == OfferStatus.PENDING.wireValue) ||
                    (actorUid == buyerId && currentStatus == OfferStatus.COUNTERED.wireValue)
                require(allowed) { "You cannot accept the offer in its current state." }
                OfferStatus.ACCEPTED
            }
            OfferTransitionAction.DECLINE_OFFER -> {
                val allowed = (actorUid == sellerId && currentStatus == OfferStatus.PENDING.wireValue) ||
                    (actorUid == buyerId && currentStatus == OfferStatus.COUNTERED.wireValue)
                require(allowed) { "You cannot decline the offer in its current state." }
                OfferStatus.DECLINED
            }
        }
    }

    fun validateOfferTransition(
        conversation: Conversation,
        actorUid: String,
        action: OfferTransitionAction,
        amount: Double? = null,
        counterAmount: Double? = null,
        productStatus: String = ProductStatus.ACTIVE.wireValue
    ): Result<OfferStatus> = validateOfferTransition(
        currentStatus = conversation.offerStatus,
        actorUid = actorUid,
        buyerId = conversation.buyerId,
        sellerId = conversation.sellerId,
        action = action,
        amount = amount ?: counterAmount,
        productStatus = productStatus
    )

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

    fun resolveAccountType(raw: String?): AccountType = AccountType.fromWire(raw)

    fun requiresOnboarding(publicUser: PublicUser?): Boolean =
        publicUser == null || publicUser.name.isBlank() || publicUser.city.isBlank()

    fun isCommercialSeller(accountType: String?): Boolean =
        AccountType.fromWire(accountType).isCommercial()

    fun buildSellerBrandSnapshot(
        name: String,
        avatar: String,
        businessLogo: String?,
        accountType: AccountType,
        businessActivity: String?
    ): SellerBrandSnapshot {
        val cleanName = name.trim().take(80).ifBlank { "تاجر هتش" }
        val cleanLogo = if (accountType.isCommercial()) businessLogo?.trim().orEmpty().take(1000) else ""
        val effectiveAvatar = if (cleanLogo.isNotBlank()) cleanLogo else avatar.trim().take(1000)
        val cleanActivity = if (accountType.isCommercial()) businessActivity?.trim().orEmpty().take(120) else ""
        return SellerBrandSnapshot(
            sellerName = cleanName,
            sellerAvatar = effectiveAvatar,
            sellerAccountType = accountType.wireValue,
            sellerBusinessActivity = cleanActivity
        )
    }

    fun buildBusinessStorefrontProducts(products: List<Product>, sellerId: String): List<Product> =
        products
            .filter { it.sellerId == sellerId && it.status == ProductStatus.ACTIVE.wireValue }
            .sortedWith(
                compareByDescending<Product> { it.featured }
                    .thenByDescending { it.urgentSale }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )

    fun buildCommercialDirectory(
        users: List<PublicUser>,
        products: List<Product>,
        targetType: AccountType
    ): List<CommercialDirectoryEntry> {
        if (!targetType.isCommercial()) return emptyList()

        val activeProductsBySeller = products
            .filter { it.status == ProductStatus.ACTIVE.wireValue && it.sellerId.isNotBlank() }
            .groupBy { it.sellerId }
            .mapValues { (_, list) ->
                list.sortedWith(
                    compareByDescending<Product> { it.featured }
                        .thenByDescending { it.urgentSale }
                        .thenByDescending { it.createdAt?.seconds ?: 0L }
                )
            }

        val allUsersById = users.filter { it.uid.isNotBlank() }.associateBy { it.uid }

        val candidateIds = linkedSetOf<String>()
        users.forEach { user ->
            if (
                user.uid.isNotBlank() &&
                user.status == UserStatus.ACTIVE.wireValue &&
                user.resolvedAccountType() == targetType &&
                user.name.isNotBlank()
            ) {
                candidateIds.add(user.uid)
            }
        }
        activeProductsBySeller.forEach { (sellerId, sellerProducts) ->
            val liveUser = allUsersById[sellerId]
            val resolvedType = liveUser?.resolvedAccountType()
                ?: sellerProducts.firstOrNull()?.resolvedSellerAccountType()
            val isActiveAccount = liveUser == null || liveUser.status == UserStatus.ACTIVE.wireValue
            if (isActiveAccount && resolvedType == targetType) {
                candidateIds.add(sellerId)
            }
        }

        return candidateIds.mapNotNull { sellerId ->
            val liveUser = allUsersById[sellerId]
            val sellerProducts = activeProductsBySeller[sellerId].orEmpty()
            val firstProd = sellerProducts.firstOrNull()
            val displayName = liveUser?.name?.takeIf { it.isNotBlank() }
                ?: firstProd?.sellerName?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            CommercialDirectoryEntry(
                sellerId = sellerId,
                name = displayName,
                logoUrl = liveUser?.displayLogoOrAvatar()?.takeIf { it.isNotBlank() }
                    ?: firstProd?.sellerAvatar.orEmpty(),
                accountType = targetType,
                businessActivity = liveUser?.businessActivity?.takeIf { it.isNotBlank() }
                    ?: firstProd?.sellerBusinessActivity.orEmpty(),
                city = liveUser?.city?.takeIf { it.isNotBlank() }
                    ?: firstProd?.city.orEmpty(),
                rating = liveUser?.rating ?: firstProd?.sellerRating ?: 5.0,
                dealsCompleted = maxOf(liveUser?.dealsCompleted ?: 0, firstProd?.sellerDealsCount ?: 0),
                verifiedIdentity = liveUser?.verifiedIdentity ?: (firstProd?.sellerVerified == true),
                products = sellerProducts
            )
        }.sortedWith(
            compareByDescending<CommercialDirectoryEntry> { it.products.size }
                .thenByDescending { it.verifiedIdentity }
                .thenByDescending { it.rating }
                .thenBy { it.name }
        )
    }

    fun isActionAllowedForGuest(action: GuestAction): Boolean = when (action) {
        GuestAction.BROWSE_MARKET,
        GuestAction.BROWSE_SWIPE_DECK,
        GuestAction.VIEW_PRODUCT_DETAIL,
        GuestAction.VIEW_COMMERCIAL_DIRECTORY,
        GuestAction.VIEW_BUSINESS_PROFILE,
        GuestAction.SEARCH_OR_FILTER -> true

        GuestAction.PUBLISH_LISTING,
        GuestAction.EDIT_OR_DELETE_LISTING,
        GuestAction.MAKE_OFFER_OR_BUY,
        GuestAction.CHAT_OR_MESSAGE,
        GuestAction.DIRECT_CONTACT_OR_CALL,
        GuestAction.TOGGLE_FAVORITE,
        GuestAction.RATE_OR_REVIEW,
        GuestAction.REPORT_VIOLATION,
        GuestAction.WATCH_REWARD_OR_BOOST -> false
    }

    fun guestPromptReasonFor(action: GuestAction): GuestPromptReason = when (action) {
        GuestAction.PUBLISH_LISTING,
        GuestAction.EDIT_OR_DELETE_LISTING -> GuestPromptReason.SELL
        GuestAction.MAKE_OFFER_OR_BUY -> GuestPromptReason.OFFER_NEGOTIATE
        GuestAction.CHAT_OR_MESSAGE,
        GuestAction.DIRECT_CONTACT_OR_CALL -> GuestPromptReason.CONTACT_OR_BUY
        GuestAction.TOGGLE_FAVORITE -> GuestPromptReason.FAVORITES
        GuestAction.RATE_OR_REVIEW,
        GuestAction.REPORT_VIOLATION -> GuestPromptReason.RATE_OR_REPORT
        else -> GuestPromptReason.GENERAL
    }

    fun sanitizeCommercialProfileForGuest(user: PublicUser): PublicUser? {
        if (user.uid.isBlank() || user.name.isBlank()) return null
        if (user.status != UserStatus.ACTIVE.wireValue) return null
        if (!user.resolvedAccountType().isCommercial()) return null
        return user.copy(
            suspendedUntil = null,
            suspendedUntilTs = null,
            suspensionDays = null,
            suspensionReason = null
        )
    }

    fun mergeUsersAndCommercialProfiles(
        publicUsers: List<PublicUser>,
        commercialProfiles: List<PublicUser>,
        currentUser: PublicUser? = null
    ): List<PublicUser> {
        val merged = linkedMapOf<String, PublicUser>()
        commercialProfiles.forEach { profile ->
            if (profile.uid.isNotBlank()) {
                merged[profile.uid] = profile
            }
        }
        publicUsers.forEach { user ->
            if (user.uid.isNotBlank()) {
                val existing = merged[user.uid]
                merged[user.uid] = if (existing == null) {
                    user
                } else {
                    user.copy(
                        businessLogo = user.businessLogo?.takeIf { it.isNotBlank() } ?: existing.businessLogo,
                        businessActivity = user.businessActivity?.takeIf { it.isNotBlank() } ?: existing.businessActivity
                    )
                }
            }
        }
        if (currentUser != null && currentUser.uid.isNotBlank()) {
            merged[currentUser.uid] = currentUser
        }
        return merged.values.toList()
    }

    fun buildDeterministicReviewId(productId: String, reviewerId: String): String {
        val cleanProduct = productId.trim()
        val cleanReviewer = reviewerId.trim()
        require(cleanProduct.isNotEmpty() && cleanReviewer.isNotEmpty()) {
            "Product ID and Reviewer ID are required."
        }
        val reviewId = "${cleanProduct}_${cleanReviewer}"
        require(reviewId.length in 1..128 && Regex("^[a-zA-Z0-9_\\-]+$").matches(reviewId)) {
            "Deterministic review ID must be 1..128 valid characters."
        }
        return reviewId
    }

    fun hasBuyerAlreadyReviewedProduct(
        reviews: List<UserReview>,
        currentUserId: String?,
        productId: String?
    ): Boolean {
        if (currentUserId.isNullOrBlank() || productId.isNullOrBlank()) return false
        return reviews.any { it.reviewerId == currentUserId && it.productId == productId }
    }

    fun findEligibleReviewConversation(
        conversations: List<Conversation>,
        products: List<Product>,
        reviews: List<UserReview>,
        currentUserId: String?,
        sellerId: String,
        productId: String? = null,
        isSuspended: Boolean = false
    ): Conversation? {
        if (currentUserId.isNullOrBlank() || isSuspended) return null
        if (sellerId.isBlank() || currentUserId == sellerId) return null

        val productsById = products.associateBy { it.id }
        return conversations.firstOrNull { conv ->
            conv.buyerId == currentUserId &&
                conv.sellerId == sellerId &&
                conv.offerStatus == OfferStatus.ACCEPTED.wireValue &&
                (productId == null || conv.productId == productId) &&
                productsById[conv.productId]?.status == ProductStatus.SOLD.wireValue &&
                !hasBuyerAlreadyReviewedProduct(reviews, currentUserId, conv.productId)
        }
    }
}
