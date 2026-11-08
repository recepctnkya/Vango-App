package com.hextech.vango

import com.hextech.vango.profile.DeviceUidLookupApi
import com.hextech.vango.profile.HexnetApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.PanelConfigPlatformApi
import com.hextech.vango.profile.readTextBodyOrThrow
import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Panel ayarlari senkronu:
 * - Platform hesabi: `panel-config-get/save.php` + `vango_medium|mini` profili
 * - Cihaz firmware: `io_profile` telemetri + `io_config_set` komutu
 */
object PanelSettingsPlatformSync {
    private val json = Json { ignoreUnknownKeys = true }
    private val client get() = VangoHttpClient.instance

    fun hasPlatformPanelConfigForDevice(): Boolean = readCachedPanelConfig() != null

    fun shouldPreservePlatformPanelLayout(): Boolean =
        uiState.value.settingsPage == SettingsPage.Panel || hasPlatformPanelConfigForDevice()

    fun persistProfile(profile: IoProfileSnapshot) {
        val deviceId = resolvedPanelDeviceId() ?: return
        LoginPreferences.setCachedIoProfileDeviceId(deviceId)
        LoginPreferences.setCachedIoProfileJson(json.encodeToString(profile))
    }

    fun persistPanelConfig(config: PanelPlatformConfigRoot) {
        // Always persist JSON so panel edits survive without login/device id.
        resolvedPanelDeviceId()?.takeIf { it > 0 }?.let { deviceId ->
            LoginPreferences.setCachedPanelConfigDeviceId(deviceId)
        }
        LoginPreferences.setCachedPanelConfigJson(json.encodeToString(config))
        LoginPreferences.setCachedPanelProfileKey(config.profile)
        applyDevicePanelProfile(config.profile)
    }

    fun readCachedPanelConfig(): PanelPlatformConfigRoot? {
        val raw = LoginPreferences.getCachedPanelConfigJson()?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val deviceId = resolvedPanelDeviceId()?.takeIf { it > 0 }
        val cachedId = LoginPreferences.getCachedPanelConfigDeviceId()
        if (deviceId != null && cachedId != null && cachedId != deviceId) return null
        return runCatching { json.decodeFromString<PanelPlatformConfigRoot>(raw) }.getOrNull()
    }

    fun readCachedProfile(): IoProfileSnapshot? {
        val deviceId = resolvedPanelDeviceId() ?: return null
        if (LoginPreferences.getCachedIoProfileDeviceId() != deviceId) return null
        val raw = LoginPreferences.getCachedIoProfileJson()?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return runCatching { json.decodeFromString<IoProfileSnapshot>(raw) }.getOrNull()
    }

    fun bootstrapCachedProfileKind() {
        LoginPreferences.getCachedPanelProfileKey()?.let { applyDevicePanelProfile(it) }
            ?: applyDevicePanelProfile(
                resolvePanelProfileKey(
                    platformProfile = null,
                    uid = LoginPreferences.getCachedDeviceUid(),
                    hardware = LoginPreferences.getCachedDeviceHardware(),
                ),
            )
    }

    fun applyProfile(profile: IoProfileSnapshot, source: String = "sync") {
        if (shouldPreservePlatformPanelLayout()) {
            persistProfile(profile)
            applyIoChannelLabelsFromDevice(profile)
            println("Panel io_profile cached from $source (platform layout preserved; labels synced)")
            return
        }
        applyRemoteIoProfileToUi(profile)
        syncVanGoRuntimeFromDevConfig(uiState.value.devConfig)
        persistProfile(profile)
        println("Panel io_profile applied from $source relays=${profile.relays} mask=${profile.relayMask}")
    }

    fun applyPlatformPanelConfig(config: PanelPlatformConfigRoot, source: String = "platform") {
        applyDevicePanelProfile(config.profile)
        val caps = devicePanelProfile.value.caps
        val merged = PanelConfigMapper.applyPlatformConfigToDevConfig(config, uiState.value.devConfig)
        uiState.value = uiState.value.copy(devConfig = merged)
        syncVanGoRuntimeFromDevConfig(merged)
        persistPanelConfig(config)
        resetPanelPlatformChannels(config.channels)
        println(
            "Panel platform config applied from $source profile=${config.profile} " +
                "relays=${caps.relays} dimmers=${caps.dimmers}",
        )
    }

    suspend fun ensurePanelDeviceIdForSync(): Int? {
        resolvedPanelDeviceId()?.takeIf { it > 0 }?.let { return it }
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) return null
        val mac = LoginPreferences.getCachedPairedDeviceMac()?.trim()
            ?: vanGoData.value.mac?.takeIf { it.isNotBlank() }
            ?: vanGoData.value.wifiMac?.takeIf { it.isNotBlank() }
        val uid = LoginPreferences.getCachedDeviceUid()
        val info = DeviceUidLookupApi.resolvePanelDeviceForRemote(mac, uid).getOrNull() ?: return null
        info.mqttDeviceId?.takeIf { it > 0 }?.let { LoginPreferences.setCachedMqttDeviceId(it) }
        info.mqttCompanyId?.takeIf { it > 0 }?.let { LoginPreferences.setCachedMqttCompanyId(it) }
        info.deviceUid?.let { LoginPreferences.setCachedDeviceUid(it) }
        return resolvedPanelDeviceId()
    }

    suspend fun refreshFromPlatform(panelDeviceId: Int? = resolvedPanelDeviceId()): Boolean {
        bootstrapCachedProfileKind()
        val deviceId = panelDeviceId?.takeIf { it > 0 }
            ?: ensurePanelDeviceIdForSync()
            ?: return applyCachedOnly()

        readCachedPanelConfig()?.let { applyPlatformPanelConfig(it, "cache") }
        readCachedProfile()?.let { applyProfile(it, "cache") }

        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) return readCachedPanelConfig() != null || readCachedProfile() != null

        var applied = false
        var platformLoaded = false
        PanelConfigPlatformApi.load(token, deviceId)?.let { loaded ->
            applyDevicePanelProfile(loaded.profileKey)
            applyPlatformPanelConfig(loaded.config, "platform")
            platformLoaded = true
            applied = true
        } ?: run {
            val fallback = PanelConfigPlatformApi.defaultForDevice(
                uid = LoginPreferences.getCachedDeviceUid(),
                hardware = LoginPreferences.getCachedDeviceHardware(),
            )
            applyDevicePanelProfile(fallback.profileKey)
            println("Panel platform config load failed for device_id=$deviceId")
        }
        fetchLatestTelemetry(token, deviceId)?.let { telemetry ->
            applyRemoteTelemetryToUiState(telemetry, mergeIoProfileLayout = !platformLoaded)
            telemetry.ioProfile?.let { profile ->
                persistProfile(profile)
                if (!platformLoaded) {
                    applyProfile(profile, "telemetry")
                } else {
                    /* Platform layout korunur; IO isim/ikon SoT yine uygulanır */
                    applyIoChannelLabelsFromDevice(profile)
                }
                applied = true
            }
        }
        return applied || readCachedPanelConfig() != null
    }

    suspend fun bootstrapFromPlatform(panelDeviceId: Int): Boolean =
        refreshFromPlatform(panelDeviceId)

    suspend fun pushPanelConfigToPlatform(devConfig: DevConfig): Boolean {
        val deviceId = resolvedPanelDeviceId() ?: ensurePanelDeviceIdForSync() ?: run {
            println("Panel platform save skipped: device_id unresolved")
            return false
        }
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            println("Panel platform save skipped: auth token missing")
            return false
        }
        val profileKey = devicePanelProfile.value.profileKey
        val existing = readCachedPanelConfig()
            ?: PanelConfigPlatformApi.load(token, deviceId)?.config
        val payload = PanelConfigMapper.devConfigToPlatformConfig(
            devConfig,
            profileKey,
            existing ?: defaultPanelPlatformConfig(profileKey),
            channelsOverlay = panelPlatformChannels.value,
        )
        val saved = PanelConfigPlatformApi.save(token, deviceId, payload) ?: run {
            println("Panel platform save failed device_id=$deviceId")
            return false
        }
        persistPanelConfig(saved)
        resetPanelPlatformChannels(saved.channels)
        println("Panel config saved to platform device_id=$deviceId profile=${saved.profile}")
        return true
    }

    private fun applyCachedOnly(): Boolean {
        bootstrapCachedProfileKind()
        var ok = false
        readCachedPanelConfig()?.let {
            applyPlatformPanelConfig(it, "cache")
            ok = true
        }
        readCachedProfile()?.let {
            applyProfile(it, "cache")
            ok = true
        }
        return ok
    }

    private suspend fun fetchLatestTelemetryRaw(authToken: String, panelDeviceId: Int): String? = runCatching {
        val response = client.get(HexnetApi.Devices.latestTelemetry) {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            parameter("id", panelDeviceId)
            parameter("device_id", panelDeviceId)
        }
        if (response.status != HttpStatusCode.OK) {
            println("Panel latest-telemetry HTTP ${response.status} device_id=$panelDeviceId")
            return@runCatching null
        }
        response.readTextBodyOrThrow()
    }.getOrNull()

    private suspend fun fetchLatestTelemetry(authToken: String, panelDeviceId: Int): RemoteTelemetry? =
        fetchLatestTelemetryRaw(authToken, panelDeviceId)?.let { RemoteTelemetryParser.parse(it) }

    /** BLE paketinde env yoksa platform telemetriden sicaklik/nem doldurur. */
    suspend fun refreshEnvSensorsFromPlatformWhileBle(): Boolean {
        if (!isBleGattLinkReady()) return false
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            println("BLE env refresh skipped: auth token missing")
            return false
        }
        val deviceId = ensurePanelDeviceIdForSync() ?: run {
            println("BLE env refresh skipped: device_id unresolved")
            return false
        }
        val raw = fetchLatestTelemetryRaw(token, deviceId) ?: return false
        BleEnvSensorParser.parseFromJson(raw)?.let { env ->
            applyBleEnvReading(env)
            println("BLE env refreshed from platform temp=${env.tempC} hum=${env.humidityPct}")
            return true
        }
        val telemetry = RemoteTelemetryParser.parse(raw) ?: return false
        val temp = telemetry.sensors.temperatureC.takeIf { it > 0 }
        val hum = telemetry.sensors.humidityPct.takeIf { it > 0 }
        if (temp == null && hum == null) {
            println("BLE env refresh: no temp/hum in telemetry device_id=$deviceId")
            return false
        }
        applyBleEnvReading(BleEnvSensors(tempC = temp, humidityPct = hum))
        return true
    }
}
