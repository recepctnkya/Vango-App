package com.hextech.vango

import kotlin.math.abs

/**
 * Shared motion helpers for the hero card.
 * Card is not compass-locked: meteorological wind FROM → card +X (right) push.
 */

/** Horizontal push sign for drifting FX (−1 left … +1 right). Calm → gentle rightward. */
fun windDriftSign(wind: WindState): Float {
    val d = wind.dirX
    return when {
        abs(d) < 0.10f -> 0.35f
        else -> d.coerceIn(-1f, 1f)
    }
}

/**
 * Fractional exclusion zones matching Compose overlays (clock/temp, weather badge, status).
 * Particles in these regions are skipped so chrome stays readable.
 */
internal object HeroSceneUiZones {
    /** Top-start clock + temperature scrim. */
    fun hitsClockTemp(xf: Float, yf: Float): Boolean =
        xf < 0.44f && yf < 0.22f

    /** Top-end weather badge. */
    fun hitsWeatherBadge(xf: Float, yf: Float): Boolean =
        xf > 0.50f && yf < 0.20f

    /** Bottom status label between phone and hitch. */
    fun hitsStatus(xf: Float, yf: Float): Boolean =
        xf in 0.12f..0.60f && yf in 0.66f..0.90f

    /** Soft corridor around the phone→vehicle link. */
    fun hitsLinkCorridor(xf: Float, yf: Float): Boolean =
        xf in 0.10f..0.58f && yf in 0.62f..0.88f

    fun hitsOverlayText(xf: Float, yf: Float): Boolean =
        hitsClockTemp(xf, yf) || hitsWeatherBadge(xf, yf) || hitsStatus(xf, yf)

    fun hitsAnyChrome(xf: Float, yf: Float): Boolean =
        hitsOverlayText(xf, yf) || hitsLinkCorridor(xf, yf)
}
