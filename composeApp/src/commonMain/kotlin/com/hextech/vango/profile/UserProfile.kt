package com.hextech.vango.profile

/**
 * Domain model for the user profile screen. Kept serialization-free so an API DTO can map
 * to/from this type later without tying the UI to wire format.
 */
data class UserProfile(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val caravanType: CaravanType = CaravanType.TowCaravan,
    /** In-memory avatar (e.g. from gallery); replace with URL when backend exists. */
    val avatarBytes: ByteArray? = null,
    val accountStatus: AccountStatus = AccountStatus.Guest,
    val userType: UserType = UserType.Guest,
    /** Last known / server-side paired device MAC (may be overridden in UI by live BLE). */
    val pairedDeviceMac: String = "",
    val deviceUid: String = "",
    val remoteAccessActive: Boolean = false,
) {
    companion object {
        fun guest(): UserProfile = UserProfile(
            caravanType = CaravanType.TowCaravan,
            accountStatus = AccountStatus.Guest,
            userType = UserType.Guest,
            remoteAccessActive = false,
        )
    }
}

enum class CaravanType {
    TowCaravan,
    MotorCaravan,
}

enum class AccountStatus {
    Guest,
    PendingApproval,
    Approved,
    Rejected,
    Passive,
}

enum class UserType {
    Guest,
    Standard,
    /** End-user / müşteri (Hexnet IoT). */
    Customer,
    Premium,
    Service,
    Admin,
}
