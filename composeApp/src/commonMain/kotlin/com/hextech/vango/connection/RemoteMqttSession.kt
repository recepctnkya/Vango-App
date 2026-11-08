package com.hextech.vango

import com.hextech.vango.mqtt.MqttTelemetryManager
import com.hextech.vango.mqtt.mqttTelemetryManagerRef
import com.hextech.vango.profile.DeviceUidLookupApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import com.hextech.vango.profile.persistDeviceLookupCache

data class RemoteSessionContext(
    val route: RemoteMqttRoute,
    val panelDevice: HexnetPanelDevice?,
)

object RemoteMqttSession {

    /**
     * myhexnet panel akışı: MAC → resolve-by-mac / lookup-registration → topic + panel device_id.
     * Başarısızlıkta null döner; [remoteSessionError] doldurulur.
     */
    suspend fun prepareSession(mac: String?, deviceUid: String? = null): RemoteSessionContext? {
        val macTrim = mac?.trim()?.takeUnless { it.isEmpty() || it == "-" || it == "\u2014" }
        val normalizedMac = macTrim?.let { normalizeMacForDisplay(it) }
            ?.takeIf { it.length == 17 && isValidMacFormat(it) }

        readCachedRemoteMqttRoute()?.let { cached ->
            if (normalizedMac == null || cachedMacMatches(normalizedMac)) {
                val panelId = LoginPreferences.getCachedMqttDeviceId()
                val companyId = LoginPreferences.getCachedMqttCompanyId()
                if (panelId != null && companyId != null) {
                    val panel = HexnetPanelDevice(
                        panelDeviceId = panelId,
                        companyId = companyId,
                        telemetryTopic = cached.telemetryTopic,
                        commandTopic = cached.commandTopic,
                    )
                    hexnetPanelDevice.value = panel
                    remoteMqttRoute.value = cached
                    return RemoteSessionContext(cached, panel)
                }
            }
        }

        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            remoteSessionError.value = "Uzaktan kontrol için myhexnet hesabına giriş yapın."
            return null
        }

        // Yerelde MAC yoksa panel cihaz listesinden (hesaba kayıtlı) çözümle
        var effectiveMac = normalizedMac
        var panelInfo = if (effectiveMac != null) {
            DeviceUidLookupApi.resolvePanelDeviceForRemote(effectiveMac, deviceUid).getOrNull()
        } else {
            null
        }
        if (panelInfo?.mqttCompanyId == null || panelInfo.mqttDeviceId == null) {
            val fromAccount = DeviceUidLookupApi.pickAccountDeviceForRemote(
                preferredMac = effectiveMac ?: LoginPreferences.getCachedPairedDeviceMac(),
                preferredDeviceId = LoginPreferences.getCachedMqttDeviceId(),
            )
            if (fromAccount?.mqttCompanyId != null && fromAccount.mqttDeviceId != null) {
                panelInfo = fromAccount
                effectiveMac = fromAccount.macAddress?.let { normalizeMacForDisplay(it) }
                    ?.takeIf { it.length == 17 && isValidMacFormat(it) }
                    ?: effectiveMac
            }
        }
        if (panelInfo?.mqttCompanyId == null || panelInfo.mqttDeviceId == null) {
            remoteSessionError.value = if (effectiveMac == null) {
                "Hesabınızda panelde kayıtlı cihaz bulunamadı. myhexnet panelinden cihaz ekleyin veya BLE ile bir kez bağlanın."
            } else {
                "Cihaz myhexnet panelinde bulunamadı. Giriş yaptığınız hesabın bu cihaza erişimi olmalı; MAC ve UID panel kaydıyla eşleşmelidir."
            }
            return null
        }

        if (effectiveMac != null) {
            persistDeviceLookupCache(effectiveMac, panelInfo)
        } else {
            panelInfo.mqttCompanyId?.let { LoginPreferences.setCachedMqttCompanyId(it) }
            panelInfo.mqttDeviceId?.let { LoginPreferences.setCachedMqttDeviceId(it) }
            panelInfo.deviceUid?.let { LoginPreferences.setCachedDeviceUid(it) }
        }

        val route = RemoteMqttRoute(
            companyId = panelInfo.mqttCompanyId,
            deviceId = panelInfo.mqttDeviceId,
        )
        val telemetryTopic = panelInfo.mqttTelemetryTopic?.trim()?.takeIf { it.isNotEmpty() }
            ?: route.telemetryTopic
        val commandTopic = panelInfo.mqttCommandTopic?.trim()?.takeIf { it.isNotEmpty() }
            ?: route.commandTopic

        val panel = HexnetPanelDevice(
            panelDeviceId = panelInfo.mqttDeviceId,
            companyId = panelInfo.mqttCompanyId,
            telemetryTopic = telemetryTopic,
            commandTopic = commandTopic,
        )
        persistRemoteMqttRoute(route)
        hexnetPanelDevice.value = panel
        remoteSessionError.value = null
        return RemoteSessionContext(
            route = RemoteMqttRoute.fromTopics(telemetryTopic, commandTopic) ?: route,
            panelDevice = panel,
        )
    }

    /** Uzaktan mod açıkken MQTT abonelik + broker bağlantısını başlatır. */
    suspend fun startRemotePipeline(
        manager: MqttTelemetryManager,
        mac: String? = currentPlatformDeviceMac(),
    ): Boolean {
        remoteSessionError.value = null
        appConnectionState.value = AppConnectionState.OFFLINE
        val session = prepareSession(mac) ?: return false
        val panel = session.panelDevice ?: run {
            remoteSessionError.value =
                "Panel cihaz kimliği (device_id) bulunamadı. Önce cihazı myhexnet paneline kaydedin."
            return false
        }
        PanelSettingsPlatformSync.bootstrapFromPlatform(panel.panelDeviceId)
        connectMqtt(manager, session, panel.panelDeviceId)
        return true
    }

    fun connectMqtt(
        manager: MqttTelemetryManager,
        session: RemoteSessionContext,
        panelDeviceId: Int? = session.panelDevice?.panelDeviceId,
    ) {
        applyResolvedRoute(session.route)
        val topic = session.panelDevice?.telemetryTopic ?: session.route.telemetryTopic
        manager.subscribeTelemetry(topic)
        manager.connect(panelDeviceId)
        println("Remote MQTT topics: $topic (panel device_id=$panelDeviceId)")
    }

    fun applyResolvedRoute(route: RemoteMqttRoute) {
        persistRemoteMqttRoute(route)
        remoteSessionError.value = null
    }

    fun onTelemetryRouteMaybeChanged(changed: Boolean) {
        if (!changed || !remoteModeEnabled.value) return
        val route = remoteMqttRoute.value ?: return
        mqttTelemetryManagerRef.value?.subscribeTelemetry(route.telemetryTopic)
    }

    private fun cachedMacMatches(requestedMac: String): Boolean {
        val cached = LoginPreferences.getCachedPairedDeviceMac()?.trim().orEmpty()
        if (cached.isEmpty()) return true
        return macsEqual(cached, requestedMac)
    }
}
