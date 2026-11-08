package com.hextech.vango

import kotlinx.coroutines.flow.MutableStateFlow

enum class VangoIoProfileKind {
    Medium,
    Mini,
    Unknown,
}

data class DevicePanelProfileState(
    val profileKey: String = "vango_medium",
    val kind: VangoIoProfileKind = VangoIoProfileKind.Medium,
    val caps: PanelProfileCap = PanelProfileCaps.getValue("vango_medium"),
    val displayName: String = "Vango IO Medium",
)

val devicePanelProfile = MutableStateFlow(DevicePanelProfileState())

const val FIRMWARE_RELAY_BUFFER_MAX = 16
const val FIRMWARE_DIMMER_MAX = 8
const val FIRMWARE_SENSOR_BUFFER_MAX = 5

fun panelProfileCap(profileKey: String): PanelProfileCap =
    PanelProfileCaps[profileKey] ?: PanelProfileCaps.getValue("vango_medium")

fun resolvePanelProfileKey(
    platformProfile: String?,
    uid: String? = null,
    hardware: String? = null,
    variant: String? = null,
): String {
    platformProfile?.trim()?.takeIf { PanelProfileCaps.containsKey(it) }?.let { return it }
    guessPanelProfileFromMetadata(uid, hardware, variant)?.let { return it }
    return guessPanelProfileFromUid(uid)
}

fun guessPanelProfileFromMetadata(
    uid: String?,
    hardware: String? = null,
    variant: String? = null,
): String? {
    val blob = listOfNotNull(uid, hardware, variant).joinToString(" ").trim().lowercase()
    if (blob.isBlank()) return null
    return when {
        blob.contains("mini") || blob.contains("vng-mn") || blob.contains("vngmn") -> "vango_mini"
        blob.contains("medium") || blob.contains("vng-md") || blob.contains("vngmd") -> "vango_medium"
        else -> null
    }
}

fun applyDevicePanelProfile(profileKey: String) {
    val key = profileKey.takeIf { PanelProfileCaps.containsKey(it) } ?: "vango_medium"
    val caps = panelProfileCap(key)
    val kind = when (key) {
        "vango_mini" -> VangoIoProfileKind.Mini
        "vango_medium" -> VangoIoProfileKind.Medium
        else -> VangoIoProfileKind.Unknown
    }
    val label = when (key) {
        "vango_mini" -> "Vango IO Mini"
        else -> "Vango IO Medium"
    }
    devicePanelProfile.value = DevicePanelProfileState(
        profileKey = key,
        kind = kind,
        caps = caps,
        displayName = label,
    )
    println("Device panel profile: $label (relays=${caps.relays}, dimmers=${caps.dimmers})")
}

fun ensureDevConfigSizedForProfile(devConfig: DevConfig, caps: PanelProfileCap): DevConfig {
    val relaySlots = caps.relays.coerceIn(1, FIRMWARE_RELAY_BUFFER_MAX)
    val dimSlots = caps.dimmers.coerceIn(1, FIRMWARE_DIMMER_MAX)
    val sensorSlots = FIRMWARE_SENSOR_BUFFER_MAX
    return devConfig.copy(
        OutputsNameBuffer = padProfileBuffer(devConfig.OutputsNameBuffer, relaySlots),
        DimsNameBuffer = padProfileBuffer(devConfig.DimsNameBuffer, dimSlots),
        SensorsNameBuffer = padProfileBuffer(devConfig.SensorsNameBuffer, sensorSlots),
        numOfOutputs = countEnabledSlots(devConfig.OutputsNameBuffer).coerceAtMost(relaySlots),
        numOfDims = countEnabledSlots(devConfig.DimsNameBuffer).coerceAtMost(dimSlots),
        numOfSensors = countEnabledSlots(devConfig.SensorsNameBuffer).coerceAtMost(sensorSlots),
        numOfTanks = caps.water.coerceIn(0, 4),
    )
}

private fun padProfileBuffer(source: ByteArray, size: Int): ByteArray {
    if (source.size == size) return source.copyOf()
    return ByteArray(size) { index -> source.getOrElse(index) { 0 } }
}