package com.hextech.vango

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

enum class PermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED
}

class AppContext(app: Application, ble: BluetoothManager): IAppContext {
    override val application: Application = app
    override val bluetoothManager: BluetoothManager = ble
    override val vanGO: AndroidVanGO = AndroidVanGO()

    override fun get(): Context {
        return application.applicationContext
    }
}

private lateinit var appContext: AppContext
private var permissionStatus: PermissionStatus = PermissionStatus.NOT_REQUESTED

actual fun setAppContext(appC: IAppContext) {
    appContext = appC as AppContext
}

actual fun getAppContext():IAppContext {
    if(::appContext.isInitialized.not()) throw Exception("Application context isn't initialized")
    return appContext
}

fun setPermissionStatus(status: PermissionStatus) {
    permissionStatus = status
}

fun getPermissionStatus(): PermissionStatus {
    return permissionStatus
}

actual fun hasLocationPermissions(): Boolean {
    val context = getAppContext().get() as Context
    val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    println("🔐 Permission Check: ACCESS_FINE_LOCATION = $fineLocation, ACCESS_COARSE_LOCATION = $coarseLocation")

    // Yaklaşık konum (yalnızca COARSE) de yeterli sayılır.
    return fineLocation || coarseLocation
}

actual fun isLocationServicesEnabled(): Boolean {
    val context = getAppContext().get() as Context
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
}

actual fun bleScanRequiresLocationEnabled(): Boolean {
    // API 31+: BLUETOOTH_SCAN + neverForLocation ile LE tarama GPS olmadan çalışır.
    return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S
}

lateinit var fusedLocationClient: FusedLocationProviderClient

actual suspend fun getLocationUpdates():Pair<Double, Double>? {
    // Check permissions before accessing location
    if (!hasLocationPermissions()) {
        println("⚠️ getLocationUpdates: Location permissions NOT granted")
        return null
    }
    if (!isLocationServicesEnabled()) {
        println("⚠️ getLocationUpdates: Location services OFF — skip (no GPS hang)")
        return null
    }
    println("✅ getLocationUpdates: Location permissions granted, fetching location...")

    return suspendCancellableCoroutine { continuation ->
        try {
            // First try lastLocation (fast, cached)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        println("✅ getLocationUpdates: Got cached location: ${location.latitude}, ${location.longitude}")
                        continuation.resume(Pair(location.latitude, location.longitude))
                    } else {
                        println("⚠️ getLocationUpdates: Cached location is null, requesting fresh location...")
                        // Request fresh location if lastLocation is null
                        requestFreshLocation(continuation)
                    }
                }
                .addOnFailureListener { exception ->
                    println("❌ getLocationUpdates: lastLocation failed - ${exception.message}, trying fresh request...")
                    requestFreshLocation(continuation)
                }
        } catch (e: SecurityException) {
            println("❌ getLocationUpdates: SecurityException - ${e.message}")
            continuation.resume(null)
        }
    }
}

private fun requestFreshLocation(continuation: kotlinx.coroutines.CancellableContinuation<Pair<Double, Double>?>) {
    try {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).setMaxUpdates(1).build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = result.lastLocation
                if (location != null) {
                    println("✅ getLocationUpdates: Got fresh location: ${location.latitude}, ${location.longitude}")
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    println("❌ getLocationUpdates: Fresh location request returned null")
                    continuation.resume(null)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            android.os.Looper.getMainLooper()
        )

        // Timeout after 10 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (continuation.isActive) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                println("❌ getLocationUpdates: Fresh location request timed out")
                continuation.resume(null)
            }
        }, 10000)
    } catch (e: SecurityException) {
        println("❌ getLocationUpdates: SecurityException in fresh request - ${e.message}")
        continuation.resume(null)
    }
}

actual fun getCountryCityName(lat: Double, long: Double):Pair<String, String> {
    var cityName: String? = null
    var countryName: String? = null
    try {
        val geoCoder = Geocoder(getAppContext().get() as Context, Locale.getDefault())
        val address = geoCoder.getFromLocation(lat, long, 1)
        
        // Safe access to address list with bounds checking
        if (!address.isNullOrEmpty() && address.size > 0) {
            val firstAddress = address[0]
            
            // Safe access to address properties
            firstAddress?.adminArea?.let {
                if (it.isNotEmpty()) {
                    cityName = it
                }
            }
            
            if (cityName == null) {
                firstAddress?.locality?.let {
                    if (it.isNotEmpty()) {
                        cityName = it
                    }
                }
            }
            
            if (cityName == null) {
                firstAddress?.subAdminArea?.let {
                    if (it.isNotEmpty()) {
                        cityName = it
                    }
                }
            }
            
            firstAddress?.countryName?.let {
                if (it.isNotEmpty()) {
                    countryName = it
                }
            }
        } else {
            println("Geocoding returned empty or null address list")
        }
    } catch (e: Exception) { // exeption aliniyor?
        println("Geocoding failed: ${e.message} --- $e")
    }

    println("The country name ${countryName} and city name ${cityName}")
    return Pair(countryName ?: "", cityName ?: "")
}

actual fun openAppSettings() {
    val context = getAppContext().get() as Context
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    println("📱 Opening app settings...")
}

actual fun openLocationSettings() {
    val context = getAppContext().get() as Context
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { println("📱 openLocationSettings failed: ${it.message}") }
}

actual fun hasNearbyDevicesPermissions(): Boolean {
    val context = getAppContext().get() as Context
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
    }
}

actual fun isBluetoothAdapterEnabled(): Boolean {
    return try {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter() ?: return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val context = getAppContext().get() as Context
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
            if (!connectGranted) {
                // CONNECT yokken isEnabled SecurityException atabilir; radyo durumunu bilmiyoruz.
                // Yakındaki cihazlar izni verilince tekrar ölçülür.
                return false
            }
        }
        adapter.isEnabled
    } catch (_: Throwable) {
        false
    }
}

actual fun requestNearbyDevicesPermissions() {
    MainActivity.current?.requestNearbyDevicesPermissions()
}

actual fun requestLocationPermissions() {
    MainActivity.current?.requestLocationPermissions()
}

actual fun requestMissingConnectionPermissions() {
    MainActivity.current?.requestConnectionPermissions()
}

actual fun requestEnableBluetooth() {
    MainActivity.current?.requestEnableBluetooth()
}

actual fun closeApplication() {
    val activity = MainActivity.current ?: return
    activity.runOnUiThread {
        activity.finishAffinity()
    }
}


//actual object AppContext {
//    private lateinit var application: Application
//    private lateinit var bleManager: BluetoothManager
//    private var permission: Int = 0
//
//    fun setPermission(permissionResult:Int) {
//        permission = permissionResult
//    }
//
//    fun getPermission(): Int {
//        return permission
//    }
//
//    fun setUp(context: Context) {
//        application = context as Application
//        bleManager = BluetoothManager()
//    }
//
//    fun get(): Context {
//        if (::application.isInitialized.not()) throw Exception("Application context isn't initialized")
//        return application.applicationContext
//    }
//
//    fun ble(): BluetoothManager {
//        if (::bleManager.isInitialized.not()) throw Exception("BluetoothManager context isn't initialized")
//        return bleManager
//    }
//}
