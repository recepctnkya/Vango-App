package com.hextech.vango

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sky period driven by solar times (not app theme).
 * Dawn/Dusk use warm tones; Day blue; Night deep blue.
 */
enum class HeroSkyPeriod {
    Dawn,
    Day,
    Dusk,
    Night,
}

/**
 * Weather scene bucket for procedural FX.
 * [windy] is orthogonal to precipitation.
 */
enum class HeroSkyWeather {
    Clear,
    PartlyCloudy,
    Overcast,
    Rain,
    Snow,
    Fog,
}

data class HeroCelestialPos(
    /** 0 = left horizon, 1 = right horizon (stylized card arc — not a compass). */
    val xFrac: Float,
    /** 0 = top of card, increases downward. */
    val yFrac: Float,
    val aboveHorizon: Boolean,
)

data class HeroMoonState(
    val pos: HeroCelestialPos,
    /** 0 = new, 1 = full. */
    val illumination: Float,
    /** True while waxing (new → full). */
    val waxing: Boolean,
)

/**
 * Snapshot for hero Canvas — targets update about once per minute; UI may lerp.
 *
 * Sun rise/set: OpenWeather `sys` when available; otherwise [approximateSolarTimes]
 * (NOAA-style day-of-year approximation — not a full ephemeris).
 *
 * Moon: approximate illumination from synodic age; path is a **stylized visual arc**
 * phase-shifted from the sun (not claimed as real moonrise azimuth).
 */
data class HeroSkySnapshot(
    val period: HeroSkyPeriod,
    val sun: HeroCelestialPos?,
    val moon: HeroMoonState?,
    val starVisibility: Float,
    val weather: HeroSkyWeather,
    val windy: Boolean,
    /** 0–1 blended cover from API cloud% + condition. */
    val cloudDensity: Float,
    val reflectionStrength: Float,
    val bodyXFrac: Float,
    val nightWindowsLit: Boolean,
    val weatherStale: Boolean,
    val weatherLabel: String?,
    val temperatureLabel: String?,
    /** Icon for the top-right weather badge (same sample as sky). */
    val weatherBadgeIcon: HeroWeatherBadgeIcon = HeroWeatherBadgeIcon.Unknown,
    /** True when a weather sample (live or cached) is driving the look. */
    val hasWeatherData: Boolean = false,
    /** Visible solar/lunar disk opacity (0 = hidden under overcast). */
    val celestialDiskAlpha: Float = 1f,
    /** Soft glow strength around sun/moon (fades before disk vanishes). */
    val celestialGlowAlpha: Float = 1f,
    /** Scene directional light multiplier (clear≈1, overcast≈0.35). */
    val lightStrengthMul: Float = 1f,
    /** Softens mountain/rock/vehicle cast shadows under clouds. */
    val shadowSoftness: Float = 0f,
    /** Far-mountain / horizon haze (fog & heavy cloud). */
    val farHaze: Float = 0f,
    /** Calendar season from date + hemisphere (vegetation bucket). */
    val season: SeasonState = SeasonState.Summer,
    /**
     * Continuous year-phase 0…1 for vegetation blends (not a discrete scene swap).
     * See [seasonProgress].
     */
    val seasonProgress: Float = 0.35f,
    /** Shared wind for trees / particles / clouds. */
    val wind: WindState = WindState.Calm,
    /** Deciduous canopy fill 0–1. */
    val foliageAmount: Float = 1f,
    /** Ground/branch/roof snow accumulation 0–1 (independent of season alone). */
    val snowCover: Float = 0f,
    /** Peak residual snow 0–1 — independent of spring ground cover. */
    val mountainSnowCover: Float = 0f,
    /** Narrow shore ice rim 0–1 — never full lake freeze from season alone. */
    val iceCoverage: Float = 0f,
    /** Falling flake intensity 0–1 (active snowfall only). */
    val snowfallIntensity: Float = 0f,
)

/**
 * Map OpenWeather sample to scene weather.
 * Null sample → neutral overcast-ish look (never assume clear sky).
 */
fun resolveHeroSkyWeather(info: WeatherInfo?): Pair<HeroSkyWeather, Boolean> {
    if (info == null) return HeroSkyWeather.Overcast to false
    val id = info.conditionId
    val windy = info.windSpeedMs >= 8f
    val weather = when {
        id in 200..599 -> HeroSkyWeather.Rain
        id in 600..699 -> HeroSkyWeather.Snow
        id in 700..799 -> HeroSkyWeather.Fog
        id == 800 -> HeroSkyWeather.Clear
        id in 801..802 -> HeroSkyWeather.PartlyCloudy
        id in 803..804 -> HeroSkyWeather.Overcast
        else -> when {
            info.cloudPct >= 85 -> HeroSkyWeather.Overcast
            info.cloudPct >= 35 -> HeroSkyWeather.PartlyCloudy
            else -> HeroSkyWeather.Clear
        }
    }
    return weather to windy
}

/**
 * Disk/glow visibility from cloud% + weather code (visual tuning, not physics).
 * Overcast/rain can hide the disk even if cloud% is moderate.
 */
fun celestialVisibility(cloudPct: Int, weather: HeroSkyWeather): Pair<Float, Float> {
    val c = cloudPct.coerceIn(0, 100) / 100f
    val (diskBase, glowBase) = when {
        c <= 0.20f -> 1.00f to 1.00f
        c <= 0.50f -> 0.72f to 0.55f
        c <= 0.80f -> 0.28f to 0.18f
        else -> 0.00f to 0.06f
    }
    return when (weather) {
        HeroSkyWeather.Clear -> diskBase to glowBase
        HeroSkyWeather.PartlyCloudy -> (diskBase * 0.85f) to (glowBase * 0.75f)
        HeroSkyWeather.Fog -> (diskBase * 0.35f).coerceAtMost(0.4f) to (glowBase * 0.25f)
        HeroSkyWeather.Overcast -> 0f to (glowBase * 0.12f).coerceAtMost(0.08f)
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0f to 0.05f
    }
}

/**
 * Build sky snapshot from local clock + optional OpenWeather sample.
 */
fun computeHeroSkySnapshot(
    nowInstant: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    weather: WeatherInfo?,
    weatherStale: Boolean,
): HeroSkySnapshot {
    val local = nowInstant.toLocalDateTime(timeZone)
    val nowSec = nowInstant.epochSeconds
    val lat = weather?.latitude ?: HeroWeatherPreferences.getSavedLatitude() ?: 41.0
    val lon = weather?.longitude ?: HeroWeatherPreferences.getSavedLongitude() ?: 29.0

    val (riseSec, setSec) = resolveSolarWindow(weather, local, timeZone, lat, lon)
    val period = skyPeriodFromSolar(nowSec, riseSec, setSec)
    val doy = dayOfYear(local)
    val sun = stylizedSunPosition(nowSec, riseSec, setSec, lat, doy)
    val moonApprox = approximateMoonState(nowInstant, sun, riseSec, setSec, lat, doy)
    val hasWeather = weather != null
    val (weatherKind, windy) = resolveHeroSkyWeather(weather)
    val season = resolveSeasonState(local, lat)
    val seasonProg = seasonProgress(local, lat)
    val wind = resolveWindState(weather)
    val foliage = foliageFromSeasonProgress(seasonProg)
    val snowCover = resolveSnowCover(weatherKind, weather?.snowMm1h, weather?.temperature)
    val mountainSnow = resolveMountainSnowCover(season, weatherKind, snowCover)
    val iceCover = resolveIceCoverage(season, weatherKind, snowCover, weather?.temperature)
    val snowfall = resolveSnowfallIntensity(weatherKind, weather?.snowMm1h)

    // Null sample → neutral cover (never treat as clear / 0% cloud)
    val cloudPct = weather?.cloudPct ?: 70
    val cloudDensity = when (weatherKind) {
        HeroSkyWeather.Clear -> (cloudPct / 100f) * 0.15f
        HeroSkyWeather.PartlyCloudy -> maxOf(0.35f, cloudPct / 100f * 0.9f)
        HeroSkyWeather.Overcast -> maxOf(0.80f, cloudPct / 100f * 0.95f)
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> maxOf(0.75f, cloudPct / 100f * 0.9f)
        HeroSkyWeather.Fog -> maxOf(0.45f, cloudPct / 100f * 0.7f)
    }.coerceIn(0f, 1f)

    val (diskA, glowA) = celestialVisibility(cloudPct, weatherKind)
    val lightMul = sceneLightMultiplier(weatherKind, cloudDensity)
    val shadowSoft = sceneShadowSoftness(weatherKind, cloudDensity)
    val haze = sceneFarHaze(weatherKind, weather?.visibilityMeters)

    val starVis = starVisibility(period, nowSec, riseSec, setSec, weatherKind, cloudDensity)
    val bodyX = when {
        sun?.aboveHorizon == true -> sun.xFrac
        moonApprox?.pos?.aboveHorizon == true -> moonApprox.pos.xFrac
        else -> 0.58f
    }
    val reflection = reflectionStrength(
        sunVisible = sun?.aboveHorizon == true,
        moonVisible = moonApprox?.pos?.aboveHorizon == true,
        cloudDensity = cloudDensity,
        weather = weatherKind,
        diskAlpha = diskA,
    )

    return HeroSkySnapshot(
        period = period,
        sun = sun,
        moon = moonApprox,
        starVisibility = starVis,
        weather = weatherKind,
        windy = windy,
        cloudDensity = cloudDensity,
        reflectionStrength = reflection,
        bodyXFrac = bodyX,
        nightWindowsLit = period == HeroSkyPeriod.Night || period == HeroSkyPeriod.Dusk,
        weatherStale = weatherStale,
        weatherLabel = heroWeatherDisplayLabel(weather, period, stale = weatherStale),
        temperatureLabel = heroOutdoorTemperatureLabel(weather),
        weatherBadgeIcon = heroWeatherBadgeIcon(weather, period),
        hasWeatherData = hasWeather,
        celestialDiskAlpha = diskA,
        celestialGlowAlpha = glowA,
        lightStrengthMul = lightMul,
        shadowSoftness = shadowSoft,
        farHaze = haze,
        season = season,
        seasonProgress = seasonProg,
        wind = wind.copy(
            // Keep bool windy aligned with motion threshold used elsewhere
            speedNorm = if (windy) wind.speedNorm.coerceAtLeast(0.50f) else wind.speedNorm,
        ),
        foliageAmount = foliage,
        snowCover = snowCover,
        mountainSnowCover = mountainSnow,
        iceCoverage = iceCover,
        snowfallIntensity = snowfall,
    )
}

/**
 * Directional light 1 = clear day; lower under clouds / precip.
 * Daytime overcast stays readable (never night-dark).
 */
fun sceneLightMultiplier(weather: HeroSkyWeather, cloudDensity: Float): Float {
    val base = when (weather) {
        HeroSkyWeather.Clear -> 1.00f
        HeroSkyWeather.PartlyCloudy -> 0.78f
        HeroSkyWeather.Fog -> 0.55f
        HeroSkyWeather.Overcast -> 0.58f
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0.48f
    }
    return (base * (1f - cloudDensity * 0.14f)).coerceIn(0.42f, 1f)
}

fun sceneShadowSoftness(weather: HeroSkyWeather, cloudDensity: Float): Float =
    when (weather) {
        HeroSkyWeather.Clear -> cloudDensity * 0.15f
        HeroSkyWeather.PartlyCloudy -> 0.30f + cloudDensity * 0.25f
        HeroSkyWeather.Fog -> 0.70f
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0.80f + cloudDensity * 0.15f
    }.coerceIn(0f, 1f)

fun sceneFarHaze(weather: HeroSkyWeather, visibilityMeters: Int?): Float {
    val fromVis = visibilityMeters?.let { v ->
        when {
            v < 1_000 -> 0.90f
            v < 3_000 -> 0.55f
            v < 6_000 -> 0.25f
            else -> 0f
        }
    } ?: 0f
    val fromWeather = when (weather) {
        HeroSkyWeather.Fog -> 0.85f
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0.40f
        HeroSkyWeather.Overcast -> 0.32f
        HeroSkyWeather.PartlyCloudy -> 0.10f
        HeroSkyWeather.Clear -> 0f
    }
    return maxOf(fromVis, fromWeather).coerceIn(0f, 1f)
}

private fun resolveSolarWindow(
    weather: WeatherInfo?,
    local: LocalDateTime,
    tz: TimeZone,
    lat: Double,
    lon: Double,
): Pair<Long, Long> {
    val rise = weather?.sunriseEpochSec
    val set = weather?.sunsetEpochSec
    if (rise != null && set != null && set > rise) return rise to set
    return approximateSolarTimes(local, tz, lat, lon)
}

/**
 * Approximate sunrise/sunset for [lat]/[lon] on [local]'s calendar day.
 * Based on a simplified day-of-year solar declination model — adequate for UI arcs,
 * not a substitute for a full astronomy library.
 */
fun approximateSolarTimes(
    local: LocalDateTime,
    tz: TimeZone,
    lat: Double,
    lon: Double,
): Pair<Long, Long> {
    val day = dayOfYear(local)
    val decl = solarDeclinationDeg(day)
    val latR = lat * PI / 180.0
    val declR = decl * PI / 180.0
    val cosHa = (sin(-0.83 * PI / 180.0) - sin(latR) * sin(declR)) /
        (cos(latR) * cos(declR))
    val haDeg = if (cosHa >= 1.0) {
        0.0
    } else if (cosHa <= -1.0) {
        180.0
    } else {
        kotlin.math.acos(cosHa) * 180.0 / PI
    }
    val eqnTime = 9.87 * sin(2.0 * (day - 81) * PI / 182.5) -
        7.53 * cos((day - 81) * PI / 182.5) -
        1.5 * sin((day - 81) * PI / 182.5)
    val solarNoonMin = 720.0 - 4.0 * lon - eqnTime
    val halfDayMin = haDeg * 4.0
    val riseMin = (solarNoonMin - halfDayMin).coerceIn(0.0, 1439.0)
    val setMin = (solarNoonMin + halfDayMin).coerceIn(0.0, 1439.0)
    val dayStart = LocalDateTime(local.year, local.monthNumber, local.dayOfMonth, 0, 0, 0, 0)
        .toInstant(tz).epochSeconds
    return (dayStart + (riseMin * 60).toLong()) to (dayStart + (setMin * 60).toLong())
}

private fun dayOfYear(local: LocalDateTime): Int {
    val monthDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val leap = local.year % 4 == 0 && (local.year % 100 != 0 || local.year % 400 == 0)
    if (leap) monthDays[2] = 29
    var d = local.dayOfMonth
    for (m in 1 until local.monthNumber) d += monthDays[m]
    return d
}

fun skyPeriodFromSolar(nowSec: Long, riseSec: Long, setSec: Long): HeroSkyPeriod {
    val dawnStart = riseSec - 45 * 60
    val dawnEnd = riseSec + 55 * 60
    val duskStart = setSec - 55 * 60
    val duskEnd = setSec + 45 * 60
    return when {
        nowSec in dawnStart until dawnEnd -> HeroSkyPeriod.Dawn
        nowSec in dawnEnd until duskStart -> HeroSkyPeriod.Day
        nowSec in duskStart until duskEnd -> HeroSkyPeriod.Dusk
        else -> HeroSkyPeriod.Night
    }
}

/**
 * Solar elevation (degrees) for [lat], solar declination [declDeg], and hour angle [hourAngleDeg]
 * (0° = solar noon, positive west of noon).
 */
fun solarElevationDeg(lat: Double, declDeg: Double, hourAngleDeg: Double): Double {
    val latR = lat * PI / 180.0
    val declR = declDeg * PI / 180.0
    val haR = hourAngleDeg * PI / 180.0
    val sinEl = sin(latR) * sin(declR) + cos(latR) * cos(declR) * cos(haR)
    return asin(sinEl.coerceIn(-1.0, 1.0)) * 180.0 / PI
}

fun solarDeclinationDeg(dayOfYear: Int): Double =
    23.44 * sin((360.0 / 365.0) * (dayOfYear - 81) * PI / 180.0)

/**
 * Card sun position from local solar time + latitude.
 * - X: east→west stylized arc (left at sunrise, right at sunset) using rise/set for this lat/lon.
 * - Y: real-ish solar elevation for this latitude and day (winter sun lower, summer higher).
 */
fun stylizedSunPosition(
    nowSec: Long,
    riseSec: Long,
    setSec: Long,
    lat: Double = 41.0,
    dayOfYear: Int = 172,
): HeroCelestialPos? {
    if (setSec <= riseSec) return null
    val decl = solarDeclinationDeg(dayOfYear)
    val noonSec = (riseSec + setSec) / 2.0
    val haDeg = (nowSec - noonSec) / 240.0 // 15°/hour ⇒ 240 s/degree
    val elevDeg = solarElevationDeg(lat, decl, haDeg)
    val maxElev = solarElevationDeg(lat, decl, 0.0).coerceAtLeast(8.0)

    if (nowSec < riseSec || nowSec > setSec || elevDeg <= -0.3) {
        val tEdge = if (nowSec < riseSec) 0.0 else 1.0
        return HeroCelestialPos(
            xFrac = (0.08 + tEdge * 0.84).toFloat(),
            yFrac = 0.72f,
            aboveHorizon = false,
        )
    }

    val t = ((nowSec - riseSec).toDouble() / (setSec - riseSec).toDouble()).coerceIn(0.0, 1.0)
    val x = (0.08 + t * 0.84).toFloat()
    // Map elevation 0°→horizon band, maxElev→near top of sky
    val elevNorm = (elevDeg / maxElev).coerceIn(0.0, 1.0)
    val y = (0.62 - elevNorm * 0.44).toFloat().coerceIn(0.12f, 0.64f)
    return HeroCelestialPos(xFrac = x, yFrac = y, aboveHorizon = elevDeg > 0.0 && y < 0.64f)
}

/**
 * Approximate moon illumination (synodic) + path shifted from the sun,
 * with elevation damped by the same latitude model.
 */
fun approximateMoonState(
    now: Instant,
    sun: HeroCelestialPos?,
    riseSec: Long,
    setSec: Long,
    lat: Double = 41.0,
    dayOfYear: Int = 172,
): HeroMoonState {
    val (illum, waxing, ageDays) = moonIllumination(now)
    val lagSec = (ageDays / 29.530588) * 24.0 * 3600.0
    val dayLen = (setSec - riseSec).coerceAtLeast(6 * 3600)
    val mRise = riseSec + (lagSec * 0.85).toLong()
    val mSet = mRise + (dayLen * 1.05).toLong()
    val raw = stylizedSunPosition(now.epochSeconds, mRise, mSet, lat, dayOfYear)
        ?: HeroCelestialPos(0.72f, 0.70f, aboveHorizon = false)
    @Suppress("UNUSED_VARIABLE")
    val unusedSun = sun
    val adjusted = raw.copy(
        yFrac = (raw.yFrac - 0.04f).coerceIn(0.08f, 0.70f),
        xFrac = (raw.xFrac + 0.04f).coerceIn(0.06f, 0.94f),
    )
    return HeroMoonState(pos = adjusted, illumination = illum, waxing = waxing)
}

/**
 * Synodic-month illumination. Reference new moon: 2000-01-06 18:14 UTC (approx).
 */
fun moonIllumination(now: Instant): Triple<Float, Boolean, Double> {
    val knownNew = Instant.parse("2000-01-06T18:14:00Z").epochSeconds.toDouble()
    val age = ((now.epochSeconds - knownNew) % 2_551_442.8 + 2_551_442.8) % 2_551_442.8
    val ageDays = age / 86_400.0
    val phase = ageDays / 29.530588
    val illum = ((1.0 - cos(phase * 2.0 * PI)) * 0.5).toFloat().coerceIn(0f, 1f)
    val waxing = phase < 0.5
    return Triple(illum, waxing, ageDays)
}

private fun starVisibility(
    period: HeroSkyPeriod,
    nowSec: Long,
    riseSec: Long,
    setSec: Long,
    weather: HeroSkyWeather,
    cloudDensity: Float,
): Float {
    val weatherMul = when (weather) {
        HeroSkyWeather.Clear -> 1f
        HeroSkyWeather.PartlyCloudy -> (1f - cloudDensity * 0.65f).coerceIn(0.15f, 1f)
        HeroSkyWeather.Fog -> 0.12f
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0f
    }
    if (weatherMul <= 0f) return 0f
    val afterSet = nowSec - setSec
    val beforeRise = riseSec - nowSec
    val fadeIn = when {
        period == HeroSkyPeriod.Day -> 0f
        period == HeroSkyPeriod.Dawn -> {
            val t = ((riseSec - nowSec).toFloat() / (45f * 60f)).coerceIn(0f, 1f)
            t
        }
        period == HeroSkyPeriod.Dusk -> {
            val t = ((nowSec - (setSec - 55 * 60)).toFloat() / (55f * 60f)).coerceIn(0f, 1f)
            t * 0.85f
        }
        else -> {
            when {
                afterSet in 0..(45 * 60) -> afterSet / (45f * 60f)
                beforeRise in 0..(45 * 60) -> beforeRise / (45f * 60f)
                else -> 1f
            }
        }
    }
    return (fadeIn * weatherMul).coerceIn(0f, 1f)
}

private fun reflectionStrength(
    sunVisible: Boolean,
    moonVisible: Boolean,
    cloudDensity: Float,
    weather: HeroSkyWeather,
    diskAlpha: Float,
): Float {
    if (!sunVisible && !moonVisible) return 0.08f
    // No sharp sun glint under overcast / heavy precip
    when (weather) {
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow -> return 0.05f
        HeroSkyWeather.Fog -> return 0.08f
        else -> Unit
    }
    // Moon path is weaker than sun; clouds further mute it
    val base = if (sunVisible) 1f else 0.32f * diskAlpha.coerceIn(0f, 1f)
    val cloudCut = 1f - cloudDensity * 0.82f
    val diskCut = 0.18f + diskAlpha * 0.82f
    return (base * cloudCut * diskCut).coerceIn(0f, 1f)
}

/** 0 = full day, 1 = deep night — shared dimming (not per-element period switches). */
fun sceneNightAmount(period: HeroSkyPeriod): Float = when (period) {
    HeroSkyPeriod.Day -> 0f
    HeroSkyPeriod.Dawn -> 0.22f
    HeroSkyPeriod.Dusk -> 0.48f
    HeroSkyPeriod.Night -> 1f
}

/**
 * Light warmth −1…+1: positive = dawn/dusk sun warmth on lit faces;
 * negative = cool overcast / night. Never paints the whole scene orange.
 */
fun sceneLightWarmth(period: HeroSkyPeriod, weather: HeroSkyWeather, cloudDensity: Float): Float {
    val c = cloudDensity.coerceIn(0f, 1f)
    val coolWx = when (weather) {
        HeroSkyWeather.Clear -> 0f
        HeroSkyWeather.PartlyCloudy -> 0.22f * c
        HeroSkyWeather.Fog -> 0.45f
        HeroSkyWeather.Overcast -> 0.55f
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0.60f
    }
    val warmPeriod = when (period) {
        HeroSkyPeriod.Dawn, HeroSkyPeriod.Dusk -> 0.72f * (1f - coolWx)
        HeroSkyPeriod.Day -> 0.12f * (1f - coolWx)
        HeroSkyPeriod.Night -> -0.35f
    }
    return (warmPeriod - coolWx * 0.55f).coerceIn(-0.85f, 0.85f)
}

/** Map legacy hour buckets → sky period when no solar data (previews / tests). */
fun heroSkyPeriodFromHour(hour: Int): HeroSkyPeriod = when (hour) {
    in 5..7 -> HeroSkyPeriod.Dawn
    in 8..16 -> HeroSkyPeriod.Day
    in 17..19 -> HeroSkyPeriod.Dusk
    else -> HeroSkyPeriod.Night
}

/** Compatibility: keep old API used by previews. */
fun heroDayPeriodFromHour(hour: Int): HeroDayPeriod = when (heroSkyPeriodFromHour(hour)) {
    HeroSkyPeriod.Dawn, HeroSkyPeriod.Day -> HeroDayPeriod.Day
    HeroSkyPeriod.Dusk -> HeroDayPeriod.Evening
    HeroSkyPeriod.Night -> HeroDayPeriod.Night
}

fun HeroSkyPeriod.toHeroDayPeriod(): HeroDayPeriod = when (this) {
    HeroSkyPeriod.Dawn, HeroSkyPeriod.Day -> HeroDayPeriod.Day
    HeroSkyPeriod.Dusk -> HeroDayPeriod.Evening
    HeroSkyPeriod.Night -> HeroDayPeriod.Night
}
