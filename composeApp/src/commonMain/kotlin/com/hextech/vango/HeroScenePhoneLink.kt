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
import androidx.compose.ui.graphics.PathEffect
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

/** Hero phone — larger outline with a mini control-panel screen (buttons / tiles). */
internal fun DrawScope.drawPhoneIcon(center: Offset, size: Float) {
    val body = Size(size * 0.62f, size)
    val left = center.x - body.width / 2f
    val top = center.y - body.height / 2f
    val stroke = 2.1.dp.toPx()
    val frameCr = CornerRadius(size * 0.12f, size * 0.12f)

    // Bezel fill (dark glass) + white outline
    drawRoundRect(
        color = Color(0xFF1A2430).copy(alpha = 0.82f),
        topLeft = Offset(left, top),
        size = body,
        cornerRadius = frameCr,
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(left, top),
        size = body,
        cornerRadius = frameCr,
        style = Stroke(width = stroke),
    )
    // Notch / speaker bar
    drawRoundRect(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(center.x - size * 0.10f, top + size * 0.055f),
        size = Size(size * 0.20f, size * 0.028f),
        cornerRadius = CornerRadius(size * 0.02f, size * 0.02f),
    )

    // Screen inset
    val screenL = left + body.width * 0.12f
    val screenT = top + size * 0.14f
    val screenW = body.width * 0.76f
    val screenH = body.height * 0.68f
    drawRoundRect(
        color = Color(0xFF0E1620).copy(alpha = 0.95f),
        topLeft = Offset(screenL, screenT),
        size = Size(screenW, screenH),
        cornerRadius = CornerRadius(size * 0.045f, size * 0.045f),
    )

    // Status strip
    drawRoundRect(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(screenL + screenW * 0.12f, screenT + screenH * 0.08f),
        size = Size(screenW * 0.76f, screenH * 0.06f),
        cornerRadius = CornerRadius(size * 0.015f, size * 0.015f),
    )

    // 2×2 control tiles (button-like)
    val pad = screenW * 0.10f
    val gap = screenW * 0.08f
    val tileW = (screenW - pad * 2f - gap) / 2f
    val tileH = screenH * 0.22f
    val gridTop = screenT + screenH * 0.22f
    val tileCols = listOf(
        Color(0xFF3D9BE8),
        Color(0xFF2ECC71),
        Color(0xFFF5A623),
        Color(0xFF8B9BB0),
    )
    for (i in 0 until 4) {
        val col = i % 2
        val row = i / 2
        val tx = screenL + pad + col * (tileW + gap)
        val ty = gridTop + row * (tileH + gap * 0.85f)
        val cr = CornerRadius(size * 0.035f, size * 0.035f)
        drawRoundRect(
            color = tileCols[i].copy(alpha = 0.88f),
            topLeft = Offset(tx, ty),
            size = Size(tileW, tileH),
            cornerRadius = cr,
        )
        // Tiny icon mark inside tile
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = min(tileW, tileH) * 0.16f,
            center = Offset(tx + tileW * 0.5f, ty + tileH * 0.45f),
        )
    }

    // Bottom nav pills
    val navY = screenT + screenH * 0.82f
    val navH = screenH * 0.08f
    val navW = screenW * 0.18f
    val navGap = screenW * 0.06f
    val navStart = screenL + (screenW - (navW * 3f + navGap * 2f)) * 0.5f
    for (i in 0 until 3) {
        drawRoundRect(
            color = Color.White.copy(alpha = if (i == 1) 0.55f else 0.28f),
            topLeft = Offset(navStart + i * (navW + navGap), navY),
            size = Size(navW, navH),
            cornerRadius = CornerRadius(navH * 0.45f, navH * 0.45f),
        )
    }

    // Home indicator
    drawRoundRect(
        color = Color.White.copy(alpha = 0.75f),
        topLeft = Offset(center.x - size * 0.09f, top + body.height - size * 0.085f),
        size = Size(size * 0.18f, size * 0.022f),
        cornerRadius = CornerRadius(size * 0.02f, size * 0.02f),
    )
}

internal fun DrawScope.drawConnectionLine(
    from: Offset,
    to: Offset,
    color: Color,
    visual: HeroLinkVisual,
    flowT: Float,
    pulse: Float,
) {
    // Stronger than ambient nature FX so link status stays primary
    val stroke = 2.15.dp.toPx()
    when (visual) {
        HeroLinkVisual.Connected -> {
            drawLine(
                color = color.copy(alpha = 0.28f * pulse),
                start = from,
                end = to,
                strokeWidth = stroke * 2.4f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color.copy(alpha = (0.95f * pulse).coerceAtMost(1f)),
                start = from,
                end = to,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            for (i in 0..2) {
                val t = ((flowT + i / 3f) % 1f)
                val x = from.x + (to.x - from.x) * t
                val y = from.y + (to.y - from.y) * t
                drawCircle(color.copy(alpha = 0.95f), radius = 2.2.dp.toPx(), center = Offset(x, y))
            }
            drawCircle(color.copy(alpha = 0.95f), radius = 2.8.dp.toPx(), center = from)
            drawCircle(color.copy(alpha = 0.95f), radius = 2.8.dp.toPx(), center = to)
        }
        HeroLinkVisual.Connecting -> {
            drawLine(
                color = color.copy(alpha = 0.22f * pulse),
                start = from,
                end = to,
                strokeWidth = stroke * 2.2f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color.copy(alpha = 0.92f * pulse),
                start = from,
                end = to,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 7.dp.toPx()), 0f),
            )
            drawCircle(color.copy(alpha = 0.92f), radius = 2.6.dp.toPx(), center = from)
            drawCircle(color.copy(alpha = 0.92f), radius = 2.6.dp.toPx(), center = to)
        }
        HeroLinkVisual.Disconnected -> {
            drawLine(
                color = color.copy(alpha = 0.58f),
                start = from,
                end = to,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 9.dp.toPx()), 0f),
            )
            drawCircle(color.copy(alpha = 0.62f), radius = 2.3.dp.toPx(), center = from)
            drawCircle(color.copy(alpha = 0.62f), radius = 2.3.dp.toPx(), center = to)
        }
    }
}
