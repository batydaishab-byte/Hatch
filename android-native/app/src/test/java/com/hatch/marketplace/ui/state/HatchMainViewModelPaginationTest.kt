package com.hatch.marketplace.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.google.firebase.Timestamp
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.Product
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import com.hatch.marketplace.data.repository.AuthRepository
import com.hatch.marketplace.data.repository.FakeAuthSessionGateway
import com.hatch.marketplace.data.repository.NoOpAuthSessionLogger
import com.hatch.marketplace.data.repository.ProductPageResult
import com.hatch.marketplace.data.repository.ProductRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HatchMainViewModelPaginationTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleProduct(index: Int, titleAr: String = "منتج رقم $index"): Product = Product(
        id = "prod_$index",
        titleAr = titleAr,
        titleEn = "Item $index",
        descriptionAr = "وصف تفصيلي للمنتج رقم $index",
        price = 100_000.0 + index * 1_000.0,
        category = "phones",
        condition = "good",
        city = "الخرطوم (ولاية الخرطوم)",
        sellerId = "seller_${index % 3}",
        sellerName = "بائع $index",
        status = "active",
        createdAt = Timestamp(1_700_000_000L - index * 60L, 0)
    )

    private fun createViewModel(
        scope: kotlinx.coroutines.CoroutineScope,
        observeProductsPage: (String?, Boolean, Int) -> kotlinx.coroutines.flow.Flow<ProductPageResult>
    ): Pair<ViewModelStore, HatchMainViewModel> {
        val gateway = FakeAuthSessionGateway()
        val authRepo = AuthRepository(
            clients = FirebaseClientsHolder(app = null, auth = null, firestore = null),
            gateway = gateway,
            sessionScope = scope,
            logger = NoOpAuthSessionLogger,
            stopTimeoutMillis = 0L
        )
        val streams = HatchViewModelStreams(
            observeProductsPage = observeProductsPage,
            languageOverrideFlow = flowOf(AppLanguage.AR)
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
        return store to vm
    }

    @Test
    fun loadMoreProductsExpandsLimitBy20DeduplicatesInFlightCallsAndStopsWhenHasMoreFalse() = runTest(mainDispatcher) {
        val observedLimits = mutableListOf<Int>()
        val page40Deferred = CompletableDeferred<ProductPageResult>()
        val first20 = (1..20).map { sampleProduct(it) }
        val total35 = (1..35).map { sampleProduct(it) }

        val (store, vm) = createViewModel(backgroundScope) { _, _, limit ->
            observedLimits.add(limit)
            flow {
                when (limit) {
                    20 -> emit(ProductPageResult(products = first20, hasMore = true, requestedLimit = 20))
                    40 -> emit(page40Deferred.await())
                    else -> emit(ProductPageResult(products = total35, hasMore = false, requestedLimit = limit))
                }
            }
        }
        val uiJob = launch { vm.uiState.collect {} }
        try {
            runCurrent()
            assertEquals(listOf(ProductRepository.PAGE_SIZE), observedLimits)
            assertEquals(20, vm.uiState.value.products.size)
            assertEquals(20, vm.uiState.value.productsLimit)
            assertTrue(vm.uiState.value.hasMoreProducts)
            assertFalse(vm.uiState.value.isLoadingMore)

            // First loadMore expands limit from 20 to 40
            vm.loadMoreProducts()
            runCurrent()
            assertEquals(listOf(20, 40), observedLimits)
            assertTrue(vm.uiState.value.isLoadingMore)

            // Duplicate call while isLoadingMore == true must be ignored
            vm.loadMoreProducts()
            runCurrent()
            assertEquals(listOf(20, 40), observedLimits)

            // Complete 40-limit page with 35 total items -> hasMore becomes false
            page40Deferred.complete(ProductPageResult(products = total35, hasMore = false, requestedLimit = 40))
            runCurrent()
            advanceUntilIdle()

            assertEquals(35, vm.uiState.value.products.size)
            assertEquals(40, vm.uiState.value.productsLimit)
            assertFalse(vm.uiState.value.isLoadingMore)
            assertFalse(vm.uiState.value.hasMoreProducts)

            // Subsequent loadMore when hasMore == false must be ignored
            vm.loadMoreProducts()
            runCurrent()
            assertEquals(listOf(20, 40), observedLimits)
        } finally {
            uiJob.cancel()
            store.clear()
        }
    }

    @Test
    fun updateSearchQueryAppliesDebounceWithoutExtraReadsAndFiltersInMemory() = runTest(mainDispatcher) {
        val observedLimits = mutableListOf<Int>()
        val catalog = (1..20).map { idx ->
            val title = if (idx <= 3) "آيفون 15 برو رقم $idx" else "جهاز لابتوب ديل رقم $idx"
            sampleProduct(idx, titleAr = title)
        }

        val (store, vm) = createViewModel(backgroundScope) { _, _, limit ->
            observedLimits.add(limit)
            flowOf(ProductPageResult(products = catalog, hasMore = true, requestedLimit = limit))
        }
        val uiJob = launch { vm.uiState.collect {} }
        try {
            runCurrent()
            assertEquals(20, vm.uiState.value.filteredMarketProducts.size)
            assertEquals(listOf(20), observedLimits)

            // Rapid typing resets debounce timer and triggers zero extra queries
            vm.updateSearchQuery("آي")
            advanceTimeBy(250L)
            runCurrent()
            assertEquals("", vm.uiState.value.filter.searchQuery)
            assertEquals(20, vm.uiState.value.filteredMarketProducts.size)

            vm.updateSearchQuery("آيفون")
            advanceTimeBy(300L)
            runCurrent()
            assertEquals("", vm.uiState.value.filter.searchQuery)

            // Once 450ms elapses after final keystroke, filter applies in memory
            advanceTimeBy(200L)
            runCurrent()
            assertEquals("آيفون", vm.uiState.value.filter.searchQuery)
            assertEquals(3, vm.uiState.value.filteredMarketProducts.size)
            assertEquals(listOf(20), observedLimits)

            // Immediate clear applies synchronously without debounce delay
            vm.updateSearchQuery("", immediate = true)
            runCurrent()
            assertEquals("", vm.uiState.value.filter.searchQuery)
            assertEquals(20, vm.uiState.value.filteredMarketProducts.size)
            assertEquals(listOf(20), observedLimits)
        } finally {
            uiJob.cancel()
            store.clear()
        }
    }

    @Test
    fun swipeDeckPrefetchLoadsNextBatchAndKeepsPassedCardsExcluded() = runTest(mainDispatcher) {
        val first20 = (1..20).map { sampleProduct(it) }
        val nextBatch = (1..30).map { sampleProduct(it) }

        val (store, vm) = createViewModel(backgroundScope) { _, _, limit ->
            if (limit <= 20) {
                flowOf(ProductPageResult(products = first20, hasMore = true, requestedLimit = 20))
            } else {
                flowOf(ProductPageResult(products = nextBatch, hasMore = false, requestedLimit = limit))
            }
        }
        val uiJob = launch { vm.uiState.collect {} }
        try {
            runCurrent()
            assertEquals(20, vm.uiState.value.swipeDeckProducts.size)

            // Swipe first 17 cards until 3 remain
            first20.take(17).forEach { vm.browseNextSwipeCard(it) }
            runCurrent()
            assertEquals(3, vm.uiState.value.swipeDeckProducts.size)
            assertTrue(vm.uiState.value.hasMoreProducts)

            // Low-card prefetch triggers next 20 batch while preserving passedProductIds
            vm.loadMoreProducts()
            runCurrent()
            advanceUntilIdle()

            assertEquals(13, vm.uiState.value.swipeDeckProducts.size)
            assertFalse(vm.uiState.value.hasMoreProducts)
            assertFalse(vm.uiState.value.isLoadingMore)
            assertTrue(vm.uiState.value.swipeDeckProducts.none { it.id == "prod_1" })
        } finally {
            uiJob.cancel()
            store.clear()
        }
    }
}
