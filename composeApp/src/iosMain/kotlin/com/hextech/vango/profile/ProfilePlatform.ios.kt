package com.hextech.vango.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

actual fun decodeProfileImage(bytes: ByteArray): ImageBitmap? {
    // TODO: decode with platform APIs when profile photo is needed on iOS
    return null
}

@Composable
actual fun rememberAvatarPicker(onPicked: (ByteArray?) -> Unit): AvatarPickerLauncher {
    return remember {
        object : AvatarPickerLauncher {
            override fun launch(mimeType: String) {
                // Gallery picker not implemented on iOS yet
            }
        }
    }
}
