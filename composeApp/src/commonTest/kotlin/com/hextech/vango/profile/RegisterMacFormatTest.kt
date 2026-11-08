package com.hextech.vango.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisterMacFormatTest {
    @Test
    fun formatsFreeTextAsColonSeparatedMac() {
        assertEquals("AA:BB:CC:DD:EE:FF", formatMacAddressInputColons("aa-bb cc.dd ee ff"))
    }

    @Test
    fun normalizesMacForDisplay() {
        assertEquals("98:A3:16:DD:85:42", normalizeMacForDisplay("98a316dd8542"))
    }

    @Test
    fun validatesColonSeparatedMacOnly() {
        assertTrue(isValidMacFormat("98:A3:16:DD:85:42"))
        assertFalse(isValidMacFormat("98-A3-16-DD-85-42"))
    }
}
