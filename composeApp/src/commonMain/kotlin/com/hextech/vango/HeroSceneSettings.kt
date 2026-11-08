package com.hextech.vango

/**
 * Named scene timings / opacities shared across modules.
 * Geometry design units live in [HeroSceneLayout] / vehicle files.
 */
internal object HeroSceneSettings {
    /** Master loop for lake ripples / park grass sway (ms). */
    const val LakeMasterMs: Int = HeroLakeMasterMs

    /** Weather / light cross-fade when period or conditions change (ms). */
    val WeatherEaseMs: Int get() = HeroSceneTransitionConfig.WeatherEaseMs

    /** Celestial position ease (ms) — live settle; sim uses hour ease. */
    val CelestialPosEaseMs: Int get() = HeroSceneTransitionConfig.LiveCelestialEaseMs

    /** Soft cloud drift clock length (seconds, wraps ~1h). */
    const val CloudDriftSec: Float = 3_600f

    /** Sky weather FX clip height fraction. */
    const val SkyClipHeightFrac: Float = 0.72f

    /** Vehicle width as fraction of card width. */
    const val VehicleWidthFrac: Float = HeroVehicleWidthFrac
}
