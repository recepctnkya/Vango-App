package com.hextech.vango

// UUIDs for the service and characteristic
const val serviceUUID:String = "0000abf0-0000-1000-8000-00805f9b34fb"
const val characteristicUUID:String = "0000abf2-0000-1000-8000-00805f9b34fb"
const val descriptorUUID:String = "00002902-0000-1000-8000-00805f9b34fb"
const val bleDevMAC:String = "80:65:99:A4:98:96"
const val bleDisplayName: String = "VANGO DISPLAY"
const val bleIoModuleName: String = "VANGO IO MODULE"

/** VanGO servis UUID — IO modül reklamında isim olmasa bile tanıma için kullanılır. */
const val bleVanGoServiceUuid: String = "0000abf0-0000-1000-8000-00805f9b34fb"

/** BLE taraması / eşleştirme yalnızca IO modül için (DISPLAY hariç). */
fun isSupportedBleDeviceName(name: String): Boolean {
    val n = name.trim()
    if (n.equals(bleIoModuleName, ignoreCase = true)) return true
    if (n.equals(bleDisplayName, ignoreCase = true)) return false
    val lower = n.lowercase()
    return lower.contains("vango") && lower.contains("io")
}

fun defaultBleNameForAdvertisedDevice(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isNotEmpty() && isSupportedBleDeviceName(trimmed)) return trimmed
    return bleIoModuleName
}

data class BleDevice(
    val name: String = "",
    val mac: String = "",
    val rssi: Int? = null,
)

interface BluetoothManager {
    fun bluetoothSupport(): Boolean
    fun bluetoothEnabled(): Boolean
    fun isConnected(): Boolean
    /**
     * Bağlı VanGO cihazının BLE donanım adresi (ör. AA:BB:CC:DD:EE:FF), yoksa null.
     * iOS veya bağlantı yoksa null dönebilir.
     */
    fun connectedDeviceMac(): String? = null
    fun connect(index: Int): Boolean
    /** MAC ile bağlanır; manuel eklenen veya taramada görünmeyen cihazlar için kullanılır. */
    fun connectByMac(mac: String): Boolean = false
    fun disconnect()
    fun startDiscovering(): Boolean
    fun stopDiscovering()
    fun discoverServices():Boolean
    fun configureMtu():Boolean
    fun enableReadChar():Boolean
    fun sendData(data:String): Boolean
    suspend fun sendWeatherData(data:String): Boolean

    fun onStatusOfBle(state:VanGoState, data:Any?) {
        getAppContext().vanGO.onBleCallback(state, data)
    }
}