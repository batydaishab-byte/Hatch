package com.hatch.marketplace.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.firestore.FirebaseFirestoreException
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
import com.hatch.marketplace.data.repository.ListingPublishRequest
import com.hatch.marketplace.data.repository.MonetizationRepository
import com.hatch.marketplace.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
    val sellErrorMessage: String? = null,
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
        get() = auth.isSignedIn

    val isOnboardingRequired: Boolean
        get() = auth.isSignedIn && (auth.needsProfileOnboarding || forceOnboardingStep)

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
            val uid = auth.uid ?: return 0
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
            val uid = auth.uid ?: return null
            return verificationRequests.firstOrNull { it.uid == uid }
        }

    val flaggedProductsCount: Int
        get() = products.count { it.status == ProductStatus.FLAGGED.wireValue }

    val adStats: AdRevenueStats
        get() = MarketplaceLogic.computeRevenueStats(sponsoredAds, adConfig)
}

internal data class HatchViewModelStreams(
    val observeProducts: (String?, Boolean) -> Flow<List<Product>> = { _, _ -> flowOf(emptyList()) },
    val observeFavoriteIds: (String?) -> Flow<Set<String>> = { _ -> flowOf(emptySet()) },
    val observeConversations: (String?, Boolean) -> Flow<List<Conversation>> = { _, _ -> flowOf(emptyList()) },
    val observeMessages: (String?) -> Flow<List<ChatMessage>> = { _ -> flowOf(emptyList()) },
    val observeNotifications: (String?) -> Flow<List<NotificationItem>> = { _ -> flowOf(emptyList()) },
    val observeReviews: () -> Flow<List<UserReview>> = { flowOf(emptyList()) },
    val observeReports: (String?, Boolean) -> Flow<List<ReportTicket>> = { _, _ -> flowOf(emptyList()) },
    val observeVerificationRequests: (String?, Boolean) -> Flow<List<VerificationRequest>> = { _, _ -> flowOf(emptyList()) },
    val observeCreditTransactions: (String?, Boolean) -> Flow<List<CreditTransaction>> = { _, _ -> flowOf(emptyList()) },
    val observePublicUsers: (Boolean, Boolean) -> Flow<List<PublicUser>> = { _, _ -> flowOf(emptyList()) },
    val observeCommercialProfiles: (Boolean) -> Flow<List<PublicUser>> = { _ -> flowOf(emptyList()) },
    val observeSponsoredAds: (Boolean) -> Flow<List<SponsoredAdItem>> = { _ -> flowOf(emptyList()) },
    val observeAdConfig: (Boolean) -> Flow<AdMobConfig> = { _ -> flowOf(AdMobConfig()) },
    val languageOverrideFlow: Flow<AppLanguage?> = flowOf(null),
    val selectedCityFlow: Flow<String> = flowOf("all"),
    val searchHistoryFlow: Flow<List<String>> = flowOf(emptyList()),
    val isOnlineFlow: Flow<Boolean> = flowOf(true),
    val fetchAdminQuotaStatus: suspend (Boolean) -> Result<CloudinaryQuotaStatus> = {
        Result.success(CloudinaryQuotaStatus())
    },
    val publishListing: (suspend (ListingPublishRequest) -> Result<String>)? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HatchMainViewModel internal constructor(
    private val authRepo: AuthRepository,
    private val productRepo: ProductRepository?,
    private val interactionRepo: InteractionRepository?,
    private val chatRepo: ChatRepository?,
    private val monetizationRepo: MonetizationRepository?,
    private val prefs: HatchPreferences?,
    private val streams: HatchViewModelStreams
) : ViewModel() {

    @Inject
    constructor(
        authRepo: AuthRepository,
        productRepo: ProductRepository,
        interactionRepo: InteractionRepository,
        chatRepo: ChatRepository,
        monetizationRepo: MonetizationRepository,
        prefs: HatchPreferences,
        networkMonitor: NetworkMonitor
    ) : this(
        authRepo = authRepo,
        productRepo = productRepo,
        interactionRepo = interactionRepo,
        chatRepo = chatRepo,
        monetizationRepo = monetizationRepo,
        prefs = prefs,
        streams = HatchViewModelStreams(
            observeProducts = productRepo::observeProducts,
            observeFavoriteIds = interactionRepo::observeFavoriteIds,
            observeConversations = chatRepo::observeConversations,
            observeMessages = chatRepo::observeMessages,
            observeNotifications = interactionRepo::observeNotifications,
            observeReviews = interactionRepo::observeReviews,
            observeReports = interactionRepo::observeReports,
            observeVerificationRequests = interactionRepo::observeVerificationRequests,
            observeCreditTransactions = interactionRepo::observeCreditTransactions,
            observePublicUsers = authRepo::observePublicUsers,
            observeCommercialProfiles = authRepo::observeCommercialProfiles,
            observeSponsoredAds = monetizationRepo::observeSponsoredAds,
            observeAdConfig = monetizationRepo::observeAdConfig,
            languageOverrideFlow = prefs.languageOverrideFlow,
            selectedCityFlow = prefs.selectedCityFlow,
            searchHistoryFlow = prefs.searchHistoryFlow,
            isOnlineFlow = networkMonitor.isOnlineFlow,
            fetchAdminQuotaStatus = monetizationRepo::fetchAdminQuotaStatus
        )
    )

    private val _localState = MutableStateFlow(HatchUiState())
    private val _activeConversationId = MutableStateFlow<String?>(null)
    private val _systemLanguage = MutableStateFlow(
        if (Locale.getDefault().language.equals("ar", ignoreCase = true)) AppLanguage.AR else AppLanguage.EN
    )
    private var productViewsBeforeAd = 0
    private var lastObservedSessionUid: String? = null
    private var lastObservedIsAdmin: Boolean = false
    private var lastCommercialSyncKey: List<Any?>? = null
    private var commercialSyncJob: Job? = null

    // Single shared AuthSession flow across all ViewModel collectors & repository flatMapLatest streams.
    // Side-effects execute once upstream of shareIn and detach as soon as uiState's WhileSubscribed(5000) stops.
    private val authSessionFlow = authRepo.observeAuthSession()
        .onEach { session ->
            handleSessionSideEffects(session)
        }
        .onCompletion {
            commercialSyncJob?.cancel()
            commercialSyncJob = null
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 0L,
                replayExpirationMillis = 0L
            ),
            replay = 1
        )

    private val productsFlow = authSessionFlow
        .map { it.uid to it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { (uid, hasAdminClaim) ->
            streams.observeProducts(uid, hasAdminClaim)
        }

    private val favoritesFlow = authSessionFlow
        .map { it.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            streams.observeFavoriteIds(uid)
        }

    private val conversationsFlow = authSessionFlow
        .map { it.uid to it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { (uid, hasAdminClaim) ->
            streams.observeConversations(uid, hasAdminClaim)
        }

    private val activeMessagesFlow = _activeConversationId.flatMapLatest { convId ->
        streams.observeMessages(convId)
    }

    private val notificationsFlow = authSessionFlow
        .map { it.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            streams.observeNotifications(uid)
        }

    private val reviewsFlow = streams.observeReviews()

    private val reportsFlow = authSessionFlow
        .map { it.uid to it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { (uid, hasAdminClaim) ->
            streams.observeReports(uid, hasAdminClaim)
        }

    private val verificationRequestsFlow = authSessionFlow
        .map { it.uid to it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { (uid, hasAdminClaim) ->
            streams.observeVerificationRequests(uid, hasAdminClaim)
        }

    private val creditTransactionsFlow = authSessionFlow
        .map { it.uid to it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { (uid, hasAdminClaim) ->
            streams.observeCreditTransactions(uid, hasAdminClaim)
        }

    private val usersFlow = authSessionFlow
        .map { it.uid to it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { (uid, hasAdminClaim) ->
            streams.observePublicUsers(uid != null, hasAdminClaim)
        }

    private val commercialProfilesFlow = authSessionFlow
        .map { it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { hasAdminClaim ->
            streams.observeCommercialProfiles(hasAdminClaim)
        }

    private val sponsoredAdsFlow = authSessionFlow
        .map { it.hasAdminClaim }
        .distinctUntilChanged()
        .flatMapLatest { hasAdminClaim ->
            streams.observeSponsoredAds(hasAdminClaim)
        }

    private val adConfigFlow = authSessionFlow
        .map { it.isSignedIn }
        .distinctUntilChanged()
        .flatMapLatest { isSignedIn ->
            streams.observeAdConfig(isSignedIn)
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
        streams.languageOverrideFlow,
        _systemLanguage
    ) { manual, system ->
        manual ?: system
    }

    private val settingsCombined = combine(
        adConfigFlow,
        effectiveLanguageFlow,
        streams.selectedCityFlow,
        streams.searchHistoryFlow,
        streams.isOnlineFlow
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

        val needsOnboardingNow = core.auth.isSignedIn && (core.auth.needsProfileOnboarding || local.forceOnboardingStep)
        val effectiveShowAuthSheet = if (core.auth.isSignedIn) needsOnboardingNow else local.showAuthSheet
        val effectiveGuestPrompt = if (core.auth.isSignedIn) null else local.guestPromptReason

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
            guestPromptReason = effectiveGuestPrompt,
            showAuthSheet = effectiveShowAuthSheet
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
            var wasOffline = false
            streams.isOnlineFlow
                .distinctUntilChanged()
                .collect { online ->
                    if (!online) {
                        wasOffline = true
                    } else if (wasOffline) {
                        wasOffline = false
                        val currentAuth = authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth
                        if (currentAuth.isSignedIn && (currentAuth.hasAdminClaim || currentAuth.hasAdminDoc)) {
                            authRepo.refreshIdTokenClaims(forceRefresh = true)
                        }
                    }
                }
        }
    }

    private fun handleSessionSideEffects(session: AuthSessionState) {
        val currentUid = session.uid
        val uidChanged = lastObservedSessionUid != null && lastObservedSessionUid != currentUid
        if (uidChanged) {
            lastCommercialSyncKey = null
            commercialSyncJob?.cancel()
            commercialSyncJob = null
            clearUserScopedState()
        } else if (lastObservedIsAdmin && !session.isAdmin) {
            val currentLocal = _localState.value
            val wasInAdminUi = currentLocal.activeTab == MainTab.ADMIN || currentLocal.adminAuditConversation != null
            if (wasInAdminUi) {
                if (_activeConversationId.value == currentLocal.adminAuditConversation?.id) {
                    _activeConversationId.value = currentLocal.activeConversationId
                }
                _localState.update { state ->
                    state.copy(
                        activeTab = if (state.activeTab == MainTab.ADMIN) MainTab.SWIPE else state.activeTab,
                        adminAuditConversation = null
                    )
                }
                showToast(
                    if (isArabic()) "تم تحديث الجلسة: لا توجد صلاحية إدارة نشطة" else "Session updated: admin access is not active",
                    ToastType.WARNING
                )
            }
        } else if (!session.isAdmin) {
            val currentLocal = _localState.value
            if (currentLocal.activeTab == MainTab.ADMIN || currentLocal.adminAuditConversation != null) {
                if (_activeConversationId.value == currentLocal.adminAuditConversation?.id) {
                    _activeConversationId.value = currentLocal.activeConversationId
                }
                _localState.update { state ->
                    state.copy(
                        activeTab = if (state.activeTab == MainTab.ADMIN) MainTab.SWIPE else state.activeTab,
                        adminAuditConversation = null
                    )
                }
            }
        }
        lastObservedSessionUid = currentUid
        lastObservedIsAdmin = session.isAdmin

        val profile = session.publicProfile
        val commercialKey = if (session.isSignedIn && profile != null) {
            listOf(
                profile.uid,
                profile.accountType,
                profile.name,
                profile.city,
                profile.businessActivity,
                profile.businessLogo,
                profile.avatar,
                profile.status,
                profile.dealsCompleted,
                profile.verifiedIdentity
            )
        } else {
            null
        }
        if (commercialKey != lastCommercialSyncKey) {
            lastCommercialSyncKey = commercialKey
            commercialSyncJob?.cancel()
            commercialSyncJob = null
            if (session.isSignedIn && profile != null && AccountType.fromWire(profile.accountType).isCommercial()) {
                val expectedUid = session.uid
                commercialSyncJob = viewModelScope.launch {
                    if (lastObservedSessionUid == expectedUid) {
                        authRepo.ensureCommercialProfileSynced(profile)
                    }
                }
            }
        }
    }

    fun requestGuestPrompt(reason: GuestPromptReason) {
        val activeAuth = authSessionFlow.replayCache.lastOrNull()
        if (uiState.value.isSignedIn || activeAuth?.isSignedIn == true || authRepo.currentFirebaseUser() != null) {
            return
        }
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
            val requestUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid ?: return
            viewModelScope.launch {
                val hasClaim = authRepo.refreshIdTokenClaims(forceRefresh = true).getOrDefault(uiState.value.auth.hasAdminClaim)
                val latestUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid
                if (hasClaim && latestUid == requestUid) {
                    refreshCloudinaryQuota()
                }
            }
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
        viewModelScope.launch { prefs?.setLanguage(next) }
    }

    fun selectCity(city: String) {
        viewModelScope.launch { prefs?.setSelectedCity(city) }
    }

    fun updateFilter(transform: (MarketFilterState) -> MarketFilterState) {
        _localState.update { it.copy(filter = transform(it.filter)) }
    }

    fun commitSearchToHistory(query: String) {
        viewModelScope.launch { prefs?.addSearchHistory(query) }
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch { prefs?.removeSearchHistoryItem(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { prefs?.clearSearchHistory() }
    }

    fun triggerPullToRefresh() {
        viewModelScope.launch {
            _localState.update { it.copy(isRefreshing = true) }
            val startUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid
            authRepo.verifyFirestoreServerConnection()
            val currentAuth = authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth
            if (currentAuth.uid == startUid && currentAuth.isSignedIn && (currentAuth.hasAdminClaim || currentAuth.hasAdminDoc)) {
                authRepo.refreshIdTokenClaims(forceRefresh = true)
            }
            val latestAuth = authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth
            if (latestAuth.uid == startUid && latestAuth.isAdmin) {
                refreshCloudinaryQuota()
            }
            delay(500)
            _localState.update { it.copy(isRefreshing = false) }
        }
    }

    fun openAuthSheet() {
        val state = uiState.value
        val activeAuth = authSessionFlow.replayCache.lastOrNull()
        val signedIn = state.isSignedIn || activeAuth?.isSignedIn == true || authRepo.currentFirebaseUser() != null
        val needsOnboarding = state.isOnboardingRequired ||
            (activeAuth?.isSignedIn == true && activeAuth.needsProfileOnboarding) ||
            _localState.value.forceOnboardingStep
        if (signedIn && !needsOnboarding) return
        _localState.update { it.copy(showAuthSheet = true, authErrorMessage = null) }
    }

    fun closeAuthSheet() {
        if (uiState.value.isOnboardingRequired) return
        _localState.update { it.copy(showAuthSheet = false, authErrorMessage = null) }
    }

    fun reportAuthError(message: String) {
        runCatching { android.util.Log.w("HatchAuthFlow", "Auth UI error reported") }
        _localState.update { it.copy(authErrorMessage = message) }
        showToast(message, ToastType.ERROR)
    }

    fun clearAuthError() {
        _localState.update { it.copy(authErrorMessage = null) }
    }

    internal fun blockSignInIfAlreadyActive(): Boolean {
        val activeAuth = authSessionFlow.replayCache.lastOrNull()
        val signedIn = uiState.value.isSignedIn || activeAuth?.isSignedIn == true || authRepo.currentFirebaseUser() != null
        if (!signedIn) return false
        runCatching { android.util.Log.w("HatchAuthFlow", "Blocked mid-session Google sign-in while already signed in") }
        reportAuthError(
            if (isArabic()) {
                "أنت مسجل الدخول بالفعل بهذا الحساب. يمكنك تعديل بياناتك من الملف الشخصي."
            } else {
                "You are already signed in to your HATCH account. Edit your details from Profile."
            }
        )
        return true
    }

    fun signInWithGoogleCredential(credential: AuthCredential) {
        if (blockSignInIfAlreadyActive()) return
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
                        prefs?.setOnboardingCompleted(true)
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
        val activeAuth = authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth
        val existingProfile = activeAuth.publicProfile ?: uiState.value.currentUser
        if (activeAuth.isSignedIn && existingProfile != null && !MarketplaceLogic.requiresOnboarding(existingProfile)) {
            _localState.update {
                it.copy(
                    forceOnboardingStep = false,
                    showAuthSheet = false,
                    authErrorMessage = null
                )
            }
            return
        }
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
                val repo = productRepo ?: run {
                    setSubmitting(false)
                    return@launch
                }
                repo.uploadSingleInspectionOrAvatarImage(avatarUri).getOrElse { e ->
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
                    prefs?.setOnboardingCompleted(true)
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
        val uid = uiState.value.auth.uid ?: return
        val previousType = AccountType.fromWire(uiState.value.currentUser?.accountType)
        val nextType = AccountType.fromWire(accountType)
        viewModelScope.launch {
            setSubmitting(true)
            val resolvedAvatar = if (avatarUri != null) {
                val repo = productRepo ?: run {
                    setSubmitting(false)
                    return@launch
                }
                repo.uploadSingleInspectionOrAvatarImage(avatarUri).getOrElse { e ->
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
        lastObservedSessionUid = null
        lastObservedIsAdmin = false
        lastCommercialSyncKey = null
        commercialSyncJob?.cancel()
        commercialSyncJob = null
        authRepo.signOut()
        clearUserScopedState()
        _localState.update { it.copy(showAuthSheet = false) }
        showToast(if (isArabic()) "تم تسجيل الخروج" else "Signed out", ToastType.INFO)
    }

    private fun clearUserScopedState() {
        _activeConversationId.value = null
        productViewsBeforeAd = 0
        _localState.update {
            it.copy(
                activeTab = MainTab.SWIPE,
                profileSubTab = ProfileSubTab.MY_ADS,
                adminSubTab = AdminSubTab.REPORTS,
                passedProductIds = emptySet(),
                activeConversationId = null,
                readBroadcastIds = emptySet(),
                cloudinaryQuota = CloudinaryQuotaStatus(),
                isRefreshing = false,
                isSubmitting = false,
                toasts = emptyList(),
                selectedProduct = null,
                selectedBusinessSellerId = null,
                commercialDirectoryType = null,
                guestPromptReason = null,
                offerTargetProduct = null,
                editingProduct = null,
                soldTargetProduct = null,
                reviewRequest = null,
                reportRequest = null,
                adminAuditConversation = null,
                showSellModal = false,
                sellErrorMessage = null,
                showAuthSheet = false,
                forceOnboardingStep = false,
                authErrorMessage = null,
                showEditProfileDialog = false,
                showVerificationRequestDialog = false,
                showNotificationsModal = false,
                showInterstitialAd = null,
                showRewardedAd = null,
                rewardBoostTargetProductId = null
            )
        }
    }

    fun openProductDetail(product: Product) {
        productViewsBeforeAd++
        _localState.update { it.copy(selectedProduct = product) }
        viewModelScope.launch { productRepo?.incrementViews(product.id) }

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
        val uid = uiState.value.auth.uid ?: return
        viewModelScope.launch { interactionRepo?.recordSwipe(uid, product.id, "right") }
    }

    fun swipeLeft(product: Product) {
        _localState.update { it.copy(passedProductIds = it.passedProductIds + product.id) }
        if (uiState.value.isCurrentlySuspended) return
        val uid = uiState.value.auth.uid ?: return
        viewModelScope.launch { interactionRepo?.recordSwipe(uid, product.id, "left") }
    }

    fun swipeRight(product: Product) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.FAVORITES)) return
        val uid = uiState.value.auth.uid ?: return
        _localState.update { it.copy(passedProductIds = it.passedProductIds + product.id) }
        viewModelScope.launch {
            interactionRepo?.recordSwipe(uid, product.id, "right")
            if (!uiState.value.favoriteIds.contains(product.id)) {
                interactionRepo?.toggleFavorite(uid, product.id, currentlyFavorite = false)
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
        val uid = uiState.value.auth.uid ?: return
        val currentlyFav = uiState.value.favoriteIds.contains(productId)
        viewModelScope.launch {
            interactionRepo?.toggleFavorite(uid, productId, currentlyFav)
                ?.onFailure { e -> showToast(e.localizedMessage ?: "Action failed", ToastType.ERROR) }
        }
    }

    fun openSellModal() {
        if (!requireActiveUserOrPrompt(GuestPromptReason.SELL)) return
        _localState.update { it.copy(showSellModal = true, sellErrorMessage = null) }
    }

    fun closeSellModal() {
        _localState.update { it.copy(showSellModal = false, sellErrorMessage = null) }
    }

    fun clearSellError() {
        _localState.update { it.copy(sellErrorMessage = null) }
    }

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
        if (_localState.value.isSubmitting) return

        val state = uiState.value
        if (!state.isSignedIn) {
            val msg = if (isArabic()) {
                "يرجى تسجيل الدخول بحساب Google أولاً لنشر إعلان."
            } else {
                "Please sign in with Google first to publish a listing."
            }
            _localState.update { it.copy(sellErrorMessage = msg) }
            requestGuestPrompt(GuestPromptReason.SELL)
            return
        }
        if (state.isCurrentlySuspended) {
            val msg = strings().suspendedActionBlocked
            _localState.update { it.copy(sellErrorMessage = msg) }
            showToast(msg, ToastType.WARNING)
            return
        }

        val seller = state.currentUser ?: run {
            val msg = if (isArabic()) {
                "تعذر تحميل بيانات ملفك الشخصي بعد. تأكد من إكمال بيانات حسابك أو انتظر لحظة ثم حاول مجدداً."
            } else {
                "Your profile details are not loaded yet. Complete your account setup or wait a moment and try again."
            }
            _localState.update { it.copy(sellErrorMessage = msg) }
            showToast(msg, ToastType.ERROR)
            return
        }

        val executor: suspend (ListingPublishRequest) -> Result<String> = streams.publishListing
            ?: productRepo?.let { repo ->
                { req ->
                    repo.createProduct(
                        seller = req.seller,
                        titleAr = req.titleAr,
                        titleEn = req.titleEn,
                        descriptionAr = req.descriptionAr,
                        descriptionEn = req.descriptionEn,
                        price = req.price,
                        originalPrice = req.originalPrice,
                        category = req.category,
                        condition = req.condition,
                        city = req.city,
                        areaAr = req.areaAr,
                        areaEn = req.areaEn,
                        localImageUris = req.localImageUris,
                        remoteImageUrls = req.remoteImageUrls,
                        paymentMethods = req.paymentMethods,
                        negotiable = req.negotiable,
                        deliveryAvailable = req.deliveryAvailable,
                        urgentSale = req.urgentSale
                    )
                }
            }
            ?: run {
                val msg = if (isArabic()) {
                    "خدمة نشر الإعلانات غير متوفرة حالياً."
                } else {
                    "Listing publishing service is currently unavailable."
                }
                _localState.update { it.copy(sellErrorMessage = msg) }
                showToast(msg, ToastType.ERROR)
                return
            }

        val request = ListingPublishRequest(
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
        )

        _localState.update { it.copy(sellErrorMessage = null) }
        viewModelScope.launch {
            setSubmitting(true)
            try {
                executor(request)
                    .onSuccess {
                        _localState.update {
                            it.copy(
                                showSellModal = false,
                                sellErrorMessage = null,
                                activeTab = MainTab.BROWSE
                            )
                        }
                        showToast(
                            if (isArabic()) "تم نشر إعلانك بنجاح! 🎉" else "Listing published! 🎉",
                            ToastType.SUCCESS
                        )
                    }
                    .onFailure { e ->
                        val msg = formatListingPublishError(e)
                        _localState.update { it.copy(sellErrorMessage = msg) }
                        showToast(msg, ToastType.ERROR)
                    }
            } catch (t: Throwable) {
                val msg = formatListingPublishError(t)
                _localState.update { it.copy(sellErrorMessage = msg) }
                showToast(msg, ToastType.ERROR)
            } finally {
                setSubmitting(false)
            }
        }
    }

    private fun formatListingPublishError(error: Throwable): String {
        val raw = error.localizedMessage?.trim().orEmpty()
        val fsCode = (error as? FirebaseFirestoreException)?.code
        if (fsCode == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            raw.contains("PERMISSION_DENIED", ignoreCase = true)
        ) {
            return if (isArabic()) {
                "رفض الخادم نشر الإعلان (تحقق من تسجيل الدخول، حالة الحساب، وصحة بيانات وصور الإعلان)."
            } else {
                "Server rejected publishing (verify sign-in, account status, and valid listing details/images)."
            }
        }
        if (fsCode == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error is UnknownHostException ||
            error is SocketTimeoutException ||
            error is IOException ||
            raw.contains("network", ignoreCase = true) ||
            raw.contains("Unable to resolve host", ignoreCase = true)
        ) {
            return if (isArabic()) {
                "تعذر الاتصال بالخادم. تحقق من اتصال الإنترنت وحاول مرة أخرى."
            } else {
                "Network connection failed. Check your internet connection and try again."
            }
        }
        if (raw.contains("MEDIA_SIGNER_URL", ignoreCase = true)) {
            return if (isArabic()) {
                "رفع صور الهاتف غير مفعّل حالياً (يتطلب MEDIA_SIGNER_URL). يمكنك إزالة صور الهاتف واستخدام رابط https://."
            } else {
                "Device image upload requires MEDIA_SIGNER_URL. Remove local photos and use an https:// link instead."
            }
        }
        if (raw.isNotBlank()) return raw
        return if (isArabic()) "تعذر نشر الإعلان، يرجى المحاولة مرة أخرى." else "Failed to publish listing. Please try again."
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
        val repo = productRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.updateProduct(
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
        val uid = uiState.value.auth.uid ?: return
        val repo = productRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.markProductAsSold(target.id, uid)
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
        if (product.sellerId == uiState.value.auth.uid) {
            showToast(if (isArabic()) "لا يمكنك تقديم عرض على منتجك" else "Cannot bid on your own listing", ToastType.WARNING)
            return
        }
        _localState.update { it.copy(offerTargetProduct = product) }
    }

    fun closeOfferModal() { _localState.update { it.copy(offerTargetProduct = null) } }

    fun submitOffer(product: Product, amount: Double, note: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.OFFER_NEGOTIATE)) return
        val buyer = uiState.value.currentUser ?: return
        val repo = chatRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.startOrGetConversation(product, buyer)
                .onSuccess { convId ->
                    val tempConv = uiState.value.conversations.firstOrNull { it.id == convId }
                        ?: Conversation(
                            id = convId,
                            productId = product.id,
                            buyerId = buyer.uid,
                            sellerId = product.sellerId,
                            participantIds = listOf(buyer.uid, product.sellerId)
                        )
                    repo.sendOffer(tempConv, buyer.uid, amount, note)
                        .onSuccess {
                            val latestUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid
                            if (latestUid == buyer.uid) {
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
        val repo = chatRepo ?: return
        viewModelScope.launch {
            repo.startOrGetConversation(product, buyer)
                .onSuccess { convId ->
                    val latestUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid
                    if (latestUid == buyer.uid) {
                        _activeConversationId.value = convId
                        _localState.update {
                            it.copy(
                                selectedProduct = null,
                                activeConversationId = convId,
                                activeTab = MainTab.CHAT
                            )
                        }
                    }
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Chat error", ToastType.ERROR) }
        }
    }

    fun selectConversation(conv: Conversation?) {
        _activeConversationId.value = conv?.id
        _localState.update { it.copy(activeConversationId = conv?.id) }
        if (uiState.value.isCurrentlySuspended) return
        val uid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid ?: return
        if (conv != null) {
            viewModelScope.launch {
                val latestUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid
                if (latestUid == uid) {
                    chatRepo?.markConversationRead(conv, uid, uiState.value.activeMessages)
                }
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.CONTACT_OR_BUY)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.uid ?: return
        viewModelScope.launch {
            chatRepo?.sendTextMessage(conv, uid, text)
                ?.onFailure { e -> showToast(e.localizedMessage ?: "Send failed", ToastType.ERROR) }
        }
    }

    fun sendInspectionPhoto(uri: Uri?, directUrl: String?, tag: String, caption: String) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.CONTACT_OR_BUY)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.uid ?: return
        viewModelScope.launch {
            setSubmitting(true)
            val resolvedUrl = when {
                !directUrl.isNullOrBlank() -> Result.success(directUrl)
                uri != null -> productRepo?.uploadSingleInspectionOrAvatarImage(uri)
                    ?: Result.failure(IllegalStateException("Product repository unavailable"))
                else -> Result.failure(IllegalArgumentException("No image selected"))
            }
            resolvedUrl.onSuccess { url ->
                chatRepo?.sendInspectionImageMessage(conv, uid, url, tag, caption)
            }.onFailure { e ->
                showToast(e.localizedMessage ?: "Photo upload failed", ToastType.ERROR)
            }
            setSubmitting(false)
        }
    }

    fun sendVoiceNote(audioUrl: String, durationSeconds: Int) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.CONTACT_OR_BUY)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.uid ?: return
        viewModelScope.launch {
            chatRepo?.sendVoiceMessage(conv, uid, audioUrl, durationSeconds)
                ?.onFailure { e -> showToast(e.localizedMessage ?: "Voice note failed", ToastType.ERROR) }
        }
    }

    fun respondToOffer(status: OfferStatus, counterAmount: Double? = null) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.OFFER_NEGOTIATE)) return
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.uid ?: return
        viewModelScope.launch {
            chatRepo?.respondToOffer(conv, uid, status, counterAmount)
                ?.onSuccess { showToast(if (isArabic()) "تم تحديث حالة العرض" else "Offer updated", ToastType.SUCCESS) }
                ?.onFailure { e -> showToast(e.localizedMessage ?: "Action failed", ToastType.ERROR) }
        }
    }

    fun openReviewModal(request: ReviewRequest) {
        if (!requireActiveUserOrPrompt(GuestPromptReason.RATE_OR_REPORT)) return
        val uid = uiState.value.auth.uid ?: return
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
        val repo = interactionRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.submitReview(
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
        val repo = interactionRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.submitReport(
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
        val uid = uiState.value.auth.uid ?: return
        if (uiState.value.isCurrentlySuspended) return
        val allIds = uiState.value.notifications.map { it.id }.toSet()
        _localState.update { it.copy(readBroadcastIds = it.readBroadcastIds + allIds) }
        viewModelScope.launch {
            uiState.value.notifications.filter { !it.read }.forEach { notif ->
                interactionRepo?.markPersonalNotificationRead(uid, notif.id)
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
        val repo = interactionRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.submitVerificationRequest(user, note, documentUrl)
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
        val uid = uiState.value.auth.uid ?: return
        viewModelScope.launch {
            productRepo?.featureProductWithCredit(productId, uid)
                ?.onSuccess { showToast(if (isArabic()) "تم تمييز الإعلان في صدارة السوق ⭐" else "Listing boosted ⭐", ToastType.SUCCESS) }
                ?.onFailure { e -> showToast(e.localizedMessage ?: "Boost failed", ToastType.ERROR) }
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
        viewModelScope.launch { monetizationRepo?.recordImpression(adId) }
    }
    fun recordAdClick(adId: String) {
        if (!uiState.value.isSignedIn || uiState.value.isCurrentlySuspended) return
        viewModelScope.launch { monetizationRepo?.recordClick(adId) }
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
        val repo = monetizationRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.sendBroadcastNotification(type, titleAr, titleEn, bodyAr, bodyEn, relatedProductId)
                .onSuccess { showToast(if (isArabic()) "تم إرسال الإشعار الجماعي بنجاح 📣" else "Broadcast sent 📣", ToastType.SUCCESS) }
                .onFailure { e -> handleAdminFailure(e, "Broadcast failed") }
            setSubmitting(false)
        }
    }

    fun updateReportStatus(reportId: String, status: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.updateReportStatus(reportId, status)
                ?.onSuccess { showToast(if (isArabic()) "تم تحديث حالة البلاغ" else "Report updated", ToastType.SUCCESS) }
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }

    fun approveFlaggedProduct(productId: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.approveFlaggedProduct(productId)
                ?.onSuccess { showToast(if (isArabic()) "تمت إعادة تفعيل الإعلان" else "Listing restored", ToastType.SUCCESS) }
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }

    fun deleteProductAsAdmin(productId: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            productRepo?.deleteProduct(productId)
                ?.onSuccess { showToast(if (isArabic()) "تم حذف الإعلان" else "Listing deleted", ToastType.INFO) }
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }

    fun suspendUser(uid: String, days: Int, reason: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.suspendUser(uid, days, reason)
                ?.onSuccess {
                    showToast(
                        if (isArabic()) "تم إيقاف الحساب وتعليق إعلاناته النشطة وإخطار المستخدم" else "User suspended and active listings hidden",
                        ToastType.WARNING
                    )
                }
                ?.onFailure { e -> handleAdminFailure(e, "Suspend failed") }
        }
    }

    fun unsuspendUser(uid: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.unsuspendUser(uid)
                ?.onSuccess { showToast(if (isArabic()) "تم فك الحظر عن الحساب" else "User unsuspended", ToastType.SUCCESS) }
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }

    fun approveVerificationRequest(request: VerificationRequest) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.approveVerificationRequest(request)
                .onSuccess { showToast(if (isArabic()) "تم اعتماد التوثيق الذهبي بنجاح ⭐" else "Gold badge approved ⭐", ToastType.SUCCESS) }
                .onFailure { e -> handleAdminFailure(e, "Failed") }
            setSubmitting(false)
        }
    }

    fun rejectVerificationRequest(request: VerificationRequest, reason: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.rejectVerificationRequest(request, reason)
                .onSuccess { showToast(if (isArabic()) "تم رفض طلب التوثيق وإشعار المستخدم" else "Verification rejected", ToastType.INFO) }
                .onFailure { e -> handleAdminFailure(e, "Failed") }
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
                .onFailure { e -> handleAdminFailure(e, "Failed") }
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
                .onFailure { e -> handleAdminFailure(e, "Credit grant failed") }
            setSubmitting(false)
        }
    }

    fun deleteUserAsAdmin(uid: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.deleteUserAsAdmin(uid)
                ?.onSuccess { showToast(if (isArabic()) "تم حذف الحساب بالكامل" else "User removed", ToastType.INFO) }
                ?.onFailure { e ->
                    val localized = if (e is UnsupportedOperationException) {
                        if (isArabic()) {
                            "الحذف الكامل للحساب يتطلب Cloud Function (Admin SDK) لحذف Firebase Auth والبيانات دون ترك سجلات يتيمة؛ استخدم الإيقاف المؤقت حالياً."
                        } else {
                            e.localizedMessage ?: "Account deletion requires Cloud Functions (Admin SDK)."
                        }
                    } else null
                    handleAdminFailure(e, localized ?: "Failed", overrideMessage = localized)
                }
        }
    }

    fun createSponsoredAd(ad: SponsoredAdItem) {
        if (!requireAdminOrDeny()) return
        val repo = monetizationRepo ?: return
        viewModelScope.launch {
            setSubmitting(true)
            repo.createSponsoredAd(ad)
                .onSuccess { showToast(if (isArabic()) "تم إطلاق الحملة الإعلانية" else "Ad launched", ToastType.SUCCESS) }
                .onFailure { e -> handleAdminFailure(e, "Create ad failed") }
            setSubmitting(false)
        }
    }

    fun toggleAdActive(adId: String, active: Boolean) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.toggleAdActive(adId, active)
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }
    fun updateAdDuration(adId: String, seconds: Int) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.updateAdDuration(adId, seconds)
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }
    fun deleteAd(adId: String) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.deleteAd(adId)
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }

    fun updateAdConfig(config: AdMobConfig) {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.updateAdConfig(config)
                ?.onSuccess { showToast(if (isArabic()) "تم حفظ إعدادات الإعلانات" else "Ad config saved", ToastType.SUCCESS) }
                ?.onFailure { e -> handleAdminFailure(e, "Failed") }
        }
    }

    fun resetAdStats() {
        if (!requireAdminOrDeny()) return
        viewModelScope.launch {
            monetizationRepo?.resetAllAdStats(uiState.value.sponsoredAds)
                ?.onSuccess { showToast(if (isArabic()) "تم تصفير عدادات الإعلانات" else "Ad stats reset", ToastType.INFO) }
                ?.onFailure { e -> handleAdminFailure(e, "Reset failed") }
        }
    }

    fun refreshCloudinaryQuota() {
        val currentAuth = authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth
        val requestUid = currentAuth.uid ?: return
        if (!currentAuth.isAdmin) return
        viewModelScope.launch {
            streams.fetchAdminQuotaStatus(true)
                .onSuccess { status ->
                    val latestAuth = authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth
                    if (latestAuth.uid == requestUid && latestAuth.isAdmin) {
                        _localState.update { it.copy(cloudinaryQuota = status) }
                    }
                }
                .onFailure { e ->
                    val latestAuth = authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth
                    if (latestAuth.uid == requestUid) {
                        val msg = e.message.orEmpty()
                        if (msg.contains("401") || msg.contains("403") || msg.contains("Forbidden", ignoreCase = true)) {
                            authRepo.refreshIdTokenClaims(forceRefresh = true)
                        }
                    }
                }
        }
    }

    private fun handleAdminFailure(
        error: Throwable,
        fallback: String,
        overrideMessage: String? = null
    ) {
        val requestUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid
        val isPermissionError = (error as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
            error.message?.contains("403") == true ||
            error.message?.contains("Forbidden", ignoreCase = true) == true
        if (isPermissionError && requestUid != null) {
            viewModelScope.launch {
                val latestUid = (authSessionFlow.replayCache.lastOrNull() ?: uiState.value.auth).uid
                if (latestUid == requestUid) {
                    authRepo.refreshIdTokenClaims(forceRefresh = true)
                }
            }
        }
        showToast(overrideMessage ?: error.localizedMessage ?: fallback, ToastType.ERROR)
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
