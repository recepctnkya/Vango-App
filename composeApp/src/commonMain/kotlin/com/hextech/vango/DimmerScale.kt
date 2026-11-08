package com.hextech.vango

import kotlin.math.roundToInt

object DimmerScale {
    fun percentToRaw255(percent: Int): Int =
        (percent.coerceIn(0, 100) * 255.0 / 100.0).roundToInt().coerceIn(0, 255)

    fun raw255ToPercent(raw: Int): Int =
        (raw.coerceIn(0, 255) * 100.0 / 255.0).roundToInt().coerceIn(0, 100)

    /** Panel `dim[]` telemetri — zaten 0-100 yuzde. */
    fun telemetryPercent(value: Int): Int = value.coerceIn(0, 100)

    /**
     * `outputs.dimmers[]` icin: deger >100 ise PWM 0-255, aksi halde yuzde kabul et.
     * (Panel app.js: dim alani yuzde; ham dimmers PWM olabilir.)
     */
    fun telemetryRawToPercent(raw: Int): Int {
        val v = raw.coerceIn(0, 255)
        return if (v > 100) raw255ToPercent(v) else v.coerceIn(0, 100)
    }

    fun telemetryToPercentByte(raw: Int, alreadyPercent: Boolean): Byte =
        (if (alreadyPercent) telemetryPercent(raw) else telemetryRawToPercent(raw)).toByte()
}