package com.hextech.vango

import android.content.Context
import android.content.pm.ApplicationInfo

actual fun isAppDebugBuild(): Boolean =
    runCatching {
        val ctx = getAppContext().get() as Context
        val flags = ctx.applicationInfo.flags
        (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }.getOrDefault(false)
