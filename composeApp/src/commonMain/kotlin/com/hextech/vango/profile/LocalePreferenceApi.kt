package com.hextech.vango.profile

import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LocalePreferencePayload(
    val locale_preference: String,
)

@Serializable
private data class LocalePreferenceResponse(
    val ok: Boolean = false,
    val message: String = "",
    val user: LoginUserDto? = null,
)

object LocalePreferenceApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun update(authToken: String, localePreference: String): Result<Unit> = runCatching {
        val token = authToken.trim()
        if (token.isEmpty()) error("Oturum yok.")

        val payload = LocalePreferencePayload(localePreference.trim().lowercase())
        val response = client.post(HexnetApi.Auth.localePreference) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            setBody(json.encodeToString(LocalePreferencePayload.serializer(), payload))
        }
        val raw = response.readTextBodyOrThrow()
        val body = json.decodeFromStringOrNull<LocalePreferenceResponse>(raw)

        when {
            response.status == HttpStatusCode.OK && body?.ok == true -> Unit
            response.status == HttpStatusCode.BadRequest ->
                error(body?.message?.ifBlank { null } ?: "Geçersiz dil tercihi.")
            response.status == HttpStatusCode.ServiceUnavailable ->
                error(body?.message?.ifBlank { null } ?: "Sunucu dil alanı henüz hazır değil.")
            response.status == HttpStatusCode.Unauthorized ->
                error(body?.message?.ifBlank { null } ?: "Oturum geçersiz.")
            else -> {
                val msg = body?.message?.ifBlank { null }
                    ?: "Dil kaydedilemedi (${response.status})."
                error(msg)
            }
        }
    }
}
