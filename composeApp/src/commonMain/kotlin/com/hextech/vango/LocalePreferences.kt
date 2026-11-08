package com.hextech.vango

/**
 * Persisted app language: "en", "tr", or null (follow system locale for resources).
 */
expect object LocalePreferences {
    fun getLanguageTag(): String?
    fun setLanguageTag(tag: String?)
    fun getLanguageUpdatedAtEpochMs(): Long
    fun setLanguageUpdatedAtEpochMs(ms: Long)
}
