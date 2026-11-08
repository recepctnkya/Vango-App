package com.hextech.vango

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hextech.vango.profile.CaravanType
import com.hextech.vango.profile.LoginPreferences
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.phone_caravan_motor
import testvango.composeapp.generated.resources.phone_caravan_tow

/** Local-time bands (kept for labels / diagnostics). Live sky uses continuous day progress. */
enum class HomeTimePeriod {
    Morning, // 06:00–10:00
    Day,     // 10:00–17:00
    Evening, // 17:00–20:00
    Night,   // 20:00–06:00
}

fun resolveHomeTimePeriod(
    nowEpochMs: Long = Clock.System.now().toEpochMilliseconds(),
): HomeTimePeriod {
    val hour = Instant.fromEpochMilliseconds(nowEpochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour
    return when (hour) {
        in 6..9 -> HomeTimePeriod.Morning
        in 10..16 -> HomeTimePeriod.Day
        in 17..19 -> HomeTimePeriod.Evening
        else -> HomeTimePeriod.Night
    }
}

/** Bumps when caravan type is persisted so home UI recomposes. */
var caravanTypeRevision by mutableStateOf(0)
    private set

fun resolvedCaravanType(): CaravanType = when (
    LoginPreferences.getCachedCaravanType()?.trim()
) {
    CaravanType.TowCaravan.name -> CaravanType.TowCaravan
    CaravanType.MotorCaravan.name -> CaravanType.MotorCaravan
    else -> CaravanType.TowCaravan
}

fun persistCaravanType(type: CaravanType) {
    LoginPreferences.setCachedCaravanType(type.name)
    caravanTypeRevision++
}

fun phoneCaravanForeground(type: CaravanType): DrawableResource = when (type) {
    CaravanType.TowCaravan -> Res.drawable.phone_caravan_tow
    CaravanType.MotorCaravan -> Res.drawable.phone_caravan_motor
}
