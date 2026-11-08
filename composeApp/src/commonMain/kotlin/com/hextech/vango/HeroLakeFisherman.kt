package com.hextech.vango

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Soft wood / silhouette tones — readable but quiet on the lake. */
private val HullDark = Color(0xFF3A2E24)
private val HullMid = Color(0xFF5A4636)
private val HullLit = Color(0xFF7A6250)
private val FigureCoat = Color(0xFF2A3848)
private val FigureSkin = Color(0xFFC4A078)
private val RodColor = Color(0xFF2A2218)

/** Open mid-lake anchors — avoid phone (left) and vehicle (right). */
private val FishSpotXFracs = floatArrayOf(0.30f, 0.38f, 0.44f, 0.34f, 0.40f)

/**
 * Occasional rowboat fisherman: arrives, casts, waits, leaves.
 * Draw above lake surface effects, under park / vehicle / phone.
 */
fun DrawScope.drawHeroLakeFisherman(
    w: Float,
    h: Float,
    lakeTop: Float,
    lakeBot: Float,
    sceneTSec: Float,
    animate: Boolean,
    period: HeroSkyPeriod,
    weather: HeroSkyWeather,
    iceCoverage: Float = 0f,
) {
    if (!animate) return
    if (iceCoverage > 0.32f) return
    val weatherMul = when (weather) {
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0f
        HeroSkyWeather.Fog -> 0.35f
        HeroSkyWeather.Overcast -> 0.70f
        HeroSkyWeather.PartlyCloudy -> 0.90f
        HeroSkyWeather.Clear -> 1f
    }
    if (weatherMul < 0.05f) return
    val periodMul = when (period) {
        HeroSkyPeriod.Night -> 0.18f
        HeroSkyPeriod.Dusk -> 0.72f
        HeroSkyPeriod.Dawn -> 0.85f
        HeroSkyPeriod.Day -> 1f
    }
    val alphaMul = weatherMul * periodMul
    if (alphaMul < 0.08f) return

    val pose = fishermanPose(w, h, lakeTop, lakeBot, sceneTSec) ?: return
    if (pose.alpha < 0.02f) return

    val a = pose.alpha * alphaMul
    drawFishermanWake(pose, a)
    drawFishermanBoat(pose, a)
    drawFishermanFigure(pose, a)
    drawFishermanRod(pose, a)
}

private data class FishermanPose(
    val x: Float,
    val y: Float,
    val boatW: Float,
    val facingRight: Boolean,
    val bob: Float,
    val rowPhase: Float,
    val alpha: Float,
    /** 0 idle travel, 1 casting, 2 fishing hold, 3 reeling. */
    val fishPhase: Int,
    val castP: Float,
    val lineDip: Float,
    val visitIndex: Int,
)

private fun fishermanPose(
    w: Float,
    h: Float,
    lakeTop: Float,
    lakeBot: Float,
    sceneTSec: Float,
): FishermanPose? {
    val t = sceneTSec.coerceAtLeast(0f)
    val (index, local, active) = resolveFishermanVisit(t)
    if (!active) return null

    val dur = visitDurationSec(index)
    val p = (local / dur).coerceIn(0f, 1f)
    val lakeH = (lakeBot - lakeTop).coerceAtLeast(1f)
    val boatW = w * 0.072f
    val spotX = w * FishSpotXFracs[((index % FishSpotXFracs.size) + FishSpotXFracs.size) % FishSpotXFracs.size]
    val fromLeft = index % 2 == 0
    val enterX = if (fromLeft) -boatW * 0.6f else w + boatW * 0.6f
    val exitX = if (fromLeft) w + boatW * 0.7f else -boatW * 0.7f
    val waterY = lakeTop + lakeH * (0.20f + 0.03f * sin(index * 1.7f))

    // Timeline: arrive → settle/cast → fish → leave
    val arriveEnd = 0.18f
    val castEnd = 0.28f
    val fishEnd = 0.78f
    val x: Float
    val facingRight: Boolean
    val rowPhase: Float
    val fishPhase: Int
    val castP: Float
    val alpha: Float

    when {
        p < arriveEnd -> {
            val u = smooth01(p / arriveEnd)
            x = lerp(enterX, spotX, u)
            facingRight = fromLeft
            rowPhase = p * 14f
            fishPhase = 0
            castP = 0f
            alpha = smooth01(p / (arriveEnd * 0.35f)).coerceIn(0f, 1f)
        }
        p < castEnd -> {
            x = spotX
            facingRight = fromLeft
            rowPhase = 0f
            fishPhase = 1
            castP = smooth01((p - arriveEnd) / (castEnd - arriveEnd))
            alpha = 1f
        }
        p < fishEnd -> {
            x = spotX + sin((p - castEnd) * 18f) * w * 0.0012f
            facingRight = fromLeft
            rowPhase = 0f
            fishPhase = 2
            castP = 1f
            alpha = 1f
        }
        else -> {
            val u = smooth01((p - fishEnd) / (1f - fishEnd))
            // Brief reel before departing
            fishPhase = if (u < 0.18f) 3 else 0
            castP = if (u < 0.18f) 1f - smooth01(u / 0.18f) else 0f
            x = lerp(spotX, exitX, if (u < 0.18f) 0f else smooth01((u - 0.18f) / 0.82f))
            facingRight = exitX > spotX
            rowPhase = if (u < 0.18f) 0f else (u - 0.18f) * 16f
            alpha = if (u > 0.78f) (1f - smooth01((u - 0.78f) / 0.22f)) else 1f
        }
    }

    val twoPi = (PI * 2.0).toFloat()
    val bob = sin(t * 1.35f * twoPi + index) * h * 0.0018f +
        sin(t * 0.55f * twoPi) * h * 0.0010f
    val lineDip = when (fishPhase) {
        1 -> castP * 0.35f
        2 -> {
            // Occasional tug while fishing
            val tugCycle = 7.5f + fract(index * 0.37f) * 3f
            val tugT = ((local - dur * castEnd) / tugCycle)
            val tugLocal = (tugT - floor(tugT.toDouble()).toFloat()).coerceIn(0f, 1f)
            val tug = if (tugLocal in 0.15f..0.42f) {
                sin(((tugLocal - 0.15f) / 0.27f) * PI.toFloat()) * 0.55f
            } else {
                0f
            }
            0.55f + 0.12f * sin(t * 0.9f * twoPi) + tug
        }
        3 -> 0.25f * castP
        else -> 0f
    }

    return FishermanPose(
        x = x,
        y = waterY + bob,
        boatW = boatW,
        facingRight = facingRight,
        bob = bob,
        rowPhase = rowPhase,
        alpha = alpha.coerceIn(0f, 1f),
        fishPhase = fishPhase,
        castP = castP.coerceIn(0f, 1f),
        lineDip = lineDip.coerceIn(0f, 1f),
        visitIndex = index,
    )
}

/** Deterministic visit schedule. Returns (index, seconds into visit, active). */
private fun resolveFishermanVisit(tSec: Float): Triple<Int, Float, Boolean> {
    var cursor = 0f
    var index = 0
    while (index < 10_000) {
        val gap = visitGapSec(index)
        val start = cursor + gap
        val dur = visitDurationSec(index)
        val end = start + dur
        if (tSec < start) return Triple(index, 0f, false)
        if (tSec < end) return Triple(index, tSec - start, true)
        cursor = end
        index++
    }
    return Triple(0, 0f, false)
}

/** First wait long; later gaps ~75–140s so he feels occasional. */
private fun visitGapSec(index: Int): Float {
    val u = fract(index * 0.6180339887f + 0.41f)
    return if (index == 0) {
        90f + u * 70f // 90–160s
    } else {
        75f + u * 65f
    }
}

/** 28–42s on the water. */
private fun visitDurationSec(index: Int): Float {
    val u = fract(index * 0.4142135623f + 0.19f)
    return 28f + u * 14f
}

private fun DrawScope.drawFishermanWake(pose: FishermanPose, a: Float) {
    if (pose.fishPhase != 0 || pose.rowPhase <= 0.01f) return
    val fade = a * 0.35f
    val dir = if (pose.facingRight) -1f else 1f
    val stern = pose.x + dir * pose.boatW * 0.42f
    for (i in 0 until 3) {
        val age = ((pose.rowPhase * 0.35f + i * 0.55f) % 1.2f) / 1.2f
        val rx = pose.boatW * (0.18f + age * 0.55f)
        val ry = pose.boatW * (0.04f + age * 0.08f)
        drawOval(
            color = Color.White.copy(alpha = fade * (1f - age) * 0.55f),
            topLeft = Offset(stern - rx + dir * age * pose.boatW * 0.15f, pose.y - ry * 0.2f),
            size = Size(rx * 2f, ry),
            style = Stroke(width = pose.boatW * 0.03f * (1f - age * 0.4f)),
        )
    }
}

private fun DrawScope.drawFishermanBoat(pose: FishermanPose, a: Float) {
    val dir = if (pose.facingRight) 1f else -1f
    scale(scaleX = dir, scaleY = 1f, pivot = Offset(pose.x, pose.y)) {
        val bw = pose.boatW
        val bh = bw * 0.22f
        val cx = pose.x
        val cy = pose.y
        // Hull
        drawPath(
            Path().apply {
                moveTo(cx - bw * 0.48f, cy)
                cubicTo(
                    cx - bw * 0.35f, cy + bh * 0.95f,
                    cx + bw * 0.25f, cy + bh * 1.05f,
                    cx + bw * 0.48f, cy + bh * 0.15f,
                )
                cubicTo(
                    cx + bw * 0.42f, cy - bh * 0.15f,
                    cx - bw * 0.20f, cy - bh * 0.35f,
                    cx - bw * 0.48f, cy,
                )
                close()
            },
            color = HullDark.copy(alpha = a * 0.92f),
        )
        drawPath(
            Path().apply {
                moveTo(cx - bw * 0.42f, cy - bh * 0.05f)
                cubicTo(
                    cx - bw * 0.15f, cy - bh * 0.42f,
                    cx + bw * 0.20f, cy - bh * 0.38f,
                    cx + bw * 0.40f, cy - bh * 0.08f,
                )
                lineTo(cx + bw * 0.36f, cy + bh * 0.08f)
                cubicTo(
                    cx + bw * 0.10f, cy - bh * 0.05f,
                    cx - bw * 0.20f, cy - bh * 0.02f,
                    cx - bw * 0.40f, cy + bh * 0.05f,
                )
                close()
            },
            color = HullMid.copy(alpha = a * 0.95f),
        )
        // Gunwale highlight
        drawLine(
            color = HullLit.copy(alpha = a * 0.55f),
            start = Offset(cx - bw * 0.40f, cy - bh * 0.18f),
            end = Offset(cx + bw * 0.38f, cy - bh * 0.10f),
            strokeWidth = bw * 0.028f,
            cap = StrokeCap.Round,
        )
        // Soft water contact shadow
        drawOval(
            color = Color(0xFF1A2838).copy(alpha = a * 0.22f),
            topLeft = Offset(cx - bw * 0.40f, cy + bh * 0.05f),
            size = Size(bw * 0.80f, bh * 0.35f),
        )
    }
}

private fun DrawScope.drawFishermanFigure(pose: FishermanPose, a: Float) {
    val dir = if (pose.facingRight) 1f else -1f
    val bw = pose.boatW
    val cx = pose.x - dir * bw * 0.06f
    val seatY = pose.y - bw * 0.12f
    val torsoH = bw * 0.22f
    // Body
    drawOval(
        color = FigureCoat.copy(alpha = a * 0.95f),
        topLeft = Offset(cx - bw * 0.055f, seatY - torsoH),
        size = Size(bw * 0.11f, torsoH * 1.05f),
    )
    // Head
    drawCircle(
        color = FigureSkin.copy(alpha = a * 0.95f),
        radius = bw * 0.038f,
        center = Offset(cx + dir * bw * 0.01f, seatY - torsoH - bw * 0.02f),
    )
    // Cap
    drawOval(
        color = Color(0xFF1E2A38).copy(alpha = a * 0.90f),
        topLeft = Offset(cx - bw * 0.04f + dir * bw * 0.01f, seatY - torsoH - bw * 0.055f),
        size = Size(bw * 0.09f, bw * 0.035f),
    )
    // Oar when rowing
    if (pose.fishPhase == 0 && pose.rowPhase > 0.01f) {
        val swing = sin(pose.rowPhase * PI.toFloat()) * 28f
        val oarLen = bw * 0.55f
        val ang = (swing - 15f) * (PI.toFloat() / 180f)
        val ox = cx + dir * bw * 0.08f
        val oy = seatY - torsoH * 0.35f
        val tipX = ox + cos(ang) * oarLen * dir
        val tipY = oy + sin(ang) * oarLen * 0.55f + bw * 0.08f
        drawLine(
            color = HullDark.copy(alpha = a * 0.75f),
            start = Offset(ox, oy),
            end = Offset(tipX, tipY),
            strokeWidth = bw * 0.022f,
            cap = StrokeCap.Round,
        )
        drawOval(
            color = HullMid.copy(alpha = a * 0.70f),
            topLeft = Offset(tipX - bw * 0.06f, tipY - bw * 0.015f),
            size = Size(bw * 0.12f, bw * 0.03f),
        )
    }
}

private fun DrawScope.drawFishermanRod(pose: FishermanPose, a: Float) {
    if (pose.fishPhase == 0 && pose.castP < 0.02f) return
    val dir = if (pose.facingRight) 1f else -1f
    val bw = pose.boatW
    val handX = pose.x + dir * bw * 0.02f
    val handY = pose.y - bw * 0.28f

    val tipLift = when (pose.fishPhase) {
        1 -> lerp(0.15f, 1f, pose.castP)
        3 -> lerp(0.55f, 0.20f, 1f - pose.castP)
        else -> 0.72f + 0.08f * sin(pose.lineDip * PI.toFloat())
    }
    val tipReach = bw * (0.55f + pose.castP * 0.35f)
    val tipX = handX + dir * tipReach
    val tipY = handY - bw * (0.18f + tipLift * 0.42f)

    drawLine(
        color = RodColor.copy(alpha = a * 0.90f),
        start = Offset(handX, handY),
        end = Offset(tipX, tipY),
        strokeWidth = bw * 0.018f,
        cap = StrokeCap.Round,
    )

    if (pose.castP < 0.08f && pose.fishPhase != 2) return

    val dip = pose.lineDip
    val lineLen = bw * (0.35f + dip * 0.85f)
    val waterHitY = pose.y + bw * 0.02f + lineLen * 0.15f
    val midX = tipX + dir * bw * 0.04f
    val midY = tipY + (waterHitY - tipY) * 0.55f + abs(sin(dip * 3f)) * bw * 0.02f
    // Line as two segments (slight sag)
    drawLine(
        color = Color.White.copy(alpha = a * 0.28f),
        start = Offset(tipX, tipY),
        end = Offset(midX, midY),
        strokeWidth = bw * 0.008f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White.copy(alpha = a * 0.22f),
        start = Offset(midX, midY),
        end = Offset(tipX + dir * bw * 0.02f, waterHitY),
        strokeWidth = bw * 0.007f,
        cap = StrokeCap.Round,
    )
    // Tiny float / bobber
    if (dip > 0.15f) {
        drawCircle(
            color = Color(0xFFC05040).copy(alpha = a * 0.75f),
            radius = bw * 0.018f,
            center = Offset(tipX + dir * bw * 0.02f, waterHitY),
        )
        // Soft ripple at float
        val pulse = 0.55f + 0.45f * abs(sin(dip * PI.toFloat()))
        drawOval(
            color = Color.White.copy(alpha = a * 0.12f * pulse),
            topLeft = Offset(
                tipX + dir * bw * 0.02f - bw * 0.06f * pulse,
                waterHitY - bw * 0.012f,
            ),
            size = Size(bw * 0.12f * pulse, bw * 0.028f),
            style = Stroke(width = bw * 0.012f),
        )
    }
}

private fun smooth01(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

private fun fract(v: Float): Float {
    val f = v - floor(v.toDouble()).toFloat()
    return if (f < 0f) f + 1f else f
}
