package com.hextech.vango

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Fixed autumn leaf seeds — spawn anchored to deciduous crown fracs. */
private data class HeroLeafSeed(
    val crownX: Float,
    val crownY: Float,
    val phase: Float,
    val periodSec: Float,
    val size: Float,
    val hue: Int,
)

private val HeroLeafSeeds = listOf(
    // Long, incommensurate periods — never a simultaneous shower
    HeroLeafSeed(0.78f, 0.55f, 0.00f, 22.7f, 0.0065f, 0),
    HeroLeafSeed(0.78f, 0.55f, 0.41f, 31.4f, 0.0055f, 1),
    HeroLeafSeed(0.78f, 0.55f, 0.73f, 18.9f, 0.0070f, 2),
    HeroLeafSeed(0.18f, 0.58f, 0.19f, 27.1f, 0.0060f, 0),
    HeroLeafSeed(0.18f, 0.58f, 0.55f, 35.6f, 0.0065f, 1),
    HeroLeafSeed(0.18f, 0.58f, 0.88f, 24.3f, 0.0050f, 2),
    HeroLeafSeed(0.91f, 0.52f, 0.33f, 29.8f, 0.0055f, 0),
    HeroLeafSeed(0.91f, 0.52f, 0.67f, 21.5f, 0.0060f, 1),
)

private val HeroRainRingSeeds = listOf(
    0.18f to 0.68f, 0.32f to 0.70f, 0.48f to 0.66f, 0.62f to 0.71f,
    0.22f to 0.74f, 0.55f to 0.73f, 0.40f to 0.69f, 0.70f to 0.67f,
    0.28f to 0.71f, 0.58f to 0.68f, 0.44f to 0.75f, 0.36f to 0.67f,
)

/**
 * Autumn falling leaves: sparse, from deciduous crowns only, wind-drift + spin.
 * Off when motion paused (static QA) or wind below breeze — season stays readable
 * from fixed canopy + ground litter alone. Never from pines.
 */
internal fun DrawScope.drawHeroAutumnLeaves(
    w: Float,
    h: Float,
    timeSec: Float,
    wind: WindState,
    foliageAmount: Float,
    season: SeasonState,
    animate: Boolean,
    nightAmount: Float,
) {
    if (season != SeasonState.Autumn || foliageAmount > 0.88f) return
    // First-check / calm: no particles
    if (!animate || wind.speedNorm < 0.28f) return
    val fallMul = ((0.88f - foliageAmount) / 0.66f).coerceIn(0.12f, 1f) *
        ((wind.speedNorm - 0.22f) / 0.55f).coerceIn(0.25f, 1f)
    val colors = listOf(HeroAutumnLeafGold, HeroAutumnLeafCopper, HeroAutumnLeafShade)
    val dim = (1f - nightAmount * 0.45f)
    val driftSign = windDriftSign(wind)
    HeroLeafSeeds.forEachIndexed { i, seed ->
        val period = seed.periodSec
        val raw = timeSec / period + seed.phase
        val cycle = raw - floor(raw.toDouble()).toFloat()
        // Drop window only; stagger so crowns never dump together
        if (cycle < 0.08f || cycle > 0.62f) return@forEachIndexed
        val periodIndex = floor((timeSec / period + seed.phase).toDouble()).toInt()
        if (((periodIndex + i * 3) % 3) != 0) return@forEachIndexed
        val t = ((cycle - 0.08f) / 0.54f).coerceIn(0f, 1f)
        val start = Offset(w * seed.crownX, h * seed.crownY)
        val drift = driftSign * w * (0.035f + wind.speedNorm * 0.07f) * t
        val fall = h * (0.22f + (i % 3) * 0.04f) * t * t
        val spin = t * PI.toFloat() * (1.2f + (i % 3) * 0.4f)
        val x = start.x + drift + sin(spin) * w * 0.008f
        val y = start.y + fall
        val xf = (x / w).coerceIn(0f, 1f)
        val yf = (y / h).coerceIn(0f, 1f)
        if (HeroSceneUiZones.hitsAnyChrome(xf, yf)) return@forEachIndexed
        val groundY = h * 0.88f
        val onGround = y >= groundY
        val life = if (onGround) {
            ((1f - (t - 0.72f) / 0.28f).coerceIn(0f, 1f))
        } else {
            1f
        }
        if (life < 0.05f) return@forEachIndexed
        val inWater = y in (h * 0.60f)..(h * 0.80f) && x < w * 0.75f
        val a = (0.52f * fallMul * life * dim * if (inWater) 0.40f else 1f)
            .coerceIn(0f, 0.68f)
        val s = w * seed.size
        val col = colors[seed.hue % colors.size].copy(alpha = a)
        val cx = if (inWater) x + driftSign * w * 0.01f * t else x
        val cy = y.coerceAtMost(groundY + h * 0.01f)
        // Tiny leaf silhouette (not oval blob); spin via rotation angle
        val rot = spin * (180f / PI.toFloat()) * driftSign
        drawFallingAutumnLeaf(Offset(cx, cy), s * 2.4f, rot, col)
    }
}

/** Compact falling-leaf silhouette (path), reused by particle pass. */
private fun DrawScope.drawFallingAutumnLeaf(
    center: Offset,
    length: Float,
    rotDeg: Float,
    color: Color,
) {
    val rad = rotDeg * (PI.toFloat() / 180f)
    val c = cos(rad)
    val s = sin(rad)
    fun p(lx: Float, ly: Float) = Offset(center.x + lx * c - ly * s, center.y + lx * s + ly * c)
    val tip = p(length * 0.5f, 0f)
    val base = p(-length * 0.4f, 0f)
    val left = p(-length * 0.05f, -length * 0.28f)
    val right = p(0f, length * 0.26f)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        quadraticBezierTo(right.x, right.y, base.x, base.y)
        quadraticBezierTo(left.x, left.y, tip.x, tip.y)
        close()
    }
    drawPath(path, color = color)
}

/** Expanding ripples on lake during rain — clipped by caller or lake path. */
internal fun DrawScope.drawHeroRainLakeRings(
    w: Float,
    h: Float,
    timeSec: Float,
    rainAlpha: Float,
    lakeClipPath: androidx.compose.ui.graphics.Path,
    nightAmount: Float,
) {
    if (rainAlpha < 0.05f) return
    val dim = (1f - nightAmount * 0.40f)
    clipPath(lakeClipPath) {
        HeroRainRingSeeds.forEachIndexed { i, (xf, yf) ->
            val period = 2.4f + (i % 4) * 0.35f
            val t = ((timeSec / period) + i * 0.17f) % 1f
            val r = w * (0.005f + t * 0.036f)
            val a = (1f - t) * 0.38f * rainAlpha * dim
            if (a < 0.02f) return@forEachIndexed
            drawCircle(
                color = Color.White.copy(alpha = a.coerceIn(0f, 0.55f)),
                radius = r,
                center = Offset(w * xf, h * yf),
                style = Stroke(width = (1.3f + (1f - t) * 1.1f).coerceAtMost(2.8f), cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Continuous snow pack over park — soft mounds, not a white rectangle or dotted fill.
 * [snowCover] drives opacity/thickness; season alone does not invent this surface.
 */
internal fun DrawScope.drawHeroSnowPack(
    w: Float,
    h: Float,
    snowCover: Float,
    light: HeroSceneLight,
) {
    if (snowCover < 0.08f) return
    val cover = snowCover.coerceIn(0f, 1f)
    val night = light.nightAmount
    // Night: blue-tinted pack (not pure white)
    fun packCol(day: Color): Color {
        val nightC = lerpColor(day, Color(0xFF6A7A90), 0.45f)
        return light.litColor(lerpColor(day, nightC, night), 0.40f)
    }
    val lit = packCol(HeroSnowLit)
    val main = packCol(HeroSnowMain)
    val hollow = packCol(HeroSnowHollow)
    val contact = packCol(HeroSnowContact)
    val baseA = (0.72f + cover * 0.26f).coerceIn(0.70f, 0.98f)

    // Soft continuous base clipped to park ground path
    clipPath(heroParkGroundPath(w, h)) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to hollow.copy(alpha = baseA * 0.55f),
                    0.35f to main.copy(alpha = baseA * 0.88f),
                    0.72f to lit.copy(alpha = baseA),
                    1.00f to main.copy(alpha = baseA * 0.92f),
                ),
                startY = h * 0.74f,
                endY = h,
            ),
        )
        // Irregular mounds — larger/higher contrast in foreground
        HeroSnowMounds.forEach { m ->
            val cx = w * m.x
            val cy = h * m.y
            val rw = w * m.rw * (0.85f + cover * 0.35f)
            val rh = h * m.rh * (0.80f + cover * 0.40f)
            val depthA = (0.40f + m.depth * 0.55f).coerceIn(0.35f, 0.95f)
            val path = irregularSnowMound(cx, cy, rw, rh, m.seed)
            drawPath(path, color = hollow.copy(alpha = 0.35f * depthA * cover))
            drawPath(
                irregularSnowMound(cx, cy - rh * 0.18f, rw * 0.82f, rh * 0.70f, m.seed + 3),
                color = main.copy(alpha = 0.55f * depthA),
            )
            drawPath(
                irregularSnowMound(cx - rw * 0.06f, cy - rh * 0.32f, rw * 0.55f, rh * 0.42f, m.seed + 7),
                color = lit.copy(alpha = 0.50f * depthA * (0.55f + 0.45f * light.direct)),
            )
        }
        // Soft contact troughs (not hard stripes)
        HeroSnowHollows.forEach { (xf, yf, seed) ->
            val cx = w * xf
            val cy = h * yf
            drawPath(
                irregularSnowMound(cx, cy, w * 0.06f, h * 0.014f, seed),
                color = contact.copy(alpha = 0.22f + cover * 0.18f),
            )
        }
    }
}

/** Soft snow dusting fallback for light cover — sparse patches only. */
internal fun DrawScope.drawHeroSnowDusting(
    w: Float,
    h: Float,
    snowCover: Float,
    light: HeroSceneLight,
) {
    if (snowCover < 0.05f) return
    if (snowCover >= 0.18f) {
        drawHeroSnowPack(w, h, snowCover, light)
        return
    }
    val a = (0.22f + snowCover * 0.45f) * (0.75f + 0.25f * light.ambient)
    val patches = listOf(
        0.06f to 0.90f, 0.12f to 0.94f, 0.88f to 0.91f, 0.94f to 0.95f,
        0.22f to 0.86f, 0.78f to 0.87f, 0.48f to 0.92f,
    )
    patches.forEachIndexed { i, (xf, yf) ->
        val rw = w * (0.04f + (i % 3) * 0.015f) * (0.7f + snowCover)
        val rh = h * 0.012f * (0.8f + snowCover * 0.4f)
        drawOval(
            HeroSnowMain.copy(alpha = a * (0.55f + (i % 3) * 0.12f)),
            topLeft = Offset(w * xf - rw * 0.5f, h * yf - rh * 0.5f),
            size = Size(rw, rh),
        )
    }
}

private data class HeroSnowMound(
    val x: Float,
    val y: Float,
    val rw: Float,
    val rh: Float,
    val depth: Float,
    val seed: Int,
)

private val HeroSnowMounds = listOf(
    // Foreground — larger
    HeroSnowMound(0.08f, 0.94f, 0.11f, 0.038f, 0.95f, 1),
    HeroSnowMound(0.22f, 0.96f, 0.09f, 0.032f, 0.90f, 2),
    HeroSnowMound(0.88f, 0.945f, 0.10f, 0.036f, 0.92f, 3),
    HeroSnowMound(0.96f, 0.97f, 0.07f, 0.028f, 0.96f, 4),
    HeroSnowMound(0.50f, 0.955f, 0.12f, 0.030f, 0.85f, 5),
    // Mid park — calmer under vehicle
    HeroSnowMound(0.42f, 0.88f, 0.08f, 0.020f, 0.45f, 6),
    HeroSnowMound(0.62f, 0.89f, 0.07f, 0.018f, 0.42f, 7),
    // Near shore — smaller, lower contrast
    HeroSnowMound(0.15f, 0.82f, 0.06f, 0.014f, 0.22f, 8),
    HeroSnowMound(0.35f, 0.805f, 0.055f, 0.012f, 0.18f, 9),
    HeroSnowMound(0.72f, 0.81f, 0.06f, 0.013f, 0.20f, 10),
    HeroSnowMound(0.90f, 0.825f, 0.05f, 0.012f, 0.24f, 11),
)

private val HeroSnowHollows = listOf(
    Triple(0.30f, 0.92f, 21),
    Triple(0.55f, 0.91f, 22),
    Triple(0.78f, 0.93f, 23),
    Triple(0.18f, 0.86f, 24),
)

private fun irregularSnowMound(cx: Float, cy: Float, rx: Float, ry: Float, seed: Int): Path =
    Path().apply {
        val n = 7
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * (PI * 2.0).toFloat()
            val spike = if (i % 2 == 0) 1.10f else 0.78f
            val wob = 0.80f + ((seed * 9 + i * 13) % 5) * 0.05f
            val x = cx + cos(ang) * rx * 0.5f * spike * wob
            val y = cy + sin(ang) * ry * 0.5f * spike * (0.88f + ((seed + i) % 3) * 0.05f)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

/**
 * Distance fog — strong far, light mid, very weak near. Keeps vehicle/UI readable.
 */
internal fun DrawScope.drawHeroDistanceFog(
    w: Float,
    h: Float,
    fogAlpha: Float,
    farHaze: Float,
    timeSec: Float,
) {
    val strength = (fogAlpha * 0.85f + farHaze * 0.35f).coerceIn(0f, 1f)
    if (strength < 0.04f) return
    val pulse = 0.94f + 0.06f * sin(timeSec * 0.18f)
    // Far (mountains / far shore)
    drawRect(
        brush = Brush.verticalGradient(
            0.28f to Color.Transparent,
            0.42f to Color(0xFFD0D8E0).copy(alpha = 0.28f * strength * pulse),
            0.58f to Color(0xFFC4CED6).copy(alpha = 0.34f * strength * pulse),
            0.68f to Color.Transparent,
        ),
    )
    // Mid — softer
    drawRect(
        brush = Brush.verticalGradient(
            0.55f to Color.Transparent,
            0.68f to Color(0xFFD8E0E6).copy(alpha = 0.14f * strength * pulse),
            0.78f to Color(0xFFD0D8E0).copy(alpha = 0.10f * strength * pulse),
            0.88f to Color.Transparent,
        ),
    )
    // Near — very low (don't wash vehicle)
    drawRect(
        brush = Brush.verticalGradient(
            0.82f to Color.Transparent,
            0.92f to Color(0xFFE0E6EA).copy(alpha = 0.05f * strength),
            1.00f to Color.Transparent,
        ),
    )
}

/** Rain wettening — darken ground slightly + sparse damp blotches (not a mirror). */
internal fun DrawScope.drawHeroRainWetGround(
    w: Float,
    h: Float,
    rainAlpha: Float,
    nightAmount: Float,
) {
    if (rainAlpha < 0.05f) return
    val a = rainAlpha * 0.22f * (1f - nightAmount * 0.25f)
    drawRect(
        color = Color(0xFF2A3038).copy(alpha = a),
        topLeft = Offset(0f, h * 0.78f),
        size = Size(w, h * 0.22f),
    )
    val blotches = listOf(0.08f to 0.88f, 0.20f to 0.93f, 0.86f to 0.90f, 0.94f to 0.95f, 0.55f to 0.91f)
    blotches.forEach { (xf, yf) ->
        drawOval(
            Color(0xFF3A4248).copy(alpha = a * 1.4f),
            topLeft = Offset(w * xf - w * 0.03f, h * yf - h * 0.01f),
            size = Size(w * 0.06f, h * 0.018f),
        )
    }
}
