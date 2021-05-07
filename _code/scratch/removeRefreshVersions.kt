package scratch

import java.io.File

fun main() {
    val versions = File("http4k/http4k/versions.properties").readLines()
        .filterNot { it.trim().isBlank() }
        .filterNot { it.startsWith("#") }
        .map {
            it.substringAfter("..").split("=").let { it[0] to it[1] }
        }

    val original = File("http4k/http4k/buildSrc/src/main/kotlin/Libs.kt").readText()

    val final = versions.fold(original) { acc, next ->
        acc.replace(next.first + ":_", next.first + ":" + next.second)
    }
    println(final)
}
