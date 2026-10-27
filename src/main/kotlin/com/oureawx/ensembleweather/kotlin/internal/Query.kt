// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin.internal

import com.oureawx.ensembleweather.kotlin.UsageException
import java.math.BigDecimal

/**
 * Builds a canonical query string (BEHAVIOR.md section 3.1): only parameters not at
 * their default, each under its own name, in alphabetical order, RFC 3986-encoded.
 */
internal class Query {
    private val params = mutableListOf<Pair<String, String>>()

    /** Adds [name]=[value] unless [value] is null or equals [default]. */
    fun add(
        name: String,
        value: String?,
        default: String? = null,
    ) {
        if (value != null && value != default) params += name to encode(value)
    }

    fun add(
        name: String,
        value: Int?,
        default: Int? = null,
    ) {
        if (value != null && value != default) params += name to value.toString()
    }

    fun add(
        name: String,
        value: Long?,
    ) {
        if (value != null) params += name to value.toString()
    }

    fun add(
        name: String,
        value: Double?,
        default: Double? = null,
    ) {
        if (value != null && value != default) params += name to number(value)
    }

    fun add(
        name: String,
        value: Boolean?,
        default: Boolean,
    ) {
        if (value != null && value != default) params += name to value.toString()
    }

    /** A comma-joined list (each item encoded, so a literal `,` is sent as `%2C`). */
    fun addList(
        name: String,
        values: List<String>?,
        default: List<String>? = null,
    ) {
        if (!values.isNullOrEmpty() && values != default) params += name to encode(values.joinToString(","))
    }

    /** A repeated parameter (`capability=a&capability=b`). */
    fun addRepeated(
        name: String,
        values: List<String>?,
    ) {
        values?.forEach { params += name to encode(it) }
    }

    /** A value already in wire form (`prob`, `totals`: `:` and `,` stay literal). */
    fun addRaw(
        name: String,
        wire: String?,
    ) {
        if (!wire.isNullOrEmpty()) params += name to wire
    }

    fun build(): String = params.sortedBy { it.first }.joinToString("&") { "${it.first}=${it.second}" }

    companion object {
        private val UNRESERVED: Set<Char> = (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~')).toSet()
        private const val HEX = "0123456789ABCDEF"
        private const val NIBBLE = 4
        private const val LOW_NIBBLE = 0x0F
        private const val BYTE = 0xFF

        /** RFC 3986 percent-encoding: unreserved characters as they are, every other byte `%XX`. */
        fun encode(value: String): String =
            buildString {
                for (byte in value.toByteArray(Charsets.UTF_8)) {
                    val c = (byte.toInt() and BYTE).toChar()
                    if (c in UNRESERVED) {
                        append(c)
                    } else {
                        val b = byte.toInt() and BYTE
                        append('%').append(HEX[b shr NIBBLE]).append(HEX[b and LOW_NIBBLE])
                    }
                }
            }

        /** The shortest decimal that round-trips, never an exponent; integral values print with no `.0`. */
        fun number(value: Double): String {
            if (!value.isFinite()) throw UsageException("not a finite number: $value")
            if (value == 0.0) return "0"
            return BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
        }
    }
}
