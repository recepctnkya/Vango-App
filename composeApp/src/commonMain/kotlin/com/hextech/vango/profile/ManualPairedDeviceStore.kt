package com.hextech.vango.profile

import com.hextech.vango.BleDevice
import com.hextech.vango.bleIoModuleName
import com.hextech.vango.vangoDeviceList
import com.hextech.vango.vangoDeviceListSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StoredManualDevice(
    val mac: String,
    val uid: String? = null,
    val name: String = "",
)

class DuplicateManualDeviceMacException(mac: String) : Exception("MAC already added: $mac")

object ManualPairedDeviceStore {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): List<StoredManualDevice> {
        val raw = ManualDevicePreferences.getJson() ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredManualDevice>>(raw)
        }.getOrElse { emptyList() }
    }

    fun macExists(mac: String): Boolean {
        val normalized = normalizeMacForDisplay(mac)
        if (normalized.length != 17) return false
        return getAll().any { it.mac.equals(normalized, ignoreCase = true) } ||
            vangoDeviceList.any { it.mac.equals(normalized, ignoreCase = true) }
    }

    fun add(mac: String, uid: String? = null, name: String? = null): Result<StoredManualDevice> = runCatching {
        val normalizedMac = normalizeMacForDisplay(mac.trim())
        if (!isValidMacFormat(normalizedMac)) {
            error("Geçersiz MAC adresi.")
        }
        if (macExists(normalizedMac)) {
            throw DuplicateManualDeviceMacException(normalizedMac)
        }
        val resolvedName = name?.trim()?.takeIf { it.isNotEmpty() }
            ?: defaultNameForMac(normalizedMac)
        val entry = StoredManualDevice(
            mac = normalizedMac,
            uid = uid?.trim()?.takeIf { it.isNotEmpty() },
            name = resolvedName,
        )
        val updated = getAll() + entry
        ManualDevicePreferences.setJson(json.encodeToString(updated))
        syncEntryToVangoList(entry)
        entry
    }

    fun syncAllToVangoList() {
        getAll().forEach { syncEntryToVangoList(it) }
    }

    fun remove(mac: String): Boolean {
        val normalized = normalizeMacForDisplay(mac.trim())
        if (!isValidMacFormat(normalized)) return false
        val current = getAll()
        if (current.none { it.mac.equals(normalized, ignoreCase = true) }) return false
        val updated = current.filterNot { it.mac.equals(normalized, ignoreCase = true) }
        ManualDevicePreferences.setJson(json.encodeToString(updated))
        removeFromVangoList(normalized)
        return true
    }

    private fun removeFromVangoList(mac: String) {
        val idx = vangoDeviceList.indexOfFirst { it.mac.equals(mac, ignoreCase = true) }
        if (idx < 0) return
        if (vangoDeviceList[idx].rssi == null) {
            vangoDeviceList.removeAt(idx)
            vangoDeviceListSize.value = vangoDeviceList.size
        }
    }

    /** Manuel kayıt veya ham MAC'i keşif listesine ekler; bağlanmadan önce çağrılır. */
    fun ensureMacInDiscoveryList(mac: String) {
        val normalized = normalizeMacForDisplay(mac.trim())
        if (normalized.length != 17 || !isValidMacFormat(normalized)) return
        if (vangoDeviceList.any { it.mac.equals(normalized, ignoreCase = true) }) return
        val stored = getAll().firstOrNull { it.mac.equals(normalized, ignoreCase = true) }
        if (stored != null) {
            syncEntryToVangoList(stored)
        } else {
            vangoDeviceList.add(BleDevice(bleIoModuleName, normalized, rssi = null))
            vangoDeviceListSize.value = vangoDeviceList.size
        }
    }

    private fun syncEntryToVangoList(entry: StoredManualDevice) {
        if (vangoDeviceList.any { it.mac.equals(entry.mac, ignoreCase = true) }) return
        val ble = BleDevice(entry.name, entry.mac, rssi = null)
        vangoDeviceList.add(ble)
        vangoDeviceListSize.value = vangoDeviceList.size
    }

    private fun defaultNameForMac(@Suppress("UNUSED_PARAMETER") mac: String): String = bleIoModuleName
}

fun StoredManualDevice.toBleDevice(): BleDevice = BleDevice(name, mac, rssi = null)

fun mergeManualDevicesIntoList(existing: List<BleDevice>): List<BleDevice> {
    val byMac = existing.associateBy { it.mac.uppercase() }.toMutableMap()
    ManualPairedDeviceStore.getAll().forEach { stored ->
        byMac.putIfAbsent(stored.mac.uppercase(), stored.toBleDevice())
    }
    return byMac.values.sortedBy { it.name }
}
