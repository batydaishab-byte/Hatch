package com.hatch.marketplace

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.GuestPromptReason
import com.hatch.marketplace.data.model.MainTab
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.ToastType
import com.hatch.marketplace.ui.components.AdMobBannerCard
import com.hatch.marketplace.ui.components.HatchBottomNav
import com.hatch.marketplace.ui.components.HatchHeader
import com.hatch.marketplace.ui.components.OfflineIndicatorBanner
import com.hatch.marketplace.ui.components.ToastHostOverlay
import com.hatch.marketplace.ui.i18n.HatchStrings
import com.hatch.marketplace.ui.modals.AdMobInterstitialModal
import com.hatch.marketplace.ui.modals.AdMobRewardedModal
import com.hatch.marketplace.ui.modals.AdminConversationModal
import com.hatch.marketplace.ui.modals.BusinessProfileModal
import com.hatch.marketplace.ui.modals.CommercialDirectoryModal
import com.hatch.marketplace.ui.modals.CreateAccountAuthSheet
import com.hatch.marketplace.ui.modals.EditListingModal
import com.hatch.marketplace.ui.modals.EditProfileDialog
import com.hatch.marketplace.ui.modals.GuestAuthPromptDialog
import com.hatch.marketplace.ui.modals.MakeOfferDialog
import com.hatch.marketplace.ui.modals.NativeAppInfoDialog
import com.hatch.marketplace.ui.modals.NotificationsModal
import com.hatch.marketplace.ui.modals.ProductDetailSheet
import com.hatch.marketplace.ui.modals.ReportViolationModal
import com.hatch.marketplace.ui.modals.SafetyGuideModal
import com.hatch.marketplace.ui.modals.SellModal
import com.hatch.marketplace.ui.modals.SoldConfirmDialog
import com.hatch.marketplace.ui.modals.UserRatingModal
import com.hatch.marketplace.ui.modals.VerificationRequestDialog
import com.hatch.marketplace.ui.screens.AdminScreen
import com.hatch.marketplace.ui.screens.ChatScreen
import com.hatch.marketplace.ui.screens.FavoritesScreen
import com.hatch.marketplace.ui.screens.MarketBrowseScreen
import com.hatch.marketplace.ui.screens.ProfileScreen
import com.hatch.marketplace.ui.screens.SplashScreen
import com.hatch.marketplace.ui.screens.SwipeDeckScreen
import com.hatch.marketplace.ui.state.HatchMainViewModel
import com.hatch.marketplace.ui.theme.HatchBgDark
import com.hatch.marketplace.ui.theme.HatchTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HatchMarketplaceRoot()
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun HatchMarketplaceRoot(
    viewModel: HatchMainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = remember(state.language) { HatchStrings.forLanguage(state.language) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val systemLangCode = ConfigurationCompat.getLocales(configuration)[0]?.language
        ?: Locale.getDefault().language

    LaunchedEffect(systemLangCode) {
        viewModel.updateSystemLocale(systemLangCode)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(
            "HatchAuthFlow",
            "GoogleSignIn ActivityResult resultCode=${result.resultCode}, hasData=${result.data != null}"
        )
        if (result.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                Log.d("HatchAuthFlow", "GoogleSignIn account received: hasIdToken=${!idToken.isNullOrBlank()}")
                if (!idToken.isNullOrBlank()) {
                    viewModel.clearAuthError()
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.signInWithGoogleCredential(credential)
                } else {
                    Log.w("HatchAuthFlow", "GoogleSignIn missing idToken (check default_web_client_id / SHA-1)")
                    viewModel.reportAuthError(
                        if (state.language == AppLanguage.AR) {
                            "تعذر استلام رمز Google (تأكد من Web Client ID وبصمة SHA-1 في Firebase)"
                        } else {
                            "Missing Google ID token (check Web Client ID and SHA-1 in Firebase)"
                        }
                    )
                }
            } catch (e: ApiException) {
                Log.w("HatchAuthFlow", "GoogleSignIn ApiException statusCode=${e.statusCode}")
                when (e.statusCode) {
                    12501 -> { /* User cancelled account chooser */ }
                    12500 -> viewModel.reportAuthError(
                        if (state.language == AppLanguage.AR) {
                            "تعذر إكمال الدخول عبر Google (رمز 12500): تأكد من إعداد OAuth وبصمة SHA-1 وتحديث خدمات Google Play"
                        } else {
                            "Google Sign-In failed (12500): check Firebase OAuth consent, SHA-1, and Google Play Services"
                        }
                    )
                    7 -> viewModel.reportAuthError(
                        if (state.language == AppLanguage.AR) {
                            "تعذر الاتصال بخدمات Google. تحقق من اتصال الإنترنت وحاول مرة أخرى."
                        } else {
                            "Network error during Google Sign-In. Please check your connection."
                        }
                    )
                    10 -> viewModel.reportAuthError(
                        if (state.language == AppLanguage.AR) {
                            "خطأ إعداد Google Sign-In (رمز 10): تأكد من تسجيل بصمة SHA-1 للتطبيق في Firebase Console"
                        } else {
                            "Google Sign-In config error (10): register app SHA-1 in Firebase Console"
                        }
                    )
                    else -> viewModel.reportAuthError(
                        if (state.language == AppLanguage.AR) {
                            "فشل تسجيل الدخول عبر Google (رمز ${e.statusCode}). حاول مرة أخرى."
                        } else {
                            "Google Sign-In failed (${e.statusCode}). Please try again."
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("HatchAuthFlow", "GoogleSignIn unexpected exception: ${e::class.java.simpleName}")
                viewModel.reportAuthError(
                    if (state.language == AppLanguage.AR) {
                        "حدث خطأ غير متوقع أثناء تسجيل الدخول عبر Google."
                    } else {
                        "Unexpected error during Google Sign-In."
                    }
                )
            }
        } else if (result.resultCode == Activity.RESULT_OK) {
            Log.w("HatchAuthFlow", "GoogleSignIn returned RESULT_OK with null data")
            viewModel.reportAuthError(
                if (state.language == AppLanguage.AR) "تعذر قراءة استجابة Google Sign-In" else "Empty Google Sign-In result"
            )
        }
    }

    fun launchGoogleSignIn() {
        viewModel.clearAuthError()
        if (viewModel.uiState.value.isSignedIn) {
            viewModel.reportAuthError(
                if (state.language == AppLanguage.AR) {
                    "أنت مسجل الدخول بالفعل بهذا الحساب. يمكنك تعديل بياناتك من الملف الشخصي."
                } else {
                    "You are already signed in to your HATCH account."
                }
            )
            return
        }
        val webClientResId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        Log.d("HatchAuthFlow", "launchGoogleSignIn: hasWebClientId=${webClientResId != 0}")
        if (webClientResId == 0) {
            viewModel.reportAuthError(
                if (state.language == AppLanguage.AR) {
                    "إعداد Google Sign-In غير مكتمل: أضف google-services.json مع تفعيل Google في Firebase"
                } else {
                    "Google Sign-In requires google-services.json with Firebase Google Auth enabled."
                }
            )
            return
        }
        val webClientId = context.getString(webClientResId)
        if (webClientId.isBlank()) {
            Log.w("HatchAuthFlow", "launchGoogleSignIn: default_web_client_id is blank")
            viewModel.reportAuthError(
                if (state.language == AppLanguage.AR) {
                    "قيمة default_web_client_id فارغة في google-services.json"
                } else {
                    "default_web_client_id is blank in google-services.json"
                }
            )
            return
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        runCatching {
            googleSignInLauncher.launch(client.signInIntent)
        }.onFailure { err ->
            Log.e("HatchAuthFlow", "Failed to launch Google Sign-In intent: ${err::class.java.simpleName}")
            viewModel.reportAuthError(
                if (state.language == AppLanguage.AR) {
                    "تعذر فتح نافذة تسجيل الدخول عبر Google على هذا الجهاز."
                } else {
                    "Unable to open Google Sign-In on this device."
                }
            )
        }
    }

    HatchTheme(language = state.language) {
        if (state.showSplash) {
            SplashScreen(strings = strings)
            return@HatchTheme
        }

        val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = HatchBgDark,
                topBar = {
                    Column {
                        HatchHeader(
                            strings = strings,
                            language = state.language,
                            currentUser = state.currentUser,
                            unreadNotifications = state.unreadNotificationsCount,
                            onLogoClick = { viewModel.selectTab(MainTab.SWIPE) },
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onOpenNotifications = { viewModel.openNotifications() },
                            onOpenSafetyGuide = { viewModel.openSafetyGuide() },
                            onOpenNativeInfo = { viewModel.openNativeInfoDialog() },
                            onOpenAuthOrProfile = {
                                if (state.isSignedIn) viewModel.selectTab(MainTab.PROFILE)
                                else viewModel.openAuthSheet()
                            }
                        )
                        OfflineIndicatorBanner(
                            isOnline = state.isOnline,
                            message = strings.offlineBanner
                        )
                    }
                },
                bottomBar = {
                    if (!isKeyboardOpen) {
                        Column {
                            AdMobBannerCard(
                                ads = state.sponsoredAds,
                                config = state.adConfig,
                                language = state.language,
                                strings = strings,
                                onImpression = { viewModel.recordAdImpression(it) },
                                onClickAd = { viewModel.recordAdClick(it.id) },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            HatchBottomNav(
                                strings = strings,
                                activeTab = state.activeTab,
                                unreadChatsCount = state.unreadChatsCount,
                                favoritesCount = state.favoriteIds.size,
                                isAdmin = state.isAdmin,
                                adminAlertsCount = state.pendingReportsCount + state.flaggedProductsCount + state.pendingVerificationRequestsCount,
                                onSelectTab = { viewModel.selectTab(it) },
                                onSellClick = { viewModel.openSellModal() }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .then(if (isKeyboardOpen) Modifier.windowInsetsPadding(WindowInsets.ime) else Modifier)
                ) {
                    when (state.activeTab) {
                        MainTab.SWIPE -> SwipeDeckScreen(
                            products = state.swipeDeckProducts,
                            favoriteIds = state.favoriteIds,
                            selectedCity = state.filter.selectedCity,
                            canUndo = state.passedProductIds.isNotEmpty(),
                            isRefreshing = state.isRefreshing,
                            language = state.language,
                            strings = strings,
                            onSelectCity = { viewModel.selectCity(it) },
                            onRefresh = { viewModel.triggerPullToRefresh() },
                            onBrowseNext = { viewModel.browseNextSwipeCard(it) },
                            onBrowsePrevious = { viewModel.undoLastSwipe() },
                            onSwipeUpOffer = { viewModel.openOfferModal(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onOpenDetail = { viewModel.openProductDetail(it) },
                            onResetDeck = { viewModel.resetSwipeDeck() },
                            onGoToBrowse = { viewModel.selectTab(MainTab.BROWSE) }
                        )

                        MainTab.BROWSE -> MarketBrowseScreen(
                            products = state.filteredMarketProducts,
                            favoriteIds = state.favoriteIds,
                            filter = state.filter,
                            searchHistory = state.searchHistory,
                            sponsoredAds = state.sponsoredAds,
                            adConfig = state.adConfig,
                            isSignedIn = state.isSignedIn,
                            isRefreshing = state.isRefreshing,
                            language = state.language,
                            strings = strings,
                            onRefresh = { viewModel.triggerPullToRefresh() },
                            onUpdateFilter = { viewModel.updateFilter(it) },
                            onSelectCity = { viewModel.selectCity(it) },
                            onCommitSearch = { viewModel.commitSearchToHistory(it) },
                            onRemoveSearchHistory = { viewModel.removeSearchHistoryItem(it) },
                            onClearSearchHistory = { viewModel.clearSearchHistory() },
                            onOpenProduct = { viewModel.openProductDetail(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onOpenAuth = { viewModel.openAuthSheet() },
                            onAdImpression = { viewModel.recordAdImpression(it) },
                            onAdClick = { viewModel.recordAdClick(it.id) },
                            onOpenStoresDirectory = { viewModel.openCommercialDirectory(AccountType.STORE) },
                            onOpenCompaniesDirectory = { viewModel.openCommercialDirectory(AccountType.COMPANY) }
                        )

                        MainTab.CHAT -> ChatScreen(
                            conversations = state.conversations,
                            activeConversationId = state.activeConversationId,
                            messages = state.activeMessages,
                            products = state.products,
                            reviews = state.reviews,
                            currentUserId = state.auth.firebaseUser?.uid.orEmpty(),
                            isCurrentlySuspended = state.isCurrentlySuspended,
                            language = state.language,
                            strings = strings,
                            onSelectConversation = { viewModel.selectConversation(it) },
                            onOpenProduct = { viewModel.openProductDetail(it) },
                            onOpenReview = { viewModel.openReviewModal(it) },
                            onSendMessage = { viewModel.sendChatMessage(it) },
                            onSendInspectionPhoto = { uri, url, tag, caption ->
                                viewModel.sendInspectionPhoto(uri, url, tag, caption)
                            },
                            onSendVoiceNote = { url, dur -> viewModel.sendVoiceNote(url, dur) },
                            onRespondOffer = { status, counter -> viewModel.respondToOffer(status, counter) },
                            onRequestMarkSold = { viewModel.requestMarkSold(it) },
                            onShowToast = { viewModel.showToast(it, ToastType.INFO) }
                        )

                        MainTab.FAVORITES -> FavoritesScreen(
                            favorites = state.favoriteProducts,
                            language = state.language,
                            strings = strings,
                            onOpenProduct = { viewModel.openProductDetail(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onExploreMarket = { viewModel.selectTab(MainTab.BROWSE) }
                        )

                        MainTab.PROFILE -> ProfileScreen(
                            isSignedIn = state.isSignedIn,
                            publicUser = state.currentUser,
                            privateUser = state.auth.privateProfile,
                            isCurrentlySuspended = state.isCurrentlySuspended,
                            myProducts = state.products.filter { it.sellerId == state.auth.firebaseUser?.uid },
                            favoriteProducts = state.favoriteProducts,
                            latestVerificationRequest = state.myLatestVerificationRequest,
                            activeSubTab = state.profileSubTab,
                            language = state.language,
                            strings = strings,
                            onSelectSubTab = { viewModel.selectProfileSubTab(it) },
                            onOpenAuth = { viewModel.openAuthSheet() },
                            onOpenEditProfile = { viewModel.openEditProfile() },
                            onOpenVerificationRequest = { viewModel.openVerificationRequestDialog() },
                            onOpenOwnBusinessProfile = {
                                state.currentUser?.uid?.let { viewModel.openBusinessProfile(it) }
                            },
                            onSignOut = { viewModel.signOut() },
                            onOpenProduct = { viewModel.openProductDetail(it) },
                            onEditProduct = { viewModel.openEditProduct(it) },
                            onMarkSold = { viewModel.requestMarkSold(it) },
                            onWatchRewardedAd = { viewModel.launchRewardedAdForCredit() },
                            onBoostProductWithCredit = { viewModel.boostListingWithExistingCredit(it) },
                            onOpenSafetyGuide = { viewModel.openSafetyGuide() },
                            onOpenNativeInfo = { viewModel.openNativeInfoDialog() }
                        )

                        MainTab.ADMIN -> {
                            if (state.isAdmin) {
                                AdminScreen(
                                    activeSubTab = state.adminSubTab,
                                    products = state.products,
                                    reports = state.reports,
                                    conversations = state.conversations,
                                    users = state.allUsers,
                                    notifications = state.notifications,
                                    verificationRequests = state.verificationRequests,
                                    creditTransactions = state.creditTransactions,
                                    pendingVerificationRequestsCount = state.pendingVerificationRequestsCount,
                                    sponsoredAds = state.sponsoredAds,
                                    adConfig = state.adConfig,
                                    adStats = state.adStats,
                                    cloudinaryQuota = state.cloudinaryQuota,
                                    isSubmitting = state.isSubmitting,
                                    language = state.language,
                                    strings = strings,
                                    onSelectSubTab = { viewModel.selectAdminSubTab(it) },
                                    onUpdateReportStatus = { id, st -> viewModel.updateReportStatus(id, st) },
                                    onOpenConversationAudit = { viewModel.openAdminConversationAudit(it) },
                                    onApproveFlaggedProduct = { viewModel.approveFlaggedProduct(it) },
                                    onDeleteProduct = { viewModel.deleteProductAsAdmin(it) },
                                    onSuspendUser = { uid, days, reason -> viewModel.suspendUser(uid, days, reason) },
                                    onUnsuspendUser = { viewModel.unsuspendUser(it) },
                                    onDeleteUser = { viewModel.deleteUserAsAdmin(it) },
                                    onApproveVerification = { viewModel.approveVerificationRequest(it) },
                                    onRejectVerification = { req, reason -> viewModel.rejectVerificationRequest(req, reason) },
                                    onSetUserVerified = { uid, verified, reason -> viewModel.setUserVerificationByAdmin(uid, verified, reason) },
                                    onGrantCredit = { uid, amount, reason -> viewModel.grantRewardCreditAsAdmin(uid, amount, reason) },
                                    onSendBroadcast = { type, tAr, tEn, bAr, bEn, relId ->
                                        viewModel.sendBroadcast(type, tAr, tEn, bAr, bEn, relId)
                                    },
                                    onCreateSponsoredAd = { viewModel.createSponsoredAd(it) },
                                    onToggleAdActive = { id, act -> viewModel.toggleAdActive(id, act) },
                                    onUpdateAdDuration = { id, sec -> viewModel.updateAdDuration(id, sec) },
                                    onDeleteAd = { viewModel.deleteAd(it) },
                                    onUpdateAdConfig = { viewModel.updateAdConfig(it) },
                                    onResetAdStats = { viewModel.resetAdStats() },
                                    onTestInterstitial = { viewModel.testShowInterstitial() },
                                    onTestRewarded = { viewModel.launchRewardedAdForCredit() },
                                    onRefreshQuota = { viewModel.refreshCloudinaryQuota() }
                                )
                            }
                        }
                    }
                }
            }

            if (state.showSellModal) {
                SellModal(
                    defaultCity = state.currentUser?.city.orEmpty(),
                    currentUser = state.currentUser,
                    isSubmitting = state.isSubmitting,
                    publishErrorMessage = state.sellErrorMessage,
                    onClearPublishError = { viewModel.clearSellError() },
                    language = state.language,
                    strings = strings,
                    onDismiss = { viewModel.closeSellModal() },
                    onPublish = { tAr, tEn, dAr, dEn, price, orig, cat, cond, city, area, uris, urls, pm, neg, del, urg ->
                        viewModel.publishNewProduct(tAr, tEn, dAr, dEn, price, orig, cat, cond, city, area, uris, urls, pm, neg, del, urg)
                    }
                )
            }

            ToastHostOverlay(
                toasts = state.toasts,
                onDismiss = { viewModel.dismissToast(it) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            )
        }

        // Modals & Dialogs
        state.selectedProduct?.let { prod ->
            val currentUid = state.auth.firebaseUser?.uid
            val eligibleReviewConv = MarketplaceLogic.findEligibleReviewConversation(
                conversations = state.conversations,
                products = state.products,
                reviews = state.reviews,
                currentUserId = currentUid,
                sellerId = prod.sellerId,
                productId = prod.id,
                isSuspended = state.isCurrentlySuspended
            )
            ProductDetailSheet(
                product = prod,
                isFavorite = state.favoriteIds.contains(prod.id),
                isOwner = currentUid == prod.sellerId,
                isSignedIn = state.isSignedIn,
                canRateSeller = eligibleReviewConv != null,
                eligibleReviewConversationId = eligibleReviewConv?.id,
                sellerReviews = state.reviews.filter { it.sellerId == prod.sellerId },
                sponsoredAds = state.sponsoredAds,
                adConfig = state.adConfig,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeProductDetail() },
                onToggleFavorite = { viewModel.toggleFavorite(prod.id) },
                onMakeOffer = { viewModel.openOfferModal(prod) },
                onStartChat = { viewModel.startChatWithSeller(prod) },
                onEditListing = { viewModel.openEditProduct(prod) },
                onMarkSold = { viewModel.requestMarkSold(prod) },
                onOpenReview = { viewModel.openReviewModal(it) },
                onOpenReport = { viewModel.openReportModal(it) },
                onOpenBusinessProfile = { sellerId ->
                    viewModel.closeProductDetail()
                    viewModel.openBusinessProfile(sellerId)
                },
                onAdImpression = { viewModel.recordAdImpression(it) },
                onAdClick = { viewModel.recordAdClick(it.id) },
                onCopiedToast = { viewModel.showToast(it, ToastType.SUCCESS) }
            )
        }

        state.selectedBusinessSellerId?.let { sellerId ->
            val sellerUser = state.directoryUsers.firstOrNull { it.uid == sellerId }
            val sellerProducts = MarketplaceLogic.buildBusinessStorefrontProducts(state.products, sellerId)
            BusinessProfileModal(
                sellerId = sellerId,
                sellerUser = sellerUser,
                sellerProducts = sellerProducts,
                favoriteIds = state.favoriteIds,
                sellerReviews = state.reviews.filter { it.sellerId == sellerId },
                isOwnProfile = state.auth.firebaseUser?.uid == sellerId,
                isSignedIn = state.isSignedIn,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeBusinessProfile() },
                onOpenProduct = { prod ->
                    viewModel.closeBusinessProfile()
                    viewModel.openProductDetail(prod)
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onStartChatWithProduct = { prod ->
                    viewModel.closeBusinessProfile()
                    viewModel.startChatWithSeller(prod)
                },
                onEditOwnProfile = {
                    viewModel.closeBusinessProfile()
                    viewModel.openEditProfile()
                },
                onGuestContactPrompt = {
                    viewModel.requestGuestPrompt(GuestPromptReason.CONTACT_OR_BUY)
                }
            )
        }

        state.commercialDirectoryType?.let { dirType ->
            val directoryEntries = remember(
                state.allUsers,
                state.commercialProfiles,
                state.currentUser,
                state.products,
                dirType
            ) {
                MarketplaceLogic.buildCommercialDirectory(
                    users = state.directoryUsers,
                    products = state.products,
                    targetType = dirType
                )
            }
            CommercialDirectoryModal(
                accountType = dirType,
                entries = directoryEntries,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeCommercialDirectory() },
                onSelectSeller = { sellerId ->
                    viewModel.closeCommercialDirectory()
                    viewModel.openBusinessProfile(sellerId)
                },
                onOpenProduct = { prod ->
                    viewModel.closeCommercialDirectory()
                    viewModel.openProductDetail(prod)
                }
            )
        }

        state.editingProduct?.let { prod ->
            EditListingModal(
                product = prod,
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeEditProduct() },
                onSave = { id, tAr, dAr, price, orig, cat, cond, city, area, imgs, pm, neg, del, urg ->
                    viewModel.saveEditedProduct(id, tAr, dAr, price, orig, cat, cond, city, area, imgs, pm, neg, del, urg)
                }
            )
        }

        state.offerTargetProduct?.let { prod ->
            MakeOfferDialog(
                product = prod,
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeOfferModal() },
                onSubmitOffer = { amount, note -> viewModel.submitOffer(prod, amount, note) }
            )
        }

        state.soldTargetProduct?.let { prod ->
            SoldConfirmDialog(
                product = prod,
                isSubmitting = state.isSubmitting,
                strings = strings,
                onDismiss = { viewModel.closeMarkSold() },
                onConfirm = { viewModel.confirmMarkSold() }
            )
        }

        if (state.showAuthSheet && (!state.isSignedIn || state.isOnboardingRequired)) {
            CreateAccountAuthSheet(
                needsOnboarding = state.isOnboardingRequired,
                defaultName = state.auth.firebaseUser?.displayName.orEmpty(),
                defaultAvatar = state.auth.firebaseUser?.photoUrl?.toString().orEmpty(),
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                authErrorMessage = state.authErrorMessage,
                onClearAuthError = { viewModel.clearAuthError() },
                onDismiss = { viewModel.closeAuthSheet() },
                onGoogleSignInClick = { launchGoogleSignIn() },
                onCompleteOnboarding = { name, phone, city, avatar, avatarUri, accountType, businessActivity, businessLogo ->
                    viewModel.saveOnboardingProfile(
                        name = name,
                        phone = phone,
                        city = city,
                        avatar = avatar,
                        avatarUri = avatarUri,
                        accountType = accountType,
                        businessActivity = businessActivity,
                        businessLogo = businessLogo
                    )
                }
            )
        }

        if (state.showEditProfileDialog && state.currentUser != null) {
            EditProfileDialog(
                user = state.currentUser!!,
                currentPhone = state.auth.privateProfile?.phone.orEmpty(),
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeEditProfile() },
                onSave = { name, phone, city, avatar, avatarUri, accountType, businessActivity, businessLogo ->
                    viewModel.updateProfile(
                        name = name,
                        phone = phone,
                        city = city,
                        avatar = avatar,
                        avatarUri = avatarUri,
                        accountType = accountType,
                        businessActivity = businessActivity,
                        businessLogo = businessLogo
                    )
                }
            )
        }

        if (state.showVerificationRequestDialog && state.currentUser != null) {
            VerificationRequestDialog(
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeVerificationRequestDialog() },
                onSubmit = { note, docUrl -> viewModel.submitVerificationRequest(note, docUrl) }
            )
        }

        state.reviewRequest?.let { req ->
            UserRatingModal(
                request = req,
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeReviewModal() },
                onSubmit = { rating, comment, tags -> viewModel.submitReview(rating, comment, tags) }
            )
        }

        state.reportRequest?.let { req ->
            ReportViolationModal(
                request = req,
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeReportModal() },
                onSubmit = { reason, msg -> viewModel.submitReport(reason, msg) }
            )
        }

        if (state.showNotificationsModal) {
            NotificationsModal(
                notifications = state.notifications,
                readBroadcastIds = state.readBroadcastIds,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeNotifications() },
                onMarkAllRead = { viewModel.markAllNotificationsRead() }
            )
        }

        if (state.showSafetyGuideModal) {
            SafetyGuideModal(
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeSafetyGuide() }
            )
        }

        if (state.showNativeInfoDialog) {
            NativeAppInfoDialog(
                language = state.language,
                onDismiss = { viewModel.closeNativeInfoDialog() }
            )
        }

        state.showInterstitialAd?.let { ad ->
            AdMobInterstitialModal(
                ad = ad,
                countdownSeconds = state.adConfig.interstitialCountdownSeconds,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.dismissInterstitialAd() },
                onClickAd = { viewModel.recordAdClick(ad.id) }
            )
        }

        state.showRewardedAd?.let { ad ->
            AdMobRewardedModal(
                ad = ad,
                durationSeconds = state.adConfig.rewardedDurationSeconds,
                language = state.language,
                onCompleted = { viewModel.completeRewardedAd() },
                onForfeitDismiss = { viewModel.dismissRewardedAd() }
            )
        }

        if (state.isAdmin) {
            state.adminAuditConversation?.let { conv ->
                AdminConversationModal(
                    conversation = conv,
                    messages = state.activeMessages,
                    language = state.language,
                    onDismiss = { viewModel.closeAdminConversationAudit() },
                    onDeleteAd = { viewModel.deleteProductAsAdmin(it) },
                    onSuspendSeller = { sellerId, sellerName ->
                        viewModel.suspendUser(
                            sellerId,
                            7,
                            if (state.language == AppLanguage.AR) "طلب عربون مخالف عبر المحادثة ($sellerName)" else "Upfront deposit scam warning ($sellerName)"
                        )
                    }
                )
            }
        }

        if (!state.isSignedIn) {
            state.guestPromptReason?.let { reason ->
                GuestAuthPromptDialog(
                    reason = reason,
                    language = state.language,
                    strings = strings,
                    onContinueWithGoogle = {
                        viewModel.dismissGuestPrompt()
                        launchGoogleSignIn()
                    },
                    onDismiss = { viewModel.dismissGuestPrompt() }
                )
            }
        }
    }
}
