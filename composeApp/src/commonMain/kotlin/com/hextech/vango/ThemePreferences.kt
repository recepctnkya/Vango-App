package com.hextech.vango

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Persisted theme: "system", "dark", "blue", or "light" (null / unknown → dark).
 *
 * Default for tablet is DARK.
 * SYSTEM = follow Android OS appearance for the app UI.
 * LIGHT/DARK/BLUE = manual lock. When pushing device panel prefs to IO Module,
 * only manual values write SoT; system does not overwrite IO.
 * Display/other clients apply IO SoT only while they themselves are in system mode.
 */
expect object ThemePreferences {
    fun getThemeMode(): String?
    fun setThemeMode(mode: String)
    fun getAccentId(): String?
    fun setAccentId(id: String)
    fun getCardStyle(): String?
    fun setCardStyle(style: String)
    fun getBrightnessPercent(): Int
    fun setBrightnessPercent(percent: Int)
    fun getAutoBrightness(): Boolean
    fun setAutoBrightness(enabled: Boolean)
}

enum class AppThemeMode {
    SYSTEM,
    DARK,
    BLUE,
    LIGHT,
}

/** Card fill style used across phone redesign surfaces. */
enum class AppCardStyle {
    /** Solid card backgrounds (Dolu). */
    STANDARD,
    /** Semi-transparent cards (Yarı Saydam). */
    MEDIUM,
    /** Near-transparent / outline cards (Şeffaf). */
    SOFT,
}

var customAppThemeMode by mutableStateOf(AppThemeMode.DARK)
var customAppAccentId by mutableStateOf("vango")
var customAppCardStyle by mutableStateOf(AppCardStyle.STANDARD)
var customAppBrightnessPercent by mutableStateOf(70)
var customAppAutoBrightness by mutableStateOf(true)

fun applyAppThemeMode(mode: AppThemeMode, fromServer: Boolean = false) {
    val value = when (mode) {
        AppThemeMode.SYSTEM -> "system"
        AppThemeMode.DARK -> "dark"
        AppThemeMode.BLUE -> "blue"
        AppThemeMode.LIGHT -> "light"
    }
    ThemePreferences.setThemeMode(value)
    customAppThemeMode = mode
    if (!fromServer) {
        com.hextech.vango.profile.ThemeLocalOverride.noteLocalWrite()
        com.hextech.vango.profile.ThemeServerPush.scheduleCurrentAppearance()
    }
}

val VALID_THEME_ACCENT_IDS = setOf(
    "vango",
    "blue",
    "purple",
    "pink",
    "orange",
    "yellow",
    "lime",
    "green",
    "red", // legacy
)

fun normalizeThemeAccentId(raw: String?): String {
    val t = raw?.trim()?.lowercase().orEmpty()
    return if (t in VALID_THEME_ACCENT_IDS) t else "vango"
}

fun applyAppAccent(id: String, fromServer: Boolean = false) {
    val normalized = normalizeThemeAccentId(id)
    ThemePreferences.setAccentId(normalized)
    customAppAccentId = normalized
    if (!fromServer) {
        com.hextech.vango.profile.ThemeLocalOverride.noteLocalWrite()
        com.hextech.vango.profile.ThemeServerPush.scheduleCurrentAppearance()
    }
}

fun loadAppAccentFromStorage() {
    customAppAccentId = ThemePreferences.getAccentId()?.trim()?.lowercase()?.ifEmpty { null } ?: "vango"
}

fun accentColorFromId(id: String): Color = when (id.trim().lowercase()) {
    "blue" -> Color(0xFF3B82F6)
    "purple" -> Color(0xFF7C3AED)
    "pink" -> Color(0xFFEC4899)
    "orange" -> Color(0xFFF97316)
    "yellow" -> Color(0xFFEAB308)
    "lime" -> Color(0xFF84CC16)
    "green" -> Color(0xFF10B981)
    "red" -> Color(0xFFEF4444)
    else -> Color(0xFF2EC5FF)
}

fun appThemeModeFromStorage(value: String?): AppThemeMode = when (value?.trim()?.lowercase()) {
    "dark" -> AppThemeMode.DARK
    "blue" -> AppThemeMode.BLUE
    "light" -> AppThemeMode.LIGHT
    "system" -> AppThemeMode.SYSTEM
    else -> AppThemeMode.DARK
}

fun cardStyleFromStorage(value: String?): AppCardStyle = when (value?.trim()?.lowercase()) {
    "medium", "semi", "semi_transparent", "semi-transparent" -> AppCardStyle.MEDIUM
    "soft", "transparent", "outline" -> AppCardStyle.SOFT
    else -> AppCardStyle.STANDARD
}

fun cardStyleStorageValue(style: AppCardStyle): String = when (style) {
    AppCardStyle.STANDARD -> "filled"
    AppCardStyle.MEDIUM -> "semi"
    AppCardStyle.SOFT -> "transparent"
}

fun applyAppCardStyle(style: AppCardStyle) {
    ThemePreferences.setCardStyle(cardStyleStorageValue(style))
    customAppCardStyle = style
}

fun loadAppCardStyleFromStorage() {
    customAppCardStyle = cardStyleFromStorage(ThemePreferences.getCardStyle())
}

/** Shared corner radius; fill style no longer changes radius. */
fun appCardCornerRadius(style: AppCardStyle = customAppCardStyle): Dp = 16.dp

/** Surface alpha for card / tile backgrounds. */
fun appCardFillAlpha(style: AppCardStyle = customAppCardStyle): Float = when (style) {
    AppCardStyle.STANDARD -> 1f
    AppCardStyle.MEDIUM -> 0.55f
    AppCardStyle.SOFT -> 0.12f
}

fun applyAppBrightness(percent: Int, auto: Boolean) {
    val clamped = percent.coerceIn(5, 100)
    ThemePreferences.setBrightnessPercent(clamped)
    ThemePreferences.setAutoBrightness(auto)
    customAppBrightnessPercent = clamped
    customAppAutoBrightness = auto
    ScreenBrightnessController.apply(clamped, auto)
}

fun loadAppBrightnessFromStorage() {
    customAppBrightnessPercent = ThemePreferences.getBrightnessPercent().coerceIn(5, 100)
    customAppAutoBrightness = ThemePreferences.getAutoBrightness()
    ScreenBrightnessController.apply(customAppBrightnessPercent, customAppAutoBrightness)
}

fun restoreDefaultThemeAppearance() {
    applyAppThemeMode(AppThemeMode.DARK)
    applyAppAccent("vango")
    applyAppCardStyle(AppCardStyle.STANDARD)
    applyAppBrightness(70, auto = true)
}

expect object ScreenBrightnessController {
    fun apply(percent: Int, auto: Boolean)
}
