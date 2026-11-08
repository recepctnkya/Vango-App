package com.hextech.vango

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID
import java.nio.charset.Charset
import android.Manifest
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Helper functions for Bluetooth permission checking
 */

/**
 * Checks if the app has BLUETOOTH_CONNECT permission (Android 12+) or legacy BLUETOOTH permission
 * @return true if permission is granted, false otherwise
 */
@Volatile
private var cachedBleConnectPermission: Boolean? = null

fun hasBluetoothConnectPermission(): Boolean {
    cachedBleConnectPermission?.let { return it }
    val context = getAppContext().get() as Context
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
    }
    if (hasPermission) cachedBleConnectPermission = true
    return hasPermission
}

/**
 * Checks if the app has BLUETOOTH_SCAN permission (Android 12+) or legacy BLUETOOTH_ADMIN permission
 * @return true if permission is granted, false otherwise
 */
fun hasBluetoothScanPermission(): Boolean {
    val context = getAppContext().get() as Context
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Checks if the app has BLUETOOTH_ADMIN permission for administrative operations
 * @return true if permission is granted, false otherwise
 */
fun hasBluetoothAdminPermission(): Boolean {
    val context = getAppContext().get() as Context
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Converts a string to ASCII-safe format by replacing special characters
 * Handles Turkish and common European special characters
 */
fun String.toAsciiSafe(): String {
    val replacements = mapOf(
        'Ç' to 'C', 'ç' to 'c',
        'Ğ' to 'G', 'ğ' to 'g',
        'İ' to 'I', 'ı' to 'i',
        'Ö' to 'O', 'ö' to 'o',
        'Ş' to 'S', 'ş' to 's',
        'Ü' to 'U', 'ü' to 'u',
        'Ä' to 'A', 'ä' to 'a',
        'Á' to 'A', 'á' to 'a',
        'À' to 'A', 'à' to 'a',
        'Â' to 'A', 'â' to 'a',
        'É' to 'E', 'é' to 'e',
        'È' to 'E', 'è' to 'e',
        'Ê' to 'E', 'ê' to 'e',
        'Ë' to 'E', 'ë' to 'e',
        'Í' to 'I', 'í' to 'i',
        'Ì' to 'I', 'ì' to 'i',
        'Î' to 'I', 'î' to 'i',
        'Ï' to 'I', 'ï' to 'i',
        'Ó' to 'O', 'ó' to 'o',
        'Ò' to 'O', 'ò' to 'o',
        'Ô' to 'O', 'ô' to 'o',
        'Ú' to 'U', 'ú' to 'u',
        'Ù' to 'U', 'ù' to 'u',
        'Û' to 'U', 'û' to 'u',
        'Ñ' to 'N', 'ñ' to 'n',
        'ß' to 's',
        '°' to ' '  // degree symbol to space
    )

    return this.map { char ->
        replacements[char] ?: if (char.code in 0..127) char else '?'
    }.joinToString("")
}

/**
 * Android implementation of the BluetoothManager interface
 * Handles all Bluetooth Low Energy (BLE) operations for VanGO devices
 * 
 * Features:
 * - Device discovery with proper permission handling
 * - Thread-safe device management
 * - GATT connection and service discovery
 * - MTU configuration for optimal data transfer
 * - Characteristic notification setup
 * - Data transmission with debug logging
 * - Comprehensive error handling and logging
 * 
 * @param adapter The BluetoothAdapter instance, must not be null
 */
class AndroidBLE(adapter: BluetoothAdapter?): BluetoothManager, BluetoothGattCallback() {
    private val bluetoothAdapter: BluetoothAdapter = requireNotNull(adapter){"Argument cant be null"}
    private var bleGatt:BluetoothGatt? = null
    @Volatile private var gattLinkLive: Boolean = false
    private var vanGoBleDevice: BluetoothDevice? = null
    private var mtuValue = 20
    private var discoveredDevices: ArrayList<BluetoothDevice> = java.util.ArrayList()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var discoverDoneRunnable: Runnable? = null
    private var leScanEndAtMs: Long = 0L
    /** IO modülü BLE ~45s gecikmeli açılıyor; yeterli keşif süresi gerekir. */
    private val leScanDurationMs = 60_000L
    private val pendingConnectMacs: ArrayDeque<String> = ArrayDeque()
    private var intentionalDisconnect = false

    /** VanGO GATT servis UUID — IO modül reklamında isim olmasa bile tanınır. */
    private val vanGoServiceParcelUuid = ParcelUuid.fromString(bleVanGoServiceUuid)

    /** BLE advertisements (many peripherals never appear in classic [BluetoothAdapter.startDiscovery]). */
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleLeScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleLeScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            println("❌ BLE LE scan failed: errorCode=$errorCode")
        }
    }

    private fun handleLeScanResult(result: ScanResult) {
        if (!hasBluetoothScanPermission()) return
        val device = result.device ?: return
        val nameFromDevice = if (hasBluetoothConnectPermission()) {
            try {
                device.name
            } catch (_: SecurityException) {
                null
            }
        } else {
            null
        }
        val nameFromAdv = result.scanRecord?.deviceName
        val resolvedName = nameFromDevice ?: nameFromAdv
        when {
            resolvedName != null && isSupportedBleDeviceName(resolvedName) ->
                tryAddDiscoveredDevice(device, resolvedName, result.rssi)
            hasVanGoServiceInAdvertisement(result.scanRecord) ->
                tryAddDiscoveredDevice(device, bleIoModuleName, result.rssi)
        }
    }

    private fun hasVanGoServiceInAdvertisement(record: ScanRecord?): Boolean {
        if (record == null) return false
        val uuids = record.serviceUuids ?: return false
        return uuids.any { it.uuid == vanGoServiceParcelUuid.uuid }
    }

    // Deferred for awaiting weather data write completion
    private var weatherWriteDeferred: CompletableDeferred<Boolean>? = null
    private val abf1WriteQueue: ArrayDeque<ByteArray> = ArrayDeque()
    private var abf1WriteInFlight = false
    private var abf1AwaitingFinish = false
    private var cachedAbf1WriteChar: BluetoothGattCharacteristic? = null
    private var abf1RediscoverAttempted = false
    private val abf1WriteUuid: UUID = UUID.fromString("0000abf1-0000-1000-8000-00805f9b34fb")
    private val abf1Watchdog = Runnable {
        println("   └─ ABF1 write watchdog — draining stuck queue")
        finishAbf1WriteLocked("watchdog")
    }

    private fun resolveAbf1WriteCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        cachedAbf1WriteChar?.let { cached ->
            if (cached.uuid == abf1WriteUuid) return cached
        }
        val direct = gatt.getService(UUID.fromString(serviceUUID))?.getCharacteristic(abf1WriteUuid)
        if (direct != null) {
            cachedAbf1WriteChar = direct
            return direct
        }
        for (service in gatt.services.orEmpty()) {
            val found = service.getCharacteristic(abf1WriteUuid)
            if (found != null) {
                println("   └─ ABF1 found under service ${service.uuid}")
                cachedAbf1WriteChar = found
                return found
            }
        }
        println(
            "   └─ ABF1 missing (services=${gatt.services.size} chars=" +
                gatt.services.orEmpty().flatMap { it.characteristics.orEmpty() }.joinToString { it.uuid.toString() } +
                ")",
        )
        return null
    }

    /**
     * Accept only IO module names (DISPLAY is excluded from BLE scan/connect).
     */
    private fun tryAddDiscoveredDevice(device: BluetoothDevice, name: String, rssi: Int) {
        val n = name.trim()
        if (!isSupportedBleDeviceName(n)) return
        synchronized(discoveredDevices) {
            if (discoveredDevices.any { it.address == device.address }) return
            discoveredDevices.add(device)
            onStatusOfBle(VanGoState.OnDiscoverDevice, BleDevice(n, device.address, rssi))
            println("📡 Discovered VanGO IO: $n (${device.address})")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleLeScan() {
        if (!hasBluetoothScanPermission()) return
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            println("❌ bluetoothLeScanner is null (Bluetooth off?)")
            return
        }
        stopBleLeScan()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            // Donanım filtresi bazı cihazlarda IO reklamını kaçırıyor; yazılımda isSupportedBleDeviceName ile süzüyoruz
            scanner.startScan(null, settings, leScanCallback)
            println("✅ BLE LE scan started (software IO filter)")
        } catch (e: Exception) {
            println("❌ BLE LE scan start failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleLeScan() {
        if (!hasBluetoothScanPermission()) return
        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(leScanCallback)
        } catch (e: Exception) {
            println("stopBleLeScan: ${e.message}")
        }
    }

    private fun cancelDiscoverDone() {
        discoverDoneRunnable?.let { mainHandler.removeCallbacks(it) }
        discoverDoneRunnable = null
    }

    private fun finishDiscovering() {
        cancelDiscoverDone()
        stopBleLeScan()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        onStatusOfBle(VanGoState.OnDiscoverDone, null)
    }

    private fun scheduleDiscoverDone() {
        cancelDiscoverDone()
        val delayMs = (leScanEndAtMs - System.currentTimeMillis()).coerceAtLeast(500L)
        discoverDoneRunnable = Runnable { finishDiscovering() }
        mainHandler.postDelayed(discoverDoneRunnable!!, delayMs)
    }

    // todo: bunun calismasi icin izin lazim izinleri eklemeyi unutma!
    // todo: konum acik olmali yoksa discover calismiyor!
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (!hasBluetoothScanPermission()) {
                        println("Missing BLUETOOTH_SCAN permission for device discovery")
                        return
                    }

                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val nameFromDevice = if (hasBluetoothConnectPermission()) {
                            try {
                                it.name
                            } catch (_: SecurityException) {
                                null
                            }
                        } else {
                            null
                        }
                        val name = nameFromDevice?.trim()?.takeIf { n -> n.isNotEmpty() }
                        if (name != null) {
                            tryAddDiscoveredDevice(it, name, -100)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Klasik tarama bitti; BLE LE taraması minimum süre dolana kadar devam etsin
                    scheduleDiscoverDone()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            onStatusOfBle(VanGoState.BluetoothNotEnabled, "Bluetooth turned off")
                        }
//                        BluetoothAdapter.STATE_ON -> {
//                            onStatusOfBle(VanGoState.OnDiscoverDone, "Bluetooth turned on")
//                        }
                    }
                }
            }
        }
    }
    fun getReceiver(): BroadcastReceiver { return receiver }

    override fun bluetoothSupport(): Boolean {
        return true
    }

    override fun bluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    override fun discoverServices():Boolean {
        if (!hasBluetoothConnectPermission()) {
            println("Missing BLUETOOTH_CONNECT permission for service discovery")
            return false
        }
        return (bleGatt?.discoverServices() != null)
    }

    override fun isConnected(): Boolean {
        return gattLinkLive && bleGatt != null
    }

    @SuppressLint("MissingPermission")
    override fun connectedDeviceMac(): String? {
        if (!hasBluetoothConnectPermission()) return null
        if (bleGatt == null) return null
        val d = vanGoBleDevice ?: return null
        return try {
            d.address?.trim()?.takeIf { it.length >= 12 }?.let(::normalizeBluetoothAddressToColons)
        } catch (_: SecurityException) {
            null
        }
    }

    override fun connect(index: Int): Boolean {
        if (!hasBluetoothConnectPermission()) {
            println("Missing BLUETOOTH_CONNECT permission for connection")
            onStatusOfBle(VanGoState.PermissionDenied, "Bluetooth connect permission denied")
            return false
        }
        
        // Thread-safe access to discoveredDevices with proper bounds checking
        synchronized(discoveredDevices) {
            if (index >= 0 && index < discoveredDevices.size) {
                return connectBluetoothDevice(discoveredDevices[index])
            } else {
                println("Invalid device index: $index, list size: ${discoveredDevices.size}")
                return false
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun connectByMac(mac: String): Boolean {
        if (!hasBluetoothConnectPermission()) {
            onStatusOfBle(VanGoState.PermissionDenied, "Bluetooth connect permission denied")
            return false
        }
        val normalized = normalizeMacForDisplay(mac)
        if (!isValidMacFormat(normalized)) {
            println("connectByMac: invalid MAC $mac")
            return false
        }
        cancelDiscoverDone()
        stopBleLeScan()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        // Platform zaten BLE MAC'e çözümledi — burada tekrar WiFi→BLE (+2) UYGULAMA
        pendingConnectMacs.clear()
        pendingConnectMacs.add(normalized)
        intentionalDisconnect = false
        println("connectByMac candidates for $normalized: $pendingConnectMacs")
        return connectNextMacCandidate()
    }

    @SuppressLint("MissingPermission")
    private fun connectNextMacCandidate(): Boolean {
        val targetMac = pendingConnectMacs.removeFirstOrNull() ?: return false
        synchronized(discoveredDevices) {
            val existingIdx = discoveredDevices.indexOfFirst { device ->
                macsEqual(normalizeBluetoothAddressToColons(device.address), targetMac)
            }
            if (existingIdx >= 0) {
                return connectBluetoothDevice(discoveredDevices[existingIdx])
            }
        }
        return try {
            val remote = bluetoothAdapter.getRemoteDevice(targetMac)
            synchronized(discoveredDevices) {
                if (discoveredDevices.none { it.address.equals(remote.address, ignoreCase = true) }) {
                    discoveredDevices.add(remote)
                }
            }
            connectBluetoothDevice(remote)
        } catch (e: Exception) {
            println("connectNextMacCandidate failed for $targetMac: ${e.message}")
            connectNextMacCandidate()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetoothDevice(device: BluetoothDevice): Boolean {
        return try {
            // Eski GATT'ı kapat; disconnect callback'i stale sayılacak (bleGatt !== gatt)
            val previous = bleGatt
            bleGatt = null
            gattLinkLive = false
            if (previous != null) {
                try {
                    previous.disconnect()
                } catch (_: Exception) {
                }
                try {
                    previous.close()
                } catch (_: Exception) {
                }
            }
            vanGoBleDevice = device
            intentionalDisconnect = false
            val context = getAppContext().get() as Context
            bleGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, this)
            }
            bleGatt != null
        } catch (e: Exception) {
            println("Error connecting to device: ${e.message}")
            false
        }
    }

    override fun startDiscovering(): Boolean {
        if (!hasBluetoothScanPermission()) {
            println("Missing BLUETOOTH_SCAN permission for discovery")
            onStatusOfBle(VanGoState.PermissionDenied, "Bluetooth scan permission denied")
            return false
        }

        val locationPerms = hasLocationPermissions()
        val locationOn = isLocationServicesEnabled()
        val needsLocation = bleScanRequiresLocationEnabled()

        if (needsLocation) {
            if (!locationPerms) {
                println("Missing location permission for BLE discovery")
                onStatusOfBle(VanGoState.PermissionDenied, "Konum izni BLE taraması için gerekli")
                return false
            }
            if (!locationOn) {
                println("Location services disabled — BLE scan will not find devices")
                onStatusOfBle(VanGoState.PermissionDenied, "Konum servisi (GPS) kapalı — BLE taraması için açın")
                return false
            }
        } else if (!locationOn) {
            // Android 12+: LE tarama neverForLocation ile GPS olmadan da çalışır
            println("⚠️ Location off — continuing with LE-only scan (classic discovery skipped)")
        }

        if (isConnected()) {
            println("startDiscovering skipped: BLE link already active")
            return true
        }
        
        cancelDiscoverDone()
        mtuValue = 20
        
        // Clear the discovered devices list to allow rediscovery
        synchronized(discoveredDevices) {
            discoveredDevices.clear()
            println("🔄 Cleared discoveredDevices list for new discovery")
        }
        
        if(bluetoothAdapter.isEnabled) {
            leScanEndAtMs = System.currentTimeMillis() + leScanDurationMs
            startBleLeScan()
            // Classic discovery çoğu OEM'de konum ister; LE yeterli olduğunda atlanabilir
            if (locationOn && locationPerms) {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                    Thread.sleep(200)
                }
                bluetoothAdapter.startDiscovery()
            }
            scheduleDiscoverDone()
            return true
        } else {
            return false
        }
    }

    override fun stopDiscovering() {
        if (!hasBluetoothScanPermission()) {
            println("Missing BLUETOOTH_SCAN permission for stopping discovery")
            return
        }

        cancelDiscoverDone()
        stopBleLeScan()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
    }

    override fun disconnect() {
        if (!hasBluetoothConnectPermission()) {
            println("Missing BLUETOOTH_CONNECT permission for disconnect")
            return
        }

        intentionalDisconnect = true
        pendingConnectMacs.clear()
        val gatt = bleGatt
        if (gatt != null) {
            println("Disconnecting device (close deferred to STATE_DISCONNECTED)")
            try {
                gatt.disconnect()
            } catch (e: Exception) {
                println("disconnect() error: ${e.message}")
                try {
                    gatt.close()
                } catch (_: Exception) {
                }
                bleGatt = null
                gattLinkLive = false
                vanGoBleDevice = null
            }
        } else {
            gattLinkLive = false
            vanGoBleDevice = null
        }
    }

    override fun configureMtu(): Boolean {
        if (!hasBluetoothConnectPermission()) {
            println("Missing BLUETOOTH_CONNECT permission for MTU configuration")
            return false
        }
        
        bleGatt?.let {
            return bleGatt?.requestMtu(512) != null
        }

        return false
    }

    private var pendingNotifySetup = false

    override fun enableReadChar():Boolean {
        if (!hasBluetoothConnectPermission()) {
            println("Missing BLUETOOTH_CONNECT permission for enabling characteristic read")
            return false
        }
        
        val gatt = bleGatt ?: return false
        if(gatt.services.isEmpty()) return false

        val characteristic = gatt.getService(UUID.fromString(serviceUUID))
            ?.getCharacteristic(UUID.fromString(characteristicUUID)) ?: return false

        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            return false
        }
        val descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUUID)) ?: return false
        pendingNotifySetup = true
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, descriptor.value) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            (gatt.writeDescriptor(descriptor) == true)
        }
    }

    override fun sendData(data: String): Boolean {
        if (!hasBluetoothConnectPermission()) {
            println("Missing BLUETOOTH_CONNECT permission for sending data")
            onStatusOfBle(VanGoState.PermissionDenied, "Bluetooth connect permission denied")
            return false
        }
        val gatt = bleGatt ?: run {
            println("sendData: bleGatt is null")
            return false
        }

        if (data.contains("\"MessageType\":\"Write\"")) {
            BleDebugger.logDataSend(data)
        } else if (data.contains("\"MessageType\":") && (data.contains("numOfO") || data.contains("outNB"))) {
            BleDebugger.logConfigSend(data)
        } else {
            BleDebugger.logDataSend(data)
        }

        val json = if (data.endsWith("\n")) data else "$data\n"
        val payload = json.toByteArray(Charsets.UTF_8)
        /* ATT max = MTU-3; tek write > MTU Android'de düşer — Configuration ~3KB chunk gerekir */
        val chunkMax = mtuValue.coerceIn(20, 509)
        val chunks = ArrayList<ByteArray>((payload.size / chunkMax) + 1)
        var off = 0
        while (off < payload.size) {
            val end = minOf(off + chunkMax, payload.size)
            chunks.add(payload.copyOfRange(off, end))
            off = end
        }
        val gattForPump: BluetoothGatt
        synchronized(abf1WriteQueue) {
            while (abf1WriteQueue.size + chunks.size > 64) {
                abf1WriteQueue.removeFirst()
            }
            for (c in chunks) {
                abf1WriteQueue.addLast(c)
            }
            println(
                "   └─ ABF1 queue size=${abf1WriteQueue.size} inFlight=$abf1WriteInFlight " +
                    "(${payload.size}B → ${chunks.size} chunk ×≤${chunkMax}B mtuPayload=$mtuValue)",
            )
            if (abf1WriteInFlight) return true
            abf1WriteInFlight = true
            gattForPump = gatt
        }
        return pumpAbf1WriteQueue(gattForPump)
    }

    @SuppressLint("MissingPermission")
    private fun pumpAbf1WriteQueue(gatt: BluetoothGatt): Boolean {
        val payload = synchronized(abf1WriteQueue) {
            abf1WriteQueue.firstOrNull()
        } ?: run {
            synchronized(abf1WriteQueue) { abf1WriteInFlight = false }
            return true
        }
        val characteristic = resolveAbf1WriteCharacteristic(gatt)
            ?: run {
                println("   └─ ❌ Characteristic ABF1 not found")
                val shouldRediscover = !abf1RediscoverAttempted
                if (shouldRediscover) {
                    abf1RediscoverAttempted = true
                    println("   └─ Rediscovering GATT services for ABF1")
                    synchronized(abf1WriteQueue) { abf1WriteInFlight = false }
                    gatt.discoverServices()
                    return true
                }
                synchronized(abf1WriteQueue) {
                    abf1WriteQueue.clear()
                    abf1WriteInFlight = false
                }
                return false
            }

        val props = characteristic.properties
        val queuedAhead = synchronized(abf1WriteQueue) { abf1WriteQueue.size }
        /* Çok parçalı Configuration: NO_RESPONSE ATT long-write yapmaz — DEFAULT zorunlu */
        val writeType = when {
            queuedAhead > 1 && props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, payload, writeType) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            characteristic.value = payload
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic) == true
        }
        if (success) {
            println("   └─ Write Result: ✅ Initiated (${payload.size}B) type=$writeType props=$props q=$queuedAhead")
            synchronized(abf1WriteQueue) { abf1AwaitingFinish = true }
            mainHandler.removeCallbacks(abf1Watchdog)
            val timeoutMs = if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) 80L else 1200L
            mainHandler.postDelayed(abf1Watchdog, timeoutMs)
            return true
        }
        println("   └─ Write Result: ❌ Failed — will retry shortly")
        mainHandler.postDelayed({
            val g = bleGatt ?: run {
                synchronized(abf1WriteQueue) {
                    abf1WriteQueue.clear()
                    abf1WriteInFlight = false
                    abf1AwaitingFinish = false
                }
                return@postDelayed
            }
            pumpAbf1WriteQueue(g)
        }, 120L)
        return true
    }

    private fun finishAbf1WriteLocked(reason: String) {
        val shouldPump: BluetoothGatt?
        synchronized(abf1WriteQueue) {
            if (!abf1AwaitingFinish) return
            abf1AwaitingFinish = false
            mainHandler.removeCallbacks(abf1Watchdog)
            if (abf1WriteQueue.isNotEmpty()) {
                abf1WriteQueue.removeFirst()
            }
            val gatt = bleGatt
            if (abf1WriteQueue.isEmpty() || gatt == null) {
                abf1WriteInFlight = false
                shouldPump = null
            } else {
                shouldPump = gatt
            }
        }
        if (shouldPump != null) {
            mainHandler.post { pumpAbf1WriteQueue(shouldPump) }
        }
    }

    private fun onAbf1WriteFinished() {
        finishAbf1WriteLocked("callback")
    }

    override suspend fun sendWeatherData(data: String): Boolean {
        println("📤 sendWeatherData: Starting...")

        if (!hasBluetoothConnectPermission()) {
            println("❌ sendWeatherData: Missing BLUETOOTH_CONNECT permission")
            onStatusOfBle(VanGoState.PermissionDenied, "Bluetooth connect permission denied")
            return false
        }
        println("✅ sendWeatherData: Bluetooth permission granted")

        if (bleGatt == null) {
            println("❌ sendWeatherData: bleGatt is null - not connected to device")
            return false
        }
        println("✅ sendWeatherData: bleGatt is valid")

        // Use weather-specific debug logging
        BleDebugger.logWeatherSend(data)

        val characteristic = bleGatt?.getService(UUID.fromString(serviceUUID))?.
        getCharacteristic(UUID.fromString("0000abf3-0000-1000-8000-00805f9b34fb"))

        characteristic?.let {
            // Create a deferred to wait for the write callback
            weatherWriteDeferred = CompletableDeferred()

            // Convert to ASCII-safe string by replacing special characters
            val asciiData = data.toAsciiSafe()
            println("   └─ ASCII data: $asciiData")
            characteristic.value = asciiData.toByteArray(Charsets.US_ASCII)
            val writeInitiated = bleGatt?.writeCharacteristic(characteristic) == true

            if (!writeInitiated) {
                println("   └─ Write Result: ❌ Failed to initiate")
                weatherWriteDeferred?.complete(false)
                weatherWriteDeferred = null
                return false
            }

            println("   └─ Write initiated, waiting for callback...")

            // Wait for the callback with a 5 second timeout
            val result = withTimeoutOrNull(5000L) {
                weatherWriteDeferred?.await() ?: false
            } ?: run {
                println("   └─ Write Result: ❌ Timeout waiting for callback")
                false
            }

            weatherWriteDeferred = null

            if (result) {
                println("   └─ Write Result: ✅ Confirmed by device")
            } else {
                println("   └─ Write Result: ❌ Failed or timeout")
            }
            return result
        } ?: run {
            println("   └─ ❌ Characteristic not found")
            return false
        }
    }

    @Override
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        println("onConnectionStateChange status=$status newState=$newState gatt=${gatt.device?.address} current=${bleGatt?.device?.address}")
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // Eski/stale bağlantı callback'ini yoksay
            if (bleGatt != null && bleGatt !== gatt) {
                println("Ignoring stale CONNECTED from non-current gatt")
                try {
                    gatt.close()
                } catch (_: Exception) {
                }
                return
            }
            pendingConnectMacs.clear()
            bleGatt = gatt
            gattLinkLive = true
            vanGoBleDevice = gatt.device
            intentionalDisconnect = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            }
            println("✅ BLE Connection established to ${gatt.device.address}")
            onStatusOfBle(VanGoState.OnGatServerConnection, true)
            super.onConnectionStateChange(gatt, status, newState)
            return
        }

        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // Yeniden bağlanırken kapatılan eski GATT — UI'ya "bağlantı yok" gönderme
            if (bleGatt !== gatt) {
                println("Ignoring stale DISCONNECTED from old gatt ${gatt.device?.address}")
                if (hasBluetoothConnectPermission()) {
                    try {
                        gatt.close()
                    } catch (_: Exception) {
                    }
                }
                return
            }

            println("❌ BLE Connection lost - status: $status intentional=$intentionalDisconnect")
            if (hasBluetoothConnectPermission()) {
                try {
                    gatt.close()
                } catch (e: Exception) {
                    println("gatt.close() error: ${e.message}")
                }
            }
            bleGatt = null
            gattLinkLive = false
            vanGoBleDevice = null
            cachedAbf1WriteChar = null
            abf1RediscoverAttempted = false

            synchronized(abf1WriteQueue) {
                abf1WriteQueue.clear()
                abf1WriteInFlight = false
                abf1AwaitingFinish = false
            }
            mainHandler.removeCallbacks(abf1Watchdog)

            if (!intentionalDisconnect && status != BluetoothGatt.GATT_SUCCESS && pendingConnectMacs.isNotEmpty()) {
                println("🔄 Retrying BLE connect with alternate MAC, remaining=$pendingConnectMacs")
                if (connectNextMacCandidate()) {
                    // Yeni deneme başladı — failed olarak yayınlama
                    super.onConnectionStateChange(gatt, status, newState)
                    return
                }
            }
            pendingConnectMacs.clear()

            if (intentionalDisconnect) {
                // Kullanıcı/mod kasıtlı kesti — state'i failed olarak işaretleme
                onStatusOfBle(VanGoState.OnGatServerConnection, false)
                super.onConnectionStateChange(gatt, status, newState)
                return
            }

            onStatusOfBle(VanGoState.OnGatServerConnection, false)
            super.onConnectionStateChange(gatt, status, newState)
            return
        }

        super.onConnectionStateChange(gatt, status, newState)
    }

    // Handle service discovery
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        println("onConnectionStateChange is !!!!!!!!!!!!!!!!!!!!!!!!!!!!!! $status")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val services: List<BluetoothGattService>  = gatt.services

            services.forEach{ service ->
                println("Service uuid --- -- -- - " + service.uuid)
                service.characteristics?.forEach { characteristic ->
                    println("caharcateristic ${characteristic.uuid}")
                }
            }
            cachedAbf1WriteChar = null
            resolveAbf1WriteCharacteristic(gatt)
            if (abf1WriteQueue.isNotEmpty()) {
                mainHandler.post { bleGatt?.let { pumpAbf1WriteQueue(it) } }
            }
        }
        onStatusOfBle(VanGoState.OnServiceDiscovering, status == BluetoothGatt.GATT_SUCCESS)
        super.onServicesDiscovered(gatt, status)
    }

    @Override
    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        println("onMtuChanged: status=$status MTU=$mtu")
        if (status == BluetoothGatt.GATT_SUCCESS && mtu >= 23) {
            /* mtuValue = ATT payload max (MTU - 3) */
            mtuValue = (mtu - 3).coerceIn(20, 509)
            println("onMtuChanged: attPayloadMax=$mtuValue")
            onStatusOfBle(VanGoState.OnMtuConfigured, mtu)
        } else {
            onStatusOfBle(VanGoState.OnMtuConfigured, 0)
        }
        super.onMtuChanged(gatt, mtu, status)
    }

    @Override
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
        println("read:   Characteristic: ${characteristic.uuid} status=$status len=${value.size}")
        if (status != BluetoothGatt.GATT_SUCCESS || value.isEmpty()) return
        if (characteristic.uuid.toString().equals(characteristicUUID, ignoreCase = true)) {
            val stringData = String(value, Charsets.UTF_8)
            BleDebugger.logIncomingRawData(stringData, "VanGO Device (read)")
            onStatusOfBle(VanGoState.OnCharacteristicNotification, stringData)
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)

        println("descWrite: status=$status Characteristic: ${descriptor?.uuid}")
        if (!pendingNotifySetup) return
        pendingNotifySetup = false
        if (status == BluetoothGatt.GATT_SUCCESS &&
            descriptor?.uuid.toString().equals(descriptorUUID, ignoreCase = true)
        ) {
            onStatusOfBle(VanGoState.OnCharacteristicNotification, null)
        } else {
            println("CCCD write failed ($status) — retrying notification setup")
            mainHandler.postDelayed({
                if (bleGatt != null && isConnected()) {
                    enableReadChar()
                }
            }, 400L)
        }
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray
    ) {
        super.onDescriptorRead(gatt, descriptor, status, value)
        println("desc: ???? ??  Characteristic: ${descriptor.uuid}")
    }

    // Override the non-deprecated onCharacteristicChanged function (API level 33 and above)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val stringData = String(value, Charsets.UTF_8)
        Log.i("VanGO_BLE", "notify ${value.size}B: ${stringData.take(320)}")
        println("📡 BLE notify ${value.size}B: ${stringData.take(120)}")
        
        onStatusOfBle(VanGoState.OnCharacteristicNotification, stringData)
        // Process the received characteristic value (ByteArray
    }

    //todo: apk ya gore ya bu kuillanilacak ya yenisi, bunu hallet
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        characteristic?.let { char ->
            char.value?.let { value ->
                val stringData = String(value, Charsets.UTF_8)
                
                // Use incoming debug logging
                BleDebugger.logIncomingRawData(stringData, "VanGO Device (Legacy)")
                
                onStatusOfBle(VanGoState.OnCharacteristicNotification, stringData)
            } ?: run {
                println("📡 BLE Characteristic Changed (Legacy) - No data received")
                onStatusOfBle(VanGoState.OnCharacteristicNotification, null)
            }
        } ?: run {
            println("📡 BLE Characteristic Changed (Legacy) - Null characteristic")
            onStatusOfBle(VanGoState.OnCharacteristicNotification, null)
        }
        super.onCharacteristicChanged(gatt, characteristic)
    }

    /**
     * Handles characteristic write operation completion
     * Called when a write operation to a BLE characteristic completes
     *
     * @param gatt The GATT client
     * @param characteristic The characteristic that was written to
     * @param status The result of the write operation
     */
    @Override
    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        val success = status == BluetoothGatt.GATT_SUCCESS
        val uuid = characteristic.uuid.toString()
        if (success) {
            println("JSON data sent successfully! char=$uuid")
        } else {
            println("Failed to send JSON data. char=$uuid status=$status")
        }

        weatherWriteDeferred?.complete(success)

        if (uuid.equals("0000abf1-0000-1000-8000-00805f9b34fb", ignoreCase = true)) {
            onAbf1WriteFinished()
        }

        onStatusOfBle(VanGoState.OnCharacteristicWrite, success)
        super.onCharacteristicWrite(gatt, characteristic, status)
    }
}

/** BLE adresini AA:BB:CC:DD:EE:FF biçimine çevirir. */
private fun normalizeBluetoothAddressToColons(raw: String): String {
    val hex = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(12)
    if (hex.length != 12) return raw.trim()
    return hex.chunked(2).joinToString(":")
}
