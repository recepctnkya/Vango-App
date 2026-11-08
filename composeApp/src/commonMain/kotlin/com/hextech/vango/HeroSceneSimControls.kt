package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Developer-only simulation strip. Never compose in release (caller gates with [isAppDebugBuild]).
 */
@Composable
fun HeroSceneSimControls(
    snap: HeroSimSnapshot,
    onToggleRun: () -> Unit,
    onReset: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleMode: () -> Unit,
    onStop: () -> Unit,
    onSpringPreview: () -> Unit = {},
    onSummerPreview: () -> Unit = {},
    onAutumnPreview: () -> Unit = {},
    onWinterPreview: () -> Unit = {},
    onWeatherForce: (HeroSimWeatherForce) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xFF0B1220).copy(alpha = 0.92f)
    val border = Color(0xFF3A4A60)
    val accent = Color(0xFF5EC4C8)
    val muted = Color.White.copy(alpha = 0.70f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "SIM · ${HeroSceneSimulation.formatDate(snap.local)} ${snap.clockLabel} · " +
                "${snap.season.name} · ${snap.scenarioLabel}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = when (snap.mode) {
                HeroSimCalendarMode.FastSeason -> "Takvim: Hızlı mevsim turu"
                HeroSimCalendarMode.Normal -> "Takvim: Normal"
            } + if (snap.running) " · çalışıyor" else " · duraklatıldı",
            color = muted,
            fontSize = 10.sp,
            maxLines = 1,
        )
        SimChipRow {
            SimChip(
                label = if (!snap.active || !snap.running) "Başlat" else "Duraklat",
                accent = accent,
                onClick = onToggleRun,
            )
            SimChip(label = "Sıfırla", accent = accent, onClick = onReset)
            SimChip(label = "Önceki Ay", accent = accent, onClick = onPrevMonth)
            SimChip(label = "Sonraki Ay", accent = accent, onClick = onNextMonth)
            SimChip(
                label = if (snap.mode == HeroSimCalendarMode.FastSeason) "Normal" else "Mevsim",
                accent = accent,
                onClick = onToggleMode,
            )
            if (snap.active) {
                SimChip(label = "Kapat", accent = Color(0xFFE57373), onClick = onStop)
            }
        }
        SimChipRow {
            SimChip(
                label = "İlkbahar",
                accent = accent,
                selected = snap.season == SeasonState.Spring &&
                    snap.weatherForce == HeroSimWeatherForce.Auto,
                onClick = onSpringPreview,
            )
            SimChip(
                label = "Yaz",
                accent = accent,
                selected = snap.season == SeasonState.Summer &&
                    snap.weatherForce == HeroSimWeatherForce.Auto,
                onClick = onSummerPreview,
            )
            SimChip(
                label = "Sonbahar",
                accent = accent,
                selected = snap.season == SeasonState.Autumn &&
                    snap.weatherForce == HeroSimWeatherForce.Auto,
                onClick = onAutumnPreview,
            )
            SimChip(
                label = "Kış",
                accent = accent,
                selected = snap.season == SeasonState.Winter &&
                    snap.weatherForce == HeroSimWeatherForce.Auto,
                onClick = onWinterPreview,
            )
        }
        SimChipRow {
            WeatherForceChip(
                label = "Auto",
                force = HeroSimWeatherForce.Auto,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
            WeatherForceChip(
                label = "Açık",
                force = HeroSimWeatherForce.Clear,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
            WeatherForceChip(
                label = "Parçalı",
                force = HeroSimWeatherForce.Partly,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
            WeatherForceChip(
                label = "Bulutlu",
                force = HeroSimWeatherForce.Overcast,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
            WeatherForceChip(
                label = "Yağmur",
                force = HeroSimWeatherForce.Rain,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
            WeatherForceChip(
                label = "Kar",
                force = HeroSimWeatherForce.Snow,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
            WeatherForceChip(
                label = "Sis",
                force = HeroSimWeatherForce.Fog,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
            WeatherForceChip(
                label = "Rüzgâr",
                force = HeroSimWeatherForce.Windy,
                current = snap.weatherForce,
                accent = accent,
                onWeatherForce = onWeatherForce,
            )
        }
    }
}

@Composable
private fun SimChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Composable
private fun WeatherForceChip(
    label: String,
    force: HeroSimWeatherForce,
    current: HeroSimWeatherForce,
    accent: Color,
    onWeatherForce: (HeroSimWeatherForce) -> Unit,
) {
    SimChip(
        label = label,
        accent = accent,
        selected = current == force,
        onClick = { onWeatherForce(force) },
    )
}

@Composable
private fun SimChip(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val fill = if (selected) accent.copy(alpha = 0.55f) else accent.copy(alpha = 0.22f)
    val stroke = if (selected) accent else accent.copy(alpha = 0.45f)
    Text(
        text = label,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .background(fill, RoundedCornerShape(6.dp))
            .border(1.dp, stroke, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}
