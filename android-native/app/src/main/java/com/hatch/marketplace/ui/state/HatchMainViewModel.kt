package com.hatch.marketplace.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.hatch.marketplace.data.local.HatchPreferences
import com.hatch.marketplace.data.media.CloudinaryQuotaStatus
import com.hatch.marketplace.data.model.AdMobConfig
import com.hatch.marketplace.data.model.AdRevenueStats
import com.hatch.marketplace.data.model.AdminSubTab
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.ChatMessage
import com.hatch.marketplace.data.model.Conversation
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val sponsoredAds: List<SponsoredAdItem> = emptyList(),
    val adConfig: AdMobConfig = AdMobConfig(),
    val cloudinaryQuota: CloudinaryQuotaStatus = CloudinaryQuotaStatus(),
    val filter: MarketFilterState = MarketFilterState(),
    val searchHistory: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val toasts: List<UiToast> = emptyList(),
    val selectedProduct: Product? = null,
    val offerTargetProduct: Product? = null,
    val editingProduct: Product? = null,
    val soldTargetProduct: Product? = null,
    val reviewRequest: ReviewRequest? = null,
    val reportRequest: ReportRequest? = null,
    val adminAuditConversation: Conversation? = null,
    val showSellModal: Boolean = false,
    val showAuthSheet: Boolean = false,
    val showEditProfileDialog: Boolean = false,
    val showNotificationsModal: Boolean = false,
    val showSafetyGuideModal: Boolean = false,
    val showNativeInfoDialog: Boolean = false,
    val showInterstitialAd: SponsoredAdItem? = null,
    val showRewardedAd: SponsoredAdItem? = null,
    val rewardBoostTargetProductId: String? = null
) {
    val currentUser: PublicUser?
        get() = auth.publicProfile

    val isSignedIn: Boolean
        get() = auth.firebaseUser != null

    val isAdmin: Boolean
        get() = auth.isAdmin

    val isCurrentlySuspended: Boolean
        get() = currentUser?.let {
            MarketplaceLogic.isUserSuspended(it.status, it.suspendedUntil)
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
    private var productViewsBeforeAd = 0

    private val authSessionFlow = authRepo.observeAuthSession()

    private val productsFlow = authSessionFlow.flatMapLatest { session ->
        productRepo.observeProducts(session.firebaseUser?.uid, session.isVerifiedAdminEmail)
    }

    private val favoritesFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeFavoriteIds(session.firebaseUser?.uid)
    }

    private val conversationsFlow = authSessionFlow.flatMapLatest { session ->
        chatRepo.observeConversations(session.firebaseUser?.uid, session.isVerifiedAdminEmail)
    }

    private val activeMessagesFlow = _activeConversationId.flatMapLatest { convId ->
        chatRepo.observeMessages(convId)
    }

    private val notificationsFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeNotifications(session.firebaseUser?.uid)
    }

    private val reviewsFlow = interactionRepo.observeReviews()

    private val reportsFlow = authSessionFlow.flatMapLatest { session ->
        interactionRepo.observeReports(session.firebaseUser?.uid, session.isVerifiedAdminEmail)
    }

    private val usersFlow = authSessionFlow.flatMapLatest { session ->
        authRepo.observePublicUsers(session.firebaseUser != null, session.isVerifiedAdminEmail)
    }

    private val sponsoredAdsFlow = authSessionFlow.flatMapLatest { session ->
        monetizationRepo.observeSponsoredAds(session.isVerifiedAdminEmail)
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

    private data class MetaStreams(
        val notifications: List<NotificationItem>,
        val reviews: List<UserReview>,
        val reports: List<ReportTicket>,
        val users: List<PublicUser>,
        val ads: List<SponsoredAdItem>
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

    private val metaCombined = combine(
        notificationsFlow,
        reviewsFlow,
        reportsFlow,
        usersFlow,
        sponsoredAdsFlow
    ) { notifs, reviews, reports, users, ads ->
        MetaStreams(notifs, reviews, reports, users, ads)
    }

    private val settingsCombined = combine(
        adConfigFlow,
        prefs.languageFlow,
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
        val reviewsBySeller = meta.reviews.groupBy { it.sellerId }
        val enrichedProducts = core.products.map { p ->
            val sellerReviews = reviewsBySeller[p.sellerId].orEmpty()
            if (sellerReviews.isNotEmpty()) {
                val avg = sellerReviews.map { it.rating }.average()
                p.copy(sellerRating = avg)
            } else p
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
            allUsers = meta.users,
            sponsoredAds = meta.ads,
            adConfig = settings.adConfig,
            filter = local.filter.copy(selectedCity = settings.selectedCity),
            searchHistory = settings.searchHistory,
            showAuthSheet = local.showAuthSheet || (core.auth.firebaseUser != null && core.auth.needsProfileOnboarding)
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
    }

    fun selectTab(tab: MainTab) {
        if ((tab == MainTab.CHAT || tab == MainTab.FAVORITES) && !uiState.value.isSignedIn) {
            showToast(strings().loginRequiredToast, ToastType.INFO)
            _localState.update { it.copy(showAuthSheet = true) }
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

    fun openAuthSheet() { _localState.update { it.copy(showAuthSheet = true) } }

    fun closeAuthSheet() {
        if (uiState.value.auth.firebaseUser != null && uiState.value.auth.needsProfileOnboarding) return
        _localState.update { it.copy(showAuthSheet = false) }
    }

    fun signInWithGoogleCredential(credential: AuthCredential) {
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.signInWithCredential(credential)
                .onSuccess {
                    showToast(
                        if (isArabic()) "تم تسجيل الدخول بحساب Google بنجاح" else "Signed in with Google",
                        ToastType.SUCCESS
                    )
                    _localState.update { it.copy(showAuthSheet = false) }
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Google Sign-In failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.signInWithEmailPassword(email, password)
                .onSuccess {
                    showToast(if (isArabic()) "مرحباً بعودتك في هتش!" else "Welcome back to HATCH!", ToastType.SUCCESS)
                    _localState.update { it.copy(showAuthSheet = false) }
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Sign-in failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun signUpWithEmail(email: String, password: String, name: String, phone: String, city: String, avatar: String) {
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.signUpWithEmailPassword(email, password, name, phone, city, avatar)
                .onSuccess {
                    showToast(if (isArabic()) "تم إنشاء حسابك في هتش بنجاح!" else "Your HATCH account is ready!", ToastType.SUCCESS)
                    _localState.update { it.copy(showAuthSheet = false) }
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Account creation failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun saveOnboardingProfile(name: String, phone: String, city: String, avatar: String) {
        val fbUser = uiState.value.auth.firebaseUser ?: return
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.saveOnboardingProfile(fbUser.uid, fbUser.email.orEmpty(), name, phone, city, avatar)
                .onSuccess {
                    prefs.setOnboardingCompleted(true)
                    _localState.update { it.copy(showAuthSheet = false) }
                    showToast(if (isArabic()) "تم حفظ ملفك التجاري بنجاح" else "Profile saved", ToastType.SUCCESS)
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Profile save failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun updateProfile(name: String, phone: String, city: String, avatar: String) {
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            setSubmitting(true)
            authRepo.updateProfile(uid, name, phone, city, avatar)
                .onSuccess {
                    _localState.update { it.copy(showEditProfileDialog = false) }
                    showToast(if (isArabic()) "تم تحديث البيانات" else "Profile updated", ToastType.SUCCESS)
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Update failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun signOut() {
        authRepo.signOut()
        _activeConversationId.value = null
        _localState.update {
            it.copy(activeTab = MainTab.SWIPE, activeConversationId = null, showEditProfileDialog = false)
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

    fun swipeLeft(product: Product) {
        _localState.update { it.copy(passedProductIds = it.passedProductIds + product.id) }
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch { interactionRepo.recordSwipe(uid, product.id, "left") }
    }

    fun swipeRight(product: Product) {
        _localState.update { it.copy(passedProductIds = it.passedProductIds + product.id) }
        val uid = uiState.value.auth.firebaseUser?.uid
        if (uid == null) {
            showToast(strings().loginRequiredToast, ToastType.INFO)
            _localState.update { it.copy(showAuthSheet = true) }
            return
        }
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
        val uid = uiState.value.auth.firebaseUser?.uid
        if (uid == null) {
            showToast(strings().loginRequiredToast, ToastType.INFO)
            _localState.update { it.copy(showAuthSheet = true) }
            return
        }
        val currentlyFav = uiState.value.favoriteIds.contains(productId)
        viewModelScope.launch {
            interactionRepo.toggleFavorite(uid, productId, currentlyFav)
                .onFailure { e -> showToast(e.localizedMessage ?: "Action failed", ToastType.ERROR) }
        }
    }

    fun openSellModal() {
        if (!requireActiveUserOrPrompt()) return
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
        val seller = uiState.value.currentUser ?: return
        if (!requireActiveUserOrPrompt()) return
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

    fun openEditProduct(product: Product) { _localState.update { it.copy(editingProduct = product) } }
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

    fun requestMarkSold(product: Product) { _localState.update { it.copy(soldTargetProduct = product) } }
    fun closeMarkSold() { _localState.update { it.copy(soldTargetProduct = null) } }

    fun confirmMarkSold() {
        val target = uiState.value.soldTargetProduct ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            setSubmitting(true)
            productRepo.markProductAsSold(target.id, uid)
                .onSuccess { newlyVerified ->
                    _localState.update { it.copy(soldTargetProduct = null, selectedProduct = null) }
                    if (newlyVerified) {
                        showToast(
                            if (isArabic()) "مبروك! أكملت 10 صفقات وتم توثيق حسابك تلقائياً ✨" else "Congrats! 10 deals completed — Account Verified ✨",
                            ToastType.SUCCESS
                        )
                    } else {
                        showToast(
                            if (isArabic()) "تم أرشفة الإعلان كمباع (+1 صفقة ناجحة)" else "Marked as Sold (+1 deal)",
                            ToastType.SUCCESS
                        )
                    }
                }
                .onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun openOfferModal(product: Product) {
        if (!requireActiveUserOrPrompt()) return
        if (product.sellerId == uiState.value.auth.firebaseUser?.uid) {
            showToast(if (isArabic()) "لا يمكنك تقديم عرض على منتجك" else "Cannot bid on your own listing", ToastType.WARNING)
            return
        }
        _localState.update { it.copy(offerTargetProduct = product) }
    }

    fun closeOfferModal() { _localState.update { it.copy(offerTargetProduct = null) } }

    fun submitOffer(product: Product, amount: Double, note: String) {
        val buyer = uiState.value.currentUser ?: return
        if (!requireActiveUserOrPrompt()) return
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
        if (!uiState.value.isSignedIn) {
            showToast(strings().loginRequiredToast, ToastType.INFO)
            _localState.update { it.copy(showAuthSheet = true) }
            return
        }
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
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        if (conv != null) {
            viewModelScope.launch {
                chatRepo.markConversationRead(conv, uid, uiState.value.activeMessages)
            }
        }
    }

    fun sendChatMessage(text: String) {
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepo.sendTextMessage(conv, uid, text)
                .onFailure { e -> showToast(e.localizedMessage ?: "Send failed", ToastType.ERROR) }
        }
    }

    fun sendInspectionPhoto(uri: Uri?, directUrl: String?, tag: String, caption: String) {
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
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepo.sendVoiceMessage(conv, uid, audioUrl, durationSeconds)
                .onFailure { e -> showToast(e.localizedMessage ?: "Voice note failed", ToastType.ERROR) }
        }
    }

    fun respondToOffer(status: OfferStatus, counterAmount: Double? = null) {
        val conv = currentActiveConversation() ?: return
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepo.respondToOffer(conv, uid, status, counterAmount)
                .onSuccess { showToast(if (isArabic()) "تم تحديث حالة العرض" else "Offer updated", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Action failed", ToastType.ERROR) }
        }
    }

    fun openReviewModal(request: ReviewRequest) {
        if (!uiState.value.isSignedIn) {
            _localState.update { it.copy(showAuthSheet = true) }
            return
        }
        _localState.update { it.copy(reviewRequest = request) }
    }

    fun closeReviewModal() { _localState.update { it.copy(reviewRequest = null) } }

    fun submitReview(rating: Int, comment: String, tags: List<String>) {
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
                productTitle = req.productTitle
            ).onSuccess {
                _localState.update { it.copy(reviewRequest = null) }
                showToast(if (isArabic()) "شكراً لتقييمك الصادق ⭐" else "Review submitted ⭐", ToastType.SUCCESS)
            }.onFailure { e -> showToast(e.localizedMessage ?: "Failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun openReportModal(request: ReportRequest) {
        if (!uiState.value.isSignedIn) {
            _localState.update { it.copy(showAuthSheet = true) }
            return
        }
        _localState.update { it.copy(reportRequest = request) }
    }

    fun closeReportModal() { _localState.update { it.copy(reportRequest = null) } }

    fun submitReport(reason: String, message: String, flagListing: Boolean) {
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
                message = message,
                flagListing = flagListing
            ).onSuccess {
                _localState.update { it.copy(reportRequest = null) }
                showToast(if (isArabic()) "تم إرسال البلاغ للإدارة بنجاح" else "Report sent to Admin", ToastType.SUCCESS)
            }.onFailure { e -> showToast(e.localizedMessage ?: "Report failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun openNotifications() { _localState.update { it.copy(showNotificationsModal = true) } }
    fun closeNotifications() { _localState.update { it.copy(showNotificationsModal = false) } }

    fun markAllNotificationsRead() {
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
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
    fun openEditProfile() { _localState.update { it.copy(showEditProfileDialog = true) } }
    fun closeEditProfile() { _localState.update { it.copy(showEditProfileDialog = false) } }

    fun launchRewardedAdForCredit(targetProductId: String? = null) {
        if (!uiState.value.isSignedIn) {
            _localState.update { it.copy(showAuthSheet = true) }
            return
        }
        val ad = uiState.value.sponsoredAds.firstOrNull {
            it.isActive && (it.format == "rewarded" || it.format == "all")
        } ?: uiState.value.sponsoredAds.firstOrNull { it.isActive }
        ?: SponsoredAdItem(
            id = "hatch_default_reward",
            titleAr = "شركاء هتش الموثوقون",
            titleEn = "HATCH Trusted Partners",
            advertiser = "HATCH Sudan",
            descriptionAr = "شاهد الإعلان كاملاً للحصول على رصيد تمييز مجاني لإعلاناتك",
            descriptionEn = "Watch the full ad to earn +1 free listing boost credit",
            durationSeconds = uiState.value.adConfig.rewardedDurationSeconds
        )
        _localState.update { it.copy(showRewardedAd = ad, rewardBoostTargetProductId = targetProductId) }
        if (ad.id != "hatch_default_reward") recordAdImpression(ad.id)
    }

    fun completeRewardedAd() {
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        val boostProductId = uiState.value.rewardBoostTargetProductId
        _localState.update { it.copy(showRewardedAd = null, rewardBoostTargetProductId = null) }
        viewModelScope.launch {
            authRepo.adjustRewardCredits(uid, +1).onSuccess {
                if (boostProductId != null) {
                    productRepo.featureProductWithCredit(boostProductId, uid).onSuccess {
                        showToast(if (isArabic()) "تم تمييز إعلانك في أعلى السوق ⭐" else "Listing featured ⭐", ToastType.SUCCESS)
                    }
                } else {
                    showToast(if (isArabic()) "حصلت على +1 رصيد تمييز مجاني 🎉" else "Earned +1 boost credit 🎉", ToastType.SUCCESS)
                }
            }
        }
    }

    fun dismissRewardedAd() { _localState.update { it.copy(showRewardedAd = null, rewardBoostTargetProductId = null) } }

    fun boostListingWithExistingCredit(productId: String) {
        val uid = uiState.value.auth.firebaseUser?.uid ?: return
        viewModelScope.launch {
            productRepo.featureProductWithCredit(productId, uid)
                .onSuccess { showToast(if (isArabic()) "تم تمييز الإعلان في صدارة السوق ⭐" else "Listing boosted ⭐", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Boost failed", ToastType.ERROR) }
        }
    }

    fun dismissInterstitialAd() { _localState.update { it.copy(showInterstitialAd = null) } }
    fun testShowInterstitial() {
        val ad = uiState.value.sponsoredAds.firstOrNull() ?: return
        _localState.update { it.copy(showInterstitialAd = ad) }
    }

    fun recordAdImpression(adId: String) { viewModelScope.launch { monetizationRepo.recordImpression(adId) } }
    fun recordAdClick(adId: String) { viewModelScope.launch { monetizationRepo.recordClick(adId) } }

    fun openAdminConversationAudit(conversation: Conversation) {
        _localState.update { it.copy(adminAuditConversation = conversation) }
        _activeConversationId.value = conversation.id
    }

    fun closeAdminConversationAudit() {
        _localState.update { it.copy(adminAuditConversation = null) }
        _activeConversationId.value = uiState.value.activeConversationId
    }

    fun sendBroadcast(type: String, titleAr: String, titleEn: String, bodyAr: String, bodyEn: String, relatedProductId: String?) {
        viewModelScope.launch {
            setSubmitting(true)
            monetizationRepo.sendBroadcastNotification(type, titleAr, titleEn, bodyAr, bodyEn, relatedProductId)
                .onSuccess { showToast(if (isArabic()) "تم إرسال الإشعار الجماعي بنجاح 📣" else "Broadcast sent 📣", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Broadcast failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun updateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            monetizationRepo.updateReportStatus(reportId, status)
                .onSuccess { showToast(if (isArabic()) "تم تحديث حالة البلاغ" else "Report updated", ToastType.SUCCESS) }
        }
    }

    fun approveFlaggedProduct(productId: String) {
        viewModelScope.launch {
            monetizationRepo.approveFlaggedProduct(productId)
                .onSuccess { showToast(if (isArabic()) "تمت إعادة تفعيل الإعلان" else "Listing restored", ToastType.SUCCESS) }
        }
    }

    fun deleteProductAsAdmin(productId: String) {
        viewModelScope.launch {
            productRepo.deleteProduct(productId)
                .onSuccess { showToast(if (isArabic()) "تم حذف الإعلان" else "Listing deleted", ToastType.INFO) }
        }
    }

    fun suspendUser(uid: String, days: Int, reason: String) {
        viewModelScope.launch {
            monetizationRepo.suspendUser(uid, days, reason)
                .onSuccess { showToast(if (isArabic()) "تم إيقاف الحساب وإخطار المستخدم" else "User suspended", ToastType.WARNING) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Suspend failed", ToastType.ERROR) }
        }
    }

    fun unsuspendUser(uid: String) {
        viewModelScope.launch {
            monetizationRepo.unsuspendUser(uid)
                .onSuccess { showToast(if (isArabic()) "تم فك الحظر عن الحساب" else "User unsuspended", ToastType.SUCCESS) }
        }
    }

    fun deleteUserAsAdmin(uid: String) {
        viewModelScope.launch {
            monetizationRepo.deleteUserAsAdmin(uid)
                .onSuccess { showToast(if (isArabic()) "تم حذف بيانات الحساب" else "User removed", ToastType.INFO) }
        }
    }

    fun createSponsoredAd(ad: SponsoredAdItem) {
        viewModelScope.launch {
            setSubmitting(true)
            monetizationRepo.createSponsoredAd(ad)
                .onSuccess { showToast(if (isArabic()) "تم إطلاق الحملة الإعلانية" else "Ad launched", ToastType.SUCCESS) }
                .onFailure { e -> showToast(e.localizedMessage ?: "Create ad failed", ToastType.ERROR) }
            setSubmitting(false)
        }
    }

    fun toggleAdActive(adId: String, active: Boolean) { viewModelScope.launch { monetizationRepo.toggleAdActive(adId, active) } }
    fun updateAdDuration(adId: String, seconds: Int) { viewModelScope.launch { monetizationRepo.updateAdDuration(adId, seconds) } }
    fun deleteAd(adId: String) { viewModelScope.launch { monetizationRepo.deleteAd(adId) } }

    fun updateAdConfig(config: AdMobConfig) {
        viewModelScope.launch {
            monetizationRepo.updateAdConfig(config)
                .onSuccess { showToast(if (isArabic()) "تم حفظ إعدادات الإعلانات" else "Ad config saved", ToastType.SUCCESS) }
        }
    }

    fun resetAdStats() {
        viewModelScope.launch {
            monetizationRepo.resetAllAdStats(uiState.value.sponsoredAds)
                .onSuccess { showToast(if (isArabic()) "تم تصفير عدادات الإعلانات" else "Ad stats reset", ToastType.INFO) }
        }
    }

    fun refreshCloudinaryQuota() {
        viewModelScope.launch {
            val status = monetizationRepo.fetchAdminQuotaStatus(uiState.value.isAdmin)
            _localState.update { it.copy(cloudinaryQuota = status) }
        }
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

    private fun requireActiveUserOrPrompt(): Boolean {
        val state = uiState.value
        if (!state.isSignedIn) {
            showToast(strings().loginRequiredToast, ToastType.INFO)
            _localState.update { it.copy(showAuthSheet = true) }
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
