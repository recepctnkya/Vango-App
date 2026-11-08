package com.hextech.vango.profile

/**
 * Abstraction for profile + auth + device pairing. Swap implementation when the backend is ready.
 */
interface ProfileRepository {
    suspend fun isSignedIn(): Boolean
    suspend fun loadProfile(): UserProfile
    suspend fun saveProfile(profile: UserProfile): Result<Unit>
    /** Demo auth; replace with Hexnet IoT auth API. [loginIdentifier] e-posta veya kullanıcı adı olabilir. */
    suspend fun signInWithCredentials(loginIdentifier: String, password: String): Result<UserProfile>
    /** Seçili avatar baytlarını sunucuya yükler ve profili günceller. */
    suspend fun uploadProfileAvatar(profile: UserProfile): Result<Unit>
    suspend fun submitRegisterRequest(input: RegisterRequestInput): Result<RegisterRequestResult>
    suspend fun resolveDeviceUidByMac(mac: String): Result<String?>
    suspend fun signOut(): Result<Unit>
    /** Placeholder for pairing request acknowledgement (e.g. push to device queue). */
    suspend fun requestDevicePairing(): Result<Unit>
}
