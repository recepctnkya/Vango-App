package com.hextech.vango.profile

/** Kullanıcı girişini yalnızca hex + `:` ile AA:BB:CC:DD:EE:FF biçimine çevirir (en fazla 17 karakter). */
fun formatMacAddressInputColons(raw: String): String {
    val hex = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(12)
    return hex.chunked(2).joinToString(":")
}

/**
 * Cursor-safe formatter for text fields:
 * keeps caret near the same logical hex position while inserting ":" separators.
 */
fun formatMacAddressInputColonsWithCursor(raw: String, cursor: Int): Pair<String, Int> {
    val clampedCursor = cursor.coerceIn(0, raw.length)
    val hexBeforeCursor = raw.take(clampedCursor).count { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
    val hex = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(12)
    val formatted = hex.chunked(2).joinToString(":")
    val cappedHexBefore = hexBeforeCursor.coerceIn(0, hex.length)
    val mappedCursor = if (cappedHexBefore == 0) 0 else cappedHexBefore + ((cappedHexBefore - 1) / 2)
    return formatted to mappedCursor.coerceIn(0, formatted.length)
}

/** Bluetooth veya serbest metin MAC adresini AA:BB:CC:DD:EE:FF biçimine getirir. */
fun normalizeMacForDisplay(raw: String): String {
    val hex = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(12)
    return hex.chunked(2).joinToString(":")
}

private val macFormatRegex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")

fun isValidMacFormat(mac: String): Boolean = macFormatRegex.matches(mac.trim())
