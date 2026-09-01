package com.splitmate.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun testFormatINR() {
        val result = CurrencyUtils.formatINR(100000.0)
        // Indian digit grouping (Lakhs and Crores)
        // We check for Rupee symbol \u20B9
        assert(result.contains("\u20B9"))
    }
    
    @Test
    fun testFormatINR_Negative() {
        // Negative balance formatting
        assertEquals("-₹110.00", CurrencyUtils.formatINR(-110.0).replace("\u00A0", " "))
    }
}
