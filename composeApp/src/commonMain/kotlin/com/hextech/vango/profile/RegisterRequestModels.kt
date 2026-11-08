package com.hextech.vango.profile

data class RegisterRequestInput(
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,
    val deviceMac: String? = null,
    val deviceUid: String? = null,
    /** Optional profile photo: JPEG, PNG, or WEBP (validated before upload). */
    val profileImage: ByteArray? = null,
) {
    /** API `full_name` alanı için birleşik ad-soyad. */
    fun combinedFullName(): String = "${firstName.trim()} ${lastName.trim()}".trim()
}

data class RegisterRequestResult(
    val message: String,
    val requestId: Int? = null,
    val requestStatus: String? = null,
)
