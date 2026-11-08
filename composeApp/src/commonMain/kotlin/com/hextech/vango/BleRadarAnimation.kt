package com.hextech.vango

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BleRadarIndicator(
    modifier: Modifier = Modifier,
    accent: Color,
    active: Boolean,
    centerIcon: @Composable () -> Unit = {
        Icon(
            Icons.Filled.Bluetooth,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp),
        )
    },
) {
    val transition = rememberInfiniteTransition(label = "ble_radar")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 2200 else 4500, easing = LinearEasing),
        ),
        label = "rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 900 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = if (active) 0.3f else 0.2f,
        targetValue = if (active) 0.9f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 900 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringAlpha",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.92f
            val sweepEnd = center + Offset(radius * 0.85f, 0f)
            drawLine(
                color = accent.copy(alpha = if (active) 0.85f else 0.45f),
                start = center,
                end = sweepEnd,
                strokeWidth = 3f,
            )
            drawCircle(
                color = accent.copy(alpha = if (active) 0.25f else 0.12f),
                radius = radius * 0.2f,
                center = sweepEnd,
            )
        }
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size((52 + i * 14).dp)
                    .scale(pulse)
                    .alpha((ringAlpha - i * 0.12f).coerceAtLeast(0.12f))
                    .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(0.98f + (pulse - 1f) * 0.15f)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            centerIcon()
        }
    }
}