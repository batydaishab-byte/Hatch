package com.hatch.marketplace.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.hatch.marketplace.data.local.HatchPreferences
import com.hatch.marketplace.data.media.CloudinaryQuotaStatus
import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.AdMobConfig
import com.hatch.marketplace.data.model.AdRevenueStats
import com.hatch.marketplace.data.model.AdminSubTab
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.ChatMessage
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.CreditTransaction
import com.hatch.marketplace.data.model.GuestPromptReason
import com.hatch.marketplace.data.model.MainTab
import com.hatch.marketplace.data.model.MarketFilterState
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.NotificationItem
import com.hatch.marketplace.data.model.OfferStatus
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.ProfileSubTab
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.ReportTicket
import com.hatch.marketplace.data.model.SponsoredAdItem
import com.hatch.marketplace.data.model.ToastType
import com.hatch.marketplace.data.model.UiToast
import com.hatch.marketplace.data.model.UserReview
import com.hatch.marketplace.data.model.VerificationRequest
import com.hatch.marketplace.data.model.displayLogoOrAvatar
import com.hatch.marketplace.data.network.NetworkMonitor
import com.hatch.marketplace.data.repository.AuthRepository
import com.hatch.marketplace.data.repository.AuthSessionState
import com.hatch.marketplace.data.repository.ChatRepository
import com.hatch.marketplace.data.repository.InteractionRepository
import com.hatch.marketplace.data.repository.MonetizationRepository
import com.hatch.marketplace.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ReportRequest(
    val userId: String? = null,
    val userName: String? = null,
    val productId: String? = null,
    val productTitle: String? = null
)

data class ReviewRequest(
    val sellerId: String,
    val sellerName: String,
    val sellerAvatar: String = "",
    val productId: String? = null,
    val conversationId: String? = null,
    val productTitle: String? = null
)

data class HatchUiState(
    val showSplash: Boolean = true,
    val activeTab: MainTab = MainTab.SWIPE,
    val profileSubTab: ProfileSubTab = ProfileSubTab.MY_ADS,
    val adminSubTab: AdminSubTab = AdminSubTab.REPORTS,
    val language: AppLanguage = AppLanguage.AR,
    val isOnline: Boolean = true,
    val firebaseConnected: Boolean = false,
    val auth: AuthSessionState = AuthSessionState(),
    val products: List<Product> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val passedProductIds: Set<String> = emptySet(),
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val activeMessages: List<ChatMessage> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val readBroadcastIds: Set<String> = emptySet(),
    val reviews: List<UserReview> = emptyList(),
    val reports: List<ReportTicket> = emptyList(),
    val allUsers: List<PublicUser> = emptyList(),
    val commercialProfiles: List<PublicUser> = emptyList(),
    val verificationRequests: List<VerificationRequest> = emptyList(),
    val creditTransactions: List<CreditTransaction> = emptyList(),
    val sponsoredAds: List<SponsoredAdItem> = emptyList(),
    val adConfig: AdMobConfig = AdMobConfig(),
    val cloudinaryQuota: CloudinaryQuotaStatus = CloudinaryQuotaStatus(),
    val filter: MarketFilterState = MarketFilterState(),
    val searchHistory: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val toasts: List<UiToast> = emptyList(),
    val selectedProduct: Product? = null,
    val selectedBusinessSellerId: String? = null,
    val commercialDirectoryType: AccountType? = null,
    val guestPromptReason: GuestPromptReason? = null,
    val offerTargetProduct: Product? = null,
    val editingProduct: Product? = null,
    val soldTargetProduct: Product? = null,
    val reviewRequest: ReviewRequest? = null,
    val reportRequest: ReportRequest? = null,
    val adminAuditConversation: Conversation? = null,
    val showSellModal: Boolean = false,
    val showAuthSheet: Boolean = false,
    val forceOnboardingStep: Boolean = false,
    val authErrorMessage: String? = null,
    val showEditProfileDialog: Boolean = false,
    val showVerificationRequestDialog: Boolean = false,
    val showNotificationsModal: Boolean = false,
    val showSafetyGuideModal: Boolean = false,
    val showNativeInfoDialog: Boolean = false,
    val showInterstitialAd: SponsoredAdItem? = null,
    val showRewardedAd: SponsoredAdItem? = null,
    val rewardBoostTargetProductId: String? = null
) {
    val currentUser: PublicUser?
        get() = auth.publicProfile

    val directoryUsers: List<PublicUser>
        get() = MarketplaceLogic.mergeUsersAndCommercialProfiles(allUsers, commercialProfiles, currentUser)

    val isSignedIn: Boolean
        get() = auth.firebaseUser != null

    val isOnboardingRequired: Boolean
        get() = auth.firebaseUser != null && (auth.needsProfileOnboarding || forceOnboardingStep)

    val isAdmin: Boolean
        get() = auth.isAdmin

    val isCurrentlySuspended: Boolean
        get() = currentUser?.let {
            MarketplaceLogic.isUserSuspended(it.status, it.suspendedUntil, it.suspendedUntilTs)
        } ?: false

    val filteredMarketProducts: List<Product>
        get() = MarketplaceLogic.filterProducts(products, filter, activeOnly = true)

    val swipeDeckProducts: List<Product>
        get() {
            val baseFilter = MarketFilterState(selectedCity = filter.selectedCity)
            return MarketplaceLogic.filterProducts(products, baseFilter, activeOnly = true)
                .filterNot { passedProductIds.contains(it.id) }
        }

    val favoriteProducts: List<Product>
        get() = products.filter { favoriteIds.contains(it.id) }

    val unreadChatsCount: Int
        get() {
            val uid = auth.firebaseUser?.uid ?: return 0
            return conversations.sumOf { conv ->
                if (conv.buyerId == uid) conv.unreadByBuyer else conv.unreadBySeller
            }
        }

    val unreadNotificationsCount: Int
        get() = notifications.count { !it.read && !readBroadcastIds.contains(it.id) }

    val pendingReportsCount: Int
        get() = reports.count { it.status == "pending" }

    val pendingVerificationRequestsCount: Int
        get() = verificationRequests.count { it.status == "pending" }

    val myLatestVerificationRequest: VerificationRequest?
        get() {
            val uid = auth.firebaseUser?.uid ?: return null
            return verificationRequests.firstOrNull { it.uid == uid }
        }

    val flaggedProductsCount: Int
        get() = products.count { it.status == ProductStatus.FLAGGED.wireValue }

    val adStats: AdRevenueStats
        get() = MarketplaceLogic.computeRevenueStats(sponsoredAds, adConfig)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HatchMainViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val productRepo: ProductRepository,
    private val interactionRepo: InteractionRepository,
    private val chatRepo: ChatRepository,
    private val monetizationRepo: MonetizationRepository,
    private val prefs: HatchPreferences,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _localState = MutableStateFlow(HatchUiState())
    private val _activeConversationId = MutableStateFlow<String?>(null)
    private val _systemLanguage = MutableStateFlow(
        if (Locale.getDefault().language.equals("ar", ignoreCase = true)) AppLanguage.AR else AppLanguage.EN
    )
    private var productViewsBeforeAd = 0

    private val authSessionFlow = authRepo.observeAuthSession()

    private val productsFlow = authSessionFlow.flatMapLatest { session ->
        productRepo.observeProducts(session.firebaseUser?.uid, session.hasAdminClaim)
    }

    private val favoritesFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeFavoriteIds(session.firebaseUser?.uid)
    }

    private val conversationsFlow = authSessionFlow.flatMapLatest { session ->
        chatRepo.observeConversations(session.firebaseUser?.uid, session.hasAdminClaim)
    }

    private val activeMessagesFlow = _activeConversationId.flatMapLatest { convId ->
        chatRepo.observeMessages(convId)
    }

    private val notificationsFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeNotifications(session.firebaseUser?.uid)
    }

    private val reviewsFlow = interactionRepo.observeReviews()

    private val reportsFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeReports(session.firebaseUser?.uid, session.hasAdminClaim)
    }

    private val verificationRequestsFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeVerificationRequests(session.firebaseUser?.uid, session.hasAdminClaim)
    }

    private val creditTransactionsFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeCreditTransactions(session.firebaseUser?.uid, session.hasAdminClaim)
    }

    private val usersFlow = authSessionFlow.flatMapLatest { session ->
        authRepo.observePublicUsers(session.firebaseUser != null, session.hasAdminClaim)
    }

    private val commercialProfilesFlow = authSessionFlow.flatMapLatest { session ->
        authRepo.observeCommercialProfiles(session.hasAdminClaim)
    }

    private val sponsoredAdsFlow = authSessionFlow.flatMapLatest { session ->
        monetizationRepo.observeSponsoredAds(session.hasAdminClaim)
    }

    private val adConfigFlow = authSessionFlow.flatMapLatest { session ->
        monetizationRepo.observeAdConfig(session.firebaseUser != null)
    }

    private data class CoreStreams(
        val auth: AuthSessionState,
        val products: List<Product>,
        val favorites: Set<String>,
        val conversations: List<Conversation>,
        val activeMessages: List<ChatMessage>
    )

    private data class LedgerStreams(
        val verificationRequests: List<VerificationRequest>,
        val creditTransactions: List<CreditTransaction>
    )

    private data class MetaStreams(
        val notifications: List<NotificationItem>,
        val reviews: List<UserReview>,
        val reports: List<ReportTicket>,
        val usersAndCommercial: Pair<List<PublicUser>, List<PublicUser>>,
        val ads: List<SponsoredAdItem>,
        val ledger: LedgerStreams
    )

    private data class SettingsStreams(
        val adConfig: AdMobConfig,
        val language: AppLanguage,
        val selectedCity: String,
        val searchHistory: List<String>,
        val isOnline: Boolean
    )

    private val coreCombined = combine(
        authSessionFlow,
        productsFlow,
        favoritesFlow,
        conversationsFlow,
        activeMessagesFlow
    ) { auth, products, favs, convs, msgs ->
        CoreStreams(auth, products, favs, convs, msgs)
    }

    private val usersCombinedFlow = combine(
        usersFlow,
        commercialProfilesFlow
    ) { users, commercial ->
        users to commercial
    }

    private val ledgerCombinedFlow = combine(
        verificationRequestsFlow,
        creditTransactionsFlow
    ) { vReqs, creditTxs ->
        LedgerStreams(vReqs, creditTxs)
    }

    private val adsAndLedgerFlow = combine(
        sponsoredAdsFlow,
        ledgerCombinedFlow
    ) { ads, ledger ->
        ads to ledger
    }

    private val metaCombined = combine(
        notificationsFlow,
        reviewsFlow,
        reportsFlow,
        usersCombinedFlow,
        adsAndLedgerFlow
    ) { notifs, reviews, reports, usersPair, adsPair ->
        MetaStreams(notifs, reviews, reports, usersPair, adsPair.first, adsPair.second)
    }

    private val effectiveLanguageFlow = combine(
        prefs.languageOverrideFlow,
        _systemLanguage
    ) { manual, system ->
        manual ?: system
    }

    private val settingsCombined = combine(
        adConfigFlow,
        effectiveLanguageFlow,
        prefs.selectedCityFlow,
        prefs.searchHistoryFlow,
        networkMonitor.isOnlineFlow
    ) { adCfg, lang, city, history, online ->
        SettingsStreams(adCfg, lang, city, history, online)
    }

    val uiState: StateFlow<HatchUiState> = combine(
        _localState,
        coreCombined,
        metaCombined,
        settingsCombined
    ) { local, core, meta, settings ->
        val (publicUsers, commercialProfiles) = meta.usersAndCommercial
        val mergedDirectoryUsers = MarketplaceLogic.mergeUsersAndCommercialProfiles(
            publicUsers = publicUsers,
            commercialProfiles = commercialProfiles,
            currentUser = core.auth.publicProfile
        )
        val reviewsBySeller = meta.reviews.groupBy { it.sellerId }
        val usersById = mergedDirectoryUsers.associateBy { it.uid }
        val enrichedProducts = core.products.map { p ->
            val sellerReviews = reviewsBySeller[p.sellerId].orEmpty()
            val liveSeller = usersById[p.sellerId]
            val avgRating = if (sellerReviews.isNotEmpty()) {
                sellerReviews.map { it.rating }.average()
            } else {
                p.sellerRating
            }
            if (liveSeller != null) {
                p.copy(
                    sellerRating = avgRating,
                    sellerName = liveSeller.name.ifBlank { p.sellerName },
                    sellerAvatar = liveSeller.displayLogoOrAvatar().ifBlank { p.sellerAvatar },
                    sellerAccountType = liveSeller.accountType,
                    sellerBusinessActivity = liveSeller.businessActivity ?: p.sellerBusinessActivity,
                    sellerDealsCount = maxOf(p.sellerDealsCount, liveSeller.dealsCompleted),
                    sellerVerified = liveSeller.verifiedIdentity
                )
            } else if (sellerReviews.isNotEmpty()) {
                p.copy(sellerRating = avgRating)
            } else {
                p
            }
        }

        local.copy(
            language = settings.language,
            isOnline = settings.isOnline,
            firebaseConnected = core.auth.isFirebaseConnected,
            auth = core.auth,
            products = enrichedProducts,
            favoriteIds = core.favorites,
            conversations = core.conversations,
            activeMessages = core.activeMessages,
            notifications = meta.notifications,
            reviews = meta.reviews,
            reports = meta.reports,
            allUsers = publicUsers,
            commercialProfiles = commercialProfiles,
            verificationRequests = meta.ledger.verificationRequests,
            creditTransactions = meta.ledger.creditTransactions,
            sponsoredAds = meta.ads,
            adConfig = settings.adConfig,
            filter = local.filter.copy(selectedCity = settings.selectedCity),
            searchHistory = settings.searchHistory,
            showAuthSheet = local.showAuthSheet || (core.auth.firebaseUser != null && (core.auth.needsProfileOnboarding || local.forceOnboardingStep))
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HatchUiState()
    )

    init {
        viewModelScope.launch {
            delay(1800)
            _localState.update { it.copy(showSplash = false) }
        }
        viewModelScope.launch {
            authRepo.verifyFirestoreServerConnection()
        }
        viewModelScope.launch {
            authSessionFlow
                .distinctUntilChangedBy {
                    listOf(
                        it.publicProfile?.uid,
                        it.publicProfile?.accountType,
                        it.publicProfile?.name,
                        it.publicProfile?.city,
                        it.publicProfile?.businessActivity,
                        it.publicProfile?.businessLogo,
                        it.publicProfile?.avatar,
                        it.publicProfile?.status,
                        it.publicProfile?.dealsCompleted,
                        it.publicProfile?.verifiedIdentity
                    )
                }
                .collectLatest { session ->
                    val profile = session.publicProfile
                    if (session.firebaseUser != null && profile != null) {
                        authRepo.ensureCommercialProfileSynced(profile)
                    }
                }
        }
    }

    fun requestGuestPrompt(reason: GuestPromptReason) {
        _localState.update { it.copy(guestPromptReason = reason) }
    }

    fun dismissGuestPrompt() {
        _localState.update { it.copy(guestPromptReason = null) }
    }

    fun selectTab(tab: MainTab) {
        if ((tab == MainTab.CHAT || tab == MainTab.FAVORITES) && !uiState.value.isSignedIn) {
            val reason = if (tab == MainTab.CHAT) GuestPromptReason.CONTACT_OR_BUY else GuestPromptReason.FAVORITES
            requestGuestPrompt(reason)
            return
        }
        if (tab == MainTab.ADMIN && !uiState.value.isAdmin) {
            return
        }
        _localState.update { it.copy(activeTab = tab) }
        if (tab == MainTab.ADMIN) {
            refreshCloudinaryQuota()
        }
    }

    fun selectProfileSubTab(subTab: ProfileSubTab) {
        _localState.update { it.copy(profileSubTab = subTab) }
    }

    fun selectAdminSubTab(subTab: AdminSubTab) {
        _localState.update { it.copy(adminSubTab = subTab) }
        if (subTab == AdminSubTab.MONETIZATION) {
            refreshCloudinaryQuota()
        }
    }

    fun updateSystemLocale(languageCode: String) {
        val next = if (languageCode.startsWith("ar", ignoreCase = true)) AppLanguage.AR else AppLanguage.EN
        if (_systemLanguage.value != next) {
            _systemLanguage.value = next
        }
    }

    fun toggleLanguage() {
        val next = if (uiState.value.language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
        viewModelScope.launch { prefs.setLanguage(next) }
    }

    fun selectCity(city: String) {
        viewModelScope.launch { prefs.setSelectedCity(city) }
    }

    fun updateFilter(transform: (MarketFilterState) -> MarketFilterState) {
        _localState.update { it.copy(filter = transform(it.filter)) }
    }

    fun commitSearchToHistory(query: String) {
        viewModelScope.launch { prefs.addSearchHistory(query) }
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch { prefs.removeSearchHistoryItem(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun triggerPullToRefresh() {
        viewModelScope.launch {
            _localState.update { it.copy(isRefreshing = true) }
            authRepo.verifyFirestoreServerConnection()
            if (uiState.value.isAdmin) refreshCloudinaryQuota()
            delay(500)
            _localState.update { it.copy(isRefreshing = false) }
        }
    }

    fun openAuthSheet() {
        _localState.update { it.copy(showAuthSheet = true, authErrorMessage = null) }
    }

    fun closeAuthSheet() {
        if (uiState.value.isOnboardingRequired) return
        _localState.update { it.copy(showAuthSheet = false, authErrorMessage = null) }
    }

    fun reportAuthError(message: String) {
        android.util.Log.w("HatchAuthFlow", "Auth UI error reported")
        _localState.update { it.copy(authErrorMessage = message) }
        showToast(message, ToastType.ERROR)
    }

    fun clearAuthError() {
        _localState.update { it.copy(authErrorMessage = null) }
    }

    fun signInWithGoogleCredential(credential: AuthCredential) {
        viewModelScope.launch {
            setSubmitting(true)
            clearAuthError()
            authRepo.signInWithGoogleAndCheckProfile(credential)
                .onSuccess { outcome ->
                    if (outcome.needsOnboarding) {
                        android.util.Log.d("HatchAuthFlow", "Routing authenticated Google user to onboarding profile form")
                        _localState.update {
                            it.copy(
                                showAuthSheet = true,
                                forceOnboardingStep = true,
                                authErrorMessage = null
                            )
                        }
                        showToast(
                            if (isArabic()) "تم تسجيل الدخول — أكمل ملفك الشخصي للمتابعة" else "Signed in — complete your profile to continue",
                            ToastType.INFO
                        )
                    } else {
                        android.util.Log.d("HatchAuthFlow", "Existing user profile complete — navigating to Home")
                        prefs.setOnboardingCompleted(true)
                        _localState.update {
                            it.copy(
                                forceOnboardingStep = false,
                                showAuthSheet = false,
                                authErrorMessage = null,
                                activeTab = MainTab.SWIPE
                            )
                        }
                        showToast(
                            if (isArabic()) "مرحباً بعودتك! تم تسجيل الدخول بنجاح" else "Welcome back! Signed in successfully",
                            ToastType.SUCCESS
                        )
                    }
                }
                .onFailure { e ->
                    val errText = e.localizedMessage ?: if (isArabic()) "فشل تسجيل الدخول عبر Google" else "Google Sign-In failed"
                    android.util.Log.e("HatchAuthFlow", "signInWithGoogleCredential failed: ${e.javaClass.simpleName}")
                    reportAuthError(errText)
                }
            setSubmitting(false)
        }
    }

    fun saveOnboardingProfile(
        name: String,
        phone: String,
        city: String,
        avatar: String,
        avatarUri: Uri? = null,
        accountType: String = AccountType.PERSONAL.wireValue,
        businessActivity: String = "",
        businessLogo: String = ""
    ) {
        val fbUser = uiState.value.auth.firebaseUser ?: authRepo.currentFirebaseUser()
        if (fbUser == null) {
            val msg = if (isArabic()) "انتهت جلسة تسجيل الدخول، يرجى تسجيل الدخول مرة أخرى" else "Auth session lost, please sign in again"
            android.util.Log.w("HatchAuthFlow", "saveOnboardingProfile aborted: no active FirebaseUser")
            _localState.update { it.copy(forceOnboardingStep = false, authErrorMessage = msg) }
            showToast(msg, ToastType.ERROR)
            return
        }
        viewModelScope.launch {
            setSubmitting(true)
            clearAuthError()
            val resolvedAvatar = if (avatarUri != null) {
                productRepo.uploadSingleInspectionOrAvatarImage(avatarUri).getOrElse { e ->
                    val msg = e.localizedMessage ?: if (isArabic()) "فشل رفع الصورة" else "Avatar upload failed"
                    reportAuthError(msg)
                    setSubmitting(false)
                    return@launch
                }
            } else {
                avatar
            }
            val resolvedType = AccountType.fromWire(accountType)
            val resolvedLogo = if (resolvedType.isCommercial()) {
                if (avatarUri != null) resolvedAvatar else businessLogo.trim().ifBlank { resolvedAvatar }
            } else {
                ""
            }
            authRepo.saveOnboardingProfile(
                uid = fbUser.uid,
                email = fbUser.email.orEmpty(),
                name = name,
                phone = phone,
                city = city,
                avatar = resolvedAvatar,
                accountType = resolvedType.wireValue,
                businessActivity = businessActivity,
                businessLogo = resolvedLogo
            )
                .onSuccess {
                    android.util.Log.d("HatchAuthFlow", "Onboarding profile saved — navigating to Home")
                    prefs.setOnboardingCompleted(true)
                    _localState.update {
                        it.copy(
                            forceOnboardingStep = false,
                            showAuthSheet = false,
                            authErrorMessage = null,
                            activeTab = MainTab.SWIPE
                        )
                    }
                    showToast(if (isArabic()) "تم حفظ ملفك التجاري بنجاح" else "Profile saved", ToastType.SUCCESS)
                }
                .onFailure { e ->
                    val msg = e.localizedMessage ?: if (isArabic()) "تعذر حفظ البيانات" else "Profile save failed"
                    android.util.Log.e("HatchAuthFlow", "saveOnboardingProfile failed: ${e.javaClass.simpleName}")
                    reportAuthError(msg)
                }
            setSubmitting(false)
        }
    }

    fun updateProfile(
        name: String,
        phone: String,
        city: String,
        avatar: String,
        avatarUri: Uri? = null,
        accountType: String = AccountType.PERSONAL.wireValue,
        businessActivity: String = "",
        businessLogo: String = ""
    ) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.GENERAL)) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        val previousType = AccountType.fromWire(uiState.value.currentUser?.accountType)
        val nextType = AccountType.fromWire(accountType)
        viewModelScope.launch {
            setSubmitting(true)
            val resolvedAvatar = if (avatarUri != null) {
                productRepo.uploadSingleInspectionOrAvatarImage(avatarUri).getOrElse { e ->
                    showToast(e.localizedMessage ?: "Avatar upload failed", ToastType.ERROR)
                    setSubmitting(false)
                    return@launch
                }
            } else {
                avatar
            }
            val resolvedLogo = if (nextType.isCommercial()) {
                if (avatarUri != null) resolvedAvatar else businessLogo.trim().ifBlank { resolvedAvatar }
            } else {
                ""
            }
            authRepo.updateProfile(
                uid = uid,
                name = name,
                phone = phone,
                city = city,
                avatar = resolvedAvatar,
                accountType = nextType.wireValue,
                businessActivity = businessActivity,
                businessLogo = resolvedLogo
            )
                .onSuccess {
                    _localState.update { it.copy(showEditProfileDialog = false) }
                    val msg = if (previousType == AccountType.PERSONAL && nextType.isCommercial()) {
                        if (isArabic()) "تمت ترقية حسابك بنجاح 🎉" else "Account upgraded successfully 🎉"
                    } else {
                        if (isArabic()) "تم تحديث البيانات" else "Profile updated"
                    }
                    showToast(msg, ToastType.SUCCESS)
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Update failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun signOut() {
        authRepo.signOut()
        _activeConversationId.value = null
        _localState.update {
            it.copy(
                activeTab = MainTab.SWIPE,
                activeConversationId = null,
                selectedBusinessSellerId = null,
                commercialDirectoryType = null,
                guestPromptReason = null,
                showEditProfileDialog = false,
                forceOnboardingStep = false,
                authErrorMessage = null
            )
        }
        showToast(if (isArabic()) "تم تسجيل الخروج" else "Signed out", ToastType.INFO)
    }

    fun openProductDetail(product: Product) {
        productViewsBeforeAd++
        _localState.update { it.copy(selectedProduct = product) }
        viewModelScope.launch { productRepo.incrementViews(product.id) }

        val config = uiState.value.adConfig
        if (config.isEnabled && productViewsBeforeAd >= 7) {
            val interstitial = uiState.value.sponsoredAds.firstOrNull {
                it.isActive && (it.format == "interstitial" || it.format == "all")
            }
            if (interstitial != null) {
                productViewsBeforeAd = 0
                _localState.update { it.copy(showInterstitialAd = interstitial) }
                recordAdImpression(interstitial.id)
            }
        }
    }

    fun closeProductDetail() { _localState.update { it.copy(selectedProduct = null) } }

    fun openBusinessProfile(sellerId: String) {
        _localState.update { it.copy(selectedBusinessSellerId = sellerId) }
    }

    fun closeBusinessProfile() {
        _localState.update { it.copy(selectedBusinessSellerId = null) }
    }

    fun openCommercialDirectory(type: AccountType) {
        _localState.update { it.copy(commercialDirectoryType = type) }
    }

    fun closeCommercialDirectory() {
        _localState.update { it.copy(commercialDirectoryType = null) }
    }

    fun browseNextSwipeCard(product: Product) {
        _localState.update { it.copy(passedProductIds = it.passedProductIds + product.id) }
        if (uiState.value.isCurrentlySuspended) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch { interactionRepo.recordSwipe(uid, product.id, "right") }
    }

    fun swipeLeft(product: Product) {
        _localState.update { it.copy(passedProductIds = it.passedProductIds + product.id) }
        if (uiState.value.isCurrentlySuspended) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch { interactionRepo.recordSwipe(uid, product.id, "left") }
    }

    fun swipeRight(product: Product) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.FAVORITES)) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        _localState.update { it.copy(passedProductIds = it.passedProductIds + product.id) }
        viewModelScope.launch {
            interactionRepo.recordSwipe(uid, product.id, "right")
            if (!uiState.value.favoriteIds.contains(product.id)) {
                interactionRepo.toggleFavorite(uid, product.id, currentlyFavorite = false)
            }
            showToast(if (isArabic()) "تم الحفظ في المفضلة ❤️" else "Saved to favorites ❤️", ToastType.SUCCESS)
        }
    }

    fun undoLastSwipe() {
        val last = uiState.value.passedProductIds.lastOrNull() ?: return
        _localState.update { it.copy(passedProductIds = it.passedProductIds - last) }
    }

    fun resetSwipeDeck() { _localState.update { it.copy(passedProductIds = emptySet()) } }

    fun toggleFavorite(productId: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.FAVORITES)) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        val currentlyFav = uiState.value.favoriteIds.contains(productId)
        viewModelScope.launch {
            interactionRepo.toggleFavorite(uid, productId, currentlyFav)
                .onFailure { e -> showToast(e.localizedMessage ?: "Action failed", ToastType.ERROR) }
        }
    }

    fun openSellModal() {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        _localState.update { it.copy(showSellModal = true) }
    }

    fun closeSellModal() { _localState.update { it.copy(showSellModal = false) } }

    fun publishNewProduct(
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
        localUris: List<Uri>,
        remoteUrls: List<String>,
        paymentMethods: List<String>,
        negotiable: Boolean,
        deliveryAvailable: Boolean,
        urgentSale: Boolean
    ) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        val seller = uiState.value.currentUser ?: return
        viewModelScope.launch {
            setSubmitting(true)
            productRepo.createProduct(
                seller = seller,
                titleAr = titleAr,
                titleEn = titleEn,
                descriptionAr = descriptionAr,
                descriptionEn = descriptionEn,
                price = price,
                originalPrice = originalPrice,
                category = category,
                condition = condition,
                city = city,
                areaAr = areaAr,
                areaEn = areaAr,
                localImageUris = localUris,
                remoteImageUrls = remoteUrls,
                paymentMethods = paymentMethods,
                negotiable = negotiable,
                deliveryAvailable = deliveryAvailable,
                urgentSale = urgentSale
            ).onSuccess {
                _localState.update { it.copy(showSellModal = false, activeTab = MainTab.BROWSE) }
                showToast(if (isArabic()) "تم نشر إعلانك بنجاح! 🎉" else "Listing published! 🎉", ToastType.SUCCESS)
            }.onFailure { e ->
                showToast(e.localizedMessage ?: "Failed to publish listing", ToastType.ERROR)
            }
            setSubmitting(false)
        }
    }

    fun openEditProduct(product: Product) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        _localState.update { it.copy(editingProduct = product) }
    }
    fun closeEditProduct() { _localState.update { it.copy(editingProduct = null) } }

    fun saveEditedProduct(
        productId: String,
        titleAr: String,
        descriptionAr: String,
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
    ) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        viewModelScope.launch {
            setSubmitting(true)
            productRepo.updateProduct(
                productId = productId,
                titleAr = titleAr,
                titleEn = titleAr,
                descriptionAr = descriptionAr,
                descriptionEn = descriptionAr,
                price = price,
                originalPrice = originalPrice,
                category = category,
                condition = condition,
                city = city,
                areaAr = areaAr,
                images = images,
                paymentMethods = paymentMethods,
                negotiable = negotiable,
                deliveryAvailable = deliveryAvailable,
                urgentSale = urgentSale
            ).onSuccess {
                _localState.update { it.copy(editingProduct = null) }
                showToast(if (isArabic()) "تم حفظ تعديلات الإعلان" else "Listing updated", ToastType.SUCCESS)
            }.onFailure { e ->
                showToast(e.localizedMessage ?: "Update failed", ToastType.ERROR)
            }
            setSubmitting(false)
        }
    }

    fun requestMarkSold(product: Product) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        _localState.update { it.copy(soldTargetProduct = product) }
    }
    fun closeMarkSold() { _localState.update { it.copy(soldTargetProduct = null) } }

    fun confirmMarkSold() {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        val target = uiState.value.soldTargetProduct ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            setSubmitting(true)
            productRepo.markProductAsSold(target.id, uid)
                .onSuccess {
                    _localState.update { it.copy(soldTargetProduct = null, selectedProduct = null) }
                    showToast(
                        if (isArabic()) "تم أرشفة الإعلان كمباع (+1 صفقة ناجحة)" else "Marked as Sold (+1 deal)",
                        ToastType.SUCCESS
                    )
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun openOfferModal(product: Product) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.OFFER_NEGOTIATE)) return
        if (product.sellerId == uiState.value.auth.firebaseUser?.uid) {
            showToast(if (isArabic()) "لا يمكنك تقديم عرض على منتجك" else "Cannot bid on your own listing", ToastType.WARNING)
            return
        }
        _localState.update { it.copy(offerTargetProduct = product) }
    }

    fun closeOfferModal() { _localState.update { it.copy(offerTargetProduct = null) } }

    fun submitOffer(product: Product, amount: Double, note: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.OFFER_NEGOTIATE)) return
        val buyer = uiState.value.currentUser ?: return
        viewModelScope.launch {
            setSubmitting(true)
            chatRepo.startOrGetConversation(product, buyer)
                .onSuccess { convId ->
                    val tempConv = uiState.value.conversations.firstOrNull { it.id == convId }
                        ?: Conversation(
                            id = convId,
                            productId = product.id,
                            buyerId = buyer.uid,
                            sellerId = product.sellerId,
                            participantIds = listOf(buyer.uid, product.sellerId)
                        )
                    chatRepo.sendOffer(tempConv, buyer.uid, amount, note)
                        .onSuccess {
                            _activeConversationId.value = convId
                            _localState.update {
                                it.copy(
                                    offerTargetProduct = null,
                                    selectedProduct = null,
                                    activeConversationId = convId,
                                    activeTab = MainTab.CHAT
                                )
                            }
                            showToast(if (isArabic()) "تم إرسال عرضك للبائع" else "Offer sent to seller", ToastType.SUCCESS)
                        }
                        .onFailure { e -> showToast(e.localizedMessage ?: "Offer failed", ToastType.ERROR) }
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Chat start failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun startChatWithSeller(product: Product) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.CONTACT_OR_BUY)) return
        val buyer = uiState.value.currentUser ?: return
        if (buyer.uid == product.sellerId) {
            showToast(if (isArabic()) "هذا هو إعلانك الخاص" else "This is your own listing", ToastType.INFO)
            return
        }
        viewModelScope.launch {
            chatRepo.startOrGetConversation(product, buyer)
                .onSuccess { convId ->
                    _activeConversationId.value = convId
                    _localState.update {
                        it.copy(
                            selectedProduct = null,
                            activeConversationId = convId,
                            activeTab = MainTab.CHAT
                        )
                    }
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Chat error", ToastType.ERROR) }
        }
    }

    fun selectConversation(conv: Conversation?) {
        _activeConversationId.value = conv?.id
        _localState.update { it.copy(activeConversationId = conv?.id) }
        if (uiState.value.isCurrentlySuspended) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        if (conv != null) {
            viewModelScope.launch {
                chatRepo.markConversationRead(conv, uid, uiState.value.activeMessages)
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.CONTACT_OR_BUY)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepo.sendTextMessage(conv, uid, text)
                .onFailure { e -> showToast(e.localizedMessage ?: "Send failed", ToastType.ERROR) }
        }
    }

    fun sendInspectionPhoto(uri: Uri?, directUrl: String?, tag: String, caption: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.CONTACT_OR_BUY)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            setSubmitting(true)
            val resolvedUrl = when {
                !directUrl.isNullOrBlank() -> Result.success(directUrl)
                uri != null -> productRepo.uploadSingleInspectionOrAvatarImage(uri)
                else -> Result.failure(IllegalArgumentException("No image selected"))
            }
            resolvedUrl.onSuccess { url ->
                chatRepo.sendInspectionImageMessage(conv, uid, url, tag, caption)
            }.onFailure { e ->
                showToast(e.localizedMessage ?: "Photo upload failed", ToastType.ERROR)
            }
            setSubmitting(false)
        }
    }

    fun sendVoiceNote(audioUrl: String, durationSeconds: Int) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.CONTACT_OR_BUY)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepo.sendVoiceMessage(conv, uid, audioUrl, durationSeconds)
                .onFailure { e -> showToast(e.localizedMessage ?: "Voice note failed", ToastType.ERROR) }
        }
    }

    fun respondToOffer(status: OfferStatus, counterAmount: Double? = null) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.OFFER_NEGOTIATE)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepo.respondToOffer(conv, uid, status, counterAmount)
                .onSuccess { showToast(if (isArabic()) "تم تحديث حالة العرض" else "Offer updated", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Action failed", ToastType.ERROR) }
        }
    }

    fun openReviewModal(request: ReviewRequest) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.RATE_OR_REPORT)) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        if (request.sellerId == uid) {
            showToast(if (isArabic()) "لا يمكنك تقييم حسابك الشخصي" else "You cannot review your own account", ToastType.WARNING)
            return
        }
        val alreadyReviewedSpecific = request.productId != null &&
            MarketplaceLogic.hasBuyerAlreadyReviewedProduct(uiState.value.reviews, uid, request.productId)
        if (alreadyReviewedSpecific) {
            showToast(
                if (isArabic()) "لقد قمت بتقييم هذه الصفقة مسبقاً" else "You have already reviewed this completed deal",
                ToastType.INFO
            )
            return
        }
        val completedConv = MarketplaceLogic.findEligibleReviewConversation(
            conversations = uiState.value.conversations,
            products = uiState.value.products,
            reviews = uiState.value.reviews,
            currentUserId = uid,
            sellerId = request.sellerId,
            productId = request.productId,
            isSuspended = uiState.value.isCurrentlySuspended
        )
        if (completedConv == null) {
            showToast(
                if (isArabic()) {
                    "يتاح التقييم بعد قبول عرضك وإتمام بيع المنتج عبر المحادثة"
                } else {
                    "Rating unlocks after your offer is accepted and the sale is marked sold"
                },
                ToastType.INFO
            )
            return
        }
        _localState.update {
            it.copy(
                reviewRequest = request.copy(
                    productId = completedConv.productId,
                    conversationId = completedConv.id,
                    productTitle = request.productTitle ?: completedConv.productTitle
                )
            )
        }
    }

    fun closeReviewModal() { _localState.update { it.copy(reviewRequest = null) } }

    fun submitReview(rating: Int, comment: String, tags: List<String>) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.RATE_OR_REPORT)) return
        val req = uiState.value.reviewRequest ?: return
        val reviewer = uiState.value.currentUser ?: return
        viewModelScope.launch {
            setSubmitting(true)
            interactionRepo.submitReview(
                sellerId = req.sellerId,
                reviewerId = reviewer.uid,
                reviewerName = reviewer.name,
                reviewerAvatar = reviewer.avatar,
                rating = rating,
                comment = comment,
                tags = tags,
                productId = req.productId,
                conversationId = req.conversationId,
                productTitle = req.productTitle
            ).onSuccess {
                _localState.update { it.copy(reviewRequest = null) }
                showToast(if (isArabic()) "شكراً لتقييمك الصادق ⭐" else "Review submitted ⭐", ToastType.SUCCESS)
            }.onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun openReportModal(request: ReportRequest) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.RATE_OR_REPORT)) return
        _localState.update { it.copy(reportRequest = request) }
    }

    fun closeReportModal() { _localState.update { it.copy(reportRequest = null) } }

    fun submitReport(reason: String, message: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.RATE_OR_REPORT)) return
        val req = uiState.value.reportRequest ?: return
        val reporter = uiState.value.currentUser ?: return
        viewModelScope.launch {
            setSubmitting(true)
            interactionRepo.submitReport(
                reporterId = reporter.uid,
                reporterName = reporter.name,
                reportedUserId = req.userId,
                reportedUserName = req.userName,
                reportedProductId = req.productId,
                reportedProductTitle = req.productTitle,
                reason = reason,
                message = message
            ).onSuccess {
                _localState.update { it.copy(reportRequest = null) }
                showToast(if (isArabic()) "تم إرسال البلاغ للإدارة بنجاح" else "Report sent to Admin", ToastType.SUCCESS)
            }.onFailure { e -> showToast(e.localizedMessage ?: "Report failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun openNotifications() {
        if (!uiState.value.isSignedIn) {
            requestGuestPrompt(GuestPromptReason.GENERAL)
            return
        }
        _localState.update { it.copy(showNotificationsModal = true) }
    }
    fun closeNotifications() { _localState.update { it.copy(showNotificationsModal = false) } }

    fun markAllNotificationsRead() {
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        if (uiState.value.isCurrentlySuspended) return
        val allIds = uiState.value.notifications.map { it.id }.toSet()
        _localState.update { it.copy(readBroadcastIds = it.readBroadcastIds + allIds) }
        viewModelScope.launch {
            uiState.value.notifications.filter { !it.read }.forEach { notif ->
                interactionRepo.markPersonalNotificationRead(uid, notif.id)
            }
        }
    }

    fun openSafetyGuide() { _localState.update { it.copy(showSafetyGuideModal = true) } }
    fun closeSafetyGuide() { _localState.update { it.copy(showSafetyGuideModal = false) } }
    fun openNativeInfoDialog() { _localState.update { it.copy(showNativeInfoDialog = true) } }
    fun closeNativeInfoDialog() { _localState.update { it.copy(showNativeInfoDialog = false) } }
    fun openEditProfile() {
        if (!requireActiveUserOrPrompt(GuestPromptReason.GENERAL)) return
        _localState.update { it.copy(showEditProfileDialog = true) }
    }
    fun closeEditProfile() { _localState.update { it.copy(showEditProfileDialog = false) } }

    fun openVerificationRequestDialog() {
        if (!requireActiveUserOrPrompt(GuestPromptReason.GENERAL)) return
        _localState.update { it.copy(showVerificationRequestDialog = true) }
    }
    fun closeVerificationRequestDialog() { _localState.update { it.copy(showVerificationRequestDialog = false) } }

    fun submitVerificationRequest(note: String, documentUrl: String? = null) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.GENERAL)) return
        val user = uiState.value.currentUser ?: return
        viewModelScope.launch {
            setSubmitting(true)
            interactionRepo.submitVerificationRequest(user, note, documentUrl)
                .onSuccess {
                    _localState.update { it.copy(showVerificationRequestDialog = false) }
                    showToast(
                        if (isArabic()) "تم إرسال طلب التوثيق الذهبي للمراجعة ✅" else "Gold verification request submitted ✅",
                        ToastType.SUCCESS
                    )
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Verification request failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun launchRewardedAdForCredit(targetProductId: String? = null) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.GENERAL)) return
        val ad = uiState.value.sponsoredAds.firstOrNull {
            it.isActive && (it.format == "rewarded" || it.format == "all")
        } ?: uiState.value.sponsoredAds.firstOrNull { it.isActive }
        ?: SponsoredAdItem(
            id = "hatch_default_reward",
            titleAr = "شركاء هتش الموثوقون",
            titleEn = "HATCH Trusted Partners",
            advertiser = "HATCH Sudan",
            descriptionAr = "شاهد الإعلان الترويجي — تُمنح أرصدة التمييز رسمياً من إدارة هتش",
            descriptionEn = "Sponsored showcase — boost credits are granted officially by HATCH Admin",
            durationSeconds = uiState.value.adConfig.rewardedDurationSeconds
        )
        _localState.update { it.copy(showRewardedAd = ad, rewardBoostTargetProductId = targetProductId) }
        if (ad.id != "hatch_default_reward") recordAdImpression(ad.id)
    }

    fun completeRewardedAd() {
        val boostProductId = uiState.value.rewardBoostTargetProductId
        val credits = uiState.value.auth.privateProfile?.rewardCredits ?: 0
        _localState.update { it.copy(showRewardedAd = null, rewardBoostTargetProductId = null) }
        if (boostProductId != null && credits >= 1) {
            boostListingWithExistingCredit(boostProductId)
        } else {
            showToast(
                if (isArabic()) "شكراً للمشاهدة — تُمنح أرصدة التمييز من إدارة هتش" else "Thanks for watching — boost credits are granted by HATCH Admin",
                ToastType.INFO
            )
        }
    }

    fun dismissRewardedAd() { _localState.update { it.copy(showRewardedAd = null, rewardBoostTargetProductId = null) } }

    fun boostListingWithExistingCredit(productId: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            productRepo.featureProductWithCredit(productId, uid)
                .onSuccess { showToast(if (isArabic()) "تم تمييز الإعلان في صدارة السوق ⭐" else "Listing boosted ⭐", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Boost failed", ToastType.ERROR) }
        }
    }

    fun dismissInterstitialAd() { _localState.update { it.copy(showInterstitialAd = null) } }
    fun testShowInterstitial() {
        if (!requireAdminOrDeny()) return
        val ad = uiState.value.sponsoredAds.firstOrNull() ?: return
        _localState.update { it.copy(showInterstitialAd = ad) }
    }

    fun recordAdImpression(adId: String) {
        if (!uiState.value.isSignedIn || uiState.value.isCurrentlySuspended) return
        viewModelScope.launch { monetizationRepo.recordImpression(adId) }
    }
    fun recordAdClick(adId: String) {
        if (!uiState.value.isSignedIn || uiState.value.isCurrentlySuspended) return
        viewModelScope.launch { monetizationRepo.recordClick(adId) }
    }

    fun openAdminConversationAudit(conversation: Conversation) {
        if (!requireAdminOrDeny()) return
        _localState.update { it.copy(adminAuditConversation = conversation) }
        _activeConversationId.value = conversation.id
    }

    fun closeAdminConversationAudit() {
        _localState.update { it.copy(adminAuditConversation = null) }
        _activeConversationId.value = uiState.value.activeConversationId
    }

    fun sendBroadcast(type: String, titleAr: String, titleEn: String, bodyAr: String, bodyEn: String, relatedProductId: String?) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            setSubmitting(true)
            monetizationRepo.sendBroadcastNotification(type, titleAr, titleEn, bodyAr, bodyEn, relatedProductId)
                .onSuccess { showToast(if (isArabic()) "تم إرسال الإشعار الجماعي بنجاح 📣" else "Broadcast sent 📣", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Broadcast failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun updateReportStatus(reportId: String, status: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo.updateReportStatus(reportId, status)
                .onSuccess { showToast(if (isArabic()) "تم تحديث حالة البلاغ" else "Report updated", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
        }
    }

    fun approveFlaggedProduct(productId: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo.approveFlaggedProduct(productId)
                .onSuccess { showToast(if (isArabic()) "تمت إعادة تفعيل الإعلان" else "Listing restored", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
        }
    }

    fun deleteProductAsAdmin(productId: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            productRepo.deleteProduct(productId)
                .onSuccess { showToast(if (isArabic()) "تم حذف الإعلان" else "Listing deleted", ToastType.INFO) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
        }
    }

    fun suspendUser(uid: String, days: Int, reason: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo.suspendUser(uid, days, reason)
                .onSuccess { showToast(if (isArabic()) "تم إيقاف الحساب وإخطار المستخدم" else "User suspended", ToastType.WARNING) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Suspend failed", ToastType.ERROR) }
        }
    }

    fun unsuspendUser(uid: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo.unsuspendUser(uid)
                .onSuccess { showToast(if (isArabic()) "تم فك الحظر عن الحساب" else "User unsuspended", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
        }
    }

    fun approveVerificationRequest(request: VerificationRequest) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.approveVerificationRequest(request)
                .onSuccess { showToast(if (isArabic()) "تم اعتماد التوثيق الذهبي بنجاح ⭐" else "Gold badge approved ⭐", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun rejectVerificationRequest(request: VerificationRequest, reason: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.rejectVerificationRequest(request, reason)
                .onSuccess { showToast(if (isArabic()) "تم رفض طلب التوثيق وإشعار المستخدم" else "Verification rejected", ToastType.INFO) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun setUserVerificationByAdmin(uid: String, verified: Boolean, reason: String = "") {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            authRepo.setUserVerificationStatus(uid, verified, reason)
                .onSuccess {
                    showToast(
                        if (verified) {
                            if (isArabic()) "تم توثيق الحساب بالشارة الذهبية ⭐" else "Gold badge granted ⭐"
                        } else {
                            if (isArabic()) "تمت إزالة شارة التوثيق" else "Verification revoked"
                        },
                        ToastType.SUCCESS
                    )
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
        }
    }

    fun grantRewardCreditAsAdmin(uid: String, amount: Int, reason: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.grantRewardCredit(uid, amount, reason)
                .onSuccess { newBal ->
                    showToast(
                        if (isArabic()) "تم منح +$amount رصيد تمييز (الرصيد الجديد: $newBal)" else "Granted +$amount credits (balance: $newBal)",
                        ToastType.SUCCESS
                    )
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Credit grant failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun deleteUserAsAdmin(uid: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo.deleteUserAsAdmin(uid)
                .onSuccess { showToast(if (isArabic()) "تم حذف بيانات الحساب" else "User removed", ToastType.INFO) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
        }
    }

    fun createSponsoredAd(ad: SponsoredAdItem) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            setSubmitting(true)
            monetizationRepo.createSponsoredAd(ad)
                .onSuccess { showToast(if (isArabic()) "تم إطلاق الحملة الإعلانية" else "Ad launched", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Create ad failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun toggleAdActive(adId: String, active: Boolean) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch { monetizationRepo.toggleAdActive(adId, active) }
    }
    fun updateAdDuration(adId: String, seconds: Int) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch { monetizationRepo.updateAdDuration(adId, seconds) }
    }
    fun deleteAd(adId: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch { monetizationRepo.deleteAd(adId) }
    }

    fun updateAdConfig(config: AdMobConfig) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo.updateAdConfig(config)
                .onSuccess { showToast(if (isArabic()) "تم حفظ إعدادات الإعلانات" else "Ad config saved", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
        }
    }

    fun resetAdStats() {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo.resetAllAdStats(uiState.value.sponsoredAds)
                .onSuccess { showToast(if (isArabic()) "تم تصفير عدادات الإعلانات" else "Ad stats reset", ToastType.INFO) }
        }
    }

    fun refreshCloudinaryQuota() {
        if (!uiState.value.isAdmin) return
        viewModelScope.launch {
            val status = monetizationRepo.fetchAdminQuotaStatus(uiState.value.isAdmin)
            _localState.update { it.copy(cloudinaryQuota = status) }
        }
    }

    private fun requireAdminOrDeny(): Boolean {
        if (!uiState.value.isAdmin) {
            showToast(if (isArabic()) "صلاحية إدارية مطلوبة" else "Admin authorization required", ToastType.ERROR)
            return false
        }
        return true
    }

    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        val toast = UiToast(id = UUID.randomUUID().toString(), message = message, type = type)
        _localState.update { it.copy(toasts = (it.toasts + toast).takeLast(3)) }
        viewModelScope.launch {
            delay(3200)
            dismissToast(toast.id)
        }
    }

    fun dismissToast(id: String) {
        _localState.update { state -> state.copy(toasts = state.toasts.filterNot { it.id == id }) }
    }

    private fun requireActiveUserOrPrompt(reason: GuestPromptReason = GuestPromptReason.GENERAL): Boolean {
        val state = uiState.value
        if (!state.isSignedIn) {
            requestGuestPrompt(reason)
            return false
        }
        if (state.isCurrentlySuspended) {
            showToast(strings().suspendedActionBlocked, ToastType.WARNING)
            return false
        }
        return true
    }

    private fun currentActiveConversation(): Conversation? {
        val id = _activeConversationId.value ?: return null
        return uiState.value.conversations.firstOrNull { it.id == id }
    }

    private fun setSubmitting(value: Boolean) { _localState.update { it.copy(isSubmitting = value) } }
    private fun isArabic(): Boolean = uiState.value.language == AppLanguage.AR
    private fun strings() = com.hatch.marketplace.ui.i18n.HatchStrings.forLanguage(uiState.value.language)
}
