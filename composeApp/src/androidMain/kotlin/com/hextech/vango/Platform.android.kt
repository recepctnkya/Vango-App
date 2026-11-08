package com.hextech.vango

import android.Manifest
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Android-specific implementation of the Platform interface
 * Provides Android platform capabilities including permission management
 */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Android implementation of VanGO device manager
 * Handles Bluetooth permissions and platform-specific operations
 */
class AndroidVanGO() : VanGO() {

    /**
     * Checks if the device has location permissions required for BLE scanning
     * @return true if location permissions are granted
     */
    private fun hasLocationPermissionsInternal(): Boolean {
        val context = getAppContext().get() as Context
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Checks if the device has all Bluetooth permissions required for BLE operations
     * Handles both Android 12+ and legacy permission models
     * @return true if all Bluetooth permissions are granted
     */
    private fun hasBluetoothPermissions(): Boolean {
        val context = getAppContext().get() as Context
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 12 permissions
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Comprehensive permission check for all required VanGO operations
     * Checks Bluetooth, location, and app-specific permissions
     * @return true if all permissions are granted and ready for BLE operations
     */
    override fun checkPermissions():Boolean {
        // Check if all required permissions are granted
        println("-----------------------------------------  ${hasBluetoothPermissions()}")
        println("-----------------------------------------  ${hasLocationPermissionsInternal()}")
        val hasAllPermissions = hasBluetoothPermissions() && hasLocationPermissionsInternal()
        
        if (hasAllPermissions) {
            setPermissionStatus(PermissionStatus.GRANTED)
            return true
        }
        
        // Wait for permission status to be updated by MainActivity
        return when (getPermissionStatus()) {
            PermissionStatus.GRANTED -> true
            PermissionStatus.DENIED -> false
            PermissionStatus.NOT_REQUESTED -> false
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun getVanGO(): VanGO = AndroidVanGO()

actual fun isWifiConnected(): Boolean = try {
    val context = getAppContext().get() as Context
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } else {
        @Suppress("DEPRECATION")
        cm.activeNetworkInfo?.let { it.isConnected && it.type == ConnectivityManager.TYPE_WIFI } == true
    }
} catch (_: Throwable) {
    false
}

actual fun isInternetAvailable(): Boolean {
    return try {
        val context = getAppContext().get() as Context
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            val hasTransport =
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            hasTransport && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    } catch (_: Throwable) {
        false
    }
}