package com.hatch.marketplace.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingPublishValidatorTest {

    @Test
    fun normalizeNumericInputAndParsePositivePriceSupportArabicPersianAndEnglishDigits() {
        assertEquals("15000", ListingPublishValidator.normalizeNumericInput("١٥٠٠٠"))
        assertEquals("2450.75", ListingPublishValidator.normalizeNumericInput("٢٤٥٠٫٧٥"))
        assertEquals("9876543210", ListingPublishValidator.normalizeNumericInput("۹۸۷۶۵۴۳۲۱۰"))
        assertEquals("1234.5678", ListingPublishValidator.normalizeNumericInput("12,34.56.78"))
        assertEquals("15000.50", ListingPublishValidator.normalizeNumericInput("١٥٬٠٠٠٫٥٠"))

        assertEquals(15000.0, ListingPublishValidator.parsePositivePrice("١٥٠٠٠")!!, 0.001)
        assertEquals(15000.5, ListingPublishValidator.parsePositivePrice("١٥٬٠٠٠٫٥٠")!!, 0.001)
        assertEquals(2450.75, ListingPublishValidator.parsePositivePrice("٢٤٥٠٫٧٥")!!, 0.001)
        assertEquals(98765.0, ListingPublishValidator.parsePositivePrice("۹۸۷۶۵")!!, 0.001)
        assertEquals(420.5, ListingPublishValidator.parsePositivePrice("420.5")!!, 0.001)
        assertEquals(1234.5678, ListingPublishValidator.parsePositivePrice("12,34.56.78")!!, 0.0001)
        assertNull(ListingPublishValidator.parsePositivePrice("٠"))
        assertNull(ListingPublishValidator.parsePositivePrice(""))
        assertNull(ListingPublishValidator.parsePositivePrice("abc"))
    }

    @Test
    fun validateManualImageUrlAcceptsValidHttpsAndExplainsWhyInvalidUrlsAreRejected() {
        val validResult = ListingPublishValidator.validateManualImageUrl(
            raw = "  HTTPS://cdn.example.com/photos/item.webp  ",
            currentCount = 0
        )
        assertTrue(validResult is ImageUrlValidationResult.Valid)
        assertEquals(
            "https://cdn.example.com/photos/item.webp",
            (validResult as ImageUrlValidationResult.Valid).normalizedUrl
        )

        val httpResult = ListingPublishValidator.validateManualImageUrl(
            raw = "http://cdn.example.com/item.webp",
            currentCount = 0
        )
        assertTrue(httpResult is ImageUrlValidationResult.Invalid)
        assertTrue((httpResult as ImageUrlValidationResult.Invalid).messageAr.contains("https://"))

        val noSchemeResult = ListingPublishValidator.validateManualImageUrl(
            raw = "cdn.example.com/item.webp",
            currentCount = 0
        )
        assertTrue(noSchemeResult is ImageUrlValidationResult.Invalid)
        assertTrue((noSchemeResult as ImageUrlValidationResult.Invalid).messageEn.contains("https://"))

        val spacesResult = ListingPublishValidator.validateManualImageUrl(
            raw = "https://cdn.example.com/my item.webp",
            currentCount = 0
        )
        assertTrue(spacesResult is ImageUrlValidationResult.Invalid)

        val duplicateResult = ListingPublishValidator.validateManualImageUrl(
            raw = "https://cdn.example.com/item.webp",
            currentCount = 1,
            existingUrls = listOf("https://cdn.example.com/item.webp")
        )
        assertTrue(duplicateResult is ImageUrlValidationResult.Invalid)

        val limitResult = ListingPublishValidator.validateManualImageUrl(
            raw = "https://cdn.example.com/item6.webp",
            currentCount = 5
        )
        assertTrue(limitResult is ImageUrlValidationResult.Invalid)
    }

    @Test
    fun validateDraftReportsClearErrorsForMissingFieldsAndSignerRequirements() {
        val missingPhoto = ListingPublishValidator.validateDraft(
            title = "آيفون 15 برو",
            description = "جهاز نظيف جداً مع الكرتونة",
            priceText = "٥٠٠٠٠٠",
            originalPriceText = "",
            city = "الخرطوم (Khartoum)",
            localImageCount = 0,
            remoteUrls = emptyList(),
            pendingUrlInput = "",
            paymentMethods = listOf("cash"),
            signerReady = false,
            isArabic = true
        )
        assertFalse(missingPhoto.isValid)
        assertNotNull(missingPhoto.errorMessage)

        val missingTitle = ListingPublishValidator.validateDraft(
            title = "ا",
            description = "وصف واضح ومناسب للمنتج",
            priceText = "2500",
            originalPriceText = "",
            city = "الخرطوم (Khartoum)",
            localImageCount = 0,
            remoteUrls = listOf("https://cdn.example.com/p1.webp"),
            pendingUrlInput = "",
            paymentMethods = listOf("bankak"),
            signerReady = false,
            isArabic = true
        )
        assertFalse(missingTitle.isValid)
        assertTrue(missingTitle.errorMessage!!.contains("عنوان"))

        val invalidPrice = ListingPublishValidator.validateDraft(
            title = "شاشة سامسونج",
            description = "شاشة بحالة ممتازة جداً",
            priceText = "٠",
            originalPriceText = "",
            city = "الخرطوم (Khartoum)",
            localImageCount = 0,
            remoteUrls = listOf("https://cdn.example.com/p1.webp"),
            pendingUrlInput = "",
            paymentMethods = listOf("cash"),
            signerReady = false,
            isArabic = true
        )
        assertFalse(invalidPrice.isValid)
        assertTrue(invalidPrice.errorMessage!!.contains("سعر"))

        val localPhotoWithoutSigner = ListingPublishValidator.validateDraft(
            title = "شاشة سامسونج",
            description = "شاشة بحالة ممتازة جداً",
            priceText = "120000",
            originalPriceText = "",
            city = "الخرطوم (Khartoum)",
            localImageCount = 1,
            remoteUrls = emptyList(),
            pendingUrlInput = "",
            paymentMethods = listOf("cash"),
            signerReady = false,
            isArabic = true
        )
        assertFalse(localPhotoWithoutSigner.isValid)
        assertTrue(localPhotoWithoutSigner.errorMessage!!.contains("MEDIA_SIGNER_URL"))
    }

    @Test
    fun validateDraftAutoCommitsTypedValidHttpsUrlAndNormalizesArabicPrice() {
        val result = ListingPublishValidator.validateDraft(
            title = "ساعة ذكية أصلية",
            description = "جديدة بالكرتونة والضمان",
            priceText = "١٥٠٠٠٠",
            originalPriceText = "١٨٠٠٠٠",
            city = "الخرطوم (Khartoum)",
            localImageCount = 0,
            remoteUrls = emptyList(),
            pendingUrlInput = "https://images.example.com/watch.webp",
            paymentMethods = listOf("bankak", "cash"),
            signerReady = false,
            isArabic = true
        )
        assertTrue(result.isValid)
        assertEquals(150000.0, result.resolvedPrice, 0.001)
        assertEquals(180000.0, result.resolvedOriginalPrice!!, 0.001)
        assertEquals(listOf("https://images.example.com/watch.webp"), result.resolvedRemoteUrls)
        assertNull(result.errorMessage)

        val badPendingUrl = ListingPublishValidator.validateDraft(
            title = "ساعة ذكية أصلية",
            description = "جديدة بالكرتونة والضمان",
            priceText = "150000",
            originalPriceText = "",
            city = "الخرطوم (Khartoum)",
            localImageCount = 0,
            remoteUrls = emptyList(),
            pendingUrlInput = "http://insecure.example.com/watch.webp",
            paymentMethods = listOf("cash"),
            signerReady = false,
            isArabic = true
        )
        assertFalse(badPendingUrl.isValid)
        assertTrue(badPendingUrl.errorMessage!!.contains("https://"))
    }
}
