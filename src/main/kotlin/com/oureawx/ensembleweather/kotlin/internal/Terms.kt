// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin.internal

import com.oureawx.ensembleweather.kotlin.Aggregation
import com.oureawx.ensembleweather.kotlin.ProbabilityResult
import com.oureawx.ensembleweather.kotlin.ProbabilityTerm
import com.oureawx.ensembleweather.kotlin.ResponseFormatException
import com.oureawx.ensembleweather.kotlin.Threshold
import com.oureawx.ensembleweather.kotlin.ThresholdUnit
import com.oureawx.ensembleweather.kotlin.ThresholdUnit.Dimension
import com.oureawx.ensembleweather.kotlin.Units
import com.oureawx.ensembleweather.kotlin.UsageException
import com.oureawx.ensembleweather.kotlin.Variable
import com.oureawx.ensembleweather.kotlin.Window
import com.oureawx.ensembleweather.kotlin.WindowTotal
import com.oureawx.ensembleweather.kotlin.WindowTotalTerm
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.truncate

/** One term in canonical form: its wire string, which its answer echoes as `term`. */
internal class CanonicalTerm(
    val wire: String,
    val sortKey: List<Comparable<*>>,
) {
    companion object {
        /** The server's order: field by field through [sortKey] (BEHAVIOR.md section 4). */
        val ORDER: Comparator<CanonicalTerm> =
            Comparator { a, b ->
                a.sortKey.indices
                    .asSequence()
                    .map {
                        compareValues(
                            a.sortKey[it],
                            b.sortKey[it],
                        )
                    }.firstOrNull { it != 0 }
                    ?: 0
            }
    }
}

/** A `prob=` or `totals=` value plus each caller term's canonical form, in the caller's order. */
internal class CanonicalTerms(
    val callerOrder: List<CanonicalTerm>,
) {
    /** The de-duplicated, sorted, comma-joined wire value (BEHAVIOR.md sections 4 and 5). */
    val wire: String = callerOrder.distinctBy { it.wire }.sortedWith(CanonicalTerm.ORDER).joinToString(",") { it.wire }
}

/** Serialises probability and total terms exactly as the server keys them (KAN-1313). */
internal object Terms {
    private val aggregationOrder = listOf(Aggregation.Total, Aggregation.Any, Aggregation.All)
    private val amounts = setOf(Variable.Precipitation, Variable.Snowfall)

    fun probabilities(
        terms: List<ProbabilityTerm>,
        units: Units,
    ): CanonicalTerms = CanonicalTerms(terms.map { canonical(it, units) })

    fun totals(terms: List<WindowTotalTerm>): CanonicalTerms = CanonicalTerms(terms.map(::canonical))

    private fun canonical(
        term: ProbabilityTerm,
        units: Units,
    ): CanonicalTerm {
        val variable = term.variable
        if (!variable.isKnown) throw UsageException("unknown probability variable '${variable.value}'")
        if (term.op.value != "ge" &&
            term.op.value != "le"
        ) {
            throw UsageException("unknown comparison '${term.op.value}'")
        }
        val default = if (variable in amounts) Aggregation.Total else Aggregation.Any
        val aggregation = term.aggregation ?: default
        if (aggregation !in aggregationOrder) throw UsageException("unknown aggregation '${aggregation.value}'")
        if ((variable in amounts) != (aggregation == Aggregation.Total)) {
            throw UsageException(
                "aggregation '${aggregation.value}' is not allowed for ${variable.value}: " +
                    "precipitation and snowfall take 'total' only, the other variables 'any' or 'all'",
            )
        }
        validate(term.window)
        val value = convert(term.threshold, variable, units)
        val decimals = precision(variable, units)
        val scaled = roundHalfAwayFromZero(value * Math.pow(TEN, decimals.toDouble()))
        val threshold = formatScaled(scaled, decimals)
        val window = term.window.toString()
        val suffix = if (aggregation == default) "" else ":${aggregation.value}"
        return CanonicalTerm(
            wire = "${variable.value}:${term.op.value}:$threshold:$window$suffix",
            sortKey =
                listOf(
                    variable.value,
                    term.op.value,
                    term.window.startHours,
                    term.window.endHours,
                    aggregationOrder.indexOf(aggregation),
                    scaled,
                ),
        )
    }

    private fun canonical(term: WindowTotalTerm): CanonicalTerm {
        if (term.variable !in amounts) {
            throw UsageException("window totals are for precipitation and snowfall, not ${term.variable.value}")
        }
        validate(term.window)
        return CanonicalTerm(
            wire = "${term.variable.value}:${term.window}",
            sortKey = listOf(term.variable.value, term.window.startHours, term.window.endHours),
        )
    }

    private fun validate(window: Window) {
        if (window.startHours < 0 || window.endHours <= window.startHours || window.endHours > Window.MAX_HOURS) {
            throw UsageException(
                "window ${window.startHours}..${window.endHours} h is out of range: " +
                    "0 <= start < end <= ${Window.MAX_HOURS}",
            )
        }
    }

    /**
     * Maps the server's answers back to the caller's terms by the `term` each one echoes,
     * never by position (BEHAVIOR.md section 4).
     */
    fun mapProbabilities(
        terms: CanonicalTerms,
        results: List<ProbabilityResult>?,
    ): List<ProbabilityResult>? {
        if (results == null) return null
        val byTerm = results.associateBy { it.term }
        return terms.callerOrder.map {
            byTerm[it.wire] ?: throw ResponseFormatException("the response has no answer for prob term '${it.wire}'")
        }
    }

    /** Maps the server's window totals back to the caller's terms by `term` (BEHAVIOR.md section 5). */
    fun mapTotals(
        terms: CanonicalTerms,
        results: List<WindowTotal>?,
    ): List<WindowTotal>? {
        if (results == null) return null
        val byTerm = results.associateBy { it.term }
        return terms.callerOrder.map {
            byTerm[it.wire] ?: throw ResponseFormatException("the response has no answer for totals term '${it.wire}'")
        }
    }

    /** The served precision of [variable] in [units] (BEHAVIOR.md section 4). */
    fun precision(
        variable: Variable,
        units: Units,
    ): Int =
        when (variable) {
            Variable.Precipitation, Variable.Snowfall -> {
                if (units ==
                    Units.Imperial
                ) {
                    IMPERIAL_AMOUNT_DECIMALS
                } else {
                    METRIC_AMOUNT_DECIMALS
                }
            }

            Variable.SnowLevel -> {
                0
            }

            else -> {
                1
            }
        }

    /** The threshold in the request's units, by the exact factors of BEHAVIOR.md section 7. */
    fun convert(
        threshold: Threshold,
        variable: Variable,
        units: Units,
    ): Double {
        val unit = threshold.unit ?: return threshold.value
        val target = target(variable, units)
        if (unit.dimension != target.dimension) {
            throw UsageException("a ${variable.value} threshold cannot be in ${unit.symbol}")
        }
        val v = threshold.value
        return when (unit.dimension) {
            Dimension.Temperature -> fromCelsius(toCelsius(v, unit), target)
            Dimension.Amount -> scale(v, unit, target, ThresholdUnit.Inches, MM_PER_INCH)
            Dimension.Height -> scale(v, unit, target, ThresholdUnit.Feet, M_PER_FOOT)
            Dimension.Speed -> scale(v, unit, target, ThresholdUnit.MilesPerHour, MS_PER_MPH)
        }
    }

    private fun target(
        variable: Variable,
        units: Units,
    ): ThresholdUnit {
        val imperial = units == Units.Imperial
        return when (variable) {
            Variable.Precipitation, Variable.Snowfall -> {
                if (imperial) ThresholdUnit.Inches else ThresholdUnit.Millimeters
            }

            Variable.SnowLevel -> {
                if (imperial) ThresholdUnit.Feet else ThresholdUnit.Meters
            }

            Variable.WindSpeed -> {
                if (imperial) ThresholdUnit.MilesPerHour else ThresholdUnit.MetersPerSecond
            }

            else -> {
                when (units) {
                    Units.Imperial -> ThresholdUnit.Fahrenheit
                    Units.Kelvin -> ThresholdUnit.Kelvin
                    else -> ThresholdUnit.Celsius
                }
            }
        }
    }

    /** Converts between a metric unit and its imperial counterpart [imperialUnit] (1 imperial = [factor] metric). */
    private fun scale(
        value: Double,
        from: ThresholdUnit,
        to: ThresholdUnit,
        imperialUnit: ThresholdUnit,
        factor: Double,
    ): Double =
        when {
            from == to -> value
            from == imperialUnit -> value * factor
            else -> value / factor
        }

    private fun toCelsius(
        value: Double,
        unit: ThresholdUnit,
    ): Double =
        when (unit) {
            ThresholdUnit.Fahrenheit -> (value - FREEZING_F) * FIVE / NINE
            ThresholdUnit.Kelvin -> value - KELVIN_OFFSET
            else -> value
        }

    private fun fromCelsius(
        celsius: Double,
        unit: ThresholdUnit,
    ): Double =
        when (unit) {
            ThresholdUnit.Fahrenheit -> celsius * NINE / FIVE + FREEZING_F
            ThresholdUnit.Kelvin -> celsius + KELVIN_OFFSET
            else -> celsius
        }

    /** `round_half_away_from_zero(x)` in IEEE-754 doubles (never banker's rounding). */
    fun roundHalfAwayFromZero(x: Double): Long {
        val whole = truncate(x)
        val rounded = if (abs(x - whole) >= HALF) whole + sign(x) else whole
        return rounded.toLong()
    }

    /** [scaled] / 10^[decimals], printed with exactly [decimals] decimals; `-0` prints as `0`. */
    fun formatScaled(
        scaled: Long,
        decimals: Int,
    ): String {
        val negative = scaled < 0
        val digits = abs(scaled).toString().padStart(decimals + 1, '0')
        val body =
            if (decimals == 0) digits else digits.dropLast(decimals) + "." + digits.takeLast(decimals)
        return if (negative) "-$body" else body
    }

    private const val IMPERIAL_AMOUNT_DECIMALS = 3
    private const val METRIC_AMOUNT_DECIMALS = 2
    private const val TEN = 10.0
    private const val HALF = 0.5
    private const val MM_PER_INCH = 25.4
    private const val M_PER_FOOT = 0.3048
    private const val MS_PER_MPH = 0.44704
    private const val FREEZING_F = 32.0
    private const val FIVE = 5.0
    private const val NINE = 9.0
    private const val KELVIN_OFFSET = 273.15
}
