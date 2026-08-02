package com.norvexa.flow.domain

import org.junit.Assert.*
import org.junit.Test

class MoneyUtilsTest {
    @Test fun parsesCommaAndDot(){assertEquals(1234L,parseMinor("12,34"));assertEquals(1234L,parseMinor("12.34"))}
    @Test fun rejectsInvalidValue(){assertNull(parseMinor("abc"))}
    @Test fun parsesRateToMicros(){assertEquals(1_250_000L,parseRateMicros("1.25"))}
}
