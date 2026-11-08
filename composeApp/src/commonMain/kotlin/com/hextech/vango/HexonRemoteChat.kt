package com.hextech.vango

import com.hextech.vango.profile.HexnetApi
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Optional Hexnet-hosted LLM for daily chat when local intent match is weak
 * and the device has internet. Endpoint may not exist yet → returns null.
 */
object HexonRemoteChat {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Request(val message: String, val source: String = "vango-hexon")

    @Serializable
    private data class Response(val reply: String? = null, val text: String? = null)

    suspend fun ask(message: String): String? {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val response = VangoHttpClient.instance.post(HexnetApi.Hexon.chat) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(Request.serializer(), Request(message = trimmed)))
            }
            if (response.status != HttpStatusCode.OK) return null
            val body = response.bodyAsText()
            val parsed = runCatching { json.decodeFromString(Response.serializer(), body) }.getOrNull()
            parsed?.reply?.takeIf { it.isNotBlank() }
                ?: parsed?.text?.takeIf { it.isNotBlank() }
                ?: body.takeIf { it.isNotBlank() && !it.trimStart().startsWith("{") }?.trim()
        } catch (_: Throwable) {
            null
        }
    }
}
