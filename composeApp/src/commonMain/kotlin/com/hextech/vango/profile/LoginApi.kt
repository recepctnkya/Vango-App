package com.hextech.vango.profile

import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LoginPayload(
    /** Sunucu JSON alanı `email`; değer e-posta veya kullanıcı adı olabilir. */
    @SerialName("email")
    val login: String,
    val password: String,
)

@Serializable
data class LoginUserDto(
    val id: Int? = null,
    val full_name: String? = null,
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val company_name: String? = null,
    val profile_avatar_url: String? = null,
    val locale_preference: String? = null,
    /** Sunucu: `light` | `dark` | `system` */
    val theme_preference: String? = null,
    /** Sunucu: `vango` | `green` | `purple` | `orange` | `red` */
    val theme_accent_id: String? = null,
    val mqtt_username: String? = null,
    val mqtt_password: String? = null,
    val mqtt_host: String? = null,
    val mqtt_port: Int? = null,
    val mqtt_client_id: String? = null,
    val updated_at: String? = null,
    val profile_updated_at: String? = null,
    val last_updated: String? = null,
)

@Serializable
private data class LoginResponse(
    val ok: Boolean = false,
    val message: String = "",
    val token: String? = null,
    val expires_at: String? = null,
    val user: LoginUserDto? = null,
    val rate_limited: Boolean? = null,
)

data class LoginSuccess(
    val token: String,
    val expiresAt: String?,
    val user: LoginUserDto?,
)

object LoginApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun login(loginOrEmail: String, password: String): Result<LoginSuccess> = runCatching {
        val payload = LoginPayload(loginOrEmail.trim(), password)
        val bodyJson = json.encodeToString(LoginPayload.serializer(), payload)
        var lastError: String? = null
        for (url in HexnetApi.apiCandidates("/auth/login.php")) {
            val response = try {
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    header("Accept", ContentType.Application.Json.toString())
                    setBody(bodyJson)
                }
            } catch (e: Exception) {
                lastError = e.message?.takeIf { it.isNotBlank() } ?: "Ağ hatası"
                continue
            }
            val raw = response.readTextBodyOrThrow()
            val body = json.decodeFromStringOrNull<LoginResponse>(raw)

            if (response.status == HttpStatusCode.OK && body?.ok == true && !body.token.isNullOrBlank()) {
                return@runCatching LoginSuccess(token = body.token, expiresAt = body.expires_at, user = body.user)
            }
            val fallback = when (response.status) {
                HttpStatusCode.BadRequest -> "E-posta veya kullanıcı adı ve şifre zorunludur"
                HttpStatusCode.Unauthorized -> "Geçersiz kullanıcı bilgileri"
                HttpStatusCode.Forbidden -> body?.message ?: "Kullanıcı aktif değil"
                HttpStatusCode.TooManyRequests -> "Çok fazla deneme. Lütfen daha sonra tekrar deneyin."
                else -> "Login sırasında sunucu hatası"
            }
            lastError = when {
                body != null -> body.message.ifBlank { null } ?: fallback
                raw.isBlank() -> "Sunucu boş yanıt döndü. Lütfen tekrar deneyin."
                else -> fallback
            }
            // Hesap hatası: alternatif URL deneme; ağ/5xx için sıradaki adayı dene
            if (response.status == HttpStatusCode.Unauthorized ||
                response.status == HttpStatusCode.Forbidden ||
                response.status == HttpStatusCode.BadRequest
            ) {
                error(lastError!!)
            }
        }
        error(lastError ?: "Login sırasında sunucu hatası")
    }
}
