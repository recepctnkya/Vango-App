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
 * Far range — pale bluish, low detail, staggered peaks (not aligned with mid/near).
 * Base descends behind opposite shore (~0.60–0.66H), not a flat horizontal cut.
 */
internal fun heroMountainFarPath(w: Float, h: Float): Path = Path().apply {
    moveTo(0f, h * 0.66f)
    cubicTo(w * 0.03f, h * 0.62f, w * 0.06f, h * 0.56f, w * 0.09f, h * 0.52f)
    quadraticBezierTo(w * 0.12f, h * 0.58f, w * 0.15f, h * 0.50f)
    cubicTo(w * 0.18f, h * 0.44f, w * 0.21f, h * 0.48f, w * 0.25f, h * 0.46f)
    lineTo(w * 0.31f, h * 0.40f)
    cubicTo(w * 0.34f, h * 0.36f, w * 0.36f, h * 0.44f, w * 0.40f, h * 0.48f)
    // Gap / lower saddle (valley opening)
    quadraticBezierTo(w * 0.45f, h * 0.54f, w * 0.50f, h * 0.50f)
    lineTo(w * 0.56f, h * 0.42f)
    cubicTo(w * 0.60f, h * 0.36f, w * 0.64f, h * 0.44f, w * 0.69f, h * 0.47f)
    quadraticBezierTo(w * 0.74f, h * 0.52f, w * 0.79f, h * 0.46f)
    cubicTo(w * 0.84f, h * 0.40f, w * 0.88f, h * 0.48f, w * 0.93f, h * 0.52f)
    lineTo(w, h * 0.56f)
    lineTo(w, h)
    lineTo(0f, h)
    close()
}

/**
 * Mid massif outline — main peak ~0.40W / 0.24H (kept), with unequal shoulders
 * and secondary peaks. Slopes mix steep + shallow; no clean triangle.
 */
internal fun heroMountainMidOutline(w: Float, h: Float): Path = Path().apply {
    moveTo(0f, h * 0.70f)
    cubicTo(w * 0.04f, h * 0.68f, w * 0.07f, h * 0.64f, w * 0.10f, h * 0.60f)
    // Left low spur
    quadraticBezierTo(w * 0.13f, h * 0.66f, w * 0.16f, h * 0.58f)
    // Left shoulder (lower than main)
    cubicTo(w * 0.19f, h * 0.50f, w * 0.22f, h * 0.52f, w * 0.25f, h * 0.46f)
    lineTo(w * 0.28f, h * 0.40f)
    // Steep rise to main peak
    cubicTo(w * 0.31f, h * 0.32f, w * 0.34f, h * 0.28f, w * 0.37f, h * 0.25f)
    quadraticBezierTo(w * 0.39f, h * 0.22f, w * 0.405f, h * 0.235f) // main summit
    // Broken right side of summit (not mirror)
    cubicTo(w * 0.42f, h * 0.26f, w * 0.435f, h * 0.32f, w * 0.455f, h * 0.38f)
    // Shallow shelf then secondary peak
    quadraticBezierTo(w * 0.48f, h * 0.44f, w * 0.51f, h * 0.42f)
    lineTo(w * 0.55f, h * 0.34f)
    cubicTo(w * 0.57f, h * 0.30f, w * 0.59f, h * 0.36f, w * 0.62f, h * 0.42f)
    // Lower right ridge with jagged short crest
    quadraticBezierTo(w * 0.66f, h * 0.48f, w * 0.70f, h * 0.46f)
    cubicTo(w * 0.74f, h * 0.44f, w * 0.78f, h * 0.52f, w * 0.83f, h * 0.55f)
    lineTo(w * 0.90f, h * 0.58f)
    cubicTo(w * 0.94f, h * 0.60f, w * 0.97f, h * 0.62f, w, h * 0.64f)
    // Base behind far shore — uneven foot
    lineTo(w, h)
    lineTo(0f, h)
    close()
}

/** Near foothills — darkest, lower, crest not aligned with mid peaks; occludes mid. */
internal fun heroMountainNearPath(w: Float, h: Float): Path = Path().apply {
    moveTo(0f, h * 0.72f)
    cubicTo(w * 0.05f, h * 0.70f, w * 0.08f, h * 0.66f, w * 0.12f, h * 0.64f)
    quadraticBezierTo(w * 0.16f, h * 0.70f, w * 0.20f, h * 0.63f)
    cubicTo(w * 0.24f, h * 0.58f, w * 0.28f, h * 0.62f, w * 0.33f, h * 0.60f)
    // Valley gap (mid mountain peeks through)
    lineTo(w * 0.40f, h * 0.66f)
    quadraticBezierTo(w * 0.46f, h * 0.70f, w * 0.52f, h * 0.64f)
    cubicTo(w * 0.58f, h * 0.58f, w * 0.64f, h * 0.62f, w * 0.70f, h * 0.66f)
    lineTo(w * 0.78f, h * 0.68f)
    cubicTo(w * 0.86f, h * 0.70f, w * 0.93f, h * 0.64f, w, h * 0.68f)
    lineTo(w, h)
    lineTo(0f, h)
    close()
}

/**
 * Broad mid-mountain slope faces — soft curved boundaries (not random triangles).
 * Some ridges end mid-slope; [lit] flips with sun direction.
 */
internal data class HeroMountainSlope(
    val lit: Boolean,
    /** Closed path as cubic-friendly control points (xFrac,yFrac). */
    val pts: List<Pair<Float, Float>>,
)

internal val HeroMidMountainSlopes: List<HeroMountainSlope> = listOf(
    // Wide lit left face from summit → foot (steep then shallow)
    HeroMountainSlope(
        lit = true,
        pts = listOf(
            0.405f to 0.235f,
            0.37f to 0.25f,
            0.31f to 0.34f,
            0.26f to 0.48f,
            0.22f to 0.58f,
            0.28f to 0.62f,
            0.36f to 0.55f,
            0.40f to 0.42f,
        ),
    ),
    // Shade right of main peak — ends mid-mountain (not to foot)
    HeroMountainSlope(
        lit = false,
        pts = listOf(
            0.405f to 0.235f,
            0.43f to 0.28f,
            0.46f to 0.38f,
            0.49f to 0.46f,
            0.45f to 0.52f,
            0.40f to 0.48f,
            0.39f to 0.36f,
        ),
    ),
    // Lit left shoulder plate
    HeroMountainSlope(
        lit = true,
        pts = listOf(
            0.16f to 0.58f,
            0.22f to 0.50f,
            0.28f to 0.40f,
            0.34f to 0.42f,
            0.32f to 0.56f,
            0.24f to 0.64f,
            0.18f to 0.64f,
        ),
    ),
    // Secondary peak lit face
    HeroMountainSlope(
        lit = true,
        pts = listOf(
            0.55f to 0.34f,
            0.51f to 0.42f,
            0.48f to 0.50f,
            0.54f to 0.56f,
            0.60f to 0.50f,
            0.60f to 0.40f,
        ),
    ),
    // Secondary shade / right spur
    HeroMountainSlope(
        lit = false,
        pts = listOf(
            0.55f to 0.34f,
            0.62f to 0.42f,
            0.70f to 0.46f,
            0.74f to 0.54f,
            0.66f to 0.58f,
            0.58f to 0.52f,
        ),
    ),
)

/** Narrow dark valleys — soft fills, no hard contour strokes. */
internal val HeroMidMountainValleys: List<List<Pair<Float, Float>>> = listOf(
    listOf(0.34f to 0.42f, 0.36f to 0.50f, 0.34f to 0.58f, 0.30f to 0.56f, 0.31f to 0.48f),
    listOf(0.48f to 0.44f, 0.50f to 0.52f, 0.47f to 0.58f, 0.44f to 0.54f, 0.45f to 0.46f),
)

/** Lower-slope olive washes — follow incline, spring/summer only. */
internal val HeroMidMountainOliveSlopes: List<List<Pair<Float, Float>>> = listOf(
    listOf(0.22f to 0.50f, 0.30f to 0.48f, 0.36f to 0.54f, 0.32f to 0.60f, 0.24f to 0.58f),
    listOf(0.40f to 0.52f, 0.48f to 0.50f, 0.54f to 0.56f, 0.50f to 0.62f, 0.42f to 0.60f),
    listOf(0.58f to 0.50f, 0.66f to 0.48f, 0.72f to 0.54f, 0.68f to 0.60f, 0.60f to 0.58f),
)

/** Residual peak snow — irregular crest/crevice patches (not a white sheet). */
internal val HeroMidMountainSnowPatches: List<List<Pair<Float, Float>>> = listOf(
    listOf(0.28f to 0.28f, 0.32f to 0.26f, 0.34f to 0.30f, 0.31f to 0.33f, 0.27f to 0.31f),
    listOf(0.46f to 0.24f, 0.51f to 0.22f, 0.54f to 0.26f, 0.50f to 0.30f, 0.45f to 0.28f),
    listOf(0.62f to 0.30f, 0.66f to 0.28f, 0.68f to 0.32f, 0.64f to 0.35f, 0.60f to 0.33f),
    listOf(0.38f to 0.32f, 0.41f to 0.30f, 0.42f to 0.34f, 0.39f to 0.36f),
)

/** Winter: wider snow fields following ridges downslope — irregular lower edge. */
internal val HeroMidMountainSnowFields: List<List<Pair<Float, Float>>> = listOf(
    listOf(
        0.24f to 0.26f, 0.30f to 0.24f, 0.36f to 0.28f, 0.34f to 0.38f,
        0.30f to 0.46f, 0.26f to 0.44f, 0.22f to 0.36f,
    ),
    listOf(
        0.42f to 0.22f, 0.50f to 0.20f, 0.56f to 0.24f, 0.54f to 0.36f,
        0.48f to 0.48f, 0.44f to 0.46f, 0.40f to 0.34f,
    ),
    listOf(
        0.58f to 0.28f, 0.66f to 0.26f, 0.72f to 0.30f, 0.70f to 0.42f,
        0.64f to 0.52f, 0.60f to 0.48f, 0.56f to 0.38f,
    ),
    listOf(
        0.34f to 0.30f, 0.40f to 0.28f, 0.42f to 0.36f, 0.38f to 0.44f, 0.34f to 0.40f,
    ),
)

/** Draw far / mid / near mountains — natural range, soft volume, base haze. */
internal fun DrawScope.drawHeroMountains(
    w: Float,
    h: Float,
    palette: HeroScenePalette,
    light: HeroSceneLight,
    farHaze: Float = 0f,
    shadowSoft: Float = 0f,
    season: SeasonState = SeasonState.Summer,
    mountainSnowCover: Float = 0f,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedShadowSoft = shadowSoft // softness lives on [light.soft]
    // Shared light contrast — soft/pale under cloud; readable in clear day
    val contrast = (0.28f + light.direct * 0.50f) * (1f - light.soft * 0.55f)
    val haze = light.haze.coerceIn(0f, 1f).coerceAtLeast(farHaze.coerceIn(0f, 1f))
    val sunFromLeft = light.dirX >= 0f
    val vegetatedSlopes = season == SeasonState.Spring ||
        season == SeasonState.Summer ||
        season == SeasonState.Autumn

    // —— Far: pale bluish, minimal detail; fog washes harder ——
    val farCol = light.litColor(
        lerpColor(palette.mountainFar, palette.skyBottom, 0.30f + haze * 0.50f),
        0.25f,
    )
    drawPath(
        heroMountainFarPath(w, h),
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to lerpColor(farCol, palette.skyBottom, 0.35f + haze * 0.40f),
                0.55f to farCol,
                1.00f to lerpColor(farCol, palette.skyBottom, 0.15f + haze * 0.25f),
            ),
            startY = h * 0.36f,
            endY = h * 0.66f,
        ),
    )

    // —— Mid: silhouette + soft slope volumes ——
    val midBase = light.shadeColor(
        lerpColor(palette.mountainMid, palette.skyBottom, haze * 0.18f),
        0.2f,
    )
    val midOutline = heroMountainMidOutline(w, h)
    drawPath(midOutline, color = midBase)

    clipPath(midOutline) {
        HeroMidMountainSlopes.forEach { slope ->
            val wantLit = if (sunFromLeft) slope.lit else !slope.lit
            val col = if (wantLit) {
                light.litColor(lerpColor(midBase, Color.White, 0.10f * contrast), 0.7f)
            } else {
                light.shadeColor(lerpColor(midBase, Color.Black, 0.14f * contrast), 0.7f)
            }
            drawPath(softMountainPoly(w, h, slope.pts), color = col.copy(alpha = 0.85f))
        }
        // Soft valleys
        val valleyCol = lerpColor(midBase, Color.Black, 0.18f * contrast)
        HeroMidMountainValleys.forEach { pts ->
            drawPath(softMountainPoly(w, h, pts), color = valleyCol.copy(alpha = 0.55f))
        }
        // Lower-slope vegetation wash — never recolor whole mass orange
        if (vegetatedSlopes) {
            val wash = when (season) {
                SeasonState.Autumn -> light.litColor(Color(0xFF6A6450), 0.30f).copy(alpha = 0.16f)
                else -> light.litColor(Color(0xFF6A7A52), 0.35f).copy(alpha = 0.22f)
            }
            HeroMidMountainOliveSlopes.forEach { pts ->
                drawPath(softMountainPoly(w, h, pts), color = wash)
            }
            // Autumn: faint earth tint on a subset of lower slopes only
            if (season == SeasonState.Autumn) {
                val earth = light.litColor(Color(0xFF7A6A50), 0.28f).copy(alpha = 0.10f)
                HeroMidMountainOliveSlopes.take(2).forEach { pts ->
                    drawPath(softMountainPoly(w, h, pts), color = earth)
                }
            }
        }
        // Crest / ridge snow — winter uses wider fields; other seasons keep crest patches
        if (mountainSnowCover > 0.05f) {
            val snowA = (0.35f + mountainSnowCover * 0.50f).coerceIn(0.20f, 0.88f)
            val litSnow = light.litColor(HeroSnowLit, 0.45f).copy(alpha = snowA * 0.90f)
            val shadeSnow = light.shadeColor(HeroSnowHollow, 0.40f).copy(alpha = snowA * 0.70f)
            if (season == SeasonState.Winter && mountainSnowCover > 0.40f) {
                HeroMidMountainSnowFields.forEachIndexed { i, pts ->
                    drawPath(
                        softMountainPoly(w, h, pts),
                        color = if (i % 2 == 0) litSnow else shadeSnow,
                    )
                }
                // Dark rock ribs peeking through
                HeroMidMountainValleys.forEach { pts ->
                    drawPath(
                        softMountainPoly(w, h, pts),
                        color = midBase.copy(alpha = 0.45f),
                    )
                }
            } else {
                HeroMidMountainSnowPatches.forEach { pts ->
                    drawPath(softMountainPoly(w, h, pts), color = litSnow)
                }
            }
        }
        // Gentle vertical depth wash (not facet mosaic)
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to Color.White.copy(alpha = 0.06f * contrast),
                0.35f to Color.Transparent,
                0.75f to Color.Black.copy(alpha = 0.08f * contrast),
                1.00f to Color.Black.copy(alpha = 0.12f * contrast),
            ),
            topLeft = Offset(0f, h * 0.22f),
            size = Size(w, h * 0.48f),
        )
    }

    // —— Near: more readable; fog barely reduces ——
    val nearHaze = haze * 0.22f
    val nearCol = light.shadeColor(
        lerpColor(palette.mountainNear, palette.skyBottom, nearHaze),
        0.25f,
    )
    drawPath(
        heroMountainNearPath(w, h),
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to lerpColor(nearCol, palette.mountainMid, 0.28f),
                0.45f to nearCol,
                1.00f to lerpColor(nearCol, Color.Black, 0.08f),
            ),
            startY = h * 0.58f,
            endY = h * 0.78f,
        ),
    )
    // Soft near slope hint (one lit/shade pair, low contrast)
    clipPath(heroMountainNearPath(w, h)) {
        val nearLit = light.litColor(lerpColor(nearCol, Color.White, 0.08f * contrast), 0.65f)
        val nearShade = light.shadeColor(lerpColor(nearCol, Color.Black, 0.10f * contrast), 0.65f)
        drawPath(
            softMountainPoly(
                w, h,
                listOf(0.12f to 0.64f, 0.20f to 0.60f, 0.28f to 0.62f, 0.24f to 0.70f, 0.14f to 0.70f),
            ),
            color = (if (sunFromLeft) nearLit else nearShade).copy(alpha = 0.55f),
        )
        drawPath(
            softMountainPoly(
                w, h,
                listOf(0.52f to 0.64f, 0.62f to 0.60f, 0.72f to 0.66f, 0.66f to 0.72f, 0.54f to 0.70f),
            ),
            color = (if (sunFromLeft) nearShade else nearLit).copy(alpha = 0.50f),
        )
    }

    // Atmospheric haze at bases (behind far shore band)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                palette.skyBottom.copy(alpha = 0.10f + haze * 0.16f),
                palette.skyBottom.copy(alpha = 0.20f + haze * 0.26f),
            ),
            startY = h * 0.52f,
            endY = h * 0.66f,
        ),
        topLeft = Offset(0f, h * 0.52f),
        size = Size(w, h * 0.14f),
    )
    // Fog veils far band more than near
    if (haze > 0.05f) {
        drawRect(
            brush = Brush.verticalGradient(
                0.28f to Color.Transparent,
                0.42f to Color(0xFFD0D8E0).copy(alpha = 0.26f * haze),
                0.52f to Color(0xFFC0CAD4).copy(alpha = 0.32f * haze),
                0.62f to Color.Transparent,
            ),
        )
    }
}

/** Soft closed polygon via quadratic midpoints — avoids sharp triangle mosaic. */
internal fun softMountainPoly(w: Float, h: Float, pts: List<Pair<Float, Float>>): Path =
    Path().apply {
        if (pts.size < 3) return@apply
        val mapped = pts.map { Offset(w * it.first, h * it.second) }
        moveTo(mapped[0].x, mapped[0].y)
        for (i in mapped.indices) {
            val a = mapped[i]
            val b = mapped[(i + 1) % mapped.size]
            quadraticBezierTo(a.x, a.y, (a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        }
        close()
    }
