package com.hextech.vango.profile

expect object ProfileDisplayPreferences {
    fun getNotifSystem(): Boolean
    fun setNotifSystem(enabled: Boolean)
    fun getNotifAlert(): Boolean
    fun setNotifAlert(enabled: Boolean)
    fun getNotifDevice(): Boolean
    fun setNotifDevice(enabled: Boolean)
    fun getNotifChannelFlags(): Int
    fun setNotifChannelFlags(flags: Int)
    fun getNotifDisabledRuleIds(): String
    fun setNotifDisabledRuleIds(csv: String)
    /** ruleId:index CSV, e.g. clean_low:1,battery:0 */
    fun getNotifRuleConditionIndices(): String
    fun setNotifRuleConditionIndices(csv: String)
    /** Local alert history lines: id|ruleId|title|epochMs */
    fun getAlertHistoryJson(): String
    fun setAlertHistoryJson(raw: String)
    /** Comma-separated read/acked alert keys (live_ruleId or history id). */
    fun getAlertReadIds(): String
    fun setAlertReadIds(csv: String)
    fun getTimezoneId(): String
    fun setTimezoneId(id: String)
    fun getRegisteredAtEpochMs(): Long
    fun setRegisteredAtEpochMs(ms: Long)
    fun getLastLoginAtEpochMs(): Long
    fun setLastLoginAtEpochMs(ms: Long)
    fun getLocalProfileUpdatedAtMs(): Long
    fun setLocalProfileUpdatedAtMs(ms: Long)
    fun getServerProfileUpdatedAtMs(): Long
    fun setServerProfileUpdatedAtMs(ms: Long)
    fun isProfilePushPending(): Boolean
    fun setProfilePushPending(pending: Boolean)
    fun clearProfileSyncState()
}
