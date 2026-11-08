package com.hextech.vango

import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager

actual object ThemePreferences {
    private const val PREFS = "vango_locale_prefs"
    private const val KEY = "theme_mode"
    private const val KEY_ACCENT = "theme_accent_id"
    private const val KEY_CARD = "theme_card_style"
    private const val KEY_BRIGHTNESS = "theme_brightness_percent"
    private const val KEY_AUTO_BRIGHTNESS = "theme_auto_brightness"

    private fun prefs(): SharedPreferences {
        val ctx = getAppContext().get() as Context
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    actual fun getThemeMode(): String? = prefs().getString(KEY, null)

    actual fun setThemeMode(mode: String) {
        prefs().edit().putString(KEY, mode).apply()
    }

    actual fun getAccentId(): String? = prefs().getString(KEY_ACCENT, null)

    actual fun setAccentId(id: String) {
        prefs().edit().putString(KEY_ACCENT, id).apply()
    }

    actual fun getCardStyle(): String? = prefs().getString(KEY_CARD, null)

    actual fun setCardStyle(style: String) {
        prefs().edit().putString(KEY_CARD, style).apply()
    }

    actual fun getBrightnessPercent(): Int =
        prefs().getInt(KEY_BRIGHTNESS, 70).coerceIn(5, 100)

    actual fun setBrightnessPercent(percent: Int) {
        prefs().edit().putInt(KEY_BRIGHTNESS, percent.coerceIn(5, 100)).apply()
    }

    actual fun getAutoBrightness(): Boolean =
        prefs().getBoolean(KEY_AUTO_BRIGHTNESS, true)

    actual fun setAutoBrightness(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_AUTO_BRIGHTNESS, enabled).apply()
    }
}

actual object ScreenBrightnessController {
    actual fun apply(percent: Int, auto: Boolean) {
        val activity = MainActivity.current ?: return
        activity.runOnUiThread {
            val lp = activity.window.attributes
            lp.screenBrightness = if (auto) {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                (percent.coerceIn(5, 100) / 100f).coerceIn(0.05f, 1f)
            }
            activity.window.attributes = lp
        }
    }
}
