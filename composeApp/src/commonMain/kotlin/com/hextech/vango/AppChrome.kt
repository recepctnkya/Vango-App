package com.hextech.vango

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppChromeColors(
    val isLight: Boolean,
    val bg: Color,
    val panel: Color,
    val card: Color,
    val tile: Color,
    val field: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val track: Color,
    val infoBg: Color,
    val ink: Color,
    val glass: Color,
)

val AppChromeDark = AppChromeColors(
    isLight = false,
    bg = Color(0xFF070B14),
    panel = Color(0xFF0E1522),
    card = Color(0xFF101826),
    tile = Color(0xFF0C1524),
    field = Color(0xFF0A1220),
    text = Color(0xFFF2F6FC),
    muted = Color(0xFF8B9BB2),
    border = Color.White.copy(alpha = 0.10f),
    track = Color(0xFF1C2738),
    infoBg = Color(0xFF123048),
    ink = Color(0xFF061018),
    glass = Color(0xFF0B1220),
)

val AppChromeBlue = AppChromeDark.copy(
    bg = Color(0xFF070D18),
    panel = Color(0xFF0D1728),
    card = Color(0xFF111A2A),
    tile = Color(0xFF0C182C),
)

val AppChromeLight = AppChromeColors(
    isLight = true,
    bg = Color(0xFFEEF2F7),
    panel = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    tile = Color(0xFFF4F7FB),
    field = Color(0xFFF0F4FA),
    text = Color(0xFF1A2332),
    muted = Color(0xFF5B6B80),
    border = Color(0xFF1A2332).copy(alpha = 0.12f),
    track = Color(0xFFD5DEEA),
    infoBg = Color(0xFFE7F1F8),
    ink = Color(0xFF061018),
    glass = Color(0xFFE2E8F0),
)

val LocalAppChrome = compositionLocalOf { AppChromeDark }

@Composable
fun rememberAppChromeColors(): AppChromeColors {
    val mode = customAppThemeMode
    val systemLight = MaterialTheme.colors.isLight
    val base = when (mode) {
        AppThemeMode.LIGHT -> AppChromeLight
        AppThemeMode.BLUE -> AppChromeBlue
        AppThemeMode.SYSTEM -> if (systemLight) AppChromeLight else AppChromeDark
        else -> AppChromeDark
    }
    val fill = appCardFillAlpha(customAppCardStyle)
    if (fill >= 0.99f) return base
    return base.copy(
        card = base.card.copy(alpha = fill),
        tile = base.tile.copy(alpha = fill),
        panel = base.panel.copy(alpha = (fill + 0.25f).coerceAtMost(1f)),
        field = base.field.copy(alpha = (fill + 0.15f).coerceAtMost(1f)),
        glass = base.glass.copy(alpha = fill.coerceAtLeast(0.08f)),
    )
}

@Composable
fun ProvideAppChrome(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppChrome provides rememberAppChromeColors()) {
        content()
    }
}
