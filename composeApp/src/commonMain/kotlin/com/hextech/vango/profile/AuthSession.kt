package com.hextech.vango.profile

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * myhexnet `expires_at` biçimi: `yyyy-MM-dd HH:mm:ss` (sunucu yerel saati).
 * Süresi dolmuş token varken yeniden giriş atlanırsa uzaktan bağlanma sessizce başarısız olur.
 */
fun LoginPreferences.hasUsableAuthToken(nowEpochMs: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
    val token = getAuthToken()?.trim().orEmpty()
    if (token.isEmpty()) return false
    val expiresRaw = getAuthExpiresAt()?.trim().orEmpty()
    if (expiresRaw.isEmpty()) return true
    val expiresAt = parseAuthExpiresAt(expiresRaw) ?: return true
    // 60 sn tolerans — saat kayması
    return expiresAt.toEpochMilliseconds() > nowEpochMs + 60_000L
}

fun LoginPreferences.clearExpiredAuthTokenIfNeeded() {
    if (getAuthToken().isNullOrBlank()) return
    if (!hasUsableAuthToken()) clearAuthSession()
}

private fun parseAuthExpiresAt(raw: String): Instant? {
    val normalized = raw.trim().replace('T', ' ')
    val dateTime = normalized.substringBefore('.').trim()
    val parts = dateTime.split(' ')
    if (parts.size != 2) return null
    val d = parts[0].split('-').mapNotNull { it.toIntOrNull() }
    val t = parts[1].split(':').mapNotNull { it.toIntOrNull() }
    if (d.size != 3 || t.size < 2) return null
    val hour = t.getOrNull(0) ?: return null
    val minute = t.getOrNull(1) ?: return null
    val second = t.getOrNull(2) ?: 0
    return runCatching {
        LocalDateTime(d[0], d[1], d[2], hour, minute, second)
            .toInstant(TimeZone.currentSystemDefault())
    }.getOrNull()
}
