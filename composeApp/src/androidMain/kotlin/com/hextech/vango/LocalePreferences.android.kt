package com.hextech.vango

import android.content.Context
import android.content.SharedPreferences

actual object LocalePreferences {
    private const val PREFS = "vango_locale_prefs"
    private const val KEY = "language_tag"
    private const val KEY_UPDATED = "language_updated_at_ms"

    private fun prefs(): SharedPreferences {
        val ctx = getAppContext().get() as Context
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    actual fun getLanguageTag(): String? = prefs().getString(KEY, null)

    actual fun setLanguageTag(tag: String?) {
        prefs().edit().apply {
            if (tag == null) remove(KEY) else putString(KEY, tag)
            apply()
        }
    }

    actual fun getLanguageUpdatedAtEpochMs(): Long =
        prefs().getLong(KEY_UPDATED, 0L)

    actual fun setLanguageUpdatedAtEpochMs(ms: Long) {
        prefs().edit().putLong(KEY_UPDATED, ms.coerceAtLeast(0L)).apply()
    }
}
