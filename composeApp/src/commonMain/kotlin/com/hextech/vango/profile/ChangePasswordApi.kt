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
private data class ChangePasswordPayload(
    val current_password: String,
    val new_password: String,
)

@Serializable
private data class ChangePasswordResponse(
    val ok: Boolean = false,
    val message: String = "",
)

object ChangePasswordApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun update(
        authToken: String,
        currentPassword: String,
        newPassword: String,
    ): Result<Unit> = runCatching {
        val token = authToken.trim()
        if (token.isEmpty()) error("Oturum yok.")
        val payload = ChangePasswordPayload(
            current_password = currentPassword,
            new_password = newPassword,
        )
        val response = client.post(HexnetApi.Auth.changePassword) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            setBody(json.encodeToString(ChangePasswordPayload.serializer(), payload))
        }
        val raw = response.readTextBodyOrThrow()
        val body = json.decodeFromStringOrNull<ChangePasswordResponse>(raw)
        when {
            response.status == HttpStatusCode.OK && body?.ok == true -> Unit
            response.status == HttpStatusCode.Unauthorized ->
                error(body?.message?.ifBlank { null } ?: "Mevcut şifre hatalı veya oturum geçersiz.")
            response.status == HttpStatusCode.NotFound ->
                error(body?.message?.ifBlank { null } ?: "Şifre değişikliği sunucuda henüz hazır değil.")
            else -> error(body?.message?.ifBlank { null } ?: "Şifre güncellenemedi (${response.status}).")
        }
    }
}
