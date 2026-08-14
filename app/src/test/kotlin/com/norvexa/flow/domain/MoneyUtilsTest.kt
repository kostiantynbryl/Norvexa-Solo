package com.norvexa.flow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyUtilsTest {
    @Test
    fun parsesCommaAndDotForTwoDigitCurrency() {
        assertEquals(1234L, parseMinor("12,34"))
        assertEquals(1234L, parseMinor("12.34"))
    }

    @Test
    fun respectsZeroFractionCurrency() {
        assertEquals(1_234L, parseMinorForCurrency("1234", "JPY"))
        assertEquals(0, currencyFractionDigits("JPY"))
    }

    @Test
    fun validatesIsoCurrencyCode() {
        assertTrue(isValidCurrencyCode("USD"))
        assertTrue(isValidCurrencyCode("PLN"))
    }

    @Test
    fun rejectsInvalidValue() {
        assertNull(parseMinor("abc"))
    }

    @Test
    fun parsesRateToMicros() {
        assertEquals(1_250_000L, parseRateMicros("1.25"))
    }
}
