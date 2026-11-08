package com.hextech.vango.profile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

actual fun decodeProfileImage(bytes: ByteArray): ImageBitmap? {
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bmp.asImageBitmap()
}

@Composable
actual fun rememberAvatarPicker(onPicked: (ByteArray?) -> Unit): AvatarPickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.size <= REGISTER_PROFILE_IMAGE_MAX_BYTES) bytes else null
            }
        }.getOrNull()?.let { bytes -> onPicked(bytes) }
    }
    return remember {
        object : AvatarPickerLauncher {
            override fun launch(mimeType: String) {
                launcher.launch(mimeType)
            }
        }
    }
}
