package com.norvexa.flow.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val DEFAULT_FRACTION_DIGITS = 2

fun currencyFractionDigits(currencyCode: String): Int = runCatching {
    Currency.getInstance(currencyCode.uppercase(Locale.ROOT)).defaultFractionDigits
        .takeIf { it >= 0 } ?: DEFAULT_FRACTION_DIGITS
}.getOrDefault(DEFAULT_FRACTION_DIGITS)

fun parseMinor(text: String, fractionDigits: Int = DEFAULT_FRACTION_DIGITS): Long? = runCatching {
    require(fractionDigits in 0..8)
    text.trim()
        .replace(',', '.')
        .toBigDecimal()
        .movePointRight(fractionDigits)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()

fun parseMinorForCurrency(text: String, currencyCode: String): Long? =
    parseMinor(text, currencyFractionDigits(currencyCode))

fun minorToDecimal(minor: Long, currencyCode: String): BigDecimal =
    BigDecimal.valueOf(minor, currencyFractionDigits(currencyCode))

fun parseRateMicros(text: String): Long? = runCatching {
    text.trim()
        .replace(',', '.')
        .toBigDecimal()
        .multiply(BigDecimal.valueOf(1_000_000L))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()

fun isValidCurrencyCode(currencyCode: String): Boolean = runCatching {
    Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
}.isSuccess

fun formatMoney(
    minor: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val normalized = currencyCode.uppercase(Locale.ROOT)
    val digits = currencyFractionDigits(normalized)
    val amount = BigDecimal.valueOf(minor, digits)
    return runCatching {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(normalized)
            maximumFractionDigits = digits
            minimumFractionDigits = digits
        }.format(amount)
    }.getOrElse {
        "${amount.setScale(digits, RoundingMode.HALF_UP)} $normalized"
    }
}
