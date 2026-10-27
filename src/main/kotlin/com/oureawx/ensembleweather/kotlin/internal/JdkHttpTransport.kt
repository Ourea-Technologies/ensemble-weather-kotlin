// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin.internal

import com.oureawx.ensembleweather.kotlin.HttpRequest
import com.oureawx.ensembleweather.kotlin.HttpResponse
import com.oureawx.ensembleweather.kotlin.HttpTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import java.net.URI
import java.net.http.HttpClient
import kotlin.time.Duration
import java.net.http.HttpRequest as JdkRequest
import java.net.http.HttpResponse as JdkResponse

/**
 * The default transport on the JVM: `java.net.http.HttpClient` (JDK 11+), which never
 * follows redirects (the SDK does) and always verifies TLS. Not on Android, which has
 * no `java.net.http`: an Android app passes its own [HttpTransport].
 */
internal class JdkHttpTransport(
    connectTimeout: Duration,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : HttpTransport {
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(java.time.Duration.ofMillis(connectTimeout.inWholeMilliseconds))
            .build()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        val builder = JdkRequest.newBuilder(URI.create(request.url)).GET()
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        val response = client.sendAsync(builder.build(), JdkResponse.BodyHandlers.ofInputStream()).await()
        val body = runInterruptible(io) { HttpTransport.readBounded(response.body()) }
        return HttpResponse(response.statusCode(), response.headers().map(), body)
    }

    companion object {
        /** Whether this runtime has `java.net.http` (JDK 11+; not Android). */
        val available: Boolean by lazy {
            try {
                Class.forName("java.net.http.HttpClient")
                true
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: LinkageError) {
                false
            }
        }
    }
}
