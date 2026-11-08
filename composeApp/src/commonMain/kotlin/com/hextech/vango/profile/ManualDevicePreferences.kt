package com.hextech.vango.profile

expect object ManualDevicePreferences {
    fun getJson(): String?
    fun setJson(value: String?)
}
