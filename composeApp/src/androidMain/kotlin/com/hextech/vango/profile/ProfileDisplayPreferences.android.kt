package com.hextech.vango.profile

import android.content.Context
import com.hextech.vango.getAppContext

actual object ProfileDisplayPreferences {
    private const val PREFS = "vango_profile_display_prefs"
    private const val KEY_NOTIF_SYSTEM = "notif_system"
    private const val KEY_NOTIF_ALERT = "notif_alert"
    private const val KEY_NOTIF_DEVICE = "notif_device"
    private const val KEY_NOTIF_CHANNELS = "notif_channel_flags"
    private const val KEY_NOTIF_DISABLED_RULES = "notif_disabled_rules"
    private const val KEY_NOTIF_CONDITIONS = "notif_rule_conditions"
    private const val KEY_ALERT_HISTORY = "alert_history_raw"
    private const val KEY_ALERT_READ = "alert_read_ids"
    private const val KEY_TIMEZONE = "timezone_id"
    private const val KEY_REGISTERED = "registered_at_ms"
    private const val KEY_LAST_LOGIN = "last_login_at_ms"
    private const val KEY_LOCAL_UPDATED = "profile_local_updated_at_ms"
    private const val KEY_SERVER_UPDATED = "profile_server_updated_at_ms"
    private const val KEY_PUSH_PENDING = "profile_push_pending"

    private fun prefs() =
        (getAppContext().get() as Context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun getNotifSystem(): Boolean = prefs().getBoolean(KEY_NOTIF_SYSTEM, true)
    actual fun setNotifSystem(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_NOTIF_SYSTEM, enabled).apply()
    }

    actual fun getNotifAlert(): Boolean = prefs().getBoolean(KEY_NOTIF_ALERT, true)
    actual fun setNotifAlert(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_NOTIF_ALERT, enabled).apply()
    }

    actual fun getNotifDevice(): Boolean = prefs().getBoolean(KEY_NOTIF_DEVICE, true)
    actual fun setNotifDevice(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_NOTIF_DEVICE, enabled).apply()
    }

    actual fun getNotifChannelFlags(): Int = prefs().getInt(KEY_NOTIF_CHANNELS, 0b1111)
    actual fun setNotifChannelFlags(flags: Int) {
        prefs().edit().putInt(KEY_NOTIF_CHANNELS, flags).apply()
    }

    actual fun getNotifDisabledRuleIds(): String =
        prefs().getString(KEY_NOTIF_DISABLED_RULES, null).orEmpty()

    actual fun setNotifDisabledRuleIds(csv: String) {
        prefs().edit().putString(KEY_NOTIF_DISABLED_RULES, csv).apply()
    }

    actual fun getNotifRuleConditionIndices(): String =
        prefs().getString(KEY_NOTIF_CONDITIONS, null).orEmpty()

    actual fun setNotifRuleConditionIndices(csv: String) {
        prefs().edit().putString(KEY_NOTIF_CONDITIONS, csv).apply()
    }

    actual fun getAlertHistoryJson(): String =
        prefs().getString(KEY_ALERT_HISTORY, null).orEmpty()

    actual fun setAlertHistoryJson(raw: String) {
        prefs().edit().putString(KEY_ALERT_HISTORY, raw).apply()
    }

    actual fun getAlertReadIds(): String =
        prefs().getString(KEY_ALERT_READ, null).orEmpty()

    actual fun setAlertReadIds(csv: String) {
        prefs().edit().putString(KEY_ALERT_READ, csv).apply()
    }

    actual fun getTimezoneId(): String =
        prefs().getString(KEY_TIMEZONE, null)?.ifBlank { null } ?: "Europe/Istanbul"

    actual fun setTimezoneId(id: String) {
        prefs().edit().putString(KEY_TIMEZONE, id).apply()
    }

    actual fun getRegisteredAtEpochMs(): Long = prefs().getLong(KEY_REGISTERED, 0L)
    actual fun setRegisteredAtEpochMs(ms: Long) {
        prefs().edit().putLong(KEY_REGISTERED, ms.coerceAtLeast(0L)).apply()
    }

    actual fun getLastLoginAtEpochMs(): Long = prefs().getLong(KEY_LAST_LOGIN, 0L)
    actual fun setLastLoginAtEpochMs(ms: Long) {
        prefs().edit().putLong(KEY_LAST_LOGIN, ms.coerceAtLeast(0L)).apply()
    }

    actual fun getLocalProfileUpdatedAtMs(): Long = prefs().getLong(KEY_LOCAL_UPDATED, 0L)
    actual fun setLocalProfileUpdatedAtMs(ms: Long) {
        prefs().edit().putLong(KEY_LOCAL_UPDATED, ms.coerceAtLeast(0L)).apply()
    }

    actual fun getServerProfileUpdatedAtMs(): Long = prefs().getLong(KEY_SERVER_UPDATED, 0L)
    actual fun setServerProfileUpdatedAtMs(ms: Long) {
        prefs().edit().putLong(KEY_SERVER_UPDATED, ms.coerceAtLeast(0L)).apply()
    }

    actual fun isProfilePushPending(): Boolean = prefs().getBoolean(KEY_PUSH_PENDING, false)
    actual fun setProfilePushPending(pending: Boolean) {
        prefs().edit().putBoolean(KEY_PUSH_PENDING, pending).apply()
    }

    actual fun clearProfileSyncState() {
        prefs().edit()
            .remove(KEY_LOCAL_UPDATED)
            .remove(KEY_SERVER_UPDATED)
            .remove(KEY_PUSH_PENDING)
            .apply()
    }
}
