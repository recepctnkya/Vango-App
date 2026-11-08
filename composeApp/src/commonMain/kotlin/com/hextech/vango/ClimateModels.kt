package com.hextech.vango

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Geçici: iklimlendirme UI testinde 3 ortam + 1 fan zorla gösterilir. */
const val CLIMATE_FORCE_TEST_PRESETS = false

private val FAN_ICONS = setOf("fan", "ac", "wind")
private const val RELAY_AC_TYPE_ID = 7

enum class ClimateFanKind { Dimmer, Relay }

data class ClimateFanTarget(
    val kind: ClimateFanKind,
    val index: Int,
    val name: String,
    val subtitle: String,
    /** true: cihaz yazılmaz, yalnızca UI / animasyon testi. */
    val demoOnly: Boolean = false,
)

data class ClimateZoneCard(
    val id: Int,
    val title: String,
    val kind: ClimateZoneKind,
    val tempC: Float,
    val humidityPct: Int,
    /** false when sensor/weather reading is unavailable (do not show 0°C as real). */
    val hasReading: Boolean = true,
)

enum class ClimateZoneKind { Indoor, Outdoor, Bedroom, Other }

private val _climateWeatherCache = MutableStateFlow<WeatherInfo?>(null)
val climateWeatherCache: StateFlow<WeatherInfo?> = _climateWeatherCache.asStateFlow()

fun updateClimateWeatherCache(info: WeatherInfo?) {
    _climateWeatherCache.value = info
}

fun isClimateFanIcon(icon: String): Boolean =
    icon.trim().lowercase() in FAN_ICONS

fun discoverClimateFans(
    data: VanGoData,
    channels: PanelPlatformChannels?,
): List<ClimateFanTarget> {
    if (CLIMATE_FORCE_TEST_PRESETS) {
        return listOf(
            ClimateFanTarget(
                kind = ClimateFanKind.Dimmer,
                index = 0,
                name = "FAN 1",
                subtitle = "Dimmer",
                demoOnly = true,
            ),
        )
    }

    val result = mutableListOf<ClimateFanTarget>()
    val dimmers = channels?.dimmers.orEmpty()
    dimmers.filter { it.enabled && isClimateFanIcon(it.icon) }.forEach { ch ->
        if (ch.id in 0 until data.numOfD) {
            result += ClimateFanTarget(
                kind = ClimateFanKind.Dimmer,
                index = ch.id,
                name = ch.name.ifBlank { "FAN ${ch.id + 1}" },
                subtitle = "Dimmer",
            )
        }
    }
    val relays = channels?.relays.orEmpty()
    relays.filter { it.enabled && isClimateFanIcon(it.icon) }.forEach { ch ->
        if (ch.id in 0 until data.numOfO) {
            result += ClimateFanTarget(
                kind = ClimateFanKind.Relay,
                index = ch.id,
                name = ch.name.ifBlank { "FAN ${ch.id + 1}" },
                subtitle = "Relay",
            )
        }
    }
    if (result.none { it.kind == ClimateFanKind.Relay }) {
        data.outNB.indices.firstOrNull { i ->
            i < data.numOfO && (data.outNB.getOrElse(i) { 0 }.toInt() and 0xFF) == RELAY_AC_TYPE_ID
        }?.let { idx ->
            result += ClimateFanTarget(
                kind = ClimateFanKind.Relay,
                index = idx,
                name = "FAN ${idx + 1}",
                subtitle = "AC",
            )
        }
    }
    return result.distinctBy { "${it.kind}:${it.index}" }
}

fun discoverClimateTempZones(
    data: VanGoData,
    env: BleEnvSensors,
    channels: PanelPlatformChannels?,
    outdoorSource: ClimateOutdoorSource,
    weather: WeatherInfo?,
): List<ClimateZoneCard> {
    val indoorTemp = BleEnvSensorParser.resolveDisplayTempC(env, data).toFloat()
    val indoorHum = BleEnvSensorParser.resolveDisplayHumidityPct(env, data)

    if (CLIMATE_FORCE_TEST_PRESETS) {
        val outdoor = resolveOutdoorReading(outdoorSource, weather, indoorTemp, indoorHum)
        return listOf(
            ClimateZoneCard(
                id = 0,
                title = defaultZoneTitle(ClimateZoneKind.Indoor),
                kind = ClimateZoneKind.Indoor,
                tempC = if (indoorTemp > 0f) indoorTemp else demoTempFor(ClimateZoneKind.Indoor),
                humidityPct = if (indoorHum > 0) indoorHum else demoHumFor(ClimateZoneKind.Indoor),
                hasReading = true,
            ),
            ClimateZoneCard(
                id = 1,
                title = defaultZoneTitle(ClimateZoneKind.Outdoor),
                kind = ClimateZoneKind.Outdoor,
                tempC = outdoor.first,
                humidityPct = outdoor.second,
                hasReading = outdoor.third,
            ),
            ClimateZoneCard(
                id = 2,
                title = defaultZoneTitle(ClimateZoneKind.Bedroom),
                kind = ClimateZoneKind.Bedroom,
                tempC = demoTempFor(ClimateZoneKind.Bedroom),
                humidityPct = demoHumFor(ClimateZoneKind.Bedroom),
                hasReading = true,
            ),
        )
    }

    val sensors = channels?.sensors
        ?.filter { it.enabled && normalizePanelSensorType(it.sensorType) == "temp_humidity" }
        ?.sortedBy { it.sortOrder }
        .orEmpty()

    val indoorHas = indoorTemp > 0f || indoorHum > 0
    if (sensors.isEmpty()) {
        return listOf(
            ClimateZoneCard(
                id = 0,
                title = defaultZoneTitle(ClimateZoneKind.Indoor),
                kind = ClimateZoneKind.Indoor,
                tempC = indoorTemp.coerceAtLeast(0f),
                humidityPct = indoorHum.coerceAtLeast(0),
                hasReading = indoorHas,
            ),
        )
    }

    return sensors.mapIndexed { order, sensor ->
        val kind = climateZoneKindFor(sensor, order)
        when (kind) {
            ClimateZoneKind.Outdoor -> {
                val outdoor = resolveOutdoorReading(
                    outdoorSource = outdoorSource,
                    weather = weather,
                    indoorTemp = indoorTemp,
                    indoorHum = indoorHum,
                )
                ClimateZoneCard(
                    id = sensor.id,
                    title = sensor.name.ifBlank { defaultZoneTitle(kind) }.uppercase(),
                    kind = kind,
                    tempC = outdoor.first,
                    humidityPct = outdoor.second,
                    hasReading = outdoor.third,
                )
            }
            else -> ClimateZoneCard(
                id = sensor.id,
                title = sensor.name.ifBlank { defaultZoneTitle(kind) }.uppercase(),
                kind = kind,
                tempC = indoorTemp.coerceAtLeast(0f),
                humidityPct = indoorHum.coerceAtLeast(0),
                hasReading = indoorHas,
            )
        }
    }
}

private fun climateZoneKindFor(sensor: PanelPlatformChannel, order: Int): ClimateZoneKind {
    val n = sensor.name.lowercase()
    return when {
        n.contains("dış") || n.contains("dis") || n.contains("out") || n.contains("ext") ->
            ClimateZoneKind.Outdoor
        n.contains("yatak") || n.contains("bed") || n.contains("room") ->
            ClimateZoneKind.Bedroom
        n.contains("iç") || n.contains("ic") || n.contains("in") || order == 0 ->
            ClimateZoneKind.Indoor
        order == 1 -> ClimateZoneKind.Outdoor
        order == 2 -> ClimateZoneKind.Bedroom
        else -> ClimateZoneKind.Other
    }
}

private fun resolveOutdoorReading(
    outdoorSource: ClimateOutdoorSource,
    weather: WeatherInfo?,
    indoorTemp: Float,
    indoorHum: Int,
): Triple<Float, Int, Boolean> {
    if (outdoorSource == ClimateOutdoorSource.Location && weather != null) {
        return Triple(weather.temperature, weather.humidity.coerceIn(0, 100), true)
    }
    return Triple(0f, 0, false)
}

private fun defaultZoneTitle(kind: ClimateZoneKind): String = when (kind) {
    ClimateZoneKind.Indoor -> "İÇ ORTAM"
    ClimateZoneKind.Outdoor -> "DIŞ ORTAM"
    ClimateZoneKind.Bedroom -> "YATAK ODASI"
    ClimateZoneKind.Other -> "ORTAM"
}

private fun demoTempFor(kind: ClimateZoneKind): Float = when (kind) {
    ClimateZoneKind.Indoor -> 24.6f
    ClimateZoneKind.Outdoor -> 28.7f
    ClimateZoneKind.Bedroom -> 22.8f
    ClimateZoneKind.Other -> 23.5f
}

private fun demoHumFor(kind: ClimateZoneKind): Int = when (kind) {
    ClimateZoneKind.Indoor -> 45
    ClimateZoneKind.Outdoor -> 38
    ClimateZoneKind.Bedroom -> 50
    ClimateZoneKind.Other -> 42
}

fun climateComfortLabel(tempC: Float, humidityPct: Int): String {
    return if (tempC in 18f..28f && humidityPct in 30..65) "Normal Konfor" else "Dikkat"
}

fun climateComfortFraction(tempC: Float, humidityPct: Int): Float {
    if (tempC <= 0f && humidityPct <= 0) return 0f
    val tScore = 1f - ((tempC - 22f).let { kotlin.math.abs(it) } / 12f).coerceIn(0f, 1f)
    val hScore = 1f - ((humidityPct - 45).let { kotlin.math.abs(it) } / 40f).coerceIn(0f, 1f)
    return ((tScore + hScore) / 2f).coerceIn(0f, 1f)
}
