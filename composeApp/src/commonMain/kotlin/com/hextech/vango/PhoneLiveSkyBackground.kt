package com.hextech.vango

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural canvas sky — currently disabled on the phone home hero.
 * Hero uses [PhoneTimedSkyBackground] (still frames + soft cross-fade) instead.
 * Kept for possible future re-enable / tablet experiments.
 */
@Composable
fun PhoneLiveSkyBackground(modifier: Modifier = Modifier) {
    var timeSec by remember { mutableStateOf(0f) }
    var dayProgress by remember { mutableStateOf(localDayProgress()) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    timeSec += ((now - last).coerceIn(0L, 32_000_000L)) / 1_000_000_000f
                }
                last = now
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            dayProgress = localDayProgress()
            kotlinx.coroutines.delay(20_000)
        }
    }
    val progressAnim by animateFloatAsState(
        targetValue = dayProgress,
        animationSpec = tween(durationMillis = 2_800, easing = FastOutSlowInEasing),
        label = "sky-day-progress",
    )
    val palette = remember(progressAnim) { skyPaletteForDayProgress(progressAnim) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawLiveSky(palette = palette, dayProgress = progressAnim, timeSec = timeSec)
    }
}

fun localDayProgress(
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
): Float {
    val local = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return ((local.hour * 3600 + local.minute * 60 + local.second) / 86_400f).coerceIn(0f, 1f)
}

private data class SkyPalette(
    val zenith: Color,
    val mid: Color,
    val horizon: Color,
    val haze: Color,
    val ridgeFar: Color,
    val ridgeMid: Color,
    val ridgeNear: Color,
    val pine: Color,
    val sunCore: Color,
    val sunBloom: Color,
    val cloud: Color,
    val starAlpha: Float,
    val sunAlpha: Float,
    val hazeAlpha: Float,
)

private fun skyPaletteForDayProgress(t: Float): SkyPalette {
    val keys = floatArrayOf(0f, 0.229f, 0.292f, 0.417f, 0.688f, 0.771f, 0.833f, 1f)
    val zenith = listOf(
        Color(0xFF080C14), Color(0xFF1A2438), Color(0xFF4E6E90), Color(0xFF456784),
        Color(0xFF3A5A78), Color(0xFF3E3448), Color(0xFF10161F), Color(0xFF080C14),
    )
    val mid = listOf(
        Color(0xFF10161F), Color(0xFF3E5068), Color(0xFF7E96AC), Color(0xFF6E8AA0),
        Color(0xFF628096), Color(0xFFB88868), Color(0xFF222830), Color(0xFF10161F),
    )
    val horizon = listOf(
        Color(0xFF161C24), Color(0xFFB89880), Color(0xFFC8BCAE), Color(0xFFB8C6D0),
        Color(0xFFA8BAC6), Color(0xFFD0A078), Color(0xFF242830), Color(0xFF161C24),
    )
    val haze = listOf(
        Color(0xFF222830), Color(0xFFD8C0A8), Color(0xFFE0DCD4), Color(0xFFD4DEE4),
        Color(0xFFC8D4DC), Color(0xFFE8C0A0), Color(0xFF323640), Color(0xFF222830),
    )
    val ridgeFar = listOf(
        Color(0xFF243044), Color(0xFF5A6A7C), Color(0xFF7A8C9C), Color(0xFF6E8090),
        Color(0xFF647486), Color(0xFF4E4650), Color(0xFF222830), Color(0xFF243044),
    )
    val ridgeMid = listOf(
        Color(0xFF181E2A), Color(0xFF3E4C5C), Color(0xFF5A6A78), Color(0xFF506070),
        Color(0xFF485868), Color(0xFF383038), Color(0xFF161A22), Color(0xFF181E2A),
    )
    val ridgeNear = listOf(
        Color(0xFF0C1016), Color(0xFF262E38), Color(0xFF3A444E), Color(0xFF343E48),
        Color(0xFF2E3842), Color(0xFF201C22), Color(0xFF0A0E12), Color(0xFF0C1016),
    )
    val pine = listOf(
        Color(0xFF080C10), Color(0xFF1A2228), Color(0xFF2A3238), Color(0xFF262E34),
        Color(0xFF222A30), Color(0xFF161418), Color(0xFF06080C), Color(0xFF080C10),
    )
    val sunCore = listOf(
        Color(0xFFE4E8F0), Color(0xFFFFDCB0), Color(0xFFFFF2DC), Color(0xFFFFF6E8),
        Color(0xFFFFE8C8), Color(0xFFFFC080), Color(0xFFE4E8F0), Color(0xFFE4E8F0),
    )
    val sunBloom = listOf(
        Color(0xFF5A6888), Color(0xFFFFB888), Color(0xFFFFE4C0), Color(0xFFFFECD0),
        Color(0xFFFFD4A0), Color(0xFFFF9858), Color(0xFF5A5878), Color(0xFF5A6888),
    )
    val cloud = listOf(
        Color(0x14F0F4F8), Color(0x22FFE4CC), Color(0x28F4F4F0), Color(0x22F0F2F4),
        Color(0x1EF0F4F8), Color(0x22FFD0A8), Color(0x10E8ECF0), Color(0x14F0F4F8),
    )
    val starA = floatArrayOf(0.68f, 0.16f, 0.02f, 0f, 0.02f, 0.1f, 0.5f, 0.68f)
    val sunA = floatArrayOf(0.4f, 0.8f, 0.88f, 0.95f, 0.9f, 0.82f, 0.42f, 0.4f)
    val hazeA = floatArrayOf(0.3f, 0.5f, 0.34f, 0.24f, 0.28f, 0.45f, 0.32f, 0.3f)

    fun c(list: List<Color>) = sampleKeyed(t, keys, list)
    fun f(arr: FloatArray) = sampleKeyedF(t, keys, arr)

    return SkyPalette(
        zenith = c(zenith), mid = c(mid), horizon = c(horizon), haze = c(haze),
        ridgeFar = c(ridgeFar), ridgeMid = c(ridgeMid), ridgeNear = c(ridgeNear),
        pine = c(pine), sunCore = c(sunCore), sunBloom = c(sunBloom), cloud = c(cloud),
        starAlpha = f(starA), sunAlpha = f(sunA), hazeAlpha = f(hazeA),
    )
}

private fun sampleKeyed(t: Float, keys: FloatArray, colors: List<Color>): Color {
    val x = t.coerceIn(0f, 1f)
    for (i in 0 until keys.lastIndex) {
        if (x <= keys[i + 1]) {
            val span = (keys[i + 1] - keys[i]).coerceAtLeast(1e-4f)
            val u = ((x - keys[i]) / span).coerceIn(0f, 1f)
            val e = u * u * (3f - 2f * u)
            return lerp(colors[i], colors[i + 1], e)
        }
    }
    return colors.last()
}

private fun sampleKeyedF(t: Float, keys: FloatArray, values: FloatArray): Float {
    val x = t.coerceIn(0f, 1f)
    for (i in 0 until keys.lastIndex) {
        if (x <= keys[i + 1]) {
            val span = (keys[i + 1] - keys[i]).coerceAtLeast(1e-4f)
            val u = ((x - keys[i]) / span).coerceIn(0f, 1f)
            val e = u * u * (3f - 2f * u)
            return values[i] + (values[i + 1] - values[i]) * e
        }
    }
    return values.last()
}

private fun DrawScope.drawLiveSky(palette: SkyPalette, dayProgress: Float, timeSec: Float) {
    val w = size.width
    val h = size.height

    // Soft atmospheric sky
    drawRect(
        brush = Brush.verticalGradient(
            0.00f to palette.zenith,
            0.34f to palette.mid,
            0.66f to palette.horizon,
            1.00f to palette.haze,
        ),
    )

    // Soft sun / moon (kept calm, sits behind ridges)
    val arc = ((dayProgress - 0.20f) / 0.58f).coerceIn(0f, 1f)
    val sunX = w * (0.42f + arc * 0.38f) // bias right so left shade stays clean
    val sunY = h * (0.55f - sin(arc * PI.toFloat()) * 0.36f)
    val isNight = palette.starAlpha > 0.35f
    val sunR = if (isNight) w * 0.034f else w * 0.046f
    if (palette.sunAlpha > 0.05f) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to palette.sunBloom.copy(alpha = 0.28f * palette.sunAlpha),
                0.45f to palette.sunBloom.copy(alpha = 0.12f * palette.sunAlpha),
                1f to Color.Transparent,
                center = Offset(sunX, sunY),
                radius = sunR * 4.2f,
            ),
            radius = sunR * 4.2f,
            center = Offset(sunX, sunY),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to palette.sunCore.copy(alpha = 0.92f * palette.sunAlpha),
                0.7f to palette.sunCore.copy(alpha = 0.65f * palette.sunAlpha),
                1f to Color.Transparent,
                center = Offset(sunX, sunY),
                radius = sunR * 1.15f,
            ),
            radius = sunR * 1.15f,
            center = Offset(sunX, sunY),
        )
    }

    if (palette.starAlpha > 0.02f) {
        val sx = floatArrayOf(0.12f, 0.22f, 0.34f, 0.48f, 0.58f, 0.70f, 0.82f, 0.90f, 0.40f, 0.64f)
        val sy = floatArrayOf(0.08f, 0.14f, 0.06f, 0.12f, 0.09f, 0.15f, 0.07f, 0.11f, 0.18f, 0.05f)
        for (i in sx.indices) {
            val tw = 0.7f + 0.3f * sin(timeSec * (0.55f + i * 0.09f) + i).toFloat()
            drawCircle(
                color = Color(0xFFF0F2F6).copy(alpha = palette.starAlpha * tw * 0.55f),
                radius = (0.5f + (i % 3) * 0.18f) * (w / 540f),
                center = Offset(w * sx[i], h * sy[i]),
            )
        }
    }

    val drift = timeSec * 2.2f
    drawSoftCloud(Offset((drift % (w + 200f)) - 100f, h * 0.14f), w * 0.28f, h * 0.05f, palette.cloud)
    drawSoftCloud(
        Offset(((drift * 0.45f + w * 0.5f) % (w + 220f)) - 110f, h * 0.10f),
        w * 0.22f,
        h * 0.04f,
        palette.cloud.copy(alpha = palette.cloud.alpha * 0.6f),
    )

    // Layered organic ridges (multi-frequency silhouette — not simple arcs)
    drawOrganicRidge(
        w = w, h = h,
        baseY = 0.78f,
        amplitude = 0.30f,
        frequencies = floatArrayOf(1.1f, 2.7f, 5.4f, 9.2f),
        amplitudes = floatArrayOf(1f, 0.45f, 0.22f, 0.10f),
        phase = 0.35f,
        samples = 48,
        color = palette.ridgeFar,
        topAlpha = 0.18f,
        bottomAlpha = 0.48f,
    )
    drawMistBand(w, h, 0.50f, 0.66f, palette.haze.copy(alpha = palette.hazeAlpha * 0.32f))
    drawOrganicRidge(
        w = w, h = h,
        baseY = 0.86f,
        amplitude = 0.28f,
        frequencies = floatArrayOf(0.9f, 2.2f, 4.6f, 8.1f, 13f),
        amplitudes = floatArrayOf(1f, 0.55f, 0.28f, 0.14f, 0.06f),
        phase = 1.7f,
        samples = 56,
        color = palette.ridgeMid,
        topAlpha = 0.32f,
        bottomAlpha = 0.68f,
    )
    drawMistBand(w, h, 0.60f, 0.78f, palette.haze.copy(alpha = palette.hazeAlpha * 0.2f))
    drawOrganicRidge(
        w = w, h = h,
        baseY = 0.94f,
        amplitude = 0.14f,
        frequencies = floatArrayOf(0.7f, 1.8f, 3.9f, 7.2f),
        amplitudes = floatArrayOf(1f, 0.4f, 0.2f, 0.08f),
        phase = 2.9f,
        samples = 40,
        color = palette.ridgeNear,
        topAlpha = 0.75f,
        bottomAlpha = 0.92f,
    )
    // Soft lit crest lines on mid ridge (subtle sophistication)
    drawRidgeCrestHints(w, h, palette.haze.copy(alpha = 0.14f * palette.hazeAlpha.coerceAtLeast(0.2f)))

    // Soft pine cluster on far right (caravan forest cue)
    drawPineCluster(w, h, palette.pine)

    // Ground wash
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to Color.Transparent,
            0.82f to Color.Black.copy(alpha = 0.2f),
            1f to Color.Black.copy(alpha = 0.45f),
        ),
    )

    // Cinematic LEFT shade — text stage (strong left → clear right)
    drawRect(
        brush = Brush.horizontalGradient(
            0.00f to Color.Black.copy(alpha = 0.72f),
            0.22f to Color.Black.copy(alpha = 0.48f),
            0.42f to Color.Black.copy(alpha = 0.18f),
            0.58f to Color.Transparent,
            1.00f to Color.Transparent,
        ),
    )
    // Soft top-left depth
    drawRect(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = 0.28f),
            0.55f to Color.Black.copy(alpha = 0.08f),
            1f to Color.Transparent,
            center = Offset(0f, h * 0.55f),
            radius = w * 0.55f,
        ),
    )
}

private fun DrawScope.drawMistBand(w: Float, h: Float, y0: Float, y1: Float, color: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            y0 to Color.Transparent,
            ((y0 + y1) * 0.5f) to color,
            y1 to Color.Transparent,
            1f to Color.Transparent,
        ),
        size = Size(w, h),
    )
}

/**
 * Organic mountain mass: many samples + multi-frequency height so the silhouette
 * reads like terrain, not a few cubic blobs.
 */
private fun DrawScope.drawOrganicRidge(
    w: Float,
    h: Float,
    baseY: Float,
    amplitude: Float,
    frequencies: FloatArray,
    amplitudes: FloatArray,
    phase: Float,
    samples: Int,
    color: Color,
    topAlpha: Float,
    bottomAlpha: Float,
) {
    val n = samples.coerceAtLeast(12)
    val ys = FloatArray(n + 1)
    for (i in 0..n) {
        val t = i / n.toFloat()
        var elev = 0f
        var sumAmp = 0f
        for (k in frequencies.indices) {
            val a = amplitudes.getOrElse(k) { 0f }
            elev += a * sin((t * frequencies[k] + phase) * PI.toFloat() * 2f).toFloat()
            // asymmetric shoulders
            elev += a * 0.35f * sin((t * frequencies[k] * 0.5f + phase * 1.3f) * PI.toFloat()).toFloat()
            sumAmp += a
        }
        elev /= sumAmp.coerceAtLeast(1e-3f)
        // envelope: slightly lower near edges for natural framing
        val edge = sin(t * PI.toFloat()).toFloat().coerceIn(0.35f, 1f)
        ys[i] = h * (baseY - amplitude * elev.coerceIn(-1f, 1f) * 0.5f - amplitude * 0.35f * edge)
    }
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, ys[0])
        for (i in 0 until n) {
            val x0 = w * (i / n.toFloat())
            val x1 = w * ((i + 1) / n.toFloat())
            val xm = (x0 + x1) * 0.5f
            val y0 = ys[i]
            val y1 = ys[i + 1]
            // Smooth segment with slight mid control for organic feel
            val ym = (y0 + y1) * 0.5f + (y1 - y0) * 0.08f
            quadraticBezierTo(xm, ym, x1, y1)
        }
        lineTo(w, h)
        close()
    }
    drawPath(
        path,
        brush = Brush.verticalGradient(
            0f to color.copy(alpha = topAlpha),
            0.55f to color.copy(alpha = (topAlpha + bottomAlpha) * 0.5f),
            1f to color.copy(alpha = bottomAlpha),
        ),
    )
}

/** Soft crest highlights — thin luminous strokes along mid-distance peaks. */
private fun DrawScope.drawRidgeCrestHints(w: Float, h: Float, color: Color) {
    val crest = Path().apply {
        val n = 28
        for (i in 0..n) {
            val t = i / n.toFloat()
            val elev =
                sin((t * 2.2f + 1.7f) * PI.toFloat() * 2f).toFloat() * 0.55f +
                    sin((t * 4.6f + 0.4f) * PI.toFloat() * 2f).toFloat() * 0.28f +
                    sin((t * 8.1f + 2.1f) * PI.toFloat() * 2f).toFloat() * 0.12f
            val y = h * (0.62f - elev * 0.10f)
            val x = w * t
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    drawPath(
        path = crest,
        color = color,
        style = Stroke(
            width = h * 0.012f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawPineCluster(w: Float, h: Float, color: Color) {
    fun softPine(cx: Float, baseY: Float, height: Float, width: Float, alpha: Float) {
        // Layered triangles for softer tree mass (less hard “icon” pine)
        val layers = 4
        for (i in 0 until layers) {
            val t = i / (layers - 1f)
            val top = baseY - height * (1f - t * 0.72f)
            val bottom = baseY - height * (0.08f + (1f - t) * 0.08f)
            val half = width * (0.35f + t * 0.65f)
            val p = Path().apply {
                moveTo(cx, top)
                lineTo(cx + half, bottom)
                quadraticBezierTo(cx, bottom + height * 0.04f, cx - half, bottom)
                close()
            }
            drawPath(p, color = color.copy(alpha = alpha * (0.35f + t * 0.45f)))
        }
        // trunk hint
        drawRect(
            color = color.copy(alpha = alpha * 0.35f),
            topLeft = Offset(cx - width * 0.06f, baseY - height * 0.12f),
            size = Size(width * 0.12f, height * 0.12f),
        )
    }
    softPine(w * 0.86f, h * 0.93f, h * 0.22f, w * 0.04f, 0.4f)
    softPine(w * 0.90f, h * 0.94f, h * 0.30f, w * 0.05f, 0.5f)
    softPine(w * 0.935f, h * 0.945f, h * 0.36f, w * 0.055f, 0.58f)
    softPine(w * 0.965f, h * 0.95f, h * 0.26f, w * 0.042f, 0.48f)
    softPine(w * 0.988f, h * 0.955f, h * 0.20f, w * 0.032f, 0.4f)
}

private fun DrawScope.drawSoftCloud(origin: Offset, width: Float, height: Float, color: Color) {
    val layers = 4
    for (i in 0 until layers) {
        val t = i / (layers - 1f).coerceAtLeast(0.001f)
        val ox = origin.x + width * (0.1f + t * 0.5f + 0.03f * cos(i * 1.2f).toFloat())
        val oy = origin.y + height * (0.12f * sin(i * 1.1f).toFloat())
        val ww = width * (0.4f - t * 0.05f)
        val hh = height * (0.8f - t * 0.1f)
        drawOval(
            color = color.copy(alpha = color.alpha * (0.5f + 0.1f * (1f - t))),
            topLeft = Offset(ox - ww / 2f, oy - hh / 2f),
            size = Size(ww, hh),
        )
    }
}
