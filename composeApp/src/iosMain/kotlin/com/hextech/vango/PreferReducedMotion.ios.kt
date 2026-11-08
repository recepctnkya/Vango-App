package com.hextech.vango

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibility

@Composable
actual fun rememberPrefersReducedMotion(): Boolean =
    remember { UIAccessibility.isReduceMotionEnabled() }
