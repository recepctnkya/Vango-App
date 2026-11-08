package com.hextech.vango.profile

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

actual object ProfileDisplayPreferences {
    private const val KEY_NOTIF_SYSTEM = "profile_notif_system"
    private const val KEY_NOTIF_ALERT = "profile_notif_alert"
    private const val KEY_NOTIF_DEVICE = "profile_notif_device"
    private const val KEY_NOTIF_CHANNELS = "profile_notif_channel_flags"
    private const val KEY_NOTIF_DISABLED_RULES = "profile_notif_disabled_rules"
    private const val KEY_NOTIF_CONDITIONS = "profile_notif_rule_conditions"
    private const val KEY_ALERT_HISTORY = "profile_alert_history_raw"
    private const val KEY_ALERT_READ = "profile_alert_read_ids"
    private const val KEY_TIMEZONE = "profile_timezone_id"
    private const val KEY_REGISTERED = "profile_registered_at_ms"
    private const val KEY_LAST_LOGIN = "profile_last_login_at_ms"
    private const val KEY_LOCAL_UPDATED = "profile_local_updated_at_ms"
    private const val KEY_SERVER_UPDATED = "profile_server_updated_at_ms"
    private const val KEY_PUSH_PENDING = "profile_push_pending"

    private fun defaults() = NSUserDefaults.standardUserDefaults

    actual fun getNotifSystem(): Boolean =
        (defaults().objectForKey(KEY_NOTIF_SYSTEM) as? NSNumber)?.boolValue ?: true
    actual fun setNotifSystem(enabled: Boolean) {
        defaults().setObject(NSNumber(bool = enabled), KEY_NOTIF_SYSTEM)
    }

    actual fun getNotifAlert(): Boolean =
        (defaults().objectForKey(KEY_NOTIF_ALERT) as? NSNumber)?.boolValue ?: true
    actual fun setNotifAlert(enabled: Boolean) {
        defaults().setObject(NSNumber(bool = enabled), KEY_NOTIF_ALERT)
    }

    actual fun getNotifDevice(): Boolean =
        (defaults().objectForKey(KEY_NOTIF_DEVICE) as? NSNumber)?.boolValue ?: true
    actual fun setNotifDevice(enabled: Boolean) {
        defaults().setObject(NSNumber(bool = enabled), KEY_NOTIF_DEVICE)
    }

    actual fun getNotifChannelFlags(): Int =
        (defaults().objectForKey(KEY_NOTIF_CHANNELS) as? NSNumber)?.intValue ?: 0b1111

    actual fun setNotifChannelFlags(flags: Int) {
        defaults().setObject(NSNumber(integer = flags.toLong()), KEY_NOTIF_CHANNELS)
    }

    actual fun getNotifDisabledRuleIds(): String =
        defaults().stringForKey(KEY_NOTIF_DISABLED_RULES).orEmpty()

    actual fun setNotifDisabledRuleIds(csv: String) {
        defaults().setObject(csv, KEY_NOTIF_DISABLED_RULES)
    }

    actual fun getNotifRuleConditionIndices(): String =
        defaults().stringForKey(KEY_NOTIF_CONDITIONS).orEmpty()

    actual fun setNotifRuleConditionIndices(csv: String) {
        defaults().setObject(csv, KEY_NOTIF_CONDITIONS)
    }

    actual fun getAlertHistoryJson(): String =
        defaults().stringForKey(KEY_ALERT_HISTORY).orEmpty()

    actual fun setAlertHistoryJson(raw: String) {
        defaults().setObject(raw, KEY_ALERT_HISTORY)
    }

    actual fun getAlertReadIds(): String =
        defaults().stringForKey(KEY_ALERT_READ).orEmpty()

    actual fun setAlertReadIds(csv: String) {
        defaults().setObject(csv, KEY_ALERT_READ)
    }

    actual fun getTimezoneId(): String =
        defaults().stringForKey(KEY_TIMEZONE)?.ifBlank { null } ?: "Europe/Istanbul"

    actual fun setTimezoneId(id: String) {
        defaults().setObject(id, KEY_TIMEZONE)
    }

    actual fun getRegisteredAtEpochMs(): Long {
        val raw = defaults().objectForKey(KEY_REGISTERED) as? NSNumber
        return raw?.longLongValue ?: 0L
    }

    actual fun setRegisteredAtEpochMs(ms: Long) {
        defaults().setObject(NSNumber(longLong = ms.coerceAtLeast(0L)), KEY_REGISTERED)
    }

    actual fun getLastLoginAtEpochMs(): Long {
        val raw = defaults().objectForKey(KEY_LAST_LOGIN) as? NSNumber
        return raw?.longLongValue ?: 0L
    }

    actual fun setLastLoginAtEpochMs(ms: Long) {
        defaults().setObject(NSNumber(longLong = ms.coerceAtLeast(0L)), KEY_LAST_LOGIN)
    }

    actual fun getLocalProfileUpdatedAtMs(): Long {
        val raw = defaults().objectForKey(KEY_LOCAL_UPDATED) as? NSNumber
        return raw?.longLongValue ?: 0L
    }

    actual fun setLocalProfileUpdatedAtMs(ms: Long) {
        defaults().setObject(NSNumber(longLong = ms.coerceAtLeast(0L)), KEY_LOCAL_UPDATED)
    }

    actual fun getServerProfileUpdatedAtMs(): Long {
        val raw = defaults().objectForKey(KEY_SERVER_UPDATED) as? NSNumber
        return raw?.longLongValue ?: 0L
    }

    actual fun setServerProfileUpdatedAtMs(ms: Long) {
        defaults().setObject(NSNumber(longLong = ms.coerceAtLeast(0L)), KEY_SERVER_UPDATED)
    }

    actual fun isProfilePushPending(): Boolean =
        (defaults().objectForKey(KEY_PUSH_PENDING) as? NSNumber)?.boolValue ?: false

    actual fun setProfilePushPending(pending: Boolean) {
        defaults().setObject(NSNumber(bool = pending), KEY_PUSH_PENDING)
    }

    actual fun clearProfileSyncState() {
        defaults().removeObjectForKey(KEY_LOCAL_UPDATED)
        defaults().removeObjectForKey(KEY_SERVER_UPDATED)
        defaults().removeObjectForKey(KEY_PUSH_PENDING)
    }
}
