package com.hextech.vango

import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * BLE otomatik yeniden bağlanma politikası.
 * Kullanıcı bilerek kopardıysa veya uzaktan moda geçildiyse yeniden bağlanılmaz.
 */
val bleAutoReconnectEnabled = MutableStateFlow(true)

fun rememberLastBleAdapterMac(mac: String?) {
    val normalized = mac?.trim()?.takeIf { it.isNotEmpty() }?.let(::normalizeMacForDisplay)
        ?.takeIf { it.length == 17 && isValidMacFormat(it) }
        ?: return
    LoginPreferences.setCachedLastBleAdapterMac(normalized)
    bleAutoReconnectEnabled.value = true
}

fun resolveLastBleReconnectMac(): String? {
    LoginPreferences.getCachedLastBleAdapterMac()?.trim()?.takeIf { it.isNotEmpty() }
        ?.let(::normalizeMacForDisplay)
        ?.takeIf { it.length == 17 && isValidMacFormat(it) }
        ?.let { return it }

    getAppContext().ble().connectedDeviceMac()?.trim()?.takeIf { it.isNotEmpty() }
        ?.let(::normalizeMacForDisplay)
        ?.takeIf { it.length == 17 && isValidMacFormat(it) }
        ?.let { return it }

    vangoDeviceList.firstOrNull { isSupportedBleDeviceName(it.name) }?.mac
        ?.trim()?.takeIf { it.isNotEmpty() }
        ?.let(::normalizeMacForDisplay)
        ?.takeIf { it.length == 17 && isValidMacFormat(it) }
        ?.let { return it }

    currentPlatformDeviceMac()?.let { wifi ->
        deriveBleMacFromWifiMac(wifi)?.let { return it }
        return normalizeMacForDisplay(wifi).takeIf { it.length == 17 && isValidMacFormat(it) }
    }
    return null
}

fun shouldAttemptBleAutoReconnect(): Boolean {
    if (!bleAutoReconnectEnabled.value) return false
    if (remoteModeEnabled.value) return false
    if (isAppRemoteMqttConnected()) return false
    return resolveLastBleReconnectMac() != null
}
