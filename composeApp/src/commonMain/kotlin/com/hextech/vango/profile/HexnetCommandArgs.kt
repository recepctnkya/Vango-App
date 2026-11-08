package com.hextech.vango.profile

import com.hextech.vango.DimmerScale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** myhexnet panel `mqtt-command.php` args biçimleri (app.js ile uyumlu). */
object HexnetCommandArgs {
    fun relaySet(relayIndex: Int, on: Boolean): JsonObject = buildJsonObject {
        put("relay", relayIndex.coerceIn(0, 15))
        put("state", if (on) 1 else 0)
    }

    /** Panel dimmer: `value` 0–255, `channel` port index. */
    fun dimmerSet(channel: Int, percent0to100: Int): JsonObject = buildJsonObject {
        put("channel", channel)
        put("value", DimmerScale.percentToRaw255(percent0to100))
    }

    /** Panel RGB: G/B kanal sırası ters (IO modülü). */
    fun rgbSet(r: Int, g: Int, b: Int, lightOn: Boolean): JsonObject = buildJsonObject {
        put("r", r.coerceIn(0, 255))
        put("g", b.coerceIn(0, 255))
        put("b", g.coerceIn(0, 255))
        put("enable", lightOn)
    }

    /** Panel / IO modül konfigürasyonu — BLE `Configuration` ile aynı alanlar. */
    fun ioConfigSet(configJson: String): JsonObject {
        val root = runCatching { Json.parseToJsonElement(configJson) }.getOrNull()
        if (root !is JsonObject) {
            return buildJsonObject { put("config_raw", configJson) }
        }
        return buildJsonObject {
            root.forEach { (key, value) -> put(key, value) }
            put(
                "links",
                buildJsonObject {
                    put("remote", JsonPrimitive(true))
                    put("ble", JsonPrimitive(false))
                    put("display", JsonPrimitive(false))
                },
            )
        }
    }
}
