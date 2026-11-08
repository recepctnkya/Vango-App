package com.hextech.vango

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoteTelemetryParserTest {
    @Test
    fun mqttDimFieldIsPercent() {
        val payload = """
            {
              "protocol": "hexnet-io",
              "outputs": { "dimmers": [255, 0, 0, 0], "relays": [] },
              "dim": [100, 0, 0, 0]
            }
        """.trimIndent()
        val parsed = RemoteTelemetryParser.parse(payload)!!
        val (source, isPercent) = parsed.resolvedDimmerSources()
        assertTrue(isPercent)
        assertEquals(listOf(100, 0, 0, 0), source)
    }

    @Test
    fun rootDimmersUsesPwmHeuristicNotPercent() {
        val payload = """
            {
              "protocol": "hexnet-io",
              "dimmers": [128, 0, 0, 0],
              "outputs": { "relays": [] }
            }
        """.trimIndent()
        val parsed = RemoteTelemetryParser.parse(payload)!!
        val (source, isPercent) = parsed.resolvedDimmerSources()
        assertEquals(false, isPercent)
        assertEquals(listOf(128, 0, 0, 0), source)
        assertEquals(50, DimmerScale.telemetryToPercentByte(source[0], isPercent).toInt())
    }

    @Test
    fun apiWrapperMergesPayloadDim() {
        val payload = """
            {
              "ok": true,
              "telemetry": {
                "protocol": "hexnet-io",
                "outputs": { "dimmers": [255, 0, 0, 0], "relays": [] }
              },
              "latest": { "dim": [75, 0, 0, 0] }
            }
        """.trimIndent()
        val parsed = RemoteTelemetryParser.parse(payload)!!
        val (source, isPercent) = parsed.resolvedDimmerSources()
        assertTrue(isPercent)
        assertEquals(listOf(75, 0, 0, 0), source)
    }

    @Test
    fun apiWrapperMergesLatestTemperatureAndHumidity() {
        val payload = """
            {
              "ok": true,
              "telemetry": {
                "protocol": "hexnet-io",
                "sensors": { "water_pct": [50, 0, 0] }
              },
              "latest": { "temperature_c": 29, "humidity_pct": 47 }
            }
        """.trimIndent()
        val parsed = RemoteTelemetryParser.parse(payload)!!
        assertEquals(29, parsed.sensors.temperatureC)
        assertEquals(47, parsed.sensors.humidityPct)
    }

    @Test
    fun apiWrapperMergesLiveApiTemperatureFields() {
        val payload = """
            {
              "ok": true,
              "live_api_temperature_c": 31,
              "live_api_humidity_pct": 52,
              "telemetry": { "protocol": "hexnet-io" }
            }
        """.trimIndent()
        val parsed = RemoteTelemetryParser.parse(payload)!!
        assertEquals(31, parsed.sensors.temperatureC)
        assertEquals(52, parsed.sensors.humidityPct)
    }
}