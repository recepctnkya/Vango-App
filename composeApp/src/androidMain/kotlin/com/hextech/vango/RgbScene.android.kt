package com.hextech.vango

import android.content.Context
import android.content.SharedPreferences

actual object RgbSceneStore {
    private const val PREFS = "vango_locale_prefs"
    private const val KEY = "rgb_scenes_json"

    private fun prefs(): SharedPreferences {
        val ctx = getAppContext().get() as Context
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    actual fun getJson(): String? = prefs().getString(KEY, null)

    actual fun setJson(json: String) {
        prefs().edit().putString(KEY, json).apply()
    }
}
