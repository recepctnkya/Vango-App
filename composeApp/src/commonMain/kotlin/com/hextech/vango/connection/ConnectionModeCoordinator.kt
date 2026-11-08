package com.hextech.vango

import com.hextech.vango.mqtt.mqttTelemetryManagerRef

/** Uygulama ayni anda yalnizca BLE veya uzaktan (MQTT) modunda olmalidir. */
fun disconnectBleLink(suppressAutoReconnect: Boolean = true) {
    if (suppressAutoReconnect) {
        bleAutoReconnectEnabled.value = false
    }
    try {
        getAppContext().ble().stopDiscovering()
    } catch (_: Exception) {
    }
    try {
        getAppContext().ble().disconnect()
    } catch (_: Exception) {
    }
    vanGoGlobal.value = vanGoGlobal.value.copy(
        isCharacteristicNotificationOn = false,
    )
    if (appConnectionState.value == AppConnectionState.BLE_CONNECTED) {
        appConnectionState.value = AppConnectionState.OFFLINE
    }
}

fun disconnectRemoteLink() {
    remoteModeEnabled.value = false
    mqttTelemetryManagerRef.value?.disconnect()
    remoteMqttRoute.value = null
    hexnetPanelDevice.value = null
    if (appConnectionState.value == AppConnectionState.REMOTE_MQTT_CONNECTED) {
        appConnectionState.value = AppConnectionState.OFFLINE
    }
}

fun activateRemoteModeExclusive() {
    disconnectBleLink(suppressAutoReconnect = true)
    remoteModeEnabled.value = true
}

fun activateBleModeExclusive() {
    disconnectRemoteLink()
    bleAutoReconnectEnabled.value = true
}
