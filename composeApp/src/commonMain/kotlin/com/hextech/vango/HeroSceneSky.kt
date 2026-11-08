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
import androidx.compose.ui.graphics.ClipOp
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

/** Fixed star positions (normalized); never under clock band (x≲0.35, y≲0.16). */
internal val HeroStarPositions: List<Pair<Float, Float>> = listOf(
    0.40f to 0.13f,
    0.47f to 0.09f,
    0.55f to 0.15f,
    0.66f to 0.11f,
    0.73f to 0.17f,
    0.81f to 0.12f,
    0.88f to 0.19f,
)

internal fun DrawScope.drawHeroCelestial(
    palette: HeroScenePalette,
    sky: HeroSkySnapshot,
    w: Float,
    h: Float,
    sunX: Float,
    sunY: Float,
    moonX: Float,
    moonY: Float,
    starVis: Float,
    fxT: Float,
    animateMotion: Boolean,
    diskAlpha: Float,
    glowAlpha: Float,
) {
    val r = min(w, h) * 0.052f
    val twinkleT = if (animateMotion) fxT else 0f

    // Shared slow field rotation for stars (not independent wander)
    val rot = if (animateMotion) twinkleT * 0.000035f else 0f
    if (starVis > 0.02f) {
        HeroStarPositions.forEachIndexed { i, (sx, sy) ->
            val cx0 = w * sx
            val cy0 = h * sy
            val pivotX = w * 0.55f
            val pivotY = h * 0.20f
            val dx = cx0 - pivotX
            val dy = cy0 - pivotY
            val c = kotlin.math.cos(rot.toDouble()).toFloat()
            val s = kotlin.math.sin(rot.toDouble()).toFloat()
            val x = pivotX + dx * c - dy * s
            val y = pivotY + dx * s + dy * c
            val tw = 0.55f + 0.45f * sin(twinkleT * (0.35f + i * 0.07f) + i * 1.7f)
            val a = (starVis * tw * (if (palette.star.alpha > 0.01f) palette.star.alpha else 0.78f))
                .coerceIn(0f, 1f)
            if (a > 0.02f) {
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = 1.05.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }
    }

    val sunAbove = sky.sun?.aboveHorizon == true && sunY < 0.64f
    if (sunAbove && (diskAlpha > 0.02f || glowAlpha > 0.02f)) {
        drawSoftSun(
            center = Offset(w * sunX, h * sunY),
            radius = r,
            diskAlpha = diskAlpha,
            glowAlpha = glowAlpha,
            warm = sky.period == HeroSkyPeriod.Dawn || sky.period == HeroSkyPeriod.Dusk,
        )
    }

    val moon = sky.moon
    val moonAbove = moon != null && moon.pos.aboveHorizon && moonY < 0.64f && moon.illumination > 0.03f
    if (moonAbove && moon != null && (diskAlpha > 0.02f || glowAlpha > 0.02f)) {
        val mc = Offset(w * moonX, h * moonY)
        if (glowAlpha > 0.04f) {
            // Soft radial glow — no hard outer ring
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.14f * glowAlpha),
                        0.45f to Color.White.copy(alpha = 0.05f * glowAlpha),
                        1.0f to Color.Transparent,
                    ),
                    center = mc,
                    radius = r * 2.8f,
                ),
                radius = r * 2.8f,
                center = mc,
            )
        }
        if (diskAlpha > 0.02f) {
            drawMoonPhase(
                center = mc,
                radius = r * 0.92f,
                illumination = moon.illumination,
                waxing = moon.waxing,
                lit = palette.moon.copy(alpha = diskAlpha),
                shade = palette.skyTop.copy(alpha = 0.92f * diskAlpha),
            )
        }
    }
}

/**
 * Soft sun: cream core → warm yellow rim; glow is a radial fade (no hard halo ring).
 */
internal fun DrawScope.drawSoftSun(
    center: Offset,
    radius: Float,
    diskAlpha: Float,
    glowAlpha: Float,
    warm: Boolean,
) {
    val glowR = radius * (if (warm) 3.6f else 3.2f)
    if (glowAlpha > 0.02f) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFFE8A0).copy(alpha = 0.42f * glowAlpha),
                    0.28f to Color(0xFFFFD070).copy(alpha = 0.18f * glowAlpha),
                    0.55f to Color(0xFFFFC060).copy(alpha = 0.06f * glowAlpha),
                    1.00f to Color.Transparent,
                ),
                center = center,
                radius = glowR,
            ),
            radius = glowR,
            center = center,
        )
    }
    if (diskAlpha <= 0.02f) return
    val diskR = radius * (if (warm) 0.95f else 1f)
    // Soft disk: open cream center → warm yellow edge (no stroked ring)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFFFF8E8).copy(alpha = diskAlpha),
                0.55f to Color(0xFFFFE8B0).copy(alpha = diskAlpha),
                0.88f to Color(0xFFFFD060).copy(alpha = diskAlpha * 0.95f),
                1.00f to Color(0xFFFFC040).copy(alpha = 0f),
            ),
            center = center,
            radius = diskR,
        ),
        radius = diskR,
        center = center,
    )
}

/**
 * Lit moon disk with terminator via clip — no outer halo / ring.
 * Illumination 0=new … 1=full; waxing grows from the right.
 */
internal fun DrawScope.drawMoonPhase(
    center: Offset,
    radius: Float,
    illumination: Float,
    waxing: Boolean,
    lit: Color,
    shade: Color,
) {
    val illum = illumination.coerceIn(0f, 1f)
    if (illum < 0.02f) return
    if (illum > 0.98f) {
        drawCircle(lit, radius = radius, center = center)
        return
    }
    drawCircle(shade, radius = radius, center = center)
    val dir = if (waxing) 1f else -1f
    val offset = dir * radius * (1.15f - illum * 2.05f)
    val cut = Path().apply {
        addOval(
            Rect(
                left = center.x + offset - radius * 0.98f,
                top = center.y - radius * 0.98f,
                right = center.x + offset + radius * 0.98f,
                bottom = center.y + radius * 0.98f,
            ),
        )
    }
    clipPath(cut, ClipOp.Difference) {
        drawCircle(lit, radius = radius, center = center)
    }
}
