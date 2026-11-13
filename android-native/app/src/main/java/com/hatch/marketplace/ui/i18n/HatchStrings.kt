package com.hatch.marketplace.ui.i18n

import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.CategoryId
import com.hatch.marketplace.data.model.PaymentMethod
import com.hatch.marketplace.data.model.ProductCondition

data class BroadcastTemplate(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val type: String,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String
)

data class InspectionTag(
    val id: String,
    val ar: String,
    val en: String
)

data class SudanAdPreset(
    val id: String,
    val advertiser: String,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val category: String,
    val ctaAr: String,
    val ctaEn: String,
    val badgeAr: String,
    val badgeEn: String,
    val imageUrl: String
)

data class HatchStrings(
    val language: AppLanguage,
    val appName: String,
    val appTagline: String,
    val splashTitle: String,
    val splashSubtitle: String,
    val navSwipe: String,
    val navBrowse: String,
    val navSell: String,
    val navChat: String,
    val navFavorites: String,
    val navProfile: String,
    val navAdmin: String,
    val allCities: String,
    val allCategories: String,
    val searchPlaceholder: String,
    val recentSearches: String,
    val clearAll: String,
    val filters: String,
    val resetFilters: String,
    val applyFilters: String,
    val priceRange: String,
    val unlimitedPrice: String,
    val onlyBankak: String,
    val onlyUrgent: String,
    val onlyNegotiable: String,
    val conditionLabel: String,
    val allConditions: String,
    val urgentBadge: String,
    val featuredBadge: String,
    val negotiableBadge: String,
    val verifiedBadge: String,
    val deliveryBadge: String,
    val viewsLabel: String,
    val remainingCards: String,
    val swipeEmptyTitle: String,
    val swipeEmptySubtitle: String,
    val reloadDeck: String,
    val browseAllAds: String,
    val swipeHintPass: String,
    val swipeHintLike: String,
    val swipeHintOffer: String,
    val makeOffer: String,
    val chatSeller: String,
    val callSeller: String,
    val shareAd: String,
    val reportAd: String,
    val rateSeller: String,
    val safetyTitle: String,
    val safetyWarningShort: String,
    val postAdTitle: String,
    val editAdTitle: String,
    val markSoldTitle: String,
    val markSoldConfirmBody: String,
    val signInTitle: String,
    val signInSubtitle: String,
    val signInGoogle: String,
    val signInEmail: String,
    val signUpEmail: String,
    val createAccountTitle: String,
    val guestBrowseBanner: String,
    val loginRequiredToast: String,
    val suspendedBannerTitle: String,
    val suspendedActionBlocked: String,
    val offlineBanner: String,
    val verifiedProgressTitle: String,
    val boostRewardTitle: String,
    val boostRewardSubtitle: String,
    val watchAdForCredit: String,
    val boostListingBtn: String,
    val chatEmptyTitle: String,
    val chatEmptySubtitle: String,
    val chatInputPlaceholder: String,
    val chatDepositBanner: String,
    val voiceRecordingLabel: String,
    val voiceHoldOrTapHint: String,
    val sendInspectionPhoto: String,
    val cameraCapture: String,
    val galleryPick: String,
    val offerPending: String,
    val offerAccepted: String,
    val offerDeclined: String,
    val offerCountered: String,
    val acceptOffer: String,
    val declineOffer: String,
    val counterOffer: String,
    val markSoldChatCta: String,
    val notificationsTitle: String,
    val markAllRead: String,
    val adminTitle: String,
    val sponsoredLabel: String,
    val adCountdownSkip: String
) {
    fun categoryLabel(category: String): String = when (CategoryId.fromWire(category)) {
        CategoryId.PHONES -> if (language == AppLanguage.AR) "هواتف وأجهزة لوحية" else "Phones & Tablets"
        CategoryId.ELECTRONICS -> if (language == AppLanguage.AR) "إلكترونيات ولابتوب" else "Electronics & Laptops"
        CategoryId.VEHICLES -> if (language == AppLanguage.AR) "سيارات ومركبات" else "Vehicles & Cars"
        CategoryId.FURNITURE -> if (language == AppLanguage.AR) "أثاث ومفروشات" else "Furniture & Home"
        CategoryId.FASHION -> if (language == AppLanguage.AR) "أزياء وموضة" else "Fashion & Style"
        CategoryId.APPLIANCES -> if (language == AppLanguage.AR) "أجهزة منزلية" else "Home Appliances"
        CategoryId.SOLAR_ENERGY -> if (language == AppLanguage.AR) "طاقة شمسية" else "Solar Energy"
        CategoryId.REAL_ESTATE -> if (language == AppLanguage.AR) "عقارات" else "Real Estate"
        CategoryId.BOOKS_HOBBIES -> if (language == AppLanguage.AR) "كتب وهوايات" else "Books & Hobbies"
    }

    fun conditionLabel(condition: String): String = when (ProductCondition.fromWire(condition)) {
        ProductCondition.NEW -> if (language == AppLanguage.AR) "جديد" else "New"
        ProductCondition.LIKE_NEW -> if (language == AppLanguage.AR) "شبه جديد" else "Like New"
        ProductCondition.GOOD -> if (language == AppLanguage.AR) "حالة جيدة" else "Good"
        ProductCondition.FAIR -> if (language == AppLanguage.AR) "مستعمل مقبول" else "Fair"
    }

    fun paymentLabel(method: String): String = when (PaymentMethod.fromWire(method)) {
        PaymentMethod.BANKAK -> if (language == AppLanguage.AR) "بنكك" else "Bankak"
        PaymentMethod.FAWRY -> if (language == AppLanguage.AR) "فوري" else "Fawry"
        PaymentMethod.CASH -> if (language == AppLanguage.AR) "كاش" else "Cash"
        PaymentMethod.OCASH -> if (language == AppLanguage.AR) "أوكاش" else "O-Cash"
    }

    val quickReplies: List<String>
        get() = if (language == AppLanguage.AR) {
            listOf(
                "هل المنتج ما زال متوفراً؟",
                "ممكن السعر النهائي لو تكرمت؟",
                "هل الدفع عبر بنكك مقبول عند المعاينة؟",
                "وين مكان المعاينة والتسليم؟",
                "ممكن صور إضافية أو فيديو للفحص؟"
            )
        } else {
            listOf(
                "Is this item still available?",
                "What is your final price?",
                "Is Bankak accepted upon in-person inspection?",
                "Where can we meet for inspection?",
                "Can you send more inspection photos?"
            )
        }

    val inspectionTags: List<InspectionTag>
        get() = listOf(
            InspectionTag("general", "معاينة عامة", "General View"),
            InspectionTag("body", "فحص الهيكل والنظافة", "Body & Condition"),
            InspectionTag("invoice", "فاتورة وضمان", "Invoice & Warranty"),
            InspectionTag("battery", "صحة البطارية والسيريال", "Battery & Serial"),
            InspectionTag("accessories", "الملحقات الأصلية", "Original Box & Items")
        )

    val reviewTags: List<Pair<String, String>>
        get() = listOf(
            "صدق الوصف" to "Accurate Description",
            "التزام بالموعد" to "Punctual Meetup",
            "تعامل راقي" to "Polite & Trustworthy",
            "سعر ممتاز" to "Fair Pricing",
            "دفع بنكك سلس" to "Smooth Bankak Transfer"
        )

    val reportReasons: List<Pair<String, String>>
        get() = listOf(
            "fake_deposit" to if (language == AppLanguage.AR) "طلب عربون / تحويل مسبق مشبوه" else "Suspicious Upfront Deposit / Bankak Scam",
            "counterfeit" to if (language == AppLanguage.AR) "منتج مقلد أو غير مطابق للوصف" else "Counterfeit or Misleading Listing",
            "spam" to if (language == AppLanguage.AR) "إعلان مكرر أو سبام" else "Spam or Duplicate Listing",
            "abuse" to if (language == AppLanguage.AR) "إساءة لفظية أو سلوك غير لائق" else "Harassment or Abusive Behavior",
            "prohibited" to if (language == AppLanguage.AR) "سلعة مخالفة لقوانين هتش" else "Prohibited Item"
        )

    val reportQuickPhrases: List<String>
        get() = if (language == AppLanguage.AR) {
            listOf(
                "البائع طلب تحويل عربون عبر بنكك قبل المعاينة",
                "الصور غير حقيقية أو منقولة من الإنترنت",
                "السعر غير حقيقي ومخالف للاتفاق",
                "البائع يتهرب من المعاينة المباشرة"
            )
        } else {
            listOf(
                "Seller requested an upfront Bankak deposit before meetup",
                "Images appear fake or copied from the web",
                "Price does not match actual listing offer",
                "Seller refuses in-person inspection"
            )
        }

    val suspensionPresetReasons: List<String>
        get() = listOf(
            "طلب عربون أو تحويل بنكك قبل المعاينة المباشرة",
            "نشر إعلانات مكررة أو معلومات مضللة",
            "عدم الالتزام بمواعيد التسليم المتفق عليها",
            "مخالفة سياسات البيع الآمن في هتش",
            "تكرار بلاغات المستخدمين بعد المراجعة"
        )

    val broadcastTemplates: List<BroadcastTemplate>
        get() = listOf(
            BroadcastTemplate(
                id = "safety",
                labelAr = "تحذير أمني",
                labelEn = "Safety Alert",
                type = "alert",
                titleAr = "تنبيه أمان من إدارة هتش",
                titleEn = "HATCH Safety Reminder",
                bodyAr = "لا تقم أبداً بتحويل عربون عبر بنكك أو فوري قبل معاينة السلعة يداً بيد في مكان عام.",
                bodyEn = "Never send an upfront Bankak or Fawry deposit before inspecting the item in person at a safe public spot."
            ),
            BroadcastTemplate(
                id = "promo",
                labelAr = "عروض مميزة",
                labelEn = "Promo Deal",
                type = "offer",
                titleAr = "تخفيضات اليوم في سوق هتش!",
                titleEn = "Today's Top Deals on HATCH!",
                bodyAr = "تفقد العروض العاجلة والموثقة الآن في الخرطوم وبورتسودان وعطبرة.",
                bodyEn = "Check out verified urgent deals now in Khartoum, Port Sudan, and Atbara."
            ),
            BroadcastTemplate(
                id = "solar",
                labelAr = "طاقة شمسية",
                labelEn = "Solar Deals",
                type = "update",
                titleAr = "عروض الطاقة الشمسية والانفرترات",
                titleEn = "Solar & Inverter Listings Updated",
                bodyAr = "تم إضافة ألواح شمسية وبطاريات ليثيوم بأسعار منافسة وضمان فحص مباشر.",
                bodyEn = "New solar panels and lithium batteries added with direct inspection options."
            ),
            BroadcastTemplate(
                id = "verify",
                labelAr = "توثيق 10 صفقات",
                labelEn = "Verification",
                type = "system",
                titleAr = "كيف تحصل على شارة التوثيق الذهبية؟",
                titleEn = "Unlock Verified Seller Badge",
                bodyAr = "أكمل 10 صفقات ناجحة بدون أي مخالفات ليتم توثيق حسابك تلقائياً!",
                bodyEn = "Complete 10 successful deals with zero reports to earn your Verified badge automatically!"
            )
        )

    val sudanAdPresets: List<SudanAdPreset>
        get() = listOf(
            SudanAdPreset(
                id = "bankak_pay",
                advertiser = "بنك الخرطوم • Bankak",
                titleAr = "حوّل وأدفع بأمان عبر تطبيق بنكك",
                titleEn = "Pay Safely via Bankak Mobile",
                descAr = "تحويل فوري بدون رسوم خفية أثناء معاينة مشترياتك في هتش.",
                descEn = "Instant transfers during your HATCH in-person meetup.",
                category = "finance",
                ctaAr = "افتح الإعلان",
                ctaEn = "View Offer",
                badgeAr = "شريك الدفع",
                badgeEn = "Payment Partner",
                imageUrl = "https://images.unsplash.com/photo-1563013544-824ae1b704d3?w=800&q=80"
            ),
            SudanAdPreset(
                id = "zain_5g",
                advertiser = "زين السودان • Zain SD",
                titleAr = "باقات إنترنت فائقة السرعة لرواد الأعمال",
                titleEn = "Zain SD Super Data Bundles",
                descAr = "تغطية قوية في بورتسودان وعطبرة ومدني والخرطوم لتصفح هتش بلا انقطاع.",
                descEn = "Reliable high-speed coverage across Sudan.",
                category = "telecom",
                ctaAr = "اكتشف الباقات",
                ctaEn = "Explore Plans",
                badgeAr = "راعي رسمي",
                badgeEn = "Sponsored",
                imageUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80"
            ),
            SudanAdPreset(
                id = "solar_hub",
                advertiser = "نور للطاقة الشمسية",
                titleAr = "منظومات طاقة شمسية متكاملة بضمان 5 سنوات",
                titleEn = "Complete Solar Systems - 5Y Warranty",
                descAr = "انفرترات هايبرد وبطاريات ليثيوم مع تركيب في بورتسودان ونهر النيل.",
                descEn = "Hybrid inverters & lithium batteries with installation.",
                category = "solar_energy",
                ctaAr = "اطلب عرض سعر",
                ctaEn = "Get Quote",
                badgeAr = "طاقة نظيفة",
                badgeEn = "Solar Partner",
                imageUrl = "https://images.unsplash.com/photo-1509391366360-2e959784a276?w=800&q=80"
            ),
            SudanAdPreset(
                id = "tirhal_delivery",
                advertiser = "ترحال إكسبرس • Tirhal",
                titleAr = "توصيل آمن وسريع داخل الولايات",
                titleEn = "Fast & Safe Local Delivery",
                descAr = "توصيل الطرود والمشتريات داخل المدينة خلال ساعتين.",
                descEn = "Same-day local courier for HATCH deals.",
                category = "delivery",
                ctaAr = "احجز توصيل",
                ctaEn = "Book Delivery",
                badgeAr = "توصيل موثوق",
                badgeEn = "Logistics",
                imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=800&q=80"
            )
        )

    companion object {
        fun forLanguage(lang: AppLanguage): HatchStrings = if (lang == AppLanguage.AR) Arabic else English

        private val Arabic = HatchStrings(
            language = AppLanguage.AR,
            appName = "هَـتـْـش",
            appTagline = "السوق السوداني الذكي",
            splashTitle = "هَـتـْـش • HATCH",
            splashSubtitle = "سوق الإعلانات والمنتجات في السودان • تصفح وفاوض واشترِ بذكاء",
            navSwipe = "تصفح سريع",
            navBrowse = "السوق",
            navSell = "بيع",
            navChat = "المحادثات",
            navFavorites = "المفضلة",
            navProfile = "حسابي",
            navAdmin = "الإدارة",
            allCities = "كل مدن السودان",
            allCategories = "الكل",
            searchPlaceholder = "ابحث عن هواتف، سيارات، طاقة شمسية، عقارات...",
            recentSearches = "عمليات البحث الأخيرة",
            clearAll = "مسح الكل",
            filters = "فلترة",
            resetFilters = "إعادة ضبط",
            applyFilters = "تطبيق الفلاتر",
            priceRange = "نطاق السعر (ج.س)",
            unlimitedPrice = "بدون حد أعلى للسعر",
            onlyBankak = "يقبل بنكك فقط",
            onlyUrgent = "بيع عاجل فقط",
            onlyNegotiable = "قابل للتفاوض فقط",
            conditionLabel = "حالة المنتج",
            allConditions = "جميع الحالات",
            urgentBadge = "عاجل",
            featuredBadge = "مميز",
            negotiableBadge = "قابل للتفاوض",
            verifiedBadge = "موثق",
            deliveryBadge = "توصيل متاح",
            viewsLabel = "مشاهدة",
            remainingCards = "منتج متبقي",
            swipeEmptyTitle = "شاهدت كل الإعلانات المتاحة حالياً!",
            swipeEmptySubtitle = "يمكنك إعادة تصفح البطاقات أو فتح السوق الكامل للبحث والفلاتر المتقدمة.",
            reloadDeck = "إعادة عرض البطاقات",
            browseAllAds = "فتح السوق الكامل",
            swipeHintPass = "تخطي",
            swipeHintLike = "حفظ",
            swipeHintOffer = "اسحب للأعلى لتقديم عرض فوري",
            makeOffer = "تقديم عرض",
            chatSeller = "تفاوض في المحادثة",
            callSeller = "اتصال",
            shareAd = "مشاركة",
            reportAd = "إبلاغ عن مخالفة",
            rateSeller = "تقييم البائع",
            safetyTitle = "إرشادات التداول الآمن في هتش",
            safetyWarningShort = "تحذير هتش: لا تحوّل أي عربون عبر بنكك أو فوري قبل معاينة السلعة شخصياً.",
            postAdTitle = "نشر إعلان جديد",
            editAdTitle = "تعديل الإعلان",
            markSoldTitle = "تأكيد إتمام البيع",
            markSoldConfirmBody = "هل تم بيع هذا المنتج بالفعل؟ سيتم أرشفة الإعلان وزيادة عداد صفقاتك الناجحة (+1) نحو شارة التوثيق.",
            signInTitle = "مرحباً بك في هتش",
            signInSubtitle = "سجّل الدخول لنشر الإعلانات والتفاوض المباشر ومزامنة مفضلاتك سحابياً",
            signInGoogle = "الدخول السريع عبر Google",
            signInEmail = "تسجيل الدخول بالبريد",
            signUpEmail = "إنشاء حساب جديد",
            createAccountTitle = "إكمال ملف التاجر",
            guestBrowseBanner = "وضع الضيف: تصفح الإعلانات بحرية — سجّل الدخول للتفاوض أو النشر",
            loginRequiredToast = "يرجى تسجيل الدخول أولاً لإتمام هذا الإجراء",
            suspendedBannerTitle = "حسابك موقوف مؤقتاً من الإدارة",
            suspendedActionBlocked = "الحساب موقوف حالياً: لا يمكنك نشر إعلانات أو تقديم عروض جديدة حتى انتهاء المدة.",
            offlineBanner = "لا يوجد اتصال بالإنترنت — يتم عرض البيانات المخزنة مؤقتاً",
            verifiedProgressTitle = "تقدم شارة التوثيق (10 صفقات بدون مخالفات)",
            boostRewardTitle = "رصيد الترويج المجاني",
            boostRewardSubtitle = "شاهد إعلاناً قصيراً لربح رصيد يميّز إعلانك في أعلى السوق",
            watchAdForCredit = "شاهد إعلان (+1 رصيد)",
            boostListingBtn = "تمييز إعلان (1 رصيد)",
            chatEmptyTitle = "لا توجد محادثات بعد",
            chatEmptySubtitle = "ابدأ التفاوض على أي منتج من السوق أو شاشة التمرير السريع.",
            chatInputPlaceholder = "اكتب رسالتك أو عرضك...",
            chatDepositBanner = "حماية هتش: ارفض طلبات العربون قبل المعاينة — اكتب أو أرسل صور الفحص هنا.",
            voiceRecordingLabel = "جارٍ تسجيل رسالة صوتية...",
            voiceHoldOrTapHint = "اضغط للتسجيل الصوتي",
            sendInspectionPhoto = "إرفاق صورة فحص",
            cameraCapture = "التقاط بالكاميرا",
            galleryPick = "اختيار من المعرض",
            offerPending = "عرض قيد الانتظار",
            offerAccepted = "تم قبول العرض",
            offerDeclined = "تم رفض العرض",
            offerCountered = "عرض مضاد",
            acceptOffer = "قبول العرض",
            declineOffer = "رفض",
            counterOffer = "عرض مضاد",
            markSoldChatCta = "تحديد كمباع وإغلاق الإعلان",
            notificationsTitle = "الإشعارات والتنبيهات",
            markAllRead = "تحديد الكل كمقروء",
            adminTitle = "لوحة إدارة هتش",
            sponsoredLabel = "إعلان ممول",
            adCountdownSkip = "تخطي الإعلان"
        )

        private val English = HatchStrings(
            language = AppLanguage.EN,
            appName = "HATCH",
            appTagline = "Sudan's Smart Marketplace",
            splashTitle = "HATCH • هتش",
            splashSubtitle = "Sudan's Classifieds & Marketplace • Swipe, Negotiate & Buy Smart",
            navSwipe = "Swipe",
            navBrowse = "Market",
            navSell = "Sell",
            navChat = "Chat",
            navFavorites = "Saved",
            navProfile = "Profile",
            navAdmin = "Admin",
            allCities = "All Sudan Cities",
            allCategories = "All",
            searchPlaceholder = "Search phones, cars, solar panels, real estate...",
            recentSearches = "Recent Searches",
            clearAll = "Clear All",
            filters = "Filters",
            resetFilters = "Reset",
            applyFilters = "Apply Filters",
            priceRange = "Price Range (SDG)",
            unlimitedPrice = "No Max Price Limit",
            onlyBankak = "Bankak Only",
            onlyUrgent = "Urgent Only",
            onlyNegotiable = "Negotiable Only",
            conditionLabel = "Condition",
            allConditions = "All Conditions",
            urgentBadge = "Urgent",
            featuredBadge = "Featured",
            negotiableBadge = "Negotiable",
            verifiedBadge = "Verified",
            deliveryBadge = "Delivery",
            viewsLabel = "views",
            remainingCards = "items left",
            swipeEmptyTitle = "You've seen all current listings!",
            swipeEmptySubtitle = "Reload the deck or open full Market Browse for filters & search.",
            reloadDeck = "Reload Cards",
            browseAllAds = "Open Full Market",
            swipeHintPass = "Pass",
            swipeHintLike = "Save",
            swipeHintOffer = "Swipe UP for Instant Offer",
            makeOffer = "Make Offer",
            chatSeller = "Negotiate in Chat",
            callSeller = "Call",
            shareAd = "Share",
            reportAd = "Report Violation",
            rateSeller = "Rate Seller",
            safetyTitle = "HATCH Safe Trading Guide",
            safetyWarningShort = "HATCH Safety: Never send upfront Bankak/Fawry deposits before inspecting in person.",
            postAdTitle = "Post New Listing",
            editAdTitle = "Edit Listing",
            markSoldTitle = "Confirm Sale",
            markSoldConfirmBody = "Confirm this item is sold? It will move to Sold Archive and increment your deals (+1) toward auto-verification.",
            signInTitle = "Welcome to HATCH",
            signInSubtitle = "Sign in to post listings, negotiate in real time, and sync favorites across devices.",
            signInGoogle = "Continue with Google",
            signInEmail = "Sign In with Email",
            signUpEmail = "Create Account",
            createAccountTitle = "Complete Seller Profile",
            guestBrowseBanner = "Guest Mode: Browse listings freely — Sign in to chat or sell",
            loginRequiredToast = "Please sign in first to continue",
            suspendedBannerTitle = "Your account is temporarily suspended",
            suspendedActionBlocked = "Suspended accounts cannot post new ads or send offers until suspension ends.",
            offlineBanner = "Offline — Showing cached marketplace data",
            verifiedProgressTitle = "Verified Seller Progress (10 clean deals)",
            boostRewardTitle = "Free Boost Credits",
            boostRewardSubtitle = "Watch a short sponsored ad to earn +1 credit and feature your listing",
            watchAdForCredit = "Watch Ad (+1 Credit)",
            boostListingBtn = "Feature Ad (1 Credit)",
            chatEmptyTitle = "No conversations yet",
            chatEmptySubtitle = "Start negotiating on any item from Market Browse or Swipe Deck.",
            chatInputPlaceholder = "Type a message or offer...",
            chatDepositBanner = "HATCH Shield: Refuse upfront deposits before inspection — share real photos here.",
            voiceRecordingLabel = "Recording voice note...",
            voiceHoldOrTapHint = "Tap to record voice note",
            sendInspectionPhoto = "Attach Inspection Photo",
            cameraCapture = "Take Camera Photo",
            galleryPick = "Choose from Gallery",
            offerPending = "Pending Offer",
            offerAccepted = "Offer Accepted",
            offerDeclined = "Offer Declined",
            offerCountered = "Counter Offer",
            acceptOffer = "Accept Offer",
            declineOffer = "Decline",
            counterOffer = "Counter",
            markSoldChatCta = "Mark as Sold & Close Listing",
            notificationsTitle = "Notifications & Alerts",
            markAllRead = "Mark All Read",
            adminTitle = "HATCH Admin Console",
            sponsoredLabel = "Sponsored",
            adCountdownSkip = "Skip Ad"
        )
    }
}
