package com.hatch.marketplace.ui.state

import com.hatch.marketplace.data.media.CloudinaryQuotaStatus
import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.MainTab
import com.hatch.marketplace.data.model.NotificationItem
import com.hatch.marketplace.data.model.PrivateUser
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.ToastType
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import com.hatch.marketplace.data.repository.AuthRepository
import com.hatch.marketplace.data.repository.AuthUserIdentity
import com.hatch.marketplace.data.repository.FakeAuthSessionGateway
import com.hatch.marketplace.data.repository.NoOpAuthSessionLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HatchMainViewModelSessionTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private data class TestHarness(
        val gateway: FakeAuthSessionGateway,
        val isOnlineFlow: MutableStateFlow<Boolean>,
        val productsObserveCalls: MutableList<Pair<String?, Boolean>>,
        val favoritesObserveCalls: MutableList<String?>,
        val conversationsObserveCalls: MutableList<Pair<String?, Boolean>>,
        val userFavorites: MutableStateFlow<Map<String, Set<String>>>,
        val userConversations: MutableStateFlow<Map<String, List<Conversation>>>,
        val userNotifications: MutableStateFlow<Map<String, List<NotificationItem>>>,
        val viewModel: HatchMainViewModel
    )

    private fun createHarness(
        scope: kotlinx.coroutines.CoroutineScope,
        fetchAdminQuotaStatus: suspend (Boolean) -> Result<CloudinaryQuotaStatus> = { isAdmin ->
            Result.success(CloudinaryQuotaStatus(creditsUsed = if (isAdmin) 5.5 else 0.0, videoUploadsEnabled = isAdmin))
        }
    ): TestHarness {
        val gateway = FakeAuthSessionGateway()
        val authRepo = AuthRepository(
            clients = FirebaseClientsHolder(app = null, auth = null, firestore = null),
            gateway = gateway,
            sessionScope = scope,
            logger = NoOpAuthSessionLogger,
            stopTimeoutMillis = 0L
        )
        val isOnline = MutableStateFlow(true)
        val productsCalls = mutableListOf<Pair<String?, Boolean>>()
        val favCalls = mutableListOf<String?>()
        val convCalls = mutableListOf<Pair<String?, Boolean>>()
        val favsByUser = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
        val convsByUser = MutableStateFlow<Map<String, List<Conversation>>>(emptyMap())
        val notifsByUser = MutableStateFlow<Map<String, List<NotificationItem>>>(emptyMap())

        val streams = HatchViewModelStreams(
            observeProducts = { uid, isAdmin ->
                productsCalls.add(uid to isAdmin)
                MutableStateFlow(listOf(Product(id = "prod_1", titleAr = "هاتف", sellerId = "seller_x")))
            },
            observeFavoriteIds = { uid ->
                favCalls.add(uid)
                favsByUser.map { if (uid == null) emptySet() else it[uid].orEmpty() }
            },
            observeConversations = { uid, isAdmin ->
                convCalls.add(uid to isAdmin)
                convsByUser.map { if (uid == null) emptyList() else it[uid].orEmpty() }
            },
            observeNotifications = { uid ->
                notifsByUser.map { if (uid == null) emptyList() else it[uid].orEmpty() }
            },
            isOnlineFlow = isOnline,
            fetchAdminQuotaStatus = fetchAdminQuotaStatus
        )

        val vm = HatchMainViewModel(
            authRepo = authRepo,
            productRepo = null,
            interactionRepo = null,
            chatRepo = null,
            monetizationRepo = null,
            prefs = null,
            streams = streams
        )

        return TestHarness(
            gateway = gateway,
            isOnlineFlow = isOnline,
            productsObserveCalls = productsCalls,
            favoritesObserveCalls = favCalls,
            conversationsObserveCalls = convCalls,
            userFavorites = favsByUser,
            userConversations = convsByUser,
            userNotifications = notifsByUser,
            viewModel = vm
        )
    }

    @Test
    fun viewModelSubscribesOnceToObserveAuthSessionAndSkipsProfileOnlyListenerRestarts() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }

        harness.gateway.setUserClaims("u1", cachedAdmin = false, serverAdmin = false)
        harness.gateway.emitAuthState(AuthUserIdentity(uid = "u1", isEmailVerified = true))
        advanceUntilIdle()

        // Single upstream Auth/IdToken/Doc subscription despite 14+ ViewModel consumers
        assertEquals(1, harness.gateway.authListenerRegistrations)
        assertEquals(1, harness.gateway.idTokenListenerRegistrations)
        assertEquals(1, harness.gateway.userDocsRegistrations)
        assertEquals(0, harness.gateway.forcedTokenRefreshCalls)
        assertEquals(listOf("u1" to false), harness.productsObserveCalls)
        assertEquals(listOf("u1"), harness.favoritesObserveCalls)

        val productsCallsAfterSignIn = harness.productsObserveCalls.toList()
        val favsCallsAfterSignIn = harness.favoritesObserveCalls.toList()

        // Profile/credit changes update uiState.auth WITHOUT restarting downstream collection listeners
        harness.gateway.emitUserDocs(
            uid = "u1",
            publicUser = PublicUser(uid = "u1", name = "سارة", city = "الخرطوم"),
            emitPublic = true,
            privateUser = PrivateUser(uid = "u1", rewardCredits = 2),
            emitPrivate = true
        )
        advanceUntilIdle()
        assertEquals("سارة", harness.viewModel.uiState.value.currentUser?.name)
        assertEquals(2, harness.viewModel.uiState.value.auth.privateProfile?.rewardCredits)

        harness.gateway.emitUserDocs(
            uid = "u1",
            publicUser = PublicUser(uid = "u1", name = "سارة", city = "بورتسودان", dealsCompleted = 4),
            emitPublic = true,
            privateUser = PrivateUser(uid = "u1", rewardCredits = 10),
            emitPrivate = true
        )
        advanceUntilIdle()
        assertEquals("بورتسودان", harness.viewModel.uiState.value.currentUser?.city)
        assertEquals(productsCallsAfterSignIn, harness.productsObserveCalls)
        assertEquals(favsCallsAfterSignIn, harness.favoritesObserveCalls)

        // Regular user pull-to-refresh does NOT force getIdToken(true)
        harness.viewModel.triggerPullToRefresh()
        advanceTimeBy(600L)
        advanceUntilIdle()
        assertEquals(0, harness.gateway.forcedTokenRefreshCalls)

        uiJob.cancel()
        advanceTimeBy(6_000L)
        advanceUntilIdle()
        assertEquals(0, harness.gateway.activeAuthListenerCount)
        assertEquals(0, harness.gateway.activeIdTokenListenerCount)
        assertEquals(0, harness.gateway.activeUserDocsCount)
    }

    @Test
    fun accountSwitchAndSignOutClearAllPreviousUserDataCompletely() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }

        harness.userFavorites.value = mapOf(
            "u1" to setOf("prod_1"),
            "u2" to emptySet()
        )
        harness.userConversations.value = mapOf(
            "u1" to listOf(Conversation(id = "conv_u1", productId = "prod_1", buyerId = "u1", sellerId = "s1")),
            "u2" to listOf(Conversation(id = "conv_u2", productId = "prod_2", buyerId = "u2", sellerId = "s2"))
        )
        harness.userNotifications.value = mapOf(
            "u1" to listOf(NotificationItem(id = "n_u1", titleAr = "إشعار 1")),
            "u2" to emptyList()
        )

        // User 1 signs in as Admin and builds local session state
        harness.gateway.setUserClaims("u1", cachedAdmin = true, serverAdmin = true)
        harness.gateway.emitAuthState(AuthUserIdentity(uid = "u1", isEmailVerified = true))
        advanceUntilIdle()
        harness.gateway.emitUserDocs(
            uid = "u1",
            publicUser = PublicUser(uid = "u1", name = "User One", city = "الخرطوم"),
            emitPublic = true,
            adminDocExists = true
        )
        advanceUntilIdle()

        harness.viewModel.selectTab(MainTab.ADMIN)
        advanceUntilIdle()
        harness.viewModel.browseNextSwipeCard(Product(id = "prod_1"))
        harness.viewModel.selectConversation(Conversation(id = "conv_u1", productId = "prod_1"))
        harness.viewModel.markAllNotificationsRead()
        advanceUntilIdle()

        assertEquals(MainTab.ADMIN, harness.viewModel.uiState.value.activeTab)
        assertTrue(harness.viewModel.uiState.value.passedProductIds.contains("prod_1"))
        assertEquals("conv_u1", harness.viewModel.uiState.value.activeConversationId)
        assertEquals(setOf("prod_1"), harness.viewModel.uiState.value.favoriteIds)
        assertEquals(5.5, harness.viewModel.uiState.value.cloudinaryQuota.creditsUsed, 0.001)

        // Direct switch to User 2 (non-admin) -> ALL User 1 data must be gone
        harness.gateway.setUserClaims("u2", cachedAdmin = false, serverAdmin = false)
        harness.gateway.emitAuthState(AuthUserIdentity(uid = "u2", isEmailVerified = true))
        runCurrent()
        harness.gateway.emitUserDocs(
            uid = "u2",
            publicUser = PublicUser(uid = "u2", name = "User Two", city = "عطبرة"),
            emitPublic = true,
            adminDocExists = false
        )
        advanceTimeBy(100L)
        runCurrent()

        val u2StateImmediate = harness.viewModel.uiState.value
        assertEquals("u2", u2StateImmediate.auth.uid)
        assertEquals("User Two", u2StateImmediate.currentUser?.name)
        assertFalse(u2StateImmediate.isAdmin)
        assertEquals(MainTab.SWIPE, u2StateImmediate.activeTab)
        assertTrue(u2StateImmediate.passedProductIds.isEmpty())
        assertTrue(u2StateImmediate.readBroadcastIds.isEmpty())
        assertNull(u2StateImmediate.activeConversationId)
        assertNull(u2StateImmediate.adminAuditConversation)
        assertTrue(u2StateImmediate.favoriteIds.isEmpty())
        assertEquals(listOf("conv_u2"), u2StateImmediate.conversations.map { it.id })
        assertEquals(0.0, u2StateImmediate.cloudinaryQuota.creditsUsed, 0.001)
        assertFalse(u2StateImmediate.showAuthSheet)
        assertTrue(u2StateImmediate.toasts.isEmpty())
        // Public marketplace catalog stays intact across account switch
        assertTrue(u2StateImmediate.products.isNotEmpty())

        advanceUntilIdle()

        // Sign out returns cleanly to Guest mode with zero user data while keeping public market data
        harness.viewModel.browseNextSwipeCard(Product(id = "prod_1"))
        harness.viewModel.signOut()
        advanceUntilIdle()

        val guestState = harness.viewModel.uiState.value
        assertFalse(guestState.isSignedIn)
        assertNull(guestState.currentUser)
        assertTrue(guestState.passedProductIds.isEmpty())
        assertTrue(guestState.conversations.isEmpty())
        assertTrue(guestState.notifications.isEmpty())
        assertEquals(MainTab.SWIPE, guestState.activeTab)
        assertFalse(guestState.showAuthSheet)
        assertTrue(guestState.products.isNotEmpty())

        uiJob.cancel()
        advanceTimeBy(6_000L)
        advanceUntilIdle()
        assertEquals(0, harness.gateway.activeAuthListenerCount)
        assertEquals(0, harness.gateway.activeIdTokenListenerCount)
        assertEquals(0, harness.gateway.activeUserDocsCount)
    }

    @Test
    fun lateAdminQuotaCallbackIgnoredAfterAccountSwitch() = runTest(mainDispatcher) {
        val deferredQuota = CompletableDeferred<Result<CloudinaryQuotaStatus>>()
        val harness = createHarness(
            scope = backgroundScope,
            fetchAdminQuotaStatus = { deferredQuota.await() }
        )
        val uiJob = launch { harness.viewModel.uiState.collect {} }

        harness.gateway.setUserClaims("u1", cachedAdmin = true, serverAdmin = true)
        harness.gateway.emitAuthState(AuthUserIdentity(uid = "u1", isEmailVerified = true))
        advanceUntilIdle()
        harness.gateway.emitUserDocs(
            uid = "u1",
            publicUser = PublicUser(uid = "u1", name = "Admin One", city = "الخرطوم"),
            emitPublic = true,
            adminDocExists = true
        )
        advanceUntilIdle()

        // Launch admin quota fetch for u1 while suspended
        harness.viewModel.selectTab(MainTab.ADMIN)
        runCurrent()

        // Switch account to u2 before u1's Cloudinary quota network request finishes
        harness.gateway.setUserClaims("u2", cachedAdmin = false, serverAdmin = false)
        harness.gateway.emitAuthState(AuthUserIdentity(uid = "u2", isEmailVerified = true))
        advanceUntilIdle()

        // Late u1 admin response arrives after u2 is already active -> must be dropped
        deferredQuota.complete(Result.success(CloudinaryQuotaStatus(creditsUsed = 42.0, videoUploadsEnabled = true)))
        advanceUntilIdle()

        val u2State = harness.viewModel.uiState.value
        assertEquals("u2", u2State.auth.uid)
        assertEquals(0.0, u2State.cloudinaryQuota.creditsUsed, 0.001)
        assertFalse(u2State.cloudinaryQuota.videoUploadsEnabled)

        uiJob.cancel()
        advanceTimeBy(6_000L)
        advanceUntilIdle()
    }

    @Test
    fun adminRevocationOnTabSelectAndNetworkReconnectExitsAdminUiImmediately() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }

        harness.gateway.setUserClaims("admin_1", cachedAdmin = true, serverAdmin = true)
        harness.gateway.emitAuthState(AuthUserIdentity(uid = "admin_1", isEmailVerified = true))
        advanceUntilIdle()
        harness.gateway.emitUserDocs(
            uid = "admin_1",
            publicUser = PublicUser(uid = "admin_1", name = "المشرف", city = "الخرطوم", accountType = AccountType.PERSONAL.wireValue),
            emitPublic = true,
            adminDocExists = true
        )
        advanceUntilIdle()

        harness.viewModel.selectTab(MainTab.ADMIN)
        advanceUntilIdle()
        assertTrue(harness.viewModel.uiState.value.isAdmin)
        assertEquals(MainTab.ADMIN, harness.viewModel.uiState.value.activeTab)

        // Admin goes offline, claim is revoked server-side while offline
        harness.isOnlineFlow.value = false
        harness.gateway.isNetworkOnline = false
        advanceUntilIdle()
        harness.gateway.setUserClaims("admin_1", cachedAdmin = true, serverAdmin = false)

        // Staying offline keeps session without crashing
        harness.viewModel.selectTab(MainTab.ADMIN)
        advanceUntilIdle()
        assertTrue(harness.viewModel.uiState.value.isAdmin)

        // Internet reconnects -> triggers admin re-verification -> boots out of Admin tab immediately
        harness.gateway.isNetworkOnline = true
        harness.isOnlineFlow.value = true
        advanceTimeBy(100L)
        runCurrent()

        val recoveredState = harness.viewModel.uiState.value
        assertFalse(recoveredState.isAdmin)
        assertFalse(recoveredState.auth.hasAdminClaim)
        assertEquals(MainTab.SWIPE, recoveredState.activeTab)
        assertNull(recoveredState.adminAuditConversation)
        assertTrue(recoveredState.toasts.any { it.type == ToastType.WARNING })

        advanceUntilIdle()
        uiJob.cancel()
        advanceTimeBy(6_000L)
        advanceUntilIdle()
        assertEquals(0, harness.gateway.activeAuthListenerCount)
        assertEquals(0, harness.gateway.activeIdTokenListenerCount)
        assertEquals(0, harness.gateway.activeUserDocsCount)
    }
}
