package com.hextech.vango.profile

import com.hextech.vango.AppThemeMode
import com.hextech.vango.applyAppAccent
import com.hextech.vango.applyAppLanguage
import com.hextech.vango.applyAppThemeMode
import com.hextech.vango.appThemeModeFromStorage
import com.hextech.vango.customAppAccentId
import com.hextech.vango.customAppLocale
import com.hextech.vango.customAppThemeMode
import com.hextech.vango.normalizeThemeAccentId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.concurrent.Volatile

/**
 * Tablet üzerindeki canlı tema seçimini, uzaktan giriş sonrası 5 sn'lik sunucu
 * senkronunun geri ezmemesi için kısa süreli "yerel otorite" tutar.
 */
object ThemeLocalOverride {
    @Volatile
    private var suppressUntilEpochMs: Long = 0L

    fun noteLocalWrite(holdMs: Long = 60_000L) {
        val until = Clock.System.now().toEpochMilliseconds() + holdMs
        if (until > suppressUntilEpochMs) suppressUntilEpochMs = until
    }

    fun shouldAcceptServerTheme(): Boolean =
        Clock.System.now().toEpochMilliseconds() >= suppressUntilEpochMs
}

/**
 * Canlı tema/vurgu değişiminde sunucuya debounce ile yazar.
 * Yazılmazsa [syncThemeFromServerIfLoggedIn] eski tercihe geri döndürür.
 */
object ThemeServerPush {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pending: Job? = null

    fun scheduleCurrentAppearance(debounceMs: Long = 400L) {
        pending?.cancel()
        pending = scope.launch {
            delay(debounceMs)
            val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
            if (token.isEmpty()) return@launch
            ThemeLocalOverride.noteLocalWrite()
            val mode = customAppThemeMode
            val accent = customAppAccentId
            runCatching {
                ThemePreferenceApi.update(
                    authToken = token,
                    themePreference = themePreferenceApiValue(mode),
                    themeAccentId = accent,
                )
            }
        }
    }
}

/** Sunucunun döndürdüğü değerler: yalnızca `light` | `dark` | `system`. */
fun normalizeThemePreference(raw: String?): String? {
    val t = raw?.trim()?.lowercase().orEmpty()
    if (t.isEmpty()) return null
    return when (t) {
        "light", "dark", "system" -> t
        else -> null
    }
}

suspend fun applyThemeFromServerPreference(raw: String?) {
    val n = normalizeThemePreference(raw) ?: return
    withContext(Dispatchers.Main) {
        if (!ThemeLocalOverride.shouldAcceptServerTheme()) return@withContext
        val next = appThemeModeFromStorage(n)
        // Tablet "blue" maps to server "dark"; keep local blue on dark sync.
        if (customAppThemeMode == AppThemeMode.BLUE && next == AppThemeMode.DARK) return@withContext
        applyAppThemeMode(next, fromServer = true)
    }
}

suspend fun applyAccentFromServerPreference(raw: String?) {
    val n = normalizeThemeAccentId(raw)
    withContext(Dispatchers.Main) {
        if (!ThemeLocalOverride.shouldAcceptServerTheme()) return@withContext
        applyAppAccent(n, fromServer = true)
    }
}

fun themePreferenceApiValue(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.SYSTEM -> "system"
    AppThemeMode.DARK, AppThemeMode.BLUE -> "dark"
    AppThemeMode.LIGHT -> "light"
}

/** Sunucunun döndürdüğü dil değerleri: `tr` | `en` | `system`. */
fun normalizeLocalePreference(raw: String?): String? {
    val t = raw?.trim()?.lowercase().orEmpty()
    if (t.isEmpty()) return null
    return when (t) {
        "tr", "en", "system" -> t
        else -> null
    }
}

fun localePreferenceApiValue(tag: String?): String = when (tag?.trim()?.lowercase()) {
    "tr" -> "tr"
    "en" -> "en"
    else -> "system"
}

/** Canlı dil seçimini 5 sn'lik sunucu senkronunun geri ezmemesi için. */
object LocaleLocalOverride {
    @Volatile
    private var suppressUntilEpochMs: Long = 0L

    fun noteLocalWrite(holdMs: Long = 60_000L) {
        val until = Clock.System.now().toEpochMilliseconds() + holdMs
        if (until > suppressUntilEpochMs) suppressUntilEpochMs = until
    }

    fun shouldAcceptServerLocale(): Boolean =
        Clock.System.now().toEpochMilliseconds() >= suppressUntilEpochMs
}

object LocaleServerPush {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pending: Job? = null

    fun scheduleCurrentLocale(debounceMs: Long = 400L) {
        pending?.cancel()
        pending = scope.launch {
            delay(debounceMs)
            val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
            if (token.isEmpty()) return@launch
            LocaleLocalOverride.noteLocalWrite()
            val tag = customAppLocale
            runCatching {
                LocalePreferenceApi.update(token, localePreferenceApiValue(tag))
            }
        }
    }
}

suspend fun applyLocaleFromServerPreference(raw: String?) {
    val n = normalizeLocalePreference(raw) ?: return
    withContext(Dispatchers.Main) {
        if (!LocaleLocalOverride.shouldAcceptServerLocale()) return@withContext
        // If user explicitly selected TR/EN on mobile, do not force-reset to system
        // just because server still returns "system" during sync lag/failures.
        if (n == "system" && customAppLocale != null) {
            return@withContext
        }
        applyAppLanguage(
            when (n) {
                "tr" -> "tr"
                "en" -> "en"
                else -> null
            },
            fromServer = true,
        )
    }
}

/**
 * Oturum varsa GET [AuthMeApi] ile sunucu temasını çeker ve UI’ye uygular.
 * Sunucu yanıt vermezse veya tema alanı yoksa mevcut yerel ayar kalır.
 */
suspend fun syncThemeFromServerIfLoggedIn() {
    val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
    if (token.isEmpty()) return
    if (!ThemeLocalOverride.shouldAcceptServerTheme()) return

    val pref = AuthMeApi.fetchThemePreference(token).getOrNull() ?: return
    val n = normalizeThemePreference(pref) ?: return
    withContext(Dispatchers.Main) {
        if (!ThemeLocalOverride.shouldAcceptServerTheme()) return@withContext
        val next = appThemeModeFromStorage(n)
        if (customAppThemeMode == AppThemeMode.BLUE && next == AppThemeMode.DARK) return@withContext
        applyAppThemeMode(next, fromServer = true)
    }
}

suspend fun syncAccentFromServerIfLoggedIn() {
    val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
    if (token.isEmpty()) return
    if (!ThemeLocalOverride.shouldAcceptServerTheme()) return

    val accent = AuthMeApi.fetchThemeAccentId(token).getOrNull() ?: return
    applyAccentFromServerPreference(accent)
}

suspend fun syncLocaleFromServerIfLoggedIn() {
    val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
    if (token.isEmpty()) return
    if (!LocaleLocalOverride.shouldAcceptServerLocale()) return

    val pref = AuthMeApi.fetchLocalePreference(token).getOrNull() ?: return
    applyLocaleFromServerPreference(pref)
}

suspend fun syncPreferencesFromServerIfLoggedIn() {
    syncThemeFromServerIfLoggedIn()
    syncAccentFromServerIfLoggedIn()
    syncLocaleFromServerIfLoggedIn()
    runCatching { profileRepositorySingleton.loadProfile() }
}

/** Android: uygulama ön plana gelince tema senkronu (web ile uyum). */
object ThemeForegroundSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onAppForeground() {
        scope.launch {
            syncPreferencesFromServerIfLoggedIn()
        }
    }
}
