package com.hextech.vango.profile

import com.hextech.vango.getCurrentTimeInSeconds
import com.hextech.vango.mqtt.MqttBrokerCredentials
import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class MqttCredentialsProvisionBody(
    val device_id: Int,
    val rotate: Boolean = false,
)

/**
 * myhexnet panel: GET/POST `/devices/mqtt-credentials.php?device_id=`
 *
 * GET yalnızca kullanıcı adını döner (şifre yok).
 * Şifre yalnızca POST ile yeni oluşturma veya rotate=1 sonrası gelir.
 */
object MqttCredentialsApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(authToken: String, panelDeviceId: Int): MqttBrokerCredentials? = runCatching {
        val token = authToken.trim()
        if (token.isEmpty() || panelDeviceId <= 0) return@runCatching null

        var response = client.get(HexnetApi.Devices.mqttCredentials) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            parameter("device_id", panelDeviceId)
        }
        var raw = response.readTextBodyOrThrow()
        var root = json.parseToJsonElement(raw).jsonObject
        var creds = if (response.status == HttpStatusCode.OK) {
            parseCredentialsRoot(root, panelDeviceId)
        } else {
            null
        }

        // Şifre yoksa: yoksa oluştur (rotate=false), varsa rotate=true ile yeni şifre al.
        if (creds == null || creds.password.isNullOrBlank()) {
            val alreadyConfigured = root.booleanOrNull("mqtt_configured") == true ||
                !creds?.username.isNullOrBlank()
            val provision = client.post(HexnetApi.Devices.mqttCredentials) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    json.encodeToString(
                        MqttCredentialsProvisionBody.serializer(),
                        MqttCredentialsProvisionBody(
                            device_id = panelDeviceId,
                            rotate = alreadyConfigured,
                        ),
                    ),
                )
            }
            raw = provision.readTextBodyOrThrow()
            root = json.parseToJsonElement(raw).jsonObject
            response = provision
            if (response.status != HttpStatusCode.OK) return@runCatching null
            creds = parseCredentialsRoot(root, panelDeviceId)
        }

        creds?.takeIf { !it.username.isNullOrBlank() && !it.password.isNullOrBlank() }
    }.getOrNull()

    private fun JsonObject.isApiOk(): Boolean {
        val el = this["ok"] ?: return false
        val c = el.jsonPrimitive.content
        return c == "true" || c == "1"
    }

    private fun JsonObject.booleanOrNull(key: String): Boolean? {
        val el = this[key] ?: return null
        return when (el.jsonPrimitive.content.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
    }

    private fun parseCredentialsRoot(root: JsonObject, panelDeviceId: Int): MqttBrokerCredentials? {
        if (!root.isApiOk()) return null
        val data = root["data"]?.jsonObject ?: root

        fun pickString(vararg keys: String): String? {
            for (k in keys) {
                val v = data[k]?.jsonPrimitive?.content?.trim()
                if (!v.isNullOrEmpty()) return v
            }
            return null
        }

        fun pickInt(vararg keys: String): Int? {
            for (k in keys) {
                val v = data[k]?.jsonPrimitive?.content?.toIntOrNull()
                if (v != null && v > 0) return v
            }
            return null
        }

        val user = pickString("username", "mqtt_username", "user")
        val pass = pickString("password", "mqtt_password", "secret")
        val host = pickString("host", "broker_host", "mqtt_host") ?: MqttBrokerCredentials.DEFAULT_HOST
        val port = pickInt("port", "broker_port", "mqtt_port") ?: MqttBrokerCredentials.DEFAULT_PORT
        val clientId = pickString("client_id", "mqtt_client_id")
            ?: "hexnet-vango-$panelDeviceId-${getCurrentTimeInSeconds()}"

        return MqttBrokerCredentials(
            host = host,
            port = port,
            username = user,
            password = pass,
            clientId = clientId,
        )
    }
}
