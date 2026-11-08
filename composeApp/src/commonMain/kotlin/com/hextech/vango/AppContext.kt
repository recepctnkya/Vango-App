package com.hextech.vango

interface IAppContext {
    val application:Any
    val bluetoothManager: BluetoothManager
    val vanGO: VanGO

    fun get(): Any {
        return application
    }

    fun ble(): BluetoothManager {
        return bluetoothManager
    }
}

expect fun setAppContext(appC: IAppContext)
expect fun getAppContext():IAppContext
expect suspend fun getLocationUpdates():Pair<Double, Double>?
expect fun getCountryCityName(lat: Double,long: Double):Pair<String, String>
expect fun hasLocationPermissions(): Boolean
/** Android: konum servisi (GPS) açık mı. */
expect fun isLocationServicesEnabled(): Boolean
/**
 * true ise BLE tarama için konum servisinin açık olması gerekir (eski Android).
 * Android 12+ + BLUETOOTH_SCAN(neverForLocation) için false.
 */
expect fun bleScanRequiresLocationEnabled(): Boolean
expect fun openAppSettings()

/** Opens system location settings (GPS/network location toggle). */
expect fun openLocationSettings()

/** BLE yakındaki cihaz izinleri (Android 12+: SCAN+CONNECT). */
expect fun hasNearbyDevicesPermissions(): Boolean
/** Telefon Bluetooth radyo açık mı. */
expect fun isBluetoothAdapterEnabled(): Boolean
/** BLE yakındaki cihaz izinlerini ister (sistem diyaloğu — yalnızca kullanıcı aksiyonuyla). */
expect fun requestNearbyDevicesPermissions()
/** Konum izinlerini ister (sistem diyaloğu — yalnızca kullanıcı aksiyonuyla). */
expect fun requestLocationPermissions()
/** Eksik BLE + konum izinlerini birlikte ister. */
expect fun requestMissingConnectionPermissions()
/** Bluetooth açma isteği (sistem diyaloğu). */
expect fun requestEnableBluetooth()

/** Platform uygulamasını kapatır (Android: aktiviteyi sonlandırır). */
expect fun closeApplication()


//expect object getAppContext()
