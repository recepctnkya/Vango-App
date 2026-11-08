package com.hextech.vango

import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * myhexnet panelinde kayıtlı MAC, cihazın **WiFi (STA) MAC** adresidir; BLE adaptör MAC ile aynı değildir.
 * ESP32/ESP8266: STA son okteti genelde BLE adaptöründen 2 küçüktür (ör. …:40 WiFi, …:42 BLE).
 */
val vanGoDeviceWifiMac = MutableStateFlow<String?>(null)

private val macJson = Json { ignoreUnknownKeys = true }

/** BLE public adresi ile STA (WiFi) arasındaki tipik son-oktet farkı. */
private const val ESP_BLE_TO_STA_LAST_OCTET_DELTA = 2

private val wifiMacJsonKeys = arrayOf(
    "wifi_mac", "wifiMac", "mac_wifi", "wifimac", "WiFiMAC", "WIFI_MAC",
    "sta_mac", "STA_MAC", "wlan_mac", "WLAN_MAC", "macWifi", "MacWifi",
)

fun normalizeAndCachePlatformWifiMac(raw: String?) {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed == "-" || trimmed == "\u2014") return
    val normalized = normalizeMacForDisplay(trimmed)
    if (normalized.length != 17 || !isValidMacFormat(normalized)) return
    val ble = currentBleAdapterMac()
    if (ble != null && macsEqual(normalized, ble)) {
        deriveWifiMacFromBleAdapterMac(ble)?.let { sta ->
            vanGoDeviceWifiMac.value = sta
            LoginPreferences.setCachedPairedDeviceMac(sta)
        }
        return
    }
    vanGoDeviceWifiMac.value = normalized
    LoginPreferences.setCachedPairedDeviceMac(normalized)
}

/**
 * Platform UID / resolve-by-mac / kayıt için kullanılacak MAC (WiFi).
 * BLE adaptör adresi bilinçli olarak kullanılmaz.
 */
fun currentPlatformDeviceMac(): String? {
    vanGoDeviceWifiMac.value?.trim()?.takeUnless { it.isEmpty() }?.let { return it }
    vanGoData.value.wifiMac?.trim()?.takeUnless { it.isEmpty() }?.let { raw ->
        val normalized = normalizeMacForDisplay(raw)
        if (normalized.length == 17 && isValidMacFormat(normalized)) {
            return normalized
        }
    }
    LoginPreferences.getCachedPairedDeviceMac()?.trim()?.takeUnless {
        it.isEmpty() || it == "-" || it == "\u2014"
    }?.let { cached ->
        val normalized = normalizeMacForDisplay(cached)
        if (normalized.length == 17 && isValidMacFormat(normalized)) {
            val ble = currentBleAdapterMac()
            if (ble == null || !macsEqual(normalized, ble)) {
                return normalized
            }
        }
    }
    remoteTelemetry.value?.device?.mac?.trim()?.takeUnless { it.isEmpty() }?.let { mac ->
        val normalized = normalizeMacForDisplay(mac)
        if (normalized.length == 17 && isValidMacFormat(normalized)) {
            val ble = currentBleAdapterMac()
            if (ble == null || !macsEqual(normalized, ble)) {
                return normalized
            }
        }
    }
    currentBleAdapterMac()?.let { deriveWifiMacFromBleAdapterMac(it) }
    return null
}

/** Bağlantı ekranında gösterim için BLE adaptör MAC (platform sorgusu değil). */
fun currentBleAdapterMac(): String? {
    val fromBle = getAppContext().ble().connectedDeviceMac()?.trim()?.takeIf { it.isNotEmpty() }
        ?: vangoDeviceList.firstOrNull()?.mac?.trim()?.takeIf { it.isNotEmpty() }
    return fromBle?.let { normalizeMacForDisplay(it) }
        ?.takeIf { it.length == 17 && isValidMacFormat(it) }
}

/**
 * BLE JSON veya bağlı adaptör adresinden WiFi (STA) MAC çıkarır.
 * Genel `mac` alanı yalnızca BLE adaptörü ile aynıysa STA'ya dönüştürülür (42 → 40).
 */
fun extractWifiMacFromBleJson(raw: String): String? {
    val root = runCatching {
        macJson.parseToJsonElement(sanitizeBleJsonForMac(raw)).jsonObject
    }.getOrNull() ?: return null

    fun JsonObject.pickMac(vararg keys: String): String? {
        for (key in keys) {
            val v = this[key]?.jsonPrimitive?.content?.trim()
            if (!v.isNullOrEmpty()) return v
        }
        return null
    }

    fun JsonObject.pickWifiMacOnly(): String? {
        val candidate = pickMac(*wifiMacJsonKeys)
        return candidate?.let { normalizeMacForDisplay(it) }
            ?.takeIf { it.length == 17 && isValidMacFormat(it) }
    }

    val explicitWifi = listOfNotNull(
        root.pickWifiMacOnly(),
        root["wifi"]?.jsonObject?.pickWifiMacOnly()
            ?: root["wifi"]?.jsonObject?.pickMac("mac")?.let { normalizeMacForDisplay(it) }
                ?.takeIf { it.length == 17 && isValidMacFormat(it) },
        root["sta"]?.jsonObject?.pickWifiMacOnly()
            ?: root["sta"]?.jsonObject?.pickMac("mac")?.let { normalizeMacForDisplay(it) }
                ?.takeIf { it.length == 17 && isValidMacFormat(it) },
        root["device"]?.jsonObject?.pickWifiMacOnly(),
    ).firstOrNull { mac ->
        val ble = currentBleAdapterMac()
        ble == null || !macsEqual(mac, ble)
    }
    if (explicitWifi != null) return explicitWifi

    val bleFromJson = listOfNotNull(
        root.pickMac("ble_mac", "bleMac", "ble_mac_addr", "BLE_MAC"),
        root["device"]?.jsonObject?.pickMac("ble_mac", "bleMac"),
    ).firstOrNull()?.let { normalizeMacForDisplay(it) }
        ?.takeIf { it.length == 17 && isValidMacFormat(it) }

    bleFromJson?.let { deriveWifiMacFromBleAdapterMac(it) }?.let { return it }

    val genericMac = root.pickMac("mac", "MAC")?.let { normalizeMacForDisplay(it) }
        ?.takeIf { it.length == 17 && isValidMacFormat(it) }
    val bleAdapter = currentBleAdapterMac()
    if (genericMac != null) {
        if (bleAdapter != null && macsEqual(genericMac, bleAdapter)) {
            return deriveWifiMacFromBleAdapterMac(genericMac)
        }
        // IO modül v0-40+: "mac" doğrudan Wi‑Fi STA
        return genericMac
    }

    return null
}

/** BLE adaptör MAC'inden STA (WiFi) MAC türetir (son oktet −2). */
fun deriveWifiMacFromBleAdapterMac(bleMac: String): String? {
    val normalized = normalizeMacForDisplay(bleMac)
    if (normalized.length != 17 || !isValidMacFormat(normalized)) return null
    val parts = normalized.split(':')
    if (parts.size != 6) return null
    val last = parts[5].toIntOrNull(16) ?: return null
    val staLast = (last - ESP_BLE_TO_STA_LAST_OCTET_DELTA) and 0xFF
    val staHex = staLast.toString(16).uppercase().padStart(2, '0')
    return parts.subList(0, 5).joinToString(":") + ":" + staHex
}

/** WiFi (STA) MAC'inden BLE adaptör MAC türetir (son oktet +2). */
fun deriveBleMacFromWifiMac(wifiMac: String): String? {
    val normalized = normalizeMacForDisplay(wifiMac)
    if (normalized.length != 17 || !isValidMacFormat(normalized)) return null
    val parts = normalized.split(':')
    if (parts.size != 6) return null
    val last = parts[5].toIntOrNull(16) ?: return null
    val bleLast = (last + ESP_BLE_TO_STA_LAST_OCTET_DELTA) and 0xFF
    val bleHex = bleLast.toString(16).uppercase().padStart(2, '0')
    return parts.subList(0, 5).joinToString(":") + ":" + bleHex
}

/** Taramada görülen BLE MAC varsa onu kullan; WiFi STA ise BLE'ye çevir. */
fun resolveBleConnectMac(inputMac: String, discoveredBleMacs: Collection<String>): String {
    val normalized = normalizeMacForDisplay(inputMac)
    if (normalized.length != 17 || !isValidMacFormat(normalized)) return normalized

    // 1) Tarama listesinde birebir varsa BLE adresidir — dokunma
    discoveredBleMacs.map { normalizeMacForDisplay(it) }
        .firstOrNull { macsEqual(it, normalized) }
        ?.let { return it }

    // 2) Son bilinen BLE adaptör MAC ise yine dokunma (+2 uygulama)
    LoginPreferences.getCachedLastBleAdapterMac()
        ?.let(::normalizeMacForDisplay)
        ?.takeIf { macsEqual(it, normalized) }
        ?.let { return it }

    // 3) Yalnızca bilinen WiFi (STA) MAC ise BLE'ye (+2) çevir
    val knownWifi = currentPlatformDeviceMac()
        ?: LoginPreferences.getCachedPairedDeviceMac()?.let(::normalizeMacForDisplay)
    if (knownWifi != null && macsEqual(normalized, knownWifi)) {
        deriveBleMacFromWifiMac(normalized)?.let { derived ->
            discoveredBleMacs.map { normalizeMacForDisplay(it) }
                .firstOrNull { macsEqual(it, derived) }
                ?.let { return it }
            return derived
        }
    }

    // 4) Zaten BLE adresi varsay — +2 YAPMA (eski bug: çift +2 → yanlış MAC / status 147)
    return normalized
}

/**
 * Bağlantı için tek BLE adaptör MAC döner.
 * WiFi→BLE dönüşümü yalnızca giriş gerçekten STA MAC ise yapılır.
 */
fun bleConnectMacCandidates(inputMac: String, discoveredBleMacs: Collection<String>): List<String> {
    val primary = resolveBleConnectMac(inputMac, discoveredBleMacs)
    return listOf(primary)
}

fun macsEqual(a: String, b: String): Boolean =
    normalizeMacForDisplay(a).equals(normalizeMacForDisplay(b), ignoreCase = true)

/**
 * Önbellekte kalan BLE adaptör MAC'ini temizler; yeni WiFi MAC BLE paketinden gelince yazılır.
 */
fun clearStaleBleMacCacheIfNeeded() {
    val ble = currentBleAdapterMac() ?: return
    val cached = LoginPreferences.getCachedPairedDeviceMac()?.trim().orEmpty()
    if (cached.isEmpty()) return
    if (macsEqual(cached, ble)) {
        LoginPreferences.setCachedPairedDeviceMac(null)
        if (vanGoDeviceWifiMac.value?.let { macsEqual(it, ble) } != false) {
            vanGoDeviceWifiMac.value = null
        }
    }
}

fun refreshWifiMacAfterBlePacket() {
    val fromState = vanGoData.value.wifiMac?.trim()?.takeUnless { it.isEmpty() }
    if (fromState != null) {
        normalizeAndCachePlatformWifiMac(fromState)
        return
    }
    currentBleAdapterMac()?.let { ble ->
        deriveWifiMacFromBleAdapterMac(ble)?.let { normalizeAndCachePlatformWifiMac(it) }
    }
}

private fun sanitizeBleJsonForMac(raw: String): String {
    val trimmed = raw.replace("\u0000", "").trim()
    if (trimmed.isEmpty()) return trimmed
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
}
