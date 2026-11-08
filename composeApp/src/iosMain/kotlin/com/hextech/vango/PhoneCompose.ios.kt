package com.hextech.vango

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun getDeviceType(): String = "Phone"

actual fun createColorWheelBitmap(diameter: Int): ImageBitmap {
    val size = diameter.coerceAtLeast(1)
    return ImageBitmap(width = size, height = size)
}
