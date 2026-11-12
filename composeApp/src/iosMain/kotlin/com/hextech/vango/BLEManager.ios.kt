package com.hextech.vango

import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val IOS_BLE_WRITE_UUID = "0000abf1-0000-1000-8000-00805f9b34fb"
private const val IOS_BLE_NOTIFY_UUID = "0000abf2-0000-1000-8000-00805f9b34fb"
private const val IOS_BLE_WEATHER_UUID = "0000abf3-0000-1000-8000-00805f9b34fb"

private val ioWriteCharacteristic = characteristicOf(serviceUUID, IOS_BLE_WRITE_UUID)
private val ioNotifyCharacteristic = characteristicOf(serviceUUID, IOS_BLE_NOTIFY_UUID)
private val weatherWriteCharacteristic = characteristicOf(serviceUUID, IOS_BLE_WEATHER_UUID)

private class IosBluetoothManager : BluetoothManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val advertisements = mutableMapOf<String, Advertisement>()
    private val discoveredIdentifiers = mutableListOf<String>()
    private val writeMutex = Mutex()

    private var scanJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private var connectionJob: Job? = null
    private var activePeripheral: Peripheral? = null
    private var activeIdentifier: String? = null
    private var pendingIdentifier: String? = null
    private var connected = false
    private var intentionalDisconnect = false

    override fun bluetoothSupport() = true

    // CoreBluetooth presents adapter availability through Kable when scanning/connecting.
    override fun bluetoothEnabled() = true

    override fun isConnected() = connected

    override fun connectedDeviceMac(): String? = activeIdentifier

    override fun connect(index: Int): Boolean {
        val identifier = discoveredIdentifiers.getOrNull(index) ?: return false
        return connectByMac(identifier)
    }

    override fun connectByMac(mac: String): Boolean {
        val identifier = mac.trim().uppercase()
        if (connected && activeIdentifier == identifier) return true
        if (connectionJob?.isActive == true) return activeIdentifier == identifier

        val advertisement = advertisements[identifier]
        if (advertisement != null) {
            connectToAdvertisement(identifier, advertisement)
            return true
        }

        pendingIdentifier = identifier
        if (scanJob?.isActive != true && !startDiscovering()) {
            pendingIdentifier = null
            return false
        }
        return true
    }

    override fun disconnect() {
        intentionalDisconnect = true
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        scanJob?.cancel()
        connectionJob?.cancel()
        val peripheral = activePeripheral
        scope.launch {
            try {
                peripheral?.disconnect()
            } catch (e: Exception) {
                println("iOS BLE disconnect failed: ${e.message}")
            } finally {
                connected = false
                activePeripheral = null
                activeIdentifier = null
                pendingIdentifier = null
                intentionalDisconnect = false
            }
        }
    }

    override fun startDiscovering(): Boolean {
        if (connected || scanJob?.isActive == true) return true

        advertisements.clear()
        discoveredIdentifiers.clear()
        scanJob = scope.launch {
            try {
                Scanner().advertisements.collect { advertisement ->
                    val name = advertisement.name
                        ?: advertisement.peripheralName
                        ?: return@collect
                    if (!isSupportedBleDeviceName(name)) return@collect

                    val identifier = identifierAsMac(advertisement)
                    advertisements[identifier] = advertisement
                    if (identifier !in discoveredIdentifiers) {
                        discoveredIdentifiers += identifier
                    }
                    onStatusOfBle(
                        VanGoState.OnDiscoverDevice,
                        BleDevice(name, identifier, advertisement.rssi),
                    )

                    if (pendingIdentifier == identifier) {
                        pendingIdentifier = null
                        connectToAdvertisement(identifier, advertisement)
                        return@collect
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("iOS BLE scan failed: ${e.message}")
            } finally {
                if (!connected && connectionJob?.isActive != true) {
                    onStatusOfBle(VanGoState.OnDiscoverDone, null)
                }
            }
        }
        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(60_000)
            scanJob?.cancel()
        }
        return true
    }

    override fun stopDiscovering() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        scanJob?.cancel()
        scanJob = null
    }

    override fun discoverServices(): Boolean {
        val peripheral = activePeripheral ?: return false
        if (!connected) return false

        scope.launch {
            val service = peripheral.services
                ?.firstOrNull { it.serviceUuid.toString().equals(serviceUUID, ignoreCase = true) }
            val hasNotify = service?.characteristics?.any {
                it.characteristicUuid.toString().equals(IOS_BLE_NOTIFY_UUID, ignoreCase = true)
            } == true
            val hasWrite = service?.characteristics?.any {
                it.characteristicUuid.toString().equals(IOS_BLE_WRITE_UUID, ignoreCase = true)
            } == true
            val hasWeather = service?.characteristics?.any {
                it.characteristicUuid.toString().equals(IOS_BLE_WEATHER_UUID, ignoreCase = true)
            } == true
            val valid = service != null && hasNotify && hasWrite && hasWeather
            if (!valid) {
                println(
                    "iOS BLE GATT mismatch: service=$serviceUUID notify=$hasNotify " +
                        "write=$hasWrite weather=$hasWeather",
                )
            }
            onStatusOfBle(VanGoState.OnServiceDiscovering, valid)
        }
        return true
    }

    override fun configureMtu(): Boolean {
        if (!connected) return false
        // CoreBluetooth negotiates the ATT MTU automatically on iOS.
        scope.launch {
            onStatusOfBle(VanGoState.OnMtuConfigured, 23)
        }
        return true
    }

    override fun enableReadChar(): Boolean {
        if (!connected) return false
        // Kable has already enabled ABF2 notifications as part of the active observer.
        scope.launch {
            onStatusOfBle(VanGoState.OnCharacteristicNotification, null)
        }
        return true
    }

    override fun sendData(data: String): Boolean {
        val peripheral = activePeripheral ?: return false
        if (!connected) return false

        scope.launch {
            val success = try {
                writeMutex.withLock {
                    val payload = (if (data.endsWith("\n")) data else "$data\n").encodeToByteArray()
                    peripheral.write(ioWriteCharacteristic, payload, WriteType.WithResponse)
                }
                true
            } catch (e: Exception) {
                println("iOS BLE data write failed: ${e.message}")
                false
            }
            onStatusOfBle(VanGoState.OnCharacteristicWrite, success)
        }
        return true
    }

    override suspend fun sendWeatherData(data: String): Boolean {
        val peripheral = activePeripheral ?: return false
        if (!connected) return false

        val success = try {
            val asciiData = data.toIosAsciiSafe().encodeToByteArray()
            writeMutex.withLock {
                peripheral.write(weatherWriteCharacteristic, asciiData, WriteType.WithResponse)
            }
            true
        } catch (e: Exception) {
            println("iOS BLE weather write failed: ${e.message}")
            false
        }
        onStatusOfBle(VanGoState.OnCharacteristicWrite, success)
        return success
    }

    private fun connectToAdvertisement(identifier: String, advertisement: Advertisement) {
        if (connectionJob?.isActive == true) return
        stopDiscovering()
        intentionalDisconnect = false

        val peripheral = scope.peripheral(advertisement)
        activePeripheral = peripheral
        activeIdentifier = identifier
        connectionJob = scope.launch {
            val disconnected = CompletableDeferred<Unit>()
            val stateJob = launch {
                var wasConnected = false
                peripheral.state.collect { state ->
                    when (state) {
                        State.Connected -> wasConnected = true
                        is State.Disconnected -> {
                            if (wasConnected) {
                                connected = false
                                if (!intentionalDisconnect) {
                                    onStatusOfBle(VanGoState.OnGatServerConnection, false)
                                }
                                disconnected.complete(Unit)
                            }
                        }
                        else -> Unit
                    }
                }
            }
            val notificationJob = launch {
                try {
                    peripheral.observe(ioNotifyCharacteristic).collect { value ->
                        onStatusOfBle(
                            VanGoState.OnCharacteristicNotification,
                            value.decodeToString(),
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("iOS BLE notification stream failed: ${e.message}")
                }
            }

            try {
                peripheral.connect()
                connected = true
                onStatusOfBle(VanGoState.OnGatServerConnection, true)
                disconnected.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("iOS BLE connection failed: ${e.message}")
                connected = false
                activePeripheral = null
                activeIdentifier = null
                onStatusOfBle(VanGoState.OnGatServerConnection, false)
            } finally {
                stateJob.cancel()
                notificationJob.cancel()
                if (!connected) {
                    activePeripheral = null
                    activeIdentifier = null
                }
            }
        }
    }

    private fun identifierAsMac(advertisement: Advertisement): String {
        // CoreBluetooth hides hardware MAC addresses; shared device storage expects a MAC-shaped key.
        val hex = advertisement.identifier.toString()
            .uppercase()
            .filter { it in '0'..'9' || it in 'A'..'F' }
        require(hex.length >= 12) { "Invalid CoreBluetooth peripheral identifier" }
        return hex.takeLast(12).chunked(2).joinToString(":")
    }
}

private fun String.toIosAsciiSafe(): String {
    val replacements = mapOf(
        'Ç' to 'C', 'ç' to 'c', 'Ğ' to 'G', 'ğ' to 'g',
        'İ' to 'I', 'ı' to 'i', 'Ö' to 'O', 'ö' to 'o',
        'Ş' to 'S', 'ş' to 's', 'Ü' to 'U', 'ü' to 'u',
        '°' to ' ',
    )
    return map { char -> replacements[char] ?: if (char.code < 128) char else '?' }.joinToString("")
}

internal fun createIosBluetoothManager(): BluetoothManager = IosBluetoothManager()
