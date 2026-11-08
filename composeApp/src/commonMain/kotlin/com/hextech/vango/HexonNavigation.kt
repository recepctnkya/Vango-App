package com.hextech.vango

/**
 * Destinations Hexon can open in the live phone shell.
 * Keep in sync with [navigateHexonDestination] and DashboardHomeScreen handlers.
 */
enum class HexonDestination {
    Home,
    DimDimmers,
    DimRelays,
    Water,
    Climate,
    Motor,
    Energy,
    NotificationsInbox,
    CaravanType,
    SettingsList,
    SettingsPanel,
    SettingsConnection,
    SettingsTheme,
    SettingsLanguage,
    SettingsProfile,
    SettingsNotificationPrefs,
    SettingsAbout,
}

fun HexonDestination.titleTr(): String = when (this) {
    HexonDestination.Home -> "Ana Sayfa"
    HexonDestination.DimDimmers -> "Aydınlatmalar"
    HexonDestination.DimRelays -> "Butonlar"
    HexonDestination.Water -> "Su Sistemi"
    HexonDestination.Climate -> "Sensör"
    HexonDestination.Motor -> "Motor"
    HexonDestination.Energy -> "Enerji İzleme"
    HexonDestination.NotificationsInbox -> "Bildirimler"
    HexonDestination.CaravanType -> "Karavan Tipi"
    HexonDestination.SettingsList -> "Ayarlar"
    HexonDestination.SettingsPanel -> "Panel Ayarları"
    HexonDestination.SettingsConnection -> "Bağlantı"
    HexonDestination.SettingsTheme -> "Tema"
    HexonDestination.SettingsLanguage -> "Dil"
    HexonDestination.SettingsProfile -> "Profil"
    HexonDestination.SettingsNotificationPrefs -> "Bildirim Ayarları"
    HexonDestination.SettingsAbout -> "Hakkında & Yardım"
}

/** Apply shell flags so DashboardHomeScreen opens the target. Closes Hexon. */
fun navigateHexonDestination(dest: HexonDestination) {
    dashboardShowHexon = false
    dashboardShowNotificationsInbox = false
    dashboardShowCaravanPage = false
    dashboardShowSettings = false
    dashboardSettingsDetailOpen = false

    when (dest) {
        HexonDestination.Home -> {
            dashboardNavigateToSection = DashboardSection.Home
        }
        HexonDestination.DimDimmers -> {
            dashboardDimPageFocus = PhoneDimPageFocus.Dimmers
            dashboardNavigateToSection = DashboardSection.Dim
        }
        HexonDestination.DimRelays -> {
            dashboardDimPageFocus = PhoneDimPageFocus.Relays
            dashboardNavigateToSection = DashboardSection.Dim
        }
        HexonDestination.Water -> {
            dashboardNavigateToSection = DashboardSection.Water
        }
        HexonDestination.Climate -> {
            dashboardNavigateToSection = DashboardSection.Climate
        }
        HexonDestination.Motor -> {
            dashboardNavigateToSection = DashboardSection.Motor
        }
        HexonDestination.Energy -> {
            dashboardNavigateToSection = DashboardSection.Energy
        }
        HexonDestination.NotificationsInbox -> {
            dashboardShowNotificationsInbox = true
        }
        HexonDestination.CaravanType -> {
            dashboardShowCaravanPage = true
        }
        HexonDestination.SettingsList -> {
            dashboardSettingsNav = DashboardSettingsNav.Panel
            dashboardSettingsDetailOpen = false
            dashboardShowSettings = true
        }
        HexonDestination.SettingsPanel -> {
            dashboardSettingsNav = DashboardSettingsNav.Panel
            dashboardSettingsDetailOpen = true
            dashboardShowSettings = true
        }
        HexonDestination.SettingsConnection -> {
            dashboardSettingsNav = DashboardSettingsNav.Connection
            dashboardSettingsDetailOpen = true
            dashboardShowSettings = true
        }
        HexonDestination.SettingsTheme -> {
            dashboardSettingsNav = DashboardSettingsNav.Theme
            dashboardSettingsDetailOpen = true
            dashboardShowSettings = true
        }
        HexonDestination.SettingsLanguage -> {
            dashboardSettingsNav = DashboardSettingsNav.Language
            dashboardSettingsDetailOpen = true
            dashboardShowSettings = true
        }
        HexonDestination.SettingsProfile -> {
            dashboardSettingsNav = DashboardSettingsNav.Profile
            dashboardSettingsDetailOpen = true
            dashboardShowSettings = true
        }
        HexonDestination.SettingsNotificationPrefs -> {
            dashboardSettingsNav = DashboardSettingsNav.Notifications
            dashboardSettingsDetailOpen = true
            dashboardShowSettings = true
        }
        HexonDestination.SettingsAbout -> {
            dashboardSettingsNav = DashboardSettingsNav.About
            dashboardSettingsDetailOpen = true
            dashboardShowSettings = true
        }
    }
}
