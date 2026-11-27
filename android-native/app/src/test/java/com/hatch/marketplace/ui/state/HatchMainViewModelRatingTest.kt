package com.hatch.marketplace.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.Conversation
import com.hatch.marketplace.data.model.OfferStatus
import com.hatch.marketplace.data.model.PrivateUser
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.ToastType
import com.hatch.marketplace.data.model.UserReview
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import com.hatch.marketplace.data.repository.AuthRepository
import com.hatch.marketplace.data.repository.AuthUserIdentity
import com.hatch.marketplace.data.repository.FakeAuthSessionGateway
import com.hatch.marketplace.data.repository.NoOpAuthSessionLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class HatchMainViewModelRatingTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private data class RatingHarness(
        val gateway: FakeAuthSessionGateway,
        val productsFlow: MutableStateFlow<List<Product>>,
        val reviewsFlow: MutableStateFlow<List<UserReview>>,
        val conversationsFlow: MutableStateFlow<List<Conversation>>,
        val publicUsersFlow: MutableStateFlow<List<PublicUser>>,
        val commercialProfilesFlow: MutableStateFlow<List<PublicUser>>,
        val submittedRequests: MutableList<ReviewSubmitRequest>,
        val viewModelStore: ViewModelStore,
        val viewModel: HatchMainViewModel
    )

    private fun createHarness(scope: kotlinx.coroutines.CoroutineScope): RatingHarness {
        val gateway = FakeAuthSessionGateway()
        val authRepo = AuthRepository(
            clients = FirebaseClientsHolder(app = null, auth = null, firestore = null),
            gateway = gateway,
            sessionScope = scope,
            logger = NoOpAuthSessionLogger,
            stopTimeoutMillis = 0L
        )
        val productsFlow = MutableStateFlow(
            listOf(
                Product(
                    id = "prod_1",
                    titleAr = "هاتف سامسونج",
                    sellerId = "seller_1",
                    sellerName = "متجر النيل",
                    sellerAccountType = AccountType.STORE.wireValue,
                    sellerRating = 5.0, // Legacy/raw snapshot value must be overridden by actual reviews
                    status = ProductStatus.SOLD.wireValue
                )
            )
        )
        val reviewsFlow = MutableStateFlow<List<UserReview>>(emptyList())
        val conversationsFlow = MutableStateFlow(
            listOf(
                Conversation(
                    id = "conv_1",
                    productId = "prod_1",
                    productTitle = "هاتف سامسونج",
                    buyerId = "buyer_1",
                    sellerId = "seller_1",
                    sellerName = "متجر النيل",
                    participantIds = listOf("buyer_1", "seller_1"),
                    offerStatus = OfferStatus.ACCEPTED.wireValue
                )
            )
        )
        val publicUsersFlow = MutableStateFlow(
            listOf(
                PublicUser(
                    uid = "seller_1",
                    name = "متجر النيل",
                    city = "الخرطوم",
                    accountType = AccountType.STORE.wireValue,
                    rating = 5.0 // Raw Firestore wire value must NOT show when 0 reviews exist
                )
            )
        )
        val commercialProfilesFlow = MutableStateFlow(
            listOf(
                PublicUser(
                    uid = "seller_1",
                    name = "متجر النيل",
                    city = "الخرطوم",
                    accountType = AccountType.STORE.wireValue,
                    rating = 5.0
                )
            )
        )
        val submittedRequests = mutableListOf<ReviewSubmitRequest>()

        val streams = HatchViewModelStreams(
            observeProducts = { _, _ -> productsFlow },
            observeConversations = { _, _ -> conversationsFlow },
            observeReviews = { reviewsFlow },
            observePublicUsers = { _, _ -> publicUsersFlow },
            observeCommercialProfiles = { commercialProfilesFlow },
            submitReview = { req ->
                submittedRequests.add(req)
                val newReview = UserReview(
                    id = "${req.productId}_${req.reviewerId}",
                    sellerId = req.sellerId,
                    reviewerId = req.reviewerId,
                    reviewerName = req.reviewerName,
                    reviewerAvatar = req.reviewerAvatar,
                    productId = req.productId,
                    conversationId = req.conversationId,
                    productTitle = req.productTitle,
                    rating = req.rating,
                    comment = req.comment,
                    tags = req.tags
                )
                reviewsFlow.value = reviewsFlow.value + newReview
                Result.success(Unit)
            }
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

        return RatingHarness(
            gateway = gateway,
            productsFlow = productsFlow,
            reviewsFlow = reviewsFlow,
            conversationsFlow = conversationsFlow,
            publicUsersFlow = publicUsersFlow,
            commercialProfilesFlow = commercialProfilesFlow,
            submittedRequests = submittedRequests,
            viewModelStore = store,
            viewModel = vm
        )
    }

    private fun signInUser(
        harness: RatingHarness,
        uid: String,
        name: String,
        rawRating: Double? = 5.0
    ) {
        harness.gateway.setUserClaims(uid, cachedAdmin = false, serverAdmin = false)
        harness.gateway.emitAuthState(AuthUserIdentity(uid = uid, isEmailVerified = true))
        harness.gateway.emitUserDocs(
            uid = uid,
            publicUser = PublicUser(uid = uid, name = name, city = "الخرطوم", rating = rawRating),
            privateUser = PrivateUser(uid = uid, phone = "+249912345678"),
            emitPublic = true,
            emitPrivate = true
        )
    }

    // 1. Account with 0 reviews -> rating must be empty (null), never 5.0
    @Test
    fun zeroRatingsLeavesSellerAndProductRatingsEmpty() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            signInUser(harness, uid = "seller_1", name = "متجر النيل", rawRating = 5.0)
            runCurrent()
            advanceUntilIdle()

            val state = harness.viewModel.uiState.value
            assertNull(state.currentUser?.rating)
            assertNull(state.allUsers.first { it.uid == "seller_1" }.rating)
            assertNull(state.commercialProfiles.first { it.uid == "seller_1" }.rating)
            assertNull(state.products.first { it.id == "prod_1" }.sellerRating)
            assertEquals(0, state.products.first { it.id == "prod_1" }.sellerReviewsCount)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 2. Single 5-star review -> rating = 5.0
    @Test
    fun singleFiveStarReviewSetsAverageToFive() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            harness.reviewsFlow.value = listOf(
                UserReview(id = "prod_1_buyer_1", sellerId = "seller_1", reviewerId = "buyer_1", productId = "prod_1", rating = 5)
            )
            runCurrent()
            advanceUntilIdle()

            val state = harness.viewModel.uiState.value
            assertEquals(5.0, state.allUsers.first { it.uid == "seller_1" }.rating!!, 0.0001)
            assertEquals(5.0, state.products.first { it.id == "prod_1" }.sellerRating!!, 0.0001)
            assertEquals(1, state.products.first { it.id == "prod_1" }.sellerReviewsCount)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 3. Single 3-star review -> rating = 3.0
    @Test
    fun singleThreeStarReviewSetsAverageToThree() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            harness.reviewsFlow.value = listOf(
                UserReview(id = "prod_1_buyer_1", sellerId = "seller_1", reviewerId = "buyer_1", productId = "prod_1", rating = 3)
            )
            runCurrent()
            advanceUntilIdle()

            val state = harness.viewModel.uiState.value
            assertEquals(3.0, state.allUsers.first { it.uid == "seller_1" }.rating!!, 0.0001)
            assertEquals(3.0, state.products.first { it.id == "prod_1" }.sellerRating!!, 0.0001)
            assertEquals(1, state.products.first { it.id == "prod_1" }.sellerReviewsCount)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 4. Multiple reviews -> exact arithmetic average
    @Test
    fun multipleReviewsCalculateExactArithmeticAverage() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            harness.reviewsFlow.value = listOf(
                UserReview(id = "r1", sellerId = "seller_1", reviewerId = "b1", productId = "p1", rating = 5),
                UserReview(id = "r2", sellerId = "seller_1", reviewerId = "b2", productId = "p2", rating = 4)
            )
            runCurrent()
            advanceUntilIdle()

            assertEquals(4.5, harness.viewModel.uiState.value.allUsers.first { it.uid == "seller_1" }.rating!!, 0.0001)

            harness.reviewsFlow.value = harness.reviewsFlow.value + listOf(
                UserReview(id = "r3", sellerId = "seller_1", reviewerId = "b3", productId = "p3", rating = 2),
                UserReview(id = "r4", sellerId = "seller_1", reviewerId = "b4", productId = "p4", rating = 1)
            )
            runCurrent()
            advanceUntilIdle()

            val state = harness.viewModel.uiState.value
            // (5 + 4 + 2 + 1) / 4 = 3.0
            assertEquals(3.0, state.allUsers.first { it.uid == "seller_1" }.rating!!, 0.0001)
            assertEquals(3.0, state.products.first { it.id == "prod_1" }.sellerRating!!, 0.0001)
            assertEquals(4, state.products.first { it.id == "prod_1" }.sellerReviewsCount)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 5. Clicking rating area opens dialog with 0 unselected stars
    @Test
    fun clickingRatingAreaOpensDialogWithZeroUnselectedStars() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            signInUser(harness, uid = "buyer_1", name = "مشتري")
            runCurrent()
            advanceUntilIdle()

            harness.viewModel.openReviewModal(
                ReviewRequest(
                    sellerId = "seller_1",
                    sellerName = "متجر النيل",
                    productId = "prod_1"
                )
            )
            runCurrent()

            val req = harness.viewModel.uiState.value.reviewRequest
            assertNotNull(req)
            assertEquals(0, req?.initialRating)
            assertFalse(req?.isAlreadyReviewed == true)
            assertEquals("prod_1", req?.productId)
            assertEquals("conv_1", req?.conversationId)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 6. Selecting 2 stars saves 2 stars and immediately updates seller/product rating to 2.0
    @Test
    fun selectingTwoStarsSavesReviewAndImmediatelyUpdatesAverageToTwo() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            signInUser(harness, uid = "buyer_1", name = "مشتري")
            harness.viewModel.openProductDetail(harness.productsFlow.value.first())
            runCurrent()
            advanceUntilIdle()

            assertNull(harness.viewModel.uiState.value.selectedProduct?.sellerRating)

            harness.viewModel.openReviewModal(
                ReviewRequest(sellerId = "seller_1", sellerName = "متجر النيل")
            )
            runCurrent()
            harness.viewModel.submitReview(rating = 2, comment = "التجربة مقبولة", tags = listOf("صادق"))
            runCurrent()
            advanceUntilIdle()

            assertEquals(1, harness.submittedRequests.size)
            assertEquals(2, harness.submittedRequests.first().rating)

            val state = harness.viewModel.uiState.value
            assertNull(state.reviewRequest)
            assertEquals(2.0, state.allUsers.first { it.uid == "seller_1" }.rating!!, 0.0001)
            assertEquals(2.0, state.commercialProfiles.first { it.uid == "seller_1" }.rating!!, 0.0001)
            assertEquals(2.0, state.products.first { it.id == "prod_1" }.sellerRating!!, 0.0001)
            assertEquals(2.0, state.selectedProduct?.sellerRating!!, 0.0001)
            assertEquals(1, state.selectedProduct?.sellerReviewsCount)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 7. Reopening after rating displays prior star rating and prevents duplicate rating
    @Test
    fun reopeningRatingShowsPriorRatingAndBlocksDuplicateSubmission() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            signInUser(harness, uid = "buyer_1", name = "مشتري")
            runCurrent()
            advanceUntilIdle()

            harness.viewModel.openReviewModal(
                ReviewRequest(sellerId = "seller_1", sellerName = "متجر النيل", productId = "prod_1")
            )
            runCurrent()
            harness.viewModel.submitReview(rating = 2, comment = "تعامل جيد", tags = emptyList())
            runCurrent()
            advanceUntilIdle()
            assertEquals(1, harness.submittedRequests.size)

            // Reopen from product detail or seller profile
            harness.viewModel.openReviewModal(
                ReviewRequest(sellerId = "seller_1", sellerName = "متجر النيل", productId = "prod_1")
            )
            runCurrent()

            val reopenedReq = harness.viewModel.uiState.value.reviewRequest
            assertNotNull(reopenedReq)
            assertEquals(2, reopenedReq?.initialRating)
            assertTrue(reopenedReq?.isAlreadyReviewed == true)
            assertEquals("تعامل جيد", reopenedReq?.existingComment)

            // Attempting to submit another rating for the same target is blocked
            harness.viewModel.submitReview(rating = 5, comment = "تقييم مكرر", tags = emptyList())
            runCurrent()
            advanceUntilIdle()

            assertEquals(1, harness.submittedRequests.size)
            assertEquals(2.0, harness.viewModel.uiState.value.allUsers.first { it.uid == "seller_1" }.rating!!, 0.0001)
            assertTrue(harness.viewModel.uiState.value.toasts.any { it.type == ToastType.INFO })
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 8. Self-rating is blocked
    @Test
    fun selfRatingIsBlockedOnOpenAndSubmit() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            signInUser(harness, uid = "seller_1", name = "متجر النيل")
            runCurrent()
            advanceUntilIdle()

            harness.viewModel.openReviewModal(
                ReviewRequest(
                    sellerId = "seller_1",
                    sellerName = "متجر النيل",
                    productId = "prod_1"
                )
            )
            runCurrent()

            val state = harness.viewModel.uiState.value
            assertNull(state.reviewRequest)
            assertTrue(state.toasts.any { it.type == ToastType.WARNING })
            assertTrue(harness.submittedRequests.isEmpty())
            assertNull(state.currentUser?.rating)
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }

    // 9. Account creation / Google linking / onboarding never auto-creates a 5-star rating
    @Test
    fun accountCreationGoogleLinkingAndOnboardingNeverAutoCreateFiveStarRating() = runTest(mainDispatcher) {
        val harness = createHarness(backgroundScope)
        val uiJob = launch { harness.viewModel.uiState.collect {} }
        try {
            // New account before onboarding
            harness.gateway.setUserClaims("brand_new_uid", cachedAdmin = false, serverAdmin = false)
            harness.gateway.emitAuthState(
                AuthUserIdentity(
                    uid = "brand_new_uid",
                    displayName = "حساب جوجل جديد",
                    email = "new@example.com",
                    isEmailVerified = true
                )
            )
            runCurrent()
            advanceUntilIdle()

            // Simulate Firestore rule-mandated initial users_public write returning rating = 5.0 after Google sign-in/onboarding
            harness.gateway.emitUserDocs(
                uid = "brand_new_uid",
                publicUser = PublicUser(
                    uid = "brand_new_uid",
                    name = "حساب جوجل جديد",
                    city = "الخرطوم (ولاية الخرطوم)",
                    rating = 5.0,
                    accountType = AccountType.STORE.wireValue
                ),
                privateUser = PrivateUser(uid = "brand_new_uid", phone = "+249912345678"),
                emitPublic = true,
                emitPrivate = true
            )
            harness.publicUsersFlow.value = harness.publicUsersFlow.value + PublicUser(
                uid = "brand_new_uid",
                name = "حساب جوجل جديد",
                city = "الخرطوم (ولاية الخرطوم)",
                rating = 5.0,
                accountType = AccountType.STORE.wireValue
            )
            harness.commercialProfilesFlow.value = harness.commercialProfilesFlow.value + PublicUser(
                uid = "brand_new_uid",
                name = "حساب جوجل جديد",
                city = "الخرطوم (ولاية الخرطوم)",
                rating = 5.0,
                accountType = AccountType.STORE.wireValue
            )
            runCurrent()
            advanceUntilIdle()

            val state = harness.viewModel.uiState.value
            assertTrue(state.isSignedIn)
            assertFalse(state.isOnboardingRequired)
            assertNull(state.currentUser?.rating)
            assertNull(state.allUsers.first { it.uid == "brand_new_uid" }.rating)
            assertNull(state.commercialProfiles.first { it.uid == "brand_new_uid" }.rating)
            assertTrue(state.reviews.isEmpty())
        } finally {
            uiJob.cancel()
            harness.viewModelStore.clear()
        }
    }
}
