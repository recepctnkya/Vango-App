package com.hextech.vango

import androidx.compose.runtime.Composable

/**
 * Device class for UI shell selection (single APK, two interfaces).
 *
 * - **Phone** (`smallestScreenWidthDp < 600` on Android): [PhoneAppTopBar], swipe tabs,
 *   hamburger drawer, [PhoneHomeDashboard].
 * - **Tablet** (`smallestScreenWidthDp >= 600`): [DashboardHeaderBar], dashboard grid,
 *   tablet footer.
 *
 * Fixed for the app process — does not change on rotation.
 */
@Composable
fun getDeviceUiClass(): DeviceUiClass =
    if (isTablet()) DeviceUiClass.Tablet else DeviceUiClass.Phone

enum class DeviceUiClass {
    Phone,
    Tablet,
}

@Composable
fun isTablet(): Boolean = getDeviceType().equals("Tablet", ignoreCase = true)

/** True when the phone UI shell should be shown (not the tablet board). */
@Composable
fun isPhoneUi(): Boolean = !isTablet()
