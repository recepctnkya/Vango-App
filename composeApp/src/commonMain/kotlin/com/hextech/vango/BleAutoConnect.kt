package com.hextech.vango

import com.hextech.vango.profile.LoginPreferences
import kotlin.concurrent.Volatile

/**
 * Tek merkezi BLE otomatik bağlanma.
 * Ortamda canlı (rssi dolu) tek IO varsa ona bağlanır; birden fazlaysa kayıtlı MAC'e kilitlenir.
 */
object BleAutoConnect {
    @Volatile
    private var lastAttemptAtMs: Long = 0L

    @Volatile
    private var lastAttemptMac: String? = null

    fun resetThrottle() {
        lastAttemptAtMs = 0L
        lastAttemptMac = null
    }

    fun tryAutoConnect(reason: String = "") {
        if (remoteModeEnabled.value) {
            println("BleAutoConnect skip ($reason): remote mode")
            return
        }
        if (!bleAutoReconnectEnabled.value) {
            println("BleAutoConnect skip ($reason): user disabled")
            return
        }
        if (isBleGattLinkReady()) return
        val pairPageSkip = isOnBleConnectionFlowPage() &&
            reason != "connection-page" &&
            reason != "mode-ble" &&
            reason != "refresh"
        if (pairPageSkip) {
            println("BleAutoConnect skip ($reason): on pair page")
            return
        }

        val state = vanGoGlobal.value.connectionState
        if (state == BleGattConnectionChain.ConnectingGattServer ||
            state == BleGattConnectionChain.DiscoveringServices ||
            state == BleGattConnectionChain.ChainEnded
        ) {
            return
        }

        val ioDevices = preferBleAdapterDevices(
            vangoDeviceList.filter { isSupportedBleDeviceName(it.name) },
        )
        // Yalnızca gerçek tarama sonuçları (önbellek/seed rssi=null — bağlanma tetiklemez)
        val live = ioDevices.filter { it.rssi != null }
        val lastMac = LoginPreferences.getCachedLastBleAdapterMac()
            ?.let { normalizeMacSafe(it) }

        val targetMac = when {
            // Ortamda tek canlı IO → her zaman ona bağlan
            live.size == 1 -> normalizeMacSafe(live.first().mac)
            // Birden fazla canlı: kayıtlı MAC reklamdaysa ona
            lastMac != null && live.any { macsEqual(it.mac, lastMac) } -> lastMac
            // Tarama bitti / retry: canlı yoksa kayıtlı MAC'e doğrudan dene
            lastMac != null &&
                live.isEmpty() &&
                (reason == "discover-done" || reason == "discover-finished-ui" ||
                    reason == "retry-failed" || reason == "startup") &&
                (state == BleGattConnectionChain.DiscoverFinished ||
                    state == BleGattConnectionChain.ConnectingGattServerFailed ||
                    reason == "startup" || reason == "retry-failed") -> lastMac
            else -> null
        } ?: run {
            println(
                "BleAutoConnect skip ($reason): waiting live IO " +
                    "(live=${live.size} last=$lastMac state=$state)",
            )
            return
        }

        val now = getCurrentTimeInSeconds() * 1000L
        if (macsEqual(targetMac, lastAttemptMac ?: "") && now - lastAttemptAtMs < 5_000L) {
            return
        }
        lastAttemptAtMs = now
        lastAttemptMac = targetMac
        bleAutoReconnectEnabled.value = true
        println("🔗 BleAutoConnect ($reason) → $targetMac (live=${live.map { it.mac }})")
        getVanGO().connectByMac(targetMac)
    }

    private fun normalizeMacSafe(mac: String): String? {
        val n = com.hextech.vango.profile.normalizeMacForDisplay(mac)
        return n.takeIf { it.length == 17 && com.hextech.vango.profile.isValidMacFormat(it) }
    }
}
