package com.hextech.vango

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared scene light — one model for sky, mountains, trees, water, ground, vehicle.
 * [dirX] > 0 → light from left, shadows stretch right.
 * [elev] 0 = low / long shadows, 1 = high / short.
 * [warmth] −1…+1 cool↔warm (lit faces / horizon only — not a whole-scene wash).
 */
internal data class HeroSceneLight(
    val dirX: Float,
    val elev: Float,
    val strength: Float,
    val ambient: Float = 0.55f,
    val direct: Float = 0.55f,
    val warmth: Float = 0f,
    val soft: Float = 0f,
    val haze: Float = 0f,
    val nightAmount: Float = 0f,
)

internal fun heroSceneLight(
    sky: HeroSkySnapshot,
    sunX: Float,
    sunY: Float,
    moonX: Float,
    moonY: Float,
    lightMul: Float,
    diskAlpha: Float,
    shadowSoft: Float,
    farHaze: Float,
    nightAmount: Float,
    warmth: Float,
): HeroSceneLight {
    val sunUp = sky.sun?.aboveHorizon == true && sunY < 0.64f
    val moonUp = sky.moon?.pos?.aboveHorizon == true && moonY < 0.64f
    val diskFactor = diskAlpha.coerceIn(0f, 1f)
    val soft = shadowSoft.coerceIn(0f, 1f)
    val haze = farHaze.coerceIn(0f, 1f)
    val night = nightAmount.coerceIn(0f, 1f)
    val warm = warmth.coerceIn(-1f, 1f)

    val (dirX, elev, directRaw) = when {
        sunUp -> {
            val elev = (1f - sunY.coerceIn(0.12f, 0.64f) / 0.64f).coerceIn(0.12f, 1f)
            val dir = (0.5f - sunX).coerceIn(-1f, 1f) * (0.20f + 0.80f * diskFactor)
            Triple(dir, elev, (0.40f + 0.60f * diskFactor) * lightMul)
        }
        moonUp -> Triple(
            (0.5f - moonX).coerceIn(-1f, 1f) * diskFactor.coerceAtLeast(0.15f),
            0.32f,
            0.28f * lightMul * diskFactor.coerceAtLeast(0.12f),
        )
        else -> Triple(0.22f, 0.40f, 0.12f * lightMul)
    }
    // Soft clouds raise fill light, cut hard beams — daytime stays readable
    val ambient = (
        (0.42f + 0.28f * soft) * (1f - night * 0.55f) * (0.75f + 0.25f * lightMul)
        ).coerceIn(0.22f, 0.85f)
    val direct = (directRaw * (1f - soft * 0.55f) * (1f - night * 0.35f)).coerceIn(0f, 1f)
    val strength = (ambient * 0.45f + direct * 0.55f).coerceIn(0.28f, 1f)
    return HeroSceneLight(
        dirX = dirX,
        elev = elev,
        strength = strength,
        ambient = ambient,
        direct = direct,
        warmth = warm,
        soft = soft,
        haze = haze,
        nightAmount = night,
    )
}

/** Warm tint for lit faces / horizon (not full-scene). */
internal val HeroWarmLit = Color(0xFFFFE0B8)
/** Cool tint for shade / overcast body. */
internal val HeroCoolShade = Color(0xFFB8C8D4)
/** Night cool mute. */
internal val HeroNightMute = Color(0xFF1A2838)

/** Lit surface: mild warmth when sun-warm; cool under cloud; night mute. */
internal fun HeroSceneLight.litColor(base: Color, amount: Float = 1f): Color {
    val a = amount.coerceIn(0f, 1f)
    var c = base
    if (warmth > 0.02f) {
        c = lerpColor(c, HeroWarmLit, warmth * direct * 0.28f * a)
    } else if (warmth < -0.02f) {
        c = lerpColor(c, HeroCoolShade, (-warmth) * 0.32f * a)
    }
    if (nightAmount > 0.02f) {
        c = lerpColor(c, HeroNightMute, nightAmount * 0.38f * a)
    }
    return c
}

/** Shade surface: slightly cooler when directional sun is strong. */
internal fun HeroSceneLight.shadeColor(base: Color, amount: Float = 1f): Color {
    val a = amount.coerceIn(0f, 1f)
    var c = lerpColor(base, HeroCoolShade, direct * 0.18f * (1f - soft) * a)
    if (nightAmount > 0.02f) {
        c = lerpColor(c, HeroNightMute, nightAmount * 0.45f * a)
    }
    return c
}
