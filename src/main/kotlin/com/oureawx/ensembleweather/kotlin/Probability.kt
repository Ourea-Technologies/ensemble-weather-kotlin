// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * A threshold for a probability term: a [value] in a [unit], or in the request's own
 * units when [unit] is `null`. A threshold in another unit (6 inches on a metric
 * request) is converted exactly before sending (BEHAVIOR.md section 7); that converts
 * your input, never a response value.
 *
 * Build one with the unit extensions: `6.inches`, `150.mm`, `32.fahrenheit`,
 * `-5.celsius`, `1500.meters`, `5000.feet`, `15.metersPerSecond`, `30.mph`, `273.15.kelvin`.
 */
public data class Threshold(
    public val value: Double,
    public val unit: ThresholdUnit? = null,
) {
    override fun toString(): String = if (unit == null) "$value" else "$value ${unit.symbol}"
}

/** The units a [Threshold] can be given in. */
public enum class ThresholdUnit(
    internal val symbol: String,
    internal val dimension: Dimension,
) {
    /** Millimetres (precipitation, snowfall). */
    Millimeters("mm", Dimension.Amount),

    /** Inches (precipitation, snowfall). */
    Inches("in", Dimension.Amount),

    /** Degrees Celsius. */
    Celsius("degC", Dimension.Temperature),

    /** Degrees Fahrenheit. */
    Fahrenheit("degF", Dimension.Temperature),

    /** Kelvin. */
    Kelvin("K", Dimension.Temperature),

    /** Metres (snow level). */
    Meters("m", Dimension.Height),

    /** Feet (snow level). */
    Feet("ft", Dimension.Height),

    /** Metres per second (wind speed). */
    MetersPerSecond("m/s", Dimension.Speed),

    /** Miles per hour (wind speed). */
    MilesPerHour("mph", Dimension.Speed),
    ;

    internal enum class Dimension { Amount, Temperature, Height, Speed }
}

/** This many millimetres. */
public val Number.mm: Threshold get() = Threshold(toDouble(), ThresholdUnit.Millimeters)

/** This many inches. */
public val Number.inches: Threshold get() = Threshold(toDouble(), ThresholdUnit.Inches)

/** This many degrees Celsius. */
public val Number.celsius: Threshold get() = Threshold(toDouble(), ThresholdUnit.Celsius)

/** This many degrees Fahrenheit. */
public val Number.fahrenheit: Threshold get() = Threshold(toDouble(), ThresholdUnit.Fahrenheit)

/** This many kelvin. */
public val Number.kelvin: Threshold get() = Threshold(toDouble(), ThresholdUnit.Kelvin)

/** This many metres. */
public val Number.meters: Threshold get() = Threshold(toDouble(), ThresholdUnit.Meters)

/** This many feet. */
public val Number.feet: Threshold get() = Threshold(toDouble(), ThresholdUnit.Feet)

/** This many metres per second. */
public val Number.metersPerSecond: Threshold get() = Threshold(toDouble(), ThresholdUnit.MetersPerSecond)

/** This many miles per hour. */
public val Number.mph: Threshold get() = Threshold(toDouble(), ThresholdUnit.MilesPerHour)

/**
 * A forecast window in whole hours from the first served hour (the current hour by
 * default): hours [startHours] up to [endHours]. At most 360 hours.
 */
public data class Window(
    public val startHours: Int,
    public val endHours: Int,
) {
    override fun toString(): String = if (startHours == 0) "${endHours}h" else "$startHours-${endHours}h"

    public companion object {
        /** The next [hours] hours: `Window.next(24)` is the default window. */
        public fun next(hours: Int): Window = Window(0, hours)

        /** The window spanning [duration] from the first served hour (`24.hours`). */
        public fun of(duration: Duration): Window = Window(0, wholeHours(duration))

        /** The window from [range]'s start to its end (`6.hours..18.hours`). */
        public fun of(range: ClosedRange<Duration>): Window =
            Window(wholeHours(range.start), wholeHours(range.endInclusive))

        /** The 24-hour default. */
        public val Default: Window = next(DEFAULT_HOURS)

        /** The longest window the API serves, in hours. */
        public const val MAX_HOURS: Int = 360

        private const val DEFAULT_HOURS = 24

        private fun wholeHours(duration: Duration): Int {
            val hours = duration.inWholeHours
            if (hours.hours != duration) throw UsageException("a window is whole hours, not $duration")
            if (hours > MAX_HOURS) throw UsageException("a window ends at most $MAX_HOURS h ahead, not $duration")
            return hours.toInt()
        }
    }
}

/**
 * One probability question for the server (BEHAVIOR.md section 4): "the chance that
 * [variable] is [op] [threshold] in [window]". The server answers it from the full
 * ensemble as a [ProbabilityResult]; the SDK never derives a probability itself.
 *
 * Build it with the infix forms, then pass it to [ForecastOptions.chance]:
 * ```kotlin
 * Variable.Snowfall atLeast 6.inches within 24.hours
 * Variable.Temperature atMost 32.fahrenheit within (6.hours..18.hours) using Aggregation.All
 * ```
 *
 * @property aggregation `null` for the variable's default ([Aggregation.Total] for
 *   precipitation and snowfall, [Aggregation.Any] for the others).
 */
public data class ProbabilityTerm(
    public val variable: Variable,
    public val op: Comparison,
    public val threshold: Threshold,
    public val window: Window = Window.Default,
    public val aggregation: Aggregation? = null,
)

/** The chance that this variable is at least [threshold] (`ge`). */
public infix fun Variable.atLeast(threshold: Threshold): ProbabilityTerm =
    ProbabilityTerm(this, Comparison.AtLeast, threshold)

/** The chance that this variable is at least [threshold], in the request's units. */
public infix fun Variable.atLeast(threshold: Number): ProbabilityTerm = atLeast(Threshold(threshold.toDouble()))

/** The chance that this variable is at most [threshold] (`le`). */
public infix fun Variable.atMost(threshold: Threshold): ProbabilityTerm =
    ProbabilityTerm(this, Comparison.AtMost, threshold)

/** The chance that this variable is at most [threshold], in the request's units. */
public infix fun Variable.atMost(threshold: Number): ProbabilityTerm = atMost(Threshold(threshold.toDouble()))

/** This term over the next [duration] (whole hours). */
public infix fun ProbabilityTerm.within(duration: Duration): ProbabilityTerm = copy(window = Window.of(duration))

/** This term over [range] (`6.hours..18.hours`). */
public infix fun ProbabilityTerm.within(range: ClosedRange<Duration>): ProbabilityTerm = copy(window = Window.of(range))

/** This term over [window]. */
public infix fun ProbabilityTerm.within(window: Window): ProbabilityTerm = copy(window = window)

/** This term read with [aggregation] (any hour, every hour, or the window total). */
public infix fun ProbabilityTerm.using(aggregation: Aggregation): ProbabilityTerm = copy(aggregation = aggregation)

/**
 * One window-total question (BEHAVIOR.md section 5): the distribution of each
 * member's own [variable] total over [window], answered as a [WindowTotal]. This is
 * the only storm total the SDK offers; it never sums hourly quantiles.
 *
 * ```kotlin
 * Variable.Snowfall totalWithin 72.hours
 * ```
 */
public data class WindowTotalTerm(
    public val variable: Variable,
    public val window: Window = Window.Default,
)

/** The window total of this variable (precipitation or snowfall) over the next [duration]. */
public infix fun Variable.totalWithin(duration: Duration): WindowTotalTerm = WindowTotalTerm(this, Window.of(duration))

/** The window total of this variable (precipitation or snowfall) over [range]. */
public infix fun Variable.totalWithin(range: ClosedRange<Duration>): WindowTotalTerm =
    WindowTotalTerm(this, Window.of(range))

/** The window total of this variable (precipitation or snowfall) over [window]. */
public infix fun Variable.totalWithin(window: Window): WindowTotalTerm = WindowTotalTerm(this, window)
