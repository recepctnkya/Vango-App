package com.hextech.vango.profile

/**
 * Pure validation for form submit. Error messages are keys resolved in UI via string resources.
 */
object ProfileValidator {

    const val ERR_FULL_NAME = "err_full_name"
    const val ERR_EMAIL = "err_email"
    const val ERR_PHONE = "err_phone"

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
    private val phoneRegex = Regex("^[0-9+()\\-\\s]{10,}$")

    fun validateForSave(profile: UserProfile): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (profile.fullName.trim().length < 2) {
            errors[ERR_FULL_NAME] = ERR_FULL_NAME
        }
        val email = profile.email.trim()
        if (email.isEmpty()) {
            errors[ERR_EMAIL] = ERR_EMAIL
        } else if (!emailRegex.matches(email)) {
            errors[ERR_EMAIL] = ERR_EMAIL
        }
        val phone = profile.phone.trim()
        if (phone.isNotEmpty() && !phoneRegex.matches(phone)) {
            errors[ERR_PHONE] = ERR_PHONE
        }
        return errors
    }
}
