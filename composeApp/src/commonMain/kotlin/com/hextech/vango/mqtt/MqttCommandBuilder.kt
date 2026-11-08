package com.hextech.vango.mqtt

import com.hextech.vango.DimmerScale
import com.hextech.vango.getCurrentTimeInSeconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object MqttCommandBuilder {
    /** Panel `command.php` / cihaz MQTT: `{ cmd, args }`. */
    private fun panelCommand(cmd: String, args: JsonObject): String =
        buildJsonObject {
            put("cmd", cmd)
            put("args", args)
        }.toString()

    fun relaySet(index: Int, state: Boolean): String {
        val safeIndex = index.coerceIn(0, 15)
        return panelCommand(
            "relay_set",
            buildJsonObject {
                put("relay", safeIndex)
                put("state", if (state) 1 else 0)
            },
        )
    }

    fun dimmerSet(channel: Int, percent0to100: Int): String {
        val pct = percent0to100.coerceIn(0, 100)
        return panelCommand(
            "dimmer_set",
            buildJsonObject {
                put("channel", channel)
                put("value", DimmerScale.percentToRaw255(pct))
            },
        )
    }

    fun rgbSet(r: Int, g: Int, b: Int, isLight: Int): String {
        val rr = r.coerceIn(0, 255)
        val gg = g.coerceIn(0, 255)
        val bb = b.coerceIn(0, 255)
        return panelCommand(
            "rgb_set",
            buildJsonObject {
                put("r", rr)
                put("g", gg)
                put("b", bb)
            },
        )
    }

    fun configSet(configJson: String): String {
        val configElement = runCatching { Json.parseToJsonElement(configJson) }.getOrNull()
        return envelope("io_config_set") {
            if (configElement is JsonObject) {
                configElement.forEach { (key, value) -> put(key, value) }
                put(
                    "links",
                    buildJsonObject {
                        put("remote", JsonPrimitive(true))
                        put("ble", JsonPrimitive(false))
                        put("display", JsonPrimitive(false))
                    },
                )
            } else {
                put("config_raw", configJson)
            }
        }
    }

    private fun envelope(cmd: String, block: JsonObjectBuilder.() -> Unit = {}): String =
        buildJsonObject {
            put("protocol", "hexnet-io")
            put("version", "1.0")
            put("ts", getCurrentTimeInSeconds())
            put("cmd", cmd)
            put("request_id", "mobile-${getCurrentTimeInSeconds()}")
            block()
        }.toString()
}
