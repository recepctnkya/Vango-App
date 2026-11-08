package com.hextech.vango

import com.hextech.vango.profile.ProfileDisplayPreferences
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Local notification rule engine — evaluates only metrics that exist today
 * (water %, battery V, indoor temp). Gas/CO/door/etc. never fire until sensors exist.
 */
data class ActiveAlert(
    val ruleId: String,
    val titleKey: String,
    val valueText: String,
    val severity: AlertSeverity,
)

enum class AlertSeverity { Info, Warning, Danger }

data class AlertHistoryEntry(
    val id: String,
    val ruleId: String,
    val title: String,
    val epochMs: Long,
)

object AlertRuleEngine {
    /** Rules that cannot be evaluated without firmware sensors. */
    val unsupportedRuleIds: Set<String> = setOf(
        "gas", "co", "humidity", "inverter", "door", "solar",
    )

    private val defaultThresholds = mapOf(
        "clean_low" to listOf(10, 15, 20),
        "clean_full" to listOf(95, 90, 85),
        "battery" to listOf(118, 120, 115), // tenths of a volt
        "temp" to listOf(30, 32, 35),
        "waste_full" to listOf(90, 80, 70),
        "grey" to listOf(15, 15, 15),
    )

    fun conditionIndex(ruleId: String): Int {
        val map = loadConditionMap()
        return map[ruleId]?.coerceIn(0, 2) ?: 0
    }

    fun setConditionIndex(ruleId: String, index: Int) {
        val map = loadConditionMap().toMutableMap()
        map[ruleId] = index.coerceIn(0, 2)
        saveConditionMap(map)
    }

    fun isRuleEnabled(ruleId: String): Boolean {
        if (ruleId in unsupportedRuleIds) return false
        val disabled = ProfileDisplayPreferences.getNotifDisabledRuleIds()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        return ruleId !in disabled
    }

    fun evaluateActive(data: VanGoData, linked: Boolean): List<ActiveAlert> {
        if (!linked) return emptyList()
        if (!ProfileDisplayPreferences.getNotifAlert()) return emptyList()

        val clean = data.getSafeSensorsDataUnsigned(2).coerceIn(0, 100)
        val gray = data.getSafeSensorsDataUnsigned(3).coerceIn(0, 100)
        val dirty = data.getSafeSensorsDataUnsigned(4).coerceIn(0, 100)
        val temp = data.getSafeSensorsDataUnsigned(0)
        val volt = data.volt
        val out = mutableListOf<ActiveAlert>()

        if (isRuleEnabled("clean_low")) {
            val thr = thresholdInt("clean_low")
            if (clean <= thr) {
                out += ActiveAlert("clean_low", "clean_low", "%$clean", AlertSeverity.Danger)
            }
        }
        if (isRuleEnabled("clean_full")) {
            val thr = thresholdInt("clean_full")
            if (clean >= thr) {
                out += ActiveAlert("clean_full", "clean_full", "%$clean", AlertSeverity.Warning)
            }
        }
        if (isRuleEnabled("grey")) {
            val thr = thresholdInt("grey")
            if (gray <= thr) {
                out += ActiveAlert("grey", "grey", "%$gray", AlertSeverity.Warning)
            }
        }
        if (isRuleEnabled("waste_full")) {
            val thr = thresholdInt("waste_full")
            if (dirty >= thr) {
                out += ActiveAlert("waste_full", "waste_full", "%$dirty", AlertSeverity.Danger)
            }
        }
        if (isRuleEnabled("temp") && temp > 0) {
            val thr = thresholdInt("temp")
            if (temp >= thr) {
                out += ActiveAlert("temp", "temp", "${temp}°C", AlertSeverity.Warning)
            }
        }
        if (isRuleEnabled("battery") && volt > 0f) {
            val thrV = thresholdInt("battery") / 10f
            if (volt <= thrV) {
                out += ActiveAlert(
                    "battery",
                    "battery",
                    "${((volt * 10).toInt() / 10.0)}V",
                    AlertSeverity.Danger,
                )
            }
        }
        return out
    }

    /**
     * Compare previous active set → append rising-edge events to local history.
     * Call whenever live telemetry updates.
     */
    fun recordTransitions(active: List<ActiveAlert>, titleFor: (ActiveAlert) -> String) {
        val prev = lastActiveIds
        val nowIds = active.map { it.ruleId }.toSet()
        val rising = active.filter { it.ruleId !in prev }
        if (rising.isNotEmpty()) {
            val now = Clock.System.now().toEpochMilliseconds()
            rising.forEach { alert ->
                AlertReadStore.clear(liveKey(alert.ruleId))
                AlertHistoryStore.append(
                    AlertHistoryEntry(
                        id = "${alert.ruleId}_$now",
                        ruleId = alert.ruleId,
                        title = titleFor(alert),
                        epochMs = now,
                    ),
                )
            }
        }
        lastActiveIds = nowIds
    }

    fun liveKey(ruleId: String): String = "live_$ruleId"

    fun countActive(data: VanGoData, linked: Boolean): Int =
        evaluateActive(data, linked).count { !AlertReadStore.isRead(liveKey(it.ruleId)) }

    fun countUnread(data: VanGoData, linked: Boolean): Int = countActive(data, linked)

    private var lastActiveIds: Set<String> = emptySet()

    private fun thresholdInt(ruleId: String): Int {
        val opts = defaultThresholds[ruleId] ?: return 0
        val idx = conditionIndex(ruleId).coerceIn(0, opts.lastIndex)
        return opts[idx]
    }

    private fun loadConditionMap(): Map<String, Int> {
        val raw = ProfileDisplayPreferences.getNotifRuleConditionIndices()
        if (raw.isBlank()) return emptyMap()
        return raw.split(',')
            .mapNotNull { part ->
                val bits = part.trim().split(':')
                if (bits.size != 2) return@mapNotNull null
                val id = bits[0].trim()
                val idx = bits[1].trim().toIntOrNull() ?: return@mapNotNull null
                id to idx
            }
            .toMap()
    }

    private fun saveConditionMap(map: Map<String, Int>) {
        val csv = map.entries.joinToString(",") { "${it.key}:${it.value}" }
        ProfileDisplayPreferences.setNotifRuleConditionIndices(csv)
    }
}

object AlertHistoryStore {
    private const val MAX = 50

    fun load(): List<AlertHistoryEntry> {
        val raw = ProfileDisplayPreferences.getAlertHistoryJson()
        if (raw.isBlank()) return emptyList()
        return raw.split('\n')
            .mapNotNull { line ->
                val p = line.split('|')
                if (p.size < 4) return@mapNotNull null
                val epoch = p[3].toLongOrNull() ?: return@mapNotNull null
                AlertHistoryEntry(
                    id = p[0],
                    ruleId = p[1],
                    title = p[2],
                    epochMs = epoch,
                )
            }
            .sortedByDescending { it.epochMs }
    }

    fun append(entry: AlertHistoryEntry) {
        val next = (listOf(entry) + load()).take(MAX)
        save(next)
    }

    fun clear() {
        ProfileDisplayPreferences.setAlertHistoryJson("")
    }

    fun formatTime(epochMs: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val d = "${dt.dayOfMonth.toString().padStart(2, '0')}.${dt.monthNumber.toString().padStart(2, '0')}"
        val t = "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        return "$d $t"
    }

    private fun save(entries: List<AlertHistoryEntry>) {
        val raw = entries.joinToString("\n") { e ->
            val safeTitle = e.title.replace('|', '/').replace('\n', ' ')
            "${e.id}|${e.ruleId}|$safeTitle|${e.epochMs}"
        }
        ProfileDisplayPreferences.setAlertHistoryJson(raw)
    }
}

object AlertReadStore {
    fun isRead(key: String): Boolean = load().contains(key)

    fun markRead(key: String) {
        if (key.isBlank()) return
        val next = load().toMutableSet()
        if (next.add(key)) save(next)
    }

    fun clear(key: String) {
        val next = load().toMutableSet()
        if (next.remove(key)) save(next)
    }

    private fun load(): Set<String> =
        ProfileDisplayPreferences.getAlertReadIds()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    private fun save(ids: Set<String>) {
        ProfileDisplayPreferences.setAlertReadIds(ids.joinToString(","))
    }
}

fun platformSupportsBle(): Boolean =
    getPlatform().name.startsWith("Android", ignoreCase = true)
