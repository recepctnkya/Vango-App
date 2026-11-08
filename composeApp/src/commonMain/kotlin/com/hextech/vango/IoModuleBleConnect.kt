package com.hextech.vango

import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.ManualPairedDeviceStore
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay

/**
 * Panel kaydındaki WiFi MAC'ten yalnızca BLE adaptör MAC'ini manuel listeye yazar.
 * WiFi (STA) MAC BLE listesinde gösterilmez.
 */
fun seedIoModuleFromPlatformRegistration() {
    pruneWifiMacTwinsFromManualStore()

    val wifiMac = currentPlatformDeviceMac()
        ?: LoginPreferences.getCachedPairedDeviceMac()?.trim()?.takeUnless {
            it.isEmpty() || it == "-" || it == "\u2014"
        }
        ?: return
    val normalizedWifi = normalizeMacForDisplay(wifiMac)
    if (normalizedWifi.length != 17 || !isValidMacFormat(normalizedWifi)) return

    val uid = LoginPreferences.getCachedDeviceUid()?.trim()?.takeIf { it.isNotEmpty() }
    val bleMac = deriveBleMacFromWifiMac(normalizedWifi) ?: return

    // Listeyi sadeleştir: yalnızca doğru BLE MAC kalsın
    ManualPairedDeviceStore.getAll()
        .map { it.mac }
        .filter { !macsEqual(it, bleMac) }
        .forEach { ManualPairedDeviceStore.remove(it) }

    if (ManualPairedDeviceStore.macExists(bleMac)) {
        ManualPairedDeviceStore.ensureMacInDiscoveryList(bleMac)
        return
    }
    runCatching {
        ManualPairedDeviceStore.add(bleMac, uid, bleIoModuleName)
    }.onFailure {
        ManualPairedDeviceStore.ensureMacInDiscoveryList(bleMac)
    }
}

/**
 * Yalnızca bilinen WiFi (STA) kaydını BLE'ye çevirir / siler.
 * UYARI: Listedeki her MAC'e körü körüne +2 uygulanmamalı (eski bug zincir kirlenmesi yaratıyordu).
 */
fun pruneWifiMacTwinsFromManualStore() {
    val knownWifi = currentPlatformDeviceMac()
        ?: LoginPreferences.getCachedPairedDeviceMac()?.let(::normalizeMacForDisplay)
        ?: return
    val bleFromWifi = deriveBleMacFromWifiMac(knownWifi) ?: return

    // WiFi STA kaydı varsa kaldır; BLE kardeşini tut/ekle
    ManualPairedDeviceStore.getAll()
        .filter { macsEqual(it.mac, knownWifi) }
        .forEach { ManualPairedDeviceStore.remove(it.mac) }

    // Yanlışlıkla +2 zinciri oluşmuş kayıtları temizle (doğru BLE dışındakiler)
    val lastBle = LoginPreferences.getCachedLastBleAdapterMac()?.let(::normalizeMacForDisplay)
    ManualPairedDeviceStore.getAll().forEach { entry ->
        val mac = normalizeMacForDisplay(entry.mac)
        val keep = macsEqual(mac, bleFromWifi) || (lastBle != null && macsEqual(mac, lastBle))
        if (!keep) {
            // Aynı OUI önekli ve bilinen BLE'nin +2 türevleri gibi duran kirleri sil
            if (mac.take(15).equals(bleFromWifi.take(15), ignoreCase = true) && !macsEqual(mac, bleFromWifi)) {
                ManualPairedDeviceStore.remove(mac)
            }
        }
    }

    // Keşif listesinden WiFi STA'yı düş
    val dropIdx = vangoDeviceList.indexOfFirst { macsEqual(it.mac, knownWifi) }
    if (dropIdx >= 0) {
        vangoDeviceList.removeAt(dropIdx)
        vangoDeviceListSize.value = vangoDeviceList.size
    }
}

/**
 * Listede WiFi/BLE ikizleri varsa yalnızca BLE adaptör MAC'ini bırakır.
 */
fun preferBleAdapterDevices(devices: List<BleDevice>): List<BleDevice> {
    if (devices.size <= 1) return devices
    val normalized = devices.map {
        it.copy(mac = normalizeMacForDisplay(it.mac))
    }
    val knownWifi = currentPlatformDeviceMac()
        ?: LoginPreferences.getCachedPairedDeviceMac()?.let(::normalizeMacForDisplay)
    val drop = mutableSetOf<String>()
    for (d in normalized) {
        if (knownWifi != null && macsEqual(d.mac, knownWifi)) {
            drop.add(d.mac.uppercase())
            continue
        }
        val ble = deriveBleMacFromWifiMac(d.mac) ?: continue
        if (macsEqual(d.mac, ble)) continue
        // d WiFi gibi duruyor ve BLE kardeşi listede varsa WiFi'yi düş
        if (normalized.any { macsEqual(it.mac, ble) }) {
            drop.add(d.mac.uppercase())
        }
    }
    return normalized.filterNot { drop.contains(it.mac.uppercase()) }
}
