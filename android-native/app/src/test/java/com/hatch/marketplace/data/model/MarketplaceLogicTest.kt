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
    fun autoVerifyTriggersAtTenDealsAndZeroReportsOnly() {
        assertFalse(MarketplaceLogic.shouldAutoVerify(dealsCompleted = 9, reportsCount = 0))
        assertFalse(MarketplaceLogic.shouldAutoVerify(dealsCompleted = 10, reportsCount = 1))
        assertTrue(MarketplaceLogic.shouldAutoVerify(dealsCompleted = 10, reportsCount = 0))
        assertTrue(MarketplaceLogic.shouldAutoVerify(dealsCompleted = 15, reportsCount = 0))
    }

    @Test
    fun adminRoleRecognizesExplicitRoleOrVerifiedOwnerEmail() {
        assertTrue(MarketplaceLogic.isAdminRole(role = "admin", email = "someone@example.com"))
        assertTrue(MarketplaceLogic.isAdminRole(role = "user", email = "batydaishab@gmail.com"))
        assertTrue(MarketplaceLogic.isAdminRole(role = null, email = "BATYDAISHAB@GMAIL.COM"))
        assertFalse(MarketplaceLogic.isAdminRole(role = "user", email = "trader@hatch.sd"))
    }

    @Test
    fun suspensionLogicHandlesActiveFutureAndExpiredSuspensions() {
        val now = 1_700_000_000_000L
        assertFalse(MarketplaceLogic.isUserSuspended("active", null, now))

        val futureIso = MarketplaceLogic.buildSuspensionEndIso(days = 7, nowMillis = now)
        assertTrue(MarketplaceLogic.isUserSuspended("suspended", futureIso, now))

        // 8 days later -> suspension expired
        val afterExpiry = now + 8 * 86_400_000L
        assertFalse(MarketplaceLogic.isUserSuspended("suspended", futureIso, afterExpiry))
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
}
