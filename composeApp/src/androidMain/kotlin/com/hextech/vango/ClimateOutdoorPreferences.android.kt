package com.hextech.vango

import android.content.Context
import android.content.SharedPreferences

actual object ClimateOutdoorPreferences {
    private const val PREFS = "vango_locale_prefs"
    private const val KEY = "climate_outdoor_source"

    private fun prefs(): SharedPreferences {
        val ctx = getAppContext().get() as Context
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    actual fun getSource(): String? = prefs().getString(KEY, null)

    actual fun setSource(source: String) {
        prefs().edit().putString(KEY, source).apply()
    }
}
