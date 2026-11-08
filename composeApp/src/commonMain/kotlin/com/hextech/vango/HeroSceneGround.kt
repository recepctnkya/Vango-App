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
 * Soft soil blotch — drawn as irregular path, not a repeated oval.
 * [lite] true = lighter patch; false = darker.
 */
internal data class HeroSoilBlotch(
    val x: Float,
    val y: Float,
    val rx: Float,
    val ry: Float,
    val lite: Boolean,
    val seed: Int,
)

internal val HeroSoilBlotches: List<HeroSoilBlotch> = listOf(
    // Corner detail — larger, varied
    HeroSoilBlotch(0.06f, 0.92f, 0.08f, 0.035f, false, 0),
    HeroSoilBlotch(0.12f, 0.96f, 0.06f, 0.028f, true, 1),
    HeroSoilBlotch(0.90f, 0.93f, 0.07f, 0.032f, false, 2),
    HeroSoilBlotch(0.96f, 0.97f, 0.05f, 0.024f, true, 3),
    // Mid park — sparse, low contrast (calm under vehicle)
    HeroSoilBlotch(0.42f, 0.86f, 0.05f, 0.018f, true, 4),
    HeroSoilBlotch(0.68f, 0.88f, 0.045f, 0.016f, false, 5),
    HeroSoilBlotch(0.55f, 0.93f, 0.04f, 0.014f, true, 6),
    // Near shore dry edge
    HeroSoilBlotch(0.20f, 0.82f, 0.055f, 0.016f, false, 7),
    HeroSoilBlotch(0.78f, 0.83f, 0.05f, 0.015f, true, 8),
)

/** Gravel: tone 0=grey, 1=stone, 2=pale brown. Fixed positions. */
internal data class HeroParkPebble(
    val x: Float,
    val y: Float,
    val size: Float,
    val depth: Float,
    val tone: Int,
)

internal val HeroParkPebbles: List<HeroParkPebble> = listOf(
    // Near shore — tiny, low contrast clusters + bare gaps
    HeroParkPebble(0.04f, 0.808f, 0.0030f, 0.10f, 0),
    HeroParkPebble(0.052f, 0.814f, 0.0026f, 0.12f, 1),
    HeroParkPebble(0.061f, 0.806f, 0.0028f, 0.11f, 2),
    HeroParkPebble(0.18f, 0.800f, 0.0024f, 0.08f, 0),
    HeroParkPebble(0.192f, 0.808f, 0.0022f, 0.09f, 1),
    HeroParkPebble(0.46f, 0.778f, 0.0025f, 0.07f, 2),
    HeroParkPebble(0.472f, 0.785f, 0.0020f, 0.08f, 0),
    HeroParkPebble(0.86f, 0.812f, 0.0028f, 0.12f, 1),
    HeroParkPebble(0.875f, 0.818f, 0.0024f, 0.13f, 0),
    HeroParkPebble(0.888f, 0.808f, 0.0022f, 0.11f, 2),
    // Left mid cluster
    HeroParkPebble(0.03f, 0.86f, 0.0045f, 0.42f, 1),
    HeroParkPebble(0.042f, 0.868f, 0.0038f, 0.45f, 0),
    HeroParkPebble(0.055f, 0.855f, 0.0040f, 0.40f, 2),
    HeroParkPebble(0.068f, 0.872f, 0.0032f, 0.48f, 1),
    // Right mid cluster
    HeroParkPebble(0.93f, 0.858f, 0.0048f, 0.48f, 0),
    HeroParkPebble(0.945f, 0.866f, 0.0040f, 0.50f, 2),
    HeroParkPebble(0.958f, 0.852f, 0.0035f, 0.46f, 1),
    // Foreground corners — larger, clearer
    HeroParkPebble(0.025f, 0.935f, 0.0085f, 0.90f, 1),
    HeroParkPebble(0.048f, 0.948f, 0.0070f, 0.92f, 0),
    HeroParkPebble(0.072f, 0.958f, 0.0060f, 0.88f, 2),
    HeroParkPebble(0.015f, 0.962f, 0.0055f, 0.94f, 0),
    HeroParkPebble(0.92f, 0.940f, 0.0090f, 0.90f, 2),
    HeroParkPebble(0.945f, 0.952f, 0.0075f, 0.93f, 1),
    HeroParkPebble(0.968f, 0.962f, 0.0065f, 0.95f, 0),
    HeroParkPebble(0.910f, 0.958f, 0.0050f, 0.88f, 1),
)

internal data class HeroRockSpec(
    val x: Float,
    val y: Float,
    val sizeW: Float,
    val depth: Float,
    val verts: List<Pair<Float, Float>>,
    val behindGrass: Boolean = false,
)

internal data class HeroRockGroup(val rocks: List<HeroRockSpec>)

internal val HeroRockGroups: List<HeroRockGroup> = listOf(
    // Bottom-left: 4 distinct stones (2–3.5% W); some clipped at card bottom
    HeroRockGroup(
        listOf(
            HeroRockSpec(
                0.030f, 0.945f, 0.034f, 0.94f,
                listOf(0.95f to -0.35f, 0.70f to 0.55f, 0.15f to 0.95f, -0.55f to 0.80f, -0.95f to 0.10f, -0.45f to -0.75f, 0.25f to -0.95f),
                behindGrass = true,
            ),
            HeroRockSpec(
                0.058f, 0.968f, 0.026f, 0.96f,
                listOf(0.85f to -0.10f, 0.95f to 0.50f, 0.20f to 0.95f, -0.75f to 0.70f, -0.90f to -0.20f, 0.05f to -0.90f),
            ),
            HeroRockSpec(
                0.016f, 0.978f, 0.022f, 0.97f,
                listOf(0.70f to -0.45f, 0.95f to 0.25f, 0.40f to 0.90f, -0.55f to 0.85f, -0.95f to 0.05f, -0.15f to -0.85f),
            ),
            HeroRockSpec(
                0.048f, 0.928f, 0.020f, 0.88f,
                listOf(0.80f to -0.55f, 0.90f to 0.35f, 0.10f to 0.90f, -0.70f to 0.60f, -0.75f to -0.40f, 0.15f to -0.95f),
                behindGrass = true,
            ),
        ),
    ),
    // Bottom-right: 3 stones — different layout from left
    HeroRockGroup(
        listOf(
            HeroRockSpec(
                0.955f, 0.950f, 0.036f, 0.93f,
                listOf(0.90f to -0.20f, 0.55f to 0.75f, -0.20f to 0.95f, -0.95f to 0.35f, -0.60f to -0.70f, 0.35f to -0.95f),
                behindGrass = true,
            ),
            HeroRockSpec(
                0.925f, 0.972f, 0.028f, 0.96f,
                listOf(0.75f to 0.05f, 0.95f to 0.55f, 0.05f to 0.95f, -0.85f to 0.45f, -0.55f to -0.65f, 0.25f to -0.85f),
            ),
            HeroRockSpec(
                0.978f, 0.965f, 0.024f, 0.95f,
                listOf(0.65f to -0.60f, 0.95f to -0.05f, 0.70f to 0.80f, -0.15f to 0.95f, -0.95f to 0.20f, -0.40f to -0.75f),
            ),
        ),
    ),
)

internal data class HeroGrassClump(
    val x: Float,
    val y: Float,
    val height: Float,
    val depth: Float,
    val blades: List<Pair<Float, Float>>,
    val sway: Boolean = false,
    val reed: Boolean = false,
    /** In summer, draw with dry straw tones (park edges / corners). */
    val dryInSummer: Boolean = false,
    /** Skip outside summer (extra dry edge clumps). */
    val summerOnly: Boolean = false,
)

internal val HeroGrassClumps: List<HeroGrassClump> = listOf(
    // Shore reeds — stay greener near water
    HeroGrassClump(
        0.08f, 0.800f, 0.028f, 0.14f,
        listOf(-0.45f to 0.9f, -0.2f to 1.0f, 0.0f to 1.05f, 0.2f to 0.95f, 0.4f to 0.8f),
        reed = true,
    ),
    HeroGrassClump(
        0.88f, 0.805f, 0.026f, 0.15f,
        listOf(-0.4f to 0.85f, -0.1f to 0.95f, 0.15f to 1.0f, 0.35f to 0.88f, 0.45f to 0.7f),
        sway = true,
    ),
    // Left corner — mix green + dry (summer)
    HeroGrassClump(
        0.022f, 0.952f, 0.048f, 0.90f,
        listOf(-0.7f to 0.85f, -0.5f to 1.0f, -0.35f to 1.15f, -0.15f to 1.05f, 0.0f to 1.05f, 0.15f to 0.95f, 0.3f to 0.9f),
        sway = true,
        dryInSummer = true,
    ),
    HeroGrassClump(
        0.040f, 0.975f, 0.036f, 0.97f,
        listOf(-0.5f to 0.9f, -0.3f to 1.05f, -0.1f to 1.2f, 0.1f to 1.1f, 0.25f to 1.0f, 0.4f to 0.85f, 0.5f to 0.75f),
    ),
    HeroGrassClump(
        0.062f, 0.945f, 0.040f, 0.86f,
        listOf(-0.55f to 0.8f, -0.35f to 0.95f, -0.15f to 1.1f, 0.05f to 1.0f, 0.2f to 0.95f, 0.35f to 0.8f, 0.45f to 0.7f),
        sway = true,
        dryInSummer = true,
    ),
    HeroGrassClump(
        0.078f, 0.965f, 0.030f, 0.94f,
        listOf(-0.4f to 0.85f, -0.2f to 0.95f, 0.05f to 1.05f, 0.25f to 0.95f, 0.4f to 0.8f),
        dryInSummer = true,
    ),
    // Right corner
    HeroGrassClump(
        0.930f, 0.955f, 0.044f, 0.90f,
        listOf(-0.6f to 0.95f, -0.4f to 1.1f, -0.2f to 1.2f, 0.0f to 1.1f, 0.15f to 1.05f, 0.35f to 0.95f, 0.45f to 0.85f, 0.65f to 0.65f),
        sway = true,
        dryInSummer = true,
    ),
    HeroGrassClump(
        0.955f, 0.978f, 0.034f, 0.97f,
        listOf(-0.45f to 0.9f, -0.25f to 1.05f, 0.0f to 1.15f, 0.2f to 1.05f, 0.35f to 0.95f, 0.5f to 0.8f),
    ),
    HeroGrassClump(
        0.970f, 0.948f, 0.038f, 0.88f,
        listOf(-0.5f to 0.85f, -0.3f to 1.0f, -0.1f to 1.1f, 0.15f to 1.0f, 0.3f to 0.9f, 0.45f to 0.8f, 0.55f to 0.7f),
        dryInSummer = true,
    ),
    // Extra dry edge clumps — summer only
    HeroGrassClump(
        0.095f, 0.938f, 0.032f, 0.82f,
        listOf(-0.55f to 0.9f, -0.25f to 1.15f, 0.05f to 1.05f, 0.3f to 0.95f, 0.5f to 0.75f, 0.65f to 0.6f),
        dryInSummer = true,
        summerOnly = true,
    ),
    HeroGrassClump(
        0.910f, 0.942f, 0.030f, 0.80f,
        listOf(-0.5f to 0.85f, -0.2f to 1.1f, 0.1f to 1.0f, 0.35f to 0.9f, 0.55f to 0.7f),
        dryInSummer = true,
        summerOnly = true,
    ),
)

/** Corner flower groups — avoid phone/link band (x 0.12–0.55). */
internal data class HeroFlowerGroup(
    val x: Float,
    val y: Float,
    val count: Int,
    val seed: Int,
)

internal val HeroSpringFlowerGroups: List<HeroFlowerGroup> = listOf(
    HeroFlowerGroup(0.035f, 0.958f, 3, 1),
    HeroFlowerGroup(0.068f, 0.972f, 2, 2),
    HeroFlowerGroup(0.048f, 0.940f, 4, 3),
    HeroFlowerGroup(0.940f, 0.960f, 3, 4),
    HeroFlowerGroup(0.968f, 0.970f, 2, 5),
)

/** Summer: sparse leftovers only. */
internal val HeroSummerFlowerGroups: List<HeroFlowerGroup> = listOf(
    HeroFlowerGroup(0.038f, 0.962f, 2, 21),
    HeroFlowerGroup(0.955f, 0.965f, 2, 22),
)

/**
 * Ground litter under deciduous crowns + shore edges — not a uniform carpet.
 * Avoid mid park (vehicle pad ~0.35–0.72). [depth] 0 far → 1 near.
 * tone: 0 gold, 1 copper, 2 brown.
 */
internal data class HeroFallenLeaf(
    val x: Float,
    val y: Float,
    val size: Float,
    val depth: Float,
    val rotDeg: Float,
    val tone: Int,
    val seed: Int,
)

internal val HeroAutumnFallenLeaves: List<HeroFallenLeaf> = listOf(
    // Under left deciduous (~0.18)
    HeroFallenLeaf(0.14f, 0.78f, 0.0048f, 0.35f, -28f, 0, 1),
    HeroFallenLeaf(0.17f, 0.81f, 0.0055f, 0.42f, 40f, 1, 2),
    HeroFallenLeaf(0.20f, 0.79f, 0.0042f, 0.38f, 12f, 2, 3),
    HeroFallenLeaf(0.12f, 0.84f, 0.0060f, 0.55f, -55f, 0, 4),
    HeroFallenLeaf(0.16f, 0.86f, 0.0050f, 0.58f, 22f, 1, 5),
    HeroFallenLeaf(0.22f, 0.83f, 0.0045f, 0.48f, -10f, 0, 6),
    HeroFallenLeaf(0.15f, 0.90f, 0.0070f, 0.78f, 35f, 1, 7),
    HeroFallenLeaf(0.19f, 0.92f, 0.0062f, 0.82f, -42f, 2, 8),
    HeroFallenLeaf(0.11f, 0.88f, 0.0058f, 0.72f, 8f, 0, 9),
    // Under right deciduous (~0.78)
    HeroFallenLeaf(0.74f, 0.77f, 0.0045f, 0.32f, 25f, 1, 10),
    HeroFallenLeaf(0.77f, 0.80f, 0.0052f, 0.40f, -35f, 0, 11),
    HeroFallenLeaf(0.80f, 0.78f, 0.0040f, 0.36f, 50f, 2, 12),
    HeroFallenLeaf(0.72f, 0.83f, 0.0058f, 0.52f, -18f, 0, 13),
    HeroFallenLeaf(0.76f, 0.85f, 0.0065f, 0.60f, 30f, 1, 14),
    HeroFallenLeaf(0.81f, 0.82f, 0.0048f, 0.48f, -60f, 2, 15),
    HeroFallenLeaf(0.73f, 0.89f, 0.0072f, 0.80f, 15f, 0, 16),
    HeroFallenLeaf(0.78f, 0.91f, 0.0068f, 0.85f, -25f, 1, 17),
    // Shore edge sparse
    HeroFallenLeaf(0.08f, 0.82f, 0.0038f, 0.22f, 45f, 1, 18),
    HeroFallenLeaf(0.28f, 0.80f, 0.0035f, 0.18f, -30f, 0, 19),
    HeroFallenLeaf(0.88f, 0.81f, 0.0036f, 0.20f, 20f, 2, 20),
    // Near corner accent (large)
    HeroFallenLeaf(0.06f, 0.94f, 0.0080f, 0.92f, -40f, 0, 21),
    HeroFallenLeaf(0.09f, 0.96f, 0.0070f, 0.95f, 28f, 1, 22),
    HeroFallenLeaf(0.94f, 0.93f, 0.0075f, 0.90f, -15f, 2, 23),
    HeroFallenLeaf(0.97f, 0.96f, 0.0065f, 0.94f, 55f, 0, 24),
    // Overlaps near left crown
    HeroFallenLeaf(0.155f, 0.875f, 0.0055f, 0.70f, 5f, 1, 25),
    HeroFallenLeaf(0.165f, 0.868f, 0.0048f, 0.68f, -70f, 0, 26),
)

/** Irregular spring bushes (united leaf masses, not equal round clouds). */
internal data class HeroSpringBush(val x: Float, val y: Float, val w: Float, val h: Float, val seed: Int)

internal val HeroSpringBushes: List<HeroSpringBush> = listOf(
    HeroSpringBush(0.055f, 0.930f, 0.038f, 0.028f, 7),
    HeroSpringBush(0.945f, 0.928f, 0.042f, 0.030f, 11),
)

/**
 * Dry gravel park — detail in front corners; calm under vehicle.
 * No parallel tire stripes; no equal round bushes.
 */
internal fun DrawScope.drawHeroParkShore(
    w: Float,
    h: Float,
    palette: HeroScenePalette,
    waterT: Float,
    animateMotion: Boolean,
    light: HeroSceneLight,
    season: SeasonState = SeasonState.Summer,
    seasonProgress: Float = 0.35f,
    wind: WindState = WindState.Calm,
    timeSec: Float = 0f,
    snowCover: Float = 0f,
    foliageAmount: Float = 1f,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedPalette = palette
    // Shared light: desaturate sand under cloud (soft), dim at night — no independent period branch
    val satCut = light.soft * 0.35f
    fun groundTone(day: Color, night: Color): Color {
        val n = lerpColor(day, night, light.nightAmount)
        val cooled = lerpColor(n, Color(0xFF9A9A90), satCut)
        return light.litColor(cooled, 0.35f)
    }
    val winterAmt = winterAmountFromProgress(seasonProgress)
    val winterWash = winterAmt * 0.22f
    // Warm dry ground rises through summer→autumn continuously
    val warmDryT = when {
        seasonProgress < 0.25f -> 0f
        seasonProgress < 0.50f -> heroSmoothstep((seasonProgress - 0.25f) / 0.25f)
        seasonProgress < 0.75f -> 1f
        else -> 1f - winterAmt
    }.coerceIn(0f, 1f)
    val dayBase = lerpColor(Color(0xFFB0A894), Color(0xFFB8AA8C), warmDryT)
    val dayDark = lerpColor(Color(0xFF8E8678), Color(0xFF9A8E72), warmDryT)
    val dayLite = lerpColor(Color(0xFFC4BBA8), Color(0xFFC9BFA8), warmDryT)
    val baseBeige = lerpColor(groundTone(dayBase, Color(0xFF5A5850)), Color(0xFFC8C4B8), winterWash)
    val soilDark = light.shadeColor(
        lerpColor(groundTone(dayDark, Color(0xFF45433C)), Color(0xFF8A8680), winterWash),
        0.4f,
    )
    val soilLite = lerpColor(groundTone(dayLite, Color(0xFF6A675E)), Color(0xFFD0CCC0), winterWash)
    val gravelGrey = groundTone(
        lerpColor(Color(0xFFA8A89C), Color(0xFFB0A890), warmDryT),
        Color(0xFF6E7068),
    )
    val gravelStone = groundTone(
        lerpColor(Color(0xFFB5AFA0), Color(0xFFC0B49C), warmDryT),
        Color(0xFF7A786E),
    )
    val gravelBrown = groundTone(
        lerpColor(Color(0xFFA89880), Color(0xFFB0A080), warmDryT),
        Color(0xFF6A6054),
    )

    val deepSnow = snowCover >= 0.18f
    // 1) Flat-ish natural base (soft depth, no long parallel bands)
    drawPath(
        heroParkGroundPath(w, h),
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to lerpColor(baseBeige, soilLite, 0.25f),
                0.45f to baseBeige,
                0.78f to lerpColor(baseBeige, soilDark, 0.22f),
                1.00f to lerpColor(soilDark, Color(0xFF5A564C), 0.15f),
            ),
            startY = h * 0.74f,
            endY = h,
        ),
    )
    // Continuous snow pack early so rocks/grass can poke through afterward
    if (deepSnow) {
        drawHeroSnowPack(w, h, snowCover, light)
    }

    // Irregular soft soil blotches (not repeated ovals / horizontal stripes)
    HeroSoilBlotches.forEach { b ->
        val cx = w * b.x
        val cy = h * b.y
        val rx = w * b.rx
        val ry = h * b.ry
        val col = if (b.lite) soilLite else soilDark
        val a = if (b.lite) 0.16f else 0.20f
        drawPath(
            irregularSoilBlotch(cx, cy, rx, ry, b.seed),
            color = col.copy(alpha = a),
        )
    }

    // 2) Gravel clusters — fixed; shore small/pale, foreground larger
    HeroParkPebbles.forEach { p ->
        val s = w * p.size * (0.75f + p.depth * 0.55f)
        val a = (0.14f + p.depth * 0.34f).coerceIn(0.12f, 0.52f)
        val col = when (p.tone % 3) {
            1 -> gravelStone
            2 -> gravelBrown
            else -> gravelGrey
        }
        // Slightly irregular chip (not perfect circle)
        val aspect = (0.48f + p.seedish() * 0.22f).coerceIn(0.45f, 0.72f)
        drawOval(
            color = col.copy(alpha = a),
            topLeft = Offset(w * p.x - s * 0.5f, h * p.y - s * aspect * 0.5f),
            size = Size(s, s * aspect),
        )
    }

    val plantSat = (1f - light.soft * 0.40f).coerceIn(0.55f, 1f)
    val (gOlive, gMid, gTip) = grassColorsFromProgress(seasonProgress)
    val olive = light.shadeColor(
        lerpColor(lerpColor(gOlive, Color(0xFF6A7060), 1f - plantSat), Color(0xFF2A3830), light.nightAmount),
        0.35f,
    )
    val midGreen = light.litColor(
        lerpColor(lerpColor(gMid, Color(0xFF6E7460), 1f - plantSat), Color(0xFF3A4A3C), light.nightAmount),
        0.4f,
    )
    val tipGreen = light.litColor(
        lerpColor(lerpColor(gTip, Color(0xFF8A9078), 1f - plantSat), Color(0xFF5A6A52), light.nightAmount),
        0.45f,
    )
    val rockLite = light.litColor(lerpColor(Color(0xFFC9C4B4), Color(0xFF8A9088), light.nightAmount), 0.4f)
    val rockDark = light.shadeColor(lerpColor(Color(0xFF6A665A), Color(0xFF4A5048), light.nightAmount), 0.45f)
    // Soft/pale under cloud; contact shadows stay readable via separate rock ovals
    val shadowCol = Color.Black.copy(
        alpha = (0.10f + 0.06f * light.nightAmount) * (0.40f + 0.60f * light.direct) * (1f - light.soft * 0.45f),
    )
    val shadowLen = (1.15f - light.elev * 0.55f).coerceIn(0.55f, 1.2f)
    val shadowDx = light.dirX * w * 0.010f * shadowLen

    fun rockPath(cx: Float, cy: Float, rw: Float, rh: Float, verts: List<Pair<Float, Float>>, yScale: Float = 1f): Path =
        Path().apply {
            val pts = verts.map { (vx, vy) -> Offset(cx + vx * rw * 0.5f, cy + vy * rh * 0.5f * yScale) }
            if (pts.isEmpty()) return@apply
            moveTo(pts[0].x, pts[0].y)
            for (i in pts.indices) {
                val a = pts[i]
                val b = pts[(i + 1) % pts.size]
                quadraticBezierTo(a.x, a.y, (a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
            }
            close()
        }

    val masterSec = HeroLakeMasterMs / 1000f

    fun drawGrass(clump: HeroGrassClump, gi: Int) {
        if (clump.summerOnly && season != SeasonState.Summer) return
        // Winter: shorter, paler — never erase clumps entirely
        val seasonH = when (season) {
            SeasonState.Winter -> if (deepSnow) 0.32f else 0.55f
            SeasonState.Autumn -> 0.85f
            else -> 1f
        }
        val dry = !clump.reed && (
            (season == SeasonState.Summer && clump.dryInSummer) ||
                (season == SeasonState.Autumn && (clump.dryInSummer || (gi % 2 == 0)))
            )
        val (bOlive, bMid, bTip) = if (dry) {
            val (d0, d1, d2) = if (season == SeasonState.Autumn) {
                seasonAutumnDryGrassColors()
            } else {
                seasonDryGrassColors()
            }
            Triple(
                light.shadeColor(lerpColor(d0, Color(0xFF6A6050), 1f - plantSat), 0.30f),
                light.litColor(lerpColor(d1, Color(0xFF8A7860), 1f - plantSat), 0.40f),
                light.litColor(lerpColor(d2, Color(0xFFA89870), 1f - plantSat), 0.45f),
            )
        } else {
            Triple(olive, midGreen, tipGreen)
        }
        val base = Offset(w * clump.x, h * clump.y)
        val ht = h * clump.height * (0.55f + clump.depth * 0.55f) * seasonH *
            if (dry) 0.92f else 1f
        // Dry blades: thinner, more curved, varied lengths
        val stroke = ((if (clump.reed) 0.85f else if (dry) 0.52f else 0.7f) + clump.depth * 0.55f).dp.toPx()
        val a = (0.55f + clump.depth * 0.40f).coerceIn(0.5f, 1f) *
            if (season == SeasonState.Winter) 0.72f else 1f
        // Grass sway only with wind (ADIM 10); no calm idle flutter
        val sway = if (animateMotion && wind.speedNorm >= 0.14f) {
            windSwayOffsetX(
                tFromBase = 1f,
                timeSec = if (timeSec > 0f) timeSec else waterT * masterSec,
                phase = gi * 0.73f + 0.31f, // offset vs tree phases
                wind = wind,
                scalePx = w * 0.55f,
            ) * 0.55f
        } else {
            0f
        }
        // Soft dark soil under clump base
        if (season == SeasonState.Spring || season == SeasonState.Summer) {
            drawOval(
                soilDark.copy(alpha = if (dry) 0.12f else 0.18f),
                topLeft = Offset(base.x - ht * 0.35f, base.y - ht * 0.06f),
                size = Size(ht * 0.70f, ht * 0.14f),
            )
        }
        clump.blades.forEachIndexed { bi, (lean, lenFrac) ->
            val leanMul = if (dry) 1.0f + (bi % 3) * 0.12f else 1f
            val lenMul = if (dry) 0.78f + (bi % 4) * 0.09f else 1f
            val tipX = base.x + lean * leanMul * ht * 0.55f +
                if (bi == clump.blades.lastIndex) sway else sway * 0.35f
            val tipY = base.y - ht * lenFrac * lenMul
            val curve = lean * ht * (0.10f + (bi % 3) * 0.03f) * if (dry) 1.35f else 1f
            val mid = Offset(
                (base.x + tipX) * 0.5f + curve,
                (base.y + tipY) * 0.5f,
            )
            drawLine(bOlive.copy(alpha = a), base, mid, stroke * 1.05f, StrokeCap.Round)
            drawLine(bMid.copy(alpha = a * 0.95f), mid, Offset((mid.x + tipX) * 0.5f, (mid.y + tipY) * 0.5f), stroke * 0.9f, StrokeCap.Round)
            drawLine(bTip.copy(alpha = a * 0.85f), Offset((mid.x + tipX) * 0.5f, (mid.y + tipY) * 0.5f), Offset(tipX, tipY), stroke * 0.7f, StrokeCap.Round)
        }
    }

    // Contact shadows under rocks
    HeroRockGroups.forEach { g ->
        g.rocks.forEach { rock ->
            val s = w * rock.sizeW
            drawOval(
                color = shadowCol.copy(alpha = shadowCol.alpha * (0.55f + rock.depth * 0.35f)),
                topLeft = Offset(w * rock.x - s * 0.42f + shadowDx * 0.4f, h * rock.y + s * 0.18f),
                size = Size(s * 0.95f, s * 0.16f),
            )
        }
    }

    // Behind-grass rocks → grass behind → remaining rocks → front grass
    fun drawRock(rock: HeroRockSpec) {
        val rw = w * rock.sizeW
        val rh = rw * 0.56f
        val cx = w * rock.x
        val cy = h * rock.y
        val side = -light.dirX.coerceIn(-1f, 1f)
        // Short contact shadow
        drawOval(
            Color.Black.copy(alpha = if (deepSnow) 0.14f else 0.10f),
            topLeft = Offset(cx - rw * 0.42f, cy + rh * 0.28f),
            size = Size(rw * 0.90f, rh * 0.22f),
        )
        // Darker side face (stays readable under snow)
        drawPath(
            rockPath(cx + side * rw * 0.10f, cy + rh * 0.08f, rw * 0.90f, rh * 0.95f, rock.verts, 1.02f),
            color = if (deepSnow) Color(0xFF5A5E64) else rockDark,
        )
        // Lit irregular top — snow cap when accumulated
        if (deepSnow) {
            drawPath(
                rockPath(cx - side * rw * 0.04f, cy - rh * 0.18f, rw * 0.78f, rh * 0.55f, rock.verts),
                color = light.litColor(HeroSnowLit, 0.45f).copy(alpha = 0.88f),
            )
            // Small drift against rock base
            drawOval(
                HeroSnowMain.copy(alpha = 0.70f),
                topLeft = Offset(cx - rw * 0.55f, cy + rh * 0.12f),
                size = Size(rw * 1.15f, rh * 0.28f),
            )
        } else {
            drawPath(
                rockPath(cx - side * rw * 0.04f, cy - rh * 0.14f, rw * 0.82f, rh * 0.68f, rock.verts),
                color = rockLite.copy(alpha = 0.70f + 0.22f * light.direct),
            )
        }
    }

    HeroRockGroups.forEach { g ->
        g.rocks.filter { it.behindGrass }.forEach { drawRock(it) }
    }
    HeroGrassClumps.forEachIndexed { gi, c -> if (c.depth < 0.88f && c.depth >= 0.4f) drawGrass(c, gi) }
    HeroRockGroups.forEach { g ->
        g.rocks.filter { !it.behindGrass }.forEach { drawRock(it) }
    }
    // Shore / sparse first, then dense corner blades in front of rocks
    HeroGrassClumps.forEachIndexed { gi, c -> if (c.depth < 0.4f) drawGrass(c, gi) }
    HeroGrassClumps.forEachIndexed { gi, c -> if (c.depth >= 0.88f) drawGrass(c, gi) }

    if (season == SeasonState.Spring || season == SeasonState.Summer || season == SeasonState.Autumn) {
        // Irregular bushes — green spring/summer; muted olive-brown in autumn
        val bushScale = when (season) {
            SeasonState.Summer -> 1.08f
            SeasonState.Autumn -> 0.92f
            else -> 1f
        }
        val bushFill = if (season == SeasonState.Autumn) {
            light.shadeColor(lerpColor(HeroAutumnGrassOlive, HeroAutumnLeafCopper, 0.25f), 0.35f)
                .copy(alpha = 0.82f)
        } else {
            olive.copy(alpha = 0.88f)
        }
        val bushEdge = if (season == SeasonState.Autumn) {
            tipGreen.copy(alpha = 0.28f)
        } else {
            tipGreen.copy(alpha = 0.35f)
        }
        HeroSpringBushes.forEach { b ->
            val bx = w * b.x
            val by = h * b.y
            val bw = w * b.w * bushScale
            val bh = h * b.h * bushScale
            val bushPath = Path().apply {
                val n = 6
                for (i in 0 until n) {
                    val ang = (i / n.toFloat()) * (PI * 2.0).toFloat()
                    val spike = if (i % 2 == 0) 1.12f else 0.72f
                    val wob = 0.75f + ((b.seed * 5 + i * 9) % 4) * 0.08f
                    val px = bx + cos(ang) * bw * 0.5f * spike * wob
                    val py = by + sin(ang) * bh * 0.5f * spike * 0.9f
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(bushPath, color = bushFill)
            drawPath(
                bushPath,
                color = bushEdge,
                style = Stroke(width = (bw * 0.08f).coerceAtLeast(1f)),
            )
        }
        // Flower density from continuous seasonProgress (same fixed group seeds)
        val flowerDens = flowerDensityFromProgress(seasonProgress)
        if (flowerDens > 0.04f) {
            val flowerGroups = if (flowerDens > 0.45f) HeroSpringFlowerGroups else HeroSummerFlowerGroups
            flowerGroups.forEach { g ->
                val gx = w * g.x
                val gy = h * g.y
                val maxN = (g.count * flowerDens).toInt().coerceIn(1, g.count)
                for (i in 0 until maxN) {
                    val ox = ((g.seed * 13 + i * 19) % 7 - 3) * w * 0.0045f
                    val oy = ((g.seed * 7 + i * 11) % 5 - 2) * h * 0.0035f
                    val ang = ((g.seed + i * 40) % 50) - 25f
                    val scale = (0.75f + flowerDens * 0.35f)
                    drawSpringDaisy(
                        center = Offset(gx + ox, gy + oy),
                        petalR = w * (0.0055f + (i % 3) * 0.0012f) * scale,
                        tiltDeg = ang,
                        stemH = h * (0.012f + (i % 2) * 0.004f) * scale,
                        olive = olive,
                        midGreen = midGreen,
                    )
                }
            }
        }
    }

    // Autumn ground litter — shaped leaves under crowns / shore (park pad stays open)
    if (season == SeasonState.Autumn) {
        drawAutumnFallenLeaves(w, h, light, foliageAmount)
    }

    // Light dusting only when cover is thin (deep pack already drawn above)
    if (!deepSnow && (season != SeasonState.Spring || snowCover > 0.08f)) {
        drawHeroSnowDusting(w, h, snowCover, light)
    }
}

/** Fixed autumn litter — recognizable leaf paths, depth-scaled, light contact shadow. */
private fun DrawScope.drawAutumnFallenLeaves(
    w: Float,
    h: Float,
    light: HeroSceneLight,
    foliageAmount: Float,
) {
    val progress = ((0.90f - foliageAmount) / 0.62f).coerceIn(0f, 1f)
    // Early: fewer on ground; late: denser under crowns
    val densitySkip = when {
        progress < 0.30f -> 3 // keep ~⅔
        progress < 0.60f -> 5 // keep most
        else -> 7 // keep almost all
    }
    HeroAutumnFallenLeaves
        .sortedBy { it.depth }
        .forEach { leaf ->
            if ((leaf.seed % 7) >= densitySkip && progress < 0.55f) return@forEach
            val s = w * leaf.size * (0.70f + leaf.depth * 0.55f)
            val cx = w * leaf.x
            val cy = h * leaf.y
            val contrast = (0.35f + leaf.depth * 0.55f).coerceIn(0.30f, 0.92f)
            val raw = when (leaf.tone % 3) {
                1 -> HeroAutumnLeafCopper
                2 -> HeroAutumnLeafShade
                else -> HeroAutumnLeafGold
            }
            val col = light.litColor(raw, 0.30f + leaf.depth * 0.15f)
                .copy(alpha = (0.55f + leaf.depth * 0.35f) * contrast)
            // Soft contact shadow so leaf sits on ground
            drawOval(
                Color.Black.copy(alpha = 0.10f + leaf.depth * 0.08f),
                topLeft = Offset(cx - s * 0.55f, cy + s * 0.15f),
                size = Size(s * 1.15f, s * 0.28f),
            )
            drawAutumnLeafShape(
                center = Offset(cx, cy),
                length = s * 2.2f,
                rotDeg = leaf.rotDeg,
                color = col,
                showVein = leaf.depth > 0.65f,
                seed = leaf.seed,
            )
        }
}

/** Pointed tip, asymmetric body, short stem — not a dot or oval. */
private fun DrawScope.drawAutumnLeafShape(
    center: Offset,
    length: Float,
    rotDeg: Float,
    color: Color,
    showVein: Boolean,
    seed: Int,
) {
    val rad = rotDeg * (PI.toFloat() / 180f)
    val cosR = cos(rad)
    val sinR = sin(rad)
    fun local(lx: Float, ly: Float): Offset = Offset(
        center.x + lx * cosR - ly * sinR,
        center.y + lx * sinR + ly * cosR,
    )
    val halfW = length * (0.28f + (seed % 3) * 0.03f)
    val asym = if (seed % 2 == 0) 1.12f else 0.88f
    val tip = local(length * 0.55f, 0f)
    val stem = local(-length * 0.42f, 0f)
    val left = local(-length * 0.05f, -halfW * asym)
    val right = local(-length * 0.02f, halfW)
    val leftWaist = local(length * 0.18f, -halfW * 0.55f * asym)
    val rightWaist = local(length * 0.22f, halfW * 0.50f)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        quadraticBezierTo(rightWaist.x, rightWaist.y, right.x, right.y)
        quadraticBezierTo(stem.x + (right.x - stem.x) * 0.3f, stem.y, stem.x, stem.y)
        quadraticBezierTo(stem.x + (left.x - stem.x) * 0.3f, stem.y, left.x, left.y)
        quadraticBezierTo(leftWaist.x, leftWaist.y, tip.x, tip.y)
        close()
    }
    drawPath(path, color = color)
    // Short stem stub
    val stemTip = local(-length * 0.52f, 0f)
    drawLine(
        color.copy(alpha = color.alpha * 0.85f),
        stem,
        stemTip,
        strokeWidth = (length * 0.06f).coerceAtLeast(0.8f),
        cap = StrokeCap.Round,
    )
    if (showVein) {
        val veinEnd = local(length * 0.35f, 0f)
        drawLine(
            Color.Black.copy(alpha = 0.18f * color.alpha),
            stem,
            veinEnd,
            strokeWidth = (length * 0.04f).coerceAtLeast(0.6f),
            cap = StrokeCap.Round,
        )
    }
}

/** Small white daisy with stem — no naked dots. */
private fun DrawScope.drawSpringDaisy(
    center: Offset,
    petalR: Float,
    tiltDeg: Float,
    stemH: Float,
    olive: Color,
    midGreen: Color,
) {
    val stemBase = Offset(center.x, center.y + stemH * 0.35f)
    drawLine(olive, stemBase, center, strokeWidth = petalR * 0.35f, cap = StrokeCap.Round)
    // Tiny leaf on stem
    drawOval(
        midGreen.copy(alpha = 0.85f),
        topLeft = Offset(center.x - petalR * 0.9f, center.y + stemH * 0.05f),
        size = Size(petalR * 1.1f, petalR * 0.45f),
    )
    val rad = tiltDeg * (PI.toFloat() / 180f)
    val petals = 5
    for (p in 0 until petals) {
        val a = rad + p * (PI.toFloat() * 2f / petals)
        val px = center.x + cos(a) * petalR * 0.85f
        val py = center.y + sin(a) * petalR * 0.85f
        drawOval(
            Color(0xFFF5F5F0),
            topLeft = Offset(px - petalR * 0.45f, py - petalR * 0.28f),
            size = Size(petalR * 0.90f, petalR * 0.55f),
        )
    }
    drawCircle(Color(0xFFE8D878), radius = petalR * 0.32f, center = center)
}

internal fun HeroParkPebble.seedish(): Float = ((x * 17f + y * 13f) % 1f)

/** Soft irregular soil blotch path (no oval silhouette). */
internal fun irregularSoilBlotch(cx: Float, cy: Float, rx: Float, ry: Float, seed: Int): Path =
    Path().apply {
        val n = 6
        for (i in 0 until n) {
            val ang = (i / n.toFloat()) * (PI * 2.0).toFloat()
            val wobble = 0.72f + ((seed * 7 + i * 11) % 5) * 0.07f
            val x = cx + cos(ang) * rx * wobble
            val y = cy + sin(ang) * ry * (0.85f + ((seed + i) % 3) * 0.08f)
            if (i == 0) moveTo(x, y) else {
                val pang = ((i - 0.5f) / n.toFloat()) * (PI * 2.0).toFloat()
                val px = cx + cos(pang) * rx * (wobble * 0.9f)
                val py = cy + sin(pang) * ry * 0.9f
                quadraticBezierTo(px, py, x, y)
            }
        }
        close()
    }
