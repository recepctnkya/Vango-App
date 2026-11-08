package com.hextech.vango

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** OpenWeatherMap current-weather refresh interval for the hero sky. */
const val HeroWeatherRefreshIntervalMs: Long = 30L * 60L * 1000L

/** Samples older than this are shown as stale (not “live”). */
const val HeroWeatherStaleAfterMs: Long = 3L * 60L * 60L * 1000L

/** Why the hero badge cannot show live weather (never invents “Güneşli”). */
enum class HeroWeatherBlockReason {
    None,
    Loading,
    NeedsLocationPermission,
    LocationServicesOff,
    NoCoordinates,
    NoInternet,
    ApiError,
}

data class HeroWeatherCity(
    val name: String,
    val lat: Double,
    val lon: Double,
)

/** Manual city fallback when GPS/permission unavailable. */
val HeroWeatherCityChoices: List<HeroWeatherCity> = listOf(
    HeroWeatherCity("İstanbul", 41.0082, 28.9784),
    HeroWeatherCity("Ankara", 39.9334, 32.8597),
    HeroWeatherCity("İzmir", 38.4192, 27.1287),
    HeroWeatherCity("Antalya", 36.8969, 30.7133),
    HeroWeatherCity("Bursa", 40.1885, 29.0610),
    HeroWeatherCity("Adana", 37.0000, 35.3213),
    HeroWeatherCity("Trabzon", 41.0015, 39.7178),
    HeroWeatherCity("Gaziantep", 37.0662, 37.3833),
)

@Serializable
private data class WeatherInfoDisk(
    val city: String,
    val temperature: Float,
    val description: String,
    val humidity: Int,
    val conditionId: Int = 800,
    val windSpeedMs: Float = 0f,
    val cloudPct: Int = 0,
    val visibilityMeters: Int? = null,
    val sunriseEpochSec: Long? = null,
    val sunsetEpochSec: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val fetchedAtEpochMs: Long = 0L,
)

private val weatherDiskJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun WeatherInfo.toDisk() = WeatherInfoDisk(
    city = city,
    temperature = temperature,
    description = description,
    humidity = humidity,
    conditionId = conditionId,
    windSpeedMs = windSpeedMs,
    cloudPct = cloudPct,
    visibilityMeters = visibilityMeters,
    sunriseEpochSec = sunriseEpochSec,
    sunsetEpochSec = sunsetEpochSec,
    latitude = latitude,
    longitude = longitude,
    fetchedAtEpochMs = fetchedAtEpochMs,
)

private fun WeatherInfoDisk.toInfo() = WeatherInfo(
    city = city,
    temperature = temperature,
    description = description,
    humidity = humidity,
    conditionId = conditionId,
    windSpeedMs = windSpeedMs,
    cloudPct = cloudPct,
    visibilityMeters = visibilityMeters,
    sunriseEpochSec = sunriseEpochSec,
    sunsetEpochSec = sunsetEpochSec,
    latitude = latitude,
    longitude = longitude,
    fetchedAtEpochMs = fetchedAtEpochMs,
)

/**
 * Shared outdoor-weather cache for hero + climate UI.
 * Source: OpenWeatherMap 2.5 /weather (see [WeatherService]).
 * Independent of BLE / caravan connection — phone internet + location (or city) only.
 */
object HeroWeatherRepository {
    private val _snapshot = MutableStateFlow<WeatherInfo?>(null)
    val snapshot: StateFlow<WeatherInfo?> = _snapshot.asStateFlow()

    private val _blockReason = MutableStateFlow(HeroWeatherBlockReason.Loading)
    val blockReason: StateFlow<HeroWeatherBlockReason> = _blockReason.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var lastAttemptEpochMs: Long = 0L
    private var promptedLocationThisSession: Boolean = false

    fun hydrateFromDisk() {
        if (_snapshot.value != null) return
        val raw = HeroWeatherPreferences.getCachedWeatherJson() ?: return
        runCatching {
            weatherDiskJson.decodeFromString<WeatherInfoDisk>(raw).toInfo()
        }.getOrNull()?.let { info ->
            _snapshot.value = info
            updateClimateWeatherCache(info)
            _blockReason.value = HeroWeatherBlockReason.None
            println("🌦️ Hero weather hydrated from disk city=${info.city} ageMs=${Clock.System.now().toEpochMilliseconds() - info.fetchedAtEpochMs}")
        }
    }

    fun current(): WeatherInfo? = _snapshot.value ?: climateWeatherCache.value

    fun isStale(info: WeatherInfo?, nowMs: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
        if (info == null || info.fetchedAtEpochMs <= 0L) return true
        return nowMs - info.fetchedAtEpochMs > HeroWeatherStaleAfterMs
    }

    fun needsRefresh(nowMs: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
        val cur = current()
        if (cur == null) return true
        if (cur.fetchedAtEpochMs <= 0L) return true
        return nowMs - cur.fetchedAtEpochMs >= HeroWeatherRefreshIntervalMs
    }

    fun hasSavedCoordinates(): Boolean =
        HeroWeatherPreferences.getSavedLatitude() != null &&
            HeroWeatherPreferences.getSavedLongitude() != null

    /** Diagnose location/network before network call — for badge + optional permission prompt. */
    fun evaluateBlockReason(): HeroWeatherBlockReason {
        if (current() != null && !needsRefresh()) return HeroWeatherBlockReason.None
        if (!isInternetAvailable()) {
            return if (current() != null) HeroWeatherBlockReason.None else HeroWeatherBlockReason.NoInternet
        }
        if (hasLocationPermissions()) {
            if (!isLocationServicesEnabled() && !hasSavedCoordinates()) {
                return HeroWeatherBlockReason.LocationServicesOff
            }
            return HeroWeatherBlockReason.None
        }
        if (hasSavedCoordinates()) return HeroWeatherBlockReason.None
        return HeroWeatherBlockReason.NeedsLocationPermission
    }

    /**
     * One-shot location (no continuous / background GPS). Falls back to saved lat/lon or city.
     * Never fabricates clear-sky weather on failure.
     */
    suspend fun refreshIfNeeded(force: Boolean = false): Boolean {
        hydrateFromDisk()
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force && !needsRefresh(now)) {
            _blockReason.value = HeroWeatherBlockReason.None
            return false
        }
        if (!force && now - lastAttemptEpochMs < 60_000L) return false
        lastAttemptEpochMs = now

        _loading.value = current() == null
        if (current() == null) _blockReason.value = HeroWeatherBlockReason.Loading

        if (!isInternetAvailable()) {
            println("🌦️ Hero weather: no internet")
            _blockReason.value = if (current() != null) HeroWeatherBlockReason.None else HeroWeatherBlockReason.NoInternet
            _loading.value = false
            return false
        }

        val coords = resolveCoordinates()
        if (coords == null) {
            val reason = when {
                !hasLocationPermissions() -> HeroWeatherBlockReason.NeedsLocationPermission
                !isLocationServicesEnabled() -> HeroWeatherBlockReason.LocationServicesOff
                else -> HeroWeatherBlockReason.NoCoordinates
            }
            println("🌦️ Hero weather: no coordinates reason=$reason")
            _blockReason.value = if (current() != null) HeroWeatherBlockReason.None else reason
            _loading.value = false
            return false
        }

        val result = WeatherService(WeatherApiKey).getWeatherResult(coords.first, coords.second)
        return when (result) {
            is WeatherFetchResult.Ok -> {
                publish(result.info)
                HeroWeatherPreferences.setSavedCoordinates(
                    result.info.latitude ?: coords.first,
                    result.info.longitude ?: coords.second,
                )
                if (result.info.city.isNotBlank()) {
                    HeroWeatherPreferences.setSavedCityLabel(result.info.city)
                }
                _blockReason.value = HeroWeatherBlockReason.None
                _loading.value = false
                println("🌦️ Hero weather OK city=${result.info.city} id=${result.info.conditionId} cloud=${result.info.cloudPct}%")
                true
            }
            is WeatherFetchResult.Err -> {
                println("🌦️ Hero weather API error: ${result.message}")
                _blockReason.value = if (current() != null) HeroWeatherBlockReason.None else HeroWeatherBlockReason.ApiError
                _loading.value = false
                false
            }
        }
    }

    /** Save manual city and fetch — works without GPS/permission. */
    suspend fun selectCity(city: HeroWeatherCity): Boolean {
        HeroWeatherPreferences.setSavedCoordinates(city.lat, city.lon)
        HeroWeatherPreferences.setSavedCityLabel(city.name)
        lastAttemptEpochMs = 0L
        return refreshIfNeeded(force = true)
    }

    fun markLocationPrompted() {
        promptedLocationThisSession = true
    }

    fun shouldAutoRequestLocation(): Boolean {
        if (promptedLocationThisSession) return false
        if (hasLocationPermissions()) return false
        if (hasSavedCoordinates()) return false
        if (current() != null) return false
        return true
    }

    fun publish(info: WeatherInfo) {
        _snapshot.value = info
        updateClimateWeatherCache(info)
        runCatching {
            HeroWeatherPreferences.setCachedWeatherJson(weatherDiskJson.encodeToString(info.toDisk()))
        }
    }

    private suspend fun resolveCoordinates(): Pair<Double, Double>? {
        if (hasLocationPermissions() && isLocationServicesEnabled()) {
            val live = getLocationUpdates()
            if (live != null) {
                println("🌦️ Hero weather live coords lat=${live.first} lon=${live.second}")
                return live
            }
            println("🌦️ Hero weather: live location null (timeout/unavailable)")
        }
        val lat = HeroWeatherPreferences.getSavedLatitude()
        val lon = HeroWeatherPreferences.getSavedLongitude()
        if (lat != null && lon != null) {
            println("🌦️ Hero weather saved coords lat=$lat lon=$lon")
            return lat to lon
        }
        return null
    }
}

/** Outdoor temp label for hero — never falls back to caravan BLE sensors. */
fun heroOutdoorTemperatureLabel(info: WeatherInfo?): String? {
    val t = info?.temperature?.toInt() ?: return null
    if (t !in -40..60) return null
    return "${t}°C"
}

/**
 * Hero top-right badge primary line (Turkish).
 * Null [info] → caller shows loading / location / unavailable — not fake clear sky.
 */
fun heroWeatherDisplayLabel(info: WeatherInfo?, period: HeroSkyPeriod, stale: Boolean = false): String? {
    if (info == null) return null
    val base = heroWeatherPrimaryTr(info.conditionId, info.cloudPct, period)
    return if (stale) "$base · eski" else base
}

/** Same wind threshold as [resolveHeroSkyWeather] — secondary "Rüzgarlı" line only. */
fun heroWeatherIsWindy(info: WeatherInfo?): Boolean =
    info != null && info.windSpeedMs >= 8f

fun heroWeatherPrimaryTr(conditionId: Int, cloudPct: Int, period: HeroSkyPeriod): String {
    val nightish = period == HeroSkyPeriod.Night || period == HeroSkyPeriod.Dusk
    return when {
        conditionId in 200..299 -> "Fırtınalı"
        conditionId in 300..399 -> "Yağmurlu"
        conditionId in 520..531 -> "Sağanak"
        conditionId in 502..504 -> "Sağanak"
        conditionId in 500..599 -> "Yağmurlu"
        conditionId in 600..699 -> "Karlı"
        conditionId in 700..799 -> "Sisli"
        conditionId == 800 -> if (nightish) "Açık" else "Güneşli"
        conditionId in 801..802 -> "Parçalı Bulutlu"
        conditionId in 803..804 -> "Bulutlu"
        else -> when {
            cloudPct >= 85 -> "Bulutlu"
            cloudPct >= 35 -> "Parçalı Bulutlu"
            nightish -> "Açık"
            else -> "Güneşli"
        }
    }
}

fun heroWeatherBadgeIcon(
    info: WeatherInfo?,
    period: HeroSkyPeriod,
): HeroWeatherBadgeIcon {
    if (info == null) return HeroWeatherBadgeIcon.Unknown
    val nightish = period == HeroSkyPeriod.Night || period == HeroSkyPeriod.Dusk
    val id = info.conditionId
    return when {
        id in 200..299 -> HeroWeatherBadgeIcon.Storm
        id in 520..531 || id in 502..504 -> HeroWeatherBadgeIcon.Shower
        id in 300..399 || id in 500..599 -> HeroWeatherBadgeIcon.Rain
        id in 600..699 -> HeroWeatherBadgeIcon.Snow
        id in 700..799 -> HeroWeatherBadgeIcon.Fog
        id == 800 -> if (nightish) HeroWeatherBadgeIcon.Moon else HeroWeatherBadgeIcon.Sun
        id in 801..802 -> HeroWeatherBadgeIcon.PartlyCloudy
        id in 803..804 -> HeroWeatherBadgeIcon.Cloudy
        else -> when {
            info.cloudPct >= 85 -> HeroWeatherBadgeIcon.Cloudy
            info.cloudPct >= 35 -> HeroWeatherBadgeIcon.PartlyCloudy
            nightish -> HeroWeatherBadgeIcon.Moon
            else -> HeroWeatherBadgeIcon.Sun
        }
    }
}

enum class HeroWeatherBadgeIcon {
    Sun,
    Moon,
    PartlyCloudy,
    Cloudy,
    Rain,
    Shower,
    Snow,
    Fog,
    Storm,
    Unknown,
    Loading,
    NeedsLocation,
}

enum class HeroWeatherBadgeAction {
    None,
    RequestLocation,
    PickCity,
}

/** Top-right weather chip model (shared by dashboard + hero scene). */
data class HeroWeatherBadgeUi(
    val primary: String,
    val windy: Boolean,
    val icon: HeroWeatherBadgeIcon,
    val action: HeroWeatherBadgeAction = HeroWeatherBadgeAction.None,
)
