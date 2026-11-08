package com.hextech.vango

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val bleManager:BluetoothManager = BluetoothManager()

    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override fun launchBTController() {
        // iOS does not expose a Bluetooth settings controller entrypoint here.
    }
}

class IOSVanGO: VanGO {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
actual fun getVanGO(): VanGO = IOSVanGO()

actual object `AppContext.android`
actual class BluetoothManager {
    actual fun getConnectedDevices(): List<String> {
        return emptyList()
    }
}
