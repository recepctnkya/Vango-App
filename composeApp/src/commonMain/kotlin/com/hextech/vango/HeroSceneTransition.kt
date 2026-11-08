package com.hextech.vango

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow

/**
 * Shared soft-transition timings for hero scene.
 * Live mode: light/season follow continuous clock; weather eases on data refresh.
 * Sim: hour ticks and season jumps use shorter/longer eases (configurable).
 */
object HeroSceneTransitionConfig {
    /** Sim virtual-hour step cross-fade (test: 2s/hour → ~1.5s ease). */
    var HourEaseMs: Int = 1_500

    /** Sim month / season jump cross-fade. */
    var SeasonEaseMs: Int = 4_000

    /** New live weather sample cross-fade. */
    var WeatherEaseMs: Int = 7_000

    /** Live celestial / light soft settle (near-continuous; keep short). */
    var LiveCelestialEaseMs: Int = 400

    /** Live season vegetation settle when calendar season bucket changes. */
    var LiveSeasonEaseMs: Int = 8_000
}

/** Smoothstep — soft ease-in/out for manual blends (t in 0…1). */
fun heroSmoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** Compose easing matching smoothstep character. */
val HeroSceneSmoothEase: Easing = CubicBezierEasing(0.40f, 0.00f, 0.20f, 1.00f)

/**
 * Perceptual-ish color lerp (approx linear-light via gamma 2.2).
 * Avoids muddy mid greys from naive sRGB channel lerp.
 */
fun lerpColorPerceptual(a: Color, b: Color, t: Float): Color {
    val x = heroSmoothstep(t.coerceIn(0f, 1f))
    fun dec(c: Float): Float = c.pow(2.2f)
    fun enc(c: Float): Float = c.coerceIn(0f, 1f).pow(1f / 2.2f)
    return Color(
        red = enc(dec(a.red) + (dec(b.red) - dec(a.red)) * x),
        green = enc(dec(a.green) + (dec(b.green) - dec(a.green)) * x),
        blue = enc(dec(a.blue) + (dec(b.blue) - dec(a.blue)) * x),
        alpha = a.alpha + (b.alpha - a.alpha) * x,
    )
}

/**
 * Animate a 0…1-ish float toward [target].
 * When the jump is large (e.g. sun wrapping past midnight), snap instead of
 * traversing the long way across the sky.
 */
@Composable
fun animateHeroFloatAsState(
    target: Float,
    durationMs: Int,
    label: String,
    wrapThreshold: Float = 0.55f,
): State<Float> {
    val anim = remember(label) { Animatable(target) }
    LaunchedEffect(target, durationMs) {
        if (durationMs <= 0) {
            anim.snapTo(target)
            return@LaunchedEffect
        }
        val cur = anim.value
        if (abs(target - cur) > wrapThreshold) {
            // Continue from current visual identity: snap onto new side, no full-sky tour
            anim.snapTo(target)
        } else {
            anim.animateTo(
                target,
                animationSpec = tween(durationMs, easing = HeroSceneSmoothEase),
            )
        }
    }
    return anim.asState()
}

/** Same as [animateHeroFloatAsState] without wrap snap (coverage, alphas). */
@Composable
fun animateHeroScalarAsState(
    target: Float,
    durationMs: Int,
    label: String,
): State<Float> {
    val anim = remember(label) { Animatable(target) }
    LaunchedEffect(target, durationMs) {
        if (durationMs <= 0) {
            anim.snapTo(target)
            return@LaunchedEffect
        }
        // Mid-transition retarget: Animatable continues from current value (no rewind)
        anim.animateTo(
            target,
            animationSpec = tween(durationMs, easing = HeroSceneSmoothEase),
        )
    }
    return anim.asState()
}
