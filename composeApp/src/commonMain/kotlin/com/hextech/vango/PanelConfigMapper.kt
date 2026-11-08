package com.hextech.vango

/** Platform panel_config <-> mobil DevConfig (firmware io_profile) eslemesi. */
object PanelConfigMapper {
    private val platformToPreset = mapOf(
        // Legacy keys
        "lightbulb" to 1, "plug" to 11, "sun" to 1, "palette" to 1, "fan" to 1,
        "gauge" to 1, "droplet" to 10, "faucet" to 10, "shower" to 10, "toilet" to 2,
        "sink" to 3, "fridge" to 9, "camera" to 13, "door" to 5, "garage" to 5,
        "pump" to 10, "heater" to 16, "ac" to 7, "tv" to 13, "wifi" to 8, "circle" to 1,
        "bolt" to 8, "wind" to 7, "person-walking" to 5,
        // Catalog keys (closest firmware preset 1..18)
        "tavan_aydinlatma" to 1, "spot_aydinlatma" to 17, "led_serit" to 1,
        "mutfak_aydinlatma" to 3, "banyo_aydinlatma" to 1, "dis_aydinlatma" to 14,
        "okuma_lambasi" to 18, "yatak_basi" to 4, "dolap_ici_aydinlatma" to 1,
        "zemin_aydinlatma" to 1,
        "priz_220v" to 11, "usb_priz" to 8, "inverter" to 8, "jenerator" to 15,
        "kompresor" to 15, "elektrikli_isitici" to 16, "su_pompasi_elektrik" to 10,
        "mikrodalga" to 12, "sac_kurutma" to 15,
        "temiz_su_tanki" to 10, "gri_su_tanki" to 10, "pis_su_tanki" to 10,
        "su_pompasi" to 10, "su_girisi" to 10, "su_cikisi" to 10, "su_filtresi" to 10,
        "musluk" to 3, "dus" to 10, "sicak_su" to 10, "su_seviye" to 10,
        "yatak_odasi" to 4, "oturma_alani" to 5, "mutfak" to 3, "banyo" to 2,
        "klozet" to 2, "masa" to 5, "koltuk" to 5, "dolap" to 5, "kapi" to 5,
        "pencere" to 5, "perde" to 5,
        "klima" to 7, "isitma" to 16, "havalandirma" to 7, "hava_cikisi" to 7,
        "sicaklik" to 7, "sogutma" to 7, "nem" to 10, "gaz_dedektoru" to 5,
        "duman_dedektoru" to 5, "co_dedektoru" to 5, "yangin_alarmi" to 5,
        "ac_kapat" to 1, "kilit" to 5, "alarm" to 5, "kamera" to 13,
        "hareket_sensoru" to 5, "kapi_sensoru" to 5, "uzaktan_kumanda" to 8,
        "bluetooth" to 8, "gps" to 8, "acil_durum" to 5,
        "buzdolabi" to 9, "dondurucu" to 9, "bulasik_makinesi" to 12,
        "camasir_makinesi" to 12, "kurutma_makinesi" to 12, "ocak" to 12,
        "firin" to 12, "davlumbaaz" to 12, "sarj_cihazi" to 8,
        "gunes_paneli" to 8, "aku" to 8,
    )

    private val presetToPlatformIcon = mapOf(
        1 to "tavan_aydinlatma", 2 to "klozet", 3 to "mutfak_aydinlatma", 4 to "yatak_basi",
        5 to "kapi", 6 to "tavan_aydinlatma", 7 to "klima", 8 to "usb_priz",
        9 to "buzdolabi", 10 to "su_pompasi", 11 to "priz_220v", 12 to "firin",
        13 to "tv", 14 to "dis_aydinlatma", 15 to "priz_220v", 16 to "elektrikli_isitici",
        17 to "spot_aydinlatma", 18 to "okuma_lambasi",
    )

    /** Web `HexnetPanelConfig.normalizeConfig` ile ayni birlestirme. */
    fun normalizePlatformConfig(
        profileKey: String,
        raw: PanelPlatformConfigRoot?,
    ): PanelPlatformConfigRoot {
        val key = profileKey.takeIf { PanelProfileCaps.containsKey(it) } ?: "vango_medium"
        val cap = PanelProfileCaps.getValue(key)
        val out = defaultPanelPlatformConfig(key)
        val source = raw ?: return out
        val profile = source.profile.takeIf { PanelProfileCaps.containsKey(it) } ?: key
        val merged = defaultPanelPlatformConfig(profile)
        val ch = source.channels
        fun mergeType(
            type: String,
            max: Int,
            saved: List<PanelPlatformChannel>,
            defaults: List<PanelPlatformChannel>,
        ): List<PanelPlatformChannel> {
            val byId = saved.associateBy { it.id }.toMutableMap()
            return (0 until max).map { id ->
                val def = defaults.getOrElse(id) {
                    PanelPlatformChannel(id = id, name = "$type ${id + 1}", icon = defaultPlatformIcon(type, id))
                }
                byId[id]?.let { row ->
                    def.copy(
                        id = id,
                        enabled = row.enabled,
                        name = row.name.trim().ifEmpty { def.name },
                        icon = row.icon.trim().ifEmpty { def.icon },
                        startup = row.startup,
                        delaySec = row.delaySec.coerceIn(0, 3600),
                        sortOrder = row.sortOrder,
                        capacityLiters = row.capacityLiters.takeIf { it > 0 } ?: def.capacityLiters,
                        showOnHome = row.showOnHome,
                    )
                } ?: def
            }.sortedBy { it.sortOrder }
        }
        fun mergeSensorType(
            max: Int,
            saved: List<PanelPlatformChannel>,
            defaults: List<PanelPlatformChannel>,
        ): List<PanelPlatformChannel> {
            val byId = saved.associateBy { it.id }
            return (0 until max).map { id ->
                val def = defaults.getOrElse(id) {
                    defaultPlatformSensors(max).getOrElse(id) {
                        PanelPlatformChannel(id = id, name = "Sensor ${id + 1}", icon = "gauge")
                    }
                }
                byId[id]?.let { row ->
                    val sensorType = normalizePanelSensorType(row.sensorType.ifBlank { def.sensorType })
                    def.copy(
                        id = id,
                        enabled = row.enabled,
                        name = row.name.trim().ifEmpty { sensorDefaultName(sensorType, id) },
                        icon = row.icon.trim().ifEmpty { sensorDefaultIcon(sensorType) },
                        sensorType = sensorType,
                        startup = row.startup,
                        delaySec = row.delaySec.coerceIn(0, 3600),
                        sortOrder = row.sortOrder,
                    )
                } ?: def
            }.sortedBy { it.sortOrder }
        }
        return merged.copy(
            version = source.version,
            profile = profile,
            channels = merged.channels.copy(
                relays = mergeType("relay", cap.relays, ch.relays, merged.channels.relays),
                dimmers = mergeType("dimmer", cap.dimmers, ch.dimmers, merged.channels.dimmers),
                rgb = mergeType("rgb", cap.rgb, ch.rgb, merged.channels.rgb),
                motors = mergeType("motor", cap.motors, ch.motors, merged.channels.motors),
                sensors = mergeSensorType(cap.sensors, ch.sensors, merged.channels.sensors),
                water = mergeType("water", cap.water, ch.water, merged.channels.water),
            ),
        )
    }

    fun applyPlatformConfigToDevConfig(platform: PanelPlatformConfigRoot, current: DevConfig): DevConfig {
        val cap = panelProfileCap(platform.profile)
        val relaySlots = cap.relays.coerceIn(1, FIRMWARE_RELAY_BUFFER_MAX)
        val dimSlots = cap.dimmers.coerceIn(1, FIRMWARE_DIMMER_MAX)
        val ch = platform.channels
        val outBuf = ByteArray(relaySlots) { 0 }
        var relayMask = 0
        ch.relays.forEach { row ->
            if (row.id in 0 until relaySlots && row.enabled) {
                /* type 0 FW'de "pasif" sanılıyordu — enabled kanalda en az 1 */
                outBuf[row.id] = iconToPreset(row.icon).coerceAtLeast(1).toByte()
                relayMask = relayMask or (1 shl row.id)
            }
        }
        val dimBuf = ByteArray(dimSlots) { 0 }
        var dimMask = 0
        ch.dimmers.forEach { row ->
            if (row.id in 0 until dimSlots && row.enabled) {
                dimBuf[row.id] = iconToPreset(row.icon).toByte()
                dimMask = dimMask or (1 shl row.id)
            }
        }
        val senBuf = ByteArray(5) { 0 }
        var sensorMask = 0
        ch.sensors.forEach { row ->
            if (!row.enabled) return@forEach
            val type = normalizePanelSensorType(row.sensorType)
            val preset = iconToPreset(row.icon).toByte()
            when {
                row.id == 0 && type == "temp_humidity" -> {
                    senBuf[0] = preset
                    senBuf[1] = preset
                    sensorMask = sensorMask or 0b11
                }
                row.id in 0..1 -> {
                    senBuf[row.id] = preset
                    sensorMask = sensorMask or (1 shl row.id)
                }
            }
        }
        ch.water.forEach { row ->
            val idx = row.id + 2
            if (idx in 2..4 && row.enabled) {
                senBuf[idx] = iconToPreset(row.icon).toByte()
                sensorMask = sensorMask or (1 shl idx)
            }
        }
        val rgbOn = ch.rgb.any { it.enabled }
        val motorOn = ch.motors.any { it.enabled }
        val tankMask = ch.water.foldIndexed(0) { i, acc, row ->
            if (row.enabled) acc or (1 shl i) else acc
        }
        return ensureDevConfigSizedForProfile(
            current.copy(
                /* Slot sayısı = kapasite; countEnabledSlots delikli maskede ilk kanalları düşürüyordu */
                numOfOutputs = if (relayMask != 0) relaySlots else countEnabledSlots(outBuf).coerceIn(0, relaySlots),
                OutputsNameBuffer = outBuf,
                relayEnableMask = if (relayMask != 0) relayMask else ((1 shl relaySlots) - 1),
                numOfDims = if (dimMask != 0) dimSlots else countEnabledSlots(dimBuf).coerceIn(0, dimSlots),
                DimsNameBuffer = dimBuf,
                dimEnableMask = dimMask,
                numOfSensors = countEnabledSlots(senBuf).coerceIn(0, FIRMWARE_SENSOR_BUFFER_MAX),
                SensorsNameBuffer = senBuf,
                sensorEnableMask = sensorMask,
                RGBEnables = if (rgbOn) "yes" else "no",
                motorEnabled = motorOn,
                numOfTanks = ch.water.count { it.enabled }.coerceIn(0, cap.water),
                tankEnableMask = tankMask,
            ),
            cap,
        )
    }

    fun devConfigToPlatformConfig(
        dev: DevConfig,
        profile: String,
        existing: PanelPlatformConfigRoot?,
        channelsOverlay: PanelPlatformChannels? = null,
    ): PanelPlatformConfigRoot {
        val base = existing ?: defaultPanelPlatformConfig(profile)
        val channelBase = channelsOverlay ?: base.channels
        val cap = PanelProfileCaps[base.profile] ?: PanelProfileCaps.getValue("vango_medium")
        fun mergeChannels(
            type: String,
            max: Int,
            saved: List<PanelPlatformChannel>,
            slotReader: (Int) -> Byte,
        ): List<PanelPlatformChannel> {
            val byId = saved.associateBy { it.id }.toMutableMap()
            return (0 until max).map { id ->
                val def = byId[id] ?: PanelPlatformChannel(
                    id = id,
                    enabled = false,
                    name = "$type ${id + 1}",
                    icon = defaultPlatformIcon(type, id),
                    sortOrder = id,
                )
                val slot = slotReader(id)
                val enabled = slot != 0.toByte()
                def.copy(
                    id = id,
                    enabled = enabled,
                    icon = if (enabled) presetToIconKey(slot.toInt() and 0xFF) else def.icon,
                )
            }
        }
        return base.copy(
            channels = channelBase.copy(
                relays = mergeChannels("relay", cap.relays, channelBase.relays) { id ->
                    dev.OutputsNameBuffer.getOrElse(id) { 0 }
                },
                dimmers = mergeChannels("dimmer", cap.dimmers, channelBase.dimmers) { id ->
                    dev.DimsNameBuffer.getOrElse(id) { 0 }
                },
                rgb = when {
                    channelBase.rgb.isNotEmpty() -> channelBase.rgb.map { row ->
                        row.copy(enabled = if (channelBase.rgb.size == 1) {
                            dev.RGBEnables.equals("yes", ignoreCase = true)
                        } else {
                            row.enabled
                        })
                    }
                    dev.RGBEnables.equals("yes", ignoreCase = true) -> listOf(
                        PanelPlatformChannel(id = 0, enabled = true, name = "RGB Aydınlatma", icon = "rgb_aydinlatma"),
                    )
                    else -> emptyList()
                },
                motors = when {
                    channelBase.motors.isNotEmpty() -> channelBase.motors
                    dev.motorEnabled -> (0 until cap.motors).map { id ->
                        PanelPlatformChannel(
                            id = id,
                            enabled = id == 0,
                            name = "Motor ${id + 1}",
                            icon = "fan",
                        )
                    }
                    else -> emptyList()
                },
                sensors = mergePlatformSensorChannels(
                    max = cap.sensors,
                    saved = channelBase.sensors,
                    dev = dev,
                ),
                water = sanitizeWaterChannels(
                    (0 until cap.water).map { wi ->
                        val prev = channelBase.water.getOrElse(wi) {
                            defaultWaterChannels(cap.water).getOrElse(wi) {
                                PanelPlatformChannel(id = wi, enabled = wi == 0, name = "Su ${wi + 1}", icon = "droplet")
                            }
                        }
                        prev.copy(id = wi)
                    },
                    count = cap.water,
                ),
            ),
        )
    }

    private fun mergePlatformSensorChannels(
        max: Int,
        saved: List<PanelPlatformChannel>,
        dev: DevConfig,
    ): List<PanelPlatformChannel> {
        val defaults = defaultPlatformSensors(max)
        return (0 until max).map { id ->
            val base = saved.getOrElse(id) { defaults.getOrElse(id) { PanelPlatformChannel(id = id) } }
            val sensorType = normalizePanelSensorType(base.sensorType)
            val enabled = isPlatformSensorEnabled(base, dev.SensorsNameBuffer)
            val slot = when {
                id == 0 && sensorType == "temp_humidity" -> dev.SensorsNameBuffer.getOrElse(0) { 0 }
                id <= 1 -> dev.SensorsNameBuffer.getOrElse(id) { 0 }
                else -> 0
            }
            base.copy(
                id = id,
                enabled = enabled,
                sensorType = sensorType,
                name = base.name.ifBlank { sensorDefaultName(sensorType, id) },
                icon = if (enabled && slot != 0.toByte()) {
                    presetToIconKey(slot.toInt() and 0xFF)
                } else {
                    base.icon.ifBlank { sensorDefaultIcon(sensorType) }
                },
            )
        }
    }

    fun iconToPreset(icon: String): Int =
        platformToPreset[icon.trim().lowercase()] ?: 1

    fun presetToIconKey(preset: Int): String =
        presetToPlatformIcon[preset] ?: "tavan_aydinlatma"

    private fun defaultPlatformIcon(type: String, index: Int): String = when (type) {
        "relay" -> if (index % 2 == 0) "tavan_aydinlatma" else "priz_220v"
        "dimmer" -> "spot_aydinlatma"
        "rgb" -> "led_serit"
        "motor" -> "fan"
        "sensor" -> "sicaklik"
        "water" -> defaultWaterIcon(0)
        else -> "tavan_aydinlatma"
    }
}