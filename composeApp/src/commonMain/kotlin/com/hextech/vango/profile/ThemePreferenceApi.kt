package com.hextech.vango.profile

import com.hextech.vango.VangoHttpClient
import com.hextech.vango.normalizeThemeAccentId
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AppearancePreferencePayload(
    val theme_preference: String? = null,
    val theme_accent_id: String? = null,
)

@Serializable
private data class ThemePreferenceResponse(
    val ok: Boolean = false,
    val message: String = "",
    val user: LoginUserDto? = null,
)

object ThemePreferenceApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun update(
        authToken: String,
        themePreference: String? = null,
        themeAccentId: String? = null,
    ): Result<Unit> = runCatching {
        val token = authToken.trim()
        if (token.isEmpty()) error("Oturum yok.")

        val theme = themePreference?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val accent = themeAccentId?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { normalizeThemeAccentId(it) }

        if (theme.isNullOrEmpty() && accent.isNullOrEmpty()) {
            error("Kaydedilecek görünüm alanı yok.")
        }

        val payload = AppearancePreferencePayload(
            theme_preference = theme,
            theme_accent_id = accent,
        )
        val response = client.post(HexnetApi.Auth.themePreference) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            setBody(json.encodeToString(AppearancePreferencePayload.serializer(), payload))
        }
        val raw = response.readTextBodyOrThrow()
        val body = json.decodeFromStringOrNull<ThemePreferenceResponse>(raw)

        when {
            response.status == HttpStatusCode.OK && body?.ok == true -> Unit
            response.status == HttpStatusCode.BadRequest ->
                error(body?.message?.ifBlank { null } ?: "Geçersiz görünüm tercihi.")
            response.status == HttpStatusCode.ServiceUnavailable ->
                error(body?.message?.ifBlank { null } ?: "Sunucu görünüm alanı henüz hazır değil.")
            response.status == HttpStatusCode.Unauthorized ->
                error(body?.message?.ifBlank { null } ?: "Oturum geçersiz.")
            else -> {
                val msg = body?.message?.ifBlank { null }
                    ?: if (raw.isBlank()) "Görünüm kaydedilemedi (${response.status})."
                    else "Görünüm kaydedilemedi (${response.status})."
                error(msg)
            }
        }
    }
}
