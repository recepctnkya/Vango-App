package com.hextech.vango

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Shared draw inputs for every hero landscape module.
 * Built once by the main scene; modules do not fetch weather or own timers.
 */
internal data class HeroSceneDrawState(
    val w: Float,
    val h: Float,
    val palette: HeroScenePalette,
    val sky: HeroSkySnapshot,
    val light: HeroSceneLight,
    val layout: HeroLayout,
    val vehicleType: HeroVehicleType,
    val waterT: Float,
    val cloudT: Float,
    val fxT: Float,
    val animateMotion: Boolean,
    val sunX: Float,
    val sunY: Float,
    val moonX: Float,
    val moonY: Float,
    val starVis: Float,
    val bodyXFrac: Float,
    val reflectionStrength: Float,
    val diskAlpha: Float,
    val glowAlpha: Float,
    val partlyCloudA: Float,
    val overcastCloudA: Float,
    val rainFxA: Float,
    val snowFxA: Float,
    val fogFxA: Float,
    val linkColor: Color,
    val linkVisual: HeroLinkVisual,
    val flowT: Float,
    val pulse: Float,
    val lakeClipPath: Path,
    val skyClipPath: Path,
    val layers: HeroSceneLayerFlags,
    val season: SeasonState = SeasonState.Summer,
    val seasonProgress: Float = 0.35f,
    val wind: WindState = WindState.Calm,
    val foliageAmount: Float = 1f,
    val snowCover: Float = 0f,
    val mountainSnowCover: Float = 0f,
    val iceCoverage: Float = 0f,
    val nightAmount: Float = 0f,
)
