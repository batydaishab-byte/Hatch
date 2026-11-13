package com.hatch.marketplace

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.MainTab
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
import com.hatch.marketplace.ui.modals.CreateAccountAuthSheet
import com.hatch.marketplace.ui.modals.EditListingModal
import com.hatch.marketplace.ui.modals.EditProfileDialog
import com.hatch.marketplace.ui.modals.MakeOfferDialog
import com.hatch.marketplace.ui.modals.NativeAppInfoDialog
import com.hatch.marketplace.ui.modals.NotificationsModal
import com.hatch.marketplace.ui.modals.ProductDetailSheet
import com.hatch.marketplace.ui.modals.ReportViolationModal
import com.hatch.marketplace.ui.modals.SafetyGuideModal
import com.hatch.marketplace.ui.modals.SellModal
import com.hatch.marketplace.ui.modals.SoldConfirmDialog
import com.hatch.marketplace.ui.modals.UserRatingModal
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

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.signInWithGoogleCredential(credential)
                } else {
                    viewModel.showToast(
                        if (state.language == AppLanguage.AR) "تعذر استلام رمز Google" else "Missing Google ID token",
                        ToastType.ERROR
                    )
                }
            }.onFailure { e ->
                viewModel.showToast(e.localizedMessage ?: "Google Sign-In failed", ToastType.ERROR)
            }
        }
    }

    fun launchGoogleSignIn() {
        val webClientResId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (webClientResId == 0) {
            viewModel.showToast(
                if (state.language == AppLanguage.AR) {
                    "تسجيل Google يتطلب google-services.json؛ يمكنك استخدام البريد الإلكتروني الآن."
                } else {
                    "Google Sign-In requires google-services.json; please use Email sign-in."
                },
                ToastType.INFO
            )
            return
        }
        val webClientId = context.getString(webClientResId)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    HatchTheme(language = state.language) {
        if (state.showSplash) {
            SplashScreen(strings = strings)
            return@HatchTheme
        }

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
                            adminAlertsCount = state.pendingReportsCount + state.flaggedProductsCount,
                            onSelectTab = { viewModel.selectTab(it) },
                            onSellClick = { viewModel.openSellModal() }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (state.activeTab) {
                        MainTab.SWIPE -> SwipeDeckScreen(
                            products = state.swipeDeckProducts,
                            selectedCity = state.filter.selectedCity,
                            canUndo = state.passedProductIds.isNotEmpty(),
                            isRefreshing = state.isRefreshing,
                            language = state.language,
                            strings = strings,
                            onSelectCity = { viewModel.selectCity(it) },
                            onRefresh = { viewModel.triggerPullToRefresh() },
                            onSwipeLeft = { viewModel.swipeLeft(it) },
                            onSwipeRight = { viewModel.swipeRight(it) },
                            onSwipeUpOffer = { viewModel.openOfferModal(it) },
                            onUndo = { viewModel.undoLastSwipe() },
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
                            onAdClick = { viewModel.recordAdClick(it.id) }
                        )

                        MainTab.CHAT -> ChatScreen(
                            conversations = state.conversations,
                            activeConversationId = state.activeConversationId,
                            messages = state.activeMessages,
                            products = state.products,
                            currentUserId = state.auth.firebaseUser?.uid.orEmpty(),
                            language = state.language,
                            strings = strings,
                            onSelectConversation = { viewModel.selectConversation(it) },
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
                            activeSubTab = state.profileSubTab,
                            language = state.language,
                            strings = strings,
                            onSelectSubTab = { viewModel.selectProfileSubTab(it) },
                            onOpenAuth = { viewModel.openAuthSheet() },
                            onOpenEditProfile = { viewModel.openEditProfile() },
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
            ProductDetailSheet(
                product = prod,
                isFavorite = state.favoriteIds.contains(prod.id),
                isOwner = state.auth.firebaseUser?.uid == prod.sellerId,
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
                onAdImpression = { viewModel.recordAdImpression(it) },
                onAdClick = { viewModel.recordAdClick(it.id) },
                onCopiedToast = { viewModel.showToast(it, ToastType.SUCCESS) }
            )
        }

        if (state.showSellModal) {
            SellModal(
                defaultCity = state.currentUser?.city.orEmpty(),
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeSellModal() },
                onPublish = { tAr, tEn, dAr, dEn, price, orig, cat, cond, city, area, uris, urls, pm, neg, del, urg ->
                    viewModel.publishNewProduct(tAr, tEn, dAr, dEn, price, orig, cat, cond, city, area, uris, urls, pm, neg, del, urg)
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

        if (state.showAuthSheet) {
            CreateAccountAuthSheet(
                needsOnboarding = state.auth.firebaseUser != null && state.auth.needsProfileOnboarding,
                defaultName = state.auth.firebaseUser?.displayName.orEmpty(),
                defaultEmail = state.auth.firebaseUser?.email.orEmpty(),
                isSubmitting = state.isSubmitting,
                language = state.language,
                strings = strings,
                onDismiss = { viewModel.closeAuthSheet() },
                onGoogleSignInClick = { launchGoogleSignIn() },
                onEmailSignIn = { email, pass -> viewModel.signInWithEmail(email, pass) },
                onEmailSignUp = { email, pass, name, phone, city, avatar ->
                    viewModel.signUpWithEmail(email, pass, name, phone, city, avatar)
                },
                onCompleteOnboarding = { name, phone, city, avatar ->
                    viewModel.saveOnboardingProfile(name, phone, city, avatar)
                }
            )
        }

        if (state.showEditProfileDialog && state.currentUser != null) {
            EditProfileDialog(
                user = state.currentUser!!,
                currentPhone = state.auth.privateProfile?.phone.orEmpty(),
                isSubmitting = state.isSubmitting,
                language = state.language,
                onDismiss = { viewModel.closeEditProfile() },
                onSave = { name, phone, city, avatar -> viewModel.updateProfile(name, phone, city, avatar) }
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
                onSubmit = { reason, msg, flag -> viewModel.submitReport(reason, msg, flag) }
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
}
