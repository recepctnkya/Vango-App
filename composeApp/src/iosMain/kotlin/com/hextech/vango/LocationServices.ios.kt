package com.hextech.vango

import platform.CoreLocation.CLLocationManager

actual fun isLocationServicesEnabled(): Boolean = CLLocationManager.locationServicesEnabled()

actual fun bleScanRequiresLocationEnabled(): Boolean = false
