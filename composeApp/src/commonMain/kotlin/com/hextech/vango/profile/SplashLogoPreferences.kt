package com.hextech.vango.profile

expect object SplashLogoPreferences {
    fun getCustomLogoBytes(): ByteArray?
    fun setCustomLogoBytes(bytes: ByteArray?)
}
