package com.hextech.vango

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Three distinct body plans — not recolors of one silhouette. */
enum class HeroFishKind {
    /** Slim long body, small forked tail. */
    SlimFork,
    /** Short oval body, broad tail. */
    OvalFan,
    /** Larger long body, prominent dorsal fin. */
    DorsalLong,
}

/**
 * Fixed fish roster (7). Positions/speeds never randomized per frame.
 * [yFrac] 0 = far shore, 1 = near shore within lake band.
 * [lenFrac] of card width. [school] groups move with shared drift + phase offsets.
 */
private data class HeroFishSpec(
    val kind: HeroFishKind,
    val yFrac: Float,
    val lenFrac: Float,
    val speed: Float,
    val phase: Float,
    val ampY: Float,
    val school: Int,
    val color: Color,
    val baseFacingRight: Boolean,
)

private val HeroFishRoster: List<HeroFishSpec> = listOf(
    // Loose school of 3 small fish (mid-depth)
    HeroFishSpec(HeroFishKind.SlimFork, 0.42f, 0.007f, 0.028f, 0.00f, 0.012f, 1, Color(0xFF3A5A6E), true),
    HeroFishSpec(HeroFishKind.SlimFork, 0.46f, 0.006f, 0.026f, 0.35f, 0.010f, 1, Color(0xFF2E4A5C), true),
    HeroFishSpec(HeroFishKind.OvalFan, 0.44f, 0.0055f, 0.030f, 0.70f, 0.014f, 1, Color(0xFF456878), false),
    // Independent mid / near
    HeroFishSpec(HeroFishKind.OvalFan, 0.58f, 0.014f, 0.018f, 1.20f, 0.018f, 0, Color(0xFF2A4858), true),
    HeroFishSpec(HeroFishKind.DorsalLong, 0.62f, 0.020f, 0.014f, 2.10f, 0.010f, 0, Color(0xFF1E3848), false),
    HeroFishSpec(HeroFishKind.SlimFork, 0.70f, 0.012f, 0.022f, 0.55f, 0.016f, 0, Color(0xFF365868), true),
    HeroFishSpec(HeroFishKind.DorsalLong, 0.52f, 0.016f, 0.016f, 2.80f, 0.012f, 0, Color(0xFF344E5E), true),
)

/**
 * Draw fish between water fill and surface ripples/reflections.
 * [animate] false when card paused / app backgrounded.
 */
fun DrawScope.drawHeroLakeFish(
    w: Float,
    h: Float,
    lakeTop: Float,
    lakeBot: Float,
    waterT: Float,
    animate: Boolean,
    period: HeroSkyPeriod,
    weather: HeroSkyWeather,
    reflectionStrength: Float,
    timeSec: Float = -1f,
) {
    val lakeH = (lakeBot - lakeTop).coerceAtLeast(1f)
    val nightMul = when (period) {
        HeroSkyPeriod.Night -> 0.28f
        HeroSkyPeriod.Dusk -> 0.55f
        HeroSkyPeriod.Dawn -> 0.75f
        HeroSkyPeriod.Day -> 1f
    }
    val weatherMul = when (weather) {
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0.40f
        HeroSkyWeather.Overcast -> 0.65f
        HeroSkyWeather.Fog -> 0.50f
        HeroSkyWeather.PartlyCloudy -> 0.85f
        HeroSkyWeather.Clear -> 1f
    }
    // Strong surface glare / chop hides fish
    val surfaceMul = (1f - reflectionStrength * 0.55f).coerceIn(0.35f, 1f)
    val globalVis = (nightMul * weatherMul * surfaceMul).coerceIn(0f, 1f)
    if (globalVis < 0.04f) return

    // Prefer continuous scene seconds (avoids 10s Restart sync)
    val tSec = when {
        !animate -> 0f
        timeSec >= 0f -> timeSec
        else -> waterT * 10f
    }
    val twoPi = (PI * 2.0).toFloat()

    // School shared slow drift (not synced tail)
    val schoolDrift = if (animate) sin(tSec * 0.11f) * w * 0.04f else 0f

    HeroFishRoster.forEach { fish ->
        // Intermittent windows: short appear, long rest (deterministic, not every-frame RNG)
        val windowSec = 26f + fish.phase * 11f
        val appearSec = 4.8f + fish.phase * 2.2f
        val localT = if (animate) {
            ((tSec + fish.phase * 13.7f) % windowSec + windowSec) % windowSec
        } else {
            appearSec * 0.4f
        }
        if (localT > appearSec) return@forEach
        val windowFade = when {
            localT < 0.9f -> (localT / 0.9f).coerceIn(0f, 1f)
            localT > appearSec - 0.9f -> ((appearSec - localT) / 0.9f).coerceIn(0f, 1f)
            else -> 1f
        }
        if (windowFade < 0.04f) return@forEach

        val depth = fish.yFrac.coerceIn(0.15f, 0.88f)
        // Far = bluish soft; near = slightly clearer (still muted lake tones)
        val depthAlpha = (0.16f + depth * 0.38f) * globalVis
        val blueWash = (1f - depth) * 0.45f
        val bodyCol = lerpFishColor(fish.color, Color(0xFF6A8AA0), blueWash)
        val len = w * fish.lenFrac.coerceIn(0.004f, 0.025f)

        // Horizontal patrol with turn phases (no instant mirror)
        val travel = if (animate) {
            ((tSec * fish.speed * 0.42f) + fish.phase) % 1f
        } else {
            (fish.phase * 0.17f) % 1f
        }
        val (xNorm, facingRight, squashX) = fishTravelPose(travel, fish.baseFacingRight)
        val margin = len * 1.2f
        var x = margin + xNorm * (w - margin * 2f)
        if (fish.school == 1) x += schoolDrift * 0.6f + cos(fish.phase * twoPi) * w * 0.018f

        val yWave = if (animate) {
            sin((tSec * fish.speed * 0.95f + fish.phase) * twoPi) * lakeH * fish.ampY
        } else {
            0f
        }
        val y = lakeTop + lakeH * depth + yWave

        // Fade near shores (don't climb onto land)
        val shoreFade = when {
            depth < 0.22f -> ((depth - 0.12f) / 0.10f).coerceIn(0f, 1f)
            depth > 0.82f -> ((0.92f - depth) / 0.10f).coerceIn(0f, 1f)
            else -> 1f
        }
        // Soft vanish under mid-surface reflection band
        val reflectFade = if (depth in 0.35f..0.55f && reflectionStrength > 0.45f) {
            0.45f + 0.55f * (1f - reflectionStrength)
        } else {
            1f
        }
        val alpha = (
            depthAlpha * shoreFade * reflectFade * windowFade * squashX.coerceIn(0.35f, 1f)
            ).coerceIn(0f, 0.48f)
        if (alpha < 0.03f) return@forEach

        val tailPhase = if (animate) {
            (tSec * (2.2f + fish.speed * 8f) + fish.phase * 4f) * twoPi
        } else {
            fish.phase * twoPi
        }
        val tailWag = sin(tailPhase) * 9f * (0.6f + fish.lenFrac * 40f)
        // Very slight body pitch — no bounce
        val bodyPitch = if (animate) sin(tailPhase * 0.35f) * 2.2f else 0f

        val dir = if (facingRight) 1f else -1f
        rotate(degrees = bodyPitch, pivot = Offset(x, y)) {
            scale(scaleX = squashX * dir, scaleY = 1f, pivot = Offset(x, y)) {
                // Local drawing assumes nose to +X; scaleX handles facing
                when (fish.kind) {
                    HeroFishKind.SlimFork -> drawSlimForkFish(x, y, len, bodyCol.copy(alpha = alpha), tailWag)
                    HeroFishKind.OvalFan -> drawOvalFanFish(x, y, len, bodyCol.copy(alpha = alpha), tailWag)
                    HeroFishKind.DorsalLong -> drawDorsalLongFish(x, y, len, bodyCol.copy(alpha = alpha), tailWag)
                }
            }
        }
    }
}

/**
 * Patrol pose: cruise → slow → squash turn → reverse cruise.
 * Returns (xNorm 0–1, facingRight, horizontal squash 0.35–1).
 */
private fun fishTravelPose(t: Float, baseRight: Boolean): Triple<Float, Boolean, Float> {
    // Split into two legs with turn windows at ends
    val leg = if (t < 0.5f) t / 0.5f else (t - 0.5f) / 0.5f
    val goingRight = if (t < 0.5f) baseRight else !baseRight
    // Ease in/out on each leg
    val eased = smoothstep(leg)
    val x = if (t < 0.5f) eased else 1f - eased
    // Turn squash near leg ends
    val edge = when {
        leg < 0.12f -> leg / 0.12f
        leg > 0.88f -> (1f - leg) / 0.12f
        else -> 1f
    }
    val squash = 0.38f + 0.62f * edge.coerceIn(0f, 1f)
    return Triple(x.coerceIn(0f, 1f), goingRight, squash)
}

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerpFishColor(a: Color, b: Color, t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * x,
        green = a.green + (b.green - a.green) * x,
        blue = a.blue + (b.blue - a.blue) * x,
        alpha = a.alpha,
    )
}

/** 1) Slim elongated body + small forked tail. */
private fun DrawScope.drawSlimForkFish(cx: Float, cy: Float, len: Float, color: Color, wag: Float) {
    val bodyL = len * 0.72f
    val bodyH = len * 0.22f
    val nose = cx + bodyL * 0.48f
    val tailBase = cx - bodyL * 0.42f
    // Body
    drawPath(
        Path().apply {
            moveTo(nose, cy)
            cubicTo(nose - bodyL * 0.15f, cy - bodyH, tailBase + bodyL * 0.08f, cy - bodyH * 0.85f, tailBase, cy)
            cubicTo(tailBase + bodyL * 0.08f, cy + bodyH * 0.85f, nose - bodyL * 0.15f, cy + bodyH, nose, cy)
            close()
        },
        color = color,
    )
    // Tiny dorsal
    drawPath(
        Path().apply {
            moveTo(cx - bodyL * 0.05f, cy - bodyH * 0.7f)
            lineTo(cx + bodyL * 0.08f, cy - bodyH * 1.35f)
            lineTo(cx + bodyL * 0.18f, cy - bodyH * 0.55f)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.85f),
    )
    // Forked tail (upper / lower lobes), wagged
    val tw = abs(wag) * 0.015f * len
    drawPath(
        Path().apply {
            moveTo(tailBase, cy)
            lineTo(tailBase - len * 0.28f, cy - len * 0.22f - tw)
            lineTo(tailBase - len * 0.12f, cy)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.9f),
    )
    drawPath(
        Path().apply {
            moveTo(tailBase, cy)
            lineTo(tailBase - len * 0.28f, cy + len * 0.22f + tw)
            lineTo(tailBase - len * 0.12f, cy)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.9f),
    )
}

/** 2) Short oval body + broad fan tail. */
private fun DrawScope.drawOvalFanFish(cx: Float, cy: Float, len: Float, color: Color, wag: Float) {
    val bodyL = len * 0.58f
    val bodyH = len * 0.38f
    val nose = cx + bodyL * 0.45f
    val tailBase = cx - bodyL * 0.40f
    drawPath(
        Path().apply {
            moveTo(nose, cy)
            cubicTo(nose - bodyL * 0.1f, cy - bodyH, tailBase + bodyL * 0.15f, cy - bodyH * 0.95f, tailBase, cy)
            cubicTo(tailBase + bodyL * 0.15f, cy + bodyH * 0.95f, nose - bodyL * 0.1f, cy + bodyH, nose, cy)
            close()
        },
        color = color,
    )
    // Pectoral hint
    drawPath(
        Path().apply {
            moveTo(cx + bodyL * 0.05f, cy + bodyH * 0.15f)
            quadraticBezierTo(cx + bodyL * 0.02f, cy + bodyH * 1.1f, cx - bodyL * 0.12f, cy + bodyH * 0.55f)
            lineTo(cx + bodyL * 0.02f, cy + bodyH * 0.05f)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.7f),
    )
    // Wide fan tail
    val tw = wag * 0.02f * len
    drawPath(
        Path().apply {
            moveTo(tailBase, cy)
            quadraticBezierTo(tailBase - len * 0.18f, cy - len * 0.08f + tw, tailBase - len * 0.36f, cy - len * 0.32f + tw)
            lineTo(tailBase - len * 0.22f, cy)
            lineTo(tailBase - len * 0.36f, cy + len * 0.32f - tw)
            quadraticBezierTo(tailBase - len * 0.18f, cy + len * 0.08f - tw, tailBase, cy)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.88f),
    )
}

/** 3) Larger elongated body + prominent dorsal fin. */
private fun DrawScope.drawDorsalLongFish(cx: Float, cy: Float, len: Float, color: Color, wag: Float) {
    val bodyL = len * 0.78f
    val bodyH = len * 0.26f
    val nose = cx + bodyL * 0.50f
    val tailBase = cx - bodyL * 0.45f
    drawPath(
        Path().apply {
            moveTo(nose, cy)
            cubicTo(nose - bodyL * 0.2f, cy - bodyH * 0.9f, tailBase + bodyL * 0.12f, cy - bodyH, tailBase, cy)
            cubicTo(tailBase + bodyL * 0.12f, cy + bodyH, nose - bodyL * 0.2f, cy + bodyH * 0.9f, nose, cy)
            close()
        },
        color = color,
    )
    // Tall dorsal
    drawPath(
        Path().apply {
            moveTo(cx - bodyL * 0.12f, cy - bodyH * 0.75f)
            lineTo(cx + bodyL * 0.02f, cy - bodyH * 2.1f)
            lineTo(cx + bodyL * 0.22f, cy - bodyH * 0.65f)
            quadraticBezierTo(cx + bodyL * 0.05f, cy - bodyH * 1.1f, cx - bodyL * 0.12f, cy - bodyH * 0.75f)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.92f),
    )
    // Anal fin
    drawPath(
        Path().apply {
            moveTo(cx - bodyL * 0.05f, cy + bodyH * 0.7f)
            lineTo(cx + bodyL * 0.08f, cy + bodyH * 1.35f)
            lineTo(cx + bodyL * 0.15f, cy + bodyH * 0.55f)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.75f),
    )
    // Crescent tail
    val tw = wag * 0.018f * len
    drawPath(
        Path().apply {
            moveTo(tailBase, cy)
            quadraticBezierTo(tailBase - len * 0.15f, cy - len * 0.05f + tw, tailBase - len * 0.32f, cy - len * 0.28f + tw)
            lineTo(tailBase - len * 0.14f, cy)
            lineTo(tailBase - len * 0.32f, cy + len * 0.28f - tw)
            quadraticBezierTo(tailBase - len * 0.15f, cy + len * 0.05f - tw, tailBase, cy)
            close()
        },
        color = color.copy(alpha = color.alpha * 0.9f),
    )
}
