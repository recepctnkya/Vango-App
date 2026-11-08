package com.hextech.vango.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Decode JPEG/PNG bytes for display in [androidx.compose.foundation.Image]. */
expect fun decodeProfileImage(bytes: ByteArray): ImageBitmap?

interface AvatarPickerLauncher {
    fun launch(mimeType: String = "image/*")
}

/** Opens the platform image picker; result is delivered as raw bytes (or null if cancelled). */
@Composable
expect fun rememberAvatarPicker(onPicked: (ByteArray?) -> Unit): AvatarPickerLauncher
