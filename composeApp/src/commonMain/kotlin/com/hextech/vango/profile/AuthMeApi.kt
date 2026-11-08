package com.hextech.vango.profile

import com.hextech.vango.getCurrentTimeInSeconds
import com.hextech.vango.mqtt.MqttBrokerCredentials
import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AuthMeResponse(
    val ok: Boolean = false,
    val message: String = "",
    val user: LoginUserDto? = null,
    val expires_at: String? = null,
)

object AuthMeApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun fetchMe(authToken: String): AuthMeResponse? {
        val token = authToken.trim()
        if (token.isEmpty()) return null

        val response = client.get(HexnetApi.Auth.me) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
        if (response.status != HttpStatusCode.OK) return null
        return json.decodeFromStringOrNull<AuthMeResponse>(response.readTextBodyOrThrow())
            ?.takeIf { it.ok }
    }

    /** Mevcut oturumdaki kullanıcı kaydı; ağ/oturum hatasında null. */
    suspend fun fetchCurrentUser(authToken: String): LoginUserDto? = fetchMe(authToken)?.user
    suspend fun fetchThemePreference(authToken: String): Result<String?> = runCatching {
        fetchMe(authToken)?.user?.theme_preference
    }

    suspend fun fetchThemeAccentId(authToken: String): Result<String?> = runCatching {
        fetchMe(authToken)?.user?.theme_accent_id
    }

    suspend fun fetchLocalePreference(authToken: String): Result<String?> = runCatching {
        fetchMe(authToken)?.user?.locale_preference
    }

    /** auth/me yanıtındaki mqtt_* alanlarından broker kimlik bilgisi üretir. */
    suspend fun fetchMqttCredentials(authToken: String): MqttBrokerCredentials? = runCatching {
        fetchMe(authToken)?.user?.toMqttBrokerCredentials()
    }.getOrNull()
}

fun LoginUserDto.toMqttBrokerCredentials(): MqttBrokerCredentials? {
    val user = mqtt_username?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val pass = mqtt_password?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val host = mqtt_host?.trim()?.takeIf { it.isNotEmpty() } ?: MqttBrokerCredentials.DEFAULT_HOST
    val port = mqtt_port?.takeIf { it in 1..65535 } ?: MqttBrokerCredentials.DEFAULT_PORT
    val clientId = mqtt_client_id?.trim()?.takeIf { it.isNotEmpty() }
        ?: "hexnet-vango-${getCurrentTimeInSeconds()}"
    return MqttBrokerCredentials(host = host, port = port, username = user, password = pass, clientId = clientId)
}
