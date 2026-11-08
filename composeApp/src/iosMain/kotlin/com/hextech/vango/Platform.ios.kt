package com.hextech.vango

import platform.UIKit.UIDevice

private class IosBluetoothManager : BluetoothManager {
    override fun bluetoothSupport() = false
    override fun bluetoothEnabled() = false
    override fun isConnected() = false
    override fun connect(index: Int) = false
    override fun disconnect() = Unit
    override fun startDiscovering() = false
    override fun stopDiscovering() = Unit
    override fun discoverServices() = false
    override fun configureMtu() = false
    override fun enableReadChar() = false
    override fun sendData(data: String) = false
    override suspend fun sendWeatherData(data: String) = false
}

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
