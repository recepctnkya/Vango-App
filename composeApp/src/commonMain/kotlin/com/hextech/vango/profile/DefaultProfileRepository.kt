package com.hextech.vango.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Default profile data source backed by Hexnet APIs plus local session cache.
 */
class DefaultProfileRepository : ProfileRepository {

    private var loggedIn: Boolean = false
    private var profile: UserProfile = UserProfile.guest()

    private fun cachedPairedMacForProfile(): String {
        val mac = LoginPreferences.getCachedPairedDeviceMac()?.trim().orEmpty()
        return when {
            mac.isEmpty() || mac == "-" || mac == "\u2014" -> "—"
            else -> mac
        }
    }

    private fun cachedCaravanTypeForProfile(): CaravanType = when (LoginPreferences.getCachedCaravanType()?.trim()) {
        CaravanType.MotorCaravan.name -> CaravanType.MotorCaravan
        else -> CaravanType.TowCaravan
    }

    private fun cachedDeviceUidForProfile(): String {
        val uid = LoginPreferences.getCachedDeviceUid()?.trim().orEmpty()
        return when {
            uid.isEmpty() || uid == "-" || uid == "\u2014" -> "—"
            else -> uid
        }
    }

    override suspend fun isSignedIn(): Boolean = withContext(Dispatchers.Default) {
        LoginPreferences.clearExpiredAuthTokenIfNeeded()
        if (!loggedIn && LoginPreferences.hasUsableAuthToken()) {
            loggedIn = true
        }
        if (loggedIn && !LoginPreferences.hasUsableAuthToken()) {
            loggedIn = false
        }
        loggedIn
    }

    override suspend fun loadProfile(): UserProfile = withContext(Dispatchers.Default) {
        if (!loggedIn) {
            UserProfile.guest()
        } else {
            syncProfileWithServer()
        }
    }

    private suspend fun syncProfileWithServer(): UserProfile {
        val local = profileFromLocalCache()
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            profile = local
            return profile
        }
        val remoteUser = runCatching { AuthMeApi.fetchCurrentUser(token) }.getOrNull()
        if (remoteUser == null) {
            profile = local
            return profile
        }
        val remote = profileFromRemoteUser(remoteUser)
        val remoteAt = parseRemoteProfileUpdatedAt(remoteUser)
            .takeIf { it > 0L }
            ?: ProfileDisplayPreferences.getServerProfileUpdatedAtMs()
        val localAt = ProfileDisplayPreferences.getLocalProfileUpdatedAtMs()
        val pending = ProfileDisplayPreferences.isProfilePushPending()
        val sameContent = profileContentKey(local) == profileContentKey(remote)

        if (sameContent) {
            applyRemoteUserToCache(remoteUser)
            ProfileDisplayPreferences.setProfilePushPending(false)
            if (remoteAt > 0L) {
                ProfileDisplayPreferences.setServerProfileUpdatedAtMs(remoteAt)
                if (localAt <= 0L) ProfileDisplayPreferences.setLocalProfileUpdatedAtMs(remoteAt)
            }
            profile = remote
            return profile
        }

        if (localProfileWins(pending, ProfileLocalOverride.shouldPreferLocal(), localAt, remoteAt)) {
            val pushed = pushProfileToServer(local, token)
            profile = if (pushed.isSuccess) {
                val url = pushed.getOrNull()
                if (!url.isNullOrBlank()) LoginPreferences.setCachedAvatarUrl(url)
                ProfileDisplayPreferences.setProfilePushPending(false)
                val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                ProfileDisplayPreferences.setServerProfileUpdatedAtMs(now)
                persistLocalProfile(local, touchLocalTime = false)
            } else {
                persistLocalProfile(local, touchLocalTime = false)
                ProfileDisplayPreferences.setProfilePushPending(true)
                local
            }
            return profile
        }

        applyRemoteUserToCache(remoteUser)
        ProfileDisplayPreferences.setProfilePushPending(false)
        val appliedAt = if (remoteAt > 0L) remoteAt else kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        ProfileDisplayPreferences.setServerProfileUpdatedAtMs(appliedAt)
        ProfileDisplayPreferences.setLocalProfileUpdatedAtMs(appliedAt)
        profile = remote
        return profile
    }

    private fun persistLocalProfile(next: UserProfile, touchLocalTime: Boolean): UserProfile {
        LoginPreferences.setCachedFullName(next.fullName.trim())
        LoginPreferences.setCachedPhone(next.phone.trim())
        LoginPreferences.setCachedUsername(next.username.trim().ifBlank { null })
        LoginPreferences.setCachedAccountEmail(next.email.trim().lowercase().ifBlank { null })
        LoginPreferences.setCachedCaravanType(next.caravanType.name)
        val mac = next.pairedDeviceMac.trim().takeIf { it.isNotEmpty() && it != "—" && it != "-" }
        val uid = next.deviceUid.trim().takeIf { it.isNotEmpty() && it != "—" && it != "-" }
        LoginPreferences.setCachedPairedDeviceMac(mac)
        LoginPreferences.setCachedDeviceUid(uid)
        if (touchLocalTime) {
            ProfileDisplayPreferences.setLocalProfileUpdatedAtMs(
                kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            )
            ProfileDisplayPreferences.setProfilePushPending(true)
        }
        this.profile = next
        return next
    }

    private suspend fun pushProfileToServer(profile: UserProfile, token: String): Result<String?> =
        runCatching { ProfileUpdateApi.update(profile, token).getOrThrow() }

    private fun applyRemoteUserToCache(user: LoginUserDto) {
        LoginPreferences.setCachedFullName(user.full_name)
        LoginPreferences.setCachedUsername(user.username?.trim()?.takeIf { it.isNotEmpty() })
        LoginPreferences.setCachedAccountEmail(user.email?.trim()?.lowercase()?.ifBlank { null })
        LoginPreferences.setCachedPhone(user.phone)
        LoginPreferences.setCachedRole(user.role)
        LoginPreferences.setCachedAvatarUrl(user.profile_avatar_url)
    }

    private suspend fun profileFromRemoteUser(user: LoginUserDto): UserProfile {
        val email = user.email?.trim()?.lowercase().orEmpty()
            .ifEmpty { LoginPreferences.getCachedAccountEmail().orEmpty() }
        val avatarBytes = ProfileAvatarLoader.loadBytes(
            user.profile_avatar_url ?: LoginPreferences.getCachedAvatarUrl(),
        )
        return UserProfile(
            fullName = user.full_name.orEmpty().ifBlank { LoginPreferences.getCachedFullName().orEmpty() },
            username = user.username.orEmpty().ifBlank { LoginPreferences.getCachedUsername().orEmpty() },
            email = email,
            phone = user.phone.orEmpty().ifBlank { LoginPreferences.getCachedPhone().orEmpty() },
            address = "",
            caravanType = cachedCaravanTypeForProfile(),
            avatarBytes = avatarBytes,
            accountStatus = AccountStatus.Approved,
            userType = roleToUserType(user.role ?: LoginPreferences.getCachedRole()),
            pairedDeviceMac = cachedPairedMacForProfile(),
            deviceUid = cachedDeviceUidForProfile(),
            remoteAccessActive = true,
        )
    }

    private suspend fun profileFromLocalCache(): UserProfile {
        val avatarBytes = ProfileAvatarLoader.loadBytes(LoginPreferences.getCachedAvatarUrl())
        return UserProfile(
            fullName = LoginPreferences.getCachedFullName().orEmpty(),
            username = LoginPreferences.getCachedUsername().orEmpty(),
            email = LoginPreferences.getCachedAccountEmail().orEmpty(),
            phone = LoginPreferences.getCachedPhone().orEmpty(),
            address = "",
            caravanType = cachedCaravanTypeForProfile(),
            avatarBytes = avatarBytes,
            accountStatus = AccountStatus.Approved,
            userType = roleToUserType(LoginPreferences.getCachedRole()),
            pairedDeviceMac = cachedPairedMacForProfile(),
            deviceUid = cachedDeviceUidForProfile(),
            remoteAccessActive = true,
        )
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.Default) {
        if (!loggedIn) return@withContext Result.failure(IllegalStateException("not_signed_in"))
        persistLocalProfile(profile, touchLocalTime = true)
        ProfileLocalOverride.noteLocalWrite()
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            return@withContext Result.success(Unit)
        }
        val pushed = pushProfileToServer(profile, token)
        if (pushed.isSuccess) {
            val url = pushed.getOrNull()
            if (!url.isNullOrBlank()) {
                LoginPreferences.setCachedAvatarUrl(url)
            }
            ProfileDisplayPreferences.setProfilePushPending(false)
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            ProfileDisplayPreferences.setServerProfileUpdatedAtMs(now)
            val avatarBytes = ProfileAvatarLoader.loadBytes(LoginPreferences.getCachedAvatarUrl())
            this@DefaultProfileRepository.profile = profile.copy(avatarBytes = avatarBytes)
        }
        Result.success(Unit)
    }

    override suspend fun submitRegisterRequest(input: RegisterRequestInput): Result<RegisterRequestResult> =
        withContext(Dispatchers.Default) {
            RegisterRequestApi.submit(input)
        }

    override suspend fun resolveDeviceUidByMac(mac: String): Result<String?> =
        withContext(Dispatchers.Default) {
            DeviceUidLookupApi.lookupByMac(mac)
        }

    override suspend fun signInWithCredentials(loginIdentifier: String, password: String): Result<UserProfile> =
        withContext(Dispatchers.Default) {
            val ok = LoginApi.login(loginIdentifier, password).getOrElse { return@withContext Result.failure(it) }
            com.hextech.vango.mqtt.MqttCredentialsProvider.cacheFromLoginUser(ok.user)
            applyThemeFromServerPreference(ok.user?.theme_preference)
            applyAccentFromServerPreference(ok.user?.theme_accent_id)
            applyLocaleFromServerPreference(ok.user?.locale_preference)
            loggedIn = true
            LoginPreferences.setAuthToken(ok.token)
            LoginPreferences.setAuthExpiresAt(ok.expiresAt)
            val resolvedEmail = ok.user?.email?.trim()?.lowercase().orEmpty()
            if (resolvedEmail.isNotEmpty()) {
                LoginPreferences.setCachedAccountEmail(resolvedEmail)
            }
            if (LoginPreferences.isRememberMeEnabled()) {
                val em = resolvedEmail.ifEmpty {
                    if (loginIdentifier.contains('@')) loginIdentifier.trim().lowercase() else ""
                }
                if (em.isNotEmpty()) {
                    LoginPreferences.setRememberedEmail(em)
                }
            }
            LoginPreferences.setCachedUsername(ok.user?.username?.trim()?.takeIf { it.isNotEmpty() })
            LoginPreferences.setCachedFullName(ok.user?.full_name)
            LoginPreferences.setCachedPhone(ok.user?.phone)
            LoginPreferences.setCachedRole(ok.user?.role)
            LoginPreferences.setCachedAvatarUrl(ok.user?.profile_avatar_url)
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val remoteAt = ok.user?.let { parseRemoteProfileUpdatedAt(it) } ?: 0L
            ProfileDisplayPreferences.setServerProfileUpdatedAtMs(if (remoteAt > 0L) remoteAt else now)
            if (!ProfileDisplayPreferences.isProfilePushPending()) {
                ProfileDisplayPreferences.setLocalProfileUpdatedAtMs(
                    if (remoteAt > 0L) remoteAt else now,
                )
            }
            val avatarBytes = ProfileAvatarLoader.loadBytes(ok.user?.profile_avatar_url)
            val emailForProfile = resolvedEmail.ifEmpty {
                if (loginIdentifier.contains('@')) loginIdentifier.trim().lowercase() else ""
            }
            profile = UserProfile(
                fullName = ok.user?.full_name.orEmpty(),
                username = ok.user?.username.orEmpty(),
                email = emailForProfile,
                phone = ok.user?.phone.orEmpty(),
                address = "",
                caravanType = cachedCaravanTypeForProfile(),
                avatarBytes = avatarBytes,
                accountStatus = AccountStatus.Approved,
                userType = roleToUserType(ok.user?.role),
                pairedDeviceMac = cachedPairedMacForProfile(),
                deviceUid = cachedDeviceUidForProfile(),
                remoteAccessActive = true,
            )
            Result.success(profile)
        }

    override suspend fun uploadProfileAvatar(profile: UserProfile): Result<Unit> = withContext(Dispatchers.Default) {
        if (!loggedIn) return@withContext Result.failure(IllegalStateException("not_signed_in"))
        val bytes = profile.avatarBytes
        if (bytes == null || bytes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Önce bir fotoğraf seçin."))
        }
        val token = LoginPreferences.getAuthToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Oturum bulunamadı."))
        }
        val newUrl = ProfileUpdateApi.update(profile, token).getOrElse { return@withContext Result.failure(it) }
        if (!newUrl.isNullOrBlank()) {
            LoginPreferences.setCachedAvatarUrl(newUrl)
        }
        val loaded = ProfileAvatarLoader.loadBytes(LoginPreferences.getCachedAvatarUrl())
        this@DefaultProfileRepository.profile = profile.copy(avatarBytes = loaded)
        Result.success(Unit)
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.Default) {
        delay(80)
        loggedIn = false
        profile = UserProfile.guest()
        LoginPreferences.clearAuthSession()
        ProfileDisplayPreferences.clearProfileSyncState()
        com.hextech.vango.mqtt.MqttCredentialsProvider.clearCache()
        Result.success(Unit)
    }

    override suspend fun requestDevicePairing(): Result<Unit> = withContext(Dispatchers.Default) {
        delay(100)
        Result.success(Unit)
    }

    private fun roleToUserType(role: String?): UserType = when (role?.trim()?.lowercase()) {
        "super_admin", "company_admin", "company_operator", "admin" -> UserType.Admin
        "customer_user" -> UserType.Customer
        "company_viewer" -> UserType.Standard
        else -> UserType.Standard
    }
}
