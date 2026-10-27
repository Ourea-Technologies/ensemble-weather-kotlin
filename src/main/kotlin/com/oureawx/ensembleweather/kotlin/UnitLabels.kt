// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

// The unit of each quantity in a response, for its unit system (BEHAVIOR.md section 7).
// Labels only: the SDK never converts a response value; ask the server for other units.

/** The temperature unit: `°C`, `°F` or `K`. */
public val Units.temperatureLabel: String
    get() =
        when (this) {
            Units.Imperial -> "°F"
            Units.Kelvin -> "K"
            else -> "°C"
        }

/** The precipitation and snowfall unit: `mm` or `in`. */
public val Units.amountLabel: String get() = if (this == Units.Imperial) "in" else "mm"

/** The wind-speed unit: `m/s` or `mph`. */
public val Units.speedLabel: String get() = if (this == Units.Imperial) "mph" else "m/s"

/** The length unit of snow levels and elevations: `m` or `ft`. */
public val Units.lengthLabel: String get() = if (this == Units.Imperial) "ft" else "m"
