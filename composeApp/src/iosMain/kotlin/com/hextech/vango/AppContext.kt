package com.hextech.vango

import platform.UIKit.UIApplication

private class IosAppContext : IAppContext {
    override val application: Any = UIApplication.sharedApplication
    override val bluetoothManager: BluetoothManager = createIosBluetoothManager()
    override val vanGO: VanGO = createIosVanGO()
}

private var appContext: IAppContext = IosAppContext()

actual fun setAppContext(appC: IAppContext) {
    appContext = appC
}

actual fun getAppContext(): IAppContext = appContext
