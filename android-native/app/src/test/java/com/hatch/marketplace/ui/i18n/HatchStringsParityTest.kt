package com.hatch.marketplace.ui.i18n

import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.CategoryId
import com.hatch.marketplace.data.model.GuestPromptReason
import com.hatch.marketplace.data.model.PaymentMethod
import com.hatch.marketplace.data.model.ProductCondition
import com.hatch.marketplace.data.model.SudaneseCities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HatchStringsParityTest {

    @Test
    fun sudaneseCitiesIncludesAll56CitiesAcrossStates() {
        assertEquals(56, SudaneseCities.ALL.size)
        assertTrue(SudaneseCities.ALL.any { it.contains("الخرطوم") })
        assertTrue(SudaneseCities.ALL.any { it.contains("بورتسودان") })
        assertTrue(SudaneseCities.ALL.any { it.contains("عطبرة") })
        assertTrue(SudaneseCities.ALL.any { it.contains("نيالا") })
    }

    @Test
    fun arabicAndEnglishStringsCoverAllEnumsTemplatesAndPresets() {
        val ar = HatchStrings.forLanguage(AppLanguage.AR)
        val en = HatchStrings.forLanguage(AppLanguage.EN)

        assertEquals(4, ar.broadcastTemplates.size)
        assertEquals(4, en.broadcastTemplates.size)
        assertEquals(4, ar.sudanAdPresets.size)
        assertEquals(5, ar.inspectionTags.size)
        assertEquals(5, ar.reviewTags.size)
        assertEquals(5, ar.suspensionPresetReasons.size)

        CategoryId.entries.forEach { cat ->
            assertTrue(ar.categoryLabel(cat.wireValue).isNotBlank())
            assertTrue(en.categoryLabel(cat.wireValue).isNotBlank())
        }
        ProductCondition.entries.forEach { cond ->
            assertTrue(ar.conditionLabel(cond.wireValue).isNotBlank())
            assertTrue(en.conditionLabel(cond.wireValue).isNotBlank())
        }
        PaymentMethod.entries.forEach { pm ->
            assertTrue(ar.paymentLabel(pm.wireValue).isNotBlank())
            assertTrue(en.paymentLabel(pm.wireValue).isNotBlank())
        }

        assertEquals("هتش", ar.navSwipe)
        assertEquals("HATCH", en.navSwipe)
        assertEquals("هتش", ar.appName)
        assertEquals("HATCH", en.appName)

        assertTrue(ar.signInGoogle.isNotBlank())
        assertTrue(en.signInGoogle.isNotBlank())
        assertTrue(ar.browseAsGuest.isNotBlank())
        assertTrue(en.browseAsGuest.isNotBlank())
        assertTrue(ar.saveAndContinue.isNotBlank())
        assertTrue(en.saveAndContinue.isNotBlank())
        assertTrue(!ar.signInSubtitle.contains("البريد الإلكتروني"))
        assertTrue(!en.signInSubtitle.contains("Email"))

        assertEquals("مرحباً بك في HATCH، اختر نوع حسابك", ar.accountTypeChooserTitle)
        assertTrue(en.accountTypeChooserTitle.isNotBlank())
        assertTrue(ar.accountPersonalTitle.isNotBlank() && en.accountPersonalTitle.isNotBlank())
        assertTrue(ar.accountStoreTitle.isNotBlank() && en.accountStoreTitle.isNotBlank())
        assertTrue(ar.accountCompanyTitle.isNotBlank() && en.accountCompanyTitle.isNotBlank())
        assertTrue(ar.upgradeAccountTitle.isNotBlank() && en.upgradeAccountTitle.isNotBlank())
        assertTrue(ar.viewStorefrontLabel.isNotBlank() && en.viewStorefrontLabel.isNotBlank())
        assertTrue(ar.storefrontProductsTitle.isNotBlank() && en.storefrontProductsTitle.isNotBlank())
        assertEquals("المتاجر", ar.storesButtonLabel)
        assertEquals("الشركات", ar.companiesButtonLabel)
        assertTrue(en.storesButtonLabel.isNotBlank() && en.companiesButtonLabel.isNotBlank())
        assertTrue(ar.registeredStoresTitle.isNotBlank() && en.registeredStoresTitle.isNotBlank())
        assertTrue(ar.registeredCompaniesTitle.isNotBlank() && en.registeredCompaniesTitle.isNotBlank())
        assertTrue(ar.noStoresYet.isNotBlank() && en.noStoresYet.isNotBlank())
        assertTrue(ar.noCompaniesYet.isNotBlank() && en.noCompaniesYet.isNotBlank())
        AccountType.entries.forEach { type ->
            assertTrue(ar.accountTypeLabel(type.wireValue).isNotBlank())
            assertTrue(en.accountTypeLabel(type.wireValue).isNotBlank())
        }

        assertTrue(ar.guestDialogTitle.isNotBlank() && en.guestDialogTitle.isNotBlank())
        assertEquals("متابعة التصفح كضيف", ar.guestDialogKeepBrowsing)
        assertTrue(en.guestDialogKeepBrowsing.isNotBlank())
        assertEquals(
            "هل ترغب في التواصل مع البائع؟ سجّل دخولك إلى HATCH للاستفادة من خدمات التواصل والشراء.",
            ar.guestPromptMessage(GuestPromptReason.CONTACT_OR_BUY)
        )
        assertEquals(
            "سجّل دخولك إلى HATCH لتتمكن من نشر منتجاتك والبيع.",
            ar.guestPromptMessage(GuestPromptReason.SELL)
        )
        assertTrue(ar.guestStorefrontReadOnlyHint.isNotBlank() && en.guestStorefrontReadOnlyHint.isNotBlank())
        assertTrue(ar.guestDetailSignInToContact.isNotBlank() && en.guestDetailSignInToContact.isNotBlank())
        GuestPromptReason.entries.forEach { reason ->
            assertTrue(ar.guestPromptMessage(reason).isNotBlank())
            assertTrue(en.guestPromptMessage(reason).isNotBlank())
        }

        assertTrue(ar.goldBadgeGrantedTitle.isNotBlank() && en.goldBadgeGrantedTitle.isNotBlank())
        assertTrue(ar.goldBadgeAdminDesc.isNotBlank() && en.goldBadgeAdminDesc.isNotBlank())
        assertTrue(ar.requestGoldVerification.isNotBlank() && en.requestGoldVerification.isNotBlank())
        assertTrue(ar.verificationPendingLabel.isNotBlank() && en.verificationPendingLabel.isNotBlank())
        assertTrue(ar.verificationRejectedLabel.isNotBlank() && en.verificationRejectedLabel.isNotBlank())
        assertTrue(ar.verificationNoteLabel.isNotBlank() && en.verificationNoteLabel.isNotBlank())
        assertTrue(ar.verificationDocUrlLabel.isNotBlank() && en.verificationDocUrlLabel.isNotBlank())
        assertTrue(ar.submitVerificationBtn.isNotBlank() && en.submitVerificationBtn.isNotBlank())
        assertTrue(ar.dealRatedLabel.isNotBlank() && en.dealRatedLabel.isNotBlank())
        assertTrue(ar.loadingMoreListings.isNotBlank() && en.loadingMoreListings.isNotBlank())
        assertTrue(ar.noMoreListings.isNotBlank() && en.noMoreListings.isNotBlank())
        assertTrue(ar.loadOlderListings.isNotBlank() && en.loadOlderListings.isNotBlank())
    }
}
