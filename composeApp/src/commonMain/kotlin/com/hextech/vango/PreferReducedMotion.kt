package com.hextech.vango

import androidx.compose.runtime.Composable

/** True when the platform asks to minimize non-essential motion. */
@Composable
expect fun rememberPrefersReducedMotion(): Boolean
