package com.hextech.vango

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * Temporary: cycle Clear → Cloudy → Wind → Rain → Snow → Fog every 5s on the phone hero.
 * Set false to drive FX from live OpenWeather data.
 */
const val HERO_WEATHER_FX_TEST_CYCLE = false

enum class HeroWeatherFx {
    Clear,
    Cloudy,
    Wind,
    Rain,
    Snow,
    Fog,
}

fun HeroWeatherFx.displayLabelTr(): String = when (this) {
    HeroWeatherFx.Clear -> "Açık"
    HeroWeatherFx.Cloudy -> "Bulutlu"
    HeroWeatherFx.Wind -> "Rüzgarlı"
    HeroWeatherFx.Rain -> "Yağmur"
    HeroWeatherFx.Snow -> "Kar"
    HeroWeatherFx.Fog -> "Sis"
}

fun resolveHeroWeatherFx(info: WeatherInfo?): HeroWeatherFx {
    if (info == null) return HeroWeatherFx.Clear
    val id = info.conditionId
    val windy = info.windSpeedMs >= 8f
    return when {
        id in 200..599 -> HeroWeatherFx.Rain
        id in 600..699 -> HeroWeatherFx.Snow
        id in 700..799 -> HeroWeatherFx.Fog
        id == 800 -> if (windy) HeroWeatherFx.Wind else HeroWeatherFx.Clear
        id in 801..804 -> if (windy) HeroWeatherFx.Wind else HeroWeatherFx.Cloudy
        else -> HeroWeatherFx.Clear
    }
}

/**
 * Soft procedural weather FX over the photoreal timed hero frames.
 * Biased to the upper sky so the baked-in caravan stays readable.
 */
@Composable
fun PhoneWeatherFxOverlay(
    fx: HeroWeatherFx,
    modifier: Modifier = Modifier,
) {
    var timeSec by remember { mutableStateOf(0f) }
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
    if (fx == HeroWeatherFx.Clear) return

    Canvas(modifier = modifier.fillMaxSize()) {
        when (fx) {
            HeroWeatherFx.Clear -> Unit
            HeroWeatherFx.Cloudy -> drawCloudyFx(timeSec)
            HeroWeatherFx.Wind -> drawWindFx(timeSec)
            HeroWeatherFx.Rain -> drawRainFx(timeSec, windy = false)
            HeroWeatherFx.Snow -> drawSnowFx(timeSec)
            HeroWeatherFx.Fog -> drawFogFx(timeSec)
        }
    }
}

private fun DrawScope.drawCloudyFx(timeSec: Float) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF6A7380).copy(alpha = 0.22f),
            0.45f to Color(0xFF6A7380).copy(alpha = 0.10f),
            0.72f to Color.Transparent,
        ),
    )
    val drift = timeSec * 18f
    drawSoftFxCloud(Offset((drift % (w + 160f)) - 80f, h * 0.10f), w * 0.34f, h * 0.07f, 0.28f)
    drawSoftFxCloud(
        Offset(((drift * 0.55f + w * 0.42f) % (w + 180f)) - 90f, h * 0.18f),
        w * 0.28f,
        h * 0.055f,
        0.20f,
    )
    drawSoftFxCloud(
        Offset(((drift * 0.32f + w * 0.75f) % (w + 140f)) - 70f, h * 0.08f),
        w * 0.22f,
        h * 0.045f,
        0.16f,
    )
}

private fun DrawScope.drawWindFx(timeSec: Float) {
    val w = size.width
    val h = size.height
    drawCloudyFx(timeSec * 1.65f)
    val streakCount = 14
    for (i in 0 until streakCount) {
        val lane = (i + 1) / (streakCount + 1f)
        val y = h * (0.06f + lane * 0.42f)
        val phase = (timeSec * (1.4f + i * 0.07f) + i * 0.37f)
        val x = ((phase * w * 0.55f) % (w + 120f)) - 60f
        val len = w * (0.10f + (i % 4) * 0.03f)
        val alpha = 0.10f + (i % 3) * 0.04f
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(x, y),
            end = Offset(x + len, y - h * 0.008f),
            strokeWidth = 1.4f + (i % 3) * 0.4f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawRainFx(timeSec: Float, windy: Boolean) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF1A2430).copy(alpha = 0.18f),
            0.55f to Color(0xFF1A2430).copy(alpha = 0.08f),
            0.85f to Color.Transparent,
        ),
    )
    val count = 42
    val tilt = if (windy) 0.28f else 0.12f
    val speed = if (windy) 1.55f else 1.15f
    for (i in 0 until count) {
        val seedX = (i * 97 % 1000) / 1000f
        val seedY = (i * 53 % 1000) / 1000f
        val fall = ((timeSec * speed * 0.55f) + seedY) % 1.15f
        val x = w * ((seedX + fall * tilt) % 1f)
        val y = h * (fall * 0.92f - 0.08f)
        if (y > h * 0.82f) continue
        val len = h * (0.035f + (i % 4) * 0.008f)
        drawLine(
            color = Color(0xFFD8E6F2).copy(alpha = 0.22f + (i % 5) * 0.05f),
            start = Offset(x, y),
            end = Offset(x + len * tilt * 2.2f, y + len),
            strokeWidth = 1.3f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnowFx(timeSec: Float) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFD8E4F0).copy(alpha = 0.12f),
            0.5f to Color.Transparent,
        ),
    )
    val count = 36
    for (i in 0 until count) {
        val seedX = (i * 73 % 1000) / 1000f
        val seedY = (i * 41 % 1000) / 1000f
        val fall = ((timeSec * 0.18f) + seedY) % 1.2f
        val sway = sin((timeSec * 0.9f + i) * PI.toFloat()).toFloat() * w * 0.03f
        val x = w * seedX + sway
        val y = h * (fall * 0.88f - 0.05f)
        if (y > h * 0.85f) continue
        val r = (1.2f + (i % 4) * 0.7f) * (w / 420f).coerceIn(0.7f, 1.4f)
        drawCircle(
            color = Color.White.copy(alpha = 0.35f + (i % 3) * 0.12f),
            radius = r,
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawFogFx(timeSec: Float) {
    val w = size.width
    val h = size.height
    val pulse = 0.85f + 0.15f * sin(timeSec * 0.35f).toFloat()
    val drift = timeSec * 12f
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFE8EEF2).copy(alpha = 0.20f * pulse),
            0.35f to Color(0xFFD5DEE6).copy(alpha = 0.28f * pulse),
            0.62f to Color(0xFFC8D2DA).copy(alpha = 0.16f * pulse),
            0.82f to Color.Transparent,
        ),
    )
    drawSoftFxCloud(
        Offset((drift % (w + 200f)) - 100f, h * 0.28f),
        w * 0.55f,
        h * 0.12f,
        0.22f * pulse,
    )
    drawSoftFxCloud(
        Offset(((drift * 0.4f + w * 0.5f) % (w + 220f)) - 110f, h * 0.42f),
        w * 0.48f,
        h * 0.10f,
        0.18f * pulse,
    )
}

private fun DrawScope.drawSoftFxCloud(center: Offset, cw: Float, ch: Float, alpha: Float) {
    val c = Color.White.copy(alpha = alpha)
    drawOval(c, topLeft = Offset(center.x - cw * 0.5f, center.y - ch * 0.35f), size = Size(cw, ch))
    drawOval(
        c.copy(alpha = alpha * 0.85f),
        topLeft = Offset(center.x - cw * 0.28f, center.y - ch * 0.55f),
        size = Size(cw * 0.55f, ch * 0.9f),
    )
    drawOval(
        c.copy(alpha = alpha * 0.75f),
        topLeft = Offset(center.x + cw * 0.02f, center.y - ch * 0.48f),
        size = Size(cw * 0.42f, ch * 0.8f),
    )
}
