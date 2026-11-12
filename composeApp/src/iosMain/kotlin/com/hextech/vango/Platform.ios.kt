package com.hextech.vango

import platform.UIKit.UIDevice

private class IosPlatform : Platform {
    override val name: String
        get() = "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"
}

private class IosVanGO : VanGO() {
    override fun checkPermissions() = true
}

actual fun getPlatform(): Platform = IosPlatform()
actual fun getVanGO(): VanGO = IosVanGO()

internal fun createIosBluetoothManager(): BluetoothManager = IosBluetoothManager()

internal fun createIosVanGO(): VanGO = IosVanGO()
