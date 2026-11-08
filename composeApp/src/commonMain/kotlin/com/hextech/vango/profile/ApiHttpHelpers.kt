package com.hextech.vango.profile

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Ktor bazen boş/eksik gövdede [bodyAsText] sırasında "unexpected EOF" vb. fırlatır.
 */
suspend fun HttpResponse.readTextBodyOrThrow(): String =
    runCatching { bodyAsText() }.getOrElse { e ->
        error(
            buildString {
                append("Sunucu yanıtı okunamadı (HTTP ${status.value}).")
                val detail = e.message?.trim().orEmpty()
                if (detail.isNotEmpty()) append(" ").append(detail)
            },
        )
    }

inline fun <reified T> Json.decodeFromStringOrNull(raw: String): T? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    return runCatching { decodeFromString(serializer<T>(), t) }.getOrNull()
}
