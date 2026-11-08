package com.hextech.vango

import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun getDeviceType(): String {
    val configuration = LocalConfiguration.current
    // Use smallest width so tablets stay tablet layout in portrait and landscape.
    val smallestWidthDp = configuration.smallestScreenWidthDp
    return if (smallestWidthDp >= 600) "Tablet" else "Phone"
}

actual fun createColorWheelBitmap(diameter: Int): androidx.compose.ui.graphics.ImageBitmap {
    val radius = diameter / 2f
    val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
    for (x in 0 until diameter) {
        for (y in 0 until diameter) {
            val dx = x - radius
            val dy = y - radius
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist <= radius) {
                val hue = (kotlin.math.atan2(dy, dx) * 180f / Math.PI).toFloat().let { if (it < 0) it + 360f else it }
                val sat = (dist / radius).coerceIn(0f, 1f)
                val color = AndroidColor.HSVToColor(floatArrayOf(hue, sat, 1f))
                bitmap.setPixel(x, y, color)
            } else {
                bitmap.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return bitmap.asImageBitmap()
} 