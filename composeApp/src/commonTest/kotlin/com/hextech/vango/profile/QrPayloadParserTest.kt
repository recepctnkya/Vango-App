package com.hextech.vango.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QrPayloadParserTest {
    @Test
    fun parsesLabeledQrPayload() {
        val parsed = parseDeviceQrContent("MAC=98:A3:16:DD:85:42; UID=VANGO_123; MODEL=VANGO DISPLAY")

        assertEquals("98:A3:16:DD:85:42", parsed.mac)
        assertEquals("VANGO_123", parsed.uid)
        assertEquals("VANGO DISPLAY", parsed.modelName)
    }

    @Test
    fun parsesBareMacPayload() {
        val parsed = parseDeviceQrContent("98a316dd8542")

        assertEquals("98:A3:16:DD:85:42", parsed.mac)
        assertNull(parsed.uid)
    }
}
