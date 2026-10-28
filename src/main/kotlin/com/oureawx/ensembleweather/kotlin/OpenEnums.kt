// SPDX-License-Identifier: BSD-2-Clause
@file:Suppress("ktlint:standard:property-naming")

package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

// Open enums (BEHAVIOR.md section 1.5): every enumerated string in a response is
// a value class over the wire string. The companion names the values this SDK
// knows; a value added to the API later still parses, keeps its raw string in
// `value`, and reports `isKnown == false`. Never an error.

/**
 * The unit system of a request and of every value in its response (BEHAVIOR.md section 7).
 *
 * [Metric] (the default): °C, mm, m/s, m. [Imperial]: °F, in, mph, ft. [Kelvin]: metric with temperatures in K.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class Units(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `metric` */
        public val Metric: Units = Units("metric")

        /** `imperial` */
        public val Imperial: Units = Units("imperial")

        /** `kelvin` */
        public val Kelvin: Units = Units("kelvin")

        /** Every value this SDK version knows. */
        public val entries: List<Units> =
            listOf(Metric, Imperial, Kelvin)
        private val known: Set<Units> = entries.toSet()
    }
}

/**
 * A location's primary type, snake_case on the wire (`ski_area`).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class LocationType(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `airport` */
        public val Airport: LocationType = LocationType("airport")

        /** `backcountry` */
        public val Backcountry: LocationType = LocationType("backcountry")

        /** `beach_coast` */
        public val BeachCoast: LocationType = LocationType("beach_coast")

        /** `campground` */
        public val Campground: LocationType = LocationType("campground")

        /** `city_town` */
        public val CityTown: LocationType = LocationType("city_town")

        /** `climbing_area` */
        public val ClimbingArea: LocationType = LocationType("climbing_area")

        /** `coastal_point` */
        public val CoastalPoint: LocationType = LocationType("coastal_point")

        /** `event_venue` */
        public val EventVenue: LocationType = LocationType("event_venue")

        /** `fire_lookout` */
        public val FireLookout: LocationType = LocationType("fire_lookout")

        /** `fishing_river_access` */
        public val FishingRiverAccess: LocationType = LocationType("fishing_river_access")

        /** `golf_course` */
        public val GolfCourse: LocationType = LocationType("golf_course")

        /** `hot_spring` */
        public val HotSpring: LocationType = LocationType("hot_spring")

        /** `lake` */
        public val Lake: LocationType = LocationType("lake")

        /** `mountain_pass` */
        public val MountainPass: LocationType = LocationType("mountain_pass")

        /** `national_park` */
        public val NationalPark: LocationType = LocationType("national_park")

        /** `observation_station` */
        public val ObservationStation: LocationType = LocationType("observation_station")

        /** `other` */
        public val Other: LocationType = LocationType("other")

        /** `park` */
        public val Park: LocationType = LocationType("park")

        /** `recreation_area` */
        public val RecreationArea: LocationType = LocationType("recreation_area")

        /** `reservoir_lake` */
        public val ReservoirLake: LocationType = LocationType("reservoir_lake")

        /** `scenic_tourist` */
        public val ScenicTourist: LocationType = LocationType("scenic_tourist")

        /** `ski_area` */
        public val SkiArea: LocationType = LocationType("ski_area")

        /** `state_park` */
        public val StatePark: LocationType = LocationType("state_park")

        /** `summit` */
        public val Summit: LocationType = LocationType("summit")

        /** `trailhead` */
        public val Trailhead: LocationType = LocationType("trailhead")

        /** `volcano` */
        public val Volcano: LocationType = LocationType("volcano")

        /** `waterfall` */
        public val Waterfall: LocationType = LocationType("waterfall")

        /** `wine_agritourism` */
        public val WineAgritourism: LocationType = LocationType("wine_agritourism")

        /** Every value this SDK version knows. */
        public val entries: List<LocationType> =
            listOf(
                Airport,
                Backcountry,
                BeachCoast,
                Campground,
                CityTown,
                ClimbingArea,
                CoastalPoint,
                EventVenue,
                FireLookout,
                FishingRiverAccess,
                GolfCourse,
                HotSpring,
                Lake,
                MountainPass,
                NationalPark,
                ObservationStation,
                Other,
                Park,
                RecreationArea,
                ReservoirLake,
                ScenicTourist,
                SkiArea,
                StatePark,
                Summit,
                Trailhead,
                Volcano,
                Waterfall,
                WineAgritourism,
            )
        private val known: Set<LocationType> = entries.toSet()
    }
}

/**
 * A forecast variable a probability or window total can be asked about (BEHAVIOR.md section 4).
 *
 * Window totals ([WindowTotalTerm]) take only [Precipitation] and [Snowfall].
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class Variable(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `temperature` */
        public val Temperature: Variable = Variable("temperature")

        /** `wind_speed` */
        public val WindSpeed: Variable = Variable("wind_speed")

        /** `snow_level` */
        public val SnowLevel: Variable = Variable("snow_level")

        /** `precipitation` */
        public val Precipitation: Variable = Variable("precipitation")

        /** `snowfall` */
        public val Snowfall: Variable = Variable("snowfall")

        /** Every value this SDK version knows. */
        public val entries: List<Variable> =
            listOf(Temperature, WindSpeed, SnowLevel, Precipitation, Snowfall)
        private val known: Set<Variable> = entries.toSet()
    }
}

/**
 * How a probability term compares the variable with its threshold: [AtLeast] (`ge`) or [AtMost] (`le`).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class Comparison(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `ge` */
        public val AtLeast: Comparison = Comparison("ge")

        /** `le` */
        public val AtMost: Comparison = Comparison("le")

        /** Every value this SDK version knows. */
        public val entries: List<Comparison> =
            listOf(AtLeast, AtMost)
        private val known: Set<Comparison> = entries.toSet()
    }
}

/**
 * How a probability term reads its window (BEHAVIOR.md section 4).
 *
 * [Total]: the window total (the only one for precipitation and snowfall, and their default).
 * [Any]: at any hour in the window (the default for the others). [All]: at every hour in it.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class Aggregation(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `total` */
        public val Total: Aggregation = Aggregation("total")

        /** `any` */
        public val Any: Aggregation = Aggregation("any")

        /** `all` */
        public val All: Aggregation = Aggregation("all")

        /** Every value this SDK version knows. */
        public val entries: List<Aggregation> =
            listOf(Total, Any, All)
        private val known: Set<Aggregation> = entries.toSet()
    }
}

/**
 * A stable weather condition code to switch on (`partly_cloudy`, `snow_likely`), hourly or daily.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class Condition(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `sunny` */
        public val Sunny: Condition = Condition("sunny")

        /** `mostly_sunny` */
        public val MostlySunny: Condition = Condition("mostly_sunny")

        /** `partly_cloudy` */
        public val PartlyCloudy: Condition = Condition("partly_cloudy")

        /** `mostly_cloudy` */
        public val MostlyCloudy: Condition = Condition("mostly_cloudy")

        /** `cloudy` */
        public val Cloudy: Condition = Condition("cloudy")

        /** `clear` */
        public val Clear: Condition = Condition("clear")

        /** `mostly_clear` */
        public val MostlyClear: Condition = Condition("mostly_clear")

        /** `slight_chance_showers` */
        public val SlightChanceShowers: Condition = Condition("slight_chance_showers")

        /** `chance_showers` */
        public val ChanceShowers: Condition = Condition("chance_showers")

        /** `chance_rain` */
        public val ChanceRain: Condition = Condition("chance_rain")

        /** `showers_likely` */
        public val ShowersLikely: Condition = Condition("showers_likely")

        /** `rain_likely` */
        public val RainLikely: Condition = Condition("rain_likely")

        /** `showers` */
        public val Showers: Condition = Condition("showers")

        /** `rain` */
        public val Rain: Condition = Condition("rain")

        /** `light_rain` */
        public val LightRain: Condition = Condition("light_rain")

        /** `heavy_rain` */
        public val HeavyRain: Condition = Condition("heavy_rain")

        /** `slight_chance_rain_snow_mix` */
        public val SlightChanceRainSnowMix: Condition = Condition("slight_chance_rain_snow_mix")

        /** `chance_rain_snow_mix` */
        public val ChanceRainSnowMix: Condition = Condition("chance_rain_snow_mix")

        /** `rain_snow_mix_likely` */
        public val RainSnowMixLikely: Condition = Condition("rain_snow_mix_likely")

        /** `rain_snow_mix` */
        public val RainSnowMix: Condition = Condition("rain_snow_mix")

        /** `slight_chance_snow` */
        public val SlightChanceSnow: Condition = Condition("slight_chance_snow")

        /** `chance_snow` */
        public val ChanceSnow: Condition = Condition("chance_snow")

        /** `snow_likely` */
        public val SnowLikely: Condition = Condition("snow_likely")

        /** `snow` */
        public val Snow: Condition = Condition("snow")

        /** `slight_chance_freezing_rain` */
        public val SlightChanceFreezingRain: Condition = Condition("slight_chance_freezing_rain")

        /** `chance_freezing_rain` */
        public val ChanceFreezingRain: Condition = Condition("chance_freezing_rain")

        /** `freezing_rain_likely` */
        public val FreezingRainLikely: Condition = Condition("freezing_rain_likely")

        /** `freezing_rain` */
        public val FreezingRain: Condition = Condition("freezing_rain")

        /** Every value this SDK version knows. */
        public val entries: List<Condition> =
            listOf(
                Sunny,
                MostlySunny,
                PartlyCloudy,
                MostlyCloudy,
                Cloudy,
                Clear,
                MostlyClear,
                SlightChanceShowers,
                ChanceShowers,
                ChanceRain,
                ShowersLikely,
                RainLikely,
                Showers,
                Rain,
                LightRain,
                HeavyRain,
                SlightChanceRainSnowMix,
                ChanceRainSnowMix,
                RainSnowMixLikely,
                RainSnowMix,
                SlightChanceSnow,
                ChanceSnow,
                SnowLikely,
                Snow,
                SlightChanceFreezingRain,
                ChanceFreezingRain,
                FreezingRainLikely,
                FreezingRain,
            )
        private val known: Set<Condition> = entries.toSet()
    }
}

/**
 * An icon hint for a condition; nighttime hints start with `night_`.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class ConditionIcon(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `sunny` */
        public val Sunny: ConditionIcon = ConditionIcon("sunny")

        /** `mostly_sunny` */
        public val MostlySunny: ConditionIcon = ConditionIcon("mostly_sunny")

        /** `partly_cloudy` */
        public val PartlyCloudy: ConditionIcon = ConditionIcon("partly_cloudy")

        /** `mostly_cloudy` */
        public val MostlyCloudy: ConditionIcon = ConditionIcon("mostly_cloudy")

        /** `cloudy` */
        public val Cloudy: ConditionIcon = ConditionIcon("cloudy")

        /** `sun_chance_showers` */
        public val SunChanceShowers: ConditionIcon = ConditionIcon("sun_chance_showers")

        /** `sun_showers_likely` */
        public val SunShowersLikely: ConditionIcon = ConditionIcon("sun_showers_likely")

        /** `sun_showers` */
        public val SunShowers: ConditionIcon = ConditionIcon("sun_showers")

        /** `chance_showers` */
        public val ChanceShowers: ConditionIcon = ConditionIcon("chance_showers")

        /** `chance_rain` */
        public val ChanceRain: ConditionIcon = ConditionIcon("chance_rain")

        /** `showers_likely` */
        public val ShowersLikely: ConditionIcon = ConditionIcon("showers_likely")

        /** `rain_likely` */
        public val RainLikely: ConditionIcon = ConditionIcon("rain_likely")

        /** `showers` */
        public val Showers: ConditionIcon = ConditionIcon("showers")

        /** `rain` */
        public val Rain: ConditionIcon = ConditionIcon("rain")

        /** `light_rain` */
        public val LightRain: ConditionIcon = ConditionIcon("light_rain")

        /** `heavy_rain` */
        public val HeavyRain: ConditionIcon = ConditionIcon("heavy_rain")

        /** `rain_snow_mix` */
        public val RainSnowMix: ConditionIcon = ConditionIcon("rain_snow_mix")

        /** `snow` */
        public val Snow: ConditionIcon = ConditionIcon("snow")

        /** `freezing_rain` */
        public val FreezingRain: ConditionIcon = ConditionIcon("freezing_rain")

        /** `night_clear` */
        public val NightClear: ConditionIcon = ConditionIcon("night_clear")

        /** `night_mostly_clear` */
        public val NightMostlyClear: ConditionIcon = ConditionIcon("night_mostly_clear")

        /** `night_partly_cloudy` */
        public val NightPartlyCloudy: ConditionIcon = ConditionIcon("night_partly_cloudy")

        /** `night_mostly_cloudy` */
        public val NightMostlyCloudy: ConditionIcon = ConditionIcon("night_mostly_cloudy")

        /** `night_cloudy` */
        public val NightCloudy: ConditionIcon = ConditionIcon("night_cloudy")

        /** `night_moon_chance_showers` */
        public val NightMoonChanceShowers: ConditionIcon = ConditionIcon("night_moon_chance_showers")

        /** `night_moon_showers_likely` */
        public val NightMoonShowersLikely: ConditionIcon = ConditionIcon("night_moon_showers_likely")

        /** `night_chance_showers` */
        public val NightChanceShowers: ConditionIcon = ConditionIcon("night_chance_showers")

        /** `night_showers_likely` */
        public val NightShowersLikely: ConditionIcon = ConditionIcon("night_showers_likely")

        /** `night_rain` */
        public val NightRain: ConditionIcon = ConditionIcon("night_rain")

        /** `night_light_rain` */
        public val NightLightRain: ConditionIcon = ConditionIcon("night_light_rain")

        /** `night_heavy_rain` */
        public val NightHeavyRain: ConditionIcon = ConditionIcon("night_heavy_rain")

        /** `night_rain_snow_mix` */
        public val NightRainSnowMix: ConditionIcon = ConditionIcon("night_rain_snow_mix")

        /** `night_snow` */
        public val NightSnow: ConditionIcon = ConditionIcon("night_snow")

        /** `night_freezing_rain` */
        public val NightFreezingRain: ConditionIcon = ConditionIcon("night_freezing_rain")

        /** `night_chance_rain` */
        public val NightChanceRain: ConditionIcon = ConditionIcon("night_chance_rain")

        /** `night_rain_likely` */
        public val NightRainLikely: ConditionIcon = ConditionIcon("night_rain_likely")

        /** `night_showers` */
        public val NightShowers: ConditionIcon = ConditionIcon("night_showers")

        /** `night_moon_showers` */
        public val NightMoonShowers: ConditionIcon = ConditionIcon("night_moon_showers")

        /** Every value this SDK version knows. */
        public val entries: List<ConditionIcon> =
            listOf(
                Sunny,
                MostlySunny,
                PartlyCloudy,
                MostlyCloudy,
                Cloudy,
                SunChanceShowers,
                SunShowersLikely,
                SunShowers,
                ChanceShowers,
                ChanceRain,
                ShowersLikely,
                RainLikely,
                Showers,
                Rain,
                LightRain,
                HeavyRain,
                RainSnowMix,
                Snow,
                FreezingRain,
                NightClear,
                NightMostlyClear,
                NightPartlyCloudy,
                NightMostlyCloudy,
                NightCloudy,
                NightMoonChanceShowers,
                NightMoonShowersLikely,
                NightChanceShowers,
                NightShowersLikely,
                NightRain,
                NightLightRain,
                NightHeavyRain,
                NightRainSnowMix,
                NightSnow,
                NightFreezingRain,
                NightChanceRain,
                NightRainLikely,
                NightShowers,
                NightMoonShowers,
            )
        private val known: Set<ConditionIcon> = entries.toSet()
    }
}

/**
 * A precipitation type: an hour's or day's `precipitation_type`, or a day's
 * `precipitation_display.kind` (which adds [None]).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class PrecipitationKind(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `none` */
        public val None: PrecipitationKind = PrecipitationKind("none")

        /** `rain` */
        public val Rain: PrecipitationKind = PrecipitationKind("rain")

        /** `snow` */
        public val Snow: PrecipitationKind = PrecipitationKind("snow")

        /** `rain_snow_mix` */
        public val RainSnowMix: PrecipitationKind = PrecipitationKind("rain_snow_mix")

        /** `freezing_rain` */
        public val FreezingRain: PrecipitationKind = PrecipitationKind("freezing_rain")

        /** Every value this SDK version knows. */
        public val entries: List<PrecipitationKind> =
            listOf(None, Rain, Snow, RainSnowMix, FreezingRain)
        private val known: Set<PrecipitationKind> = entries.toSet()
    }
}

/**
 * Which member variables a forecast carries: [Plume] (Free) or [Full] (Pro).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class MembersView(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `plume` */
        public val Plume: MembersView = MembersView("plume")

        /** `full` */
        public val Full: MembersView = MembersView("full")

        /** Every value this SDK version knows. */
        public val entries: List<MembersView> =
            listOf(Plume, Full)
        private val known: Set<MembersView> = entries.toSet()
    }
}

/**
 * A live nowcast source a body was built without (`nowcast_unavailable`).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class NowcastSource(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `observation` */
        public val Observation: NowcastSource = NowcastSource("observation")

        /** `radar` */
        public val Radar: NowcastSource = NowcastSource("radar")

        /** `cloud` */
        public val Cloud: NowcastSource = NowcastSource("cloud")

        /** `nearest_sensors` */
        public val NearestSensors: NowcastSource = NowcastSource("nearest_sensors")

        /** Every value this SDK version knows. */
        public val entries: List<NowcastSource> =
            listOf(Observation, Radar, Cloud, NearestSensors)
        private val known: Set<NowcastSource> = entries.toSet()
    }
}

/**
 * Whether a live observation carried weight in a "right now" block ([Nowcast]) or not ([Model]).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class ConditionsSource(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `nowcast` */
        public val Nowcast: ConditionsSource = ConditionsSource("nowcast")

        /** `model` */
        public val Model: ConditionsSource = ConditionsSource("model")

        /** Every value this SDK version knows. */
        public val entries: List<ConditionsSource> =
            listOf(Nowcast, Model)
        private val known: Set<ConditionsSource> = entries.toSet()
    }
}

/**
 * Where a coordinate forecast's elevation came from: the request, or the USGS 3DEP 10 m DEM.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class ElevationSource(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `request` */
        public val Request: ElevationSource = ElevationSource("request")

        /** `dem_3dep_10m` */
        public val Dem3dep10m: ElevationSource = ElevationSource("dem_3dep_10m")

        /** Every value this SDK version knows. */
        public val entries: List<ElevationSource> =
            listOf(Request, Dem3dep10m)
        private val known: Set<ElevationSource> = entries.toSet()
    }
}

/**
 * The kind of a temperature inversion.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class InversionKind(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `surface_shallow` */
        public val SurfaceShallow: InversionKind = InversionKind("surface_shallow")

        /** `cold_pool` */
        public val ColdPool: InversionKind = InversionKind("cold_pool")

        /** `elevated` */
        public val Elevated: InversionKind = InversionKind("elevated")

        /** Every value this SDK version knows. */
        public val entries: List<InversionKind> =
            listOf(SurfaceShallow, ColdPool, Elevated)
        private val known: Set<InversionKind> = entries.toSet()
    }
}

/**
 * Columbia Gorge gap-flow strength.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class GapFlowStatus(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `none` */
        public val None: GapFlowStatus = GapFlowStatus("none")

        /** `moderate` */
        public val Moderate: GapFlowStatus = GapFlowStatus("moderate")

        /** `strong` */
        public val Strong: GapFlowStatus = GapFlowStatus("strong")

        /** `extreme` */
        public val Extreme: GapFlowStatus = GapFlowStatus("extreme")

        /** Every value this SDK version knows. */
        public val entries: List<GapFlowStatus> =
            listOf(None, Moderate, Strong, Extreme)
        private val known: Set<GapFlowStatus> = entries.toSet()
    }
}

/**
 * A day's wind severity class.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class WindStatus(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `calm` */
        public val Calm: WindStatus = WindStatus("calm")

        /** `light` */
        public val Light: WindStatus = WindStatus("light")

        /** `moderate` */
        public val Moderate: WindStatus = WindStatus("moderate")

        /** `strong` */
        public val Strong: WindStatus = WindStatus("strong")

        /** `very_strong` */
        public val VeryStrong: WindStatus = WindStatus("very_strong")

        /** `extreme` */
        public val Extreme: WindStatus = WindStatus("extreme")

        /** Every value this SDK version knows. */
        public val entries: List<WindStatus> =
            listOf(Calm, Light, Moderate, Strong, VeryStrong, Extreme)
        private val known: Set<WindStatus> = entries.toSet()
    }
}

/**
 * How snow density changes over a storm.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class StormOrientation(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `right_side_up` */
        public val RightSideUp: StormOrientation = StormOrientation("right_side_up")

        /** `upside_down` */
        public val UpsideDown: StormOrientation = StormOrientation("upside_down")

        /** `mixed` */
        public val Mixed: StormOrientation = StormOrientation("mixed")

        /** Every value this SDK version knows. */
        public val entries: List<StormOrientation> =
            listOf(RightSideUp, UpsideDown, Mixed)
        private val known: Set<StormOrientation> = entries.toSet()
    }
}

/**
 * The eight-way moon phase.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class MoonPhase(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `new_moon` */
        public val NewMoon: MoonPhase = MoonPhase("new_moon")

        /** `waxing_crescent` */
        public val WaxingCrescent: MoonPhase = MoonPhase("waxing_crescent")

        /** `first_quarter` */
        public val FirstQuarter: MoonPhase = MoonPhase("first_quarter")

        /** `waxing_gibbous` */
        public val WaxingGibbous: MoonPhase = MoonPhase("waxing_gibbous")

        /** `full_moon` */
        public val FullMoon: MoonPhase = MoonPhase("full_moon")

        /** `waning_gibbous` */
        public val WaningGibbous: MoonPhase = MoonPhase("waning_gibbous")

        /** `last_quarter` */
        public val LastQuarter: MoonPhase = MoonPhase("last_quarter")

        /** `waning_crescent` */
        public val WaningCrescent: MoonPhase = MoonPhase("waning_crescent")

        /** Every value this SDK version knows. */
        public val entries: List<MoonPhase> =
            listOf(
                NewMoon,
                WaxingCrescent,
                FirstQuarter,
                WaxingGibbous,
                FullMoon,
                WaningGibbous,
                LastQuarter,
                WaningCrescent,
            )
        private val known: Set<MoonPhase> = entries.toSet()
    }
}

/**
 * The astronomical season (hemisphere-aware).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class Season(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `spring` */
        public val Spring: Season = Season("spring")

        /** `summer` */
        public val Summer: Season = Season("summer")

        /** `autumn` */
        public val Autumn: Season = Season("autumn")

        /** `winter` */
        public val Winter: Season = Season("winter")

        /** Every value this SDK version knows. */
        public val entries: List<Season> =
            listOf(Spring, Summer, Autumn, Winter)
        private val known: Set<Season> = entries.toSet()
    }
}

/**
 * The kind of a data source in the attribution registry.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class SourceKind(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `model` */
        public val Model: SourceKind = SourceKind("model")

        /** `peer` */
        public val Peer: SourceKind = SourceKind("peer")

        /** `observation` */
        public val Observation: SourceKind = SourceKind("observation")

        /** `alerts` */
        public val Alerts: SourceKind = SourceKind("alerts")

        /** `climatology` */
        public val Climatology: SourceKind = SourceKind("climatology")

        /** `terrain` */
        public val Terrain: SourceKind = SourceKind("terrain")

        /** `places` */
        public val Places: SourceKind = SourceKind("places")

        /** `basemap` */
        public val Basemap: SourceKind = SourceKind("basemap")

        /** Every value this SDK version knows. */
        public val entries: List<SourceKind> =
            listOf(Model, Peer, Observation, Alerts, Climatology, Terrain, Places, Basemap)
        private val known: Set<SourceKind> = entries.toSet()
    }
}

/**
 * Why a probability or a window total is `null` (BEHAVIOR.md sections 4 and 5).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class NullReason(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `window_not_covered` */
        public val WindowNotCovered: NullReason = NullReason("window_not_covered")

        /** `too_few_members` */
        public val TooFewMembers: NullReason = NullReason("too_few_members")

        /** `variable_unavailable` */
        public val VariableUnavailable: NullReason = NullReason("variable_unavailable")

        /** Every value this SDK version knows. */
        public val entries: List<NullReason> =
            listOf(WindowNotCovered, TooFewMembers, VariableUnavailable)
        private val known: Set<NullReason> = entries.toSet()
    }
}

/**
 * How a window total reads its window: [Total] for an amount (the only one this SDK asks for).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class TotalAggregation(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `total` */
        public val Total: TotalAggregation = TotalAggregation("total")

        /** `min` */
        public val Min: TotalAggregation = TotalAggregation("min")

        /** `max` */
        public val Max: TotalAggregation = TotalAggregation("max")

        /** Every value this SDK version knows. */
        public val entries: List<TotalAggregation> =
            listOf(Total, Min, Max)
        private val known: Set<TotalAggregation> = entries.toSet()
    }
}

/**
 * The CAP severity of an alert.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class AlertSeverity(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `extreme` */
        public val Extreme: AlertSeverity = AlertSeverity("extreme")

        /** `severe` */
        public val Severe: AlertSeverity = AlertSeverity("severe")

        /** `moderate` */
        public val Moderate: AlertSeverity = AlertSeverity("moderate")

        /** `minor` */
        public val Minor: AlertSeverity = AlertSeverity("minor")

        /** `unknown` */
        public val Unknown: AlertSeverity = AlertSeverity("unknown")

        /** Every value this SDK version knows. */
        public val entries: List<AlertSeverity> =
            listOf(Extreme, Severe, Moderate, Minor, Unknown)
        private val known: Set<AlertSeverity> = entries.toSet()
    }
}

/**
 * The CAP urgency of an alert.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class AlertUrgency(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `immediate` */
        public val Immediate: AlertUrgency = AlertUrgency("immediate")

        /** `expected` */
        public val Expected: AlertUrgency = AlertUrgency("expected")

        /** `future` */
        public val Future: AlertUrgency = AlertUrgency("future")

        /** `past` */
        public val Past: AlertUrgency = AlertUrgency("past")

        /** `unknown` */
        public val Unknown: AlertUrgency = AlertUrgency("unknown")

        /** Every value this SDK version knows. */
        public val entries: List<AlertUrgency> =
            listOf(Immediate, Expected, Future, Past, Unknown)
        private val known: Set<AlertUrgency> = entries.toSet()
    }
}

/**
 * The CAP certainty of an alert.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class AlertCertainty(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `observed` */
        public val Observed: AlertCertainty = AlertCertainty("observed")

        /** `likely` */
        public val Likely: AlertCertainty = AlertCertainty("likely")

        /** `possible` */
        public val Possible: AlertCertainty = AlertCertainty("possible")

        /** `unlikely` */
        public val Unlikely: AlertCertainty = AlertCertainty("unlikely")

        /** `unknown` */
        public val Unknown: AlertCertainty = AlertCertainty("unknown")

        /** Every value this SDK version knows. */
        public val entries: List<AlertCertainty> =
            listOf(Observed, Likely, Possible, Unlikely, Unknown)
        private val known: Set<AlertCertainty> = entries.toSet()
    }
}

/**
 * The observation network of a catalog station (`snotel`, `rwis_wsdot`).
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class StationNetwork(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `snotel` */
        public val Snotel: StationNetwork = StationNetwork("snotel")

        /** `snolite` */
        public val Snolite: StationNetwork = StationNetwork("snolite")

        /** `raws` */
        public val Raws: StationNetwork = StationNetwork("raws")

        /** `asos` */
        public val Asos: StationNetwork = StationNetwork("asos")

        /** `awos` */
        public val Awos: StationNetwork = StationNetwork("awos")

        /** `metar` */
        public val Metar: StationNetwork = StationNetwork("metar")

        /** `rwis_wsdot` */
        public val RwisWsdot: StationNetwork = StationNetwork("rwis_wsdot")

        /** `rwis_odot` */
        public val RwisOdot: StationNetwork = StationNetwork("rwis_odot")

        /** `rwis_caltrans` */
        public val RwisCaltrans: StationNetwork = StationNetwork("rwis_caltrans")

        /** `rwis_idot` */
        public val RwisIdot: StationNetwork = StationNetwork("rwis_idot")

        /** `rwis_ndot` */
        public val RwisNdot: StationNetwork = StationNetwork("rwis_ndot")

        /** `rwis_udot` */
        public val RwisUdot: StationNetwork = StationNetwork("rwis_udot")

        /** `rwis_cdot` */
        public val RwisCdot: StationNetwork = StationNetwork("rwis_cdot")

        /** `rwis_mdt` */
        public val RwisMdt: StationNetwork = StationNetwork("rwis_mdt")

        /** `rwis_wydot` */
        public val RwisWydot: StationNetwork = StationNetwork("rwis_wydot")

        /** `rwis_other` */
        public val RwisOther: StationNetwork = StationNetwork("rwis_other")

        /** `agrimet` */
        public val Agrimet: StationNetwork = StationNetwork("agrimet")

        /** `agweathernet` */
        public val Agweathernet: StationNetwork = StationNetwork("agweathernet")

        /** `cocorahs` */
        public val Cocorahs: StationNetwork = StationNetwork("cocorahs")

        /** `coop` */
        public val Coop: StationNetwork = StationNetwork("coop")

        /** `usgs` */
        public val Usgs: StationNetwork = StationNetwork("usgs")

        /** `tempest` */
        public val Tempest: StationNetwork = StationNetwork("tempest")

        /** `davis` */
        public val Davis: StationNetwork = StationNetwork("davis")

        /** `wxflow` */
        public val Wxflow: StationNetwork = StationNetwork("wxflow")

        /** `cdec` */
        public val Cdec: StationNetwork = StationNetwork("cdec")

        /** `cimis` */
        public val Cimis: StationNetwork = StationNetwork("cimis")

        /** `mesonet_oregon` */
        public val MesonetOregon: StationNetwork = StationNetwork("mesonet_oregon")

        /** `mesonet_synoptic` */
        public val MesonetSynoptic: StationNetwork = StationNetwork("mesonet_synoptic")

        /** `mesonet_other` */
        public val MesonetOther: StationNetwork = StationNetwork("mesonet_other")

        /** `ndbc_buoy` */
        public val NdbcBuoy: StationNetwork = StationNetwork("ndbc_buoy")

        /** `other` */
        public val Other: StationNetwork = StationNetwork("other")

        /** `hads` */
        public val Hads: StationNetwork = StationNetwork("hads")

        /** `madis` */
        public val Madis: StationNetwork = StationNetwork("madis")

        /** `nwac` */
        public val Nwac: StationNetwork = StationNetwork("nwac")

        /** `mt_mesonet` */
        public val MtMesonet: StationNetwork = StationNetwork("mt_mesonet")

        /** Every value this SDK version knows. */
        public val entries: List<StationNetwork> =
            listOf(
                Snotel,
                Snolite,
                Raws,
                Asos,
                Awos,
                Metar,
                RwisWsdot,
                RwisOdot,
                RwisCaltrans,
                RwisIdot,
                RwisNdot,
                RwisUdot,
                RwisCdot,
                RwisMdt,
                RwisWydot,
                RwisOther,
                Agrimet,
                Agweathernet,
                Cocorahs,
                Coop,
                Usgs,
                Tempest,
                Davis,
                Wxflow,
                Cdec,
                Cimis,
                MesonetOregon,
                MesonetSynoptic,
                MesonetOther,
                NdbcBuoy,
                Other,
                Hads,
                Madis,
                Nwac,
                MtMesonet,
            )
        private val known: Set<StationNetwork> = entries.toSet()
    }
}

/**
 * The reporting status of a catalog station.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class StationStatus(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `active` */
        public val Active: StationStatus = StationStatus("active")

        /** `intermittent` */
        public val Intermittent: StationStatus = StationStatus("intermittent")

        /** `inactive` */
        public val Inactive: StationStatus = StationStatus("inactive")

        /** `decommissioned` */
        public val Decommissioned: StationStatus = StationStatus("decommissioned")

        /** `unknown` */
        public val Unknown: StationStatus = StationStatus("unknown")

        /** Every value this SDK version knows. */
        public val entries: List<StationStatus> =
            listOf(Active, Intermittent, Inactive, Decommissioned, Unknown)
        private val known: Set<StationStatus> = entries.toSet()
    }
}

/**
 * The variant of a catalog page template.
 *
 * An open enum: a value this SDK version does not know parses with `isKnown == false`
 * and keeps the raw string in [value].
 */
@Serializable
@JvmInline
public value class TemplateVariant(
    public val value: String,
) {
    /** Whether this SDK version knows this value (false for one the API added later). */
    public val isKnown: Boolean get() = this in known

    override fun toString(): String = value

    public companion object {
        /** `mountain` */
        public val Mountain: TemplateVariant = TemplateVariant("mountain")

        /** `coastal` */
        public val Coastal: TemplateVariant = TemplateVariant("coastal")

        /** `trail` */
        public val Trail: TemplateVariant = TemplateVariant("trail")

        /** `pass` */
        public val Pass: TemplateVariant = TemplateVariant("pass")

        /** `generic` */
        public val Generic: TemplateVariant = TemplateVariant("generic")

        /** `station` */
        public val Station: TemplateVariant = TemplateVariant("station")

        /** Every value this SDK version knows. */
        public val entries: List<TemplateVariant> =
            listOf(Mountain, Coastal, Trail, Pass, Generic, Station)
        private val known: Set<TemplateVariant> = entries.toSet()
    }
}
