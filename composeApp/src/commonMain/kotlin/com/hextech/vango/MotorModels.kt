package com.hextech.vango

enum class MotorKind {
    /** Yatak kaldırma — Yukarı / Aşağı */
    Lift,
    /** Tente — Açılıyor / Kapanıyor */
    Awning,
    Other,
}

enum class MotorMoveDir { Idle, Forward, Reverse }

data class MotorCardModel(
    val id: Int,
    val title: String,
    val subtitle: String,
    val kind: MotorKind,
    val accentIndex: Int,
    /** Dedicated H-bridge IN1 (forward) — after lighting relays. */
    val forwardRelay: Int? = null,
    /** Dedicated H-bridge IN2 (reverse). */
    val reverseRelay: Int? = null,
)

/**
 * IO lighting relays occupy 0 .. numOfO-1.
 * Each motor uses the next two digital outputs as FWD/REV (H-bridge),
 * so Motor 1 = numOfO+0/+1, Motor 2 = numOfO+2/+3 (clamped to 0..15).
 */
fun motorBridgePins(motorIndex: Int, lightingRelayCount: Int): Pair<Int, Int> {
    val base = lightingRelayCount.coerceIn(0, 14)
    val fwd = (base + motorIndex * 2).coerceIn(0, 14)
    val rev = (fwd + 1).coerceIn(0, 15)
    return fwd to rev
}

fun discoverMotors(
    data: VanGoData,
    channels: PanelPlatformChannels?,
): List<MotorCardModel> {
    val listed = channels?.motors.orEmpty()
        .filter { it.enabled }
        .sortedBy { it.id }
    if (channels != null) {
        if (listed.isEmpty()) return emptyList()
        // Use channel id so Motor 2 keeps its pins/title even if Motor 1 is off.
        return listed.map { ch -> motorCardFromChannel(ch, ch.id, data.numOfO) }
    }
    val motorOn = data.motorEn?.let { it != 0 } ?: (data.io?.motor == true)
    if (!motorOn) return emptyList()
    return (0 until 2).map { i ->
        motorCardFromChannel(
            PanelPlatformChannel(id = i, enabled = true, name = defaultMotorSubtitle(guessMotorKind("", i), i)),
            i,
            data.numOfO,
        )
    }
}

private fun motorCardFromChannel(
    ch: PanelPlatformChannel,
    index: Int,
    lightingRelayCount: Int,
): MotorCardModel {
    val slot = index.coerceAtLeast(0)
    val (fwd, rev) = motorBridgePins(slot, lightingRelayCount)
    val kind = guessMotorKind(ch.name, slot)
    return MotorCardModel(
        id = ch.id,
        title = "MOTOR ${slot + 1}",
        subtitle = ch.name.ifBlank { defaultMotorSubtitle(kind, slot) },
        kind = kind,
        accentIndex = slot % 2,
        forwardRelay = fwd,
        reverseRelay = rev,
    )
}

fun motorLiveDir(data: VanGoData, motor: MotorCardModel): MotorMoveDir {
    val fwdOn = motor.forwardRelay?.let { idx ->
        idx < data.outDB.size && data.outDB[idx].toInt() != 0
    } ?: false
    val revOn = motor.reverseRelay?.let { idx ->
        idx < data.outDB.size && data.outDB[idx].toInt() != 0
    } ?: false
    return when {
        fwdOn && !revOn -> MotorMoveDir.Forward
        revOn && !fwdOn -> MotorMoveDir.Reverse
        else -> MotorMoveDir.Idle
    }
}

private fun defaultMotorSubtitle(kind: MotorKind, index: Int): String = when (kind) {
    MotorKind.Lift -> "Yatak Kaldırma Motoru"
    MotorKind.Awning -> "Tente Motoru"
    MotorKind.Other -> "Motor ${index + 1}"
}

private fun guessMotorKind(name: String, index: Int): MotorKind {
    val n = name.lowercase()
    return when {
        n.contains("yatak") || n.contains("bed") || n.contains("lift") || n.contains("kaldır") || n.contains("kaldir") ->
            MotorKind.Lift
        n.contains("tente") || n.contains("awning") || n.contains("tent") -> MotorKind.Awning
        index == 0 -> MotorKind.Lift
        index == 1 -> MotorKind.Awning
        else -> MotorKind.Other
    }
}
