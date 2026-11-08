package com.hextech.vango

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector

val PanelSensorTypes = listOf(
    "temp_humidity",
    "voltage_current",
    "gas",
    "motion",
)

fun normalizePanelSensorType(type: String?): String {
    val t = type?.trim()?.lowercase().orEmpty()
    return if (t in PanelSensorTypes) t else "temp_humidity"
}

fun sensorDefaultName(type: String, index: Int): String {
    val base = when (normalizePanelSensorType(type)) {
        "temp_humidity" -> "Sicaklik / Nem"
        "voltage_current" -> "Voltaj / Akim"
        "gas" -> "Gaz Sensoru"
        "motion" -> "Hareket Sensoru"
        else -> "Sensor"
    }
    return if (index > 0) "$base ${index + 1}" else base
}

fun sensorDefaultIcon(type: String): String = when (normalizePanelSensorType(type)) {
    "temp_humidity" -> "gauge"
    "voltage_current" -> "bolt"
    "gas" -> "wind"
    "motion" -> "person-walking"
    else -> "gauge"
}

fun sensorTypeLabel(type: String): String = when (normalizePanelSensorType(type)) {
    "temp_humidity" -> "Sicaklik / Nem"
    "voltage_current" -> "Voltaj / Akim"
    "gas" -> "Gaz"
    "motion" -> "Hareket"
    else -> "Sensor"
}

fun sensorTypeVector(type: String): ImageVector = when (normalizePanelSensorType(type)) {
    "voltage_current" -> Icons.Default.Bolt
    else -> Icons.Default.Info
}

fun defaultPlatformSensors(count: Int): List<PanelPlatformChannel> =
    (0 until count).map { i ->
        val type = "temp_humidity"
        PanelPlatformChannel(
            id = i,
            enabled = i == 0,
            name = sensorDefaultName(type, i),
            icon = sensorDefaultIcon(type),
            sensorType = type,
            sortOrder = i,
        )
    }

fun isTempHumiditySensorEnabled(buffer: ByteArray): Boolean =
    buffer.getOrElse(0) { 0 } != 0.toByte() || buffer.getOrElse(1) { 0 } != 0.toByte()

fun isPlatformSensorEnabled(
    sensor: PanelPlatformChannel,
    buffer: ByteArray,
): Boolean = when {
    sensor.id == 0 && normalizePanelSensorType(sensor.sensorType) == "temp_humidity" ->
        isTempHumiditySensorEnabled(buffer)
    sensor.id <= 1 ->
        buffer.getOrElse(sensor.id) { 0 } != 0.toByte()
    else -> sensor.enabled
}

fun shouldShowCombinedTempHumidityCard(
    data: VanGoData,
    env: BleEnvSensors,
    platformChannels: PanelPlatformChannels?,
): Boolean {
    if (BleEnvSensorParser.resolveDisplayTempC(env, data) > 0) return true
    if (BleEnvSensorParser.resolveDisplayHumidityPct(env, data) > 0) return true
    if (data.getSafeSensorsEnabled(0) != 0.toByte()) return true
    if (platformChannels?.sensors?.any {
            it.enabled && normalizePanelSensorType(it.sensorType) == "temp_humidity"
        } == true) {
        return true
    }
    return true
}