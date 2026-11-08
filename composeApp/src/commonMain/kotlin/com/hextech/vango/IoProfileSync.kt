package com.hextech.vango

import com.hextech.vango.profile.LoginPreferences

/**
 * BLE / MQTT / UI [DevConfig] ile IO modül flash profilini hizalar.
 */
fun applyIoProfileToUi(devConfig: DevConfig, profile: IoProfileSnapshot): DevConfig =
    mergeDevConfigWithIoProfile(devConfig, profile)

fun applyVanGoBleProfileToUi(data: VanGoData) {
    val fields = IoProfileBleFields(
        motorEn = data.motorEn,
        rgbEn = data.rgbEn,
        tankN = data.tankN,
        relayMask = data.relayMask,
        dimMask = data.dimMask,
        sensorMask = data.sensorMask,
        tankMask = data.tankMask,
        ioLink = data.ioLink,
        io = data.io,
    )
    val snap = fields.toSnapshot() ?: return
    val seq = data.panelSeq?.takeIf { it > 0 } ?: snap.panelSeq
    val changed = data.panelChanged == true || snap.panelChanged
    val enriched = if (seq > 0 || snap.channels != null) {
        snap.copy(panelSeq = seq, panelChanged = changed)
    } else {
        snap
    }
    if (!PanelSettingsPlatformSync.shouldPreservePlatformPanelLayout()) {
        val ui = uiState.value
        uiState.value = ui.copy(devConfig = applyIoProfileToUi(ui.devConfig, enriched))
    }
    applyIoChannelLabelsFromDevice(enriched)
}

fun applyRemoteIoProfileToUi(profile: IoProfileSnapshot?) {
    if (profile == null) return
    val ui = uiState.value
    uiState.value = ui.copy(devConfig = applyIoProfileToUi(ui.devConfig, profile))
    syncVanGoRuntimeFromDevConfig(uiState.value.devConfig)
    applyIoChannelLabelsFromDevice(profile)
}

/**
 * IO flash isim/ikon SoT — panel_seq daha yeniyse veya panel_changed ise tablete uygula.
 */
fun applyIoChannelLabelsFromDevice(profile: IoProfileSnapshot) {
    val channels = profile.channels ?: return
    if (channels.relays.isEmpty() && channels.dimmers.isEmpty() &&
        channels.sensors.isEmpty() && channels.water.isEmpty()
    ) {
        return
    }
    /* Ayar ekranında kaydedilmemiş düzenlemeyi ezme */
    if (uiState.value.settingsPage == SettingsPage.Panel && panelSettingsSession.value.hasChanges) {
        return
    }
    val lastSeq = PanelSettingsPlatformSync.readCachedProfile()?.panelSeq ?: 0
    val seq = profile.panelSeq
    if (!profile.panelChanged && seq > 0 && seq <= lastSeq) {
        return
    }
    val profileKey = devicePanelProfile.value.profileKey
    val base = PanelSettingsPlatformSync.readCachedPanelConfig()
        ?: defaultPanelPlatformConfig(profileKey)
    val merged = mergeIoChannelLabels(base.channels, channels)
    PanelSettingsPlatformSync.applyPlatformPanelConfig(
        base.copy(profile = profileKey, channels = merged),
        "io-sot-seq=$seq",
    )
    PanelSettingsPlatformSync.persistProfile(profile.copy(panelSeq = seq.coerceAtLeast(lastSeq)))
    /* Cihaz SoT ilerledi — bekleyen eski tablet push'unu iptal et */
    if (seq > lastSeq && LoginPreferences.isPanelConfigPendingBlePush()) {
        LoginPreferences.setPanelConfigPendingBlePush(false)
    }
    println("Panel labels applied from IO SoT seq=$seq relays=${merged.relays.size}")
}

private fun mergeIoChannelLabels(
    current: PanelPlatformChannels,
    fromIo: PanelPlatformChannels,
): PanelPlatformChannels {
    fun mergeList(
        cur: List<PanelPlatformChannel>,
        io: List<PanelPlatformChannel>,
    ): List<PanelPlatformChannel> {
        if (io.isEmpty()) return cur
        val byId = cur.associateBy { it.id }.toMutableMap()
        io.forEach { row ->
            val prev = byId[row.id]
            byId[row.id] = (prev ?: row).copy(
                name = row.name.ifBlank { prev?.name.orEmpty() },
                icon = row.icon.ifBlank { prev?.icon.orEmpty() },
                enabled = row.enabled,
                sensorType = row.sensorType.ifBlank { prev?.sensorType.orEmpty() },
            )
        }
        return byId.values.sortedBy { it.sortOrder.takeIf { o -> o != 0 } ?: it.id }
    }
    return current.copy(
        relays = mergeList(current.relays, fromIo.relays),
        dimmers = mergeList(current.dimmers, fromIo.dimmers),
        sensors = mergeList(current.sensors, fromIo.sensors),
        water = mergeList(current.water, fromIo.water),
        rgb = mergeList(current.rgb, fromIo.rgb),
        motors = mergeList(current.motors, fromIo.motors),
    )
}

fun syncVanGoRuntimeFromDevConfig(devConfig: DevConfig) {
    val current = vanGoData.value
    val profileCaps = devicePanelProfile.value.caps
    val outSize = profileCaps.relays.coerceIn(1, FIRMWARE_RELAY_BUFFER_MAX)
    val dimSize = profileCaps.dimmers.coerceIn(1, FIRMWARE_DIMMER_MAX)
    val outNB = padBuffer(devConfig.OutputsNameBuffer.copyOf(), outSize)
    val dimNB = padBuffer(devConfig.DimsNameBuffer.copyOf(), dimSize)
    val senNB = padBuffer(devConfig.SensorsNameBuffer.copyOf(), FIRMWARE_SENSOR_BUFFER_MAX)
    vanGoData.value = VanGoData(
        slvConn = current.slvConn,
        wifiMac = current.wifiMac,
        numOfO = profileCaps.relays.coerceAtLeast(countEnabledSlots(outNB)),
        numOfD = profileCaps.dimmers.coerceAtLeast(countEnabledSlots(dimNB)),
        numOfS = devConfig.numOfSensors.coerceAtLeast(countEnabledSlots(senNB)),
        RGBE = if (devConfig.RGBEnables.equals("yes", ignoreCase = true)) "yes" else "no",
        Th = current.Th,
        volt = current.volt,
        outNB = outNB,
        dimNB = dimNB,
        senNB = senNB,
        outDB = current.outDB,
        dDB = current.dDB,
        sDB = current.sDB,
        RGBDB = current.RGBDB,
        mac = current.mac,
        motorEn = current.motorEn,
        rgbEn = current.rgbEn,
        tankN = current.tankN,
        relayMask = current.relayMask,
        dimMask = current.dimMask,
        sensorMask = current.sensorMask,
        tankMask = current.tankMask,
        ioLink = current.ioLink,
        io = current.io,
        sensorTemp = current.sensorTemp,
        sensorHumidity = current.sensorHumidity,
        panelSeq = current.panelSeq,
        panelChanged = current.panelChanged,
    )
}

private fun padBuffer(source: ByteArray, size: Int): ByteArray {
    if (source.size >= size) return source.copyOf(size)
    return source + ByteArray(size - source.size)
}
