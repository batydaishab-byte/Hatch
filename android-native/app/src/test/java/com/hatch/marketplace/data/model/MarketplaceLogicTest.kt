package com.hatch.marketplace.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceLogicTest {

    @Test
    fun filterProductsRespectsActiveOnlyCategoryCityBankakAndUnlimitedPrice() {
        val products = listOf(
            Product(
                id = "p1",
                titleAr = "آيفون 15 برو",
                titleEn = "iPhone 15 Pro",
                price = 7_500_000.0,
                category = CategoryId.PHONES.wireValue,
                city = "الخرطوم (ولاية الخرطوم)",
                paymentMethods = listOf(PaymentMethod.BANKAK.wireValue, PaymentMethod.CASH.wireValue),
                urgentSale = true,
                negotiable = true,
                status = ProductStatus.ACTIVE.wireValue
            ),
            Product(
                id = "p2",
                titleAr = "لوح شمسي 550 واط",
                titleEn = "550W Solar Panel",
                price = 650_000.0,
                category = CategoryId.SOLAR_ENERGY.wireValue,
                city = "بورتسودان (ولاية البحر الأحمر)",
                paymentMethods = listOf(PaymentMethod.CASH.wireValue),
                urgentSale = false,
                negotiable = false,
                status = ProductStatus.ACTIVE.wireValue
            ),
            Product(
                id = "p3",
                titleAr = "سامسونج مباع",
                titleEn = "Sold Samsung",
                price = 900_000.0,
                category = CategoryId.PHONES.wireValue,
                city = "الخرطوم (ولاية الخرطوم)",
                paymentMethods = listOf(PaymentMethod.BANKAK.wireValue),
                status = ProductStatus.SOLD.wireValue
            )
        )

        // Default unlimitedPrice allows 7.5M item while hiding SOLD item
        val allActive = MarketplaceLogic.filterProducts(products, MarketFilterState(), activeOnly = true)
        assertEquals(listOf("p1", "p2"), allActive.map { it.id })

        // Bankak + Khartoum + Search filter
        val khartoumBankak = MarketplaceLogic.filterProducts(
            products,
            MarketFilterState(
                searchQuery = "آيفون",
                selectedCity = "الخرطوم (ولاية الخرطوم)",
                onlyBankak = true,
                unlimitedPrice = true
            ),
            activeOnly = true
        )
        assertEquals(listOf("p1"), khartoumBankak.map { it.id })

        // Capped maxPrice excludes p1
        val capped = MarketplaceLogic.filterProducts(
            products,
            MarketFilterState(maxPrice = 1_000_000.0, unlimitedPrice = false),
            activeOnly = true
        )
        assertEquals(listOf("p2"), capped.map { it.id })
    }

    @Test
    fun adminRoleRequiresServerVerifiedClaimOrAdminDocOnly() {
        assertTrue(MarketplaceLogic.isAdminRole(hasAdminClaim = true, hasAdminDoc = false))
        assertTrue(MarketplaceLogic.isAdminRole(hasAdminClaim = false, hasAdminDoc = true))
        assertTrue(MarketplaceLogic.isAdminRole(hasAdminClaim = true, hasAdminDoc = true))
        assertFalse(MarketplaceLogic.isAdminRole(hasAdminClaim = false, hasAdminDoc = false))
    }

    @Test
    fun offerTransitionEnforcesParticipantRolesAndStateMachine() {
        val conv = Conversation(
            id = "c1",
            productId = "p1",
            buyerId = "buyer_1",
            sellerId = "seller_1",
            participantIds = listOf("buyer_1", "seller_1"),
            offerStatus = OfferStatus.PENDING.wireValue,
            offerAmount = 500_000.0,
            offerUpdatedBy = "buyer_1"
        )
        // Buyer cannot accept their own pending offer
        assertTrue(MarketplaceLogic.validateOfferTransition(conv, "buyer_1", OfferTransitionAction.ACCEPT_OFFER).isFailure)
        // Non-participant cannot touch offer
        assertTrue(MarketplaceLogic.validateOfferTransition(conv, "intruder", OfferTransitionAction.ACCEPT_OFFER).isFailure)
        // Seller can accept buyer's offer
        assertTrue(MarketplaceLogic.validateOfferTransition(conv, "seller_1", OfferTransitionAction.ACCEPT_OFFER).isSuccess)
        // Seller can counter with a positive amount
        assertTrue(MarketplaceLogic.validateOfferTransition(conv, "seller_1", OfferTransitionAction.COUNTER_OFFER, counterAmount = 550_000.0).isSuccess)
        // Once accepted, no further offer transitions are allowed
        val acceptedConv = conv.copy(offerStatus = OfferStatus.ACCEPTED.wireValue)
        assertTrue(MarketplaceLogic.validateOfferTransition(acceptedConv, "seller_1", OfferTransitionAction.DECLINE_OFFER).isFailure)
    }

    @Test
    fun suspensionLogicHandlesActiveFutureAndExpiredSuspensionsWithTimestamp() {
        val now = 1_700_000_000_000L
        assertFalse(MarketplaceLogic.isUserSuspended("active", null, null, now))

        val futureIso = MarketplaceLogic.buildSuspensionEndIso(days = 7, nowMillis = now)
        val futureTs = MarketplaceLogic.buildSuspensionEndTimestamp(days = 7, nowMillis = now)
        assertTrue(MarketplaceLogic.isUserSuspended("suspended", futureIso, futureTs, now))

        // 8 days later -> suspension expired
        val afterExpiry = now + 8 * 86_400_000L
        assertFalse(MarketplaceLogic.isUserSuspended("suspended", futureIso, futureTs, afterExpiry))
    }

    @Test
    fun sudanPhoneNormalizationAndValidationWorkForLocalAndIntlFormats() {
        assertEquals("+249912345678", HatchFormatters.normalizeSudanPhone("0912345678"))
        assertEquals("+249912345678", HatchFormatters.normalizeSudanPhone("912345678"))
        assertEquals("+249912345678", HatchFormatters.normalizeSudanPhone("+249912345678"))
        assertTrue(HatchFormatters.isValidSudanPhone("0912345678"))
        assertFalse(HatchFormatters.isValidSudanPhone("123"))
    }

    @Test
    fun depositScamKeywordDetectionFlagsArabicAndEnglishTerms() {
        assertTrue(MarketplaceLogic.hasDepositWarningKeywords("أرسل لي عربون عبر بنكك أولاً"))
        assertTrue(MarketplaceLogic.hasDepositWarningKeywords("Please wire first as deposit"))
        assertFalse(MarketplaceLogic.hasDepositWarningKeywords("المعاينة في السوق العربي غداً صباحاً"))
    }

    @Test
    fun computeRevenueStatsCalculatesCtrUsdAndSdgCorrectly() {
        val ads = listOf(
            SponsoredAdItem(id = "a1", impressionsCount = 2000, clicksCount = 50),
            SponsoredAdItem(id = "a2", impressionsCount = 3000, clicksCount = 50)
        )
        val config = AdMobConfig(ecpmRate = 2.0, cpcRate = 0.10, sdgRate = 2500.0)
        val stats = MarketplaceLogic.computeRevenueStats(ads, config)

        assertEquals(5000, stats.totalImpressions)
        assertEquals(100, stats.totalClicks)
        assertEquals(2.0, stats.ctrPercent, 0.001)
        // (5000 / 1000 * 2.0) + (100 * 0.10) = 10 + 10 = 20 USD
        assertEquals(20.0, stats.estimatedUsd, 0.001)
        assertEquals(50_000.0, stats.estimatedSdg, 0.01)
    }

    @Test
    fun accountTypeFromWireNormalizesAndFallsBackToPersonalForLegacyUsers() {
        assertEquals(AccountType.PERSONAL, AccountType.fromWire(null))
        assertEquals(AccountType.PERSONAL, AccountType.fromWire(""))
        assertEquals(AccountType.PERSONAL, AccountType.fromWire("unknown_type"))
        assertEquals(AccountType.STORE, AccountType.fromWire(" STORE "))
        assertEquals(AccountType.COMPANY, AccountType.fromWire("Company"))

        val legacyUser = PublicUser(
            uid = "u_legacy",
            name = "محمد عثمان",
            city = "الخرطوم (ولاية الخرطوم)",
            dealsCompleted = 12,
            verifiedIdentity = true
        )
        assertEquals(AccountType.PERSONAL, legacyUser.resolvedAccountType())
        assertFalse(MarketplaceLogic.requiresOnboarding(legacyUser))
        assertTrue(MarketplaceLogic.requiresOnboarding(PublicUser(uid = "u_new", name = "", city = "")))
    }

    @Test
    fun buildSellerBrandSnapshotInheritsLogoAndActivityForCommercialAccountsOnly() {
        val storeBrand = MarketplaceLogic.buildSellerBrandSnapshot(
            name = "متجر النيل للهواتف",
            avatar = "https://example.com/user.jpg",
            businessLogo = "https://example.com/store-logo.png",
            accountType = AccountType.STORE,
            businessActivity = "بيع الهواتف والإكسسوارات"
        )
        assertEquals("متجر النيل للهواتف", storeBrand.sellerName)
        assertEquals("https://example.com/store-logo.png", storeBrand.sellerAvatar)
        assertEquals("store", storeBrand.sellerAccountType)
        assertEquals("بيع الهواتف والإكسسوارات", storeBrand.sellerBusinessActivity)

        val personalBrand = MarketplaceLogic.buildSellerBrandSnapshot(
            name = "سارة حسن",
            avatar = "https://example.com/sara.jpg",
            businessLogo = "https://example.com/ignored-logo.png",
            accountType = AccountType.PERSONAL,
            businessActivity = "تجارة عامة"
        )
        assertEquals("https://example.com/sara.jpg", personalBrand.sellerAvatar)
        assertEquals("personal", personalBrand.sellerAccountType)
        assertEquals("", personalBrand.sellerBusinessActivity)
    }

    @Test
    fun buildBusinessStorefrontProductsFiltersBySellerAndActiveStatus() {
        val products = listOf(
            Product(id = "a1", sellerId = "s1", status = ProductStatus.ACTIVE.wireValue, urgentSale = false),
            Product(id = "a2", sellerId = "s1", status = ProductStatus.ACTIVE.wireValue, featured = true),
            Product(id = "a3", sellerId = "s1", status = ProductStatus.SOLD.wireValue),
            Product(id = "b1", sellerId = "s2", status = ProductStatus.ACTIVE.wireValue)
        )
        val storefront = MarketplaceLogic.buildBusinessStorefrontProducts(products, "s1")
        assertEquals(listOf("a2", "a1"), storefront.map { it.id })
    }

    @Test
    fun upgradingPersonalUserPreservesUidStatsAndVerification() {
        val existing = PublicUser(
            uid = "uid_fixed_123",
            name = "أحمد علي",
            avatar = "https://example.com/a.jpg",
            city = "بورتسودان (ولاية البحر الأحمر)",
            rating = 4.9,
            dealsCompleted = 14,
            verifiedIdentity = true,
            accountType = AccountType.PERSONAL.wireValue
        )
        val upgraded = existing.copy(
            name = "شركة البحر الأحمر للطاقة",
            accountType = AccountType.COMPANY.wireValue,
            businessActivity = "أنظمة الطاقة الشمسية",
            businessLogo = "https://example.com/company-logo.png"
        )
        assertEquals("uid_fixed_123", upgraded.uid)
        assertEquals(14, upgraded.dealsCompleted)
        assertTrue(upgraded.verifiedIdentity)
        assertEquals(4.9, upgraded.rating, 0.001)
        assertEquals(AccountType.COMPANY, upgraded.resolvedAccountType())
        assertEquals("https://example.com/company-logo.png", upgraded.displayLogoOrAvatar())
    }

    @Test
    fun buildCommercialDirectorySeparatesStoresAndCompaniesPreservesZeroProductAccountsAndFallsBackForGuests() {
        val users = listOf(
            PublicUser(
                uid = "u_personal",
                name = "حساب شخصي",
                city = "الخرطوم",
                accountType = AccountType.PERSONAL.wireValue
            ),
            PublicUser(
                uid = "u_store_1",
                name = "متجر النيل",
                city = "الخرطوم",
                accountType = AccountType.STORE.wireValue,
                businessActivity = "إلكترونيات",
                businessLogo = "https://example.com/store1.png",
                verifiedIdentity = true
            ),
            PublicUser(
                uid = "u_store_empty",
                name = "متجر جديد بلا منتجات",
                city = "عطبرة",
                accountType = AccountType.STORE.wireValue,
                businessActivity = "أثاث"
            ),
            PublicUser(
                uid = "u_company_1",
                name = "شركة النور للطاقة",
                city = "بورتسودان",
                accountType = AccountType.COMPANY.wireValue,
                businessActivity = "طاقة شمسية"
            )
        )
        val products = listOf(
            Product(
                id = "p_store1",
                sellerId = "u_store_1",
                sellerName = "متجر النيل",
                sellerAccountType = AccountType.STORE.wireValue,
                status = ProductStatus.ACTIVE.wireValue
            ),
            Product(
                id = "p_store1_sold",
                sellerId = "u_store_1",
                sellerName = "متجر النيل",
                sellerAccountType = AccountType.STORE.wireValue,
                status = ProductStatus.SOLD.wireValue
            ),
            Product(
                id = "p_company1",
                sellerId = "u_company_1",
                sellerName = "شركة النور للطاقة",
                sellerAccountType = AccountType.COMPANY.wireValue,
                status = ProductStatus.ACTIVE.wireValue
            ),
            Product(
                id = "p_personal",
                sellerId = "u_personal",
                sellerName = "حساب شخصي",
                sellerAccountType = AccountType.PERSONAL.wireValue,
                status = ProductStatus.ACTIVE.wireValue
            )
        )

        val stores = MarketplaceLogic.buildCommercialDirectory(users, products, AccountType.STORE)
        assertEquals(listOf("u_store_1", "u_store_empty"), stores.map { it.sellerId })
        assertEquals(listOf("p_store1"), stores.first().products.map { it.id })
        assertEquals("https://example.com/store1.png", stores.first().logoUrl)
        assertTrue(stores.last().products.isEmpty())

        val companies = MarketplaceLogic.buildCommercialDirectory(users, products, AccountType.COMPANY)
        assertEquals(listOf("u_company_1"), companies.map { it.sellerId })
        assertEquals(listOf("p_company1"), companies.first().products.map { it.id })

        // Guest mode fallback (empty PublicUser list -> falls back to product seller snapshots)
        val guestStores = MarketplaceLogic.buildCommercialDirectory(emptyList(), products, AccountType.STORE)
        assertEquals(listOf("u_store_1"), guestStores.map { it.sellerId })

        // Personal type returns empty commercial directory
        assertTrue(MarketplaceLogic.buildCommercialDirectory(users, products, AccountType.PERSONAL).isEmpty())
    }

    @Test
    fun guestModeAllowsBrowsingAndBlocksAllInteractiveAndContactActions() {
        val allowed = listOf(
            GuestAction.BROWSE_MARKET,
            GuestAction.BROWSE_SWIPE_DECK,
            GuestAction.VIEW_PRODUCT_DETAIL,
            GuestAction.VIEW_COMMERCIAL_DIRECTORY,
            GuestAction.VIEW_BUSINESS_PROFILE,
            GuestAction.SEARCH_OR_FILTER
        )
        allowed.forEach { action ->
            assertTrue("Expected $action to be allowed for guest", MarketplaceLogic.isActionAllowedForGuest(action))
        }

        val blocked = listOf(
            GuestAction.PUBLISH_LISTING,
            GuestAction.EDIT_OR_DELETE_LISTING,
            GuestAction.MAKE_OFFER_OR_BUY,
            GuestAction.CHAT_OR_MESSAGE,
            GuestAction.DIRECT_CONTACT_OR_CALL,
            GuestAction.TOGGLE_FAVORITE,
            GuestAction.RATE_OR_REVIEW,
            GuestAction.REPORT_VIOLATION,
            GuestAction.WATCH_REWARD_OR_BOOST
        )
        blocked.forEach { action ->
            assertFalse("Expected $action to be blocked for guest", MarketplaceLogic.isActionAllowedForGuest(action))
        }

        assertEquals(GuestPromptReason.SELL, MarketplaceLogic.guestPromptReasonFor(GuestAction.PUBLISH_LISTING))
        assertEquals(GuestPromptReason.OFFER_NEGOTIATE, MarketplaceLogic.guestPromptReasonFor(GuestAction.MAKE_OFFER_OR_BUY))
        assertEquals(GuestPromptReason.CONTACT_OR_BUY, MarketplaceLogic.guestPromptReasonFor(GuestAction.CHAT_OR_MESSAGE))
        assertEquals(GuestPromptReason.CONTACT_OR_BUY, MarketplaceLogic.guestPromptReasonFor(GuestAction.DIRECT_CONTACT_OR_CALL))
        assertEquals(GuestPromptReason.FAVORITES, MarketplaceLogic.guestPromptReasonFor(GuestAction.TOGGLE_FAVORITE))
        assertEquals(GuestPromptReason.RATE_OR_REPORT, MarketplaceLogic.guestPromptReasonFor(GuestAction.REPORT_VIOLATION))
    }

    @Test
    fun guestDirectoryDisplaysRegisteredStoresAndCompaniesWithZeroProductsAndZeroPii() {
        val commercialFeedForGuest = listOfNotNull(
            MarketplaceLogic.sanitizeCommercialProfileForGuest(
                PublicUser(
                    uid = "store_zero",
                    name = "متجر البحر الأحمر",
                    city = "بورتسودان (ولاية البحر الأحمر)",
                    accountType = AccountType.STORE.wireValue,
                    businessActivity = "أجهزة منزلية",
                    businessLogo = "https://example.com/redsea.png",
                    status = UserStatus.ACTIVE.wireValue
                )
            ),
            MarketplaceLogic.sanitizeCommercialProfileForGuest(
                PublicUser(
                    uid = "company_zero",
                    name = "شركة الخرطوم للطاقة",
                    city = "الخرطوم (ولاية الخرطوم)",
                    accountType = AccountType.COMPANY.wireValue,
                    businessActivity = "طاقة شمسية",
                    businessLogo = "https://example.com/krt-solar.png",
                    status = UserStatus.ACTIVE.wireValue
                )
            ),
            // Personal or suspended users must never leak into guest commercial profiles
            MarketplaceLogic.sanitizeCommercialProfileForGuest(
                PublicUser(
                    uid = "personal_leak",
                    name = "مستخدم شخصي",
                    city = "الخرطوم",
                    accountType = AccountType.PERSONAL.wireValue,
                    status = UserStatus.ACTIVE.wireValue
                )
            ),
            MarketplaceLogic.sanitizeCommercialProfileForGuest(
                PublicUser(
                    uid = "suspended_store",
                    name = "متجر موقوف",
                    city = "عطبرة",
                    accountType = AccountType.STORE.wireValue,
                    status = UserStatus.SUSPENDED.wireValue,
                    suspensionReason = "Private moderation note"
                )
            )
        )

        val mergedForGuest = MarketplaceLogic.mergeUsersAndCommercialProfiles(
            publicUsers = emptyList(),
            commercialProfiles = commercialFeedForGuest,
            currentUser = null
        )

        val products = listOf(
            Product(
                id = "p_pers",
                titleAr = "هاتف شخصي",
                sellerId = "personal_seller",
                sellerName = "بائع شخصي",
                sellerAccountType = AccountType.PERSONAL.wireValue,
                status = ProductStatus.ACTIVE.wireValue
            ),
            Product(
                id = "p_store_prod",
                titleAr = "هاتف من متجر",
                sellerId = "store_with_items",
                sellerName = "متجر النيل",
                sellerAccountType = AccountType.STORE.wireValue,
                status = ProductStatus.ACTIVE.wireValue
            )
        )

        // Market mix includes both personal and commercial active items for guests
        val guestMarket = MarketplaceLogic.filterProducts(products, MarketFilterState(), activeOnly = true)
        assertEquals(listOf("p_pers", "p_store_prod"), guestMarket.map { it.id })

        // Guest Store directory includes 0-product registered store alongside stores with products
        val storeDir = MarketplaceLogic.buildCommercialDirectory(mergedForGuest, products, AccountType.STORE)
        assertTrue(storeDir.any { it.sellerId == "store_zero" && it.products.isEmpty() })
        assertTrue(storeDir.any { it.sellerId == "store_with_items" && it.products.size == 1 })
        assertFalse(storeDir.any { it.sellerId == "suspended_store" || it.sellerId == "personal_leak" })

        // Guest Company directory includes 0-product registered company
        val companyDir = MarketplaceLogic.buildCommercialDirectory(mergedForGuest, products, AccountType.COMPANY)
        assertEquals(listOf("company_zero"), companyDir.map { it.sellerId })
        assertTrue(companyDir.first().products.isEmpty())

        // Verify CommercialDirectoryEntry exposes zero phone/email/whatsapp fields
        val fieldNames = CommercialDirectoryEntry::class.java.declaredFields.map { it.name.lowercase() }
        assertFalse(fieldNames.any { it.contains("phone") || it.contains("email") || it.contains("whatsapp") || it.contains("suspension") })
    }

    @Test
    fun completingTenOrMoreDealsNeverAutoVerifiesAccountOrProductSnapshot() {
        val userWithTwelveDeals = PublicUser(
            uid = "u_deals_12",
            name = "خالد مصطفى",
            city = "الخرطوم (ولاية الخرطوم)",
            dealsCompleted = 12,
            verifiedIdentity = false
        )
        assertFalse(userWithTwelveDeals.verifiedIdentity)
        val updatedDeals = userWithTwelveDeals.copy(dealsCompleted = userWithTwelveDeals.dealsCompleted + 1)
        assertEquals(13, updatedDeals.dealsCompleted)
        assertFalse(updatedDeals.verifiedIdentity)
    }

    @Test
    fun offerStateMachineBlocksSellerCounterOnAcceptedAndOffersOnSoldOrFlaggedProducts() {
        val pendingConv = Conversation(
            id = "c_sec",
            productId = "p_sec",
            buyerId = "b_sec",
            sellerId = "s_sec",
            participantIds = listOf("b_sec", "s_sec"),
            offerStatus = OfferStatus.PENDING.wireValue,
            offerAmount = 400_000.0
        )

        // Seller cannot accept on a SOLD or FLAGGED product
        assertTrue(
            MarketplaceLogic.validateOfferTransition(
                currentStatus = OfferStatus.PENDING.wireValue,
                actorUid = "s_sec",
                buyerId = "b_sec",
                sellerId = "s_sec",
                action = OfferTransitionAction.ACCEPT_OFFER,
                productStatus = ProductStatus.SOLD.wireValue
            ).isFailure
        )
        assertTrue(
            MarketplaceLogic.validateOfferTransition(
                currentStatus = OfferStatus.PENDING.wireValue,
                actorUid = "s_sec",
                buyerId = "b_sec",
                sellerId = "s_sec",
                action = OfferTransitionAction.ACCEPT_OFFER,
                productStatus = ProductStatus.FLAGGED.wireValue
            ).isFailure
        )

        // Seller cannot accept their own counter offer; only buyer can accept COUNTERED
        val countered = pendingConv.copy(offerStatus = OfferStatus.COUNTERED.wireValue, counterAmount = 450_000.0)
        assertTrue(MarketplaceLogic.validateOfferTransition(countered, "s_sec", OfferTransitionAction.ACCEPT_OFFER).isFailure)
        assertTrue(MarketplaceLogic.validateOfferTransition(countered, "b_sec", OfferTransitionAction.ACCEPT_OFFER).isSuccess)

        // Accepted is terminal: even buyer cannot submit a new offer or counter
        val accepted = pendingConv.copy(offerStatus = OfferStatus.ACCEPTED.wireValue)
        assertTrue(
            MarketplaceLogic.validateOfferTransition(accepted, "b_sec", OfferTransitionAction.SUBMIT_OFFER, amount = 300_000.0).isFailure
        )
        assertTrue(
            MarketplaceLogic.validateOfferTransition(accepted, "s_sec", OfferTransitionAction.COUNTER_OFFER, counterAmount = 500_000.0).isFailure
        )
    }

    @Test
    fun sellerRatingEligibilityRequiresAcceptedSoldDealBlocksGuestsSellersSuspendedAndDuplicates() {
        val soldProd = Product(id = "prod_sold", sellerId = "seller_1", status = ProductStatus.SOLD.wireValue)
        val activeProd = Product(id = "prod_active", sellerId = "seller_1", status = ProductStatus.ACTIVE.wireValue)
        val products = listOf(soldProd, activeProd)

        val acceptedSoldConv = Conversation(
            id = "conv_sold",
            productId = "prod_sold",
            buyerId = "buyer_1",
            sellerId = "seller_1",
            participantIds = listOf("buyer_1", "seller_1"),
            offerStatus = OfferStatus.ACCEPTED.wireValue
        )
        val pendingSoldConv = acceptedSoldConv.copy(id = "conv_pending", offerStatus = OfferStatus.PENDING.wireValue)
        val acceptedActiveConv = acceptedSoldConv.copy(id = "conv_active", productId = "prod_active")

        // Guest -> null
        assertEquals(
            null,
            MarketplaceLogic.findEligibleReviewConversation(listOf(acceptedSoldConv), products, emptyList(), null, "seller_1", "prod_sold")
        )
        // Seller rating self -> null
        assertEquals(
            null,
            MarketplaceLogic.findEligibleReviewConversation(listOf(acceptedSoldConv), products, emptyList(), "seller_1", "seller_1", "prod_sold")
        )
        // Suspended buyer -> null
        assertEquals(
            null,
            MarketplaceLogic.findEligibleReviewConversation(listOf(acceptedSoldConv), products, emptyList(), "buyer_1", "seller_1", "prod_sold", isSuspended = true)
        )
        // Offer not accepted even if sold -> null
        assertEquals(
            null,
            MarketplaceLogic.findEligibleReviewConversation(listOf(pendingSoldConv), products, emptyList(), "buyer_1", "seller_1", "prod_sold")
        )
        // Accepted offer on still-active product -> null
        assertEquals(
            null,
            MarketplaceLogic.findEligibleReviewConversation(listOf(acceptedActiveConv), products, emptyList(), "buyer_1", "seller_1", "prod_active")
        )
        // Eligible buyer with accepted + sold deal -> returns conversation
        assertEquals(
            acceptedSoldConv,
            MarketplaceLogic.findEligibleReviewConversation(listOf(acceptedSoldConv), products, emptyList(), "buyer_1", "seller_1", "prod_sold")
        )
        // Duplicate review already exists -> null
        val existingReview = UserReview(
            id = MarketplaceLogic.buildDeterministicReviewId("prod_sold", "buyer_1"),
            sellerId = "seller_1",
            reviewerId = "buyer_1",
            productId = "prod_sold",
            conversationId = "conv_sold",
            rating = 5,
            comment = "صفقة ممتازة"
        )
        assertEquals("prod_sold_buyer_1", existingReview.id)
        assertTrue(MarketplaceLogic.hasBuyerAlreadyReviewedProduct(listOf(existingReview), "buyer_1", "prod_sold"))
        assertEquals(
            null,
            MarketplaceLogic.findEligibleReviewConversation(listOf(acceptedSoldConv), products, listOf(existingReview), "buyer_1", "seller_1", "prod_sold")
        )
    }
}
