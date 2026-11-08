package com.hextech.vango

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

actual fun getBatteryLevelPercent(): Int? {
    val ctx = getAppContext().get() as? Context ?: return null
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && bm != null) {
        val v = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (v in 0..100) return v
    }
    val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (level >= 0 && scale > 0) (level * 100 / scale) else null
}
