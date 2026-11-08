package com.hextech.vango.profile

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.concurrent.Volatile

internal fun profileContentKey(profile: UserProfile): String =
    listOf(
        profile.fullName.trim().lowercase(),
        profile.email.trim().lowercase(),
        profile.phone.trim(),
        profile.username.trim().lowercase(),
    ).joinToString("|")

internal fun parseRemoteProfileUpdatedAt(user: LoginUserDto): Long {
    val raw = listOf(user.updated_at, user.profile_updated_at, user.last_updated)
        .mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() } }
        .firstOrNull()
        ?: return 0L
    raw.toLongOrNull()?.let { n ->
        return if (n < 10_000_000_000L) n * 1000L else n
    }
    runCatching { Instant.parse(raw).toEpochMilliseconds() }.getOrNull()?.let { return it }
    val isoGuess = when {
        raw.contains(' ') && !raw.contains('T') -> raw.replace(' ', 'T')
        else -> raw
    }.let { if (it.endsWith("Z") || it.contains('+')) it else "${it}Z" }
    return runCatching { Instant.parse(isoGuess).toEpochMilliseconds() }.getOrNull() ?: 0L
}

internal object ProfileLocalOverride {
    @Volatile
    private var suppressUntilEpochMs: Long = 0L

    fun noteLocalWrite(holdMs: Long = 90_000L) {
        val until = Clock.System.now().toEpochMilliseconds() + holdMs
        if (until > suppressUntilEpochMs) suppressUntilEpochMs = until
    }

    fun shouldPreferLocal(): Boolean =
        Clock.System.now().toEpochMilliseconds() < suppressUntilEpochMs
}

/** Yerel bekleyen/yeni kayıt sunucudan daha yeni ise yerel kazanır. */
internal fun localProfileWins(
    pendingLocal: Boolean,
    preferLocalHold: Boolean,
    localUpdatedAtMs: Long,
    remoteUpdatedAtMs: Long,
): Boolean {
    if (preferLocalHold) return true
    if (!pendingLocal) {
        return remoteUpdatedAtMs > 0L && localUpdatedAtMs > remoteUpdatedAtMs
    }
    if (remoteUpdatedAtMs <= 0L) return true
    return localUpdatedAtMs >= remoteUpdatedAtMs
}
