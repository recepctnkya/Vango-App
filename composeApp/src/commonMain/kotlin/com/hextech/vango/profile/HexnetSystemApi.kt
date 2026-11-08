package com.hextech.vango.profile

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
private data class MqttTcpProbe(
    val ok: Boolean = false,
    val host: String? = null,
    val port: Int? = null,
    val message: String? = null,
)

@Serializable
private data class ServerProbe(
    val mqtt_tcp: MqttTcpProbe? = null,
)

@Serializable
private data class MqttInfoResponse(
    val ok: Boolean = false,
    val topic_prefix: String? = null,
    val server_probe: ServerProbe? = null,
)

data class HexnetMqttInfo(
    val topicPrefix: String = "hexnet/v1",
    val brokerHost: String = MqttBrokerCredentials.DEFAULT_HOST,
    val brokerPort: Int = MqttBrokerCredentials.DEFAULT_PORT,
)

object HexnetSystemApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    /** Panel `mqttTestLoadInfo` ile aynı: GET /system/mqtt-info.php */
    suspend fun fetchMqttInfo(authToken: String): HexnetMqttInfo? = runCatching {
        val token = authToken.trim()
        if (token.isEmpty()) return@runCatching null

        val response = client.get(HexnetApi.System.mqttInfo) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
        if (response.status != HttpStatusCode.OK) return@runCatching null
        val body = json.decodeFromStringOrNull<MqttInfoResponse>(response.readTextBodyOrThrow())
            ?: return@runCatching null
        if (!body.ok) return@runCatching null

        val probe = body.server_probe?.mqtt_tcp
        HexnetMqttInfo(
            topicPrefix = body.topic_prefix?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: "hexnet/v1",
            brokerHost = probe?.host?.trim()?.takeIf { it.isNotEmpty() } ?: MqttBrokerCredentials.DEFAULT_HOST,
            brokerPort = probe?.port?.takeIf { it in 1..65535 } ?: MqttBrokerCredentials.DEFAULT_PORT,
        )
    }.getOrNull()
}
