package com.hextech.vango

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** IO modül flash profili — firmware `io_profile` / BLE `io` ile uyumlu. */
@Serializable
data class IoProfileLinks(
    val display: Boolean = false,
    val ble: Boolean = false,
    val remote: Boolean = false,
)

@Serializable
data class IoProfileSnapshot(
    val relays: Int = 0,
    val dimmers: Int = 0,
    val sensors: Int = 0,
    val tanks: Int = 0,
    val motor: Boolean = true,
    val rgb: Boolean = true,
    @SerialName("relay_mask") val relayMask: Int = 0,
    @SerialName("dim_mask") val dimMask: Int = 0,
    @SerialName("sensor_mask") val sensorMask: Int = 0,
    @SerialName("tank_mask") val tankMask: Int = 0,
    @SerialName("panel_seq") val panelSeq: Int = 0,
    @SerialName("panel_changed") val panelChanged: Boolean = false,
    /** IO flash labels SoT — isim/ikon. */
    val channels: PanelPlatformChannels? = null,
    val links: IoProfileLinks = IoProfileLinks(),
)

/** BLE telemetri paketindeki kısa alanlar (IO modül v0-40+). */
@Serializable
data class IoProfileBleFields(
    @SerialName("motorEn") val motorEn: Int? = null,
    @SerialName("rgbEn") val rgbEn: Int? = null,
    @SerialName("tankN") val tankN: Int? = null,
    @SerialName("rMask") val relayMask: Int? = null,
    @SerialName("dMask") val dimMask: Int? = null,
    @SerialName("sMask") val sensorMask: Int? = null,
    @SerialName("tMask") val tankMask: Int? = null,
    @SerialName("ioLink") val ioLink: Int? = null,
    @SerialName("io") val io: IoProfileSnapshot? = null,
)

const val IO_LINK_DISPLAY = 0x01
const val IO_LINK_BLE = 0x02
const val IO_LINK_REMOTE = 0x04

fun maskToSlotBytes(mask: Int, maxSlots: Int, defaultType: Int = 1): ByteArray {
    val out = ByteArray(maxSlots) { 0 }
    for (i in 0 until maxSlots) {
        if ((mask and (1 shl i)) != 0) {
            out[i] = defaultType.toByte()
        }
    }
    return out
}

fun countEnabledSlots(buf: ByteArray): Int = buf.count { it != 0.toByte() }

fun IoProfileBleFields.toSnapshot(fallback: IoProfileSnapshot? = null): IoProfileSnapshot? {
    val nested = io
    if (nested != null) return nested
    val hasMask = relayMask != null || dimMask != null || sensorMask != null
    if (!hasMask && motorEn == null && rgbEn == null && tankN == null) {
        return fallback
    }
    return IoProfileSnapshot(
        relays = fallback?.relays ?: countBits16(relayMask ?: fallback?.relayMask ?: 0),
        dimmers = fallback?.dimmers ?: countBits8(dimMask ?: fallback?.dimMask ?: 0),
        sensors = fallback?.sensors ?: countBits8(sensorMask ?: fallback?.sensorMask ?: 0),
        tanks = tankN ?: fallback?.tanks ?: countBits8(tankMask ?: fallback?.tankMask ?: 0),
        motor = motorEn?.let { it != 0 } ?: fallback?.motor ?: true,
        rgb = rgbEn?.let { it != 0 } ?: fallback?.rgb ?: true,
        relayMask = relayMask ?: fallback?.relayMask ?: 0,
        dimMask = dimMask ?: fallback?.dimMask ?: 0,
        sensorMask = sensorMask ?: fallback?.sensorMask ?: 0,
        tankMask = tankMask ?: fallback?.tankMask ?: 0,
        panelSeq = fallback?.panelSeq ?: 0,
        panelChanged = fallback?.panelChanged ?: false,
        channels = fallback?.channels,
        links = fallback?.links ?: IoProfileLinks(),
    )
}

private fun countBits16(v: Int): Int {
    var n = 0
    var x = v and 0xFFFF
    while (x != 0) {
        n += x and 1
        x = x shr 1
    }
    return n
}

private fun countBits8(v: Int): Int = countBits16(v and 0xFF)

fun devConfigFromIoProfile(profile: IoProfileSnapshot, messageType: String = "Configuration"): DevConfig {
    val relayMask = profile.relayMask
    val dimMask = profile.dimMask
    val sensorMask = profile.sensorMask
    return DevConfig(
        MessageType = messageType,
        numOfOutputs = profile.relays.coerceIn(0, 16),
        OutputsNameBuffer = maskToSlotBytes(relayMask, 16),
        numOfDims = profile.dimmers.coerceIn(0, FIRMWARE_DIMMER_MAX),
        DimsNameBuffer = maskToSlotBytes(dimMask, FIRMWARE_DIMMER_MAX),
        numOfSensors = profile.sensors.coerceIn(0, 5),
        SensorsNameBuffer = maskToSlotBytes(sensorMask, 5),
        RGBEnables = if (profile.rgb) "yes" else "no",
        Theme = "0",
        motorEnabled = profile.motor,
        numOfTanks = profile.tanks.coerceIn(0, 4),
        tankEnableMask = profile.tankMask,
        relayEnableMask = relayMask,
        dimEnableMask = dimMask,
        sensorEnableMask = sensorMask,
        links = profile.links,
    )
}

fun mergeDevConfigWithIoProfile(current: DevConfig, profile: IoProfileSnapshot): DevConfig {
    val merged = devConfigFromIoProfile(profile, current.MessageType)
    return current.copy(
        numOfOutputs = merged.numOfOutputs,
        OutputsNameBuffer = merged.OutputsNameBuffer,
        numOfDims = merged.numOfDims,
        DimsNameBuffer = merged.DimsNameBuffer,
        numOfSensors = merged.numOfSensors,
        SensorsNameBuffer = merged.SensorsNameBuffer,
        RGBEnables = merged.RGBEnables,
        motorEnabled = merged.motorEnabled,
        numOfTanks = merged.numOfTanks,
        tankEnableMask = merged.tankEnableMask,
        relayEnableMask = merged.relayEnableMask,
        dimEnableMask = merged.dimEnableMask,
        sensorEnableMask = merged.sensorEnableMask,
        links = IoProfileLinks(
            display = current.links.display || profile.links.display,
            ble = current.links.ble || profile.links.ble,
            remote = current.links.remote || profile.links.remote,
        ),
    )
}

fun devConfigForBleWrite(current: DevConfig): DevConfig {
    val relayMask = current.relayEnableMask.takeIf { it != 0 }
        ?: slotBufferToMask(current.OutputsNameBuffer)
    val dimMask = current.dimEnableMask.takeIf { it != 0 }
        ?: slotBufferToMask(current.DimsNameBuffer)
    val sensorMask = current.sensorEnableMask.takeIf { it != 0 }
        ?: slotBufferToMask(current.SensorsNameBuffer)
    return current.copy(
        MessageType = "Configuration",
        numOfOutputs = countEnabledSlots(current.OutputsNameBuffer),
        numOfDims = countEnabledSlots(current.DimsNameBuffer),
        numOfSensors = countEnabledSlots(current.SensorsNameBuffer),
        motorEnabled = current.motorEnabled,
        relayEnableMask = relayMask,
        dimEnableMask = dimMask,
        sensorEnableMask = sensorMask,
        tankEnableMask = current.tankEnableMask,
        links = current.links.copy(ble = true),
    )
}

fun devConfigToIoProfileSnapshot(config: DevConfig): IoProfileSnapshot {
    val relayMask = config.relayEnableMask.takeIf { it != 0 }
        ?: slotBufferToMask(config.OutputsNameBuffer)
    val dimMask = config.dimEnableMask.takeIf { it != 0 }
        ?: slotBufferToMask(config.DimsNameBuffer)
    val sensorMask = config.sensorEnableMask.takeIf { it != 0 }
        ?: slotBufferToMask(config.SensorsNameBuffer)
    return IoProfileSnapshot(
        relays = config.numOfOutputs.coerceIn(0, 16),
        dimmers = config.numOfDims.coerceIn(0, FIRMWARE_DIMMER_MAX),
        sensors = config.numOfSensors.coerceIn(0, 5),
        tanks = config.numOfTanks.coerceIn(0, 4),
        motor = config.motorEnabled,
        rgb = config.RGBEnables.equals("yes", ignoreCase = true),
        relayMask = relayMask,
        dimMask = dimMask,
        sensorMask = sensorMask,
        tankMask = config.tankEnableMask,
        links = config.links,
    )
}

private fun slotBufferToMask(buf: ByteArray): Int {
    var mask = 0
    for (i in buf.indices) {
        if (buf[i] != 0.toByte()) mask = mask or (1 shl i)
    }
    return mask
}
