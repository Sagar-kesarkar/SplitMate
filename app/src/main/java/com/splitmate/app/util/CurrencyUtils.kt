package com.splitmate.app.util

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    private val inrLocale = Locale.forLanguageTag("en-IN")
    private val inrCurrency = Currency.getInstance("INR")
    
    private val currencyFormatter = NumberFormat.getNumberInstance(inrLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /**
     * Formats a double value as an Indian Rupee string with correct digit grouping.
     * Example: 100000 -> ₹1,00,000.00
     */
    fun formatINR(amount: Double): String {
        val isNegative = amount < -0.01
        val absoluteAmount = kotlin.math.abs(amount)
        val formattedNumber = currencyFormatter.format(absoluteAmount)
        return if (isNegative) "-₹$formattedNumber" else "₹$formattedNumber"
    }
}
