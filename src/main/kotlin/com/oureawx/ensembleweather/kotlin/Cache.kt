// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Where the SDK keeps responses for ETag revalidation (BEHAVIOR.md section 10).
 *
 * The cache is on by default ([InMemoryResponseCache]). Set
 * [EnsembleWeather.Builder.cache] to `null` to turn it off, or to your own
 * implementation to share entries across processes. A key is the full request URL
 * plus a hash of the API key (never the key itself), so keyed and keyless entries
 * never mix. Implementations must be safe to call from several coroutines at once.
 */
public interface ResponseCache {
    /** The entry stored under [key], if any. */
    public suspend fun get(key: String): CachedResponse?

    /** Stores [entry] under [key], replacing any older one. */
    public suspend fun put(
        key: String,
        entry: CachedResponse,
    )

    /** Removes the entry under [key], if any. */
    public suspend fun remove(key: String)
}

/**
 * A stored response.
 *
 * @property etag the `ETag` exactly as received (a weak `W/` prefix included); sent back
 *   byte for byte as `If-None-Match`.
 * @property body the response body.
 * @property freshUntilEpochMillis until when the body may be served without asking the
 *   server, from the response's `max-age` (the API sends `max-age=0`, so every call
 *   revalidates); `null` when it must always be revalidated.
 */
public class CachedResponse(
    public val etag: String,
    public val body: ByteArray,
    public val freshUntilEpochMillis: Long? = null,
) {
    override fun toString(): String = "CachedResponse(etag=$etag, ${body.size} bytes)"
}

/**
 * The default cache: an in-memory LRU bounded to [maxEntries] entries and
 * [maxBytes] bytes of bodies (256 and 32 MiB by default).
 */
public class InMemoryResponseCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) : ResponseCache {
    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
        require(maxBytes > 0) { "maxBytes must be positive" }
    }

    private val mutex = Mutex()
    private val entries = LinkedHashMap<String, CachedResponse>(INITIAL_CAPACITY, LOAD_FACTOR, true)
    private var bytes = 0L

    override suspend fun get(key: String): CachedResponse? = mutex.withLock { entries[key] }

    override suspend fun put(
        key: String,
        entry: CachedResponse,
    ) {
        mutex.withLock {
            entries.remove(key)?.let { bytes -= it.body.size }
            if (entry.body.size > maxBytes) return
            entries[key] = entry
            bytes += entry.body.size
            val eldest = entries.entries.iterator()
            while ((entries.size > maxEntries || bytes > maxBytes) && eldest.hasNext()) {
                bytes -=
                    eldest
                        .next()
                        .value.body.size
                eldest.remove()
            }
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock { entries.remove(key)?.let { bytes -= it.body.size } }
    }

    /** How many entries it holds now. */
    public suspend fun size(): Int = mutex.withLock { entries.size }

    public companion object {
        /** The default entry bound: 256. */
        public const val DEFAULT_MAX_ENTRIES: Int = 256

        /** The default body-size bound: 32 MiB. */
        public const val DEFAULT_MAX_BYTES: Long = 32L * 1024 * 1024
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
}
