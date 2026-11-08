package com.hextech.vango

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BleEnvSensorParserTest {
    @Test
    fun parsesFloatTemperatureAndHumidityAtRoot() {
        val raw = """{"slvConn":"yes","temperature_c":23.6,"humidity_pct":54.2}"""
        val env = BleEnvSensorParser.parseFromJson(raw)
        assertNotNull(env)
        assertEquals(24, env.tempC)
        assertEquals(54, env.humidityPct)
    }

    @Test
    fun parsesTempHumAliasesLikePlatform() {
        val raw = """{"payload":{"temp":21,"humidity":48}}"""
        val env = BleEnvSensorParser.parseFromJson(raw)
        assertNotNull(env)
        assertEquals(21, env.tempC)
        assertEquals(48, env.humidityPct)
    }

    @Test
    fun parsesReadingsObject() {
        val raw = """{"readings":{"temp":19,"hum":62}}"""
        val env = BleEnvSensorParser.parseFromJson(raw)
        assertNotNull(env)
        assertEquals(19, env.tempC)
        assertEquals(62, env.humidityPct)
    }

    @Test
    fun parsesLegacySensorTempFields() {
        val raw = """{"sensorTemp":25,"sensorHumidity":70}"""
        val env = BleEnvSensorParser.parseFromJson(raw)
        assertNotNull(env)
        assertEquals(25, env.tempC)
        assertEquals(70, env.humidityPct)
    }

    @Test
    fun parsesSdbArrayFallback() {
        val raw = """{"sDB":[22,55,0,0,0]}"""
        val env = BleEnvSensorParser.parseFromJson(raw)
        assertNotNull(env)
        assertEquals(22, env.tempC)
        assertEquals(55, env.humidityPct)
    }

    @Test
    fun parsesHexnetIoProtocolPayload() {
        val raw = """
            {
              "protocol": "hexnet-io",
              "sensors": { "temperature_c": 30, "humidity_pct": 49 }
            }
        """.trimIndent()
        val env = BleEnvSensorParser.parseFromJson(raw)
        assertNotNull(env)
        assertEquals(30, env.tempC)
        assertEquals(49, env.humidityPct)
    }

    @Test
    fun parsesSensorSlotArray() {
        val raw = """{"sensors":[{"type":"temp_humidity","temp":28,"hum":51}]}"""
        val env = BleEnvSensorParser.parseFromJson(raw)
        assertNotNull(env)
        assertEquals(28, env.tempC)
        assertEquals(51, env.humidityPct)
    }

    @Test
    fun returnsNullWhenNoSensorFields() {
        val raw = """{"slvConn":"yes","volt":12.4}"""
        assertNull(BleEnvSensorParser.parseFromJson(raw))
    }
}