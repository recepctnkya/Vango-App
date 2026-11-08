package com.hextech.vango

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.hextech.vango.profile.ThemeForegroundSync
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun VangoRootWithTestTags() {
    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        App()
    }
}

class MainActivity : ComponentActivity(), DefaultLifecycleObserver {

    companion object {
        @Volatile
        var current: MainActivity? = null
            private set
    }

    private var bleService: VanGoBleService? = null
    private var serviceBound = false
    private val permissionRequestCode = 123

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VanGoBleService.VanGoBinder
            bleService = binder.getService()
            serviceBound = true
            println("🔗 BLE Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleService = null
            serviceBound = false
            println("🔗 BLE Service disconnected")
        }
    }

    // Permission launcher for background location (must be requested separately on Android 10+)
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        println("🔐 Background location permission: ${if (granted) "GRANTED" else "DENIED"}")
        if (granted) {
            println("✅ Background location granted - weather updates will work from service")
        } else {
            println("⚠️ Background location denied - weather updates may not work when app is backgrounded")
        }
    }

    // Permission launcher for newer Android versions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        println("🔐 Permission launcher callback received!")
        permissions.forEach { (permission, granted) ->
            println("🔐 Result: $permission = ${if (granted) "GRANTED" else "DENIED"}")
        }
        // Yalnız istenen izinler döner; tam seti yeniden doğrula
        val btReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
        val locationReady =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fullyReady = btReady && locationReady
        if (fullyReady) {
            setPermissionStatus(PermissionStatus.GRANTED)
            println("✅ All permissions granted")
            requestBackgroundLocationPermission()
        } else {
            setPermissionStatus(PermissionStatus.DENIED)
            println("❌ Some permissions denied: ${permissions.filter { !it.value }.keys}")
        }
    }

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // Background location permission for weather updates from foreground service
    private val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    } else {
        null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)
        current = this
        lockOrientationForDevice()

        // Add lifecycle observer to handle app background/foreground events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize AppContext with proper BLE manager
        setAppContext(AppContext(this.application, AndroidBLE(BluetoothAdapter.getDefaultAdapter())))
        enableEdgeToEdge()


        hideSystemBars()
        // Sistem izin diyaloğunu açılışta gösterme — uygulama içi izin ekranı ister.
        refreshPermissionStatusWithoutPrompt()
        registeBluetoothReceiver()

        setContent {
            VangoRootWithTestTags()
        }
    }

    /** Tablets: landscape (yan). Phones: portrait (düz). Same UI shell, different orientation. */
    private fun lockOrientationForDevice() {
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        requestedOrientation = if (isTablet) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super<ComponentActivity>.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permissionRequestCode -> {
                var allGranted = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }
                
                if (allGranted) {
                    setPermissionStatus(PermissionStatus.GRANTED)
                } else {
                    setPermissionStatus(PermissionStatus.DENIED)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        requestConnectionPermissions()
    }

    /** Açılışta yalnızca durum senkronu; sistem diyaloğu açılmaz. */
    private fun refreshPermissionStatusWithoutPrompt() {
        val btReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
        val locationReady =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (btReady && locationReady) {
            setPermissionStatus(PermissionStatus.GRANTED)
            println("🔐 Permissions already granted (no prompt)")
        } else {
            setPermissionStatus(PermissionStatus.NOT_REQUESTED)
            println("🔐 Permissions incomplete — waiting for in-app request")
        }
    }

    private fun launchPermissionRequest(permissions: Array<String>) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            refreshPermissionStatusWithoutPrompt()
            if (getPermissionStatus() == PermissionStatus.GRANTED) {
                requestBackgroundLocationPermission()
            }
            return
        }
        println("🔐 Requesting permissions (user action): $missing")
        permissionLauncher.launch(missing.toTypedArray())
    }

    /** PermissionDenied ekranı: tüm eksik bağlantı izinleri. */
    fun requestConnectionPermissions() {
        launchPermissionRequest(requiredPermissions)
    }

    fun requestNearbyDevicesPermissions() {
        val nearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
            )
        }
        launchPermissionRequest(nearby)
    }

    fun requestLocationPermissions() {
        launchPermissionRequest(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    fun requestEnableBluetooth() {
        try {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        } catch (e: Exception) {
            println("❌ requestEnableBluetooth failed: ${e.message}")
        }
    }

    private fun requestBackgroundLocationPermission() {
        // Background location permission is only needed on Android 10+ (API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!backgroundLocationGranted) {
                println("🔐 Requesting background location permission...")
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                println("✅ Background location already granted")
            }
        }
    }

    private fun registeBluetoothReceiver() {
        // Register the BroadcastReceiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)

        registerReceiver((getAppContext().ble() as AndroidBLE).getReceiver(), filter)
    }

    // App lifecycle methods for background/foreground handling
    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
        println("🟢 App moved to foreground")
        ThemeForegroundSync.onAppForeground()

        // Start BLE service when app comes to foreground if device is connected
        if (getAppContext().vanGO.isConnected() && !serviceBound) {
            startBleService()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        println("🔴 App moved to background")
        
        // Ensure BLE service is running when app goes to background
        if (getAppContext().vanGO.isConnected()) {
            startBleService()
        }
    }

    private fun startBleService() {
        val serviceIntent = Intent(this, VanGoBleService::class.java).apply {
            action = VanGoBleService.ACTION_START_SERVICE
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            println("🚀 BLE Service starting...")
        } catch (e: Exception) {
            println("❌ BLE Service start failed: ${e.message}")
        }
    }

    private fun stopBleService() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        
        val serviceIntent = Intent(this, VanGoBleService::class.java).apply {
            action = VanGoBleService.ACTION_STOP_SERVICE
        }
        startService(serviceIntent)
        
        println("🛑 BLE Service stopping...")
    }

    override fun onDestroy() {
        if (current === this) current = null
        super<ComponentActivity>.onDestroy()
        
        // Remove lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        
        // Only stop service if app is being destroyed and not just going to background
        if (isFinishing) {
            stopBleService()
        }
        
        println("🔥 MainActivity destroyed")
    }

    override fun onResume() {
        super<ComponentActivity>.onResume()
        println("▶️ MainActivity resumed")
    }

    override fun onPause() {
        super<ComponentActivity>.onPause()
        println("⏸️ MainActivity paused")
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}