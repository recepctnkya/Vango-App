package com.hextech.vango.mqtt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MqttCommandBuilderTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun rgbSetUsesPanelCommandEnvelope() {
        val root = json.parseToJsonElement(MqttCommandBuilder.rgbSet(r = 12, g = 34, b = 56, isLight = 1)).jsonObject
        val args = root["args"]!!.jsonObject

        assertEquals("rgb_set", root["cmd"]?.jsonPrimitive?.content)
        assertEquals(12, args["r"]?.jsonPrimitive?.int)
        assertEquals(34, args["g"]?.jsonPrimitive?.int)
        assertEquals(56, args["b"]?.jsonPrimitive?.int)
    }

    @Test
    fun relaySetCoercesIndexAndState() {
        val root = json.parseToJsonElement(MqttCommandBuilder.relaySet(index = 99, state = true)).jsonObject
        val args = root["args"]!!.jsonObject

        assertEquals("relay_set", root["cmd"]?.jsonPrimitive?.content)
        assertEquals(15, args["relay"]?.jsonPrimitive?.int)
        assertEquals(1, args["state"]?.jsonPrimitive?.int)
    }

    @Test
    fun dimmerSetScalesPercentToRaw255() {
        val root = json.parseToJsonElement(MqttCommandBuilder.dimmerSet(channel = 0, percent0to100 = 100)).jsonObject
        val args = root["args"]!!.jsonObject

        assertEquals("dimmer_set", root["cmd"]?.jsonPrimitive?.content)
        assertEquals(0, args["channel"]?.jsonPrimitive?.int)
        assertEquals(255, args["value"]?.jsonPrimitive?.int)
    }
}