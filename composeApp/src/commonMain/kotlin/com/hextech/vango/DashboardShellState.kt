package com.hextech.vango

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Tablet dashboard → Ayarlar içi sekme (sidebar). */
enum class DashboardSettingsNav {
    Panel, Connection, Theme, Language, Profile, Notifications, About
}

/** Phone Dim page: which list to show (home “Tümü >” targets). */
enum class PhoneDimPageFocus {
    Dimmers,
    Relays,
}

/** Phone Dim / Relays full list focus (Dim page). */
var dashboardDimPageFocus by mutableStateOf(PhoneDimPageFocus.Dimmers)

/** Close settings and open this dashboard section (e.g. panel motor Önizle). */
var dashboardNavigateToSection by mutableStateOf<DashboardSection?>(null)

/** Ayarlar ekranı açık mı (footer'dan Ayarlar seçili). BLE state değişince kaybolmaması için remember dışında tutulur. */
var dashboardShowSettings by mutableStateOf(false)

/** Phone: bell opens event inbox (not settings rules). */
var dashboardShowNotificationsInbox by mutableStateOf(false)

/** Phone: home hero caravan card → type picker page. */
var dashboardShowCaravanPage by mutableStateOf(false)

/** Phone: Hexon local assistant chat overlay. */
var dashboardShowHexon by mutableStateOf(false)

/** Ayarlar içinde seçili sidebar öğesi. */
var dashboardSettingsNav by mutableStateOf(DashboardSettingsNav.Panel)

/**
 * Phone settings: false = full-width menu list, true = detail page.
 * Tablet always shows sidebar + detail together.
 */
var dashboardSettingsDetailOpen by mutableStateOf(false)

internal fun isOnTabletSettingsConnectionPage(): Boolean =
    dashboardShowSettings && dashboardSettingsNav == DashboardSettingsNav.Connection

internal fun isOnTabletSettingsShell(): Boolean = dashboardShowSettings
