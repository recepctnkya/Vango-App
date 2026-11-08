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

/** Master clock length for lake ripple phases (ms). */
internal const val HeroLakeMasterMs = 10_000

/** Fixed short lake ripples — length as fraction of W; yFrac within lake path (0 far → 1 near). */
internal data class HeroLakeRipple(
    val yFrac: Float,
    val xCenterFrac: Float,
    val lengthFrac: Float,
    val strokeDp: Float,
    val phase: Float,
    val periodSec: Float,
    val alphaMid: Float,
    val alphaAmp: Float,
)

/**
 * Perspective wave ticks — far: short/thin/dense; near: longer/spaced.
 * Never span full width; lengths and x positions vary.
 */
internal val HeroLakeRipples: List<HeroLakeRipple> = listOf(
    // Incommensurate periods (not multiples of 10s) so phases rarely realign
    HeroLakeRipple(0.08f, 0.18f, 0.018f, 0.45f, 0.00f, 11.3f, 0.050f, 0.016f),
    HeroLakeRipple(0.11f, 0.41f, 0.022f, 0.45f, 0.28f, 7.1f, 0.052f, 0.018f),
    HeroLakeRipple(0.13f, 0.63f, 0.016f, 0.48f, 0.51f, 13.7f, 0.048f, 0.015f),
    HeroLakeRipple(0.16f, 0.82f, 0.020f, 0.48f, 0.73f, 9.4f, 0.055f, 0.018f),
    HeroLakeRipple(0.20f, 0.29f, 0.026f, 0.52f, 0.14f, 15.2f, 0.056f, 0.020f),
    HeroLakeRipple(0.24f, 0.55f, 0.024f, 0.52f, 0.62f, 8.6f, 0.058f, 0.020f),
    HeroLakeRipple(0.28f, 0.74f, 0.030f, 0.55f, 0.39f, 12.1f, 0.060f, 0.022f),
    HeroLakeRipple(0.36f, 0.22f, 0.038f, 0.62f, 0.08f, 10.7f, 0.065f, 0.024f),
    HeroLakeRipple(0.42f, 0.48f, 0.034f, 0.62f, 0.44f, 6.3f, 0.068f, 0.024f),
    HeroLakeRipple(0.48f, 0.69f, 0.042f, 0.68f, 0.77f, 14.5f, 0.070f, 0.026f),
    HeroLakeRipple(0.54f, 0.35f, 0.048f, 0.70f, 0.21f, 9.9f, 0.074f, 0.028f),
    HeroLakeRipple(0.60f, 0.58f, 0.040f, 0.72f, 0.58f, 11.8f, 0.076f, 0.028f),
    HeroLakeRipple(0.70f, 0.26f, 0.062f, 0.82f, 0.33f, 7.9f, 0.080f, 0.030f),
    HeroLakeRipple(0.78f, 0.52f, 0.070f, 0.88f, 0.05f, 13.2f, 0.085f, 0.030f),
    HeroLakeRipple(0.86f, 0.38f, 0.078f, 0.92f, 0.66f, 8.1f, 0.088f, 0.032f),
    HeroLakeRipple(0.92f, 0.64f, 0.068f, 0.95f, 0.42f, 16.4f, 0.086f, 0.030f),
)

/**
 * Fixed perspective light-path rows on the lake (t=0 far → t=1 near).
 * [segments]: 1 = continuous dash; 2–3 = broken pieces with water gaps.
 * [jitterX]: tiny static offset of the row centre (± fraction of W).
 */
internal data class HeroBodyPathRow(
    val t: Float,
    val segments: Int,
    val jitterX: Float,
    val phase: Float,
    val periodSec: Float,
    val alphaMid: Float,
)

/** 17 irregular rows — denser far, more spaced near; widths from t² at draw time. */
internal val HeroBodyPathRows: List<HeroBodyPathRow> = listOf(
    // Periods deliberately incommensurate with ripples / fish / 10s legacy waterT
    HeroBodyPathRow(0.04f, 1, 0.0012f, 0.08f, 11.7f, 0.10f),
    HeroBodyPathRow(0.09f, 2, -0.0018f, 0.31f, 7.3f, 0.11f),
    HeroBodyPathRow(0.14f, 1, 0.0006f, 0.55f, 13.1f, 0.12f),
    HeroBodyPathRow(0.19f, 1, -0.0024f, 0.17f, 8.9f, 0.11f),
    HeroBodyPathRow(0.25f, 3, 0.0015f, 0.72f, 15.4f, 0.13f),
    HeroBodyPathRow(0.31f, 1, -0.0009f, 0.42f, 6.7f, 0.12f),
    HeroBodyPathRow(0.37f, 2, 0.0021f, 0.88f, 12.6f, 0.14f),
    HeroBodyPathRow(0.44f, 1, -0.0014f, 0.23f, 9.8f, 0.13f),
    HeroBodyPathRow(0.51f, 2, 0.0008f, 0.61f, 14.2f, 0.15f),
    HeroBodyPathRow(0.58f, 1, -0.0020f, 0.05f, 7.9f, 0.14f),
    HeroBodyPathRow(0.65f, 3, 0.0017f, 0.49f, 16.1f, 0.16f),
    HeroBodyPathRow(0.72f, 1, -0.0005f, 0.78f, 10.4f, 0.15f),
    HeroBodyPathRow(0.78f, 2, 0.0026f, 0.36f, 8.3f, 0.17f),
    HeroBodyPathRow(0.84f, 1, -0.0011f, 0.94f, 13.8f, 0.16f),
    HeroBodyPathRow(0.89f, 2, 0.0003f, 0.14f, 11.2f, 0.18f),
    HeroBodyPathRow(0.94f, 1, -0.0019f, 0.67f, 6.4f, 0.17f),
    HeroBodyPathRow(0.98f, 2, 0.0010f, 0.41f, 9.1f, 0.15f),
)

/** Soft far-water reflection blobs under opposite shore (not sharp mountain copies). */
internal data class HeroFarReflection(
    val xFrac: Float,
    val yFrac: Float,
    val wFrac: Float,
    val hFrac: Float,
)

internal val HeroFarReflections: List<HeroFarReflection> = listOf(
    HeroFarReflection(0.08f, 0.08f, 0.12f, 0.10f),
    HeroFarReflection(0.22f, 0.12f, 0.10f, 0.08f),
    HeroFarReflection(0.78f, 0.10f, 0.11f, 0.09f),
    HeroFarReflection(0.90f, 0.14f, 0.09f, 0.07f),
)

/**
 * Near-shore water edge knots (xFrac, yFrac). Soft irregular scoops — not a sine ribbon.
 * y grows toward park; envelope stays within prior water footprint (~0.74–0.80H).
 */
internal val HeroNearShoreKnots: List<Pair<Float, Float>> = listOf(
    0.00f to 0.770f,
    0.06f to 0.784f,
    0.12f to 0.794f,
    0.18f to 0.766f,
    0.25f to 0.778f,
    0.32f to 0.754f,
    0.40f to 0.748f,
    0.48f to 0.738f,
    0.55f to 0.746f,
    0.62f to 0.760f,
    0.70f to 0.776f,
    0.78f to 0.792f,
    0.86f to 0.798f,
    0.93f to 0.788f,
    1.00f to 0.778f,
)

/** Sample near-shore waterline Y (pixels). */
internal fun heroNearShoreY(h: Float, xFrac: Float): Float {
    val x = xFrac.coerceIn(0f, 1f)
    val knots = HeroNearShoreKnots
    if (x <= knots.first().first) return h * knots.first().second
    if (x >= knots.last().first) return h * knots.last().second
    for (i in 0 until knots.lastIndex) {
        val (x0, y0) = knots[i]
        val (x1, y1) = knots[i + 1]
        if (x <= x1) {
            val u = ((x - x0) / (x1 - x0).coerceAtLeast(1e-4f)).coerceIn(0f, 1f)
            val s = u * u * (3f - 2f * u)
            return h * (y0 + (y1 - y0) * s)
        }
    }
    return h * knots.last().second
}

/**
 * Wet-sand band thickness as fraction of H — varies and soft-gaps (0 = no wet strip).
 * Stays thin; never reaches vehicle dry pad on the right.
 */
internal fun heroWetWidthFrac(xFrac: Float): Float {
    val x = xFrac.coerceIn(0f, 1f)
    // Soft gaps
    if (x in 0.10f..0.13f || x in 0.47f..0.51f || x in 0.83f..0.87f) return 0f
    val lobe = when {
        x < 0.22f -> 0.014f + 0.008f * sin(x * 18f)
        x < 0.45f -> 0.010f + 0.012f * abs(sin(x * 11f + 0.4f))
        x < 0.68f -> 0.016f + 0.006f * sin(x * 14f + 1.1f)
        else -> 0.011f + 0.005f * abs(sin(x * 9f + 0.2f)) // thinner under vehicle side
    }
    return lobe.coerceIn(0f, 0.024f)
}

/** Dry park starts just landward of the wet strip. */
internal fun heroDryEdgeY(h: Float, xFrac: Float): Float =
    heroNearShoreY(h, xFrac) + h * heroWetWidthFrac(xFrac).coerceAtLeast(0.006f)

internal fun appendNearShoreEdge(
    path: Path,
    w: Float,
    h: Float,
    leftToRight: Boolean,
    yAt: (Float) -> Float,
    skipFirst: Boolean = false,
) {
    val knots = if (leftToRight) HeroNearShoreKnots else HeroNearShoreKnots.asReversed()
    val start = if (skipFirst) 1 else 0
    for (i in start until knots.size) {
        val (xf, _) = knots[i]
        val x = w * xf
        val y = yAt(xf)
        if (i == start && !skipFirst) {
            path.moveTo(x, y)
        } else {
            val (pxf, _) = knots[i - 1]
            val midX = w * (pxf + xf) * 0.5f
            val midY = (yAt(pxf) + y) * 0.5f
            val bias = h * (0.004f * if ((i + if (leftToRight) 0 else 1) % 3 == 0) 1f else -0.6f)
            path.cubicTo(
                lerpFloat(w * pxf, midX, 0.55f), lerpFloat(yAt(pxf), midY, 0.35f) + bias,
                lerpFloat(midX, x, 0.45f), lerpFloat(midY, y, 0.55f) - bias * 0.5f,
                x, y,
            )
        }
    }
}

/**
 * Closed lake path. Far edge ~0.60–0.65H.
 * Front shore = irregular soft curves (shared with wet/dry edge helpers).
 */
internal fun heroLakePath(w: Float, h: Float): Path = Path().apply {
    moveTo(0f, h * 0.64f)
    cubicTo(w * 0.18f, h * 0.60f, w * 0.35f, h * 0.62f, w * 0.50f, h * 0.61f)
    cubicTo(w * 0.70f, h * 0.60f, w * 0.87f, h * 0.62f, w, h * 0.65f)
    lineTo(w, heroNearShoreY(h, 1f))
    appendNearShoreEdge(this, w, h, leftToRight = false, yAt = { xf -> heroNearShoreY(h, xf) }, skipFirst = true)
    close()
}

/** Dry park — starts landward of wet sand, then down to card bottom. */
internal fun heroParkGroundPath(w: Float, h: Float): Path = Path().apply {
    moveTo(w, heroDryEdgeY(h, 1f))
    appendNearShoreEdge(this, w, h, leftToRight = false, yAt = { xf -> heroDryEdgeY(h, xf) }, skipFirst = true)
    lineTo(0f, h)
    lineTo(w, h)
    close()
}

/** Narrow dark land tongues at far water edge (open center). */
internal fun DrawScope.drawHeroFarShore(w: Float, h: Float, palette: HeroScenePalette) {
    val land = Color(
        red = palette.mountainNear.red * 0.55f + palette.tree.red * 0.45f,
        green = palette.mountainNear.green * 0.50f + palette.tree.green * 0.50f,
        blue = palette.mountainNear.blue * 0.60f + palette.tree.blue * 0.40f,
        alpha = 1f,
    )
    // Left spit
    drawPath(
        Path().apply {
            moveTo(0f, h * 0.60f)
            lineTo(w * 0.02f, h * 0.585f)
            cubicTo(w * 0.08f, h * 0.575f, w * 0.14f, h * 0.595f, w * 0.20f, h * 0.605f)
            cubicTo(w * 0.16f, h * 0.625f, w * 0.08f, h * 0.635f, 0f, h * 0.640f)
            close()
        },
        color = land,
    )
    // Right spit
    drawPath(
        Path().apply {
            moveTo(w, h * 0.605f)
            lineTo(w * 0.98f, h * 0.590f)
            cubicTo(w * 0.92f, h * 0.575f, w * 0.86f, h * 0.595f, w * 0.78f, h * 0.608f)
            cubicTo(w * 0.84f, h * 0.628f, w * 0.92f, h * 0.640f, w, h * 0.648f)
            close()
        },
        color = land,
    )
}

/** Depth-graded lake + clipped ripples / body glints / soft far reflections. */
internal fun DrawScope.drawHeroLake(
    w: Float,
    h: Float,
    palette: HeroScenePalette,
    sky: HeroSkySnapshot,
    waterT: Float,
    sceneTSec: Float,
    animateWater: Boolean,
    lakeClipPath: Path,
    bodyXFrac: Float,
    reflectionStrength: Float,
    light: HeroSceneLight,
    drawWaterLife: Boolean = true,
    iceCoverage: Float = 0f,
) {
    val lake = heroLakePath(w, h)
    lakeClipPath.rewind()
    lakeClipPath.addPath(lake)

    // Shared light — no independent day/night water palette
    val dayFar = Color(0xFF6A8DA8)
    val dayDeep = Color(0xFF3E6280)
    val nightFar = Color(0xFF243F58)
    val nightDeep = Color(0xFF152C40)
    val farC = light.litColor(lerpColor(dayFar, nightFar, light.nightAmount), 0.55f)
    val deepC = light.shadeColor(lerpColor(dayDeep, nightDeep, light.nightAmount), 0.55f)
    val shallowC = light.litColor(
        lerpColor(Color(0xFF7A9AA0), Color(0xFF3A5560), light.nightAmount),
        0.45f,
    )
    // Soft deep fill — cold tint from continuous winter amount (never full white)
    val winterCold = winterAmountFromProgress(sky.seasonProgress)
    val farUse = lerpColor(farC, Color(0xFF4A6280), winterCold * 0.28f)
    val deepUse = lerpColor(deepC, Color(0xFF2A4058), winterCold * 0.22f)
    drawPath(
        lake,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to farUse,
                0.55f to lerpColor(farUse, deepUse, 0.55f),
                1.00f to deepUse,
            ),
            startY = h * 0.60f,
            endY = h * 0.80f,
        ),
    )

    // Prefer continuous scene seconds (cloudT); avoid Restart snaps from a short 0–1 loop
    val tSec = if (animateWater) sceneTSec else 0f
    val twoPi = (PI * 2.0).toFloat()
    val lakeTop = h * 0.60f
    val lakeBot = h * 0.81f
    val lakeH = lakeBot - lakeTop
    val rippleMul = (0.32f + reflectionStrength * 0.48f).coerceIn(0.22f, 0.85f)

    // Shallow wash along near shore — soft blobs, not color bands
    clipPath(lakeClipPath) {
        val samples = 14
        for (i in 0 until samples) {
            val xf = (i + 0.5f) / samples
            val shoreY = heroNearShoreY(h, xf)
            val cx = w * xf + w * 0.01f * sin(i * 1.7f)
            val reach = h * (0.028f + 0.018f * abs(sin(i * 2.3f + 0.5f)))
            val bw = w * (0.07f + 0.04f * abs(cos(i * 1.1f)))
            val bh = reach * (1.1f + 0.35f * sin(i * 0.9f))
            drawOval(
                color = shallowC.copy(alpha = 0.22f + (i % 3) * 0.04f),
                topLeft = Offset(cx - bw * 0.5f, shoreY - bh * 0.92f),
                size = Size(bw, bh),
            )
            if (i % 2 == 0) {
                drawOval(
                    color = shallowC.copy(alpha = 0.12f),
                    topLeft = Offset(cx - bw * 0.25f, shoreY - bh * 1.35f),
                    size = Size(bw * 0.55f, bh * 0.55f),
                )
            }
        }
    }

    // Layer: water base → fish + underwater dolphin → ripples / reflections → breach
    if (drawWaterLife) {
        clipPath(lakeClipPath) {
            drawHeroLakeFish(
                w = w,
                h = h,
                lakeTop = lakeTop,
                lakeBot = lakeBot,
                waterT = if (tSec > 0f) ((tSec / 10f) % 1f) else waterT,
                animate = animateWater,
                period = sky.period,
                weather = sky.weather,
                reflectionStrength = reflectionStrength,
                timeSec = tSec,
            )
            drawHeroLakeDolphinUnderwater(
                w = w,
                h = h,
                lakeTop = lakeTop,
                lakeBot = lakeBot,
                sceneTSec = sceneTSec,
                animate = animateWater,
                period = sky.period,
            )
        }
    }

    clipPath(lakeClipPath) {
        // Soft fragmented mountain/tree reflections — far water only
        val reflBase = palette.mountainMid
        HeroFarReflections.forEachIndexed { i, blob ->
            val a = (0.045f + (i % 3) * 0.012f).coerceIn(0.04f, 0.08f)
            val y0 = lakeTop + lakeH * blob.yFrac
            val bw = w * blob.wFrac
            val bh = lakeH * blob.hFrac
            val x0 = w * blob.xFrac - bw * 0.5f
            drawOval(
                color = reflBase.copy(alpha = a),
                topLeft = Offset(x0, y0),
                size = Size(bw * 0.45f, bh * 0.55f),
            )
            drawOval(
                color = reflBase.copy(alpha = a * 0.85f),
                topLeft = Offset(x0 + bw * 0.52f, y0 + bh * 0.15f),
                size = Size(bw * 0.42f, bh * 0.50f),
            )
        }

        HeroLakeRipples.forEach { r ->
            val angle = if (animateWater) {
                (tSec / r.periodSec + r.phase) * twoPi
            } else {
                r.phase * twoPi
            }
            // Low continuous intensity — phases stay desynced from trees
            val dx = if (animateWater) w * 0.0024f * sin(angle) else 0f
            val dy = if (animateWater) h * 0.00085f * sin(angle * 0.71f + r.phase * 2f) else 0f
            val alpha = (
                if (animateWater) {
                    (r.alphaMid * 0.85f + r.alphaAmp * 0.70f * sin(angle * 0.55f + 1.2f))
                } else {
                    r.alphaMid * 0.85f
                }
                ).coerceIn(0.03f, 0.095f) * rippleMul
            // Anchor near-shore ripples to local shoreline so they don't cut the wet strip
            val baseY = if (r.yFrac > 0.65f) {
                val shoreY = heroNearShoreY(h, r.xCenterFrac)
                lerpFloat(lakeTop, shoreY, 0.72f + (r.yFrac - 0.65f) * 0.7f) + dy
            } else {
                lakeTop + lakeH * r.yFrac + dy
            }
            val half = w * r.lengthFrac * 0.5f
            val cx = w * r.xCenterFrac + dx
            // Slight tilt — avoid perfect parallel full-width stripes
            val tilt = h * 0.004f * sin(r.phase * 6f + r.xCenterFrac * 4f)
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(cx - half, baseY - tilt),
                end = Offset(cx + half, baseY + tilt),
                strokeWidth = r.strokeDp.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        drawHeroBodyLightPath(
            w = w,
            yFar = lakeTop,
            yNear = lakeBot,
            sky = sky,
            bodyXFrac = bodyXFrac,
            reflectionStrength = reflectionStrength,
            waterT = waterT,
            animateWater = animateWater,
            masterSec = 10f,
            twoPi = twoPi,
            light = light,
            timeSec = tSec,
        )
    }

    // Breach / splash above the waterline (not lake-edge clipped); still under park/vehicle/UI
    if (drawWaterLife) {
        drawHeroLakeDolphinSurface(
            w = w,
            h = h,
            lakeTop = lakeTop,
            lakeBot = lakeBot,
            sceneTSec = sceneTSec,
            animate = animateWater,
            period = sky.period,
        )
        drawHeroLakeFisherman(
            w = w,
            h = h,
            lakeTop = lakeTop,
            lakeBot = lakeBot,
            sceneTSec = sceneTSec,
            animate = animateWater,
            period = sky.period,
            weather = sky.weather,
            iceCoverage = iceCoverage,
        )
    }

    // Narrow irregular ice/snow shore rim — never a thick white frame or full freeze
    if (iceCoverage > 0.04f) {
        drawHeroShoreIceRim(w, h, iceCoverage, light)
    }
}

/** Thin broken ice/snow edge between open water and land. */
internal fun DrawScope.drawHeroShoreIceRim(
    w: Float,
    h: Float,
    iceCoverage: Float,
    light: HeroSceneLight,
) {
    val a = (0.35f + iceCoverage * 1.8f).coerceIn(0.30f, 0.72f)
    val ice = light.litColor(HeroSnowMain, 0.35f)
    val steps = 22
    for (i in 0 until steps) {
        if ((i * 3 + 1) % 5 == 0) continue // soft gaps
        val xf0 = i / steps.toFloat()
        val xf1 = ((i + 1.1f) / steps).coerceAtMost(1f)
        val shoreY = heroNearShoreY(h, (xf0 + xf1) * 0.5f)
        val thick = h * (0.006f + iceCoverage * 0.010f) * (0.75f + abs(sin(i * 1.3f)) * 0.35f)
        val left = w * xf0
        val right = w * xf1
        drawOval(
            ice.copy(alpha = a * (0.55f + (i % 3) * 0.12f)),
            topLeft = Offset(left, shoreY - thick * 0.35f),
            size = Size((right - left) * 1.05f, thick),
        )
    }
}

/** Broken shore-foam segments — desynced 5–8s wash cycles; never a continuous white contour. */
internal data class HeroShoreFoamSeg(
    val x0: Float,
    val x1: Float,
    val periodSec: Float,
    val phase: Float,
    val maxAdvanceFrac: Float,
)

internal val HeroShoreFoamSegs: List<HeroShoreFoamSeg> = listOf(
    HeroShoreFoamSeg(0.02f, 0.11f, 6.2f, 0.00f, 0.010f),
    HeroShoreFoamSeg(0.16f, 0.27f, 7.4f, 0.33f, 0.012f),
    HeroShoreFoamSeg(0.34f, 0.44f, 5.6f, 0.61f, 0.009f),
    HeroShoreFoamSeg(0.54f, 0.64f, 8.0f, 0.18f, 0.011f),
    HeroShoreFoamSeg(0.72f, 0.80f, 6.8f, 0.72f, 0.007f), // thin near vehicle — stay off dry pad
)

/**
 * Wet grey-beige sand + animated shore foam.
 * Drawn after water/fish/ripples, before dry park. Shoreline geometry stays fixed.
 */
internal fun DrawScope.drawHeroWetShoreAndFoam(
    w: Float,
    h: Float,
    light: HeroSceneLight,
    sceneTSec: Float,
    animate: Boolean,
) {
    val wetDark = light.shadeColor(
        lerpColor(Color(0xFF6A655C), Color(0xFF4A5248), light.nightAmount),
        0.5f,
    )
    val wetMid = light.litColor(
        lerpColor(Color(0xFF7A7468), Color(0xFF555A52), light.nightAmount),
        0.4f,
    )
    val foamCol = Color.White

    // Wet sand patches — variable width, soft edges, gaps (no continuous dark band)
    val steps = 28
    for (i in 0 until steps) {
        val xf0 = i / steps.toFloat()
        val xf1 = (i + 1.15f) / steps.toFloat()
        val xMid = (xf0 + xf1) * 0.5f
        val wetH = heroWetWidthFrac(xMid)
        if (wetH < 0.003f) continue
        val shoreY = heroNearShoreY(h, xMid)
        val dryY = shoreY + h * wetH
        val left = w * xf0
        val right = w * xf1.coerceAtMost(1f)
        val path = Path().apply {
            val yL0 = heroNearShoreY(h, xf0)
            val yR0 = heroNearShoreY(h, xf1.coerceAtMost(1f))
            val yL1 = heroDryEdgeY(h, xf0)
            val yR1 = heroDryEdgeY(h, xf1.coerceAtMost(1f))
            moveTo(left, yL0)
            lineTo(right, yR0)
            lineTo(right, yR1)
            quadraticBezierTo(
                w * xMid, dryY + h * 0.004f,
                left, yL1,
            )
            close()
        }
        val a = (0.38f + 0.12f * abs(sin(i * 1.3f))).coerceIn(0.32f, 0.55f)
        drawPath(path, color = wetDark.copy(alpha = a))
        // Soft fade toward dry sand
        drawOval(
            color = wetMid.copy(alpha = a * 0.35f),
            topLeft = Offset(left, dryY - h * wetH * 0.35f),
            size = Size((right - left) * 1.05f, h * wetH * 0.7f),
        )
    }

    // Shore foam — only thin broken traces move; shoreline itself stays put
    HeroShoreFoamSegs.forEach { seg ->
        val foamPeriod = seg.periodSec.coerceIn(5f, 8f)
        val cycle = if (animate) {
            val raw = sceneTSec / foamPeriod + seg.phase
            raw - floor(raw.toDouble()).toFloat()
        } else {
            0.35f
        }
        val advanceT = when {
            cycle < 0.42f -> {
                val u = cycle / 0.42f
                u * u * (3f - 2f * u)
            }
            cycle < 0.55f -> 1f
            else -> {
                val u = ((cycle - 0.55f) / 0.45f).coerceIn(0f, 1f)
                1f - u * u * (3f - 2f * u)
            }
        }
        val fade = when {
            cycle < 0.42f -> 0.35f + 0.65f * (cycle / 0.42f)
            cycle < 0.55f -> 1f
            else -> (1f - (cycle - 0.55f) / 0.45f).coerceIn(0f, 1f)
        }
        val maxAdv = h * seg.maxAdvanceFrac
        val adv = maxAdv * advanceT
        val midX = w * (seg.x0 + seg.x1) * 0.5f
        val shoreY = heroNearShoreY(h, (seg.x0 + seg.x1) * 0.5f)
        // Foam sits on water edge and creeps slightly onto wet sand only
        val y = shoreY + adv - h * 0.004f
        val span = w * (seg.x1 - seg.x0)
        val alpha = (0.16f + 0.22f * fade) * fade
        // Broken pieces — not one continuous contour
        val pieces = 2 + ((seg.phase * 10).toInt() % 2)
        for (p in 0 until pieces) {
            val u0 = p / pieces.toFloat()
            val u1 = (p + 0.72f) / pieces.toFloat()
            val gap = span * 0.06f
            val x0 = midX - span * 0.5f + span * u0 + gap
            val x1 = midX - span * 0.5f + span * u1 - gap
            if (x1 <= x0) continue
            val yJitter = h * 0.002f * sin((seg.phase + p) * 8f)
            drawLine(
                color = foamCol.copy(alpha = alpha),
                start = Offset(x0, y + yJitter),
                end = Offset(x1, y - yJitter * 0.5f),
                strokeWidth = (0.7f + fade * 0.6f).dp.toPx(),
                cap = StrokeCap.Round,
            )
            if (fade > 0.55f && p == 0) {
                drawOval(
                    color = foamCol.copy(alpha = alpha * 0.45f),
                    topLeft = Offset(lerpFloat(x0, x1, 0.3f), y - h * 0.003f),
                    size = Size((x1 - x0) * 0.25f, h * 0.006f),
                )
            }
        }
    }
}

/**
 * Perspective sun/moon light path on water: far = narrow & dense, near = wide & spaced.
 * width(t) = W × (0.008 + 0.065 × t²). Clipped by caller to lake path.
 */
internal fun DrawScope.drawHeroBodyLightPath(
    w: Float,
    yFar: Float,
    yNear: Float,
    sky: HeroSkySnapshot,
    bodyXFrac: Float,
    reflectionStrength: Float,
    @Suppress("UNUSED_PARAMETER") waterT: Float,
    animateWater: Boolean,
    @Suppress("UNUSED_PARAMETER") masterSec: Float,
    twoPi: Float,
    light: HeroSceneLight,
    timeSec: Float = 0f,
) {
    val sunUp = sky.sun?.aboveHorizon == true
    val moonUp = sky.moon?.pos?.aboveHorizon == true
    if (!sunUp && !moonUp) return
    // Shared soft threshold — overcast / heavy cloud skip hard glints
    if (reflectionStrength <= 0.10f || light.direct < 0.12f) return

    val lakeH = (yNear - yFar).coerceAtLeast(1f)
    val coreCol = when {
        !sunUp -> Color(0xFFE8F0F6)
        light.warmth > 0.25f -> lerpColor(Color(0xFFFFF6E6), Color(0xFFFFE8C0), light.warmth)
        else -> Color(0xFFFFF6E6)
    }
    val edgeCol = when {
        !sunUp -> Color(0xFFC5D6E4)
        light.warmth > 0.25f -> lerpColor(Color(0xFFE8DCC8), Color(0xFFFFC878), light.warmth * 0.7f)
        else -> Color(0xFFE8DCC8)
    }
    val brightnessScale = if (sunUp) 1f else 0.38f
    val softCut = (1f - light.soft * 0.85f).coerceIn(0.12f, 1f)
    val strength = reflectionStrength * brightnessScale * softCut * light.direct.coerceIn(0.15f, 1f) * 0.85f

    HeroBodyPathRows.forEach { row ->
        val t = row.t.coerceIn(0f, 1f)
        val t2 = t * t
        val width = w * (0.008f + 0.065f * t2)
        val stroke = (0.45f + t * 0.70f).dp.toPx()
        val y = yFar + lakeH * t

        val angle = if (animateWater) {
            (timeSec / row.periodSec + row.phase) * twoPi
        } else {
            row.phase * twoPi
        }
        // Near rows drift more; never move the whole path as one block
        val driftAmp = w * (0.0010f + 0.0038f * t)
        val dx = if (animateWater) driftAmp * sin(angle) else 0f
        val cx = w * bodyXFrac + w * row.jitterX + dx

        val pulse = if (animateWater) {
            0.90f + 0.10f * sin(angle * 0.45f + row.phase * 1.7f)
        } else {
            1f
        }
        val peakA = (row.alphaMid * strength * pulse).coerceIn(0.03f, 0.22f)

        fun drawSeg(x0: Float, x1: Float, alphaMul: Float) {
            if (x1 - x0 < 0.5f) return
            val a = peakA * alphaMul
            drawLine(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to edgeCol.copy(alpha = 0f),
                        0.22f to edgeCol.copy(alpha = a * 0.55f),
                        0.50f to coreCol.copy(alpha = a),
                        0.78f to edgeCol.copy(alpha = a * 0.55f),
                        1.00f to edgeCol.copy(alpha = 0f),
                    ),
                    startX = x0,
                    endX = x1,
                ),
                start = Offset(x0, y),
                end = Offset(x1, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        val half = width * 0.5f
        when (row.segments) {
            1 -> drawSeg(cx - half, cx + half, 1f)
            2 -> {
                val gap = width * 0.22f
                val piece = (width - gap) * 0.5f
                drawSeg(cx - half, cx - half + piece, 0.92f)
                drawSeg(cx + half - piece, cx + half, 0.85f)
            }
            else -> {
                val gap = width * 0.14f
                val piece = (width - 2f * gap) / 3f
                var x = cx - half
                drawSeg(x, x + piece, 0.80f)
                x += piece + gap
                drawSeg(x, x + piece, 1f)
                x += piece + gap
                drawSeg(x, x + piece, 0.75f)
            }
        }
    }
}
