package com.hextech.vango.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** In-memory custom splash / header logo; persisted via [SplashLogoPreferences]. */
object SplashLogoState {
    var customLogoBytes: ByteArray? by mutableStateOf(null)
}
