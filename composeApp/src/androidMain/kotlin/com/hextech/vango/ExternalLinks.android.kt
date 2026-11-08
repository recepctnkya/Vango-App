package com.hextech.vango

import android.content.Context
import android.content.Intent
import android.net.Uri

actual fun openExternalUrl(url: String) {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val ctx = getAppContext().get() as Context
        ctx.startActivity(intent)
    } catch (e: Exception) {
        println("openExternalUrl failed: ${e.message}")
    }
}