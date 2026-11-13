package com.hatch.marketplace.ui.i18n

import com.hatch.marketplace.data.model.AppLanguage
import com.hatch.marketplace.data.model.CategoryId
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
    }
}
