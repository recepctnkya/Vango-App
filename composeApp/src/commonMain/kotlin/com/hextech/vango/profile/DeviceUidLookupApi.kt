package com.hextech.vango.profile

import com.hextech.vango.applyDevicePanelProfile
import com.hextech.vango.resolvePanelProfileKey
import com.hextech.vango.RemoteMqttRoute
import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Panel ile aynı mantık: cihaz doğrulama — GET + `mac` / `device_uid` sorgu parametreleri.
 * Oturum varsa `Authorization: Bearer` eklenir (panel ile uyum).
 * Kayıt ekranında token yoksa sunucu "token gerekli" dönebilir; bu durumda UID boş bırakılır (kırmızı hata yok).
 * Tam otomatik UID için sunucunun bu GET’e anonim izin vermesi veya ayrı public uç sağlaması gerekir.
 */
private val DEVICE_UID_LOOKUP_PUBLIC_ENDPOINT get() = HexnetApi.Devices.lookupByMacPublic
private val DEVICE_UID_LOOKUP_ENDPOINT get() = HexnetApi.Devices.lookupRegistration

@Serializable
private data class DeviceUidLookupResponse(
    val ok: Boolean = false,
    val message: String = "",
    val device_uid: String? = null,
    val hardware_version: String? = null,
    val firmware_version: String? = null,
    val mechanical_no: String? = null,
    val company_id: Int? = null,
    val device_id: Int? = null,
)

@Serializable
private data class DeviceUidLookupPayload(
    val mac: String,
)

data class DeviceServerInfo(
    val deviceUid: String? = null,
    val hardware: String? = null,
    val software: String? = null,
    val mechanic: String? = null,
    val mqttCompanyId: Int? = null,
    /** Panel `devices.id` — MQTT topic ve mqtt-command için kullanılır. */
    val mqttDeviceId: Int? = null,
    val mqttTelemetryTopic: String? = null,
    val mqttCommandTopic: String? = null,
    /** Panel `mac_address` (WiFi STA). */
    val macAddress: String? = null,
)

/** myhexnet `devices/list.php` satırı — oturumdaki hesaba görünür cihazlar. */
data class PanelDeviceRow(
    val panelDeviceId: Int,
    val companyId: Int,
    val name: String,
    val macAddress: String?,
    val deviceUid: String?,
    val isActive: Boolean,
    val status: String?,
) {
    fun toServerInfo(): DeviceServerInfo? {
        if (panelDeviceId <= 0 || companyId <= 0) return null
        return DeviceServerInfo(
            deviceUid = deviceUid,
            mqttCompanyId = companyId,
            mqttDeviceId = panelDeviceId,
            macAddress = macAddress,
        )
    }
}

/** Oturumdan bağımsız; çıkışta silinmez. */
fun persistDeviceLookupCache(mac: String, info: DeviceServerInfo) {
    fun good(s: String?): String? =
        s?.trim()?.takeIf { it.isNotEmpty() && it != "-" && it != "\u2014" }
    good(mac)?.let { LoginPreferences.setCachedPairedDeviceMac(it) }
    good(info.deviceUid)?.let { LoginPreferences.setCachedDeviceUid(it) }
    good(info.hardware)?.let { LoginPreferences.setCachedDeviceHardware(it) }
    good(info.software)?.let { LoginPreferences.setCachedDeviceSoftware(it) }
    good(info.mechanic)?.let { LoginPreferences.setCachedDeviceMechanic(it) }
    info.mqttCompanyId?.takeIf { it > 0 }?.let { LoginPreferences.setCachedMqttCompanyId(it) }
    info.mqttDeviceId?.takeIf { it > 0 }?.let { LoginPreferences.setCachedMqttDeviceId(it) }
    val profileKey = resolvePanelProfileKey(null, info.deviceUid, info.hardware)
    LoginPreferences.setCachedPanelProfileKey(profileKey)
    applyDevicePanelProfile(profileKey)
}

fun readCachedDeviceServerInfo(): DeviceServerInfo? {
    fun goodPref(getter: () -> String?): String? =
        getter()?.trim()?.takeIf { it.isNotEmpty() && it != "-" && it != "\u2014" }
    val uid = goodPref { LoginPreferences.getCachedDeviceUid() }
    val hw = goodPref { LoginPreferences.getCachedDeviceHardware() }
    val sw = goodPref { LoginPreferences.getCachedDeviceSoftware() }
    val mech = goodPref { LoginPreferences.getCachedDeviceMechanic() }
    if (uid == null && hw == null && sw == null && mech == null) return null
    return DeviceServerInfo(deviceUid = uid, hardware = hw, software = sw, mechanic = mech)
}

object DeviceUidLookupApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param mac AA:BB:… biçiminde; boş değilse gönderilir.
     * @param deviceUid Opsiyonel; doluysa sorguya eklenir (paneldeki GET ile aynı parametreler).
     * @return UID varsa string; yoksa null.
     */
    suspend fun lookupRegistrationDevice(mac: String, deviceUid: String? = null): Result<String?> =
        runCatching {
            val macTrim = mac.trim()
            val uidTrim = deviceUid?.trim().orEmpty()
            if (macTrim.isEmpty() && uidTrim.isEmpty()) {
                error("MAC veya cihaz UID gerekli.")
            }
            val normalizedMac = if (macTrim.isNotEmpty()) {
                val n = normalizeMacForDisplay(macTrim)
                if (n.length != 17 || !isValidMacFormat(n)) error("Geçersiz MAC adresi.")
                n
            } else {
                ""
            }

            val token = LoginPreferences.getAuthToken()?.trim().orEmpty()

            val publicLookup = client.post(DEVICE_UID_LOOKUP_PUBLIC_ENDPOINT) {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(json.encodeToString(DeviceUidLookupPayload.serializer(), DeviceUidLookupPayload(normalizedMac)))
            }
            val publicRaw = publicLookup.readTextBodyOrThrow()
            val publicBody = json.decodeFromStringOrNull<DeviceUidLookupResponse>(publicRaw)
            when {
                publicLookup.status == HttpStatusCode.OK && publicBody?.ok == true ->
                    return@runCatching publicBody.device_uid?.trim()?.takeIf { it.isNotEmpty() }
                publicLookup.status == HttpStatusCode.OK &&
                    publicBody?.message?.equals("device_not_found", ignoreCase = true) == true ->
                    return@runCatching null
                publicLookup.status == HttpStatusCode.BadRequest -> {
                    val msg = publicBody?.message?.ifBlank { null } ?: "Geçersiz MAC adresi."
                    error(msg)
                }
                publicLookup.status == HttpStatusCode.TooManyRequests ->
                    error("Çok fazla UID sorgusu yapıldı. Lütfen kısa süre sonra tekrar deneyin.")
                // Public uç hazır değilse / yönlendirilirse eski GET uçla geriye dönük dene.
                publicLookup.status != HttpStatusCode.MethodNotAllowed &&
                    publicLookup.status != HttpStatusCode.NotFound &&
                    publicLookup.status != HttpStatusCode.ServiceUnavailable &&
                    publicLookup.status != HttpStatusCode.InternalServerError -> {
                    val msg = publicBody?.message?.ifBlank { null }
                    if (!msg.isNullOrBlank()) error(msg)
                }
            }

            val response = client.get(DEVICE_UID_LOOKUP_ENDPOINT) {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                if (token.isNotEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (normalizedMac.isNotEmpty()) {
                    parameter("mac", normalizedMac)
                }
                if (uidTrim.isNotEmpty()) {
                    parameter("device_uid", uidTrim)
                }
            }
            val raw = response.readTextBodyOrThrow()
            val body = json.decodeFromStringOrNull<DeviceUidLookupResponse>(raw)
            when {
                response.status == HttpStatusCode.OK && body?.ok == true ->
                    body.device_uid?.trim()?.takeIf { it.isNotEmpty() }
                // Sunucu bu uçta Bearer istiyor; kayıt ekranında oturum yoksa ok:false + "token" mesajı gelir — UID yok say
                response.status == HttpStatusCode.Unauthorized ||
                    response.status == HttpStatusCode.Forbidden ->
                    if (token.isEmpty()) null else error(body?.message ?: "Yetkisiz.")
                response.status == HttpStatusCode.OK && body?.ok == false && token.isEmpty() &&
                    body?.message?.contains("token", ignoreCase = true) == true -> null
                response.status == HttpStatusCode.OK -> null
                response.status == HttpStatusCode.NotFound -> null
                response.status == HttpStatusCode.BadRequest -> {
                    val msg = body?.message?.ifBlank { null } ?: "Geçersiz MAC veya UID."
                    error(msg)
                }
                response.status == HttpStatusCode.ServiceUnavailable -> error("Servis şu an kullanılamıyor.")
                else -> {
                    val msg = body?.message?.ifBlank { null }
                        ?: if (raw.isBlank()) {
                            "UID sorgusu için sunucu boş yanıt döndü (${response.status})."
                        } else {
                            "UID sorgulanamadı (${response.status})."
                        }
                    error(msg)
                }
            }
        }

    /** Geriye dönük: yalnızca MAC ile sorgu. */
    suspend fun lookupByMac(mac: String): Result<String?> = lookupRegistrationDevice(mac = mac, deviceUid = null)

    /**
     * Bağlantı sayfası için sunucudan UID + cihaz ek bilgileri çeker.
     * - Önce public MAC→UID ucu ile UID alır.
     * - Sonra (token varsa) admin lookup ile cihaz meta alanlarını dener.
     * Dönüşte alan bulunamazsa null gelir; UI tarafı "-" gösterebilir.
     */
    suspend fun lookupDeviceServerInfoByMac(mac: String): Result<DeviceServerInfo> = runCatching {
        val normalizedMac = normalizeMacForDisplay(mac.trim())
        if (normalizedMac.length != 17 || !isValidMacFormat(normalizedMac)) {
            error("Geçersiz MAC adresi.")
        }

        val publicLookup = client.post(DEVICE_UID_LOOKUP_PUBLIC_ENDPOINT) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(json.encodeToString(DeviceUidLookupPayload.serializer(), DeviceUidLookupPayload(normalizedMac)))
        }
        val publicRaw = publicLookup.readTextBodyOrThrow()
        val publicBody = json.decodeFromStringOrNull<DeviceUidLookupResponse>(publicRaw)
        val uid = publicBody?.device_uid?.trim()?.takeIf { it.isNotEmpty() }
        val publicHardware = publicBody?.hardware_version?.trim()?.takeIf { it.isNotEmpty() }
        val publicSoftware = publicBody?.firmware_version?.trim()?.takeIf { it.isNotEmpty() }
        val publicMechanic = publicBody?.mechanical_no?.trim()?.takeIf { it.isNotEmpty() }

        val publicCompanyId = publicBody?.company_id?.takeIf { it > 0 }
        val publicDeviceId = publicBody?.device_id?.takeIf { it > 0 }

        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            return@runCatching DeviceServerInfo(
                deviceUid = uid,
                hardware = publicHardware,
                software = publicSoftware,
                mechanic = publicMechanic,
                mqttCompanyId = publicCompanyId,
                mqttDeviceId = publicDeviceId,
            )
        }

        val response = client.get(DEVICE_UID_LOOKUP_ENDPOINT) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("mac", normalizedMac)
            uid?.let { parameter("device_uid", it) }
        }
        if (response.status != HttpStatusCode.OK) {
            return@runCatching DeviceServerInfo(
                deviceUid = uid,
                hardware = publicHardware,
                software = publicSoftware,
                mechanic = publicMechanic,
                mqttCompanyId = publicCompanyId,
                mqttDeviceId = publicDeviceId,
            )
        }

        val raw = response.readTextBodyOrThrow()
        val root = json.parseToJsonElement(raw).asObjectOrNull() ?: return@runCatching DeviceServerInfo(deviceUid = uid)
        if (root.jsonFlagFalse("found") || root.jsonFlagFalse("ok")) {
            return@runCatching DeviceServerInfo(
                deviceUid = uid,
                hardware = publicHardware,
                software = publicSoftware,
                mechanic = publicMechanic,
                mqttCompanyId = publicCompanyId,
                mqttDeviceId = publicDeviceId,
            )
        }
        parseLookupRegistrationRoot(root)?.let { parsed ->
            return@runCatching parsed.copy(
                deviceUid = parsed.deviceUid ?: uid,
                hardware = parsed.hardware ?: publicHardware,
                software = parsed.software ?: publicSoftware,
                mechanic = parsed.mechanic ?: publicMechanic,
                mqttCompanyId = parsed.mqttCompanyId ?: publicCompanyId,
                mqttDeviceId = parsed.mqttDeviceId ?: publicDeviceId,
            )
        }
        val device = root["device"]?.asObjectOrNull()

        fun pick(vararg keys: String): String? {
            val source = device ?: root
            for (k in keys) {
                val value = source[k]?.asStringOrNull()?.trim()
                if (!value.isNullOrEmpty()) return value
            }
            return null
        }

        fun pickInt(vararg keys: String): Int? {
            val source = device ?: root
            for (k in keys) {
                val el = source[k] ?: continue
                val n = el.asStringOrNull()?.toIntOrNull()
                    ?: runCatching { el.jsonPrimitive.content.toInt() }.getOrNull()
                if (n != null && n > 0) return n
            }
            return null
        }

        DeviceServerInfo(
            deviceUid = pick("device_uid", "uid") ?: uid,
            hardware = pick("hardware", "hardware_name", "hardware_version", "product_name", "model") ?: publicHardware,
            software = pick("software", "software_name", "software_version", "firmware", "firmware_version") ?: publicSoftware,
            mechanic = pick("mekanik", "mechanic", "mechanical", "mechanic_type", "mechanical_no") ?: publicMechanic,
            mqttCompanyId = pickInt("company_id", "mqtt_company_id") ?: publicCompanyId,
            mqttDeviceId = pickInt("device_id", "mqtt_device_id", "id", "panel_device_id") ?: publicDeviceId,
        )
    }

    /**
     * myhexnet uzaktan oturum: resolve-by-mac → lookup-registration → UID → lookupDeviceServerInfoByMac.
     */
    suspend fun resolvePanelDeviceForRemote(
        mac: String?,
        deviceUid: String? = null,
    ): Result<DeviceServerInfo?> = runCatching {
        val macTrim = mac?.trim()?.takeUnless { it.isEmpty() || it == "-" || it == "\u2014" }
        val uidTrim = deviceUid?.trim()?.takeUnless { it.isEmpty() || it == "-" || it == "\u2014" }
            ?: LoginPreferences.getCachedDeviceUid()?.trim()?.takeUnless { it.isEmpty() }
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        val normalizedMac = macTrim?.let { normalizeMacForDisplay(it) }
            ?.takeIf { it.length == 17 && isValidMacFormat(it) }

        val publicMeta = normalizedMac?.let { fetchPublicMacRegistration(it) }
        if (publicMeta?.mqttCompanyId != null && publicMeta.mqttDeviceId != null) {
            return@runCatching enrichMqttTopics(publicMeta)
        }
        val effectiveUid = uidTrim ?: publicMeta?.deviceUid?.trim()?.takeUnless { it.isEmpty() }
        publicMeta?.deviceUid?.let { LoginPreferences.setCachedDeviceUid(it) }

        if (token.isNotEmpty() && normalizedMac != null) {
            resolveByMacAuthenticated(normalizedMac, token)?.let { return@runCatching it }
            lookupRegistrationPanelDevice(normalizedMac, token, effectiveUid)?.let { return@runCatching it }
        }

        if (token.isNotEmpty() && !effectiveUid.isNullOrBlank()) {
            lookupRegistrationPanelDeviceByUid(effectiveUid, token)?.let { return@runCatching it }
        }

        if (!macTrim.isNullOrBlank()) {
            val info = lookupDeviceServerInfoByMac(macTrim).getOrNull()
            if (info?.mqttCompanyId != null && info.mqttDeviceId != null) {
                return@runCatching enrichMqttTopics(info)
            }
        }

        val cachedMac = LoginPreferences.getCachedPairedDeviceMac()?.trim()?.let { normalizeMacForDisplay(it) }
        val cachedCompany = LoginPreferences.getCachedMqttCompanyId()
        val cachedDevice = LoginPreferences.getCachedMqttDeviceId()
        if (
            normalizedMac != null &&
            cachedMac != null &&
            normalizedMac == cachedMac &&
            cachedCompany != null && cachedCompany > 0 &&
            cachedDevice != null && cachedDevice > 0
        ) {
            return@runCatching enrichMqttTopics(
                DeviceServerInfo(
                    deviceUid = effectiveUid,
                    mqttCompanyId = cachedCompany,
                    mqttDeviceId = cachedDevice,
                ),
            )
        }
        null
    }

    private suspend fun fetchPublicMacRegistration(normalizedMac: String): DeviceServerInfo? {
        val response = client.post(DEVICE_UID_LOOKUP_PUBLIC_ENDPOINT) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(json.encodeToString(DeviceUidLookupPayload.serializer(), DeviceUidLookupPayload(normalizedMac)))
        }
        if (response.status != HttpStatusCode.OK) return null
        val body = json.decodeFromStringOrNull<DeviceUidLookupResponse>(response.readTextBodyOrThrow()) ?: return null
        if (!body.ok) return null
        return DeviceServerInfo(
            deviceUid = body.device_uid?.trim()?.takeIf { it.isNotEmpty() },
            hardware = body.hardware_version?.trim()?.takeIf { it.isNotEmpty() },
            software = body.firmware_version?.trim()?.takeIf { it.isNotEmpty() },
            mechanic = body.mechanical_no?.trim()?.takeIf { it.isNotEmpty() },
            mqttCompanyId = body.company_id?.takeIf { it > 0 },
            mqttDeviceId = body.device_id?.takeIf { it > 0 },
        )
    }

    private suspend fun resolveByMacAuthenticated(mac: String, token: String): DeviceServerInfo? {
        val response = client.get(HexnetApi.Devices.resolveByMac) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("mac", mac)
        }
        val raw = response.readTextBodyOrThrow()
        if (response.status != HttpStatusCode.OK) {
            println("resolve-by-mac HTTP ${response.status} mac=$mac body=${raw.take(240)}")
            return null
        }
        val root = json.parseToJsonElement(raw).asObjectOrNull() ?: return null
        if (root.jsonFlagTrue("success") || root.jsonFlagTrue("ok")) {
            val data = root["data"]?.asObjectOrNull() ?: root["device"]?.asObjectOrNull()
            data?.let { parsePanelDevicePayload(it, root) }?.let { return it }
        }
        if (!root.jsonFlagFalse("success") && !root.jsonFlagFalse("ok")) {
            parsePanelDevicePayload(root, root)?.let { return it }
            root["data"]?.asObjectOrNull()?.let { parsePanelDevicePayload(it, root) }?.let { return it }
        }
        println("resolve-by-mac rejected mac=$mac body=${raw.take(240)}")
        return null
    }

    private suspend fun lookupRegistrationPanelDeviceByUid(uid: String, token: String): DeviceServerInfo? {
        val response = client.get(DEVICE_UID_LOOKUP_ENDPOINT) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("device_uid", uid)
        }
        if (response.status != HttpStatusCode.OK) return null
        val root = json.parseToJsonElement(response.readTextBodyOrThrow()).asObjectOrNull() ?: return null
        return parseLookupRegistrationRoot(root)?.copy(deviceUid = uid)
    }

    /** UID ile kayıtlı cihazın MAC adresini ve meta bilgilerini döner. */
    suspend fun lookupMacAndInfoByDeviceUid(uid: String): Result<Pair<String, DeviceServerInfo?>> =
        runCatching {
            val uidTrim = uid.trim()
            if (uidTrim.isEmpty()) error("UID gerekli.")
            val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
            if (token.isEmpty()) error("UID sorgusu için giriş yapın.")

            val response = client.get(DEVICE_UID_LOOKUP_ENDPOINT) {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("device_uid", uidTrim)
            }
            if (response.status != HttpStatusCode.OK) {
                error("UID sorgulanamadı (${response.status}).")
            }
            val root = json.parseToJsonElement(response.readTextBodyOrThrow()).asObjectOrNull()
                ?: error("Geçersiz sunucu yanıtı.")
            val found = root["found"]?.jsonPrimitive?.content
            if (found == "false") error("Bu UID ile kayıtlı cihaz bulunamadı.")
            val device = root["device"]?.asObjectOrNull() ?: root

            fun pickMac(): String? {
                for (key in listOf("mac", "ble_mac", "mac_address", "wifi_mac", "device_mac")) {
                    val raw = device.pickStringFromObject(key) ?: continue
                    val normalized = normalizeMacForDisplay(raw)
                    if (normalized.length == 17 && isValidMacFormat(normalized)) return normalized
                }
                return null
            }

            val mac = pickMac() ?: error("Bu UID için MAC adresi bulunamadı.")
            val info = DeviceServerInfo(
                deviceUid = device.pickStringFromObject("device_uid", "uid") ?: uidTrim,
                hardware = device.pickStringFromObject("hardware", "product_name", "hardware_version"),
                software = device.pickStringFromObject("firmware_version", "software_version"),
                mechanic = device.pickStringFromObject("mechanical_no", "mechanic"),
                mqttCompanyId = device.pickIntFromObject("company_id"),
                mqttDeviceId = device.pickIntFromObject("id", "device_id"),
            )
            mac to info
        }

    private suspend fun lookupRegistrationPanelDevice(
        mac: String,
        token: String,
        deviceUid: String? = null,
    ): DeviceServerInfo? {
        val response = client.get(DEVICE_UID_LOOKUP_ENDPOINT) {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("mac", mac)
            deviceUid?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("device_uid", it) }
        }
        if (response.status != HttpStatusCode.OK) {
            println("lookup-registration HTTP ${response.status} mac=$mac uid=$deviceUid")
            return null
        }
        val root = json.parseToJsonElement(response.readTextBodyOrThrow()).asObjectOrNull() ?: return null
        return parseLookupRegistrationRoot(root)
    }

    private fun parseLookupRegistrationRoot(root: JsonObject): DeviceServerInfo? {
        if (root.jsonFlagFalse("found") || root.jsonFlagFalse("ok")) return null
        val hasPositive = root.jsonFlagTrue("found") || root.jsonFlagTrue("ok") || root["device"] != null
        if (!hasPositive) return null
        val device = root["device"]?.asObjectOrNull() ?: root
        return parsePanelDevicePayload(device, root)
    }

    private fun parsePanelDevicePayload(source: JsonObject, root: JsonObject): DeviceServerInfo? {
        val companyId = source.pickIntFromObject("company_id", "mqtt_company_id")
            ?: root.pickIntFromObject("company_id", "mqtt_company_id")
            ?: return null
        val deviceId = source.pickIntFromObject("device_id", "id", "panel_device_id", "mqtt_device_id")
            ?: root.pickIntFromObject("device_id", "id", "panel_device_id", "mqtt_device_id")
            ?: return null
        val mqtt = source["mqtt"]?.asObjectOrNull() ?: root["mqtt"]?.asObjectOrNull()
        val telemetry = mqtt?.get("telemetry_topic")?.asStringOrNull()
            ?: source.pickStringFromObject("mqtt_telemetry_topic", "telemetry_topic")
        val command = mqtt?.get("command_topic")?.asStringOrNull()
            ?: source.pickStringFromObject("mqtt_command_topic", "command_topic")
        return enrichMqttTopics(
            DeviceServerInfo(
                deviceUid = source.pickStringFromObject("device_uid", "uid"),
                hardware = source.pickStringFromObject("hardware", "product_name", "hardware_version"),
                software = source.pickStringFromObject("firmware_version", "software_version"),
                mechanic = source.pickStringFromObject("mechanical_no", "mechanic"),
                mqttCompanyId = companyId,
                mqttDeviceId = deviceId,
                mqttTelemetryTopic = telemetry,
                mqttCommandTopic = command,
            ),
        )
    }

    private fun enrichMqttTopics(info: DeviceServerInfo): DeviceServerInfo {
        val companyId = info.mqttCompanyId ?: return info
        val deviceId = info.mqttDeviceId ?: return info
        val route = RemoteMqttRoute(companyId = companyId, deviceId = deviceId)
        return info.copy(
            mqttTelemetryTopic = info.mqttTelemetryTopic?.takeIf { it.isNotBlank() } ?: route.telemetryTopic,
            mqttCommandTopic = info.mqttCommandTopic?.takeIf { it.isNotBlank() } ?: route.commandTopic,
        )
    }

    /**
     * Oturumdaki hesaba panelde kayıtlı cihazları getirir (`GET /devices/list.php`).
     * Yerelde WiFi MAC yokken uzaktan bağlantı için kullanılır.
     */
    suspend fun listAccountDevices(): Result<List<PanelDeviceRow>> {
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) return Result.failure(IllegalStateException("Oturum gerekli"))
        var lastError: String? = null
        for (url in HexnetApi.apiCandidates("/devices/list.php")) {
            val response = try {
                client.get(url) {
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            } catch (e: Exception) {
                lastError = e.message
                continue
            }
            val raw = try {
                response.readTextBodyOrThrow()
            } catch (e: Exception) {
                lastError = e.message
                continue
            }
            if (response.status != HttpStatusCode.OK) {
                lastError = "HTTP ${response.status.value}"
                continue
            }
            val root = json.parseToJsonElement(raw).asObjectOrNull()
            if (root == null) {
                lastError = "Geçersiz yanıt"
                continue
            }
            if (root.jsonFlagFalse("ok")) {
                lastError = root.pickStringFromObject("message") ?: "Liste alınamadı"
                continue
            }
            val arr = root["items"] as? JsonArray ?: return Result.success(emptyList())
            return Result.success(arr.mapNotNull { parsePanelDeviceRow(it.asObjectOrNull()) })
        }
        return Result.failure(IllegalStateException(lastError ?: "Cihaz listesi alınamadı"))
    }

    /**
     * Panel listesinden uzaktan oturum için cihaz seçer.
     * Tercih: preferredMac → preferredDeviceId → tek cihaz → ilk aktif MAC'li.
     */
    suspend fun pickAccountDeviceForRemote(
        preferredMac: String? = null,
        preferredDeviceId: Int? = null,
    ): DeviceServerInfo? {
        val devices = listAccountDevices().getOrNull().orEmpty()
            .filter { it.isActive && it.panelDeviceId > 0 && it.companyId > 0 }
        if (devices.isEmpty()) return null

        val prefMac = preferredMac?.trim()?.takeUnless { it.isEmpty() || it == "-" }
            ?.let { normalizeMacForDisplay(it) }
            ?.takeIf { it.length == 17 && isValidMacFormat(it) }
        val byMac = prefMac?.let { mac ->
            devices.firstOrNull { it.macAddress != null && macsEqualPanel(it.macAddress, mac) }
        }
        val byId = preferredDeviceId?.takeIf { it > 0 }?.let { id ->
            devices.firstOrNull { it.panelDeviceId == id }
        }
        val picked = byMac
            ?: byId
            ?: devices.singleOrNull()
            ?: devices.firstOrNull { !it.macAddress.isNullOrBlank() }
            ?: devices.firstOrNull()
            ?: return null

        val info = picked.toServerInfo()?.let { enrichMqttTopics(it) } ?: return null
        picked.macAddress?.let { mac ->
            val n = normalizeMacForDisplay(mac)
            if (n.length == 17 && isValidMacFormat(n)) {
                persistDeviceLookupCache(n, info)
            } else {
                info.mqttCompanyId?.let { LoginPreferences.setCachedMqttCompanyId(it) }
                info.mqttDeviceId?.let { LoginPreferences.setCachedMqttDeviceId(it) }
                info.deviceUid?.let { LoginPreferences.setCachedDeviceUid(it) }
            }
        } ?: run {
            info.mqttCompanyId?.let { LoginPreferences.setCachedMqttCompanyId(it) }
            info.mqttDeviceId?.let { LoginPreferences.setCachedMqttDeviceId(it) }
            info.deviceUid?.let { LoginPreferences.setCachedDeviceUid(it) }
        }
        return info
    }

    private fun parsePanelDeviceRow(obj: JsonObject?): PanelDeviceRow? {
        if (obj == null) return null
        val id = obj.pickIntFromObject("id", "device_id") ?: return null
        val companyId = obj.pickIntFromObject("company_id") ?: return null
        val name = obj.pickStringFromObject("device_name", "name", "product_name") ?: "Cihaz $id"
        val macRaw = obj.pickStringFromObject("mac_address", "mac", "wifi_mac")
        val mac = macRaw?.let { normalizeMacForDisplay(it) }
            ?.takeIf { it.length == 17 && isValidMacFormat(it) }
        val uid = obj.pickStringFromObject("device_uid", "uid")
        val active = when (val a = obj["is_active"]) {
            is JsonPrimitive -> a.content != "0" && !a.content.equals("false", true)
            else -> true
        }
        val status = obj.pickStringFromObject("status")
        return PanelDeviceRow(
            panelDeviceId = id,
            companyId = companyId,
            name = name,
            macAddress = mac,
            deviceUid = uid,
            isActive = active,
            status = status,
        )
    }
}

private fun macsEqualPanel(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    return normalizeMacForDisplay(a).equals(normalizeMacForDisplay(b), ignoreCase = true)
}

private fun JsonObject.jsonFlagTrue(key: String): Boolean = this[key].jsonFlagTrue()
private fun JsonObject.jsonFlagFalse(key: String): Boolean = this[key].jsonFlagFalse()

private fun JsonElement?.jsonFlagTrue(): Boolean {
    val primitive = this as? JsonPrimitive ?: return false
    return primitive.content.equals("true", ignoreCase = true) || primitive.content == "1"
}

private fun JsonElement?.jsonFlagFalse(): Boolean {
    val primitive = this as? JsonPrimitive ?: return false
    return primitive.content.equals("false", ignoreCase = true) || primitive.content == "0"
}

private fun JsonObject.pickStringFromObject(vararg keys: String): String? {
    for (k in keys) {
        val v = this[k]?.asStringOrNull()?.trim()
        if (!v.isNullOrEmpty()) return v
    }
    return null
}

private fun JsonObject.pickIntFromObject(vararg keys: String): Int? {
    for (k in keys) {
        val el = this[k] ?: continue
        val n = el.asStringOrNull()?.toIntOrNull()
            ?: runCatching { el.jsonPrimitive.content.toInt() }.getOrNull()
        if (n != null && n > 0) return n
    }
    return null
}

private fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()
private fun JsonElement.asStringOrNull(): String? = runCatching { jsonPrimitive.content }.getOrNull()
