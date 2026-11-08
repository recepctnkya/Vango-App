package com.hextech.vango.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun DeviceQrScannerView(
    modifier: Modifier = Modifier,
    flashEnabled: Boolean = false,
    /** Metin okundu; true dönerse tekrar tarama durur. */
    onQrScanned: (String) -> Boolean,
    onCameraError: (String) -> Unit,
)
