package com.hextech.vango

import com.hextech.vango.mqtt.mqttTelemetryManagerRef
import com.hextech.vango.profile.HexnetCommandArgs
import com.hextech.vango.profile.HexnetDeviceCommandApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.ManualPairedDeviceStore
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import com.hextech.vango.profile.toBleDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.DrawableResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.ac
import testvango.composeapp.generated.resources.bedroom
import testvango.composeapp.generated.resources.corridor
import testvango.composeapp.generated.resources.ex_light
import testvango.composeapp.generated.resources.ex_outlet
import testvango.composeapp.generated.resources.heater
import testvango.composeapp.generated.resources.hexnet_mark
import testvango.composeapp.generated.resources.kitchen
import testvango.composeapp.generated.resources.lamp
import testvango.composeapp.generated.resources.outlet
import testvango.composeapp.generated.resources.oven
import testvango.composeapp.generated.resources.reading_l
import testvango.composeapp.generated.resources.refrigerator
import testvango.composeapp.generated.resources.spot
import testvango.composeapp.generated.resources.step
import testvango.composeapp.generated.resources.toilet
import testvango.composeapp.generated.resources.tv
import testvango.composeapp.generated.resources.usb
import testvango.composeapp.generated.resources.water
import testvango.composeapp.generated.resources.water_p
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.InternalSerializationApi

// Custom serializer for ByteArray to handle unsigned byte values (0-255)
// Serializes ByteArray as a JSON array of integers (0-255)
@OptIn(InternalSerializationApi::class)
object UnsignedByteArraySerializer : KSerializer<ByteArray> {
    // Use ListSerializer to handle the actual encoding/decoding
    private val intListSerializer = ListSerializer(serializer<Int>())
    
    override val descriptor: SerialDescriptor = intListSerializer.descriptor

    override fun serialize(encoder: Encoder, value: ByteArray) {
        // Convert ByteArray to List<Int> with unsigned values (0-255)
        val intList = value.map { byte -> (byte.toInt() and 0xFF) }
        // Delegate to ListSerializer for encoding
        intListSerializer.serialize(encoder, intList)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        // Decode as List<Int> first using ListSerializer
        val intList = intListSerializer.deserialize(decoder)
        // Convert unsigned ints (0-255) back to signed bytes
        return intList.map { unsignedInt -> 
            unsignedInt.coerceIn(0, 255).toByte()
        }.toByteArray()
    }
}

interface Platform {
    val name: String
}

val WeatherApiKey:String = "dc8650fc7d326ea705368d59da010bcc"
@Serializable
data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String, // City name
    val wind: Wind? = null,
    val clouds: Clouds? = null,
    val sys: Sys? = null,
    val coord: Coord? = null,
    val dt: Long? = null,
    /** Visibility in metres (OpenWeather). */
    val visibility: Int? = null,
    val rain: PrecipVolume? = null,
    val snow: PrecipVolume? = null,
) {
    @Serializable
    data class Main(
        val temp: Float, // Temperature in Kelvin
        val humidity: Int
    )

    @Serializable
    data class Weather(
        val id: Int = 800,
        val main: String = "",
        val description: String,
        val icon: String
    )

    @Serializable
    data class Wind(
        val speed: Float = 0f,
        /** Meteorological degrees (0 = N, 90 = E), optional. */
        val deg: Float? = null,
    )

    /** Optional 1h precip volume (mm) from OpenWeather. */
    @Serializable
    data class PrecipVolume(
        val `1h`: Float? = null,
    )

    @Serializable
    data class Clouds(
        val all: Int = 0,
    )

    @Serializable
    data class Sys(
        val sunrise: Long? = null,
        val sunset: Long? = null,
    )

    @Serializable
    data class Coord(
        val lat: Double? = null,
        val lon: Double? = null,
    )
}

data class WeatherInfo(
    val city: String,
    val temperature: Float, // Converted to Celsius
    val description: String,
    val humidity: Int,
    /** OpenWeather condition id (2xx…8xx). */
    val conditionId: Int = 800,
    /** Wind speed from API, m/s. */
    val windSpeedMs: Float = 0f,
    /** Wind direction degrees (0=N), if present. */
    val windDeg: Float? = null,
    /** Snow volume last 1h (mm), if present — used for light accumulation only. */
    val snowMm1h: Float? = null,
    /** Cloud cover 0–100 from API. */
    val cloudPct: Int = 0,
    /** Visibility in metres from OpenWeather, if present. */
    val visibilityMeters: Int? = null,
    /** Unix seconds (UTC) sunrise from OpenWeather `sys`, if present. */
    val sunriseEpochSec: Long? = null,
    /** Unix seconds (UTC) sunset from OpenWeather `sys`, if present. */
    val sunsetEpochSec: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** When this sample was obtained (epoch ms, device clock). */
    val fetchedAtEpochMs: Long = 0L,
)

fun getCurrentTimeInSeconds(): Long {
    return Clock.System.now().epochSeconds
}

private fun getCurrentDateTime(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

fun getFormattedTime(): String {
    val dateTime = getCurrentDateTime()
    val hour = dateTime.hour.toString().padStart(2, '0') // Ensure 2 digits
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

fun getFormattedDate(): String {
    val dateTime = getCurrentDateTime()
    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
    val month = dateTime.monthNumber.toString().padStart(2, '0')
    val year = dateTime.year
    return "$day/$month/$year"
}

/** True when VanGO device reports an active slave/remote link (protocol string from JSON). */
fun isRemoteLinkActive(slvConn: String): Boolean {
    val s = slvConn.trim().lowercase()
    if (s.isEmpty() || s == "no") return false
    return s == "yes" || s == "on" || s == "true" || s == "1" || s == "connected"
}

/**
 * `slvConn` çoğu cihazda uzaktan bağlantı durumu ("yes"/"no" vb.) olarak gelir; bu metin cihaz UID değildir.
 * Yalnızca durum anahtarları ise UI'da "-" döner; aksi halde (ör. UID bu alana yazılırsa) ham metin döner.
 */
fun deviceUidForUiFromSlvConn(slvConn: String): String {
    val t = slvConn.trim()
    if (t.isEmpty() || t == "\u2014" || t == "-") return "-"
    val lower = t.lowercase()
    val statusOnly = lower == "yes" || lower == "no" || lower == "on" || lower == "off" ||
        lower == "true" || lower == "false" || lower == "1" || lower == "0" ||
        lower == "connected" || lower == "disconnected"
    if (statusOnly) return "-"
    return t
}

expect fun isWifiConnected(): Boolean

/** True when the device has a usable internet path (Wi‑Fi or cellular). */
expect fun isInternetAvailable(): Boolean

enum class BleGattConnectionChain  {
    Idle,
    BluetoothNotSupported,
    BluetoothNotEnabled,
    PermissionDenied,
    DiscoveringDevice,
    DiscoverFinished,
    ConnectingGattServer,
    ConnectingGattServerFailed,
    DiscoveringServices,
    ServicesDiscoveringFailed,
    UnexpectedError,
    ChainEnded,
}

enum class Operations {
    Idle,
    BleConnectionState,
    EnableNotification,
    MtuConfiguring,
    ReadDataDevice, // send weather info
    WriteDataDevice, // event-based setting requests
    ConfigureDevice, //configuration of IOs
    RulesDevice, // Rules -- ?
}

enum class OperationState {
    On,
    Succeed,
    Failed,
}

data class VanGoGlobal (
    var connectionState: BleGattConnectionChain = BleGattConnectionChain.Idle,
    var currentMtu:Int = 20,
    var isCharacteristicNotificationOn:Boolean = false,
    private var currentOperation: Operations = Operations.BleConnectionState,
    var operationStatus: OperationState = OperationState.On,
    var receivedPackageTime: Long = getCurrentTimeInSeconds(), // last received data package time //todo: Bunu takip icin maindeApp ta while da calisan bir coroutine olacak delay  ile bakacak!
) {
    fun lockOperation(operation: Operations):Boolean {
        if(currentOperation == Operations.Idle) {
            currentOperation = operation
            operationStatus = OperationState.On
            return true
        }
        return false
    }

    fun unlockOperation() {
        currentOperation = Operations.Idle
    }

    fun operation(): Operations {
        return currentOperation
    }
}

enum class VanGoState {
    BluetoothNotEnabled,
    PermissionDenied,
    OnDiscoverDevice,
    OnDiscoverDone,
    OnGatServerConnection,
    OnServiceDiscovering,
    OnMtuConfigured,
    OnCharacteristicNotification,
    OnCharacteristicWrite,
}

// Safe array access extension functions
fun ByteArray.safeGet(index: Int): Byte? {
    return if (index >= 0 && index < this.size) this[index] else null
}

fun ByteArray.safeSet(index: Int, value: Byte): Boolean {
    return if (index >= 0 && index < this.size) {
        this[index] = value
        true
    } else false
}

fun <T> List<T>.safeGet(index: Int): T? {
    return if (index >= 0 && index < this.size) this[index] else null
}

fun <T> MutableList<T>.safeSet(index: Int, value: T): Boolean {
    return if (index >= 0 && index < this.size) {
        this[index] = value
        true
    } else false
}

// Safe buffer size validation functions
fun ByteArray.isValidSize(minSize: Int): Boolean {
    return this.size >= minSize
}

fun <T> List<T>.isValidSize(minSize: Int): Boolean {
    return this.size >= minSize
}

// Debug utilities for BLE data analysis
object BleDebugger {
    private var incomingDebugEnabled = false
    private var outgoingDataDebugEnabled = false
    private var outgoingConfigDebugEnabled = false
    private var outgoingWeatherDebugEnabled = false
    private var packetCounter = 0
    
    fun enableIncomingDebug(enabled: Boolean) {
        incomingDebugEnabled = enabled
    }
    
    fun enableOutgoingDataDebug(enabled: Boolean) {
        outgoingDataDebugEnabled = enabled
    }
    
    fun enableOutgoingConfigDebug(enabled: Boolean) {
        outgoingConfigDebugEnabled = enabled
    }
    
    fun enableOutgoingWeatherDebug(enabled: Boolean) {
        outgoingWeatherDebugEnabled = enabled
    }
    
    fun isIncomingDebugEnabled(): Boolean = incomingDebugEnabled
    
    fun isOutgoingDataDebugEnabled(): Boolean = outgoingDataDebugEnabled
    
    fun isOutgoingConfigDebugEnabled(): Boolean = outgoingConfigDebugEnabled
    
    fun isOutgoingWeatherDebugEnabled(): Boolean = outgoingWeatherDebugEnabled
    
    @Deprecated("Use enableOutgoingDataDebug(), enableOutgoingConfigDebug(), and enableOutgoingWeatherDebug() instead")
    fun enableOutgoingDebug(enabled: Boolean) {
        outgoingDataDebugEnabled = enabled
        outgoingConfigDebugEnabled = enabled
        outgoingWeatherDebugEnabled = enabled
    }
    
    @Deprecated("Use isOutgoingDataDebugEnabled(), isOutgoingConfigDebugEnabled(), or isOutgoingWeatherDebugEnabled() instead")
    fun isOutgoingDebugEnabled(): Boolean = outgoingDataDebugEnabled || outgoingConfigDebugEnabled || outgoingWeatherDebugEnabled
    
    @Deprecated("Use enableIncomingDebug() and specific outgoing debug methods instead")
    fun enableDebug(enabled: Boolean) {
        incomingDebugEnabled = enabled
        outgoingDataDebugEnabled = enabled
        outgoingConfigDebugEnabled = enabled
        outgoingWeatherDebugEnabled = enabled
    }
    
    @Deprecated("Use specific debug enabled methods instead")
    fun isDebugEnabled(): Boolean = incomingDebugEnabled || outgoingDataDebugEnabled || outgoingConfigDebugEnabled || outgoingWeatherDebugEnabled
    
    private fun getCurrentTimestamp(): String {
        return getFormattedTime() + "." + (getCurrentTimeInSeconds() % 1000).toString().padStart(3, '0')
    }
    
    fun logIncomingRawData(data: String, source: String = "BLE") {
        if (!incomingDebugEnabled) return
        
        packetCounter++
        val timestamp = getCurrentTimestamp()
        
        println("┌──────────────────────────────────────────────────────────────────")
        println("│ 📥 INCOMING BLE DEBUG PACKET #$packetCounter")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 🕐 Timestamp: $timestamp")
        println("│ 📥 Source: $source")
        println("│ 📏 Data Length: ${data.length} characters")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 📄 Raw JSON Data:")
        println("│ $data")
        println("├──────────────────────────────────────────────────────────────────")
        
        // Show hex dump of data
        val hexDump = data.encodeToByteArray().joinToString(" ") { byte -> 
            (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
        println("│ 🔢 Hex Dump:")
        println("│ $hexDump")
        println("└──────────────────────────────────────────────────────────────────")
    }
    
    fun logOutgoingDataRawData(data: String, target: String = "BLE") {
        if (!outgoingDataDebugEnabled) return
        
        packetCounter++
        val timestamp = getCurrentTimestamp()
        
        println("┌──────────────────────────────────────────────────────────────────")
        println("│ 📤 OUTGOING DATA PACKET #$packetCounter")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 🕐 Timestamp: $timestamp")
        println("│ 📤 Target: $target")
        println("│ 📏 Data Length: ${data.length} characters")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 📄 Raw JSON Data:")
        println("│ $data")
        println("├──────────────────────────────────────────────────────────────────")
        
        // Show hex dump of data
        val hexDump = data.encodeToByteArray().joinToString(" ") { byte -> 
            (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
        println("│ 🔢 Hex Dump:")
        println("│ $hexDump")
        println("└──────────────────────────────────────────────────────────────────")
    }
    
    fun logOutgoingConfigRawData(data: String, target: String = "BLE") {
        if (!outgoingConfigDebugEnabled) return
        
        packetCounter++
        val timestamp = getCurrentTimestamp()
        
        println("┌──────────────────────────────────────────────────────────────────")
        println("│ ⚙️ OUTGOING CONFIG PACKET #$packetCounter")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 🕐 Timestamp: $timestamp")
        println("│ 📤 Target: $target")
        println("│ 📏 Data Length: ${data.length} characters")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 📄 Raw JSON Data:")
        println("│ $data")
        println("├──────────────────────────────────────────────────────────────────")
        
        // Show hex dump of data
        val hexDump = data.encodeToByteArray().joinToString(" ") { byte -> 
            (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
        println("│ 🔢 Hex Dump:")
        println("│ $hexDump")
        println("└──────────────────────────────────────────────────────────────────")
    }
    
    fun logOutgoingWeatherRawData(data: String, target: String = "BLE") {
        if (!outgoingWeatherDebugEnabled) return
        
        packetCounter++
        val timestamp = getCurrentTimestamp()
        
        println("┌──────────────────────────────────────────────────────────────────")
        println("│ 🌤️ OUTGOING WEATHER PACKET #$packetCounter")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 🕐 Timestamp: $timestamp")
        println("│ 📤 Target: $target")
        println("│ 📏 Data Length: ${data.length} characters")
        println("├──────────────────────────────────────────────────────────────────")
        println("│ 📄 Raw JSON Data:")
        println("│ $data")
        println("├──────────────────────────────────────────────────────────────────")
        
        // Show hex dump of data
        val hexDump = data.encodeToByteArray().joinToString(" ") { byte -> 
            (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
        println("│ 🔢 Hex Dump:")
        println("│ $hexDump")
        println("└──────────────────────────────────────────────────────────────────")
    }
    
    @Deprecated("Use logOutgoingDataRawData(), logOutgoingConfigRawData(), or logOutgoingWeatherRawData() instead")
    fun logOutgoingRawData(data: String, target: String = "BLE") {
        logOutgoingDataRawData(data, target)
    }
    
    @Deprecated("Use logIncomingRawData() or specific outgoing methods instead")
    fun logRawData(data: String, source: String = "BLE") {
        logIncomingRawData(data, source)
    }
    
    fun logJsonParsing(data: String, success: Boolean, error: String? = null, isIncoming: Boolean = true) {
        if (isIncoming && !incomingDebugEnabled) return
        if (!isIncoming && !outgoingDataDebugEnabled && !outgoingConfigDebugEnabled && !outgoingWeatherDebugEnabled) return
        
        val direction = if (isIncoming) "📥 INCOMING" else "📤 OUTGOING"
        
        if (success) {
            println("✅ $direction JSON Parsing: SUCCESS")
        } else {
            println("❌ $direction JSON Parsing: FAILED")
            println("   Error: ${error ?: "Unknown error"}")
            println("   Data: ${data.take(100)}${if (data.length > 100) "..." else ""}")
        }
    }
    
    fun logVanGoDataValidation(data: VanGoData, isValid: Boolean) {
        if (!incomingDebugEnabled) return
        
        println("🔍 📥 INCOMING VanGoData Validation Report:")
        println("   Overall Valid: ${if (isValid) "✅ YES" else "❌ NO"}")
        println("   ├─ slvConn: \"${data.slvConn}\"")
        println("   ├─ numOfO: ${data.numOfO}")
        println("   ├─ numOfD: ${data.numOfD}")
        println("   ├─ numOfS: ${data.numOfS}")
        println("   ├─ RGBE: \"${data.RGBE}\"")
        println("   ├─ Th: ${data.Th}")
        println("   ├─ volt: ${data.volt}")
        println("   └─ Buffer Sizes:")
        println("      ├─ outNB: ${data.outNB.size} (req ≥16) ${if (data.outNB.isValidSize(16)) "✅" else "❌"}")
        println("      ├─ dimNB: ${data.dimNB.size} (req ≥4) ${if (data.dimNB.isValidSize(4)) "✅" else "❌"}")
        println("      ├─ senNB: ${data.senNB.size} (req ≥5) ${if (data.senNB.isValidSize(5)) "✅" else "❌"}")
        println("      ├─ outDB: ${data.outDB.size} (req ≥9) ${if (data.outDB.isValidSize(9)) "✅" else "❌"}")
        println("      ├─ dDB: ${data.dDB.size} (req ≥4) ${if (data.dDB.isValidSize(4)) "✅" else "❌"}")
        println("      ├─ sDB: ${data.sDB.size} (req ≥3) ${if (data.sDB.isValidSize(3)) "✅" else "❌"}")
        println("      └─ RGBDB: ${data.RGBDB.size} (req ≥3) ${if (data.RGBDB.isValidSize(3)) "✅" else "❌"}")
    }
    
    fun logBufferContents(data: VanGoData) {
        if (!incomingDebugEnabled) return
        
        println("📊 📥 INCOMING Buffer Contents Analysis:")
        
        println("   🔌 Outputs Name Buffer (${data.outNB.size}):")
        println("      ${data.outNB.mapIndexed { i, b -> "[$i]:$b" }.joinToString(", ")}")
        
        println("   🔅 Dims Name Buffer (${data.dimNB.size}):")
        println("      ${data.dimNB.mapIndexed { i, b -> "[$i]:$b" }.joinToString(", ")}")
        
        println("   📡 Sensors Name Buffer (${data.senNB.size}):")
        println("      ${data.senNB.mapIndexed { i, b -> "[$i]:${if(b.toInt() != 0) "ON" else "OFF"}" }.joinToString(", ")}")
        
        println("   🔌 Outputs Data Buffer (${data.outDB.size}):")
        println("      ${data.outDB.mapIndexed { i, b -> "[$i]:${if(b.toInt() != 0) "ON" else "OFF"}" }.joinToString(", ")}")
        
        println("   🔅 Dims Data Buffer (${data.dDB.size}):")
        println("      ${data.dDB.mapIndexed { i, b -> "[$i]:$b%" }.joinToString(", ")}")
        
        println("   📊 Sensors Data Buffer (${data.sDB.size}):")
        println("      ${data.sDB.mapIndexed { i, b -> "[$i]:$b" }.joinToString(", ")}")
        
        println("   🌈 RGB Data Buffer (${data.RGBDB.size}):")
        println("      ${data.RGBDB.mapIndexed { i, b -> when(i) { 0 -> "R:$b"; 1 -> "G:$b"; 2 -> "B:$b"; else -> "[$i]:$b" } }.joinToString(", ")}")
    }
    
    fun logSecurityCheck(operation: String, passed: Boolean, details: String = "", isIncoming: Boolean = true) {
        if (isIncoming && !incomingDebugEnabled) return
        if (!isIncoming && !outgoingDataDebugEnabled && !outgoingConfigDebugEnabled && !outgoingWeatherDebugEnabled) return
        
        val direction = if (isIncoming) "📥" else "📤"
        val status = if (passed) "✅ PASSED" else "❌ FAILED"
        println("🛡️  $direction Security Check: $operation - $status")
        if (details.isNotEmpty()) {
            println("   Details: $details")
        }
    }
    
    fun logWeatherSend(weatherData: String) {
        if (!outgoingWeatherDebugEnabled) return
        
        println("🌤️ 📤 OUTGOING Weather Data:")
        println("   └─ Data: $weatherData")
        logOutgoingWeatherRawData(weatherData, "VanGO Device")
    }
    
    fun logConfigSend(configData: String) {
        if (!outgoingConfigDebugEnabled) return
        
        println("⚙️ 📤 OUTGOING Configuration Data:")
        println("   └─ Data: $configData")
        logOutgoingConfigRawData(configData, "VanGO Device")
    }
    
    fun logDigitalPortCommand(portNo: Int, portValue: Boolean) {
        if (!outgoingDataDebugEnabled) return
        
        println("🔌 📤 OUTGOING Digital Port Command:")
        println("   └─ Port: $portNo, Value: ${if (portValue) "ON" else "OFF"}")
    }
    
    fun logDataSend(data: String) {
        if (!outgoingDataDebugEnabled) return
        
        println("📡 📤 OUTGOING Data Message:")
        println("   └─ Data: $data")
        logOutgoingDataRawData(data, "VanGO Device")
    }
}

@Serializable
class VanGoData(
    val slvConn: String,
    /** Cihazın WiFi (STA) MAC — myhexnet kaydı ve UID sorgusu için. */
    @kotlinx.serialization.SerialName("wifi_mac")
    val wifiMac: String? = null,
    val numOfO:Int,
    val numOfD: Int,
    val numOfS: Int,
    val RGBE: String,
    val Th: Int,
    val volt: Float,
    @Serializable(with = UnsignedByteArraySerializer::class)
    val outNB: ByteArray = byteArrayOf(),
    @Serializable(with = UnsignedByteArraySerializer::class)
    val dimNB: ByteArray = byteArrayOf(),
    @Serializable(with = UnsignedByteArraySerializer::class)
    val senNB: ByteArray = byteArrayOf(),
    @Serializable(with = UnsignedByteArraySerializer::class)
    val outDB: ByteArray,
    @Serializable(with = UnsignedByteArraySerializer::class)
    val dDB: ByteArray,
    @Serializable(with = UnsignedByteArraySerializer::class)
    val sDB: ByteArray,
    @Serializable(with = UnsignedByteArraySerializer::class)
    val RGBDB: ByteArray,
    /** IO modül firmware: Wi‑Fi STA MAC (`mac` alanı). */
    val mac: String? = null,
    val motorEn: Int? = null,
    val rgbEn: Int? = null,
    val tankN: Int? = null,
    val relayMask: Int? = null,
    val dimMask: Int? = null,
    val sensorMask: Int? = null,
    val tankMask: Int? = null,
    val ioLink: Int? = null,
    val io: IoProfileSnapshot? = null,
    val sensorTemp: Int? = null,
    val sensorHumidity: Int? = null,
    val panelSeq: Int? = null,
    val panelChanged: Boolean? = null,
) {
    // Data validation function
    fun isValid(): Boolean {
        return outNB.isValidSize(0) &&
               dimNB.isValidSize(0) &&
               senNB.isValidSize(0) &&
               dDB.isValidSize(0) &&
               outDB.isValidSize(0) &&
               sDB.isValidSize(0) &&
               RGBDB.isValidSize(0)
    }
    
    // Safe access methods for critical buffers
    fun getSafeOutputsName(index: Int): Byte {
        val result = outNB.safeGet(index) ?: 0
        if (BleDebugger.isDebugEnabled() && outNB.safeGet(index) == null) {
            BleDebugger.logSecurityCheck("getSafeOutputsName($index)", false, "Index out of bounds, returned default: 0")
        }
        //println("getSafeOutputsName($index): $result")
        return result
    }
    
    fun getSafeDimsName(index: Int): Byte {
        val result = dimNB.safeGet(index) ?: 1 // Default to lamp
        if (BleDebugger.isDebugEnabled() && dimNB.safeGet(index) == null) {
            BleDebugger.logSecurityCheck("getSafeDimsName($index)", false, "Index out of bounds, returned default: 1 (lamp)")
        }
        //println("getSafeDimsName($index): $result")
        return result
    }
    
    fun getSafeSensorsEnabled(index: Int): Byte {
        val result = senNB.safeGet(index) ?: 0
        if (BleDebugger.isDebugEnabled() && senNB.safeGet(index) == null) {
            BleDebugger.logSecurityCheck("getSafeSensorsEnabled($index)", false, "Index out of bounds, returned default: 0")
        }
        //println("getSafeSensorsEnabled($index): $result")
        return result
    }
    
    fun getSafeOutputsData(index: Int): Byte {
        val result = outDB.safeGet(index) ?: 0
        if (BleDebugger.isDebugEnabled() && outDB.safeGet(index) == null) {
            BleDebugger.logSecurityCheck("getSafeOutputsData($index)", false, "Index out of bounds, returned default: 0")
        }
        //println("getSafeOutputsData($index): $result")
        return result
    }
    
    fun getSafeDimsData(index: Int): Byte {
        val result = dDB.safeGet(index) ?: 0
        if (BleDebugger.isDebugEnabled() && dDB.safeGet(index) == null) {
            BleDebugger.logSecurityCheck("getSafeDimsData($index)", false, "Index out of bounds, returned default: 0")
        }
        //println("getSafeDimsData($index): $result")
        return result
    }
    
    fun getSafeSensorsData(index: Int): Byte {
        val result = sDB.safeGet(index) ?: 0
        if (BleDebugger.isDebugEnabled() && sDB.safeGet(index) == null) {
            BleDebugger.logSecurityCheck("getSafeSensorsData($index)", false, "Index out of bounds, returned default: 0")
        }
        //println("getSafeSensorsData($index): $result")
        return result
    }

    /** Sensor byte'ını UI için unsigned (0..255) değere çevirir. */
    fun getSafeSensorsDataUnsigned(index: Int): Int {
        return getSafeSensorsData(index).toInt() and 0xFF
    }
    
    fun getSafeRGBData(index: Int): Byte {
        val result = RGBDB.safeGet(index) ?: 0
        if (BleDebugger.isDebugEnabled() && RGBDB.safeGet(index) == null) {
            BleDebugger.logSecurityCheck("getSafeRGBData($index)", false, "Index out of bounds, returned default: 0")
        }
        //println("getSafeRGBData($index): $result")
        return result
    }

    /**
     * Logical sRGB for Compose previews (icon tile, wheel seed). Device buffer indices 0–2 are
     * wire-ordered R, B, G — swap 1↔2 so UI matches [writeRGBPort] semantics and physical output.
     */
    fun rgb888ForUi(): Triple<Int, Int, Int> {
        val r = getSafeRGBData(0).toInt() and 0xFF
        val g = getSafeRGBData(2).toInt() and 0xFF
        val b = getSafeRGBData(1).toInt() and 0xFF
        return Triple(r, g, b)
    }
}

@Serializable
class WriteData (
    val MessageType: String,
    val writeDataType: String,
    val writeNo: Int,
    val writeData: Int 
)

@Serializable
class WriteDataRGB (
    val MessageType: String,
    val writeDataType: String,
    val writeNo: Int,
    val writeData: IntArray
)

@Serializable
class WeatherWrite (
    val MessageType: String,
    val Time: String, //"12:24",
    val Date: String, //"11/01/25",
    val Weather: String, //"Cloudy",//Aciklama asagida var
    val Temperature: String, //"33",
    val Location: String //"Istanbul"
)

@Serializable
data class DevConfig (
    val MessageType: String,
    val numOfOutputs: Int,
    @Serializable(with = UnsignedByteArraySerializer::class)
    var OutputsNameBuffer: ByteArray,
    val numOfDims: Int,
    @Serializable(with = UnsignedByteArraySerializer::class)
    var DimsNameBuffer: ByteArray,
    val numOfSensors: Int,
    @Serializable(with = UnsignedByteArraySerializer::class)
    var SensorsNameBuffer:ByteArray,
    @SerialName("RGBEnabled")
    var RGBEnables: String,
    var Theme: String,
    @SerialName("motorEnabled")
    val motorEnabled: Boolean = true,
    val numOfTanks: Int = 0,
    val tankEnableMask: Int = 0,
    val relayEnableMask: Int = 0,
    val dimEnableMask: Int = 0,
    val sensorEnableMask: Int = 0,
    val links: IoProfileLinks = IoProfileLinks(),
)

/** BLE/MQTT Configuration JSON — panel isim/ikon `channels` ekler (IO flash SoT). */
fun encodeDevConfigForDevice(config: DevConfig): String {
    val prepared = if (config.MessageType == "Configuration") config else devConfigForBleWrite(config)
    val base = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }.encodeToJsonElement(prepared).jsonObject
    val channels = panelPlatformChannels.value
        ?: PanelSettingsPlatformSync.readCachedPanelConfig()?.channels
    if (channels == null) {
        return Json.encodeToString(JsonObject(base))
    }
    val withChannels = buildJsonObject {
        base.forEach { (k, v) -> put(k, v) }
        put("channels", Json.encodeToJsonElement(channels))
    }
    return Json.encodeToString(withChannels)
}

/**
 * Sadece isim/ikon — ATT/MQTT icin hafif paket (~1KB alti).
 * Tam Configuration + zengin channels 3KB+ olabiliyor.
 */
fun encodePanelChannelsForDevice(): String? {
    val channels = panelPlatformChannels.value
        ?: PanelSettingsPlatformSync.readCachedPanelConfig()?.channels
        ?: return null
    fun slim(list: List<PanelPlatformChannel>) = buildJsonArray {
        list.forEach { ch ->
            add(
                buildJsonObject {
                    put("id", ch.id)
                    put("name", ch.name)
                    put("icon", ch.icon)
                },
            )
        }
    }
    return Json.encodeToString(
        buildJsonObject {
            put("MessageType", "PanelChannels")
            put(
                "channels",
                buildJsonObject {
                    put("relays", slim(channels.relays))
                    put("dimmers", slim(channels.dimmers))
                    put("sensors", slim(channels.sensors))
                    put("water", slim(channels.water))
                },
            )
        },
    )
}

// Global state management for VanGO application
val vanGoGlobal = MutableStateFlow(VanGoGlobal())
val vangoDeviceListSize = MutableStateFlow<Int>(0)
val vangoDeviceList = ArrayList<BleDevice>()
data class BleEnvSensors(val tempC: Int? = null, val humidityPct: Int? = null)
val bleEnvSensors = MutableStateFlow(BleEnvSensors())
private val bleRxBuffer = StringBuilder()
private val vanGoJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}
val vanGoData = MutableStateFlow(VanGoData("No", null, 8, 2, 5, "yes", 1, .0f,
    byteArrayOf(1,2,3,4,5,6,7,8),
    byteArrayOf(7,5),
    byteArrayOf(7,8,7,7,7),
    ByteArray(16) { 0 },
    byteArrayOf(0, 0),
    byteArrayOf(0, 0, 0, 0, 0),
    byteArrayOf(0,0,0,0),))
var printComing:Boolean = false

/**
 * Display ile aynı mantık: kullanıcı toggle sonrası telemetri hemen eski değere çekmesin.
 * Feedback istenen değerle gelince veya grace dolunca kabul et.
 */
object OutputUiHold {
    const val GRACE_MS = 3500L
    private val relayUntil = mutableMapOf<Int, Long>()
    private val relayWant = mutableMapOf<Int, Boolean>()
    private val dimUntil = mutableMapOf<Int, Long>()
    private val dimWant = mutableMapOf<Int, Int>()
    private var rgbUntil = 0L
    private var rgbWantOn: Boolean? = null

    fun markRelay(port: Int, on: Boolean) {
        val now = Clock.System.now().toEpochMilliseconds()
        relayUntil[port] = now + GRACE_MS
        relayWant[port] = on
    }

    fun markDimmer(port: Int, percent: Int) {
        val now = Clock.System.now().toEpochMilliseconds()
        dimUntil[port] = now + GRACE_MS
        dimWant[port] = percent.coerceIn(0, 100)
    }

    fun markRgbEnable(on: Boolean) {
        rgbUntil = Clock.System.now().toEpochMilliseconds() + GRACE_MS
        rgbWantOn = on
    }

    fun mergeRelays(cur: ByteArray, incoming: ByteArray): ByteArray {
        if (incoming.isEmpty()) return cur
        if (cur.isEmpty()) return incoming
        val now = Clock.System.now().toEpochMilliseconds()
        val size = maxOf(cur.size, incoming.size)
        return ByteArray(size) { index ->
            val newB = if (index < incoming.size) incoming[index] else cur.getOrElse(index) { 0 }
            val until = relayUntil[index] ?: 0L
            if (until == 0L) {
                newB
            } else if (now >= until) {
                /* Grace bitti, feedback yok/gecikti → cihaz telemetrisine don */
                relayUntil.remove(index)
                relayWant.remove(index)
                newB
            } else {
                val wantOn = relayWant[index] == true
                val newOn = newB.toInt() != 0
                if (newOn == wantOn) {
                    relayUntil.remove(index)
                    relayWant.remove(index)
                    newB
                } else {
                    /* cur eski kalmis olabilir — istenen UI degerini zorla tut */
                    if (wantOn) 1 else 0
                }
            }
        }
    }

    /** UI tile: hold aktifken gosterilecek deger (yoksa null = cihaz). */
    fun relayOverride(index: Int): Boolean? {
        val until = relayUntil[index] ?: return null
        if (Clock.System.now().toEpochMilliseconds() >= until) return null
        return relayWant[index]
    }

    fun mergeDimmers(cur: ByteArray, incoming: ByteArray): ByteArray {
        if (incoming.isEmpty()) return cur
        if (cur.isEmpty()) return incoming
        val now = Clock.System.now().toEpochMilliseconds()
        val size = maxOf(cur.size, incoming.size)
        return ByteArray(size) { index ->
            val newB = if (index < incoming.size) incoming[index] else cur.getOrElse(index) { 0 }
            val oldB = cur.getOrElse(index) { newB }
            val until = dimUntil[index] ?: 0L
            if (until == 0L || now >= until) {
                if (until != 0L) {
                    dimUntil.remove(index)
                    dimWant.remove(index)
                }
                newB
            } else {
                val want = dimWant[index] ?: (oldB.toInt() and 0xFF)
                val got = newB.toInt() and 0xFF
                if (got == want) {
                    dimUntil.remove(index)
                    dimWant.remove(index)
                    newB
                } else {
                    oldB
                }
            }
        }
    }

    fun mergeRgb(cur: ByteArray, incoming: ByteArray): ByteArray {
        if (incoming.isEmpty()) return cur
        if (cur.isEmpty()) return incoming
        val now = Clock.System.now().toEpochMilliseconds()
        if (rgbUntil == 0L || now >= rgbUntil || rgbWantOn == null) {
            rgbUntil = 0L
            rgbWantOn = null
            return if (incoming.size >= cur.size) incoming else {
                val out = cur.copyOf(maxOf(cur.size, incoming.size))
                incoming.copyInto(out)
                out
            }
        }
        val wantOn = rgbWantOn == true
        val newOn = incoming.getOrElse(3) { 0 }.toInt() != 0
        return if (newOn == wantOn) {
            rgbUntil = 0L
            rgbWantOn = null
            incoming
        } else {
            cur
        }
    }
}

/** IO periyodik notify çoğu zaman isim tamponlarını (outNB/dimNB/senNB) göndermez.
 *  outDB/dDB de sıkça yalnızca aydınlatma röleleri kadar gelir; motor H-bridge pinleri
 *  (numOfO+) paket dışında kalır — MQTT merge gibi indekse göre birleştir, eksik indeksi koru.
 */
private fun mergeBleLiveTelemetry(cur: VanGoData, incoming: VanGoData): VanGoData {
    fun keepBuf(new: ByteArray, old: ByteArray): ByteArray =
        if (new.isNotEmpty()) new else old

    fun mergeOutputBuf(new: ByteArray, old: ByteArray): ByteArray {
        if (new.isEmpty()) return old
        if (old.isEmpty()) return new
        val size = maxOf(new.size, old.size)
        return ByteArray(size) { index ->
            if (index < new.size) new[index] else old[index]
        }
    }

    val mergedOut = OutputUiHold.mergeRelays(cur.outDB, mergeOutputBuf(incoming.outDB, cur.outDB))
    val mergedDim = OutputUiHold.mergeDimmers(cur.dDB, mergeOutputBuf(incoming.dDB, cur.dDB))
    val mergedRgb = OutputUiHold.mergeRgb(cur.RGBDB, keepBuf(incoming.RGBDB, cur.RGBDB))

    return VanGoData(
        slvConn = incoming.slvConn,
        wifiMac = incoming.wifiMac ?: cur.wifiMac,
        numOfO = incoming.numOfO,
        numOfD = incoming.numOfD,
        numOfS = incoming.numOfS,
        RGBE = incoming.RGBE,
        Th = incoming.Th,
        volt = incoming.volt,
        outNB = keepBuf(incoming.outNB, cur.outNB),
        dimNB = keepBuf(incoming.dimNB, cur.dimNB),
        senNB = keepBuf(incoming.senNB, cur.senNB),
        outDB = mergedOut,
        dDB = mergedDim,
        sDB = keepBuf(incoming.sDB, cur.sDB),
        RGBDB = mergedRgb,
        mac = incoming.mac ?: cur.mac,
        motorEn = incoming.motorEn ?: cur.motorEn,
        rgbEn = incoming.rgbEn ?: cur.rgbEn,
        tankN = incoming.tankN ?: cur.tankN,
        relayMask = incoming.relayMask ?: cur.relayMask,
        dimMask = incoming.dimMask ?: cur.dimMask,
        sensorMask = incoming.sensorMask ?: cur.sensorMask,
        tankMask = incoming.tankMask ?: cur.tankMask,
        ioLink = incoming.ioLink ?: cur.ioLink,
        io = incoming.io ?: cur.io,
        sensorTemp = incoming.sensorTemp ?: cur.sensorTemp,
        sensorHumidity = incoming.sensorHumidity ?: cur.sensorHumidity,
        panelSeq = incoming.panelSeq ?: cur.panelSeq,
        panelChanged = incoming.panelChanged ?: cur.panelChanged,
    )
}

private fun vanGoDataWithRelayState(portNo: Int, on: Boolean): VanGoData {
    val cur = vanGoData.value
    val size = maxOf(cur.outDB.size, portNo + 1, cur.numOfO, 16)
    val out = ByteArray(size) { index ->
        when {
            index == portNo -> if (on) 1 else 0
            index < cur.outDB.size -> cur.outDB[index]
            else -> 0
        }
    }
    return vanGoDataReplacing(cur, outDB = out)
}

private fun vanGoDataWithDimmerState(portNo: Int, percent: Int): VanGoData {
    val cur = vanGoData.value
    val pct = percent.coerceIn(0, 100).toByte()
    val size = maxOf(cur.dDB.size, portNo + 1, cur.numOfD, 8)
    val dim = ByteArray(size) { index ->
        when {
            index == portNo -> pct
            index < cur.dDB.size -> cur.dDB[index]
            else -> 0
        }
    }
    return vanGoDataReplacing(cur, dDB = dim)
}

private fun vanGoDataWithRgbState(r: Int, g: Int, b: Int, isLight: Int): VanGoData {
    val cur = vanGoData.value
    val size = maxOf(cur.RGBDB.size, 4)
    val rgb = ByteArray(size) { i ->
        when (i) {
            0 -> r.coerceIn(0, 255).toByte()
            1 -> g.coerceIn(0, 255).toByte()
            2 -> b.coerceIn(0, 255).toByte()
            3 -> if (isLight != 0) 1.toByte() else 0
            else -> if (i < cur.RGBDB.size) cur.RGBDB[i] else 0
        }
    }
    return vanGoDataReplacing(cur, RGBDB = rgb)
}

private fun vanGoDataReplacing(
    cur: VanGoData,
    outDB: ByteArray = cur.outDB,
    dDB: ByteArray = cur.dDB,
    RGBDB: ByteArray = cur.RGBDB,
): VanGoData = VanGoData(
    slvConn = cur.slvConn,
    wifiMac = cur.wifiMac,
    numOfO = cur.numOfO,
    numOfD = cur.numOfD,
    numOfS = cur.numOfS,
    RGBE = cur.RGBE,
    Th = cur.Th,
    volt = cur.volt,
    outNB = cur.outNB,
    dimNB = cur.dimNB,
    senNB = cur.senNB,
    outDB = outDB,
    dDB = dDB,
    sDB = cur.sDB,
    RGBDB = RGBDB,
    mac = cur.mac,
    motorEn = cur.motorEn,
    rgbEn = cur.rgbEn,
    tankN = cur.tankN,
    relayMask = cur.relayMask,
    dimMask = cur.dimMask,
    sensorMask = cur.sensorMask,
    tankMask = cur.tankMask,
    ioLink = cur.ioLink,
    io = cur.io,
    sensorTemp = cur.sensorTemp,
    sensorHumidity = cur.sensorHumidity,
    panelSeq = cur.panelSeq,
    panelChanged = cur.panelChanged,
)


internal fun applyBleEnvReading(env: BleEnvSensors?) {
    if (env == null) return
    /* 0/gecersiz degerler onceki (platform/BLE) okumayi ezmesin */
    val temp = env.tempC?.takeIf { it in 1..125 }
    val hum = env.humidityPct?.takeIf { it in 1..100 }
    if (temp == null && hum == null) return
    bleEnvSensors.value = bleEnvSensors.value.copy(
        tempC = temp ?: bleEnvSensors.value.tempC,
        humidityPct = hum ?: bleEnvSensors.value.humidityPct,
    )
    val cur = vanGoData.value
    val size = maxOf(cur.sDB.size, 3)
    val sdb = ByteArray(size) { index ->
        when (index) {
            0 -> temp?.coerceIn(0, 255)?.toByte() ?: cur.sDB.getOrElse(0) { 0 }
            1 -> hum?.coerceIn(0, 255)?.toByte() ?: cur.sDB.getOrElse(1) { 0 }
            else -> cur.sDB.getOrElse(index) { 0 }
        }
    }
    vanGoData.value = VanGoData(
        slvConn = cur.slvConn,
        wifiMac = cur.wifiMac,
        numOfO = cur.numOfO,
        numOfD = cur.numOfD,
        numOfS = cur.numOfS,
        RGBE = cur.RGBE,
        Th = cur.Th,
        volt = cur.volt,
        outNB = cur.outNB,
        dimNB = cur.dimNB,
        senNB = cur.senNB,
        outDB = cur.outDB,
        dDB = cur.dDB,
        sDB = sdb,
        RGBDB = cur.RGBDB,
        mac = cur.mac,
        motorEn = cur.motorEn,
        rgbEn = cur.rgbEn,
        tankN = cur.tankN,
        relayMask = cur.relayMask,
        dimMask = cur.dimMask,
        sensorMask = cur.sensorMask,
        tankMask = cur.tankMask,
        ioLink = cur.ioLink,
        io = cur.io,
        sensorTemp = temp ?: cur.sensorTemp,
        sensorHumidity = hum ?: cur.sensorHumidity,
    )
}

private fun sanitizeBleJson(raw: String): String {
    val trimmed = raw.replace("\u0000", "").trim()
    if (trimmed.isEmpty()) return trimmed
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    if (start >= 0 && end > start) {
        return trimmed.substring(start, end + 1)
    }
    return trimmed
}

private fun ingestBleChunk(chunk: String): List<String> {
    val normalized = chunk.replace("\u0000", "")
    if (normalized.isBlank()) return emptyList()
    bleRxBuffer.append(normalized)

    val out = mutableListOf<String>()
    var depth = 0
    var start = -1
    var i = 0
    while (i < bleRxBuffer.length) {
        val ch = bleRxBuffer[i]
        if (ch == '{') {
            if (depth == 0) start = i
            depth++
        } else if (ch == '}') {
            if (depth > 0) {
                depth--
                if (depth == 0 && start >= 0) {
                    out += bleRxBuffer.substring(start, i + 1)
                    start = -1
                }
            }
        }
        i++
    }

    if (out.isNotEmpty()) {
        val lastEnd = bleRxBuffer.lastIndexOf('}')
        if (lastEnd >= 0) {
            bleRxBuffer.delete(0, lastEnd + 1)
        }
    }

    // Guard against indefinite growth on malformed streams.
    if (bleRxBuffer.length > 16384) {
        val keepFrom = bleRxBuffer.lastIndexOf('{').takeIf { it >= 0 } ?: bleRxBuffer.length
        bleRxBuffer.delete(0, keepFrom)
    }

    return out
}

/**
 * Abstract base class for VanGO device management
 * Provides common functionality for BLE communication, device control, and data management
 * 
 * Key Features:
 * - Device discovery and connection management
 * - MTU configuration for optimal data transfer
 * - Characteristic notification setup
 * - Digital port control (I/O operations)
 * - Configuration data transmission
 * - Weather information integration
 * - Comprehensive error handling and debug logging
 * 
 * Platform-specific implementations must override checkPermissions()
 */
abstract class VanGO {
    /**
     * Platform-specific permission validation
     * Must be implemented by concrete classes to handle platform-specific permission models
     * @return true if all required permissions are granted
     */
    abstract fun checkPermissions():Boolean

    /**
     * Checks if a VanGO device is currently connected
     * @return true if device is connected and ready for communication
     */
    fun isConnected(): Boolean {
        return getAppContext().ble().isConnected()
    }

    /**
     * Initiates discovery of nearby VanGO devices
     * Clears existing device list and starts BLE scanning
     * Updates global connection state to indicate discovery is in progress
     */
    fun discoverVangos() {
        if (isBleHardwareConnected() || isBleGattLinkReady()) {
            println("discoverVangos skipped: BLE already connected")
            return
        }
        val st = vanGoGlobal.value.connectionState
        if (st == BleGattConnectionChain.ConnectingGattServer ||
            st == BleGattConnectionChain.DiscoveringServices
        ) {
            println("discoverVangos skipped: connection in progress ($st)")
            return
        }
        getAppContext().ble().stopDiscovering()
        vangoDeviceList.clear()
        vangoDeviceListSize.value = 0
        vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.DiscoveringDevice)
        if (!getAppContext().ble().startDiscovering()) {
            println("discoverVangos: startDiscovering failed")
            vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.DiscoverFinished)
            return
        }
        println("discoverVangos: ${vanGoGlobal.value}")
    }

    /**
     * Connects to a discovered VanGO device
     * @param index Index of the device in the discovered devices list
     * Updates global connection state based on connection success/failure
     */
    fun connect(index: Int) {
        val device = vangoDeviceList.getOrNull(index)
        if (device == null) {
            println("connect: invalid list index $index (size=${vangoDeviceList.size})")
            vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.ConnectingGattServerFailed)
            return
        }
        connectByMac(device.mac)
    }

    fun connectByMac(mac: String): Boolean {
        val normalized = normalizeMacForDisplay(mac.trim())
        if (!isValidMacFormat(normalized)) {
            println("connectByMac: invalid MAC $mac")
            vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.ConnectingGattServerFailed)
            return false
        }
        pruneWifiMacTwinsFromManualStore()
        val discovered = vangoDeviceList.map { it.mac } +
            listOfNotNull(getAppContext().ble().connectedDeviceMac())
        // Tek sefer çözümle — Android katmanında tekrar +2 uygulanmasın
        val bleOnly = resolveBleConnectMac(normalized, discovered)
        println("connectByMac resolve: in=$normalized → ble=$bleOnly discovered=$discovered")
        ManualPairedDeviceStore.ensureMacInDiscoveryList(bleOnly)
        if (getAppContext().ble().connectByMac(bleOnly)) {
            vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.ConnectingGattServer)
            return true
        }
        vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.ConnectingGattServerFailed)
        return false
    }

    /**
     * Configures the Maximum Transmission Unit (MTU) for BLE communication
     * Attempts to set MTU to 512 bytes for optimal data transfer performance
     * Includes comprehensive error handling and operation locking
     */
    fun configureMtu() {
        val success = getAppContext().ble().configureMtu()
        if (!success) {
            BleDebugger.logSecurityCheck("configureMtu", false, "Failed to configure MTU - device may not support MTU configuration or permissions denied")
            println("❌ MTU Configuration Failed - cannot proceed with MTU setup")
        } else {
            BleDebugger.logSecurityCheck("configureMtu", true, "MTU configuration request initiated successfully")
            println("✅ MTU Configuration initiated successfully")
            
            val lockSuccess = vanGoGlobal.value.lockOperation(Operations.MtuConfiguring)
            if (!lockSuccess) {
                BleDebugger.logSecurityCheck("configureMtu.lockOperation", false, "Failed to lock operation for MTU configuration - another operation in progress")
                println("❌ Failed to lock operation for MTU configuration - another operation already in progress")
            } else {
                BleDebugger.logSecurityCheck("configureMtu.lockOperation", true, "MTU configuration operation locked successfully")
                println("✅ MTU configuration operation locked successfully")
            }
        }
    }

    /**
     * Enables characteristic notifications for receiving data from VanGO device
     * Sets up notification callback to receive real-time data updates
     * Includes operation locking to prevent concurrent notification setup
     */
    fun enableCharacteristicNotification() {
        val success = getAppContext().ble().enableReadChar()
        if (!success) {
            BleDebugger.logSecurityCheck("enableCharacteristicNotification.enableReadChar", false, "Failed to enable characteristic read notification")
            println("❌ Failed to enable characteristic read notification")
        } else {
            BleDebugger.logSecurityCheck("enableCharacteristicNotification.enableReadChar", true, "Characteristic read notification enabled successfully")
            println("✅ Characteristic read notification enabled successfully")
            
            val lockSuccess = vanGoGlobal.value.lockOperation(Operations.EnableNotification)
            if (!lockSuccess) {
                BleDebugger.logSecurityCheck("enableCharacteristicNotification.lockOperation", false, "Failed to lock operation for notification enabling - another operation in progress")
                println("❌ Failed to lock operation for notification enabling - another operation already in progress")
            } else {
                BleDebugger.logSecurityCheck("enableCharacteristicNotification.lockOperation", true, "Notification enabling operation locked successfully")
                println("✅ Notification enabling operation locked successfully")
            }
        }
    }

    private fun sendMyhexnetMqttCommand(cmd: String, args: kotlinx.serialization.json.JsonObject): Boolean {
        val panelId = resolvedPanelDeviceId() ?: run {
            println("Remote command skipped: panel device_id missing")
            return false
        }
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            println("Remote command skipped: auth token missing")
            return false
        }
        return runBlocking {
            HexnetDeviceCommandApi.send(token, panelId, cmd, args)
        }
    }

    private fun dispatchRemoteCommand(
        cmd: String,
        args: kotlinx.serialization.json.JsonObject,
        mqttPublish: () -> Boolean,
    ): Boolean {
        val httpOk = sendMyhexnetMqttCommand(cmd, args)
        val mqttOk = if (!httpOk && isAppRemoteMqttConnected()) mqttPublish() else false
        val ok = httpOk || mqttOk
        if (!ok) {
            remoteSessionError.value =
                "Komut cihaza iletilemedi. Panel oturumu ve cihaz kaydı kontrol edin."
            println("Remote command failed cmd=$cmd http=$httpOk mqtt=$mqttOk")
        } else {
            remoteSessionError.value = null
        }
        return finishRemoteWrite(ok)
    }

    private fun finishRemoteWrite(ok: Boolean): Boolean {
        if (ok) HeroLinkBus.notifyOutboundTx()
        vanGoGlobal.value.operationStatus = if (ok) OperationState.Succeed else OperationState.Failed
        return ok
    }

    private fun finishBleWrite(ok: Boolean): Boolean {
        if (ok) HeroLinkBus.notifyOutboundTx()
        return ok
    }

    /**
     * Sends device configuration data to the VanGO device.
     * Panel isim/ikon SoT için [panelPlatformChannels] `channels` olarak eklenir.
     */
    fun writeConfig(config: DevConfig):Boolean {
        val writeDataJson = encodeDevConfigForDevice(config)
        val panelChannelsJson = encodePanelChannelsForDevice()
        var bleOk = false
        if (isBleGattLinkReady()) {
            try {
                /* Once isim/ikon (kucuk), sonra tam Configuration */
                if (panelChannelsJson != null) {
                    BleDebugger.logConfigSend(panelChannelsJson)
                    val chOk = getAppContext().ble().sendData(panelChannelsJson)
                    println(if (chOk) "✅ PanelChannels BLE sent (${panelChannelsJson.length} chars)" else "❌ PanelChannels BLE failed")
                    bleOk = chOk
                }
                BleDebugger.logConfigSend(writeDataJson)
                val cfgOk = getAppContext().ble().sendData(writeDataJson)
                if (!cfgOk) {
                    BleDebugger.logSecurityCheck("writeConfig.sendData", false, "Failed to send configuration data to device", false)
                    println("❌ Failed to send configuration BLE")
                } else {
                    BleDebugger.logSecurityCheck("writeConfig.sendData", true, "Configuration BLE sent", false)
                    println("✅ Configuration BLE sent (${writeDataJson.length} chars, chunked)")
                    bleOk = true
                }
            } catch (e: Exception) {
                BleDebugger.logSecurityCheck("writeConfig.jsonEncoding", false, "Exception during JSON encoding: ${e.message}", false)
                println("Error parsing JSON: ${e.message}")
            }
        }
        /* BLE yaninda MQTT yedek — isim/ikon SoT tablet MTU chunk basarisiz olsa bile IO'ya ulassin */
        var mqttOk = false
        if (canSendRemoteCommands()) {
            try {
                val remoteCfg = devConfigForBleWrite(config).copy(
                    links = config.links.copy(remote = true),
                )
                val configJson = encodeDevConfigForDevice(remoteCfg)
                val args = com.hextech.vango.profile.HexnetCommandArgs.ioConfigSet(configJson)
                val httpOk = sendMyhexnetMqttCommand("io_config_set", args)
                mqttOk = if (httpOk) {
                    true
                } else if (isAppRemoteMqttConnected()) {
                    mqttTelemetryManagerRef.value?.publishConfigCommand(configJson) == true
                } else {
                    false
                }
                if (mqttOk) {
                    PanelSettingsPlatformSync.persistProfile(devConfigToIoProfileSnapshot(remoteCfg))
                    remoteSessionError.value = null
                    println("✅ Configuration MQTT/io_config_set sent")
                } else if (!bleOk) {
                    remoteSessionError.value =
                        "Panel ayarları cihaza iletilemedi. BLE veya uzaktan oturum/MQTT bağlantısını kontrol edin."
                }
            } catch (e: Exception) {
                println("Remote config publish failed: ${e.message}")
            }
        }
        val ok = bleOk || mqttOk
        if (!ok) {
            if (!isBleGattLinkReady() && !canSendRemoteCommands()) {
                println("writeConfig skipped: BLE link not ready")
            }
            return false
        }
        return if (bleOk) finishBleWrite(true) else finishRemoteWrite(true)
    }

    /**
     * Writes analog/dimmer port values (0-100) to the VanGO device
     * Used for dimmer controls and analog outputs
     * @param portNo The port number to write to
     * @param portValue The analog value (0-100)
     * @return Boolean indicating success/failure
     */
    fun writeDimmerPort(portNo: Int, portValue: Int):Boolean {
        /* BLE bağlıysa MQTT/remote'dan önce BLE kullan (karşılıklı kontrol). */
        if (isBleGattLinkReady() || isBleHardwareConnected()) {
            val clampedValue = portValue.coerceIn(0, 100)
            val writeData = WriteData(
                MessageType = "Write",
                writeDataType = "Dim",
                writeNo = portNo,
                writeData = clampedValue
            )
            try {
                val writeDataJson = vanGoJson.encodeToString(writeData)
                BleDebugger.logDigitalPortCommand(portNo, portValue > 0)
                BleDebugger.logDataSend(writeDataJson)
                val success = getAppContext().ble().sendData(writeDataJson)
                if (!success) {
                    BleDebugger.logSecurityCheck("writeDimmerPort.sendData", false, "Failed to send dimmer port data to device (port: $portNo, value: $portValue)", false)
                    println("❌ Failed to send dimmer BLE - Port: $portNo, Value: $portValue")
                    return false
                }
                BleDebugger.logSecurityCheck("writeDimmerPort.sendData", true, "Dimmer BLE sent (port: $portNo, value: $portValue)", false)
                println("✅ Dimmer BLE sent - Port: $portNo, Value: $portValue")
                OutputUiHold.markDimmer(portNo, clampedValue)
                vanGoData.value = vanGoDataWithDimmerState(portNo, clampedValue)
                return finishBleWrite(true)
            } catch (e: Exception) {
                BleDebugger.logSecurityCheck("writeDimmerPort.jsonEncoding", false, "Exception during JSON encoding: ${e.message}", false)
                println("Error parsing JSON: ${e.message}")
                return false
            }
        }
        if (canSendRemoteCommands()) {
            val clamped = portValue.coerceIn(0, 100)
            val ok = dispatchRemoteCommand(
                "dimmer_set",
                com.hextech.vango.profile.HexnetCommandArgs.dimmerSet(portNo, clamped),
            ) {
                mqttTelemetryManagerRef.value?.publishDimmerCommand(portNo, clamped) == true
            }
            if (ok) {
                OutputUiHold.markDimmer(portNo, clamped)
                vanGoData.value = vanGoDataWithDimmerState(portNo, clamped)
            }
            return ok
        }
        println("writeDimmerPort skipped: BLE link not ready")
        return false
    }

    /**
     * Controls a digital I/O port on the VanGO device
     * @param portNo Port number to control (0-based indexing)
     * @param portValue true to turn on/high, false to turn off/low
     * @return true if command was sent successfully, false otherwise
     */
    fun writeDigitalPort(portNo: Int, portValue: Boolean):Boolean {
        /* BLE tercih; başarısızsa MQTT'ye düş (heap/GATT yazımı sessizce düşebiliyor). */
        if (isBleGattLinkReady() || isBleHardwareConnected()) {
            val value = if (portValue) 1 else 0
            val writeData = WriteData(
                MessageType = "Write",
                writeDataType = "Output",
                writeNo = portNo,
                writeData = value
            )
            try {
                val writeDataJson = vanGoJson.encodeToString(writeData)
                BleDebugger.logDigitalPortCommand(portNo, portValue)
                BleDebugger.logDataSend(writeDataJson)
                val success = getAppContext().ble().sendData(writeDataJson)
                println("data sent (BLE): $writeDataJson")
                if (success) {
                    OutputUiHold.markRelay(portNo, portValue)
                    vanGoData.value = vanGoDataWithRelayState(portNo, portValue)
                    vanGoGlobal.value.operationStatus = OperationState.Succeed
                    BleDebugger.logSecurityCheck("writeDigitalPort.sendData", true, "Digital BLE sent (port: $portNo, value: $portValue)", false)
                    println("✅ Digital BLE sent - Port: $portNo, Value: $portValue")
                    return finishBleWrite(true)
                }
                BleDebugger.logSecurityCheck("writeDigitalPort.sendData", false, "BLE failed — trying MQTT (port: $portNo, value: $portValue)", false)
                println("❌ BLE write failed — fallback MQTT port=$portNo")
            } catch (e: Exception) {
                BleDebugger.logSecurityCheck("writeDigitalPort.jsonEncoding", false, "Exception during JSON encoding: ${e.message}", false)
                println("Error parsing JSON: ${e.message}")
            }
        }
        if (canSendRemoteCommands()) {
            val ok = dispatchRemoteCommand(
                "relay_set",
                HexnetCommandArgs.relaySet(portNo, portValue),
            ) {
                mqttTelemetryManagerRef.value?.publishRelayCommand(portNo, portValue) == true
            }
            if (ok) {
                OutputUiHold.markRelay(portNo, portValue)
                vanGoData.value = vanGoDataWithRelayState(portNo, portValue)
            }
            return ok
        }
        println("writeDigitalPort skipped: BLE/MQTT not ready")
        vanGoGlobal.value.operationStatus = OperationState.Failed
        return false
    }

    /**
     * Writes RGB color values to the VanGO device
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return Boolean indicating success/failure
     */
    fun writeRGBPort(r: Int, g: Int, b: Int, isLight: Int):Boolean {
        val clampedR = r.coerceIn(0, 255)
        val clampedG = g.coerceIn(0, 255)
        val clampedB = b.coerceIn(0, 255)
        if (isBleGattLinkReady() || isBleHardwareConnected()) {
            val writeData = WriteDataRGB(
                MessageType = "Write",
                writeDataType = "RGB",
                writeNo = 0,
                writeData = intArrayOf(clampedR, clampedG, clampedB, isLight)
            )
            try {
                val writeDataJson = vanGoJson.encodeToString(writeData)
                BleDebugger.logDigitalPortCommand(0, true)
                BleDebugger.logDataSend(writeDataJson)
                val success = getAppContext().ble().sendData(writeDataJson)
                if (!success) {
                    BleDebugger.logSecurityCheck("writeRGBPort.sendData", false, "Failed to send RGB BLE (R: $clampedR, G: $clampedG, B: $clampedB)", false)
                    println("❌ Failed to send RGB BLE - R: $clampedR, G: $clampedG, B: $clampedB")
                    return false
                }
                BleDebugger.logSecurityCheck("writeRGBPort.sendData", true, "RGB BLE sent (R: $clampedR, G: $clampedG, B: $clampedB)", false)
                println("✅ RGB BLE sent - R: $clampedR, G: $clampedG, B: $clampedB")
                OutputUiHold.markRgbEnable(isLight != 0)
                vanGoData.value = vanGoDataWithRgbState(clampedR, clampedG, clampedB, isLight)
                return finishBleWrite(true)
            } catch (e: Exception) {
                BleDebugger.logSecurityCheck("writeRGBPort.jsonEncoding", false, "Exception during JSON encoding: ${e.message}", false)
                println("Error parsing JSON: ${e.message}")
                return false
            }
        }
        if (canSendRemoteCommands()) {
            val ok = dispatchRemoteCommand(
                "rgb_set",
                HexnetCommandArgs.rgbSet(r, g, b, isLight > 0),
            ) {
                mqttTelemetryManagerRef.value?.publishRgbCommand(r, g, b, isLight) == true
            }
            if (ok) {
                OutputUiHold.markRgbEnable(isLight != 0)
                vanGoData.value = vanGoDataWithRgbState(clampedR, clampedG, clampedB, isLight)
            }
            return ok
        }
        println("writeRGBPort skipped: BLE link not ready")
        return false
    }

    /**
     * Fetches current weather information and sends it to the VanGO device
     * Uses device location to get weather data from OpenWeatherMap API
     * Weather data includes temperature, description, and location information
     * See: https://openweathermap.org/weather-conditions#Weather-Condition-Codes-2
     */
    suspend fun sendWeatherInfo() {
        // Check if location permissions are granted before attempting to get location
        if (!hasLocationPermissions()) {
            println("⚠️ sendWeatherInfo: Location permissions NOT granted - skipping weather update")
            BleDebugger.logSecurityCheck("sendWeatherInfo.locationPermission", false, "Location permissions denied", false)
            return
        }
        println("✅ sendWeatherInfo: Location permissions granted")

        // Check BLE connection
        if (!isConnected()) {
            println("⚠️ sendWeatherInfo: BLE device NOT connected - skipping weather update")
            BleDebugger.logSecurityCheck("sendWeatherInfo.bleConnection", false, "BLE device not connected", false)
            return
        }
        println("✅ sendWeatherInfo: BLE device connected")

        val coordinates = getLocationUpdates()
        if(coordinates != null) {
            println("✅ sendWeatherInfo: Got coordinates: ${coordinates.first}, ${coordinates.second}")
            val w = WeatherService(WeatherApiKey).getWeather(coordinates.first, coordinates.second)
            if(w != null) {
                println("✅ sendWeatherInfo: Got weather data: ${w.city}, ${w.temperature}°C, ${w.description}")
                val weatherWrite = Json.encodeToString(
                    WeatherWrite(
                        MessageType = "Read",
                        Time = getFormattedTime(),
                        Date = getFormattedDate(),
                        Weather = w.description,
                        Temperature = w.temperature.toInt().toString(),
                        Location = w.city
                    )
                )
                BleDebugger.logWeatherSend(weatherWrite)
                val success = getAppContext().ble().sendWeatherData(weatherWrite)
                if (!success) {
                    BleDebugger.logSecurityCheck("sendWeatherInfo.sendWeatherData", false, "Failed to send weather data to device", false)
                    println("❌ Failed to send weather data to device")
                } else {
                    BleDebugger.logSecurityCheck("sendWeatherInfo.sendWeatherData", true, "Weather data sent successfully", false)
                    println("✅ Weather data sent successfully - Location: ${w.city}, Temp: ${w.temperature.toInt()}°C")
                }
            } else {
                BleDebugger.logSecurityCheck("sendWeatherInfo.weatherService", false, "Failed to fetch weather data from API", false)
                println("❌ Failed to fetch weather data from weather service")
            }
        } else {
            BleDebugger.logSecurityCheck("sendWeatherInfo.locationUpdate", false, "Failed to get location coordinates", false)
            println("Failed to fetch the location information")
        }
    }

    var lastUpdateTime = Clock.System.now().epochSeconds;
    var lastEnvMissingLogEpoch = 0L
    private fun onPackageReceive(data:String) {
        // Log raw received data with comprehensive debugging

        if(Clock.System.now().epochSeconds - lastUpdateTime > 10) {
            println("onPackageReceive: $data")
            lastUpdateTime = Clock.System.now().epochSeconds
        }
        BleDebugger.logIncomingRawData(data, "VanGO Device")
        
        try {
            val envFromJson = BleEnvSensorParser.parseFromJson(data)
            applyBleEnvReading(envFromJson)
            if (envFromJson == null && Clock.System.now().epochSeconds - lastEnvMissingLogEpoch > 10) {
                val preview = data.replace("\u0000", "").trim().take(180)
                println("BLE env not found in packet preview: $preview")
                lastEnvMissingLogEpoch = Clock.System.now().epochSeconds
            }
            extractWifiMacFromBleJson(data)?.let { normalizeAndCachePlatformWifiMac(it) }
            val safeData = sanitizeBleJson(data)
            val receivedData = vanGoJson.decodeFromString<VanGoData>(safeData)
            receivedData.mac?.trim()?.takeUnless { it.isEmpty() }?.let { normalizeAndCachePlatformWifiMac(it) }
            BleDebugger.logJsonParsing(data, true, isIncoming = true)
            
            // Validate the received data before using it
            val isValid = receivedData.isValid()
            BleDebugger.logVanGoDataValidation(receivedData, isValid)
            
            if (isValid) {
                receivedData.wifiMac?.let { normalizeAndCachePlatformWifiMac(it) }
                    ?: refreshWifiMacAfterBlePacket()
                val merged = mergeBleLiveTelemetry(vanGoData.value, receivedData)
                vanGoData.value = merged
                applyBleEnvReading(
                    envFromJson ?: BleEnvSensorParser.parseFromVanGoData(merged),
                )
                applyVanGoBleProfileToUi(receivedData)
                lastSuccessfulTelemetryEpochSec.value = getCurrentTimeInSeconds()
                BleDebugger.logBufferContents(receivedData)
                BleDebugger.logSecurityCheck("Data Processing", true, "VanGoData accepted and processed successfully", true)
//                println("✅ Valid VanGoData received and processed successfully")
            } else {
                applyBleEnvReading(envFromJson)
                BleDebugger.logSecurityCheck("Data Validation", false, "Buffer sizes don't match expected minimums - data rejected for security", true)
                println("❌ Invalid VanGoData received - buffer sizes don't match expected minimums:")
                println("   outNB: ${receivedData.outNB.size} (expected >= 16)")
                println("   dimNB: ${receivedData.dimNB.size} (expected >= 4)")
                println("   senNB: ${receivedData.senNB.size} (expected >= 5)")
                println("   dDB: ${receivedData.dDB.size} (expected >= 4)")
                println("   outDB: ${receivedData.outDB.size} (expected >= 9)")
                println("   sDB: ${receivedData.sDB.size} (expected >= 3)")
                println("   RGBDB: ${receivedData.RGBDB.size} (expected >= 4)")
                return
            }
        } catch (e: Exception) {
            BleDebugger.logJsonParsing(data, false, e.message, isIncoming = true)
            BleDebugger.logSecurityCheck("JSON Parsing", false, "Exception during parsing: ${e.message}", true)
            println("❌ Error parsing JSON: ${e.message}")
            println("   Raw data: $data")
        }
    }

    fun onBleCallback(state:VanGoState, data:Any?) {
        when(state) {
            VanGoState.BluetoothNotEnabled -> {
                vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.BluetoothNotEnabled)
            }

            VanGoState.PermissionDenied -> {
                vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.PermissionDenied)
                println("Permission denied: ${data as? String ?: "Unknown permission issue"}")
            }

            VanGoState.OnDiscoverDevice -> {
                val dev = data as BleDevice
                if (!isSupportedBleDeviceName(dev.name)) return
                if (vangoDeviceList.any { it.mac.equals(dev.mac, ignoreCase = true) }) {
                    // RSSI güncellemesi için mevcut kaydı canlı yap
                    val idx = vangoDeviceList.indexOfFirst { it.mac.equals(dev.mac, ignoreCase = true) }
                    if (idx >= 0 && dev.rssi != null && vangoDeviceList[idx].rssi == null) {
                        vangoDeviceList[idx] = vangoDeviceList[idx].copy(rssi = dev.rssi)
                        vangoDeviceListSize.value = vangoDeviceList.size
                        BleAutoConnect.tryAutoConnect("rssi-update")
                    }
                    return
                }
                val asBle = deriveBleMacFromWifiMac(dev.mac)
                if (asBle != null && !macsEqual(dev.mac, asBle) &&
                    vangoDeviceList.any { macsEqual(it.mac, asBle) }
                ) {
                    return
                }
                println("Found new ble device $dev  and List $vangoDeviceList")
                vangoDeviceList.add(dev)
                if (asBle != null && macsEqual(dev.mac, asBle)) {
                    val wifiTwinIdx = vangoDeviceList.indexOfFirst { d ->
                        val w = normalizeMacForDisplay(d.mac)
                        deriveBleMacFromWifiMac(w)?.let { macsEqual(it, asBle) } == true &&
                            !macsEqual(w, asBle)
                    }
                    if (wifiTwinIdx >= 0) vangoDeviceList.removeAt(wifiTwinIdx)
                }
                vangoDeviceListSize.value = vangoDeviceList.size
                BleAutoConnect.tryAutoConnect("discovered")
            }

            VanGoState.OnDiscoverDone -> {
                println("OnDiscoverDone")
                if (getAppContext().vanGO.isConnected() ||
                    vanGoGlobal.value.connectionState == BleGattConnectionChain.ConnectingGattServer ||
                    vanGoGlobal.value.connectionState == BleGattConnectionChain.DiscoveringServices ||
                    vanGoGlobal.value.connectionState == BleGattConnectionChain.ChainEnded
                ) {
                    println("OnDiscoverDone ignored — BLE link already established")
                    return
                }
                vanGoGlobal.value =
                    vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.DiscoverFinished)
                BleAutoConnect.tryAutoConnect("discover-done")
            }

            VanGoState.OnGatServerConnection -> {
                println("OnGatServerConnection connected=$data state=${vanGoGlobal.value.connectionState}")
                if (data as Boolean) {
                    if (getAppContext().ble().discoverServices()) {
                        vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.DiscoveringServices)
                    } else {
                        vanGoGlobal.value = vanGoGlobal.value.copy(connectionState = BleGattConnectionChain.UnexpectedError)
                    }
                } else {
                    // Donanım hâlâ bağlıysa (yeni GATT) failed yazma
                    if (getAppContext().vanGO.isConnected()) {
                        println("BLE disconnect event ignored — hardware still connected")
                        return
                    }
                    val state = vanGoGlobal.value.connectionState
                    val duringScan = state == BleGattConnectionChain.DiscoveringDevice ||
                        state == BleGattConnectionChain.DiscoverFinished
                    if (duringScan) {
                        println("BLE disconnect during scan — keeping discovery state")
                        return
                    }
                    vanGoGlobal.value = vanGoGlobal.value.copy(
                        connectionState = BleGattConnectionChain.ConnectingGattServerFailed,
                        currentMtu = 20,
                        isCharacteristicNotificationOn = false,
                    )
                    if (appConnectionState.value == AppConnectionState.BLE_CONNECTED &&
                        !remoteModeEnabled.value
                    ) {
                        appConnectionState.value = AppConnectionState.OFFLINE
                    }
                    println("🔄 Connection failed - reset MTU to 20 and notification state to false")
                }
            }
            VanGoState.OnServiceDiscovering -> {
                println("OnServiceDiscovering")
                if(data as Boolean) {
                    vanGoGlobal.value = vanGoGlobal.value.copy(
                        currentOperation = Operations.Idle, 
                        connectionState = BleGattConnectionChain.ChainEnded,
                        currentMtu = 20,
                        isCharacteristicNotificationOn = false
                    )
                    rememberLastBleAdapterMac(getAppContext().ble().connectedDeviceMac())
                    syncBleAppConnectionStateIfLinked()
                    println("✅ Connection established - reset MTU to 20 and notification state to false for initial setup")
                    pushPendingPanelConfigOverBle()
                } else {
                    // Service discovery failed - reset state for retry
                    vanGoGlobal.value = vanGoGlobal.value.copy(
                        connectionState = BleGattConnectionChain.ServicesDiscoveringFailed,
                        currentMtu = 20,
                        isCharacteristicNotificationOn = false
                    )
                    println("🔄 Service discovery failed - reset MTU to 20 and notification state to false")
                }
            }
            VanGoState.OnMtuConfigured -> {
                println("OnMtuConfigured")
                if(data as Int != 0) {
                    vanGoGlobal.value.currentMtu = data
                } else {
                    println("Failed to change mtu, now what ????? ")
                }
                vanGoGlobal.value.unlockOperation()
                // Let the ATT stack settle before CCCD write (avoids GATT_ERROR 133 on some devices).
                if (data as Int != 0) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(250)
                        if (vanGoGlobal.value.connectionState == BleGattConnectionChain.ChainEnded &&
                            !vanGoGlobal.value.isCharacteristicNotificationOn &&
                            vanGoGlobal.value.operation() == Operations.Idle
                        ) {
                            getAppContext().vanGO.enableCharacteristicNotification()
                        }
                    }
                }
            }
            VanGoState.OnCharacteristicNotification -> {
                // println("OnCharacteristicNotification")
                if(vanGoGlobal.value.operation() == Operations.EnableNotification) {
                    vanGoGlobal.value.unlockOperation()
                    val wasNotificationOn = vanGoGlobal.value.isCharacteristicNotificationOn
                    vanGoGlobal.value.isCharacteristicNotificationOn = true

                    // Send initial weather update when device first connects
                    if (!wasNotificationOn) {
                        clearStaleBleMacCacheIfNeeded()
                        syncBleAppConnectionStateIfLinked()
                        refreshWifiMacAfterBlePacket()
                        println("🌤️ Device fully connected - sending initial weather update")
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                sendWeatherInfo()
                            } catch (e: Exception) {
                                println("❌ Initial weather update failed: ${e.message}")
                            }
                        }
                    }
                }
                if(data == null) return
                val packetChunk = data as String
                val packets = ingestBleChunk(packetChunk)
                if (packets.isNotEmpty()) {
                    packets.forEach { onPackageReceive(it) }
                } else {
                    val trimmed = packetChunk.trim()
                    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                        onPackageReceive(trimmed)
                    }
                }
            }
            VanGoState.OnCharacteristicWrite -> {   
                println("OnCharacteristicWrite")
                if(data as Boolean) {
                    vanGoGlobal.value.operationStatus = OperationState.Succeed
                    // Write-without-response still confirms enqueue; treat relay/dim commands as done.
                    if (vanGoGlobal.value.operation() == Operations.WriteDataDevice) {
                        vanGoGlobal.value.unlockOperation()
                    }
                } else {
                    vanGoGlobal.value.operationStatus = OperationState.Failed
                }
            }
        }
    }

    fun typeOfString(intLabel:Int): Any {
        when(intLabel) {
            1 -> return Res.string.lamp
            2 -> return Res.string.toilet
            3 -> return Res.string.kitchen
            4 -> return Res.string.bedroom
            5 -> return Res.string.corridor
            6 -> return Res.string.step
            7 -> return Res.string.ac
            8 -> return Res.string.usb
            9 -> return Res.string.refrigerator
            10 -> return Res.string.water_p
            11 -> return Res.string.outlet
            12 -> return Res.string.oven
            13 -> return Res.string.tv
            14 -> return Res.string.ex_light
            15 -> return Res.string.ex_outlet
            16 -> return Res.string.heater
            17 -> return Res.string.spot
            18 -> return Res.string.reading_l
        }
        return Res.string.lamp
    }

    fun typeOfStringDim(intLabel:Int): Any {
        when(intLabel) {
            1 -> return Res.string.lamp
            2 -> return Res.string.toilet
            3 -> return Res.string.kitchen
            4 -> return Res.string.bedroom
            5 -> return Res.string.corridor
            6 -> return Res.string.step
            7 -> return Res.string.ex_light
            8 -> return Res.string.spot
        }
        return Res.string.lamp
    }

    fun getHexnetIcon(): DrawableResource {
        return Res.drawable.hexnet_mark
    }

    fun typeOfIcon(intLabel:Int): Any {
        when(intLabel) {
            1 -> return Res.drawable.lamp
            2 -> return Res.drawable.toilet
            3 -> return Res.drawable.lamp
            4 -> return Res.drawable.lamp
            5 -> return Res.drawable.lamp
            6 -> return Res.drawable.lamp
            7 -> return Res.drawable.ac
            8 -> return Res.drawable.usb
            9 -> return Res.drawable.refrigerator
            10 -> return Res.drawable.water
            11 -> return Res.drawable.outlet
            12 -> return Res.drawable.oven
            13 -> return Res.drawable.tv
            14 -> return Res.drawable.lamp
            15 -> return Res.drawable.outlet
            16 -> return Res.drawable.outlet
            17 -> return Res.drawable.lamp
            18 -> return Res.drawable.lamp // reading lamp olarak bir ikon var ama bana daha gelmedi!!!
        }
        return Res.drawable.lamp
    }
}

expect fun getPlatform(): Platform
expect fun getVanGO(): VanGO
