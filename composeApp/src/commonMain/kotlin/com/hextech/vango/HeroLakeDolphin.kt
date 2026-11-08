package com.hextech.vango

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Grey-blue dorsal / lighter belly. */
private val DolphinBack = Color(0xFF6A8598)
private val DolphinBelly = Color(0xFFA8BCC8)

/** Open-water breach X anchors — avoid phone/link (left) and vehicle (right). */
private val BreachXFracs = floatArrayOf(0.34f, 0.46f, 0.52f, 0.40f, 0.48f, 0.36f)

/**
 * Underwater dolphin (caller clips to lake). Surface reflections/ripples draw above this.
 */
fun DrawScope.drawHeroLakeDolphinUnderwater(
    w: Float,
    h: Float,
    lakeTop: Float,
    lakeBot: Float,
    sceneTSec: Float,
    animate: Boolean,
    period: HeroSkyPeriod,
) {
    val pose = dolphinPose(w, h, lakeTop, lakeBot, sceneTSec, animate) ?: return
    val nightMul = dolphinNightMul(period)
    if (nightMul < 0.05f) return

    val underAlpha = pose.underAlpha * nightMul
    if (underAlpha < 0.02f) return

    drawDolphinAt(
        pose = pose,
        alpha = underAlpha,
        aboveWater = false,
        surfaceY = pose.surfaceY,
    )
}

/**
 * Breach / splash layer — not clipped to the lake edge so the arc can clear the waterline.
 * Draw after ripples, before park / vehicle / phone.
 */
fun DrawScope.drawHeroLakeDolphinSurface(
    w: Float,
    h: Float,
    lakeTop: Float,
    lakeBot: Float,
    sceneTSec: Float,
    animate: Boolean,
    period: HeroSkyPeriod,
) {
    val pose = dolphinPose(w, h, lakeTop, lakeBot, sceneTSec, animate) ?: return
    val nightMul = dolphinNightMul(period)
    if (nightMul < 0.05f) return

    // Soft cover of the submerged body at the waterline during breach
    if (pose.breachP in 0.25f..0.85f && pose.aboveAlpha > 0.02f) {
        val coverA = (0.10f + pose.breachP * 0.08f) * nightMul
        val cx = pose.x
        val sy = pose.surfaceY
        drawOval(
            color = Color.White.copy(alpha = coverA * 0.55f),
            topLeft = Offset(cx - pose.len * 0.55f, sy - pose.len * 0.04f),
            size = Size(pose.len * 1.1f, pose.len * 0.10f),
        )
        drawLine(
            color = Color.White.copy(alpha = coverA),
            start = Offset(cx - pose.len * 0.5f, sy + pose.len * 0.01f),
            end = Offset(cx + pose.len * 0.45f, sy - pose.len * 0.01f),
            strokeWidth = pose.len * 0.035f,
            cap = StrokeCap.Round,
        )
    }

    val aboveAlpha = pose.aboveAlpha * nightMul
    if (aboveAlpha >= 0.02f) {
        drawDolphinAt(
            pose = pose,
            alpha = aboveAlpha,
            aboveWater = true,
            surfaceY = pose.surfaceY,
        )
    }

    drawDolphinSplashes(pose, nightMul, animate)
}

private fun dolphinNightMul(period: HeroSkyPeriod): Float = when (period) {
    HeroSkyPeriod.Night -> 0.22f
    HeroSkyPeriod.Dusk -> 0.50f
    HeroSkyPeriod.Dawn -> 0.72f
    HeroSkyPeriod.Day -> 1f
}

private data class DolphinPose(
    val x: Float,
    val y: Float,
    val len: Float,
    val facingRight: Boolean,
    val pitchDeg: Float,
    val tailWag: Float,
    val surfaceY: Float,
    val underAlpha: Float,
    val aboveAlpha: Float,
    val breachP: Float,
    /** Splash age 0–1 from exit contact; -1 = inactive. */
    val exitSplash: Float,
    /** Splash age 0–1 from dive contact; -1 = inactive. */
    val diveSplash: Float,
    val splashX: Float,
)

private fun dolphinPose(
    w: Float,
    h: Float,
    lakeTop: Float,
    lakeBot: Float,
    sceneTSec: Float,
    animate: Boolean,
): DolphinPose? {
    val lakeH = (lakeBot - lakeTop).coerceAtLeast(1f)
    val len = w * 0.045f
    val twoPi = (PI * 2.0).toFloat()
    val t = if (animate) sceneTSec.coerceAtLeast(0f) else 0f

    // Schedule breaches: variable 25–45s gaps, duration 2–3s
    val (breachIndex, breachLocal, inBreach) = resolveBreachWindow(t)
    val breachDur = breachDurationSec(breachIndex)
    val breachP = if (inBreach) (breachLocal / breachDur).coerceIn(0f, 1f) else -1f

    val surfXFrac = BreachXFracs[((breachIndex % BreachXFracs.size) + BreachXFracs.size) % BreachXFracs.size]
    val surfX = w * surfXFrac
    // Local waterline near open mid-lake (slightly below far edge)
    val surfaceY = lakeTop + lakeH * 0.10f

    // Cruise: slow curved patrol in open band (not under phone / vehicle)
    val cruiseCycle = 38f
    val cruiseT = ((t / cruiseCycle) % 1f + 1f) % 1f
    val (xNorm, facingCruise) = dolphinCruiseX(cruiseT)
    val cruiseMarginL = w * 0.24f
    val cruiseMarginR = w * 0.58f
    val cruiseX = cruiseMarginL + xNorm * (cruiseMarginR - cruiseMarginL)
    val cruiseDepth = 0.42f + 0.10f * sin(t * 0.17f * twoPi)
    val cruiseYWave = sin(t * 0.13f * twoPi) * lakeH * 0.035f
    val cruiseY = lakeTop + lakeH * cruiseDepth + cruiseYWave

    val facingRight: Boolean
    val x: Float
    val y: Float
    val pitchDeg: Float
    val underAlpha: Float
    val aboveAlpha: Float
    var exitSplash = -1f
    var diveSplash = -1f
    val splashX: Float

    if (breachP < 0f) {
        facingRight = facingCruise
        x = cruiseX
        y = cruiseY
        pitchDeg = sin(t * 0.21f * twoPi) * 4f
        underAlpha = 0.22f + 0.06f * sin(t * 0.09f * twoPi)
        aboveAlpha = 0f
        splashX = x
    } else {
        // Approach surface point from cruise, then arc
        val approach = smooth01(breachP / 0.28f)
        val targetFacing = surfX >= cruiseX
        facingRight = if (breachP < 0.20f) facingCruise else targetFacing
        val baseX = lerp(cruiseX, surfX, approach.coerceIn(0f, 1f))
        // Drift away after dive (alternate side each breach)
        val awaySign = if (breachIndex % 2 == 0) 1f else -1f
        val away = if (breachP > 0.72f) {
            smooth01((breachP - 0.72f) / 0.28f) * w * 0.08f * awaySign
        } else {
            0f
        }
        x = baseX + away + sin(breachP * twoPi) * len * 0.08f

        // Vertical: rise → breach arc → dive → sink
        val riseY = lerp(cruiseY, surfaceY + len * 0.12f, smooth01(breachP / 0.30f))
        val arcH = when {
            breachP < 0.30f -> 0f
            breachP < 0.62f -> {
                val u = (breachP - 0.30f) / 0.32f
                // Short low arc — peak ~0.28 of body length above water
                sin(u * PI.toFloat()) * len * 0.28f
            }
            else -> 0f
        }
        val diveSink = if (breachP > 0.62f) {
            smooth01((breachP - 0.62f) / 0.38f) * lakeH * 0.22f
        } else {
            0f
        }
        y = when {
            breachP < 0.30f -> riseY
            breachP < 0.62f -> surfaceY - arcH + len * 0.06f
            else -> surfaceY + len * 0.10f + diveSink
        }

        // Pitch: climb → crest → nose-down dive
        pitchDeg = when {
            breachP < 0.28f -> lerp(0f, -18f, smooth01(breachP / 0.28f))
            breachP < 0.45f -> lerp(-18f, -8f, smooth01((breachP - 0.28f) / 0.17f))
            breachP < 0.62f -> lerp(-8f, 22f, smooth01((breachP - 0.45f) / 0.17f))
            breachP < 0.85f -> lerp(22f, 8f, smooth01((breachP - 0.62f) / 0.23f))
            else -> lerp(8f, 0f, smooth01((breachP - 0.85f) / 0.15f))
        }

        // Underwater part fades during crest; above-water peaks mid-arc
        val crest = when {
            breachP < 0.28f -> 0f
            breachP < 0.42f -> smooth01((breachP - 0.28f) / 0.14f) // dorsal first
            breachP < 0.62f -> 1f
            breachP < 0.78f -> 1f - smooth01((breachP - 0.62f) / 0.16f)
            else -> 0f
        }
        aboveAlpha = crest * 0.85f
        underAlpha = when {
            breachP < 0.28f -> lerp(0.24f, 0.18f, smooth01(breachP / 0.28f))
            breachP < 0.70f -> 0.14f
            else -> lerp(0.14f, 0f, smooth01((breachP - 0.70f) / 0.30f)) // fade while leaving
        }

        // Splash windows (~0.55s each)
        val splashLife = 0.55f
        if (breachLocal >= breachDur * 0.30f) {
            val age = breachLocal - breachDur * 0.30f
            if (age < splashLife) exitSplash = (age / splashLife).coerceIn(0f, 1f)
        }
        if (breachLocal >= breachDur * 0.70f) {
            val age = breachLocal - breachDur * 0.70f
            if (age < splashLife) diveSplash = (age / splashLife).coerceIn(0f, 1f)
        }
        splashX = surfX
    }

    val tailWag = if (animate) {
        sin((t * 2.4f + breachIndex * 0.7f) * twoPi) * 11f
    } else {
        0f
    }

    return DolphinPose(
        x = x,
        y = y,
        len = len,
        facingRight = facingRight,
        pitchDeg = pitchDeg,
        tailWag = tailWag,
        surfaceY = surfaceY,
        underAlpha = underAlpha.coerceIn(0f, 0.40f),
        aboveAlpha = aboveAlpha.coerceIn(0f, 0.90f),
        breachP = breachP,
        exitSplash = exitSplash,
        diveSplash = diveSplash,
        splashX = splashX,
    )
}

/**
 * Deterministic breach schedule from continuous time.
 * Returns (index, seconds into breach, currently breaching).
 */
private fun resolveBreachWindow(tSec: Float): Triple<Int, Float, Boolean> {
    var cursor = 0f
    var index = 0
    // Walk schedule until we pass t (bounded loop)
    while (index < 10_000) {
        val gap = breachGapSec(index)
        val start = cursor + gap
        val dur = breachDurationSec(index)
        val end = start + dur
        if (tSec < start) {
            return Triple(index, 0f, false)
        }
        if (tSec < end) {
            return Triple(index, tSec - start, true)
        }
        cursor = end
        index++
    }
    return Triple(0, 0f, false)
}

/**
 * Gaps between breaches. First wait is long & variable so the card
 * doesn't jump as soon as it opens; later gaps 32–55s.
 */
private fun breachGapSec(index: Int): Float {
    val u = fract(index * 0.6180339887f + 0.17f)
    return if (index == 0) {
        52f + u * 48f // 52–100s before first surface
    } else {
        32f + u * 23f
    }
}

/** 2–3s breach duration. */
private fun breachDurationSec(index: Int): Float {
    val u = fract(index * 0.4142135623f + 0.33f)
    return 2f + u * 1f
}

private fun dolphinCruiseX(t: Float): Pair<Float, Boolean> {
    val leg = if (t < 0.5f) t / 0.5f else (t - 0.5f) / 0.5f
    val eased = smooth01(leg)
    val x = if (t < 0.5f) eased else 1f - eased
    val facingRight = t < 0.5f
    return x to facingRight
}

private fun DrawScope.drawDolphinAt(
    pose: DolphinPose,
    alpha: Float,
    aboveWater: Boolean,
    surfaceY: Float,
) {
    // Waterline clip in screen space (before pitch) so the body splits cleanly at the surface.
    val waterClip = Path().apply {
        if (aboveWater) {
            addRect(Rect(0f, 0f, size.width, surfaceY))
        } else {
            addRect(Rect(0f, surfaceY, size.width, size.height))
        }
    }
    val dir = if (pose.facingRight) 1f else -1f
    clipPath(waterClip) {
        rotate(degrees = pose.pitchDeg, pivot = Offset(pose.x, pose.y)) {
            scale(scaleX = dir, scaleY = 1f, pivot = Offset(pose.x, pose.y)) {
                drawDolphinSilhouette(
                    cx = pose.x,
                    cy = pose.y,
                    len = pose.len,
                    alpha = alpha,
                    wag = pose.tailWag,
                )
            }
        }
    }
}

/**
 * Distinct dolphin path (not a fish remix): long body, short beak, curved dorsal,
 * pectorals, horizontal notched flukes. Local nose toward +X.
 */
private fun DrawScope.drawDolphinSilhouette(
    cx: Float,
    cy: Float,
    len: Float,
    alpha: Float,
    wag: Float,
) {
    val bodyL = len * 0.78f
    val bodyH = len * 0.22f
    val nose = cx + bodyL * 0.52f
    val beakTip = nose + len * 0.10f
    val tailBase = cx - bodyL * 0.48f
    val back = DolphinBack.copy(alpha = alpha)
    val belly = DolphinBelly.copy(alpha = alpha * 0.92f)

    drawPath(
        Path().apply {
            moveTo(beakTip, cy + bodyH * 0.05f)
            cubicTo(
                nose - bodyL * 0.02f, cy - bodyH * 0.15f,
                cx + bodyL * 0.15f, cy - bodyH * 1.05f,
                cx - bodyL * 0.05f, cy - bodyH * 0.95f,
            )
            cubicTo(
                cx - bodyL * 0.28f, cy - bodyH * 0.85f,
                tailBase + bodyL * 0.08f, cy - bodyH * 0.35f,
                tailBase, cy,
            )
            lineTo(tailBase, cy + bodyH * 0.15f)
            cubicTo(
                cx - bodyL * 0.10f, cy - bodyH * 0.15f,
                cx + bodyL * 0.20f, cy - bodyH * 0.20f,
                beakTip, cy + bodyH * 0.05f,
            )
            close()
        },
        color = back,
    )
    drawPath(
        Path().apply {
            moveTo(beakTip, cy + bodyH * 0.05f)
            cubicTo(
                nose, cy + bodyH * 0.55f,
                cx + bodyL * 0.05f, cy + bodyH * 1.05f,
                cx - bodyL * 0.15f, cy + bodyH * 0.90f,
            )
            cubicTo(
                cx - bodyL * 0.35f, cy + bodyH * 0.55f,
                tailBase + bodyL * 0.10f, cy + bodyH * 0.35f,
                tailBase, cy + bodyH * 0.15f,
            )
            lineTo(tailBase, cy)
            cubicTo(
                cx - bodyL * 0.05f, cy + bodyH * 0.05f,
                cx + bodyL * 0.25f, cy + bodyH * 0.05f,
                beakTip, cy + bodyH * 0.05f,
            )
            close()
        },
        color = belly,
    )
    drawPath(
        Path().apply {
            moveTo(nose - len * 0.02f, cy - bodyH * 0.08f)
            lineTo(beakTip, cy + bodyH * 0.02f)
            lineTo(nose - len * 0.01f, cy + bodyH * 0.18f)
            close()
        },
        color = back.copy(alpha = alpha * 0.95f),
    )
    drawPath(
        Path().apply {
            moveTo(cx - bodyL * 0.08f, cy - bodyH * 0.85f)
            cubicTo(
                cx - bodyL * 0.02f, cy - bodyH * 1.85f,
                cx + bodyL * 0.08f, cy - bodyH * 2.15f,
                cx + bodyL * 0.18f, cy - bodyH * 0.70f,
            )
            quadraticBezierTo(cx + bodyL * 0.05f, cy - bodyH * 1.15f, cx - bodyL * 0.08f, cy - bodyH * 0.85f)
            close()
        },
        color = back.copy(alpha = alpha * 0.95f),
    )
    drawPath(
        Path().apply {
            moveTo(cx + bodyL * 0.08f, cy + bodyH * 0.25f)
            cubicTo(
                cx + bodyL * 0.05f, cy + bodyH * 1.15f,
                cx - bodyL * 0.05f, cy + bodyH * 1.35f,
                cx - bodyL * 0.12f, cy + bodyH * 0.55f,
            )
            lineTo(cx + bodyL * 0.02f, cy + bodyH * 0.15f)
            close()
        },
        color = back.copy(alpha = alpha * 0.75f),
    )
    val tw = wag * 0.012f * len
    drawPath(
        Path().apply {
            moveTo(tailBase, cy)
            cubicTo(
                tailBase - len * 0.12f, cy - len * 0.04f + tw,
                tailBase - len * 0.28f, cy - len * 0.10f + tw,
                tailBase - len * 0.38f, cy - len * 0.02f + tw * 0.6f,
            )
            lineTo(tailBase - len * 0.22f, cy + tw * 0.2f)
            lineTo(tailBase - len * 0.38f, cy + len * 0.02f - tw * 0.6f)
            cubicTo(
                tailBase - len * 0.28f, cy + len * 0.10f - tw,
                tailBase - len * 0.12f, cy + len * 0.04f - tw,
                tailBase, cy,
            )
            close()
        },
        color = back.copy(alpha = alpha * 0.90f),
    )
}

private fun DrawScope.drawDolphinSplashes(pose: DolphinPose, nightMul: Float, animate: Boolean) {
    if (!animate) return
    fun ring(age: Float, x: Float) {
        if (age < 0f) return
        val fade = (1f - age).coerceIn(0f, 1f)
        val rx = pose.len * (0.35f + age * 1.1f)
        val ry = pose.len * (0.08f + age * 0.18f)
        val a = 0.22f * fade * nightMul
        drawOval(
            color = Color.White.copy(alpha = a),
            topLeft = Offset(x - rx, pose.surfaceY - ry * 0.45f),
            size = Size(rx * 2f, ry),
            style = Stroke(width = pose.len * 0.04f * (1f - age * 0.5f)),
        )
        // Tiny droplets
        for (i in 0 until 4) {
            val ang = (i * 0.9f + age * 2f)
            val dx = cos(ang) * pose.len * (0.15f + age * 0.35f)
            val dy = -sin(age * PI.toFloat()) * pose.len * (0.12f + i * 0.03f) * (1f - age)
            drawCircle(
                color = Color.White.copy(alpha = a * 0.85f),
                radius = pose.len * (0.025f - age * 0.012f).coerceAtLeast(0.4f),
                center = Offset(x + dx * (if (i % 2 == 0) 1f else -0.7f), pose.surfaceY + dy),
            )
        }
    }
    ring(pose.exitSplash, pose.splashX)
    ring(pose.diveSplash, pose.splashX + pose.len * 0.15f)
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
