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
 * Framing pines + a few deciduous trees (seasonal foliage).
 * [farOnly] = opposite shore only; near pass draws before park/vehicle so trunks tuck under.
 */
internal fun DrawScope.drawHeroTreeClusters(
    w: Float,
    h: Float,
    palette: HeroScenePalette,
    light: HeroSceneLight,
    farOnly: Boolean,
    season: SeasonState = SeasonState.Summer,
    seasonProgress: Float = 0.35f,
    foliageAmount: Float = 1f,
    wind: WindState = WindState.Calm,
    timeSec: Float = 0f,
    snowCover: Float = 0f,
) {
    val farCol = light.shadeColor(
        Color(
            red = palette.tree.red * 0.28f + palette.mountainFar.red * 0.72f,
            green = palette.tree.green * 0.30f + palette.mountainFar.green * 0.70f,
            blue = palette.tree.blue * 0.22f + palette.mountainFar.blue * 0.78f,
            alpha = 1f,
        ),
        0.3f,
    )
    val farLit = light.litColor(lerpColor(farCol, Color.White, 0.05f + 0.04f * light.direct), 0.35f)
    if (farOnly) {
        drawHeroFarShoreForest(w, h, farCol, farLit, light)
        return
    }

    val nearCol = light.shadeColor(palette.tree, 0.35f)
    val midCol = light.shadeColor(lerpColor(nearCol, palette.mountainNear, 0.35f), 0.3f)
    val plantPop = (0.28f + 0.18f * light.direct) * (1f - light.soft * 0.45f)
    // Pines: always evergreen; summer/autumn mature dark; spring tip shoots only
    val pineDarken = when (season) {
        SeasonState.Summer -> 0.28f
        SeasonState.Autumn -> 0.24f
        SeasonState.Spring -> 0.16f
        SeasonState.Winter -> 0.10f
    }
    val pineCol = lerpColor(nearCol, Color.Black, pineDarken)
    val pineMid = lerpColor(midCol, Color.Black, pineDarken * 0.75f)
    val litOlive = light.litColor(lerpColor(pineCol, Color(0xFF3E5538), plantPop * 0.40f), 0.40f)
    val litMid = light.litColor(lerpColor(pineMid, Color(0xFF364C34), plantPop * 0.35f), 0.38f)
    // No branch snow in spring/summer unless real snow weather drives cover
    val treeSnow = when (season) {
        SeasonState.Spring, SeasonState.Summer -> 0f
        else -> snowCover
    }

    fun pine(
        base: Offset,
        height: Float,
        color: Color,
        lit: Color,
        profile: Int,
        maxBranchW: Float,
        leanOut: Float,
        detail: Float,
        phase: Float,
    ) = drawHeroPine(
        base = base,
        height = height,
        color = color,
        litColor = lit,
        light = light,
        profile = profile,
        maxBranchW = maxBranchW,
        leanOut = leanOut,
        detail = detail,
        timeSec = timeSec,
        wind = wind,
        swayPhase = phase,
        snowCover = treeSnow,
        season = season,
    )

    // —— RIGHT pines (back → front) ——
    pine(Offset(w * 0.84f, h * 0.62f), h * 0.28f, lerpColor(pineMid, farCol, 0.45f), farLit, 3, w * 0.055f, 0.35f, 0.45f, 0.10f)
    pine(Offset(w * 0.895f, h * 0.73f), h * 0.59f, pineMid, litMid, 2, w * 0.085f, 0.55f, 0.70f, 0.37f)
    pine(Offset(w * 0.965f, h * 0.84f), h * 0.77f, pineCol, litOlive, 0, w * 0.10f, 0.85f, 1f, 0.62f)

    // —— LEFT pines ——
    pine(Offset(w * 0.105f, h * 0.76f), h * 0.47f, pineMid, litMid, 1, w * 0.07f, -0.40f, 0.75f, 0.88f)
    pine(Offset(w * 0.045f, h * 0.84f), h * 0.88f, pineCol, litOlive, 4, w * 0.12f, -0.90f, 1f, 1.15f)

    // Deciduous companions — colors from continuous seasonProgress (geometry fixed)
    val (decDark, decLit) = deciduousColorsFromProgress(seasonProgress, foliageAmount)
    val dDark = light.shadeColor(decDark, 0.35f)
    val dLit = light.litColor(decLit, 0.45f)
    drawHeroDeciduousTree(
        base = Offset(w * 0.78f, h * 0.72f),
        height = h * 0.38f,
        crownW = w * 0.09f,
        dark = dDark,
        lit = dLit,
        light = light,
        foliage = foliageAmount,
        timeSec = timeSec,
        wind = wind,
        swayPhase = 0.22f,
        snowCover = treeSnow,
        profile = 0,
        season = season,
        seasonProgress = seasonProgress,
    )
    drawHeroDeciduousTree(
        base = Offset(w * 0.18f, h * 0.74f),
        height = h * 0.34f,
        crownW = w * 0.08f,
        dark = dDark,
        lit = dLit,
        light = light,
        foliage = foliageAmount,
        timeSec = timeSec,
        wind = wind,
        swayPhase = 1.40f,
        snowCover = treeSnow,
        profile = 1,
        season = season,
        seasonProgress = seasonProgress,
    )
    drawHeroDeciduousTree(
        base = Offset(w * 0.91f, h * 0.68f),
        height = h * 0.26f,
        crownW = w * 0.055f,
        dark = lerpColor(dDark, farCol, 0.25f),
        lit = lerpColor(dLit, farLit, 0.2f),
        light = light,
        foliage = foliageAmount * 0.9f,
        timeSec = timeSec,
        wind = wind,
        swayPhase = 2.05f,
        snowCover = treeSnow,
        profile = 2,
        season = season,
        seasonProgress = seasonProgress,
    )
}

/**
 * Fixed leaf-cluster seeds (relative to tip / crown). 8–12 per profile.
 * tone: 0 = shade, 1 = main, 2 = sunlit shoot (upper/outer only).
 */
private data class DecidCluster(
    val dx: Float,
    val dy: Float,
    val rw: Float,
    val rh: Float,
    val tone: Int,
    val seed: Int,
)

private val DecidClustersByProfile: List<List<DecidCluster>> = listOf(
    // profile 0 — 10 clusters
    listOf(
        DecidCluster(-0.42f, -0.08f, 0.38f, 0.28f, 0, 1),
        DecidCluster(0.08f, -0.22f, 0.44f, 0.32f, 1, 2),
        DecidCluster(0.48f, -0.06f, 0.34f, 0.26f, 1, 3),
        DecidCluster(-0.18f, 0.12f, 0.36f, 0.24f, 0, 4),
        DecidCluster(0.28f, 0.10f, 0.40f, 0.30f, 1, 5),
        DecidCluster(-0.52f, 0.18f, 0.28f, 0.22f, 0, 6),
        DecidCluster(0.55f, 0.16f, 0.30f, 0.24f, 2, 7),
        DecidCluster(0.00f, -0.38f, 0.32f, 0.22f, 2, 8),
        DecidCluster(-0.30f, -0.28f, 0.26f, 0.20f, 2, 9),
        DecidCluster(0.22f, -0.02f, 0.22f, 0.18f, 1, 10),
    ),
    // profile 1 — 11
    listOf(
        DecidCluster(0.40f, -0.10f, 0.36f, 0.28f, 0, 11),
        DecidCluster(-0.10f, -0.24f, 0.42f, 0.30f, 1, 12),
        DecidCluster(-0.48f, -0.04f, 0.32f, 0.26f, 1, 13),
        DecidCluster(0.15f, 0.14f, 0.38f, 0.26f, 0, 14),
        DecidCluster(-0.28f, 0.12f, 0.34f, 0.28f, 1, 15),
        DecidCluster(0.50f, 0.18f, 0.28f, 0.22f, 0, 16),
        DecidCluster(-0.55f, 0.20f, 0.30f, 0.24f, 2, 17),
        DecidCluster(0.05f, -0.40f, 0.30f, 0.20f, 2, 18),
        DecidCluster(0.32f, -0.30f, 0.24f, 0.18f, 2, 19),
        DecidCluster(-0.20f, -0.02f, 0.22f, 0.16f, 1, 20),
        DecidCluster(0.18f, 0.28f, 0.26f, 0.20f, 0, 21),
    ),
    // profile 2 — 9 (smaller tree)
    listOf(
        DecidCluster(-0.35f, -0.12f, 0.36f, 0.28f, 0, 22),
        DecidCluster(0.12f, -0.26f, 0.40f, 0.30f, 1, 23),
        DecidCluster(0.42f, -0.02f, 0.30f, 0.24f, 1, 24),
        DecidCluster(-0.12f, 0.14f, 0.34f, 0.26f, 0, 25),
        DecidCluster(0.28f, 0.16f, 0.32f, 0.24f, 1, 26),
        DecidCluster(0.00f, -0.38f, 0.28f, 0.20f, 2, 27),
        DecidCluster(-0.45f, 0.10f, 0.24f, 0.20f, 0, 28),
        DecidCluster(0.48f, 0.12f, 0.26f, 0.22f, 2, 29),
        DecidCluster(0.18f, -0.08f, 0.20f, 0.16f, 2, 30),
    ),
)

/** Deciduous: trunk + limbs + fixed leaf clusters (or bare in winter). */
internal fun DrawScope.drawHeroDeciduousTree(
    base: Offset,
    height: Float,
    crownW: Float,
    dark: Color,
    lit: Color,
    light: HeroSceneLight,
    foliage: Float,
    timeSec: Float,
    wind: WindState,
    swayPhase: Float,
    snowCover: Float,
    profile: Int,
    season: SeasonState = SeasonState.Summer,
    seasonProgress: Float = 0.35f,
) {
    val trunkH = height * 0.55f
    val tipSway = windSwayOffsetX(1f, timeSec, swayPhase, wind, crownW)
    val midSway = windSwayOffsetX(0.55f, timeSec, swayPhase, wind, crownW)
    val trunk = Color(0xFF4A3A2E)
    val tip = Offset(base.x + tipSway * 0.35f, base.y - trunkH)
    val mid = Offset(base.x + midSway * 0.45f, base.y - trunkH * 0.55f)
    drawLine(trunk, base, mid, strokeWidth = height * 0.028f, cap = StrokeCap.Round)
    drawLine(trunk, mid, tip, strokeWidth = height * 0.016f, cap = StrokeCap.Round)

    val limbDir = if (profile % 2 == 0) 1f else -1f
    val limbEnd = Offset(
        tip.x + limbDir * crownW * 0.38f + tipSway * 0.25f,
        tip.y + height * 0.10f,
    )
    val limb2 = Offset(
        tip.x - limbDir * crownW * 0.28f + tipSway * 0.15f,
        tip.y + height * 0.06f,
    )
    drawLine(trunk.copy(alpha = 0.9f), tip, limbEnd, strokeWidth = height * 0.011f, cap = StrokeCap.Round)
    drawLine(trunk.copy(alpha = 0.82f), tip, limb2, strokeWidth = height * 0.009f, cap = StrokeCap.Round)

    if (foliage < 0.12f) {
        val barkDark = Color(0xFF3A3028)
        // Darker shade pass on trunk (already stroked above)
        drawLine(barkDark.copy(alpha = 0.55f), Offset(base.x + height * 0.006f, base.y), mid, strokeWidth = height * 0.018f, cap = StrokeCap.Round)
        val twigs = mutableListOf<Pair<Offset, Offset>>()
        for (i in 0 until 6) {
            val a = (i / 6f) * PI.toFloat() * 1.05f - 0.5f
            val len = crownW * (0.26f + (i % 3) * 0.09f)
            val end = Offset(tip.x + cos(a) * len * limbDir, tip.y + sin(a) * len * 0.55f)
            drawLine(
                barkDark.copy(alpha = 0.78f),
                tip,
                end,
                strokeWidth = height * 0.0065f,
                cap = StrokeCap.Round,
            )
            twigs += tip to end
        }
        twigs += tip to limbEnd
        twigs += tip to limb2
        // Thin snow only on upper faces of horizontal/sloped limbs — not the vertical trunk
        if (snowCover > 0.08f) {
            val snowA = (0.40f + snowCover * 0.45f).coerceIn(0.35f, 0.85f)
            fun snowOnLimb(from: Offset, to: Offset, thick: Float) {
                val dy = to.y - from.y
                val dx = to.x - from.x
                if (abs(dy) > abs(dx) * 1.35f) return
                val lift = thick * 0.55f
                drawLine(
                    HeroSnowLit.copy(alpha = snowA),
                    Offset(from.x, from.y - lift),
                    Offset(to.x, to.y - lift),
                    strokeWidth = thick,
                    cap = StrokeCap.Round,
                )
            }
            snowOnLimb(tip, limbEnd, height * 0.010f)
            snowOnLimb(tip, limb2, height * 0.008f)
            twigs.forEach { (a, b) -> snowOnLimb(a, b, height * 0.0055f) }
            // Soft mound at trunk base
            drawOval(
                HeroSnowMain.copy(alpha = 0.55f * snowCover),
                topLeft = Offset(base.x - crownW * 0.22f, base.y - height * 0.02f),
                size = Size(crownW * 0.44f, height * 0.04f),
            )
        }
        return
    }

    val a = foliage.coerceIn(0f, 1f)
    val autumnProg = when {
        seasonProgress in 0.48f..0.78f -> heroSmoothstep((seasonProgress - 0.48f) / 0.27f)
        season == SeasonState.Autumn -> ((0.90f - a) / 0.62f).coerceIn(0f, 1f)
        else -> 0f
    }
    val outerLit = light.litColor(deciduousLitFromProgress(seasonProgress), 0.50f)
    // Continuous fill scale along year phase (geometry seeds unchanged)
    val fillScale = when {
        seasonProgress < 0.25f -> lerpFloat(0.92f, 1.05f, heroSmoothstep(seasonProgress / 0.25f))
        seasonProgress < 0.50f -> lerpFloat(1.05f, 1.18f, heroSmoothstep((seasonProgress - 0.25f) / 0.25f))
        seasonProgress < 0.75f -> {
            val t = heroSmoothstep((seasonProgress - 0.50f) / 0.25f)
            lerpFloat(1.12f, 0.78f, t)
        }
        else -> lerpFloat(0.78f, 0.70f, heroSmoothstep((seasonProgress - 0.75f) / 0.25f))
    }
    val clusters = DecidClustersByProfile[profile.coerceIn(0, DecidClustersByProfile.lastIndex)]
    // Late autumn: drop some clusters so limbs show through
    fun keepCluster(c: DecidCluster): Boolean {
        if (season != SeasonState.Autumn) return true
        return when {
            autumnProg < 0.35f -> true
            autumnProg < 0.65f -> (c.seed % 5) != 0
            else -> (c.seed % 3) != 0 && !(c.tone == 2 && (c.seed % 2 == 0))
        }
    }
    // Extra twigs visible through thinning canopy
    if (season == SeasonState.Autumn && autumnProg > 0.40f) {
        val twigN = (2 + (autumnProg * 3f).toInt()).coerceIn(2, 5)
        for (i in 0 until twigN) {
            val ang = (i / twigN.toFloat()) * PI.toFloat() * 0.95f - 0.45f
            val len = crownW * (0.22f + (i % 3) * 0.07f) * (0.85f + autumnProg * 0.25f)
            drawLine(
                Color(0xFF4A3A2E).copy(alpha = 0.55f + autumnProg * 0.25f),
                tip,
                Offset(tip.x + cos(ang) * len * limbDir, tip.y + sin(ang) * len * 0.50f),
                strokeWidth = height * 0.006f,
                cap = StrokeCap.Round,
            )
        }
    }
    // Shade → main → outer lit — autumn mixes green/gold/copper/brown per cluster
    clusters.sortedBy { it.tone }.forEach { c ->
        if (!keepCluster(c)) return@forEach
        val cx = tip.x + tipSway * 0.12f + c.dx * crownW
        val cy = tip.y - height * 0.06f + c.dy * height
        val col = if (autumnProg > 0.08f) {
            val raw = autumnClusterLeafColor(c.tone, c.seed, a)
            when (c.tone) {
                0 -> light.shadeColor(raw, 0.40f)
                2 -> light.litColor(raw, 0.50f)
                else -> light.litColor(raw, 0.35f)
            }
        } else {
            when (c.tone) {
                0 -> dark
                2 -> outerLit
                else -> lit
            }
        }
        val alpha = when {
            season == SeasonState.Summer && c.tone == 0 -> 0.96f * a
            season == SeasonState.Summer && c.tone == 1 -> 0.94f * a
            season == SeasonState.Summer && c.tone == 2 -> 0.70f * a
            season == SeasonState.Autumn && c.tone == 0 -> 0.94f * a
            season == SeasonState.Autumn && c.tone == 2 -> 0.78f * a
            c.tone == 0 -> 0.92f * a
            c.tone == 2 -> 0.72f * a
            else -> 0.88f * a
        }
        drawLeafCluster(
            cx = cx,
            cy = cy,
            rw = crownW * c.rw * fillScale,
            rh = height * c.rh * fillScale,
            color = col.copy(alpha = alpha),
            seed = c.seed,
        )
    }
}

/** Opaque irregular leaf mass — jagged edge, attached look (not a transparent disc). */
private fun DrawScope.drawLeafCluster(
    cx: Float,
    cy: Float,
    rw: Float,
    rh: Float,
    color: Color,
    seed: Int,
) {
    val n = 7 + (seed % 3)
    val path = Path()
    for (i in 0 until n) {
        val ang = (i / n.toFloat()) * (PI * 2.0).toFloat() - 0.3f
        val spike = if (i % 2 == 0) 1.08f else 0.78f
        val wobble = 0.82f + ((seed * 11 + i * 17) % 5) * 0.05f
        val x = cx + cos(ang) * rw * 0.5f * spike * wobble
        val y = cy + sin(ang) * rh * 0.5f * spike * (0.9f + ((seed + i) % 3) * 0.04f)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

/**
 * Far-shore forest — small filled, merging silhouettes (not thin sticks / stacked triangles).
 * Seed: xFrac, baseYFrac, heightFrac (0.03–0.10H), widthFrac, profile.
 */
internal fun DrawScope.drawHeroFarShoreForest(
    w: Float,
    h: Float,
    color: Color,
    lit: Color,
    light: HeroSceneLight,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedLit = lit
    @Suppress("UNUSED_PARAMETER")
    val unusedLight = light
    val specs = listOf(
        // left spit — overlapping masses
        floatArrayOf(0.035f, 0.608f, 0.050f, 0.028f, 0f),
        floatArrayOf(0.058f, 0.600f, 0.078f, 0.036f, 1f),
        floatArrayOf(0.082f, 0.610f, 0.042f, 0.024f, 2f),
        floatArrayOf(0.105f, 0.598f, 0.088f, 0.040f, 0f),
        floatArrayOf(0.128f, 0.612f, 0.038f, 0.022f, 3f),
        floatArrayOf(0.150f, 0.604f, 0.062f, 0.030f, 1f),
        floatArrayOf(0.172f, 0.614f, 0.034f, 0.020f, 2f),
        // sparse mid — keep card center open
        floatArrayOf(0.30f, 0.616f, 0.032f, 0.018f, 0f),
        floatArrayOf(0.44f, 0.612f, 0.028f, 0.016f, 1f),
        floatArrayOf(0.56f, 0.618f, 0.034f, 0.018f, 2f),
        // right spit
        floatArrayOf(0.78f, 0.610f, 0.040f, 0.022f, 2f),
        floatArrayOf(0.802f, 0.598f, 0.072f, 0.034f, 0f),
        floatArrayOf(0.828f, 0.612f, 0.036f, 0.020f, 3f),
        floatArrayOf(0.852f, 0.596f, 0.095f, 0.042f, 1f),
        floatArrayOf(0.878f, 0.608f, 0.048f, 0.026f, 2f),
        floatArrayOf(0.902f, 0.594f, 0.080f, 0.036f, 0f),
        floatArrayOf(0.928f, 0.610f, 0.044f, 0.024f, 3f),
        floatArrayOf(0.955f, 0.602f, 0.058f, 0.028f, 1f),
    )
    // Soft ground wash under crowns so individuals blend into a ridge
    drawPath(
        Path().apply {
            moveTo(w * 0.02f, h * 0.618f)
            cubicTo(w * 0.08f, h * 0.590f, w * 0.14f, h * 0.600f, w * 0.18f, h * 0.618f)
            lineTo(w * 0.18f, h * 0.628f)
            lineTo(w * 0.02f, h * 0.628f)
            close()
        },
        color = color.copy(alpha = 0.55f),
    )
    drawPath(
        Path().apply {
            moveTo(w * 0.76f, h * 0.618f)
            cubicTo(w * 0.84f, h * 0.588f, w * 0.92f, h * 0.598f, w * 0.98f, h * 0.616f)
            lineTo(w * 0.98f, h * 0.630f)
            lineTo(w * 0.76f, h * 0.630f)
            close()
        },
        color = color.copy(alpha = 0.55f),
    )
    specs.forEach { s ->
        drawHeroFarPineBlob(
            cx = w * s[0],
            baseY = h * s[1],
            height = h * s[2],
            halfW = w * s[3] * 0.5f,
            color = color,
            profile = s[4].toInt(),
        )
    }
}

/** Tiny filled pine mass for far shore — single connected blob, no layered polygons. */
internal fun DrawScope.drawHeroFarPineBlob(
    cx: Float,
    baseY: Float,
    height: Float,
    halfW: Float,
    color: Color,
    profile: Int,
) {
    val tipY = baseY - height
    val lean = when (profile % 4) {
        1 -> -0.18f
        2 -> 0.22f
        3 -> -0.10f
        else -> 0.08f
    }
    val midY = tipY + height * 0.42f
    val lowY = tipY + height * 0.78f
    val path = Path().apply {
        moveTo(cx + lean * halfW, tipY)
        // Right scallops (connected, overlapping)
        cubicTo(
            cx + halfW * 0.35f, tipY + height * 0.12f,
            cx + halfW * 0.85f, midY - height * 0.06f,
            cx + halfW * (0.70f + (profile % 3) * 0.08f), midY,
        )
        cubicTo(
            cx + halfW * 1.05f, midY + height * 0.10f,
            cx + halfW * 0.95f, lowY - height * 0.04f,
            cx + halfW * 0.55f, lowY,
        )
        lineTo(cx + halfW * 0.18f, baseY)
        lineTo(cx - halfW * 0.16f, baseY)
        // Left scallops (asymmetric widths)
        cubicTo(
            cx - halfW * 0.50f, lowY,
            cx - halfW * 1.00f, midY + height * 0.08f,
            cx - halfW * (0.65f + (profile % 2) * 0.10f), midY,
        )
        cubicTo(
            cx - halfW * 0.80f, midY - height * 0.08f,
            cx - halfW * 0.30f, tipY + height * 0.14f,
            cx + lean * halfW, tipY,
        )
        close()
    }
    drawPath(path, color = color)
}
/**
 * Pine from trunk + asymmetric side branches + irregular needle clusters.
 * No stacked diamond / bead lobes. [leanOut] <0 left, >0 right. [profile] varies branch layout.
 */
internal fun DrawScope.drawHeroPine(
    base: Offset,
    height: Float,
    color: Color,
    litColor: Color,
    light: HeroSceneLight,
    profile: Int = 0,
    maxBranchW: Float = height * 0.45f,
    leanOut: Float = 0f,
    detail: Float = 1f,
    timeSec: Float = 0f,
    wind: WindState = WindState.Calm,
    swayPhase: Float = 0f,
    snowCover: Float = 0f,
    season: SeasonState = SeasonState.Summer,
) {
    val tipY = base.y - height
    val lean = leanOut.coerceIn(-1f, 1f)
    val tipX = base.x + lean * maxBranchW * 0.05f
    // Soft single-curve lean — no zigzag
    val midX = lerpFloat(base.x, tipX, 0.52f) + lean * maxBranchW * 0.018f
    val trunkBotW = (maxBranchW * 0.10f).coerceIn(height * 0.016f, maxBranchW * 0.13f)
    val trunkTopW = trunkBotW * 0.18f
    val trunkColor = lerpColor(color, Color.Black, 0.28f)
    val needleDark = color
    val needleLit = lerpColor(litColor, color, 0.35f)
    val springShoot = season == SeasonState.Spring

    fun trunkXAt(tFromTip: Float): Float {
        val t = tFromTip.coerceIn(0f, 1f)
        // Quadratic Bezier base → mid → tip
        val u = 1f - t
        val x = u * u * tipX + 2f * u * t * midX + t * t * base.x
        // Wind: root fixed (t≈1 at base in tip-fraction), tip bends more
        val tFromBase = 1f - t
        return x + windSwayOffsetX(tFromBase, timeSec, swayPhase, wind, maxBranchW)
    }
    fun trunkWAt(tFromTip: Float): Float {
        val t = tFromTip.coerceIn(0f, 1f)
        return lerpFloat(trunkTopW, trunkBotW, t * t)
    }

    data class PineBranch(
        val t: Float,
        val side: Float,
        val len: Float,
        val droop: Float,
        val thick: Float,
        val seed: Int,
    )

    // 8–12 main branches; layout shifts with profile so trees don't match
    val branchCount = (8 + (profile % 5)).coerceIn(8, 12)
    val branches = ArrayList<PineBranch>(branchCount)
    var bi = 0
    var slot = 0
    while (bi < branchCount && slot < branchCount + 4) {
        val t = 0.10f + (slot + 0.35f) / (branchCount + 1.2f) * 0.78f
        // Skip some slots so L/R aren't mirrored twins
        val skip = (slot * 3 + profile * 5) % 7 == 1 && bi > 1 && bi < branchCount - 1
        if (!skip) {
            val preferRight = ((slot + profile) % 2 == 0) xor (profile % 3 == 1)
            val side = if (preferRight) 1f else -1f
            // Upper short, lower longer; per-side length mismatch
            val widen = (0.22f + t * 0.78f).coerceIn(0.22f, 1f)
            val sideMul = if (side > 0f) {
                0.72f + ((slot * 11 + profile * 7) % 5) * 0.055f
            } else {
                0.68f + ((slot * 13 + profile * 3) % 5) * 0.060f
            }
            val leanMul = if (side * lean > 0f) 1f + abs(lean) * 0.22f else 1f - abs(lean) * 0.10f
            val shortFoot = if (t > 0.78f) 0.62f + (1f - t) * 1.4f else 1f
            val len = (maxBranchW * 0.48f * widen * sideMul * leanMul * shortFoot)
                .coerceAtMost(maxBranchW * 0.52f)
            branches += PineBranch(
                t = t + ((slot * 17 + profile) % 5) * 0.008f,
                side = side,
                len = len,
                droop = height * (0.018f + t * 0.045f) * (0.85f + ((slot + profile) % 3) * 0.08f),
                thick = trunkWAt(t) * (0.55f + (1f - t) * 0.35f),
                seed = slot * 31 + profile * 17,
            )
            bi++
        }
        slot++
    }

    // Tip leader needles (narrow apex)
    val tipCluster = Path().apply {
        val y0 = tipY
        val y1 = tipY + height * 0.11f
        moveTo(tipX, y0)
        lineTo(tipX + maxBranchW * 0.06f, y1 * 0.35f + tipY * 0.65f)
        quadraticBezierTo(tipX + maxBranchW * 0.04f, y1, tipX, tipY + height * 0.09f)
        quadraticBezierTo(tipX - maxBranchW * 0.04f, y1, tipX - maxBranchW * 0.055f, tipY + height * 0.04f)
        close()
    }
    drawPath(tipCluster, color = needleDark)
    if (snowCover > 0.08f) {
        val sx = trunkXAt(0.02f)
        drawOval(
            Color.White.copy(alpha = 0.38f * snowCover),
            topLeft = Offset(sx - maxBranchW * 0.06f, tipY - height * 0.01f),
            size = Size(maxBranchW * 0.12f, height * 0.03f),
        )
    }

    // Needle masses first (behind trunk mid-section), then trunk, then near-trunk accents
    branches.forEach { br ->
        val ax = trunkXAt(br.t)
        val ay = tipY + height * br.t
        val ex = ax + br.side * br.len
        val ey = ay + br.droop
        drawPineNeedleCluster(
            ax = ax,
            ay = ay,
            ex = ex,
            ey = ey,
            halfThick = br.len * (0.16f + (1f - br.t) * 0.10f),
            color = needleDark,
            seed = br.seed,
            detail = detail,
        )
    }

    // 1) Continuous tapering trunk base → tip
    drawPath(
        Path().apply {
            val steps = 8
            moveTo(base.x - trunkBotW * 0.5f, base.y)
            lineTo(base.x + trunkBotW * 0.5f, base.y)
            for (i in 1..steps) {
                val t = i / steps.toFloat() // 0 base → 1 tip in this loop? use from base up
                val tf = 1f - t // from tip fraction
                val x = trunkXAt(tf)
                val hw = trunkWAt(tf) * 0.5f
                lineTo(x + hw, tipY + height * tf)
            }
            for (i in steps downTo 0) {
                val tf = i / steps.toFloat()
                val x = trunkXAt(tf)
                val hw = trunkWAt(tf) * 0.5f
                lineTo(x - hw, tipY + height * tf)
            }
            close()
        },
        color = trunkColor,
    )

    // 2) Branch cores (read as wood arms under needles)
    val branchStroke = trunkColor.copy(alpha = 0.85f)
    branches.forEach { br ->
        val ax = trunkXAt(br.t)
        val ay = tipY + height * br.t
        val ex = ax + br.side * br.len * 0.92f
        val ey = ay + br.droop * 0.92f
        val mx = lerpFloat(ax, ex, 0.55f)
        val my = lerpFloat(ay, ey, 0.45f) + br.droop * 0.15f
        drawPath(
            Path().apply {
                moveTo(ax, ay)
                quadraticBezierTo(mx, my, ex, ey)
            },
            color = branchStroke,
            style = Stroke(
                width = br.thick.coerceIn(height * 0.006f, trunkBotW * 0.55f),
                cap = StrokeCap.Round,
            ),
        )
    }

    // Re-lay tip needles and outer needle tips so they sit over trunk near apex
    drawPath(tipCluster, color = needleDark)
    branches.filter { it.t < 0.38f }.forEach { br ->
        val ax = trunkXAt(br.t)
        val ay = tipY + height * br.t
        val ex = ax + br.side * br.len
        val ey = ay + br.droop
        drawPineNeedleCluster(
            ax = ax + br.side * br.len * 0.15f,
            ay = ay,
            ex = ex,
            ey = ey,
            halfThick = br.len * 0.12f,
            color = needleDark,
            seed = br.seed + 9,
            detail = detail * 0.85f,
        )
    }

    // Snow on upward-facing branch tops — attached clumps, green still visible beneath
    if (snowCover > 0.10f) {
        val snowA = (0.35f + snowCover * 0.50f).coerceIn(0.30f, 0.88f)
        branches.forEach { br ->
            // More snow on lower wide branches; sparse on tip leaders
            val amount = (0.25f + br.t * 0.85f).coerceIn(0.20f, 1f)
            if ((br.seed % 5) == 0 && br.t < 0.35f) return@forEach
            val ax = trunkXAt(br.t)
            val ay = tipY + height * br.t
            val ex = ax + br.side * br.len * 0.88f
            val ey = ay + br.droop * 0.75f
            val segs = 2 + (br.seed % 3)
            for (s in 0 until segs) {
                val t0 = 0.12f + s * (0.70f / segs)
                val t1 = (t0 + 0.18f + (br.seed % 3) * 0.04f).coerceAtMost(0.92f)
                val x0 = lerpFloat(ax, ex, t0)
                val y0 = lerpFloat(ay, ey, t0) - br.len * 0.04f * amount
                val x1 = lerpFloat(ax, ex, t1)
                val y1 = lerpFloat(ay, ey, t1) - br.len * 0.05f * amount
                val thick = br.len * (0.06f + amount * 0.08f) * (0.7f + snowCover * 0.4f)
                drawLine(
                    HeroSnowLit.copy(alpha = snowA * (0.65f + amount * 0.35f)),
                    Offset(x0, y0),
                    Offset(x1, y1),
                    strokeWidth = thick,
                    cap = StrokeCap.Round,
                )
            }
        }
        // Soft mound + contact at pine base
        drawOval(
            Color.Black.copy(alpha = 0.12f * snowCover),
            topLeft = Offset(base.x - maxBranchW * 0.28f, base.y - height * 0.01f),
            size = Size(maxBranchW * 0.56f, height * 0.035f),
        )
        drawOval(
            HeroSnowMain.copy(alpha = 0.60f * snowCover),
            topLeft = Offset(base.x - maxBranchW * 0.32f, base.y - height * 0.025f),
            size = Size(maxBranchW * 0.64f, height * 0.045f),
        )
    }

    // 5) Low-contrast light faces on a few sunward branch tops
    if (detail > 0.35f) {
        val sunSide = if (light.dirX >= 0f) -1f else 1f
        val contrast = (0.22f + 0.55f * light.strength).coerceIn(0.22f, 0.85f)
        branches.forEachIndexed { i, br ->
            if (br.side != sunSide) return@forEachIndexed
            if ((i + profile) % 3 == 0) return@forEachIndexed
            val ax = trunkXAt(br.t)
            val ay = tipY + height * br.t
            val ex = ax + br.side * br.len * 0.78f
            val ey = ay + br.droop * 0.70f
            val mx = lerpFloat(ax, ex, 0.40f)
            val my = lerpFloat(ay, ey, 0.35f) - br.len * 0.06f
            drawPath(
                Path().apply {
                    moveTo(mx, my)
                    quadraticBezierTo(
                        lerpFloat(mx, ex, 0.55f),
                        my - br.len * 0.04f,
                        ex,
                        ey - br.len * 0.02f,
                    )
                    quadraticBezierTo(
                        lerpFloat(mx, ex, 0.60f),
                        ey + br.len * 0.05f,
                        mx + br.side * br.len * 0.08f,
                        my + br.len * 0.05f,
                    )
                    close()
                },
                color = needleLit.copy(
                    alpha = (0.22f + 0.18f * contrast) * detail.coerceIn(0.35f, 1f),
                ),
            )
        }
    }

    // Spring: sparse light-green tip shoots only (not whole-tree lime)
    if (springShoot && detail > 0.4f) {
        val shootCol = light.litColor(HeroSpringLeafShoot, 0.40f).copy(alpha = 0.55f)
        branches.filter { it.t < 0.42f }.forEachIndexed { i, br ->
            if ((i + profile) % 2 != 0) return@forEachIndexed
            val ax = trunkXAt(br.t)
            val ay = tipY + height * br.t
            val ex = ax + br.side * br.len * 0.95f
            val ey = ay + br.droop * 0.85f
            drawPath(
                Path().apply {
                    moveTo(lerpFloat(ax, ex, 0.55f), lerpFloat(ay, ey, 0.50f) - br.len * 0.04f)
                    lineTo(ex + br.side * br.len * 0.04f, ey - br.len * 0.06f)
                    lineTo(ex, ey + br.len * 0.02f)
                    close()
                },
                color = shootCol,
            )
        }
        drawPath(
            tipCluster,
            color = shootCol.copy(alpha = 0.40f),
        )
    }
}

/**
 * Irregular needle mass along a branch — jagged outer spikes, no diamonds / stars / equal triangles.
 */
internal fun DrawScope.drawPineNeedleCluster(
    ax: Float,
    ay: Float,
    ex: Float,
    ey: Float,
    halfThick: Float,
    color: Color,
    seed: Int,
    detail: Float,
) {
    val dx = ex - ax
    val dy = ey - ay
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux
    val spikes = (5 + (seed % 4) + if (detail > 0.7f) 2 else 0).coerceIn(5, 9)
    val path = Path()
    // Upper jagged edge trunk → tip
    path.moveTo(ax + px * halfThick * 0.25f, ay + py * halfThick * 0.25f)
    for (i in 1..spikes) {
        val u = i / (spikes + 1f)
        val along = u * len
        val wobble = 0.55f + ((seed * 13 + i * 17) % 7) * 0.09f
        val outward = halfThick * wobble * (0.70f + sin(u * 2.6f + seed) * 0.18f)
        // Alternate slight back-notch so edge isn't a smooth oval
        val notch = if (i % 2 == 0) 0.72f else 1f
        val sx = ((seed * 7 + i * 11) % 5) * 0.04f
        path.lineTo(
            ax + ux * along + px * outward * notch,
            ay + uy * along + py * outward * notch + halfThick * sx * 0.15f,
        )
    }
    path.lineTo(ex + px * halfThick * 0.15f, ey + py * halfThick * 0.15f)
    path.lineTo(ex - px * halfThick * 0.10f, ey - py * halfThick * 0.10f)
    // Lower jagged edge tip → trunk
    for (i in spikes downTo 1) {
        val u = i / (spikes + 1f)
        val along = u * len
        val wobble = 0.50f + ((seed * 19 + i * 23) % 7) * 0.08f
        val outward = halfThick * wobble * (0.65f + cos(u * 2.2f + seed * 0.7f) * 0.16f)
        val notch = if (i % 2 == 1) 0.68f else 1f
        path.lineTo(
            ax + ux * along - px * outward * notch,
            ay + uy * along - py * outward * notch,
        )
    }
    path.close()
    drawPath(path, color = color)
}
