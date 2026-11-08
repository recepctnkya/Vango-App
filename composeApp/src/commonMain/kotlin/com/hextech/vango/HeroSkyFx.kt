package com.hextech.vango

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/**
 * Fixed cloud anchors — shape/speed never random per frame.
 * [depth] 0 = far (small/pale), 1 = near (larger/clearer).
 * [profile] selects a distinct bump layout (not a uniform scale of one shape).
 */
private data class HeroCloudSpec(
    val xFrac: Float,
    val yFrac: Float,
    val wFrac: Float,
    val hFrac: Float,
    val alpha: Float,
    val speed: Float,
    val depth: Float,
    val profile: Int,
)

/** Partly cloudy — separate clusters, staggered heights, sky gaps. */
private val HeroPartlyClouds = listOf(
    // Long thin
    HeroCloudSpec(0.06f, 0.09f, 0.34f, 0.055f, 0.40f, 0.011f, 0.80f, 0),
    // Short plump
    HeroCloudSpec(0.48f, 0.16f, 0.18f, 0.062f, 0.30f, 0.007f, 0.40f, 1),
    // Medium asymmetric
    HeroCloudSpec(0.76f, 0.07f, 0.28f, 0.058f, 0.36f, 0.009f, 0.70f, 2),
    // Small high
    HeroCloudSpec(0.30f, 0.22f, 0.16f, 0.040f, 0.22f, 0.005f, 0.25f, 3),
    // Low long wisp (gap left of badge/clock)
    HeroCloudSpec(0.58f, 0.26f, 0.24f, 0.038f, 0.18f, 0.006f, 0.20f, 4),
)

/** Overcast — wider, overlapping layers at varied heights. */
private val HeroOvercastClouds = listOf(
    HeroCloudSpec(0.00f, 0.06f, 0.42f, 0.090f, 0.48f, 0.006f, 0.55f, 0),
    HeroCloudSpec(0.32f, 0.12f, 0.40f, 0.078f, 0.42f, 0.0045f, 0.42f, 5),
    HeroCloudSpec(0.64f, 0.05f, 0.44f, 0.095f, 0.46f, 0.0055f, 0.62f, 2),
    HeroCloudSpec(0.14f, 0.19f, 0.36f, 0.070f, 0.34f, 0.0038f, 0.32f, 1),
    HeroCloudSpec(0.50f, 0.21f, 0.38f, 0.072f, 0.38f, 0.0042f, 0.48f, 4),
    HeroCloudSpec(0.78f, 0.15f, 0.30f, 0.060f, 0.30f, 0.0050f, 0.35f, 3),
)

/** Fixed rain column seeds. */
private val HeroRainSeeds: List<Pair<Float, Float>> = List(58) { i ->
    ((i * 97 % 1000) / 1000f) to ((i * 53 % 1000) / 1000f)
}

/** Fixed snowflake seeds. */
private val HeroSnowSeeds: List<Pair<Float, Float>> = List(42) { i ->
    ((i * 73 % 1000) / 1000f) to ((i * 41 % 1000) / 1000f)
}

/**
 * Weather FX with cross-faded cloud banks ([partlyAlpha] / [overcastAlpha]).
 * Alphas / tint are animated 4–6s by the caller — never snap cloud count or regenerate shapes.
 */
fun DrawScope.drawHeroSkyWeatherFx(
    weather: HeroSkyWeather,
    windy: Boolean,
    cloudDensity: Float,
    timeSec: Float,
    skyDim: Color,
    partlyAlpha: Float,
    overcastAlpha: Float,
    rainAlpha: Float,
    snowAlpha: Float,
    fogAlpha: Float,
    cloudTint: Color = Color.White.copy(alpha = 0.5f),
    wind: WindState = WindState.Calm,
    nightAmount: Float = 0f,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedWeather = weather

    val speedMul = (0.72f + wind.speedNorm * 0.38f).coerceIn(0.72f, 1.35f) *
        if (windy) 1.06f else 1f
    val driftSign = windDriftSign(wind)
    val dens = cloudDensity.coerceIn(0f, 1f)
    // Keep precip readable at night — mild dim only
    val precipDim = (1f - nightAmount * 0.22f).coerceIn(0.70f, 1f)

    if (overcastAlpha > 0.01f) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to skyDim.copy(alpha = 0.22f * overcastAlpha),
                0.45f to skyDim.copy(alpha = 0.10f * overcastAlpha),
                0.75f to Color.Transparent,
            ),
        )
    }

    if (partlyAlpha > 0.01f) {
        drawHeroCloudBank(
            specs = HeroPartlyClouds,
            timeSec = timeSec,
            speedMul = speedMul,
            driftSign = driftSign,
            density = dens.coerceAtLeast(0.20f),
            bankAlpha = partlyAlpha,
            cooler = false,
            cloudTint = cloudTint,
        )
    }
    if (overcastAlpha > 0.01f) {
        drawHeroCloudBank(
            specs = HeroOvercastClouds,
            timeSec = timeSec,
            speedMul = speedMul * 0.88f,
            driftSign = driftSign,
            density = dens.coerceAtLeast(0.45f),
            bankAlpha = overcastAlpha,
            cooler = true,
            cloudTint = cloudTint,
        )
    }
    // Rain in sky clip; snow is drawn later (full card) so flakes can land on the park
    if (rainAlpha > 0.01f) drawHeroRain(timeSec, wind, rainAlpha * precipDim)
    // Soft sky fog only — distance fog is drawn later in the landscape stack
    if (fogAlpha > 0.01f) drawHeroFog(timeSec, fogAlpha * 0.55f, cloudTint, wind)
}

private fun DrawScope.drawHeroCloudBank(
    specs: List<HeroCloudSpec>,
    timeSec: Float,
    speedMul: Float,
    driftSign: Float,
    density: Float,
    bankAlpha: Float,
    cooler: Boolean,
    cloudTint: Color,
) {
    val w = size.width
    val h = size.height
    // Far first, near last
    specs.sortedBy { it.depth }.forEach { c ->
        val cw = w * c.wFrac * (0.88f + c.depth * 0.18f)
        val ch = h * c.hFrac * (0.90f + c.depth * 0.16f)
        // Continuous wrap; signed drift matches shared wind (trees / leaves / precip)
        val travel = w + cw
        val seed = ((c.xFrac * travel) % travel + travel) % travel
        val drift = timeSec * c.speed * speedMul * w * driftSign
        val x = -cw + ((seed + drift) % travel + travel) % travel
        val depthAlpha = (0.55f + c.depth * 0.45f)
        drawSoftHeroCloud(
            anchor = Offset(x + cw * 0.5f, h * c.yFrac),
            cw = cw,
            ch = ch,
            alpha = c.alpha * density.coerceIn(0.15f, 1f) * bankAlpha * depthAlpha,
            profile = c.profile,
            cooler = cooler,
            near = c.depth > 0.55f,
            cloudTint = cloudTint,
        )
    }
}

/**
 * Single closed soft-puff path: 4–7 top bumps, calmer irregular bottom, tapered tips.
 * Soft underlays (same silhouette, slightly larger) for narrow edge fade — no stroke contour.
 */
private fun DrawScope.drawSoftHeroCloud(
    anchor: Offset,
    cw: Float,
    ch: Float,
    alpha: Float,
    profile: Int,
    cooler: Boolean,
    near: Boolean,
    cloudTint: Color,
) {
    if (alpha < 0.02f || cw < 2f || ch < 2f) return
    val path = buildSoftCloudPath(anchor, cw, ch, profile) ?: return
    val bounds = path.getBounds()
    if (bounds.isEmpty) return

    val tintAmt = cloudTint.alpha.coerceIn(0f, 1f) * 0.35f
    val topBase = if (cooler) Color(0xFFEEF1F4) else Color(0xFFF7F5F2)
    val midBase = if (cooler) Color(0xFFD5DBE2) else Color(0xFFE4E7EB)
    val botBase = if (cooler) Color(0xFFAEB8C4) else Color(0xFFC2CAD4)
    val topCol = lerpCloud(topBase, cloudTint.copy(alpha = 1f), tintAmt * 0.4f)
    val midCol = lerpCloud(midBase, cloudTint.copy(alpha = 1f), tintAmt * 0.55f)
    val botCol = lerpCloud(botBase, cloudTint.copy(alpha = 1f), tintAmt * 0.7f)

    // Narrow soft rim — same shape scaled up, low alpha (no outline stroke / double border)
    val rimScale = if (near) 1.055f else 1.08f
    val rim = buildSoftCloudPath(anchor, cw * rimScale, ch * rimScale, profile)
    if (rim != null) {
        drawPath(rim, color = midCol.copy(alpha = alpha * 0.14f))
        val rim2 = buildSoftCloudPath(anchor, cw * (1f + (rimScale - 1f) * 0.55f), ch * (1f + (rimScale - 1f) * 0.55f), profile)
        if (rim2 != null) {
            drawPath(rim2, color = midCol.copy(alpha = alpha * 0.10f))
        }
    }

    // One united fill: broken-white top → blue-grey underside
    drawPath(
        path,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to topCol.copy(alpha = alpha),
                0.38f to midCol.copy(alpha = alpha * 0.97f),
                1.00f to botCol.copy(alpha = alpha * 0.82f),
            ),
            startY = bounds.top,
            endY = bounds.bottom,
        ),
    )

    // Soft volume — clipped so shadows never read as separate ovals
    clipPath(path) {
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to topCol.copy(alpha = alpha * 0.20f),
                0.40f to Color.Transparent,
                0.70f to botCol.copy(alpha = alpha * 0.10f),
                1.00f to botCol.copy(alpha = alpha * 0.28f),
            ),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height),
        )
        // Low-contrast belly shadow (soft blob, not a hard patch)
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to botCol.copy(alpha = alpha * 0.22f),
                    0.55f to botCol.copy(alpha = alpha * 0.08f),
                    1.0f to Color.Transparent,
                ),
                center = Offset(bounds.center.x + bounds.width * 0.06f, bounds.top + bounds.height * 0.72f),
                radius = bounds.width * 0.42f,
            ),
            topLeft = Offset(bounds.left + bounds.width * 0.12f, bounds.top + bounds.height * 0.45f),
            size = Size(bounds.width * 0.76f, bounds.height * 0.55f),
        )
        // Soft upper highlight
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to topCol.copy(alpha = alpha * 0.28f),
                    0.5f to topCol.copy(alpha = alpha * 0.08f),
                    1.0f to Color.Transparent,
                ),
                center = Offset(bounds.center.x - bounds.width * 0.08f, bounds.top + bounds.height * 0.28f),
                radius = bounds.width * 0.35f,
            ),
            topLeft = Offset(bounds.left + bounds.width * 0.15f, bounds.top),
            size = Size(bounds.width * 0.55f, bounds.height * 0.48f),
        )
    }
}

/**
 * Closed cloud silhouette from fixed bump profiles.
 * Top: 4–7 soft peaks at uneven heights. Bottom: calmer irregular curve. Tips taper.
 */
private fun buildSoftCloudPath(
    anchor: Offset,
    cw: Float,
    ch: Float,
    profile: Int,
): Path? {
    if (cw < 1f || ch < 1f) return null
    val left = anchor.x - cw * 0.5f
    val right = anchor.x + cw * 0.5f
    val midY = anchor.y
    val bumps = cloudBumpProfile(profile)
    val path = Path()

    // Left tip (tapered)
    val tipY = midY + ch * bumps.leftTipY
    path.moveTo(left, tipY)

    // Top edge — soft peaks left → right
    for (i in bumps.topX.indices) {
        val x = left + cw * bumps.topX[i]
        val y = midY - ch * bumps.topH[i]
        val prevX = if (i == 0) left else left + cw * bumps.topX[i - 1]
        val prevY = if (i == 0) tipY else midY - ch * bumps.topH[i - 1]
        val c1x = prevX + (x - prevX) * 0.35f
        val c1y = prevY - ch * bumps.topLift[i] * 0.25f
        val c2x = x - (x - prevX) * 0.35f
        val c2y = y - ch * bumps.topLift[i] * 0.15f
        path.cubicTo(c1x, c1y, c2x, c2y, x, y)
    }

    // Right tip
    val rightTipY = midY + ch * bumps.rightTipY
    val lastTopX = left + cw * bumps.topX.last()
    val lastTopY = midY - ch * bumps.topH.last()
    path.cubicTo(
        lastTopX + (right - lastTopX) * 0.45f, lastTopY + ch * 0.08f,
        right - cw * 0.04f, rightTipY - ch * 0.06f,
        right, rightTipY,
    )

    // Bottom edge — calmer, not a mirror of the top (right → left)
    for (i in bumps.botX.indices.reversed()) {
        val x = left + cw * bumps.botX[i]
        val y = midY + ch * bumps.botH[i]
        val nextI = i + 1
        val prevX = if (nextI >= bumps.botX.size) right else left + cw * bumps.botX[nextI]
        val prevY = if (nextI >= bumps.botX.size) rightTipY else midY + ch * bumps.botH[nextI]
        path.cubicTo(
            prevX - (prevX - x) * 0.40f, prevY + ch * 0.02f,
            x + (prevX - x) * 0.40f, y + ch * 0.02f,
            x, y,
        )
    }

    // Close to left tip with soft belly
    val firstBotX = left + cw * bumps.botX.first()
    val firstBotY = midY + ch * bumps.botH.first()
    path.cubicTo(
        firstBotX - cw * 0.08f, firstBotY,
        left + cw * 0.02f, tipY + ch * 0.05f,
        left, tipY,
    )
    path.close()
    return path
}

/** Fixed bump layout — distinct silhouettes, not scaled copies of one cloud. */
private data class CloudBumpProfile(
    val topX: FloatArray,
    val topH: FloatArray,
    val topLift: FloatArray,
    val botX: FloatArray,
    val botH: FloatArray,
    val leftTipY: Float,
    val rightTipY: Float,
)

private fun cloudBumpProfile(profile: Int): CloudBumpProfile = when (profile % 6) {
    // Long thin — 6 low peaks
    0 -> CloudBumpProfile(
        topX = floatArrayOf(0.10f, 0.24f, 0.40f, 0.56f, 0.72f, 0.88f),
        topH = floatArrayOf(0.38f, 0.62f, 0.48f, 0.70f, 0.52f, 0.40f),
        topLift = floatArrayOf(0.20f, 0.35f, 0.22f, 0.40f, 0.25f, 0.18f),
        botX = floatArrayOf(0.14f, 0.38f, 0.62f, 0.84f),
        botH = floatArrayOf(0.28f, 0.36f, 0.30f, 0.26f),
        leftTipY = 0.05f,
        rightTipY = 0.08f,
    )
    // Short plump — 4 tall peaks
    1 -> CloudBumpProfile(
        topX = floatArrayOf(0.18f, 0.40f, 0.62f, 0.82f),
        topH = floatArrayOf(0.55f, 0.82f, 0.72f, 0.50f),
        topLift = floatArrayOf(0.30f, 0.45f, 0.38f, 0.22f),
        botX = floatArrayOf(0.20f, 0.50f, 0.78f),
        botH = floatArrayOf(0.42f, 0.48f, 0.38f),
        leftTipY = 0.10f,
        rightTipY = 0.06f,
    )
    // Asymmetric heavy left — 5 peaks
    2 -> CloudBumpProfile(
        topX = floatArrayOf(0.12f, 0.28f, 0.46f, 0.66f, 0.86f),
        topH = floatArrayOf(0.72f, 0.88f, 0.55f, 0.48f, 0.36f),
        topLift = floatArrayOf(0.40f, 0.48f, 0.28f, 0.22f, 0.16f),
        botX = floatArrayOf(0.16f, 0.42f, 0.70f, 0.90f),
        botH = floatArrayOf(0.40f, 0.34f, 0.28f, 0.22f),
        leftTipY = 0.12f,
        rightTipY = 0.02f,
    )
    // Compact mid — 4 peaks, quieter
    3 -> CloudBumpProfile(
        topX = floatArrayOf(0.20f, 0.42f, 0.64f, 0.84f),
        topH = floatArrayOf(0.48f, 0.68f, 0.60f, 0.42f),
        topLift = floatArrayOf(0.22f, 0.34f, 0.28f, 0.18f),
        botX = floatArrayOf(0.22f, 0.52f, 0.80f),
        botH = floatArrayOf(0.32f, 0.36f, 0.30f),
        leftTipY = 0.04f,
        rightTipY = 0.07f,
    )
    // Long with 7 small peaks
    4 -> CloudBumpProfile(
        topX = floatArrayOf(0.08f, 0.20f, 0.34f, 0.48f, 0.62f, 0.76f, 0.90f),
        topH = floatArrayOf(0.32f, 0.50f, 0.42f, 0.58f, 0.44f, 0.52f, 0.34f),
        topLift = floatArrayOf(0.16f, 0.28f, 0.20f, 0.32f, 0.22f, 0.26f, 0.14f),
        botX = floatArrayOf(0.12f, 0.35f, 0.55f, 0.75f, 0.92f),
        botH = floatArrayOf(0.24f, 0.30f, 0.26f, 0.32f, 0.22f),
        leftTipY = 0.02f,
        rightTipY = 0.05f,
    )
    // Overcast slab-ish — 5 wide peaks, deeper belly
    else -> CloudBumpProfile(
        topX = floatArrayOf(0.10f, 0.30f, 0.50f, 0.70f, 0.88f),
        topH = floatArrayOf(0.45f, 0.68f, 0.78f, 0.62f, 0.42f),
        topLift = floatArrayOf(0.24f, 0.36f, 0.42f, 0.30f, 0.18f),
        botX = floatArrayOf(0.14f, 0.40f, 0.65f, 0.88f),
        botH = floatArrayOf(0.38f, 0.44f, 0.40f, 0.34f),
        leftTipY = 0.08f,
        rightTipY = 0.10f,
    )
}

private fun lerpCloud(a: Color, b: Color, t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * x,
        green = a.green + (b.green - a.green) * x,
        blue = a.blue + (b.blue - a.blue) * x,
        alpha = a.alpha,
    )
}

private fun DrawScope.drawHeroRain(timeSec: Float, wind: WindState, alpha: Float) {
    val w = size.width
    val h = size.height
    val drift = windDriftSign(wind)
    val tilt = (0.10f + wind.speedNorm * 0.20f) * (if (drift >= 0f) 1f else -1f)
    val speed = 1.05f + wind.speedNorm * 0.48f
    HeroRainSeeds.forEachIndexed { i, (seedX, seedY) ->
        val fall = ((timeSec * speed * 0.52f) + seedY) % 1.12f
        val depth = ((i % 5) / 4f) // far = small/pale
        val x = w * ((seedX + fall * abs(tilt) * (if (tilt >= 0f) 1f else -1f) * 0.5f + 1f) % 1f)
        val y = h * (fall * 0.78f - 0.05f)
        if (y > h * 0.72f || y < 0f) return@forEachIndexed
        val xf = x / w
        val yf = y / h
        if (HeroSceneUiZones.hitsAnyChrome(xf, yf)) return@forEachIndexed
        val len = h * (0.028f + (i % 4) * 0.008f) * (0.78f + depth * 0.50f)
        val streakA = (0.22f + (i % 5) * 0.055f) * alpha * (0.62f + depth * 0.40f)
        drawLine(
            color = Color(0xFFE8F2FA).copy(alpha = streakA.coerceIn(0f, 0.72f)),
            start = Offset(x, y),
            end = Offset(x + len * tilt * 2.4f, y + len),
            strokeWidth = (1.15f + depth * 0.95f),
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Falling snow that reaches the park and settles briefly before merging into cover.
 * Drawn outside the sky clip so flakes are not cut off at mid-card.
 * Near flakes: larger/faster; far: smaller/paler/slower. Wind drifts together.
 */
internal fun DrawScope.drawHeroSnow(
    timeSec: Float,
    wind: WindState,
    alpha: Float,
    snowCover: Float = 0f,
) {
    if (alpha < 0.01f && snowCover < 0.05f) return
    val w = size.width
    val h = size.height
    val drift = windDriftSign(wind)
    val fallMul = alpha.coerceIn(0f, 1f)
    // Park band where flakes land and stay (not lake mid, not under chrome)
    val groundMin = h * 0.82f
    val groundMax = h * 0.96f

    HeroSnowSeeds.forEachIndexed { i, (seedX, seedY) ->
        val depth = 0.25f + (i % 5) * 0.15f // 0 far → 1 near
        val fallSpeed = 0.055f + depth * 0.055f
        val period = 1.0f / fallSpeed
        val raw = timeSec * fallSpeed + seedY * 1.7f + i * 0.13f
        val cycle = raw - floor(raw.toDouble()).toFloat()
        // 0…0.72 fall · 0.72…0.92 settle on ground · 0.92…1 idle (absorbed into pack)
        val sway = sin((timeSec * (0.40f + depth * 0.25f) + i) * PI.toFloat()) * w * (0.010f + depth * 0.008f) +
            drift * w * (0.018f + wind.speedNorm * 0.028f) * cycle.coerceAtMost(0.72f)
        val landX = (w * seedX + sway * 0.35f + drift * w * 0.02f * (i % 3) * 0.15f)
            .coerceIn(w * 0.02f, w * 0.98f)
        val landY = groundMin + (groundMax - groundMin) * ((seedY * 0.6f + (i % 7) * 0.05f) % 1f)

        val rBase = (1.25f + depth * 1.85f) * (w / 420f).coerceIn(0.70f, 1.40f)
        val flakeA = (0.48f + depth * 0.42f)

        if (cycle < 0.72f && fallMul > 0.02f) {
            // Falling
            val t = cycle / 0.72f
            val x = w * seedX + sway
            val y = lerpFloat(-h * 0.04f, landY, t * t * 0.35f + t * 0.65f)
            val xf = (x / w).coerceIn(0f, 1f)
            val yf = (y / h).coerceIn(0f, 1f)
            if (!HeroSceneUiZones.hitsAnyChrome(xf, yf)) {
                drawFallingSnowFlake(
                    cx = x,
                    cy = y,
                    r = rBase,
                    alpha = (flakeA * fallMul).coerceIn(0f, 0.92f),
                    seed = i,
                    near = depth > 0.55f,
                )
            }
        } else if (cycle < 0.92f && fallMul > 0.02f) {
            // Settled on ground — stay put briefly
            val settle = 1f - (cycle - 0.72f) / 0.20f
            val xf = (landX / w).coerceIn(0f, 1f)
            val yf = (landY / h).coerceIn(0f, 1f)
            if (!HeroSceneUiZones.hitsAnyChrome(xf, yf) && landY > h * 0.78f) {
                val a = flakeA * fallMul * (0.60f + settle * 0.40f)
                drawSettledSnowFlake(landX, landY, rBase * 1.20f, a, i)
            }
        }
        // After absorb: persistent crumbs grow with snowCover (landed accumulation)
        if (snowCover > 0.06f && (i % 2 == 0)) {
            val persist = ((snowCover - 0.06f) / 0.45f).coerceIn(0f, 1f)
            val xf = (landX / w).coerceIn(0f, 1f)
            val yf = (landY / h).coerceIn(0f, 1f)
            if (!HeroSceneUiZones.hitsAnyChrome(xf, yf) && landY > h * 0.80f) {
                drawSettledSnowFlake(
                    landX + (i % 3 - 1) * w * 0.008f,
                    landY + (i % 2) * h * 0.004f,
                    rBase * (0.75f + persist * 0.40f),
                    flakeA * 0.70f * persist,
                    i + 17,
                )
            }
        }
        @Suppress("UNUSED_VARIABLE")
        val unusedPeriod = period
    }
}

/** Falling flake — soft core + cross arms so winter snow reads clearly. */
private fun DrawScope.drawFallingSnowFlake(
    cx: Float,
    cy: Float,
    r: Float,
    alpha: Float,
    seed: Int,
    near: Boolean,
) {
    if (alpha < 0.02f) return
    val col = Color.White.copy(alpha = alpha)
    val soft = Color(0xFFE8F2FA).copy(alpha = alpha * 0.55f)
    drawCircle(soft, radius = r * 1.35f, center = Offset(cx, cy))
    drawCircle(col, radius = r * 0.72f, center = Offset(cx, cy))
    val arm = r * (if (near) 1.55f else 1.15f)
    val stroke = (r * 0.42f).coerceIn(0.9f, 2.4f)
    drawLine(
        color = col,
        start = Offset(cx - arm, cy),
        end = Offset(cx + arm, cy),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = col,
        start = Offset(cx, cy - arm),
        end = Offset(cx, cy + arm),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    if (near || seed % 2 == 0) {
        val d = arm * 0.72f
        drawLine(
            color = col.copy(alpha = alpha * 0.85f),
            start = Offset(cx - d, cy - d),
            end = Offset(cx + d, cy + d),
            strokeWidth = stroke * 0.75f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = col.copy(alpha = alpha * 0.85f),
            start = Offset(cx - d, cy + d),
            end = Offset(cx + d, cy - d),
            strokeWidth = stroke * 0.75f,
            cap = StrokeCap.Round,
        )
    }
}

/** Tiny settled flake — not a round white dot carpet; irregular chip on the pack. */
private fun DrawScope.drawSettledSnowFlake(
    cx: Float,
    cy: Float,
    r: Float,
    alpha: Float,
    seed: Int,
) {
    if (alpha < 0.02f) return
    val col = HeroSnowLit.copy(alpha = alpha.coerceIn(0f, 0.82f))
    val rx = r * (1.15f + (seed % 3) * 0.18f)
    val ry = r * (0.58f + (seed % 2) * 0.22f)
    drawOval(
        col,
        topLeft = Offset(cx - rx, cy - ry * 0.35f),
        size = Size(rx * 2f, ry * 1.4f),
    )
    // Soft contact so it sits on the surface
    drawOval(
        Color.Black.copy(alpha = alpha * 0.14f),
        topLeft = Offset(cx - rx * 0.9f, cy + ry * 0.25f),
        size = Size(rx * 1.8f, ry * 0.45f),
    )
}

private fun DrawScope.drawHeroFog(
    timeSec: Float,
    alpha: Float,
    cloudTint: Color,
    wind: WindState = WindState.Calm,
) {
    val w = size.width
    val h = size.height
    val pulse = 0.92f + 0.08f * sin(timeSec * 0.22f).toFloat()
    drawRect(
        brush = Brush.verticalGradient(
            0.20f to Color.Transparent,
            0.38f to Color(0xFFD5DEE6).copy(alpha = 0.16f * pulse * alpha),
            0.52f to Color(0xFFC8D2DA).copy(alpha = 0.24f * pulse * alpha),
            0.62f to Color(0xFFB8C4CE).copy(alpha = 0.14f * pulse * alpha),
            0.72f to Color.Transparent,
        ),
    )
    val travel = w + w * 0.55f
    val drift = timeSec * 6.5f * windDriftSign(wind)
    val x = -w * 0.55f + ((drift % travel + travel) % travel)
    drawSoftHeroCloud(
        anchor = Offset(x + w * 0.275f, h * 0.42f),
        cw = w * 0.55f,
        ch = h * 0.10f,
        alpha = 0.14f * pulse * alpha,
        profile = 3,
        cooler = true,
        near = false,
        cloudTint = cloudTint,
    )
}
