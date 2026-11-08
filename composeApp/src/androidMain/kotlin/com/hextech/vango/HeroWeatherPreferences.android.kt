package com.hextech.vango

import android.content.Context
import android.content.SharedPreferences

actual object HeroWeatherPreferences {
    private const val PREFS = "vango_hero_weather_prefs"
    private const val KEY_JSON = "cached_weather_json"
    private const val KEY_LAT = "saved_lat"
    private const val KEY_LON = "saved_lon"
    private const val KEY_CITY = "saved_city"

    private fun prefs(): SharedPreferences {
        val ctx = getAppContext().get() as Context
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    actual fun getCachedWeatherJson(): String? = prefs().getString(KEY_JSON, null)

    actual fun setCachedWeatherJson(json: String?) {
        prefs().edit().putString(KEY_JSON, json).apply()
    }

    actual fun getSavedLatitude(): Double? {
        val p = prefs()
        if (!p.contains(KEY_LAT)) return null
        return p.getFloat(KEY_LAT, 0f).toDouble()
    }

    actual fun getSavedLongitude(): Double? {
        val p = prefs()
        if (!p.contains(KEY_LON)) return null
        return p.getFloat(KEY_LON, 0f).toDouble()
    }

    actual fun setSavedCoordinates(lat: Double?, lon: Double?) {
        val e = prefs().edit()
        if (lat == null || lon == null) {
            e.remove(KEY_LAT).remove(KEY_LON)
        } else {
            e.putFloat(KEY_LAT, lat.toFloat()).putFloat(KEY_LON, lon.toFloat())
        }
        e.apply()
    }

    actual fun getSavedCityLabel(): String? = prefs().getString(KEY_CITY, null)

    actual fun setSavedCityLabel(label: String?) {
        prefs().edit().putString(KEY_CITY, label).apply()
    }
}
