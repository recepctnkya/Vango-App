package com.hextech.vango

import platform.Foundation.NSUserDefaults

actual object HeroWeatherPreferences {
    private const val KEY_JSON = "vango_hero_cached_weather_json"
    private const val KEY_LAT = "vango_hero_saved_lat"
    private const val KEY_LON = "vango_hero_saved_lon"
    private const val KEY_CITY = "vango_hero_saved_city"
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun getCachedWeatherJson(): String? = defaults.stringForKey(KEY_JSON)

    actual fun setCachedWeatherJson(json: String?) {
        if (json == null) defaults.removeObjectForKey(KEY_JSON)
        else defaults.setObject(json, KEY_JSON)
    }

    actual fun getSavedLatitude(): Double? {
        defaults.objectForKey(KEY_LAT) ?: return null
        return defaults.doubleForKey(KEY_LAT)
    }

    actual fun getSavedLongitude(): Double? {
        defaults.objectForKey(KEY_LON) ?: return null
        return defaults.doubleForKey(KEY_LON)
    }

    actual fun setSavedCoordinates(lat: Double?, lon: Double?) {
        if (lat == null || lon == null) {
            defaults.removeObjectForKey(KEY_LAT)
            defaults.removeObjectForKey(KEY_LON)
        } else {
            defaults.setDouble(lat, KEY_LAT)
            defaults.setDouble(lon, KEY_LON)
        }
    }

    actual fun getSavedCityLabel(): String? = defaults.stringForKey(KEY_CITY)

    actual fun setSavedCityLabel(label: String?) {
        if (label == null) defaults.removeObjectForKey(KEY_CITY)
        else defaults.setObject(label, KEY_CITY)
    }
}
