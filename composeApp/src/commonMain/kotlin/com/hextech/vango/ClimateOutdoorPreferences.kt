package com.hextech.vango

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Dış ortam verisi: panel sensörü veya cihaz konumuna göre hava durumu. */
enum class ClimateOutdoorSource {
    Sensor,
    Location,
}

expect object ClimateOutdoorPreferences {
    fun getSource(): String?
    fun setSource(source: String)
}

var climateOutdoorSource by mutableStateOf(ClimateOutdoorSource.Location)

fun climateOutdoorSourceFromStorage(raw: String?): ClimateOutdoorSource =
    when (raw?.trim()?.lowercase()) {
        "sensor" -> ClimateOutdoorSource.Sensor
        else -> ClimateOutdoorSource.Location
    }

fun applyClimateOutdoorSource(source: ClimateOutdoorSource) {
    ClimateOutdoorPreferences.setSource(
        when (source) {
            ClimateOutdoorSource.Sensor -> "sensor"
            ClimateOutdoorSource.Location -> "location"
        },
    )
    climateOutdoorSource = source
}

fun loadClimateOutdoorSourceFromStorage() {
    climateOutdoorSource = climateOutdoorSourceFromStorage(ClimateOutdoorPreferences.getSource())
}
