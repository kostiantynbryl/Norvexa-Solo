package com.norvexa.flow.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun parseMinor(text: String): Long? = runCatching { text.trim().replace(',', '.').toBigDecimal().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact() }.getOrNull()
fun parseRateMicros(text: String): Long? = runCatching { text.trim().replace(',', '.').toBigDecimal().multiply(BigDecimal.valueOf(1_000_000L)).setScale(0, RoundingMode.HALF_UP).longValueExact() }.getOrNull()
fun formatMoney(minor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
    val amount = BigDecimal.valueOf(minor, 2)
    return runCatching {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT)); maximumFractionDigits = 2; minimumFractionDigits = 2
        }.format(amount)
    }.getOrElse { "${amount.setScale(2, RoundingMode.HALF_UP)} ${currencyCode.uppercase(Locale.ROOT)}" }
}
