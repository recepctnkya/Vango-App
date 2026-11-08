package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.tablet_landscape_battery
import testvango.composeapp.generated.resources.tablet_landscape_battery_unknown
import testvango.composeapp.generated.resources.weekday_friday
import testvango.composeapp.generated.resources.weekday_monday
import testvango.composeapp.generated.resources.weekday_saturday
import testvango.composeapp.generated.resources.weekday_sunday
import testvango.composeapp.generated.resources.weekday_thursday
import testvango.composeapp.generated.resources.weekday_tuesday
import testvango.composeapp.generated.resources.weekday_wednesday

@Composable
private fun weekdayString(day: DayOfWeek): String {
    val res = when (day) {
        DayOfWeek.MONDAY -> Res.string.weekday_monday
        DayOfWeek.TUESDAY -> Res.string.weekday_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.weekday_wednesday
        DayOfWeek.THURSDAY -> Res.string.weekday_thursday
        DayOfWeek.FRIDAY -> Res.string.weekday_friday
        DayOfWeek.SATURDAY -> Res.string.weekday_saturday
        DayOfWeek.SUNDAY -> Res.string.weekday_sunday
        else -> error("Unsupported day of week: $day")
    }
    return stringResource(res)
}

@Composable
fun TabletLandscapeInfoBar(modifier: Modifier = Modifier) {
    var now by remember {
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
    }
    var batteryPct by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        batteryPct = getBatteryLevelPercent()
        var tick = 0
        while (true) {
            now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            if (tick % 30 == 0) {
                batteryPct = getBatteryLevelPercent()
            }
            tick++
            delay(1000L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = weekdayString(now.dayOfWeek),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatDate(now),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTime(now),
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = batteryPct?.let { stringResource(Res.string.tablet_landscape_battery, it) }
                    ?: stringResource(Res.string.tablet_landscape_battery_unknown),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatDate(dt: LocalDateTime): String {
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    return "$d/$m/${dt.year}"
}

private fun formatTime(dt: LocalDateTime): String {
    val h = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "$h:$min"
}
