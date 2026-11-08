package com.hextech.vango

import com.hextech.vango.profile.LoginPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/** MQTT topic path: hexnet/v1/{companyId}/{deviceId}/telemetry|command */
data class RemoteMqttRoute(
    val companyId: Int,
    val deviceId: Int,
) {
    val telemetryTopic: String get() = "hexnet/v1/$companyId/$deviceId/telemetry"
    val commandTopic: String get() = "hexnet/v1/$companyId/$deviceId/command"

    companion object {
        val defaultTest = RemoteMqttRoute(companyId = 1, deviceId = 1)

        fun fromTopics(telemetry: String, command: String?): RemoteMqttRoute? {
            val parts = telemetry.trim().removeSuffix("/telemetry").split("/")
            if (parts.size < 4) return null
            val deviceId = parts.last().toIntOrNull() ?: return null
            val companyId = parts[parts.size - 2].toIntOrNull() ?: return null
            return RemoteMqttRoute(companyId = companyId, deviceId = deviceId)
        }
    }
}

val remoteMqttRoute = MutableStateFlow<RemoteMqttRoute?>(null)
val remoteSessionError = MutableStateFlow<String?>(null)

fun persistRemoteMqttRoute(route: RemoteMqttRoute) {
    LoginPreferences.setCachedMqttCompanyId(route.companyId)
    LoginPreferences.setCachedMqttDeviceId(route.deviceId)
    remoteMqttRoute.value = route
}

fun readCachedRemoteMqttRoute(): RemoteMqttRoute? {
    val company = LoginPreferences.getCachedMqttCompanyId() ?: return null
    val device = LoginPreferences.getCachedMqttDeviceId() ?: return null
    if (company <= 0 || device <= 0) return null
    return RemoteMqttRoute(companyId = company, deviceId = device)
}

fun parseNumericDeviceId(uid: String?): Int? {
    val t = uid?.trim().orEmpty()
    if (t.isEmpty()) return null
    return t.toIntOrNull()?.takeIf { it > 0 }
        ?: t.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }
}

/**
 * Telemetriden gelen company/device kimlikleri önbelleğe yazılır.
 * @return route değiştiyse true (yeniden abone olunmalı)
 */
fun updateRemoteMqttRouteFromTelemetry(telemetry: RemoteTelemetry): Boolean {
    val company = telemetry.device.companyId
    val device = telemetry.device.id
    if (company <= 0 || device <= 0) return false
    val next = RemoteMqttRoute(companyId = company, deviceId = device)
    val prev = remoteMqttRoute.value ?: readCachedRemoteMqttRoute()
    if (prev == next) return false
    persistRemoteMqttRoute(next)
    return true
}

fun resolveRemoteMqttRoute(
    companyId: Int? = null,
    deviceId: Int? = null,
    deviceUid: String? = null,
): RemoteMqttRoute {
    if (companyId != null && deviceId != null && companyId > 0 && deviceId > 0) {
        return RemoteMqttRoute(companyId, deviceId)
    }
    readCachedRemoteMqttRoute()?.let { return it }
    remoteMqttRoute.value?.let { return it }
    val fromUid = parseNumericDeviceId(deviceUid)
    if (fromUid != null) {
        val company = LoginPreferences.getCachedMqttCompanyId()?.takeIf { it > 0 } ?: 1
        return RemoteMqttRoute(companyId = company, deviceId = fromUid)
    }
    return RemoteMqttRoute.defaultTest
}
