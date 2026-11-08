package com.hextech.vango

import com.hextech.vango.profile.LoginPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class AppConnectionState {
    BLE_CONNECTED,
    REMOTE_MQTT_CONNECTED,
    OFFLINE
}

val appConnectionState = MutableStateFlow(AppConnectionState.OFFLINE)
val remoteModeEnabled = MutableStateFlow(false)
val remoteMqttAutoReconnect = MutableStateFlow(true)
val remoteMqttCheckIntervalSec = MutableStateFlow(30)
val remoteTelemetry = MutableStateFlow<RemoteTelemetry?>(null)
val lastSuccessfulTelemetryEpochSec = MutableStateFlow(0L)
/** Son başarılı uzaktan (MQTT) telemetri zamanı — platform→araç yarısı için. */
val lastRemoteTelemetryEpochSec = MutableStateFlow(0L)

fun isAppRemoteMqttConnected(): Boolean =
    appConnectionState.value == AppConnectionState.REMOTE_MQTT_CONNECTED

/**
 * Uzaktan: platform (MQTT) oturumu açıkken aracın taze telemetrisi var mı.
 * Yalnızca platforma bağlı olmak yetmez — son telemetri [maxAgeSec] içinde gelmeli.
 */
fun isRemoteVehicleReachable(maxAgeSec: Long = 20L): Boolean {
    if (!isAppRemoteMqttConnected()) return false
    val lastTel = lastRemoteTelemetryEpochSec.value
    if (lastTel <= 0L) return false
    val age = getCurrentTimeInSeconds() - lastTel
    return age in 0L..maxAgeSec
}

/** True when remote mode is the active control path (not BLE). */
fun isRemoteModeSelected(): Boolean =
    remoteModeEnabled.value && !isBleGattLinkReady()

/** Uzaktan seçiliyken platforma bağlanması yetmiyor. */
fun isRemoteFullyLinked(): Boolean =
    isAppRemoteMqttConnected() && isRemoteVehicleReachable()

/** Uzaktan mod açık ve panel cihaz kimliği çözümlenmiş. */
fun isRemoteUserModeActive(): Boolean =
    remoteModeEnabled.value && resolvedPanelDeviceId() != null

/** MQTT veya panel HTTP komut yolu kullanılabilir. */
fun isRemoteSessionUsable(): Boolean =
    isAppRemoteMqttConnected() || canSendRemoteCommands()

/** GATT hazır ve Android BLE katmanında cihaz seçili. */
fun isBleHardwareConnected(): Boolean = try {
    getAppContext().vanGO.isConnected()
} catch (_: Exception) {
    false
}

/** Canlı IO oturumu: GATT gerçekten bağlı ve notify veya taze telemetri var. */
fun isBleGattLinkReady(): Boolean {
    if (!isBleHardwareConnected()) return false
    if (vanGoGlobal.value.isCharacteristicNotificationOn) return true
    val lastTel = lastSuccessfulTelemetryEpochSec.value
    return lastTel > 0L && (getCurrentTimeInSeconds() - lastTel) <= 8L
}

/** Bağlantı sayfası ve üst çubuk ile aynı BLE durumunu paylaşır. */
fun syncBleAppConnectionStateIfLinked() {
    if (!isBleGattLinkReady()) return
    if (appConnectionState.value == AppConnectionState.REMOTE_MQTT_CONNECTED) {
        // BLE öncelikli: uzaktan bayrağını düşürüp BLE durumuna geç
        if (remoteModeEnabled.value) {
            remoteModeEnabled.value = false
        }
    }
    appConnectionState.value = AppConnectionState.BLE_CONNECTED
}

/** UI: BLE gerçekten bağlı mı. Stale BLE_CONNECTED / ChainEnded tek başına yetmez. */
fun isBleLinkActiveForUi(): Boolean = isBleGattLinkReady()

/** GATT el sıkışması veya tarama — bağlı değil, aranıyor. */
fun isBleHandshakeInProgress(): Boolean {
    if (isBleGattLinkReady()) return false
    val st = vanGoGlobal.value.connectionState
    return st == BleGattConnectionChain.DiscoveringDevice ||
        st == BleGattConnectionChain.ConnectingGattServer ||
        st == BleGattConnectionChain.DiscoveringServices ||
        (st == BleGattConnectionChain.ChainEnded && isBleHardwareConnected())
}

/** Panel `device_id` — MQTT topic ve mqtt-command.php için. */
fun resolvedPanelDeviceId(): Int? =
    hexnetPanelDevice.value?.panelDeviceId?.takeIf { it > 0 }
        ?: LoginPreferences.getCachedMqttDeviceId()?.takeIf { it > 0 }

/**
 * Mobil → cihaz komutu: MQTT telemetri dinleniyor veya uzaktan mod açık + panel kimliği var.
 * Gönderim öncelikle panel HTTP (`mqtt-command.php`), yedek doğrudan MQTT publish.
 */
fun canSendRemoteCommands(): Boolean {
    if (resolvedPanelDeviceId() == null) return false
    if (isAppRemoteMqttConnected()) return true
    if (!remoteModeEnabled.value) return false
    return !LoginPreferences.getAuthToken().isNullOrBlank()
}

/** Uygulama uzaktan kontrol ediyor veya cihaz uzak bağlantı bildiriyor. */
fun isRemoteControlActive(slvConn: String): Boolean =
    isRemoteSessionUsable() || isRemoteLinkActive(slvConn)

@Serializable
data class RemoteTelemetry(
    val ts: Long = 0,
    val protocol: String = "",
    val version: String = "",
    val power: PowerInfo = PowerInfo(),
    val device: DeviceInfo = DeviceInfo(),
    val status: StatusInfo = StatusInfo(),
    val outputs: OutputsInfo = OutputsInfo(),
    val sensors: SensorsInfo = SensorsInfo(),
    @SerialName("io_profile")
    val ioProfile: IoProfileSnapshot? = null,
    /** Panel telemetri `dim[]` — zaten 0-100 yuzde. */
    val dim: List<Int>? = null,
    /** Kok `dimmers[]` (varsa dim ile ayni olabilir). */
    val dimmers: List<Int>? = null,
)

/**
 * Panel `mapTelemetryToUI` yalnizca `dim[]` alanini yuzde kabul eder.
 * Kok `dimmers[]` ve `outputs.dimmers[]` PWM (0-255) olabilir.
 */
internal fun RemoteTelemetry.resolvedDimmerSources(): Pair<List<Int>, Boolean> {
    dim?.takeIf { it.isNotEmpty() }?.let { return it to true }
    dimmers?.takeIf { it.isNotEmpty() }?.let { return it to false }
    return outputs.dimmers to false
}

@Serializable
data class PowerInfo(
    @SerialName("vin_v")
    val vinV: Double = 0.0,
    @SerialName("battery_v")
    val batteryV: Double = 0.0,
    @SerialName("battery_pct")
    val batteryPct: Int = 0,
)

@Serializable
data class DeviceInfo(
    val mac: String = "",
    val id: Int = 0,
    @SerialName("company_id")
    val companyId: Int = 0,
    @SerialName("customer_id")
    val customerId: Int = 0,
)

@Serializable
data class StatusInfo(
    val online: Boolean = false,
    @SerialName("wifi_rssi")
    val wifiRssi: Int = 0,
    val mqtt: String = "",
)

@Serializable
data class OutputsInfo(
    val rgb: RgbInfo = RgbInfo(),
    val relays: List<Int> = emptyList(),
    val dimmers: List<Int> = emptyList(),
    @SerialName("relays_mask")
    val relaysMask: Int = 0,
)

@Serializable
data class RgbInfo(
    val r: Int = 0,
    val g: Int = 0,
    val b: Int = 0,
)

@Serializable
data class SensorsInfo(
    @SerialName("water_pct")
    val waterPct: List<Int> = emptyList(),
    @SerialName("humidity_pct")
    val humidityPct: Int = 0,
    @SerialName("temperature_c")
    val temperatureC: Int = 0,
)

private fun Int.toUnsignedByte(): Byte = (coerceIn(0, 255) and 0xFF).toByte()

fun applyRemoteTelemetryToUiState(
    telemetry: RemoteTelemetry,
    mergeIoProfileLayout: Boolean = !PanelSettingsPlatformSync.shouldPreservePlatformPanelLayout(),
) {
    val current = vanGoData.value

    val relaySize = maxOf(current.outDB.size, telemetry.outputs.relays.size, 16)
    val (dimSource, dimIsPercent) = telemetry.resolvedDimmerSources()
    val dimSize = maxOf(current.dDB.size, dimSource.size, 4)

    val relayIncoming = ByteArray(relaySize) { index ->
        telemetry.outputs.relays.getOrNull(index)?.coerceIn(0, 1)?.toUnsignedByte()
            ?: current.outDB.getOrElse(index) { 0 }
    }
    val relayData = OutputUiHold.mergeRelays(current.outDB, relayIncoming)

    val dimmerIncoming = ByteArray(dimSize) { index ->
        dimSource.getOrNull(index)?.let { raw ->
            DimmerScale.telemetryToPercentByte(raw, dimIsPercent)
        } ?: current.dDB.getOrElse(index) { 0 }
    }
    val dimmerData = OutputUiHold.mergeDimmers(current.dDB, dimmerIncoming)

    val water = telemetry.sensors.waterPct
    val sensors = byteArrayOf(
        telemetry.sensors.temperatureC.toUnsignedByte(),
        telemetry.sensors.humidityPct.toUnsignedByte(),
        (water.getOrNull(0) ?: current.getSafeSensorsDataUnsigned(2)).toUnsignedByte(),
        (water.getOrNull(1) ?: current.getSafeSensorsDataUnsigned(3)).toUnsignedByte(),
        (water.getOrNull(2) ?: current.getSafeSensorsDataUnsigned(4)).toUnsignedByte(),
    )

    val rgb = telemetry.outputs.rgb
    val light = if ((rgb.r + rgb.g + rgb.b) > 0) 1 else 0
    val rgbIncoming = byteArrayOf(
        rgb.r.toUnsignedByte(),
        rgb.b.toUnsignedByte(),
        rgb.g.toUnsignedByte(),
        light.toByte(),
    )
    val rgbDb = OutputUiHold.mergeRgb(current.RGBDB, rgbIncoming)

    val remoteTemp = telemetry.sensors.temperatureC.takeIf { it > 0 }
    val remoteHum = telemetry.sensors.humidityPct.takeIf { it > 0 }
    if (remoteTemp != null || remoteHum != null) {
        bleEnvSensors.value = BleEnvSensors(
            tempC = remoteTemp ?: bleEnvSensors.value.tempC,
            humidityPct = remoteHum ?: bleEnvSensors.value.humidityPct,
        )
    }

    val staMac = telemetry.device.mac.takeIf { it.isNotBlank() }
        ?: current.wifiMac
    val profile = telemetry.ioProfile
    val numOfO = if (mergeIoProfileLayout) {
        profile?.relays?.coerceIn(0, 16) ?: current.numOfO
    } else {
        current.numOfO
    }
    val numOfD = if (mergeIoProfileLayout) {
        profile?.dimmers?.coerceIn(0, FIRMWARE_DIMMER_MAX) ?: current.numOfD
    } else {
        current.numOfD
    }
    val numOfS = if (mergeIoProfileLayout) {
        profile?.sensors?.coerceIn(0, 5) ?: current.numOfS
    } else {
        current.numOfS
    }
    val outNB = if (mergeIoProfileLayout) {
        profile?.let { maskToSlotBytes(it.relayMask, 16) } ?: current.outNB
    } else {
        current.outNB
    }
    val dimNB = if (mergeIoProfileLayout) {
        profile?.let { maskToSlotBytes(it.dimMask, FIRMWARE_DIMMER_MAX) } ?: current.dimNB
    } else {
        current.dimNB
    }
    val senNB = if (mergeIoProfileLayout) {
        profile?.let { maskToSlotBytes(it.sensorMask, 5) } ?: current.senNB
    } else {
        current.senNB
    }
    val rgbE = if (mergeIoProfileLayout) {
        profile?.rgb?.let { if (it) "yes" else "no" } ?: current.RGBE
    } else {
        current.RGBE
    }

    vanGoData.value = VanGoData(
        slvConn = "connected",
        wifiMac = staMac,
        numOfO = numOfO,
        numOfD = numOfD,
        numOfS = numOfS,
        RGBE = rgbE,
        Th = current.Th,
        volt = telemetry.power.vinV.toFloat(),
        outNB = outNB,
        dimNB = dimNB,
        senNB = senNB,
        outDB = relayData,
        dDB = dimmerData,
        sDB = sensors,
        RGBDB = rgbDb
    )
    lastSuccessfulTelemetryEpochSec.value = getCurrentTimeInSeconds()
    lastRemoteTelemetryEpochSec.value = lastSuccessfulTelemetryEpochSec.value
    telemetry.device.mac.takeIf { it.isNotBlank() }?.let { normalizeAndCachePlatformWifiMac(it) }
    if (mergeIoProfileLayout) {
        applyRemoteIoProfileToUi(telemetry.ioProfile)
    }
    telemetry.ioProfile?.let { PanelSettingsPlatformSync.persistProfile(it) }
    val routeChanged = updateRemoteMqttRouteFromTelemetry(telemetry)
    RemoteMqttSession.onTelemetryRouteMaybeChanged(routeChanged)
}
