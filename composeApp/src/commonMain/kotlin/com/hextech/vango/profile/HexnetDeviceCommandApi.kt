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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Panel uzaktan komut: once canli UI ile ayni `command.php?id=`, sonra `mqtt-command.php`.
 */
@Serializable
private data class MqttCommandResponse(
    val ok: Boolean = false,
    val message: String = "",
)

@Serializable
private data class MqttCommandRequest(
    val device_id: Int,
    val cmd: String,
    val args: JsonObject,
)

@Serializable
private data class PanelCommandRequest(
    val cmd: String,
    val args: JsonObject,
)

object HexnetDeviceCommandApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun send(
        authToken: String,
        panelDeviceId: Int,
        cmd: String,
        args: JsonObject,
    ): Boolean {
        val token = authToken.trim()
        if (token.isEmpty() || panelDeviceId <= 0) return false
        if (sendViaCommandPhp(token, panelDeviceId, cmd, args)) return true
        return sendViaMqttCommandPhp(token, panelDeviceId, cmd, args)
    }

    private suspend fun sendViaCommandPhp(
        token: String,
        panelDeviceId: Int,
        cmd: String,
        args: JsonObject,
    ): Boolean = runCatching {
        val payload = PanelCommandRequest(cmd = cmd, args = args)
        val response = client.post(HexnetApi.Devices.command(panelDeviceId)) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(json.encodeToString(PanelCommandRequest.serializer(), payload))
        }
        val raw = response.readTextBodyOrThrow()
        val body = json.decodeFromStringOrNull<MqttCommandResponse>(raw)
        val ok = response.status == HttpStatusCode.OK && body?.ok == true
        if (!ok) {
            println(
                "command.php failed device_id=$panelDeviceId cmd=$cmd " +
                    "status=${response.status} body=${raw.take(240)}",
            )
        } else {
            println("command.php ok device_id=$panelDeviceId cmd=$cmd")
        }
        ok
    }.getOrElse { e ->
        println("command.php error device_id=$panelDeviceId cmd=$cmd: ${e.message}")
        false
    }

    private suspend fun sendViaMqttCommandPhp(
        token: String,
        panelDeviceId: Int,
        cmd: String,
        args: JsonObject,
    ): Boolean = runCatching {
        val payload = MqttCommandRequest(device_id = panelDeviceId, cmd = cmd, args = args)
        val response = client.post(HexnetApi.Devices.mqttCommand) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(json.encodeToString(MqttCommandRequest.serializer(), payload))
        }
        val raw = response.readTextBodyOrThrow()
        val body = json.decodeFromStringOrNull<MqttCommandResponse>(raw)
        val ok = response.status == HttpStatusCode.OK && body?.ok == true
        if (!ok) {
            println(
                "mqtt-command.php failed device_id=$panelDeviceId cmd=$cmd " +
                    "status=${response.status} body=${raw.take(240)}",
            )
        } else {
            println("mqtt-command.php ok device_id=$panelDeviceId cmd=$cmd")
        }
        ok
    }.getOrElse { e ->
        println("mqtt-command.php error device_id=$panelDeviceId cmd=$cmd: ${e.message}")
        false
    }
}
