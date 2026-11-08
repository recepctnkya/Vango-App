package com.hextech.vango

/**
 * Durable outdoor-weather cache + optional fallback coordinates (saved city / last fix).
 * Provider: OpenWeatherMap current weather (`WeatherService`).
 */
expect object HeroWeatherPreferences {
    fun getCachedWeatherJson(): String?
    fun setCachedWeatherJson(json: String?)
    fun getSavedLatitude(): Double?
    fun getSavedLongitude(): Double?
    fun setSavedCoordinates(lat: Double?, lon: Double?)
    fun getSavedCityLabel(): String?
    fun setSavedCityLabel(label: String?)
}
