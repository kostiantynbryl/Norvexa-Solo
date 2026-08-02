package com.norvexa.flow.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money is stored in the smallest unit of a currency (for example, cents).
 * Float and Double are intentionally not used for financial calculations.
 */
data class Money(
    val minorUnits: Long,
    val currencyCode: String,
) {
    init {
        require(currencyCode.matches(Regex("[A-Z]{3}"))) {
            "Currency code must be a three-letter ISO 4217 code"
        }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = Math.subtractExact(minorUnits, other.minorUnits))
    }

    fun multiply(factor: BigDecimal, roundingMode: RoundingMode = RoundingMode.HALF_UP): Money =
        copy(
            minorUnits = BigDecimal.valueOf(minorUnits)
                .multiply(factor)
                .setScale(0, roundingMode)
                .longValueExact(),
        )

    private fun requireSameCurrency(other: Money) {
        require(currencyCode == other.currencyCode) {
            "Cannot combine $currencyCode and ${other.currencyCode}"
        }
    }

    companion object {
        fun zero(currencyCode: String): Money = Money(0L, currencyCode)
    }
}
