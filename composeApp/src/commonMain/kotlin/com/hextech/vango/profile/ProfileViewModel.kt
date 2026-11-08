package com.hextech.vango.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileViewState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isEditing: Boolean = false,
    val draft: UserProfile = UserProfile.guest(),
    val persisted: UserProfile = UserProfile.guest(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isAvatarUploading: Boolean = false,
    /** Sunucu kaydı veya ağ hatası mesajı; başarılı kayıtta temizlenir. */
    val saveError: String? = null,
    /** Başarılı kayıttan sonra kullanıcıya gösterilecek bilgi popup metni. */
    val saveInfoMessage: String? = null,
)

class ProfileViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {
    private val usernameRegex = Regex("^[a-zA-Z0-9._-]{3,40}$")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val macRegex = Regex("^([0-9A-Fa-f]{2}([:-])){5}[0-9A-Fa-f]{2}$")

    private val _state = MutableStateFlow(ProfileViewState(isLoading = true))
    val state: StateFlow<ProfileViewState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, fieldErrors = emptyMap()) }
            runCatching {
                val signedIn = repository.isSignedIn()
                val profile = repository.loadProfile()
                signedIn to profile
            }.onSuccess { (signedIn, profile) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = signedIn,
                        draft = profile,
                        persisted = profile,
                        isEditing = false,
                        saveError = null,
                    )
                }
            }.onFailure {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateDraft(transform: (UserProfile) -> UserProfile) {
        _state.update { s ->
            s.copy(draft = transform(s.draft), fieldErrors = emptyMap(), saveError = null, saveInfoMessage = null)
        }
    }

    /** Avatar: yalnızca taslakta güncellenir; sunucuya [save] ile gider. */
    fun updateAvatarBytes(bytes: ByteArray?) {
        val next = _state.value.draft.copy(avatarBytes = bytes)
        _state.update { it.copy(draft = next, saveError = null, saveInfoMessage = null) }
    }

    fun setEditing(value: Boolean) {
        _state.update { it.copy(isEditing = value, fieldErrors = emptyMap(), saveError = null, saveInfoMessage = null) }
    }

    fun signInWithCredentials(loginIdentifier: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, fieldErrors = emptyMap()) }
            repository.signInWithCredentials(loginIdentifier, password)
                .onSuccess { profile ->
                    ProfileGuestPreferences.setAllowDirectProfile(false)
                    com.hextech.vango.GuestBleWarn.dismiss()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            draft = profile,
                            persisted = profile,
                            isEditing = false,
                            fieldErrors = emptyMap(),
                            saveError = null,
                            saveInfoMessage = null,
                        )
                    }
                    onResult(Result.success(Unit))
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false) }
                    onResult(Result.failure(it))
                }
        }
    }

    /** İlk adım (metin alanları) için doğrulama; hata mesajı veya null. */
    fun validateRegisterTextFields(input: RegisterRequestInput, passwordConfirm: String): String? {
        val first = input.firstName.trim()
        val last = input.lastName.trim()
        val username = input.username.trim()
        val email = input.email.trim()
        val password = input.password
        val mac = input.deviceMac?.trim().orEmpty()
        return when {
            first.isBlank() -> "Ad zorunludur."
            last.isBlank() -> "Soyad zorunludur."
            !usernameRegex.matches(username) ->
                "Kullanıcı adı 3-40 karakter olmalı ve yalnızca harf/rakam/._- içermelidir."
            password.length < 6 -> "Şifre en az 6 karakter olmalıdır."
            password != passwordConfirm -> "Şifreler eşleşmiyor."
            !emailRegex.matches(email) -> "Geçerli bir e-posta giriniz."
            mac.isNotBlank() && !macRegex.matches(mac) -> "Cihaz MAC formatı geçersiz (AA:BB:CC:DD:EE:FF)."
            else -> null
        }
    }

    fun validateRegisterProfileImagePart(input: RegisterRequestInput): String? {
        input.profileImage?.takeIf { it.isNotEmpty() }?.let { bytes ->
            return validateRegisterProfileImage(bytes)
        }
        return null
    }

    /** Kayıt formu: MAC için sunucudaki ön kayıtlı UID (varsa). */
    fun resolveDeviceUidByMac(mac: String, onResult: (Result<String?>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.resolveDeviceUidByMac(mac))
        }
    }

    fun submitRegisterRequest(
        input: RegisterRequestInput,
        passwordConfirm: String,
        onResult: (Result<RegisterRequestResult>) -> Unit,
    ) {
        viewModelScope.launch {
            validateRegisterTextFields(input, passwordConfirm)?.let {
                onResult(Result.failure(IllegalArgumentException(it)))
                return@launch
            }
            validateRegisterProfileImagePart(input)?.let {
                onResult(Result.failure(IllegalArgumentException(it)))
                return@launch
            }
            val username = input.username.trim()
            val email = input.email.trim()
            val mac = input.deviceMac?.trim().orEmpty()

            _state.update { it.copy(isLoading = true) }
            repository.submitRegisterRequest(
                input.copy(
                    firstName = input.firstName.trim(),
                    lastName = input.lastName.trim(),
                    username = username,
                    email = email,
                    deviceMac = mac.ifBlank { null },
                )
            ).onSuccess {
                _state.update { s -> s.copy(isLoading = false) }
                onResult(Result.success(it))
            }.onFailure {
                _state.update { s -> s.copy(isLoading = false) }
                onResult(Result.failure(it))
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val currentState = _state.value
            val current = currentState.draft
            val errors = ProfileValidator.validateForSave(current)
            if (errors.isNotEmpty()) {
                _state.update { it.copy(fieldErrors = errors) }
                return@launch
            }
            val hasChanges = normalizeForCompare(current) != normalizeForCompare(currentState.persisted)
            if (!hasChanges) {
                _state.update {
                    it.copy(
                        isEditing = false,
                        fieldErrors = emptyMap(),
                        saveError = null,
                        saveInfoMessage = "Bilgilerinizde bir değişiklik olmadı.",
                    )
                }
                return@launch
            }
            _state.update { it.copy(isSaving = true, fieldErrors = emptyMap(), saveError = null) }
            repository.saveProfile(current)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            persisted = current,
                            saveError = null,
                            saveInfoMessage = "Bilgileriniz güncellendi.",
                        )
                    }
                    refresh()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            saveError = e.message?.ifBlank { null } ?: "Profil kaydedilemedi.",
                            saveInfoMessage = null,
                        )
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.signOut()
            ProfileGuestPreferences.setAllowDirectProfile(false)
            ProfileGuestPreferences.setGuestBleWarnDismissed(false)
            com.hextech.vango.AlertReadStore.clear(com.hextech.vango.GUEST_BLE_WARN_LIVE_KEY)
            if (com.hextech.vango.GuestBleWarn.isBleLinked()) {
                com.hextech.vango.GuestBleWarn.onGuestBleLinked()
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    isEditing = false,
                    draft = UserProfile.guest(),
                    persisted = UserProfile.guest(),
                    fieldErrors = emptyMap(),
                    saveError = null,
                    saveInfoMessage = null,
                    isAvatarUploading = false,
                )
            }
        }
    }

    fun requestDevicePairing() {
        viewModelScope.launch {
            repository.requestDevicePairing()
        }
    }

    fun cancelEdit() {
        refresh()
    }

    fun dismissSaveInfoMessage() {
        _state.update { it.copy(saveInfoMessage = null) }
    }

    private fun avatarUploadMessage(): String = "Profil fotoğrafı sunucuya yüklendi."

    fun uploadProfileAvatar(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val draft = _state.value.draft
            val av = draft.avatarBytes
            if (av == null || av.isEmpty()) {
                onResult(Result.failure(IllegalStateException("Önce bir fotoğraf seçin.")))
                return@launch
            }
            _state.update { it.copy(isAvatarUploading = true, saveError = null) }
            repository.uploadProfileAvatar(draft)
                .onSuccess {
                    val p = repository.loadProfile()
                    _state.update {
                        it.copy(
                            isAvatarUploading = false,
                            draft = p,
                            persisted = p,
                            saveError = null,
                            saveInfoMessage = avatarUploadMessage(),
                        )
                    }
                    onResult(Result.success(Unit))
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isAvatarUploading = false,
                            saveError = e.message?.ifBlank { null } ?: "Fotoğraf yüklenemedi.",
                        )
                    }
                    onResult(Result.failure(e))
                }
        }
    }

    private fun normalizeForCompare(profile: UserProfile): UserProfile = profile.copy(
        fullName = profile.fullName.trim(),
        username = profile.username.trim(),
        email = profile.email.trim(),
        phone = profile.phone.trim(),
        address = profile.address.trim(),
        pairedDeviceMac = profile.pairedDeviceMac.trim(),
        deviceUid = profile.deviceUid.trim(),
    )
}
