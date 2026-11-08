package com.hextech.vango

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime
import kotlin.math.sin

/** Calendar season for vegetation / ground (not weather precip). */
enum class SeasonState {
    Spring,
    Summer,
    Autumn,
    Winter,
}

/**
 * Wind for shared scene motion — direction in card space, strength 0–1.
 * [dirX] > 0 pushes features toward the right (matches light.dirX convention loosely).
 */
data class WindState(
    /** −1…+1 horizontal push in card space (from meteorological deg). */
    val dirX: Float,
    /** 0 calm → 1 strong gale (from windSpeedMs). */
    val speedNorm: Float,
    /** Soft gust modulation frequency (Hz-ish for sin clocks). */
    val gustHz: Float = 0.35f,
) {
    val isBreezy: Boolean get() = speedNorm > 0.22f
    val isWindy: Boolean get() = speedNorm > 0.48f

    companion object {
        val Calm = WindState(dirX = 0.15f, speedNorm = 0.08f)
    }
}

/**
 * Map meteorological wind FROM degrees (0=N, 90=E, 180=S, 270=W)
 * → card horizontal push (−1 left … +1 right).
 *
 * Wind FROM west (270°) blows toward east → +dirX (features drift right).
 * Card is not compass-locked; +X is a consistent visual “east/right”.
 */
fun windDirXFromDeg(deg: Float?): Float {
    if (deg == null) return 0.20f
    val rad = Math.toRadians(deg.toDouble())
    // Eastward component of TO direction = −sin(fromDeg)
    return (-sin(rad)).toFloat().coerceIn(-1f, 1f)
}

fun windSpeedNorm(speedMs: Float): Float =
    ((speedMs - 0.5f) / 12f).coerceIn(0f, 1f)

fun resolveWindState(info: WeatherInfo?): WindState {
    if (info == null) return WindState(dirX = 0.18f, speedNorm = 0.20f)
    val speed = windSpeedNorm(info.windSpeedMs)
    return WindState(
        dirX = windDirXFromDeg(info.windDeg),
        speedNorm = speed,
        gustHz = 0.28f + speed * 0.35f,
    )
}

/**
 * Season from local calendar month + latitude hemisphere.
 * Southern hemisphere months are shifted by 6.
 */
fun resolveSeasonState(local: LocalDateTime, latitude: Double): SeasonState {
    val month = local.monthNumber // 1–12
    val m = if (latitude < 0.0) ((month + 5) % 12) + 1 else month
    return when (m) {
        3, 4, 5 -> SeasonState.Spring
        6, 7, 8 -> SeasonState.Summer
        9, 10, 11 -> SeasonState.Autumn
        else -> SeasonState.Winter
    }
}

/**
 * Continuous year-phase season progress 0…1 (NH):
 * 0.00 = Mar 1 (spring) · 0.25 = Jun 1 (summer) · 0.50 = Sep 1 (autumn) · 0.75 = Dec 1 (winter).
 * Do **not** swap four discrete scenes — blend vegetation along this curve.
 */
fun seasonProgress(local: LocalDateTime, latitude: Double): Float {
    val doy = dayOfYearApprox(local)
    val shifted = if (latitude < 0.0) (doy + 183) % 366 else doy
    // Mar 1 ≈ day 60 (non-leap)
    val p = ((shifted - 60 + 366) % 366) / 366f
    return p.coerceIn(0f, 1f)
}

private fun dayOfYearApprox(local: LocalDateTime): Int {
    val md = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val leap = local.year % 4 == 0 && (local.year % 100 != 0 || local.year % 400 == 0)
    val base = md[(local.monthNumber - 1).coerceIn(0, 11)] + local.dayOfMonth
    return if (leap && local.monthNumber > 2) base + 1 else base
}

/**
 * Dominant season + blend t toward the next (smooth near boundaries).
 * [t] is 0 in mid-season, rises in the last ~transitionWidth of each quarter.
 */
fun seasonBlend(progress: Float, transitionWidth: Float = 0.10f): Triple<SeasonState, SeasonState, Float> {
    val order = arrayOf(
        SeasonState.Spring, SeasonState.Summer, SeasonState.Autumn, SeasonState.Winter,
    )
    val phase = (progress.coerceIn(0f, 1f) * 4f) % 4f
    val idx = phase.toInt().coerceIn(0, 3)
    val frac = phase - idx
    val tw = transitionWidth.coerceIn(0.04f, 0.24f)
    val t = if (frac < 1f - tw) {
        0f
    } else {
        heroSmoothstep((frac - (1f - tw)) / tw)
    }
    val from = order[idx]
    val to = order[(idx + 1) % 4]
    return Triple(from, to, t)
}

/**
 * Continuous autumn advance 0–1 within Sep–Nov (early → mid → late).
 * Drives canopy mix, gaps, and ground litter — not weather.
 */
fun autumnProgress(month: Int, dayOfMonth: Int): Float {
    val base = when (month) {
        9 -> 0.00f
        10 -> 0.38f
        11 -> 0.72f
        else -> 0.50f
    }
    return (base + (dayOfMonth.coerceIn(1, 31) / 31f) * 0.30f).coerceIn(0f, 1f)
}

/**
 * Deciduous canopy fill 0–1 from continuous [seasonProgress].
 * Spring: leaves appear · summer: full · autumn: color then thin · winter: bare.
 * Cluster **geometry** stays fixed; only fill/opacity changes.
 */
fun foliageFromSeasonProgress(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    // Piecewise continuous curve across year phase
    return when {
        p < 0.25f -> {
            // Spring: 0.55 → 0.95
            val t = heroSmoothstep(p / 0.25f)
            lerpFloat(0.55f, 0.95f, t)
        }
        p < 0.50f -> {
            // Summer: hold full
            val t = heroSmoothstep((p - 0.25f) / 0.25f)
            lerpFloat(0.95f, 1.0f, t)
        }
        p < 0.75f -> {
            // Autumn: thin after mid
            val t = heroSmoothstep((p - 0.50f) / 0.25f)
            lerpFloat(0.92f, 0.28f, t)
        }
        else -> {
            // Winter: bare
            val t = heroSmoothstep((p - 0.75f) / 0.25f)
            lerpFloat(0.28f, 0.02f, t)
        }
    }.coerceIn(0.02f, 1f)
}

/**
 * Deciduous canopy fill 0–1 (pines stay evergreen separately).
 * Prefer [foliageFromSeasonProgress] when continuous progress is available.
 */
fun seasonFoliageAmount(season: SeasonState, dayOfMonth: Int, month: Int = 10): Float = when (season) {
    SeasonState.Spring -> 0.82f + (dayOfMonth / 31f) * 0.14f
    SeasonState.Summer -> 1f
    SeasonState.Autumn -> {
        val p = autumnProgress(month, dayOfMonth)
        (0.90f - p * 0.62f).coerceIn(0.22f, 0.90f)
    }
    SeasonState.Winter -> 0.02f
}

/**
 * Surface snow accumulation 0–1 (ground / branches / roof).
 * Driven by snow weather volume — not by SeasonState alone.
 * Winter without snow weather → 0 (bare winter scene allowed).
 * Active snowfall stopping later can keep cover if caller caches; here cover tracks sample.
 */
fun resolveSnowCover(weather: HeroSkyWeather, snowMm1h: Float?, temperatureC: Float?): Float {
    if (weather != HeroSkyWeather.Snow) return 0f
    val vol = snowMm1h ?: 0f
    val cold = ((temperatureC ?: 0f) < 1f)
    val fromVol = if (vol > 0.05f) {
        (0.22f + (vol / 4.0f) * 0.40f).coerceIn(0.20f, 0.55f)
    } else {
        0.20f
    }
    return if (cold) fromVol else fromVol * 0.55f
}

/**
 * Falling-snow particle intensity 0–1 — only while weather reports snow.
 * Independent of [resolveSnowCover] (ground can stay white after flakes stop).
 */
fun resolveSnowfallIntensity(weather: HeroSkyWeather, snowMm1h: Float?): Float {
    if (weather != HeroSkyWeather.Snow) return 0f
    val vol = snowMm1h ?: 0.6f
    return (0.55f + (vol / 3.0f) * 0.45f).coerceIn(0.50f, 1f)
}

/**
 * Narrow shore ice rim 0–1 — never freeze the whole lake just because it's winter.
 */
fun resolveIceCoverage(
    season: SeasonState,
    weather: HeroSkyWeather,
    snowCover: Float,
    temperatureC: Float?,
): Float {
    if (season != SeasonState.Winter) return 0f
    val cold = (temperatureC ?: 0f) <= -1f
    if (!cold) return 0f
    val base = when (weather) {
        HeroSkyWeather.Snow -> 0.10f + snowCover * 0.10f
        HeroSkyWeather.Overcast, HeroSkyWeather.Fog -> 0.06f + snowCover * 0.06f
        else -> if (snowCover > 0.25f) 0.05f else 0f
    }
    return base.coerceIn(0f, 0.22f)
}

/**
 * Residual peak snow on mountains — separate from ground [resolveSnowCover].
 * Spring: faint crest leftovers only. Never auto-thick from “it's spring”.
 */
fun resolveMountainSnowCover(
    season: SeasonState,
    weather: HeroSkyWeather,
    groundSnow: Float,
): Float = when (season) {
    SeasonState.Winter -> (0.62f + groundSnow * 0.28f).coerceIn(0.55f, 0.92f)
    SeasonState.Autumn -> if (weather == HeroSkyWeather.Snow) 0.28f else 0.08f
    SeasonState.Spring -> when (weather) {
        HeroSkyWeather.Snow -> 0.22f
        else -> 0.14f // residual crevices only
    }
    SeasonState.Summer -> if (weather == HeroSkyWeather.Snow) 0.10f else 0.03f
}

/** Winter snow pack palette (lit / main / hollow / contact). */
val HeroSnowLit = Color(0xFFEEF3F6)
val HeroSnowMain = Color(0xFFDAE5EE)
val HeroSnowHollow = Color(0xFFB8CCDC)
val HeroSnowContact = Color(0xFF819AAE)

/** Spring deciduous tones — shade / main / shoot. */
val HeroSpringLeafShade = Color(0xFF456B35)
val HeroSpringLeafMain = Color(0xFF78A847)
val HeroSpringLeafShoot = Color(0xFFB5CD68)

/** Summer deciduous — darker, fuller (shade / main / lit). */
val HeroSummerLeafShade = Color(0xFF294C2E)
val HeroSummerLeafMain = Color(0xFF47783C)
val HeroSummerLeafLit = Color(0xFF7EAA50)

/** Summer grass — green blades + dry clumps. */
val HeroSummerGrassGreen = Color(0xFF6D853E)
val HeroSummerGrassDry = Color(0xFFB99B55)
val HeroSummerGrassDryTip = Color(0xFFD6BE79)

/** Autumn deciduous — residual green / gold / copper / shade brown. */
val HeroAutumnLeafGreen = Color(0xFF647447)
val HeroAutumnLeafGold = Color(0xFFD2AA48)
val HeroAutumnLeafCopper = Color(0xFFB9753E)
val HeroAutumnLeafShade = Color(0xFF735038)

/** Autumn grass — pale olive / straw / light brown. */
val HeroAutumnGrassOlive = Color(0xFF6E7348)
val HeroAutumnGrassStraw = Color(0xFFC4A85A)
val HeroAutumnGrassBrown = Color(0xFF8A6E48)

/** Seasonal canopy tint for deciduous (pines use darker evergreen). */
fun seasonDeciduousColors(season: SeasonState, foliage: Float): Pair<Color, Color> = when (season) {
    SeasonState.Spring -> HeroSpringLeafShade to HeroSpringLeafMain
    SeasonState.Summer -> HeroSummerLeafShade to HeroSummerLeafMain
    SeasonState.Autumn -> {
        // Shade + main: brown/copper → gold; green is applied per-cluster
        val progress = ((0.90f - foliage) / 0.62f).coerceIn(0f, 1f)
        val shade = lerpColor(HeroAutumnLeafCopper, HeroAutumnLeafShade, progress * 0.85f)
        val main = lerpColor(HeroAutumnLeafGold, HeroAutumnLeafCopper, progress * 0.55f)
        shade to main
    }
    SeasonState.Winter -> Color(0xFF5A5048) to Color(0xFF6A6058)
}

/**
 * Continuous deciduous shade/main from [seasonProgress] — adjacent seasons blend.
 * Autumn prefers gold/copper before density drops (foliage handled separately).
 */
fun deciduousColorsFromProgress(progress: Float, foliage: Float): Pair<Color, Color> {
    val (from, to, t) = seasonBlend(progress)
    val a = seasonDeciduousColors(from, foliage)
    val b = seasonDeciduousColors(to, foliage)
    return lerpColor(a.first, b.first, t) to lerpColor(a.second, b.second, t)
}

/** Lit outer-leaf color for deciduous (spring shoot vs summer sunlit). */
fun seasonDeciduousLitLeaf(season: SeasonState): Color = when (season) {
    SeasonState.Spring -> HeroSpringLeafShoot
    SeasonState.Summer -> HeroSummerLeafLit
    SeasonState.Autumn -> HeroAutumnLeafGold
    SeasonState.Winter -> Color(0xFF6A6058)
}

fun deciduousLitFromProgress(progress: Float): Color {
    val (from, to, t) = seasonBlend(progress)
    return lerpColor(seasonDeciduousLitLeaf(from), seasonDeciduousLitLeaf(to), t)
}

/**
 * Per-cluster autumn tone (shade / main / lit) — mixes green early, gold+copper mid,
 * copper+brown late. [tone] 0=inner shade, 1=main, 2=outer lit.
 */
fun autumnClusterLeafColor(tone: Int, seed: Int, foliage: Float): Color {
    val progress = ((0.90f - foliage) / 0.62f).coerceIn(0f, 1f)
    val keepGreen = progress < 0.42f && (seed % 3 == 0)
    return when (tone) {
        0 -> lerpColor(HeroAutumnLeafCopper, HeroAutumnLeafShade, 0.35f + progress * 0.55f)
        2 -> if (keepGreen) {
            lerpColor(HeroAutumnLeafGreen, HeroAutumnLeafGold, 0.35f)
        } else {
            lerpColor(HeroAutumnLeafGold, HeroAutumnLeafCopper, progress * 0.40f)
        }
        else -> when {
            keepGreen -> HeroAutumnLeafGreen
            (seed % 2 == 0) -> lerpColor(HeroAutumnLeafGold, HeroAutumnLeafCopper, progress * 0.50f)
            else -> lerpColor(HeroAutumnLeafCopper, HeroAutumnLeafGold, 0.35f)
        }
    }
}

/** Seasonal grass base / mid / tip (green blades). */
fun seasonGrassColors(season: SeasonState): Triple<Color, Color, Color> = when (season) {
    SeasonState.Spring -> Triple(Color(0xFF3A5530), Color(0xFF5A7A40), HeroSpringLeafShoot)
    SeasonState.Summer -> Triple(
        lerpColor(HeroSummerGrassGreen, Color.Black, 0.18f),
        HeroSummerGrassGreen,
        lerpColor(HeroSummerGrassGreen, Color.White, 0.22f),
    )
    SeasonState.Autumn -> Triple(
        lerpColor(HeroAutumnGrassOlive, HeroAutumnGrassBrown, 0.35f),
        lerpColor(HeroAutumnGrassOlive, HeroAutumnGrassStraw, 0.40f),
        HeroAutumnGrassStraw,
    )
    SeasonState.Winter -> Triple(Color(0xFF4A4840), Color(0xFF5A5848), Color(0xFF7A7460))
}

fun grassColorsFromProgress(progress: Float): Triple<Color, Color, Color> {
    val (from, to, t) = seasonBlend(progress)
    val a = seasonGrassColors(from)
    val b = seasonGrassColors(to)
    return Triple(
        lerpColor(a.first, b.first, t),
        lerpColor(a.second, b.second, t),
        lerpColor(a.third, b.third, t),
    )
}

/** Soft winter amount 0…1 from continuous progress (for ground wash / lake tint). */
fun winterAmountFromProgress(progress: Float): Float {
    // Winter quarter 0.75…1.00, plus soft edges into late autumn / early spring
    val p = progress.coerceIn(0f, 1f)
    return when {
        p >= 0.75f -> heroSmoothstep((p - 0.75f) / 0.12f).coerceAtMost(1f)
        p < 0.08f -> 1f - heroSmoothstep(p / 0.08f) // early spring leftover
        p > 0.68f -> heroSmoothstep((p - 0.68f) / 0.07f) // late autumn into winter
        else -> 0f
    }.coerceIn(0f, 1f)
}

/** Spring flower density 0…1 from progress. */
fun flowerDensityFromProgress(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p < 0.05f -> 0f
        p < 0.22f -> heroSmoothstep((p - 0.05f) / 0.17f)
        p < 0.32f -> 1f - heroSmoothstep((p - 0.22f) / 0.10f) * 0.55f
        p < 0.48f -> 0.35f * (1f - heroSmoothstep((p - 0.32f) / 0.16f))
        else -> 0f
    }.coerceIn(0f, 1f)
}

/** Autumn straw blades (park edges) — pale olive base still allowed near water via reed. */
fun seasonAutumnDryGrassColors(): Triple<Color, Color, Color> =
    Triple(
        lerpColor(HeroAutumnGrassBrown, Color.Black, 0.10f),
        HeroAutumnGrassStraw,
        lerpColor(HeroAutumnGrassStraw, Color.White, 0.18f),
    )

/** Dry summer grass tones (park edges / corners). */
fun seasonDryGrassColors(): Triple<Color, Color, Color> =
    Triple(
        lerpColor(HeroSummerGrassDry, Color.Black, 0.12f),
        HeroSummerGrassDry,
        HeroSummerGrassDryTip,
    )

/**
 * Soft sway offset at height fraction t (0=base, 1=tip); base stays put.
 * Near-calm wind → no sway (continuous motion is waves/clouds only).
 * Phases are detuned so trees/grass never lock-step with lake ripples.
 */
fun windSwayOffsetX(
    tFromBase: Float,
    timeSec: Float,
    phase: Float,
    wind: WindState,
    scalePx: Float,
): Float {
    if (wind.speedNorm < 0.14f) return 0f
    val t = tFromBase.coerceIn(0f, 1f)
    val bend = t * t // root fixed, tip bends more
    val wave = sin((timeSec * wind.gustHz * (0.88f + phase * 0.05f) + phase) * (Math.PI * 2.0).toFloat())
    val flutter = 0.24f * sin((timeSec * wind.gustHz * 1.51f + phase * 1.41f) * (Math.PI * 2.0).toFloat())
    val amp = scalePx * (0.006f + wind.speedNorm * 0.032f) * bend
    return wind.dirX * amp * (wave * 0.80f + flutter * 0.20f)
}
