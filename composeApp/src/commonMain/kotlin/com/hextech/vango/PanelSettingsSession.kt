package com.hextech.vango

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hextech.vango.profile.LoginPreferences

data class PanelSettingsSessionState(
    val baseline: DevConfig? = null,
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val showSavedPopup: Boolean = false,
    val savedMessage: String = "Panel ayarlarınız kaydedildi",
    val showDiscardPopup: Boolean = false,
)

private val _panelSettingsSession = MutableStateFlow(PanelSettingsSessionState())
val panelSettingsSession: StateFlow<PanelSettingsSessionState> = _panelSettingsSession.asStateFlow()

private val _panelPlatformChannels = MutableStateFlow<PanelPlatformChannels?>(null)
val panelPlatformChannels: StateFlow<PanelPlatformChannels?> = _panelPlatformChannels.asStateFlow()

fun resetPanelPlatformChannels(channels: PanelPlatformChannels?) {
    _panelPlatformChannels.value = channels?.copy(water = sanitizeWaterChannels(channels.water))
}

fun ensurePanelChannelsLoadedFromCache() {
    if (_panelPlatformChannels.value != null) return
    PanelSettingsPlatformSync.readCachedPanelConfig()?.channels?.let { cached ->
        resetPanelPlatformChannels(cached.copy(water = sanitizeWaterChannels(cached.water)))
    }
}

private fun currentOrDefaultPanelChannels(): PanelPlatformChannels =
    _panelPlatformChannels.value
        ?: defaultPanelPlatformConfig(devicePanelProfile.value.profileKey).channels

fun updatePanelSensorChannel(id: Int, transform: (PanelPlatformChannel) -> PanelPlatformChannel) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(
        sensors = upsertChannel(current.sensors, id, transform),
    )
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun replacePanelSensorChannels(sensors: List<PanelPlatformChannel>) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(sensors = sensors)
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun updatePanelRelayChannel(id: Int, transform: (PanelPlatformChannel) -> PanelPlatformChannel) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(
        relays = upsertChannel(current.relays, id, transform),
    )
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun replacePanelRelayChannels(relays: List<PanelPlatformChannel>) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(relays = relays)
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun swapPanelRelayContent(idA: Int, idB: Int) {
    swapChannelContent(idA, idB, { it.relays }) { copy, list -> copy.copy(relays = list) }
}

fun updatePanelDimmerChannel(id: Int, transform: (PanelPlatformChannel) -> PanelPlatformChannel) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(
        dimmers = upsertChannel(current.dimmers, id, transform),
    )
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun replacePanelDimmerChannels(dimmers: List<PanelPlatformChannel>) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(dimmers = dimmers)
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun swapPanelDimmerContent(idA: Int, idB: Int) {
    swapChannelContent(idA, idB, { it.dimmers }) { copy, list -> copy.copy(dimmers = list) }
}

fun updatePanelRgbChannel(id: Int, transform: (PanelPlatformChannel) -> PanelPlatformChannel) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(
        rgb = upsertChannel(current.rgb, id, transform),
    )
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun replacePanelRgbChannels(rgb: List<PanelPlatformChannel>) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(rgb = rgb)
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun addPanelRgbChannel() {
    val cap = devicePanelProfile.value.caps.rgb.coerceAtLeast(1)
    val current = currentOrDefaultPanelChannels()
    val id = nextFreeChannelId(current.rgb, cap) ?: return
    replacePanelRgbChannels(
        current.rgb + PanelPlatformChannel(
            id = id,
            enabled = true,
            name = "RGB Aydınlatma",
            icon = "rgb_aydinlatma",
            sortOrder = id,
        ),
    )
}

fun removePanelRgbChannel(id: Int) {
    val current = currentOrDefaultPanelChannels()
    replacePanelRgbChannels(current.rgb.filter { it.id != id })
}

fun updatePanelMotorChannel(id: Int, transform: (PanelPlatformChannel) -> PanelPlatformChannel) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(
        motors = upsertChannel(current.motors, id, transform),
    )
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun replacePanelMotorChannels(motors: List<PanelPlatformChannel>) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(motors = motors)
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun addPanelMotorChannel() {
    val cap = devicePanelProfile.value.caps.motors.coerceAtLeast(1)
    val current = currentOrDefaultPanelChannels()
    val id = nextFreeChannelId(current.motors, cap) ?: return
    replacePanelMotorChannels(
        current.motors + PanelPlatformChannel(
            id = id,
            enabled = true,
            name = "Motor ${id + 1}",
            icon = "fan",
            sortOrder = id,
        ),
    )
}

fun removePanelMotorChannel(id: Int) {
    val current = currentOrDefaultPanelChannels()
    replacePanelMotorChannels(current.motors.filter { it.id != id })
}

fun swapPanelMotorContent(idA: Int, idB: Int) {
    swapChannelContent(idA, idB, { it.motors }) { copy, list -> copy.copy(motors = list) }
}

private fun nextFreeChannelId(list: List<PanelPlatformChannel>, max: Int): Int? {
    val used = list.map { it.id }.toSet()
    return (0 until max).firstOrNull { it !in used }
}

fun updatePanelWaterChannel(id: Int, transform: (PanelPlatformChannel) -> PanelPlatformChannel) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(
        water = upsertChannel(current.water, id, transform),
    )
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun replacePanelWaterChannels(water: List<PanelPlatformChannel>) {
    val current = currentOrDefaultPanelChannels()
    _panelPlatformChannels.value = current.copy(water = water)
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

fun swapPanelWaterContent(idA: Int, idB: Int) {
    swapChannelContent(idA, idB, { it.water }) { copy, list -> copy.copy(water = list) }
}

fun swapPanelSensorContent(idA: Int, idB: Int) {
    swapChannelContent(idA, idB, { it.sensors }) { copy, list -> copy.copy(sensors = list) }
}

private fun upsertChannel(
    list: List<PanelPlatformChannel>,
    id: Int,
    transform: (PanelPlatformChannel) -> PanelPlatformChannel,
): List<PanelPlatformChannel> {
    val existing = list.firstOrNull { it.id == id }
    return if (existing != null) {
        list.map { row -> if (row.id == id) transform(row) else row }
    } else {
        list + transform(
            PanelPlatformChannel(id = id, enabled = false, name = "CH ${id + 1}", icon = "circle", sortOrder = id),
        )
    }
}

private fun swapChannelContent(
    idA: Int,
    idB: Int,
    read: (PanelPlatformChannels) -> List<PanelPlatformChannel>,
    write: (PanelPlatformChannels, List<PanelPlatformChannel>) -> PanelPlatformChannels,
) {
    if (idA == idB) return
    val current = currentOrDefaultPanelChannels()
    val list = read(current)
    val a = list.firstOrNull { it.id == idA } ?: return
    val b = list.firstOrNull { it.id == idB } ?: return
    _panelPlatformChannels.value = write(
        current,
        list.map { row ->
            when (row.id) {
                idA -> row.copy(
                    name = b.name,
                    icon = b.icon,
                    enabled = b.enabled,
                    sensorType = b.sensorType,
                    capacityLiters = b.capacityLiters,
                    showOnHome = b.showOnHome,
                )
                idB -> row.copy(
                    name = a.name,
                    icon = a.icon,
                    enabled = a.enabled,
                    sensorType = a.sensorType,
                    capacityLiters = a.capacityLiters,
                    showOnHome = a.showOnHome,
                )
                else -> row
            }
        },
    )
    syncPanelChannelsToDevConfig()
    markPanelConfigTouched()
}

/** @deprecated Use [swapPanelRelayContent] so Röle numbers stay bound to slot id. */
fun swapPanelRelaySortOrder(idA: Int, idB: Int) = swapPanelRelayContent(idA, idB)

private fun syncPanelChannelsToDevConfig() {
    val channels = _panelPlatformChannels.value ?: return
    val profileKey = devicePanelProfile.value.profileKey
    val platform = PanelPlatformConfigRoot(
        profile = profileKey,
        channels = channels,
    )
    val merged = PanelConfigMapper.applyPlatformConfigToDevConfig(platform, uiState.value.devConfig)
    uiState.value = uiState.value.copy(devConfig = merged)
    syncVanGoRuntimeFromDevConfig(merged)
}

private fun syncRelayChannelsToDevConfig() = syncPanelChannelsToDevConfig()

fun setPanelSensorEnabled(sensor: PanelPlatformChannel, enabled: Boolean) {
    updatePanelSensorChannel(sensor.id) { it.copy(enabled = enabled) }
    val type = normalizePanelSensorType(sensor.sensorType)
    when {
        sensor.id == 0 && type == "temp_humidity" ->
            syncTempHumiditySensorToDevConfig(enabled, iconPreset = 1)
        sensor.id <= 1 ->
            syncSensorSlotToDevConfig(sensor.id, enabled, iconPreset = 1)
    }
}

fun setPanelSensorType(
    sensorId: Int,
    sensorType: String,
    updateName: Boolean = true,
) {
    val normalized = normalizePanelSensorType(sensorType)
    updatePanelSensorChannel(sensorId) { row ->
        row.copy(
            sensorType = normalized,
            name = if (updateName) sensorDefaultName(normalized, sensorId) else row.name,
        )
    }
    if (sensorId == 0 && normalized != "temp_humidity") {
        val cfg = uiState.value.devConfig
        val buf = cfg.SensorsNameBuffer.copyOf()
        buf[1] = 0
        uiState.value = uiState.value.copy(devConfig = cfg.copy(SensorsNameBuffer = buf))
        markPanelConfigTouched()
    }
}

private fun cloneDevConfig(config: DevConfig): DevConfig = config.copy(
    OutputsNameBuffer = config.OutputsNameBuffer.copyOf(),
    DimsNameBuffer = config.DimsNameBuffer.copyOf(),
    SensorsNameBuffer = config.SensorsNameBuffer.copyOf(),
)

private fun devConfigContentEquals(a: DevConfig, b: DevConfig): Boolean {
    return a.MessageType == b.MessageType &&
        a.numOfOutputs == b.numOfOutputs &&
        a.OutputsNameBuffer.contentEquals(b.OutputsNameBuffer) &&
        a.numOfDims == b.numOfDims &&
        a.DimsNameBuffer.contentEquals(b.DimsNameBuffer) &&
        a.numOfSensors == b.numOfSensors &&
        a.SensorsNameBuffer.contentEquals(b.SensorsNameBuffer) &&
        a.RGBEnables == b.RGBEnables &&
        a.Theme == b.Theme &&
        a.motorEnabled == b.motorEnabled &&
        a.numOfTanks == b.numOfTanks &&
        a.tankEnableMask == b.tankEnableMask &&
        a.relayEnableMask == b.relayEnableMask &&
        a.dimEnableMask == b.dimEnableMask &&
        a.sensorEnableMask == b.sensorEnableMask &&
        a.links == b.links
}

fun beginPanelSettingsSessionIfNeeded() {
    PanelSettingsPlatformSync.bootstrapCachedProfileKind()
    _panelSettingsSession.update { it.copy(isSyncing = true, syncError = null) }
    CoroutineScope(Dispatchers.Default).launch {
        val synced = try {
            if (isRemoteSessionUsable() || isRemoteUserModeActive() || !LoginPreferences.getAuthToken().isNullOrBlank()) {
                PanelSettingsPlatformSync.refreshFromPlatform()
            } else {
                PanelSettingsPlatformSync.readCachedProfile()?.let {
                    PanelSettingsPlatformSync.applyProfile(it, "cache")
                }
                PanelSettingsPlatformSync.readCachedPanelConfig()?.let {
                    PanelSettingsPlatformSync.applyPlatformPanelConfig(it, "cache")
                }
                PanelSettingsPlatformSync.readCachedPanelConfig() != null
            }
        } catch (e: Exception) {
            println("Panel settings platform sync failed: ${e.message}")
            false
        }
        _panelSettingsSession.update { s ->
            s.copy(
                baseline = cloneDevConfig(uiState.value.devConfig),
                hasChanges = false,
                isSyncing = false,
                syncError = if (!synced && !LoginPreferences.getAuthToken().isNullOrBlank()) {
                    "Platform ayarlari yuklenemedi"
                } else {
                    null
                },
            )
        }
        PanelSettingsPlatformSync.readCachedPanelConfig()?.channels?.let {
            resetPanelPlatformChannels(it.copy(water = sanitizeWaterChannels(it.water)))
        }
            ?: resetPanelPlatformChannels(
                defaultPanelPlatformConfig(devicePanelProfile.value.profileKey).channels,
            )
    }
    refreshPanelChangesState()
}

fun refreshPanelChangesState() {
    val current = uiState.value.devConfig
    _panelSettingsSession.update { s ->
        val baseline = s.baseline ?: return@update s
        s.copy(hasChanges = !devConfigContentEquals(current, baseline))
    }
}

fun markPanelConfigTouched() {
    _panelSettingsSession.update { it.copy(hasChanges = true) }
}

fun dismissPanelSavedPopup() {
    _panelSettingsSession.update { it.copy(showSavedPopup = false, syncError = null) }
}

fun requestPanelDiscardPopup() {
    val hasChanges = _panelSettingsSession.value.hasChanges
    if (hasChanges) {
        _panelSettingsSession.update { it.copy(showDiscardPopup = true) }
    } else {
        closePanelSettingsSession()
        uiState.value = uiState.value.copy(
            selectedTab = SelectedPage.Menu,
            settingsPage = SettingsPage.Main,
        )
    }
}

fun dismissPanelDiscardPopup() {
    _panelSettingsSession.update { it.copy(showDiscardPopup = false) }
}

fun confirmPanelDiscardAndExit() {
    _panelSettingsSession.update { it.copy(showDiscardPopup = false) }
    closePanelSettingsSession()
    uiState.value = uiState.value.copy(
        selectedTab = SelectedPage.Menu,
        settingsPage = SettingsPage.Main,
    )
}

fun closePanelSettingsSession() {
    _panelSettingsSession.value = PanelSettingsSessionState()
    // Keep panelPlatformChannels so home labels/order survive leaving settings.
}

/**
 * BLE bağlandıktan sonra bekleyen panel konfigürasyonunu cihaza yazar.
 */
fun pushPendingPanelConfigOverBle() {
    if (!LoginPreferences.isPanelConfigPendingBlePush()) return
    if (!isBleGattLinkReady()) return
    CoroutineScope(Dispatchers.Default).launch {
        if (!LoginPreferences.isPanelConfigPendingBlePush()) return@launch
        if (!isBleGattLinkReady()) return@launch

        val cached = PanelSettingsPlatformSync.readCachedPanelConfig()
            ?: run {
                LoginPreferences.setPanelConfigPendingBlePush(false)
                return@launch
            }

        PanelSettingsPlatformSync.applyPlatformPanelConfig(cached, "ble-pending-push")
        val prepared = devConfigForBleWrite(uiState.value.devConfig)
        syncVanGoRuntimeFromDevConfig(prepared)

        if (!vanGoGlobal.value.lockOperation(Operations.WriteDataDevice)) {
            println("Panel BLE push skipped: write lock busy")
            return@launch
        }
        var deviceSaved = false
        try {
            // Allow GATT/notifications to settle after connect.
            delay(600L)
            if (!isBleGattLinkReady()) return@launch
            val started = getAppContext().vanGO.writeConfig(prepared)
            if (started) {
                val startTime = getCurrentTimeInSeconds()
                while (true) {
                    when (vanGoGlobal.value.operationStatus) {
                        OperationState.Succeed -> {
                            deviceSaved = true
                            break
                        }
                        OperationState.Failed -> break
                        else -> {
                            if (getCurrentTimeInSeconds() - startTime > 10) break
                            delay(100L)
                        }
                    }
                }
            }
        } finally {
            vanGoGlobal.value.unlockOperation()
        }

        if (deviceSaved) {
            LoginPreferences.setPanelConfigPendingBlePush(false)
            PanelSettingsPlatformSync.persistProfile(devConfigToIoProfileSnapshot(prepared))
            println("Panel config pushed to device over BLE after connect")
        } else {
            println("Panel BLE push failed — will retry on next connect")
        }
    }
}

fun savePanelSettings() {
    if (_panelSettingsSession.value.isSaving || !_panelSettingsSession.value.hasChanges) return
    _panelSettingsSession.update { it.copy(isSaving = true, syncError = null) }

    CoroutineScope(Dispatchers.Default).launch {
        syncPanelChannelsToDevConfig()
        val current = cloneDevConfig(uiState.value.devConfig)
        val prepared = devConfigForBleWrite(current)
        syncVanGoRuntimeFromDevConfig(prepared)

        val profileKey = devicePanelProfile.value.profileKey
        val channels = panelPlatformChannels.value
            ?: defaultPanelPlatformConfig(profileKey).channels
        val local = PanelPlatformConfigRoot(profile = profileKey, channels = channels)
        PanelSettingsPlatformSync.persistPanelConfig(local)
        resetPanelPlatformChannels(channels)

        /* SoT: önce IO flash, sonra platform telemetri ile kendini günceller */
        var deviceSaved = false
        val canWriteDevice = isBleGattLinkReady() || canSendRemoteCommands()
        if (canWriteDevice && vanGoGlobal.value.lockOperation(Operations.WriteDataDevice)) {
            val started = getAppContext().vanGO.writeConfig(prepared)
            if (started) {
                val startTime = getCurrentTimeInSeconds()
                while (true) {
                    when (vanGoGlobal.value.operationStatus) {
                        OperationState.Succeed -> {
                            deviceSaved = true
                            break
                        }
                        OperationState.Failed -> break
                        else -> {
                            if (getCurrentTimeInSeconds() - startTime > 8) break
                            delay(100L)
                        }
                    }
                }
            }
            vanGoGlobal.value.unlockOperation()
            if (deviceSaved) {
                PanelSettingsPlatformSync.persistProfile(devConfigToIoProfileSnapshot(prepared))
            }
        }

        val hasAuth = !LoginPreferences.getAuthToken().isNullOrBlank()
        var platformSaved = false
        if (hasAuth) {
            platformSaved = PanelSettingsPlatformSync.pushPanelConfigToPlatform(prepared)
        }

        // If not written to the device now, queue for automatic BLE push after connect.
        LoginPreferences.setPanelConfigPendingBlePush(!deviceSaved)

        val localOnly = !hasAuth && !canWriteDevice
        val savedMessage = when {
            platformSaved && deviceSaved -> "Panel ayarları platform ve cihaza kaydedildi"
            deviceSaved -> "Panel ayarları cihaza kaydedildi"
            platformSaved -> "Panel ayarları platforma kaydedildi (cihaz bekleniyor)"
            localOnly -> "Panel ayarları kaydedildi (BLE bağlanınca cihaza gönderilecek)"
            else -> "Panel ayarları kaydedildi"
        }

        uiState.value = uiState.value.copy(devConfig = prepared.copy(links = current.links))
        _panelSettingsSession.update {
            it.copy(
                baseline = cloneDevConfig(uiState.value.devConfig),
                hasChanges = false,
                isSaving = false,
                showSavedPopup = true,
                savedMessage = savedMessage,
                syncError = when {
                    canWriteDevice && !deviceSaved -> "Cihaza yazılamadı; BLE sonrası tekrar denenecek"
                    hasAuth && !platformSaved && !localOnly -> "Platforma kaydedilemedi"
                    else -> null
                },
            )
        }
    }
}
