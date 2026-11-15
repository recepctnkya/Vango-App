package com.hextech.vango

actual fun isWifiConnected(): Boolean = false

// Let Ktor report connectivity; a synchronous reachability check can falsely fail on iOS.
actual fun isInternetAvailable(): Boolean = true
