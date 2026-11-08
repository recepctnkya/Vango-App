package com.hextech.vango

import android.content.Intent
import kotlin.system.exitProcess

actual fun restartApp() {
    val activity = MainActivity.current
    val ctx = activity?.applicationContext ?: (getAppContext().get() as? android.content.Context) ?: return
    val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    } ?: return
    ctx.startActivity(launch)
    activity?.finishAffinity()
    exitProcess(0)
}
