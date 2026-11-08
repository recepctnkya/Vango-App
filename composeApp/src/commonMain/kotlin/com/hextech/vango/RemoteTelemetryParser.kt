package com.hextech.vango

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object RemoteTelemetryParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val latestFoldKeys = listOf(
        "water_pct",
        "outputs_mask",
        "rgb",
        "dim",
        "relay",
        "relays",
        "dimmer",
        "dimmers",
        "tank",
        "tanks",
        "water_tanks",
        "inputs",
        "digital_inputs",
        "mqtt_status",
        "online_status",
        "temperature_c",
        "humidity_pct",
        "ts",
        "protocol_version",
    )

    fun parse(payload: String): RemoteTelemetry? {
        val root = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonObject
            ?: return runCatching { json.decodeFromString<RemoteTelemetry>(payload) }.getOrNull()

        val telemetryEl = root["telemetry"] as? JsonObject
        val dataEl = root["data"] as? JsonObject
        val latestEl = root["latest"] as? JsonObject

        val baseObject = when {
            telemetryEl != null -> telemetryEl
            dataEl != null -> dataEl
            else -> root
        }

        val base = runCatching {
            json.decodeFromJsonElement(RemoteTelemetry.serializer(), baseObject)
        }.getOrNull() ?: return null

        val mergedPayload = mergePayload(
            telemetryPayload = telemetryEl?.get("payload") ?: baseObject["payload"],
            latest = latestEl ?: root["latest"] as? JsonObject,
            payloadJson = root["payload_json"]?.jsonPrimitive?.content
                ?: root["live_api_payload_json"]?.jsonPrimitive?.content,
            rootDim = root["dim"] ?: baseObject["dim"],
            rootDimmers = root["dimmers"] ?: baseObject["dimmers"],
        )

        val withDim = applyMergedDim(base, mergedPayload)
        return applyMergedEnv(withDim, mergedPayload, root, latestEl)
    }

    internal fun mergePayload(
        telemetryPayload: JsonElement?,
        latest: JsonObject?,
        payloadJson: String?,
        rootDim: JsonElement? = null,
        rootDimmers: JsonElement? = null,
    ): JsonObject {
        val merged = linkedMapOf<String, JsonElement>()

        fun putAllFrom(obj: JsonObject?) {
            obj?.forEach { (key, value) -> merged[key] = value }
        }

        putAllFrom(telemetryPayload as? JsonObject)

        latest?.forEach { (key, value) ->
            if (key in latestFoldKeys && value !is JsonObject) {
                merged[key] = value
            }
        }

        payloadJson?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            runCatching { json.parseToJsonElement(raw) }.getOrNull()?.jsonObject?.let { putAllFrom(it) }
        }

        rootDim?.let { merged["dim"] = it }
        rootDimmers?.let { merged["dimmers"] = it }

        return JsonObject(merged)
    }

    internal fun applyMergedDim(telemetry: RemoteTelemetry, mergedPayload: JsonObject): RemoteTelemetry {
        val dimPercent = intListFrom(mergedPayload["dim"])
        if (dimPercent != null) {
            return telemetry.copy(dim = dimPercent)
        }
        return telemetry
    }

    internal fun applyMergedEnv(
        telemetry: RemoteTelemetry,
        mergedPayload: JsonObject,
        root: JsonObject,
        latest: JsonObject?,
    ): RemoteTelemetry {
        val temp = intFromJson(mergedPayload["temperature_c"])
            ?: intFromJson(mergedPayload["temp"])
            ?: intFromJson(mergedPayload["temperature"])
            ?: intFromJson(root["live_api_temperature_c"])
            ?: intFromJson(latest?.get("temperature_c"))
        val hum = intFromJson(mergedPayload["humidity_pct"])
            ?: intFromJson(mergedPayload["humidity"])
            ?: intFromJson(root["live_api_humidity_pct"])
            ?: intFromJson(latest?.get("humidity_pct"))
        if (temp == null && hum == null) return telemetry
        return telemetry.copy(
            sensors = telemetry.sensors.copy(
                temperatureC = temp?.takeIf { it > 0 } ?: telemetry.sensors.temperatureC,
                humidityPct = hum?.takeIf { it > 0 } ?: telemetry.sensors.humidityPct,
            ),
        )
    }

    internal fun intFromJson(element: JsonElement?): Int? {
        val primitive = element as? JsonPrimitive ?: return null
        primitive.intOrNull?.let { return it }
        return primitive.content.toDoubleOrNull()?.toInt()
    }

    internal fun intListFrom(element: JsonElement?): List<Int>? {
        val array = element as? JsonArray ?: return null
        if (array.isEmpty()) return null
        return array.mapNotNull { item ->
            runCatching { item.jsonPrimitive.intOrNull }.getOrNull()
        }.takeIf { it.isNotEmpty() }
    }
}