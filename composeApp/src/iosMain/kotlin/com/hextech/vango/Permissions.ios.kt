package com.hextech.vango

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSError
import platform.Foundation.NSObject
import kotlin.coroutines.resume

private object IosLocationProvider : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private val mutex = Mutex()
    private var continuation: CancellableContinuation<Pair<Double, Double>?>? = null

    suspend fun getLocation(): Pair<Double, Double>? = mutex.withLock {
        withContext(Dispatchers.Main) {
            withTimeoutOrNull(20_000L) {
                suspendCancellableCoroutine { request ->
                    continuation = request
                    manager.delegate = this@IosLocationProvider
                    manager.desiredAccuracy = platform.CoreLocation.kCLLocationAccuracyKilometer
                    request.invokeOnCancellation {
                        manager.stopUpdatingLocation()
                        manager.delegate = null
                        continuation = null
                    }

                    when (manager.authorizationStatus) {
                        kCLAuthorizationStatusAuthorizedAlways,
                        kCLAuthorizationStatusAuthorizedWhenInUse -> manager.requestLocation()
                        kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
                        else -> finish(null)
                    }
                }
            }
        }
    }

    fun requestPermission() {
        manager.delegate = this
        if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
            manager.requestWhenInUseAuthorization()
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> {
                if (continuation?.isActive == true) manager.requestLocation()
            }
            kCLAuthorizationStatusNotDetermined -> Unit
            else -> finish(null)
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        finish(location?.let { it.coordinate.latitude to it.coordinate.longitude })
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        println("iOS location request failed: ${didFailWithError.localizedDescription}")
        finish(null)
    }

    private fun finish(result: Pair<Double, Double>?) {
        manager.stopUpdatingLocation()
        val pending = continuation
        continuation = null
        if (pending?.isActive == true) pending.resume(result)
    }
}

actual fun hasLocationPermissions(): Boolean =
    CLLocationManager().authorizationStatus.let {
        it == kCLAuthorizationStatusAuthorizedAlways ||
            it == kCLAuthorizationStatusAuthorizedWhenInUse
    }

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
    IosLocationProvider.requestPermission()
}

actual fun requestMissingConnectionPermissions() {
}

actual fun requestEnableBluetooth() {
}

actual suspend fun getLocationUpdates(): Pair<Double, Double>? = IosLocationProvider.getLocation()

actual fun getCountryCityName(lat: Double, long: Double): Pair<String, String> = "" to ""
