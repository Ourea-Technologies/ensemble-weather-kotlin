// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * An hour's freezing level: the lowest altitude at which the air is at 0 °C. Heights
 * are above mean sea level, in the request's length unit.
 *
 * Tagged by `kind` on the wire. A kind this SDK version does not know parses to
 * [Unknown] with its raw [Unknown.kind], never an error.
 */
@Serializable(with = FreezingLevelSerializer::class)
public sealed interface FreezingLevel {
    /** At or below 0 °C at every level: freezing from the surface up. */
    public data object BelowSurface : FreezingLevel

    /**
     * The lowest 0 °C [height].
     *
     * @property aboveFreezingLayerAloft an above-freezing layer sits higher up (a warm nose).
     */
    public data class Height(
        public val height: Double,
        public val aboveFreezingLayerAloft: Boolean,
    ) : FreezingLevel

    /** Above 0 °C at every level read, up to [top]; the freezing level is higher. */
    public data class AboveProfile(
        public val top: Double,
    ) : FreezingLevel

    /** A kind this SDK version does not know; the raw object is kept in [json]. */
    public data class Unknown(
        public val kind: String,
        public val json: JsonObject,
    ) : FreezingLevel
}

/**
 * Reads [FreezingLevel] by its closed `kind` tag. It never instantiates a type named
 * by the payload (SDK-STANDARD "Security": no polymorphic deserialisation).
 */
internal object FreezingLevelSerializer : KSerializer<FreezingLevel> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): FreezingLevel {
        val json = decoder as? JsonDecoder ?: throw SerializationException("FreezingLevel is JSON only")
        val obj = json.decodeJsonElement() as? JsonObject ?: throw SerializationException("freezing_level")
        val kind = obj["kind"]?.jsonPrimitive?.content ?: throw SerializationException("freezing_level.kind")
        return when (kind) {
            "below_surface" -> {
                FreezingLevel.BelowSurface
            }

            "height" -> {
                val height = obj.number("height")
                val aloft = obj["above_freezing_layer_aloft"]?.jsonPrimitive?.booleanOrNull
                if (height == null ||
                    aloft == null
                ) {
                    FreezingLevel.Unknown(kind, obj)
                } else {
                    FreezingLevel.Height(height, aloft)
                }
            }

            "above_profile" -> {
                obj.number("top")?.let(FreezingLevel::AboveProfile) ?: FreezingLevel.Unknown(kind, obj)
            }

            else -> {
                FreezingLevel.Unknown(kind, obj)
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: FreezingLevel,
    ) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("FreezingLevel is JSON only")
        val element: JsonElement =
            when (value) {
                FreezingLevel.BelowSurface -> {
                    buildJsonObject { put("kind", "below_surface") }
                }

                is FreezingLevel.Height -> {
                    buildJsonObject {
                        put("kind", "height")
                        put("height", value.height)
                        put("above_freezing_layer_aloft", value.aboveFreezingLayerAloft)
                    }
                }

                is FreezingLevel.AboveProfile -> {
                    buildJsonObject {
                        put("kind", "above_profile")
                        put("top", value.top)
                    }
                }

                is FreezingLevel.Unknown -> {
                    value.json
                }
            }
        json.encodeJsonElement(element)
    }

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)
            ?.takeIf {
                !it.isString
            }?.doubleOrNull
}
