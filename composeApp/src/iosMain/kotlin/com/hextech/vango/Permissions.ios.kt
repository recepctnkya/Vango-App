package com.hextech.vango

actual fun hasLocationPermissions(): Boolean = true

actual fun openAppSettings() {
    // iOS: no-op in this build target
}

actual fun openLocationSettings() {
    // iOS: no-op in this build target
}

actual fun hasNearbyDevicesPermissions(): Boolean = true

actual fun isBluetoothAdapterEnabled(): Boolean = true

actual fun requestNearbyDevicesPermissions() {
}

actual fun requestLocationPermissions() {
}

actual fun requestMissingConnectionPermissions() {
}

actual fun requestEnableBluetooth() {
}

actual suspend fun getLocationUpdates(): Pair<Double, Double>? = null

actual fun getCountryCityName(lat: Double, long: Double): Pair<String, String> = "" to ""
