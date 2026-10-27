// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.mock

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Runs every program in examples/ as a user would (its own JVM, the built library on
 * the classpath) against the mock API, never the real one. The Pro-only example runs
 * twice: without a key (it must say so and exit 0) and with a mock key.
 */
fun main() {
    val examplesDir = File(System.getProperty("ourea.examplesDir"))
    val classpath = System.getProperty("ourea.examplesClasspath")
    val java = System.getProperty("ourea.java")
    val mainClasses =
        examplesDir.listFiles { f -> f.extension == "kt" }!!.sortedBy { it.name }.map { file ->
            val pkg = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE).find(file.readText())?.groupValues?.get(1)
            val cls = file.nameWithoutExtension + "Kt"
            file.name to if (pkg == null) cls else "$pkg.$cls"
        }
    MockServer(File(System.getProperty("ourea.bodies"))).use { mock ->
        val runs =
            mainClasses.map { Triple(it.first, it.second, null as String?) } +
                mainClasses.filter { it.first == "PointForecast.kt" }.map {
                    Triple(
                        it.first,
                        it.second,
                        "ow_mock_pro_key",
                    )
                }
        var failed = 0
        for ((file, mainClass, key) in runs) {
            val process =
                ProcessBuilder(java, "-cp", classpath, mainClass)
                    .redirectErrorStream(true)
                    .apply {
                        environment().remove("OUREA_API_KEY")
                        environment()["OUREA_BASE_URL"] = mock.url
                        if (key != null) environment()["OUREA_API_KEY"] = key
                    }.start()
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            val output = process.inputStream.bufferedReader().readText()
            val ok = finished && process.exitValue() == 0 && output.isNotBlank()
            if (!ok) failed++
            println("${if (ok) "PASS" else "FAIL"} $file${if (key != null) " (mock key)" else ""}")
            output.lines().filter { it.isNotBlank() }.forEach { println("    $it") }
        }
        println("${runs.size - failed}/${runs.size} example runs passed")
        if (failed > 0) exitProcess(1)
    }
}

private const val TIMEOUT_SECONDS = 60L
