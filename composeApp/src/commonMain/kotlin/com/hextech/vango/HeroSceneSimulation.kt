package com.hextech.vango

import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Dev-only hero scene simulation.
 * Feeds the same [WeatherInfo] + Instant path as live data — does not touch
 * [HeroWeatherRepository] cache, BLE, or the device clock.
 */
enum class HeroSimCalendarMode {
    /** 23→00 advances to the next real calendar day. */
    Normal,
    /** 23→00 jumps to the 15th of the next month (season tour). Default. */
    FastSeason,
}

/** Manual weather override for SIM QA. Auto = calendar scenario. */
enum class HeroSimWeatherForce {
    Auto,
    Clear,
    Partly,
    Overcast,
    Rain,
    Snow,
    Fog,
    Windy,
}

data class HeroSimSnapshot(
    val active: Boolean,
    val running: Boolean,
    val mode: HeroSimCalendarMode,
    val local: LocalDateTime,
    val weather: WeatherInfo,
    val season: SeasonState,
    val scenarioLabel: String,
    val weatherForce: HeroSimWeatherForce,
    val clockLabel: String,
)

object HeroSceneSimulation {
    const val IstanbulLat = 41.0082
    const val IstanbulLon = 28.9784
    val IstanbulTz: TimeZone = TimeZone.of("Europe/Istanbul")

    /** Real ms per virtual hour step. */
    const val StepRealMs: Long = 2_000L

    /** Cross-fade for light/weather on each virtual hour (≤ step). */
    val SceneEaseMs: Int get() = HeroSceneTransitionConfig.HourEaseMs

    /** Cross-fade for month / season preview jumps. */
    val SeasonEaseMs: Int get() = HeroSceneTransitionConfig.SeasonEaseMs

    /** Last applied local month — dashboard uses to pick hour vs season ease. */
    @Volatile
    var lastEaseMonth: Int = -1

    private val StartLocal = LocalDateTime(2025, 1, 15, 0, 0, 0, 0)

    private val _active = MutableStateFlow(false)
    private val _running = MutableStateFlow(false)
    private val _mode = MutableStateFlow(HeroSimCalendarMode.FastSeason)
    private val _local = MutableStateFlow(StartLocal)
    private val _weatherForce = MutableStateFlow(HeroSimWeatherForce.Auto)

    val active: StateFlow<Boolean> = _active.asStateFlow()
    val running: StateFlow<Boolean> = _running.asStateFlow()
    val mode: StateFlow<HeroSimCalendarMode> = _mode.asStateFlow()
    val local: StateFlow<LocalDateTime> = _local.asStateFlow()
    val weatherForce: StateFlow<HeroSimWeatherForce> = _weatherForce.asStateFlow()

    fun snapshot(): HeroSimSnapshot {
        val loc = _local.value
        val wx = weatherFor(loc)
        val season = resolveSeasonState(loc, IstanbulLat)
        val force = _weatherForce.value
        return HeroSimSnapshot(
            active = _active.value,
            running = _running.value,
            mode = _mode.value,
            local = loc,
            weather = wx,
            season = season,
            scenarioLabel = if (force == HeroSimWeatherForce.Auto) {
                scenarioLabelFor(loc.monthNumber, loc.hour)
            } else {
                forcedScenario(force, loc).label
            },
            weatherForce = force,
            clockLabel = formatClock(loc),
        )
    }

    fun instant(): Instant = _local.value.toInstant(IstanbulTz)

    fun start() {
        if (!isAppDebugBuild()) return
        _active.value = true
        _running.value = true
    }

    fun pause() {
        _running.value = false
    }

    fun resume() {
        if (_active.value) _running.value = true
    }

    fun toggleRun() {
        if (!_active.value) {
            start()
            return
        }
        _running.value = !_running.value
    }

    /** Leave simulation — live weather/time path resumes; cache untouched. */
    fun stop() {
        _active.value = false
        _running.value = false
        _weatherForce.value = HeroSimWeatherForce.Auto
    }

    fun reset() {
        if (!isAppDebugBuild()) return
        _local.value = StartLocal
        _weatherForce.value = HeroSimWeatherForce.Auto
        _active.value = true
        _running.value = true
    }

    fun setWeatherForce(force: HeroSimWeatherForce) {
        if (!isAppDebugBuild()) return
        _weatherForce.value = force
        _active.value = true
        _running.value = false
    }

    fun setMode(mode: HeroSimCalendarMode) {
        _mode.value = mode
    }

    fun toggleMode() {
        _mode.value = when (_mode.value) {
            HeroSimCalendarMode.FastSeason -> HeroSimCalendarMode.Normal
            HeroSimCalendarMode.Normal -> HeroSimCalendarMode.FastSeason
        }
    }

    /** Jump to the 15th 00:00 of the next month (keeps year wrap). */
    fun nextMonth() {
        if (!isAppDebugBuild()) return
        val cur = _local.value
        val nextMonth = if (cur.monthNumber == 12) 1 else cur.monthNumber + 1
        val nextYear = if (cur.monthNumber == 12) cur.year + 1 else cur.year
        _local.value = LocalDateTime(nextYear, nextMonth, 15, 0, 0, 0, 0)
        _active.value = true
    }

    /** Jump to the 15th 00:00 of the previous month (keeps year wrap). */
    fun prevMonth() {
        if (!isAppDebugBuild()) return
        val cur = _local.value
        val prevMonth = if (cur.monthNumber == 1) 12 else cur.monthNumber - 1
        val prevYear = if (cur.monthNumber == 1) cur.year - 1 else cur.year
        _local.value = LocalDateTime(prevYear, prevMonth, 15, 0, 0, 0, 0)
        _active.value = true
    }

    /** Static spring reference: 15 Apr 12:00, partly cloudy (for visual QA). */
    fun jumpToSpringPreview() {
        if (!isAppDebugBuild()) return
        _weatherForce.value = HeroSimWeatherForce.Auto
        _local.value = LocalDateTime(2025, 4, 15, 12, 0, 0, 0)
        _active.value = true
        _running.value = false
    }

    /** Static summer reference: 15 Jul 12:00, sunny 29°C (for visual QA). */
    fun jumpToSummerPreview() {
        if (!isAppDebugBuild()) return
        _weatherForce.value = HeroSimWeatherForce.Auto
        _local.value = LocalDateTime(2025, 7, 15, 12, 0, 0, 0)
        _active.value = true
        _running.value = false
    }

    /** Static autumn reference: 15 Oct 12:00, partly cloudy 16°C, light wind (QA). */
    fun jumpToAutumnPreview() {
        if (!isAppDebugBuild()) return
        _weatherForce.value = HeroSimWeatherForce.Auto
        _local.value = LocalDateTime(2025, 10, 15, 12, 0, 0, 0)
        _active.value = true
        _running.value = false
    }

    /** Static winter reference: 15 Jan 12:00, −2°C, snowing, light wind (QA). */
    fun jumpToWinterPreview() {
        if (!isAppDebugBuild()) return
        _weatherForce.value = HeroSimWeatherForce.Auto
        _local.value = LocalDateTime(2025, 1, 15, 12, 0, 0, 0)
        _active.value = true
        _running.value = false
    }

    /**
     * Advance one virtual hour. Call once per real [StepRealMs] while running
     * and app resumed — never batch catch-up after background.
     */
    fun tickOneHour() {
        if (!_active.value || !_running.value) return
        val cur = _local.value
        _local.value = when {
            cur.hour < 23 -> LocalDateTime(
                cur.year, cur.monthNumber, cur.dayOfMonth,
                cur.hour + 1, 0, 0, 0,
            )
            _mode.value == HeroSimCalendarMode.FastSeason -> {
                val nextMonth = if (cur.monthNumber == 12) 1 else cur.monthNumber + 1
                val nextYear = if (cur.monthNumber == 12) cur.year + 1 else cur.year
                LocalDateTime(nextYear, nextMonth, 15, 0, 0, 0, 0)
            }
            else -> {
                // Normal: next calendar day 00:00 via Instant + 1h
                val next = cur.toInstant(IstanbulTz)
                    .toEpochMilliseconds() + 3_600_000L
                Instant.fromEpochMilliseconds(next).toLocalDateTime(IstanbulTz)
                    .let { LocalDateTime(it.year, it.monthNumber, it.dayOfMonth, 0, 0, 0, 0) }
            }
        }
    }

    fun formatClock(local: LocalDateTime = _local.value): String =
        local.hour.toString().padStart(2, '0') + ":" +
            local.minute.toString().padStart(2, '0')

    fun formatDate(local: LocalDateTime = _local.value): String =
        local.dayOfMonth.toString().padStart(2, '0') + "." +
            local.monthNumber.toString().padStart(2, '0') + "." +
            local.year

    /**
     * Deterministic test weather by month + hour band.
     * Same start → same loop. Not a forecast model.
     */
    fun weatherFor(local: LocalDateTime): WeatherInfo {
        val force = _weatherForce.value
        val scenario = if (force == HeroSimWeatherForce.Auto) {
            scenarioFor(local.monthNumber, local.hour)
        } else {
            forcedScenario(force, local)
        }
        return WeatherInfo(
            city = "İstanbul",
            temperature = scenario.tempC,
            description = scenario.description,
            humidity = scenario.humidity,
            conditionId = scenario.conditionId,
            windSpeedMs = scenario.windMs,
            windDeg = scenario.windDeg,
            snowMm1h = scenario.snowMm1h,
            cloudPct = scenario.cloudPct,
            visibilityMeters = scenario.visibilityM,
            sunriseEpochSec = null, // approximateSolarTimes from date+lat
            sunsetEpochSec = null,
            latitude = IstanbulLat,
            longitude = IstanbulLon,
            fetchedAtEpochMs = 0L, // never write to live cache
        )
    }

    fun scenarioLabelFor(month: Int, hour: Int): String = scenarioFor(month, hour).label

    private fun forcedScenario(force: HeroSimWeatherForce, local: LocalDateTime): SimWx {
        val baseTemp = when (local.monthNumber) {
            12, 1, 2 -> 0f
            3, 4, 5 -> 14f
            6, 7, 8 -> 28f
            else -> 16f
        }
        return when (force) {
            HeroSimWeatherForce.Auto -> scenarioFor(local.monthNumber, local.hour)
            HeroSimWeatherForce.Clear ->
                wx("Açık", "açık", 800, 8, 2.0f, 20f, baseTemp + 2f, 45, 16_000, null)
            HeroSimWeatherForce.Partly ->
                wx("Parçalı", "parçalı bulutlu", 801, 45, 3.0f, 200f, baseTemp, 55, 12_000, null)
            HeroSimWeatherForce.Overcast ->
                wx("Bulutlu", "kapalı", 804, 95, 4.5f, 220f, baseTemp - 2f, 70, 8_000, null)
            HeroSimWeatherForce.Rain ->
                wx("Yağmur", "yağmur", 501, 90, 5.5f, 250f, baseTemp - 3f, 88, 5_000, null)
            HeroSimWeatherForce.Snow ->
                wx("Kar", "kar yağışı", 601, 92, 3.5f, 40f, minOf(baseTemp, 0f) - 1f, 85, 3_500, 2.0f)
            HeroSimWeatherForce.Fog ->
                wx("Sis", "sisli", 741, 70, 1.5f, 180f, baseTemp - 1f, 95, 900, null)
            HeroSimWeatherForce.Windy ->
                wx("Rüzgâr", "rüzgârlı", 802, 55, 12.0f, 280f, baseTemp, 60, 10_000, null)
        }
    }

    private data class SimWx(
        val label: String,
        val description: String,
        val conditionId: Int,
        val cloudPct: Int,
        val windMs: Float,
        val windDeg: Float,
        val tempC: Float,
        val humidity: Int,
        val visibilityM: Int?,
        val snowMm1h: Float?,
    )

    private fun scenarioFor(month: Int, hour: Int): SimWx {
        // Fixed summer QA preview: 15 Jul 12:00 → sunny 29°C
        if (month == 7 && hour == 12) {
            return wx("Güneşli", "güneşli", 800, 8, 2.5f, 20f, 29f, 40, 16_000, null)
        }
        // Fixed autumn QA: 15 Oct 12:00 → partly cloudy 16°C, light wind (no leaf-fall)
        if (month == 10 && hour == 12) {
            return wx("Parçalı", "parçalı bulutlu", 801, 45, 3.0f, 240f, 16f, 55, 12_000, null)
        }
        // Fixed winter QA: 15 Jan 12:00 → snowy −2°C, light wind, mid accumulation (flakes off via breeze gate)
        if (month == 1 && hour == 12) {
            return wx("Karlı", "kar yağışı", 601, 88, 3.0f, 40f, -2f, 82, 4_000, 2.5f)
        }
        val band = hour / 6 // 0=night 0–5, 1=morning 6–11, 2=day 12–17, 3=eve 18–23
        return when (month) {
            1, 2 -> when (band) {
                0 -> wx("Kar", "kar yağışı", 601, 92, 4.5f, 30f, -2f, 85, 4_000, 1.2f)
                1 -> wx("Bulutlu", "kapalı", 804, 95, 5.5f, 40f, 1f, 78, 8_000, null)
                2 -> wx("Kar", "hafif kar", 600, 88, 3.8f, 20f, 0f, 80, 5_000, 0.4f)
                else -> wx("Bulutlu", "bulutlu", 803, 90, 6.0f, 50f, -1f, 82, 6_000, null)
            }
            3, 4, 5 -> when (band) {
                0 -> wx("Açık", "açık gece", 800, 12, 2.0f, 200f, 8f, 60, 12_000, null)
                1 -> wx("Sis", "sabah sisi", 741, 55, 1.5f, 180f, 10f, 92, 1_200, null)
                // Reference spring midday: partly cloudy 18°C
                2 -> wx("Parçalı", "parçalı bulutlu", 801, 42, 3.0f, 220f, 18f, 45, 14_000, null)
                else -> wx("Yağmur", "yağmur", 500, 85, 4.0f, 240f, 12f, 88, 5_000, null)
            }
            6, 7, 8 -> when (band) {
                0 -> wx("Açık", "açık", 800, 8, 2.5f, 10f, 22f, 50, 15_000, null)
                1 -> wx("Güneşli", "güneşli", 800, 10, 3.2f, 20f, 28f, 40, 16_000, null)
                2 -> wx("Parçalı", "parçalı bulutlu", 801, 40, 4.0f, 30f, 32f, 35, 14_000, null)
                else -> wx("Parçalı", "parçalı bulutlu", 802, 50, 3.5f, 40f, 26f, 45, 13_000, null)
            }
            9, 10, 11 -> when (band) {
                0 -> wx("Rüzgâr", "rüzgârlı bulutlu", 804, 80, 11.0f, 270f, 12f, 70, 9_000, null)
                1 -> wx("Bulutlu", "bulutlu", 803, 75, 9.5f, 260f, 14f, 68, 10_000, null)
                2 -> wx("Yağmur", "yağmurlu", 501, 88, 10.0f, 280f, 15f, 85, 6_000, null)
                else -> wx("Rüzgâr", "rüzgârlı", 802, 60, 12.5f, 290f, 11f, 72, 8_000, null)
            }
            else -> when (band) { // December
                0 -> wx("Sis", "sisli", 741, 70, 2.0f, 100f, 2f, 95, 900, null)
                1 -> wx("Kar", "karlı", 601, 90, 4.0f, 80f, 0f, 88, 3_500, 0.8f)
                2 -> wx("Kar", "kar yağışı", 600, 85, 3.5f, 90f, 1f, 86, 4_500, 0.5f)
                else -> wx("Sis", "sis + kar", 741, 80, 3.0f, 70f, -1f, 90, 1_500, 0.2f)
            }
        }
    }

    private fun wx(
        label: String,
        description: String,
        conditionId: Int,
        cloudPct: Int,
        windMs: Float,
        windDeg: Float,
        tempC: Float,
        humidity: Int,
        visibilityM: Int?,
        snowMm1h: Float?,
    ) = SimWx(label, description, conditionId, cloudPct, windMs, windDeg, tempC, humidity, visibilityM, snowMm1h)
}
