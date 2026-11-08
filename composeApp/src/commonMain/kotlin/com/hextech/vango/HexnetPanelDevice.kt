package com.hextech.vango

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * myhexnet panel cihaz kaydı — topic ve komut API'leri `device_id` (panel satır id) kullanır.
 * Firmware `mqtt_did` ile aynı olmalıdır.
 */
data class HexnetPanelDevice(
    val panelDeviceId: Int,
    val companyId: Int,
    val telemetryTopic: String? = null,
    val commandTopic: String? = null,
)

val hexnetPanelDevice = MutableStateFlow<HexnetPanelDevice?>(null)
