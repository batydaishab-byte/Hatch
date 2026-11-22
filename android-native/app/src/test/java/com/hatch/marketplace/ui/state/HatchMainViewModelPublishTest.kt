package com.hatch.marketplace.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.MainTab
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.ToastType
import com.hatch.marketplace.data.model.UserStatus
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import com.hatch.marketplace.data.repository.AuthRepository
import com.hatch.marketplace.data.repository.AuthUserIdentity
import com.hatch.marketplace.data.repository.FakeAuthSessionGateway
import com.hatch.marketplace.data.repository.ListingPublishRequest
import com.hatch.marketplace.data.repository.NoOpAuthSessionLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class HatchMainViewModelPublishTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private data class PublishHarness(
        val gateway: FakeAuthSessionGateway,
        val viewModelStore: ViewModelStore,
        val viewModel: HatchMainViewModel
    )

    private fun createHarness(
        scope: kotlinx.coroutines.CoroutineScope,
        publishListing: (suspend (ListingPublishRequest) -> Result<String>)? = null
    ): PublishHarness {
        val gateway = FakeAuthSessionGateway()
        val authRepo = AuthRepository(
            clients = FirebaseClientsHolder(app = null, auth = null, firestore = null),
            gateway = gateway,
            sessionScope = scope,
            logger = NoOpAuthSessionLogger,
            stopTimeoutMillis = 0L
        )
        val streams = HatchViewModelStreams(
            languageOverrideFlow = flowOf(AppLanguage.AR),
            isOnlineFlow = MutableStateFlow(true),
            publishListing = publishListing
        )
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = HatchMainViewModel(
                    authRepo = authRepo,
                    productRepo = null,
                    interactionRepo = null,
                    chatRepo = null,
                    monetizationRepo = null,
                    prefs = null,
                    streams = streams
                ) as T
            }
        )[HatchMainViewModel::class.java]

        return PublishHarness(
            gateway = gateway,
            viewModelStore = store,
            viewModel = vm
        )
    }

    private fun HatchMainViewModel.callPublishSample() {
        publishNewProduct(
            titleAr = "هاتف جالكسي S24",
            titleEn = "Galaxy S24",
            descriptionAr = "حالة ممتازة جداً مع الضمان",
            descriptionEn = "Excellent condition with warranty",
            price = 350000.0,
            originalPrice = 400000.0,
            category = "phones",
            condition = "like_new",
            city = "الخرطوم (Khartoum)",
            areaAr = "الرياض",
            localUris = emptyList(),
            remoteUrls = listOf("https://cdn.example.com/galaxy.webp"),
            paymentMethods = listOf("bankak", "cash"),
            negotiable = true,
            deliveryAvailable = false,
            urgentSale = false
        )
    }

    @Test
    fun publishNewProductDoesNotFailSilentlyWhenProfileOrRepositoryMissing() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope, publishListing = null)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            harness.gateway.setUserClaims("u1", cachedAdmin = false, serverAdmin = false)
            harness.gateway.emitAuthState(AuthUserIdentity(uid = "u1", isEmailVerified = true))
            runCurrent()

            // Signed in, but publicProfile not yet loaded
            harness.viewModel.openSellModal()
            harness.viewModel.callPublishSample()
            runCurrent()

            val missingProfileState = harness.viewModel.uiState.value
            assertTrue(missingProfileState.showSellModal)
            assertNotNull(missingProfileState.sellErrorMessage)
            assertTrue(missingProfileState.toasts.any { it.type == ToastType.ERROR })

            // Now load publicProfile, while productRepo & publishListing remain unavailable
            harness.gateway.emitUserDocs(
                uid = "u1",
                publicUser = PublicUser(uid = "u1", name = "أحمد", city = "الخرطوم"),
                emitPublic = true
            )
            runCurrent()

            harness.viewModel.clearSellError()
            harness.viewModel.callPublishSample()
            runCurrent()

            val missingRepoState = harness.viewModel.uiState.value
            assertTrue(missingRepoState.showSellModal)
            assertNotNull(missingRepoState.sellErrorMessage)
            assertFalse(missingRepoState.isSubmitting)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    @Test
    fun publishNewProductSurfacesPermissionNetworkAndUploadErrorsAndKeepsModalOpen() = runTest(mainDispatcher) {
        var nextResult: Result<String> = Result.failure(
            IllegalStateException("PERMISSION_DENIED: Missing or insufficient permissions.")
        )
        val harness = createHarness(backgroundScope) { nextResult }
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            harness.gateway.setUserClaims("u1", cachedAdmin = false, serverAdmin = false)
            harness.gateway.emitAuthState(AuthUserIdentity(uid = "u1", isEmailVerified = true))
            runCurrent()
            harness.gateway.emitUserDocs(
                uid = "u1",
                publicUser = PublicUser(uid = "u1", name = "أحمد", city = "الخرطوم"),
                emitPublic = true
            )
            runCurrent()

            harness.viewModel.openSellModal()
            runCurrent()

            // 1. Firestore permission denied
            harness.viewModel.callPublishSample()
            runCurrent()
            var state = harness.viewModel.uiState.value
            assertTrue(state.showSellModal)
            assertFalse(state.isSubmitting)
            assertTrue(state.sellErrorMessage.orEmpty().contains("رفض الخادم"))
            assertTrue(state.toasts.lastOrNull()?.type == ToastType.ERROR)

            // 2. Network error
            nextResult = Result.failure(UnknownHostException("Unable to resolve host"))
            harness.viewModel.callPublishSample()
            runCurrent()
            state = harness.viewModel.uiState.value
            assertTrue(state.showSellModal)
            assertFalse(state.isSubmitting)
            assertTrue(state.sellErrorMessage.orEmpty().contains("اتصال الإنترنت"))

            // 3. Local image upload signer error
            nextResult = Result.failure(
                IllegalArgumentException("Device image upload requires configuring MEDIA_SIGNER_URL")
            )
            harness.viewModel.callPublishSample()
            runCurrent()
            state = harness.viewModel.uiState.value
            assertTrue(state.showSellModal)
            assertFalse(state.isSubmitting)
            assertTrue(state.sellErrorMessage.orEmpty().contains("MEDIA_SIGNER_URL"))
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    @Test
    fun publishNewProductBlocksSuspendedSellerAndSucceedsForValidActiveSeller() = runTest(mainDispatcher) {
        val captured = mutableListOf<ListingPublishRequest>()
        val harness = createHarness(backgroundScope) { req ->
            captured.add(req)
            Result.success("prod_new_123")
        }
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            harness.gateway.setUserClaims("u1", cachedAdmin = false, serverAdmin = false)
            harness.gateway.emitAuthState(AuthUserIdentity(uid = "u1", isEmailVerified = true))
            runCurrent()

            // Suspended user must be blocked
            harness.gateway.emitUserDocs(
                uid = "u1",
                publicUser = PublicUser(
                    uid = "u1",
                    name = "مستخدم موقوف",
                    status = UserStatus.SUSPENDED.wireValue
                ),
                emitPublic = true
            )
            runCurrent()
            harness.viewModel.callPublishSample()
            runCurrent()
            assertTrue(captured.isEmpty())
            assertNotNull(harness.viewModel.uiState.value.sellErrorMessage)

            // Active user publishes successfully
            harness.gateway.emitUserDocs(
                uid = "u1",
                publicUser = PublicUser(
                    uid = "u1",
                    name = "أحمد التاجر",
                    city = "الخرطوم (Khartoum)",
                    status = UserStatus.ACTIVE.wireValue
                ),
                emitPublic = true
            )
            runCurrent()

            harness.viewModel.openSellModal()
            harness.viewModel.callPublishSample()
            runCurrent()

            assertEquals(1, captured.size)
            assertEquals("u1", captured.first().seller.uid)
            assertEquals(350000.0, captured.first().price, 0.001)
            val successState = harness.viewModel.uiState.value
            assertFalse(successState.showSellModal)
            assertNull(successState.sellErrorMessage)
            assertEquals(MainTab.BROWSE, successState.activeTab)
            assertFalse(successState.isSubmitting)
            assertTrue(successState.toasts.any { it.type == ToastType.SUCCESS })

            advanceUntilIdle()
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }
}
