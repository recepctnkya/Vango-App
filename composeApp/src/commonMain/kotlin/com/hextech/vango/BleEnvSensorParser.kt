package com.hextech.vango

import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BleEnvSensorParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val tempKeys = listOf(
        "sensorTemp", "temperature_c", "temp", "temperature", "sensor_temp", "in_temp", "dht_temp",
        "live_api_temperature_c",
    )
    private val humKeys = listOf(
        "sensorHumidity", "humidity_pct", "humidity", "hum", "sensor_humidity", "in_humidity", "dht_hum",
        "live_api_humidity_pct",
    )
    private val nestedObjects = listOf(
        "latest", "sensors", "readings", "payload", "telemetry", "data", "env", "iot", "environment",
    )
    private val embeddedJsonKeys = listOf(
        "payload_json", "live_api_payload_json", "telemetry_json", "merged_payload_json",
    )

    fun parseFromJson(raw: String): BleEnvSensors? {
        val sanitized = sanitizeBleJsonForEnv(raw)
        if (sanitized.isEmpty()) return null
        val root = runCatching { json.parseToJsonElement(sanitized).jsonObject }.getOrNull()
            ?: return null

        parseEmbeddedJsonStrings(root)?.let { return it }
        parseFromChannelReadings(root)?.let { return it }

        val temp = pickInt(root, tempKeys)
            ?: root["sDB"]?.jsonArray?.getOrNull(0)?.let(::jsonElementToSensorInt)
            ?: parseFromSensorArray(root["sensors"]?.jsonArray, tempKeys)
            ?: parseThAsTemp(root["Th"])
            ?: deepPickInt(root, tempKeys)
        val hum = pickInt(root, humKeys)
            ?: root["sDB"]?.jsonArray?.getOrNull(1)?.let(::jsonElementToSensorInt)
            ?: parseFromSensorArray(root["sensors"]?.jsonArray, humKeys)
            ?: deepPickInt(root, humKeys)

        val fromHexnet = parseFromHexnetProtocol(sanitized)
        val resolvedTemp = temp ?: fromHexnet?.tempC
        val resolvedHum = hum ?: fromHexnet?.humidityPct

        val tempOk = resolvedTemp?.takeIf { it in 1..125 }
        val humOk = resolvedHum?.takeIf { it in 1..100 }
        if (tempOk == null && humOk == null) return null
        return BleEnvSensors(
            tempC = tempOk,
            humidityPct = humOk,
        )
    }

    fun resolveDisplayTempC(env: BleEnvSensors, data: VanGoData): Int =
        env.tempC?.takeIf { it > 0 }
            ?: data.sensorTemp?.takeIf { it > 0 }
            ?: data.getSafeSensorsDataUnsigned(0)

    fun resolveDisplayHumidityPct(env: BleEnvSensors, data: VanGoData): Int =
        env.humidityPct?.takeIf { it > 0 }
            ?: data.sensorHumidity?.takeIf { it > 0 }
            ?: data.getSafeSensorsDataUnsigned(1)

    fun parseFromVanGoData(data: VanGoData): BleEnvSensors? {
        val temp = data.sensorTemp?.takeIf { it in 1..125 }
            ?: parseThAsTempFromInt(data.Th)
            ?: data.getSafeSensorsDataUnsigned(0).takeIf { it in 1..125 }
        val hum = data.sensorHumidity?.takeIf { it in 1..100 }
            ?: data.getSafeSensorsDataUnsigned(1).takeIf { it in 1..100 }
        if (temp == null && hum == null) return null
        return BleEnvSensors(
            tempC = temp,
            humidityPct = hum,
        )
    }

    private fun parseEmbeddedJsonStrings(root: JsonObject): BleEnvSensors? {
        for (key in embeddedJsonKeys) {
            val inner = root[key]?.jsonPrimitive?.content?.trim().orEmpty()
            if (inner.startsWith("{")) {
                parseFromJson(inner)?.let { return it }
            }
        }
        return null
    }

    private fun parseFromChannelReadings(root: JsonObject): BleEnvSensors? {
        val channels = root["channels"]?.jsonObject ?: return null
        val sensors = channels["sensors"]?.jsonArray ?: return null
        var temp: Int? = null
        var hum: Int? = null
        for (item in sensors) {
            val obj = item as? JsonObject ?: continue
            val type = obj["sensor_type"]?.jsonPrimitive?.content
                ?: obj["sensorType"]?.jsonPrimitive?.content
            if (type != null && type != "temp_humidity") continue
            temp = temp ?: pickInt(obj, tempKeys)
            hum = hum ?: pickInt(obj, humKeys)
        }
        if (temp == null && hum == null) return null
        return BleEnvSensors(tempC = temp, humidityPct = hum)
    }

    private fun parseFromHexnetProtocol(raw: String): BleEnvSensors? {
        val tel = RemoteTelemetryParser.parse(raw) ?: return null
        val temp = tel.sensors.temperatureC.takeIf { it > 0 }
        val hum = tel.sensors.humidityPct.takeIf { it > 0 }
        if (temp == null && hum == null) return null
        return BleEnvSensors(tempC = temp, humidityPct = hum)
    }

    private fun parseFromSensorArray(array: JsonArray?, keys: List<String>): Int? {
        if (array == null) return null
        for (item in array) {
            val obj = item as? JsonObject ?: continue
            for (key in keys) {
                jsonElementToSensorInt(obj[key])?.let { return it }
            }
        }
        return null
    }

    private fun parseThAsTemp(el: JsonElement?): Int? {
        val value = jsonElementToSensorInt(el) ?: return null
        return parseThAsTempFromInt(value)
    }

    private fun parseThAsTempFromInt(value: Int): Int? =
        value.takeIf { it in 10..50 }

    private fun deepPickInt(element: JsonElement?, keys: List<String>): Int? {
        when (element) {
            is JsonObject -> {
                for (key in keys) {
                    jsonElementToSensorInt(element[key])?.let { return it }
                }
                for (value in element.values) {
                    deepPickInt(value, keys)?.let { return it }
                }
            }
            is JsonArray -> {
                for (item in element) {
                    deepPickInt(item, keys)?.let { return it }
                }
            }
            else -> {}
        }
        return null
    }

    internal fun jsonElementToSensorInt(el: JsonElement?): Int? {
        val primitive = el as? JsonPrimitive ?: return null
        primitive.intOrNull?.let { return it }
        return primitive.content.toDoubleOrNull()?.roundToInt()
    }

    private fun pickInt(root: JsonObject, keys: List<String>): Int? {
        for (key in keys) {
            jsonElementToSensorInt(root[key])?.let { return it }
        }
        for (nestedKey in nestedObjects) {
            when (val nestedEl = root[nestedKey]) {
                is JsonObject -> {
                    for (key in keys) {
                        jsonElementToSensorInt(nestedEl[key])?.let { return it }
                    }
                }
                is JsonArray -> parseFromSensorArray(nestedEl, keys)?.let { return it }
                else -> {}
            }
        }
        return null
    }

    private fun sanitizeBleJsonForEnv(raw: String): String {
        val trimmed = raw.replace("\u0000", "").trim()
        if (trimmed.isEmpty()) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1)
        }
        return trimmed
    }
}