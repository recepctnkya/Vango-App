package com.hextech.vango

/**
 * Device battery charge 0–100, or null if unavailable.
 */
expect fun getBatteryLevelPercent(): Int?
