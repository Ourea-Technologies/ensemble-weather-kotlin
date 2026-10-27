// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import com.oureawx.ensembleweather.kotlin.internal.Query
import com.oureawx.ensembleweather.kotlin.internal.Terms
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TermsTest {
    private fun wire(
        units: Units,
        vararg terms: ProbabilityTerm,
    ) = Terms.probabilities(terms.toList(), units).wire

    @Test
    fun `rounds half away from zero in doubles, never banker's`() {
        assertEquals(63, Terms.roundHalfAwayFromZero(62.5))
        assertEquals(-63, Terms.roundHalfAwayFromZero(-62.5))
        assertEquals(100, Terms.roundHalfAwayFromZero(1.005 * 100))
        assertEquals(3, Terms.roundHalfAwayFromZero(2.5))
        assertEquals(0, Terms.roundHalfAwayFromZero(0.49999999999999994))
    }

    @Test
    fun `prints exactly the served decimals, and -0 as 0`() {
        assertEquals("0.0", Terms.formatScaled(Terms.roundHalfAwayFromZero(-0.04 * 10), 1))
        assertEquals("-5.0", Terms.formatScaled(-50, 1))
        assertEquals("0.063", Terms.formatScaled(63, 3))
        assertEquals("1524", Terms.formatScaled(1524, 0))
        assertEquals("-0.05", Terms.formatScaled(-5, 2))
    }

    @Test
    fun `converts the caller's threshold with the exact factors`() {
        assertEquals("snowfall:ge:152.40:24h", wire(Units.Metric, Variable.Snowfall atLeast 6.inches))
        assertEquals("snowfall:ge:0.394:24h", wire(Units.Imperial, Variable.Snowfall atLeast 10.mm))
        assertEquals("temperature:le:0.0:24h", wire(Units.Metric, Variable.Temperature atMost 32.fahrenheit))
        assertEquals("temperature:le:273.2:24h", wire(Units.Kelvin, Variable.Temperature atMost 32.fahrenheit))
        assertEquals("temperature:ge:32.0:24h", wire(Units.Imperial, Variable.Temperature atLeast 0.celsius))
        assertEquals("temperature:ge:-273.2:24h", wire(Units.Metric, Variable.Temperature atLeast 0.kelvin))
        assertEquals("snow_level:le:1524:24h", wire(Units.Metric, Variable.SnowLevel atMost 5000.feet))
        assertEquals("snow_level:le:4921:24h", wire(Units.Imperial, Variable.SnowLevel atMost 1500.meters))
        assertEquals("wind_speed:ge:13.4:24h", wire(Units.Metric, Variable.WindSpeed atLeast 30.mph))
        assertEquals("wind_speed:ge:33.6:24h", wire(Units.Imperial, Variable.WindSpeed atLeast 15.metersPerSecond))
    }

    @Test
    fun `a unitless threshold is in the request's units`() {
        assertEquals("precipitation:ge:5.00:24h", wire(Units.Metric, Variable.Precipitation atLeast 5))
    }

    @Test
    fun `sorts, de-duplicates and omits the default aggregation`() {
        val sent =
            wire(
                Units.Metric,
                Variable.WindSpeed atLeast 10.metersPerSecond within (12.hours..36.hours),
                Variable.Temperature atMost 0.celsius using Aggregation.All,
                Variable.Temperature atMost 0.celsius using Aggregation.Any,
                Variable.WindSpeed atLeast 10.metersPerSecond,
                Variable.Temperature atMost 0.celsius,
            )
        assertEquals(
            "temperature:le:0.0:24h,temperature:le:0.0:24h:all,wind_speed:ge:10.0:24h,wind_speed:ge:10.0:12-36h",
            sent,
        )
    }

    @Test
    fun `refuses terms the server would refuse, before sending`() {
        assertFailsWith<UsageException> {
            wire(
                Units.Metric,
                Variable.Precipitation atLeast 1.mm using Aggregation.Any,
            )
        }
        assertFailsWith<UsageException> {
            wire(
                Units.Metric,
                Variable.Temperature atLeast 1.celsius using Aggregation.Total,
            )
        }
        assertFailsWith<UsageException> { wire(Units.Metric, Variable.Temperature atLeast 6.inches) }
        assertFailsWith<UsageException> { wire(Units.Metric, Variable("visibility") atLeast 1) }
        assertFailsWith<UsageException> { wire(Units.Metric, Variable.Snowfall atLeast 1.mm within Window(0, 361)) }
        assertFailsWith<UsageException> { wire(Units.Metric, Variable.Snowfall atLeast 1.mm within Window(24, 12)) }
        assertFailsWith<UsageException> { Variable.Snowfall atLeast 1.mm within 90.minutes }
        assertFailsWith<UsageException> { Terms.totals(listOf(Variable.Temperature totalWithin 24.hours)) }
    }

    @Test
    fun `windows print as N h or A-B h`() {
        assertEquals("24h", Window.Default.toString())
        assertEquals("6-18h", Window.of(6.hours..18.hours).toString())
        assertEquals(
            "precipitation:48h,snowfall:24h,snowfall:12-36h",
            Terms
                .totals(
                    listOf(
                        Variable.Snowfall totalWithin 24.hours,
                        Variable.Precipitation totalWithin 48.hours,
                        Variable.Snowfall totalWithin (12.hours..36.hours),
                    ),
                ).wire,
        )
    }

    @Test
    fun `query values are RFC 3986 encoded and numbers never use exponents`() {
        assertEquals("Mt.%20Hood%20%26%20co%2F%C3%B1", Query.encode("Mt. Hood & co/ñ"))
        assertEquals("a-b_c.d~e", Query.encode("a-b_c.d~e"))
        assertEquals("1661", Query.number(1661.0))
        assertEquals("45.3311", Query.number(45.3311))
        assertEquals("0.00001", Query.number(1e-5))
        assertEquals("-121.6649", Query.number(-121.6649))
        assertEquals("0", Query.number(-0.0))
        assertEquals("12345678901234567000", Query.number(1.2345678901234567e19))
        assertFailsWith<UsageException> { Query.number(Double.NaN) }
    }
}
