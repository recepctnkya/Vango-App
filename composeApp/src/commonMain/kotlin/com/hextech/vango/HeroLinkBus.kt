package com.hextech.vango

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/**
 * Lightweight bus for the phone-home hero link strip:
 * increments when the phone successfully sends data toward the vehicle.
 */
object HeroLinkBus {
    var txEpoch by mutableLongStateOf(0L)
        private set

    fun notifyOutboundTx() {
        txEpoch += 1L
    }
}
