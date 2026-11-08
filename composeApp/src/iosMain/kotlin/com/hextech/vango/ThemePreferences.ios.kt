package com.hextech.vango

import platform.Foundation.NSUserDefaults

actual object ThemePreferences {
    private const val KEY = "theme_mode"
    private const val KEY_ACCENT = "theme_accent_id"
    private const val KEY_CARD = "theme_card_style"
    private const val KEY_BRIGHTNESS = "theme_brightness_percent"
    private const val KEY_AUTO_BRIGHTNESS = "theme_auto_brightness"

    actual fun getThemeMode(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)

    actual fun setThemeMode(mode: String) {
        NSUserDefaults.standardUserDefaults.setObject(mode, KEY)
    }

    actual fun getAccentId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_ACCENT)

    actual fun setAccentId(id: String) {
        NSUserDefaults.standardUserDefaults.setObject(id, KEY_ACCENT)
    }

    actual fun getCardStyle(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_CARD)

    actual fun setCardStyle(style: String) {
        NSUserDefaults.standardUserDefaults.setObject(style, KEY_CARD)
    }

    actual fun getBrightnessPercent(): Int {
        val raw = NSUserDefaults.standardUserDefaults.integerForKey(KEY_BRIGHTNESS)
        return if (raw <= 0) 70 else raw.toInt().coerceIn(5, 100)
    }

    actual fun setBrightnessPercent(percent: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(
            percent.coerceIn(5, 100).toLong(),
            KEY_BRIGHTNESS,
        )
    }

    actual fun getAutoBrightness(): Boolean {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(KEY_AUTO_BRIGHTNESS) == null) return true
        return defaults.boolForKey(KEY_AUTO_BRIGHTNESS)
    }

    actual fun setAutoBrightness(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, KEY_AUTO_BRIGHTNESS)
    }
}

actual object ScreenBrightnessController {
    actual fun apply(percent: Int, auto: Boolean) {
        // iOS brightness is system-controlled; no-op for now.
    }
}
