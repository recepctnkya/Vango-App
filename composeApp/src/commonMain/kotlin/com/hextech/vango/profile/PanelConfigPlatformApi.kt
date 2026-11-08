package com.hextech.vango.profile

import com.hextech.vango.PanelConfigMapper
import com.hextech.vango.PanelPlatformConfigRoot
import com.hextech.vango.VangoHttpClient
import com.hextech.vango.defaultPanelPlatformConfig
import com.hextech.vango.resolvePanelProfileKey
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class PanelConfigLoadResult(
    val profileKey: String,
    val config: PanelPlatformConfigRoot,
    val device: PanelDeviceMeta? = null,
)

@Serializable
data class PanelDeviceMeta(
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("device_uid") val deviceUid: String? = null,
    @SerialName("hardware_version") val hardwareVersion: String? = null,
    @SerialName("variant_name") val variantName: String? = null,
    @SerialName("product_name") val productName: String? = null,
)

/** myhexnet panel-config-get.php / panel-config-save.php */
object PanelConfigPlatformApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(authToken: String, panelDeviceId: Int): PanelConfigLoadResult? {
        val token = authToken.trim()
        if (token.isEmpty() || panelDeviceId <= 0) return null
        return runCatching {
            val response = client.get(HexnetApi.Devices.panelConfigGet) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                parameter("id", panelDeviceId)
            }
            val raw = response.readTextBodyOrThrow()
            if (response.status != HttpStatusCode.OK) {
                println("panel-config-get HTTP ${response.status}: ${raw.take(200)}")
                return@runCatching null
            }
            val root = json.parseToJsonElement(raw).jsonObject
            if (!root.responseOk()) {
                println("panel-config-get failed: ${root.stringField("message")}")
                return@runCatching null
            }
            val body = json.decodeFromJsonElement(PanelConfigGetResponse.serializer(), root)
            val device = body.device
            val profileKey = resolvePanelProfileKey(
                platformProfile = body.profile ?: body.config?.profile,
                uid = device?.deviceUid,
                hardware = device?.hardwareVersion ?: device?.productName,
                variant = device?.variantName,
            )
            val normalized = PanelConfigMapper.normalizePlatformConfig(
                profileKey,
                body.config ?: defaultPanelPlatformConfig(profileKey),
            )
            PanelConfigLoadResult(
                profileKey = profileKey,
                config = normalized.copy(profile = profileKey),
                device = device,
            )
        }.getOrNull()
    }

    suspend fun save(
        authToken: String,
        panelDeviceId: Int,
        config: PanelPlatformConfigRoot,
    ): PanelPlatformConfigRoot? {
        val token = authToken.trim()
        if (token.isEmpty() || panelDeviceId <= 0) return null
        return runCatching {
            val payload = PanelConfigSaveRequest(deviceId = panelDeviceId, config = config)
            val response = client.post(HexnetApi.Devices.panelConfigSave) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(json.encodeToString(PanelConfigSaveRequest.serializer(), payload))
            }
            val raw = response.readTextBodyOrThrow()
            val root = json.parseToJsonElement(raw).jsonObject
            if (response.status != HttpStatusCode.OK || !root.responseOk()) {
                println("panel-config-save failed: ${root.stringField("message")} raw=${raw.take(200)}")
                return@runCatching null
            }
            val body = json.decodeFromJsonElement(PanelConfigSaveResponse.serializer(), root)
            val saved = body.config ?: config
            PanelConfigMapper.normalizePlatformConfig(saved.profile, saved)
        }.getOrNull()
    }

    fun defaultForDevice(uid: String?, hardware: String? = null): PanelConfigLoadResult {
        val profileKey = resolvePanelProfileKey(null, uid, hardware, null)
        return PanelConfigLoadResult(
            profileKey = profileKey,
            config = defaultPanelPlatformConfig(profileKey),
        )
    }
}

@Serializable
private data class PanelConfigGetResponse(
    val ok: Boolean = false,
    val message: String = "",
    val profile: String? = null,
    val device: PanelDeviceMeta? = null,
    val config: PanelPlatformConfigRoot? = null,
)

@Serializable
private data class PanelConfigSaveRequest(
    @SerialName("device_id") val deviceId: Int,
    val config: PanelPlatformConfigRoot,
)

@Serializable
private data class PanelConfigSaveResponse(
    val ok: Boolean = false,
    val message: String = "",
    val config: PanelPlatformConfigRoot? = null,
)

private fun JsonObject.responseOk(): Boolean =
    this["ok"].jsonFlagTrue() || this["success"].jsonFlagTrue()

private fun JsonObject.stringField(key: String): String =
    this[key]?.jsonPrimitive?.content?.trim().orEmpty()

private fun JsonElement?.jsonFlagTrue(): Boolean {
    val primitive = this as? JsonPrimitive ?: return false
    return primitive.content.equals("true", ignoreCase = true) || primitive.content == "1"
}
