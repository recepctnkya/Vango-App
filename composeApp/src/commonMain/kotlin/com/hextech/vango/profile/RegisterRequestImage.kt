package com.hextech.vango.profile

/** Max ~4 MB per API contract. */
internal const val REGISTER_PROFILE_IMAGE_MAX_BYTES = 4 * 1024 * 1024

/** Max 4 MB for [avatar-upload.php] (Hexnet IoT). */
internal const val AVATAR_UPLOAD_MAX_BYTES = 4 * 1024 * 1024

/**
 * Returns (Content-Type, filename) for a supported image, or null if bytes are not JPEG/PNG/WEBP.
 */
internal fun sniffRegisterProfileImage(bytes: ByteArray): Pair<String, String>? {
    if (bytes.size < 12) return null
    // JPEG
    if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
        return "image/jpeg" to "profile.jpg"
    }
    // PNG
    if (bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() &&
        bytes[3] == 0x47.toByte()
    ) {
        return "image/png" to "profile.png"
    }
    // WEBP (RIFF....WEBP)
    if (bytes[0] == 0x52.toByte() &&
        bytes[1] == 0x49.toByte() &&
        bytes[2] == 0x46.toByte() &&
        bytes[3] == 0x46.toByte() &&
        bytes[8] == 0x57.toByte() &&
        bytes[9] == 0x45.toByte() &&
        bytes[10] == 0x42.toByte() &&
        bytes[11] == 0x50.toByte()
    ) {
        return "image/webp" to "profile.webp"
    }
    return null
}

/**
 * Kayıt dışı avatar yükleme: JPEG, PNG, GIF, WebP ([avatar-upload.php] sözleşmesi).
 */
internal fun sniffAvatarUploadImage(bytes: ByteArray): Pair<String, String>? {
    sniffRegisterProfileImage(bytes)?.let { return it }
    if (bytes.size < 6) return null
    // GIF87a / GIF89a
    if (bytes[0] == 0x47.toByte() &&
        bytes[1] == 0x49.toByte() &&
        bytes[2] == 0x46.toByte() &&
        bytes[3] == 0x38.toByte() &&
        (bytes[4] == 0x37.toByte() || bytes[4] == 0x39.toByte()) &&
        bytes[5] == 0x61.toByte()
    ) {
        return "image/gif" to "avatar.gif"
    }
    return null
}

internal fun validateRegisterProfileImage(bytes: ByteArray): String? {
    if (bytes.isEmpty()) return null
    if (bytes.size > REGISTER_PROFILE_IMAGE_MAX_BYTES) {
        return "Profil fotoğrafı en fazla 4 MB olabilir."
    }
    if (sniffRegisterProfileImage(bytes) == null) {
        return "Profil fotoğrafı JPG, PNG veya WEBP formatında olmalıdır."
    }
    return null
}
