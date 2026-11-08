package com.hextech.vango

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

sealed class WeatherFetchResult {
    data class Ok(val info: WeatherInfo) : WeatherFetchResult()
    data class Err(val message: String) : WeatherFetchResult()
}

/**
 * Outdoor weather from OpenWeatherMap current weather API.
 * Uses phone internet only — no BLE / caravan dependency.
 */
class WeatherService(private val apiKey: String) {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getWeather(latitude: Double, longitude: Double): WeatherInfo? =
        when (val r = getWeatherResult(latitude, longitude)) {
            is WeatherFetchResult.Ok -> r.info
            is WeatherFetchResult.Err -> null
        }

    suspend fun getWeatherResult(latitude: Double, longitude: Double): WeatherFetchResult {
        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey"
                val response = client.get(url)
                val body = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    println("🌦️ Weather HTTP ${response.status.value}: ${body.take(200)}")
                    return@withContext WeatherFetchResult.Err("HTTP ${response.status.value}")
                }
                val weatherData = json.decodeFromString<WeatherResponse>(body)
                val primary = weatherData.weather.firstOrNull()
                WeatherFetchResult.Ok(
                    WeatherInfo(
                        city = weatherData.name,
                        temperature = weatherData.main.temp - 273.15f,
                        description = primary?.description ?: "Unknown",
                        humidity = weatherData.main.humidity,
                        conditionId = primary?.id ?: 800,
                        windSpeedMs = weatherData.wind?.speed ?: 0f,
                        windDeg = weatherData.wind?.deg,
                        snowMm1h = weatherData.snow?.`1h`,
                        cloudPct = weatherData.clouds?.all?.coerceIn(0, 100) ?: 0,
                        visibilityMeters = weatherData.visibility,
                        sunriseEpochSec = weatherData.sys?.sunrise,
                        sunsetEpochSec = weatherData.sys?.sunset,
                        latitude = weatherData.coord?.lat ?: latitude,
                        longitude = weatherData.coord?.lon ?: longitude,
                        fetchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                    ),
                )
            } catch (e: Exception) {
                println("🌦️ Weather fetch failed: ${e.message}")
                WeatherFetchResult.Err(e.message ?: "network")
            }
        }
    }
}
