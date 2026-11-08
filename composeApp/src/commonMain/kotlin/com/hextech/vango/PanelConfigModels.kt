package com.hextech.vango

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** myhexnet `panel-config-get/save.php` — web Panel Ayarlari ile ayni sema. */
@Serializable
data class PanelPlatformConfigRoot(
    val version: Int = 1,
    val profile: String = "vango_medium",
    val channels: PanelPlatformChannels = PanelPlatformChannels(),
)

@Serializable
data class PanelPlatformChannels(
    val relays: List<PanelPlatformChannel> = emptyList(),
    val dimmers: List<PanelPlatformChannel> = emptyList(),
    val rgb: List<PanelPlatformChannel> = emptyList(),
    val motors: List<PanelPlatformChannel> = emptyList(),
    val sensors: List<PanelPlatformChannel> = emptyList(),
    val water: List<PanelPlatformChannel> = emptyList(),
)

@Serializable
data class PanelPlatformChannel(
    val id: Int = 0,
    val enabled: Boolean = true,
    val name: String = "",
    val icon: String = "circle",
    @SerialName("sensor_type") val sensorType: String = "temp_humidity",
    val startup: String = "last",
    @SerialName("delay_sec") val delaySec: Int = 0,
    @SerialName("sort_order") val sortOrder: Int = 0,
    /** Su tankı toplam hacmi (litre). 0 = tanımsız. */
    @SerialName("capacity_l") val capacityLiters: Int = 0,
    /** Aktif tankın ana sayfada kart olarak görünmesi. Kapalı tank zaten ana sayfada çıkmaz. */
    @SerialName("show_on_home") val showOnHome: Boolean = true,
)

val PanelProfileCaps = mapOf(
    "vango_medium" to PanelProfileCap(relays = 12, dimmers = 8, rgb = 1, motors = 2, sensors = 4, water = 3),
    "vango_mini" to PanelProfileCap(relays = 4, dimmers = 2, rgb = 1, motors = 1, sensors = 3, water = 2),
)

data class PanelProfileCap(
    val relays: Int,
    val dimmers: Int,
    val rgb: Int,
    val motors: Int,
    val sensors: Int,
    val water: Int,
)

internal fun guessPanelProfileFromUid(uid: String?): String {
    guessPanelProfileFromMetadata(uid, null, null)?.let { return it }
    return "vango_medium"
}

fun defaultWaterIcon(id: Int): String = when (id) {
    0 -> "temiz_su_tanki"
    1 -> "gri_su_tanki"
    2 -> "pis_su_tanki"
    else -> "su_seviye"
}

fun defaultWaterChannels(count: Int): List<PanelPlatformChannel> {
    val names = listOf("Temiz Su", "Gri Su", "Kirli Su", "Yakıt")
    val caps = listOf(120, 100, 100, 80)
    return (0 until count).map { i ->
        PanelPlatformChannel(
            id = i,
            enabled = i == 0,
            name = names.getOrElse(i) { "Su ${i + 1}" },
            icon = defaultWaterIcon(i),
            sortOrder = i,
            capacityLiters = caps.getOrElse(i) { 100 },
        )
    }
}

/** Takılı olmayan gri/kirli tank ADC gürültüsü veya sahte % göstermesin. */
fun isWaterTankInstalled(id: Int, channels: PanelPlatformChannels?): Boolean {
    val water = channels?.water.orEmpty()
    if (water.isEmpty()) return id == 0
    return water.firstOrNull { it.id == id }?.enabled ?: (id == 0)
}

/** Ana sayfa su kartı: tank hem takılı hem ana sayfada göster olarak işaretli olmalı. */
fun isWaterTankShownOnHome(id: Int, channels: PanelPlatformChannels?): Boolean {
    if (!isWaterTankInstalled(id, channels)) return false
    val water = channels?.water.orEmpty()
    if (water.isEmpty()) return id == 0
    return water.firstOrNull { it.id == id }?.showOnHome ?: true
}

fun waterTankCapacityLiters(id: Int, channels: PanelPlatformChannels?): Int {
    val fromChannel = channels?.water?.firstOrNull { it.id == id }?.capacityLiters ?: 0
    if (fromChannel > 0) return fromChannel
    return defaultWaterChannels(3).getOrNull(id)?.capacityLiters ?: 0
}

fun waterRemainingLiters(percent: Int, capacityLiters: Int): Int {
    if (capacityLiters <= 0) return 0
    return ((percent.coerceIn(0, 100) / 100.0) * capacityLiters).toInt()
}

fun sanitizeWaterChannels(water: List<PanelPlatformChannel>, count: Int = 3): List<PanelPlatformChannel> {
    val base = if (water.isEmpty()) defaultWaterChannels(count) else water
    return (0 until count).map { i ->
        val ch = base.firstOrNull { it.id == i } ?: defaultWaterChannels(count)[i]
        val key = ch.icon.trim().lowercase()
        val needsTankLogo = key.isEmpty() ||
            key == "su_seviye" ||
            key == "circle" ||
            key == "droplet" ||
            key == "lightbulb" ||
            key == "sun" ||
            PanelIconCatalog.find(key)?.category != PanelIconCategory.Water
        ch.copy(
            id = i,
            icon = if (needsTankLogo) defaultWaterIcon(i) else key,
        )
    }
}

internal fun defaultPanelPlatformConfig(profile: String): PanelPlatformConfigRoot {
    val key = if (PanelProfileCaps.containsKey(profile)) profile else "vango_medium"
    val cap = PanelProfileCaps.getValue(key)
    val relayNames = listOf(
        "Tavan Aydınlatma",
        "Led Şerit",
        "Spot Aydınlatma",
        "Mutfak Aydınlatma",
        "Dolap İçi Aydınlatma",
        "Yatak Başı",
        "Banyo Aydınlatma",
        "Su Pompası",
        "Isıtıcı",
        "Fan",
        "İnverter",
        "Buzdolabı",
    )
    fun mk(type: String, count: Int, names: List<String>, icons: List<String>? = null): List<PanelPlatformChannel> =
        (0 until count).map { i ->
            PanelPlatformChannel(
                id = i,
                enabled = true,
                name = names.getOrElse(i) { "$type ${i + 1}" },
                icon = icons?.getOrElse(i) { defaultPlatformIcon(type, i) } ?: defaultPlatformIcon(type, i),
                sortOrder = i,
            )
        }
    val relayIcons = listOf(
        "tavan_aydinlatma", "led_serit", "spot_aydinlatma", "mutfak_aydinlatma",
        "dolap_ici_aydinlatma", "yatak_basi", "banyo_aydinlatma", "su_pompasi",
        "elektrikli_isitici", "fan", "inverter", "buzdolabi",
    )
    val dimNames = (1..cap.dimmers).map { "Dimmer $it" }
    return PanelPlatformConfigRoot(
        version = 1,
        profile = key,
        channels = PanelPlatformChannels(
            relays = mk("relay", cap.relays, relayNames, relayIcons),
            dimmers = mk("dimmer", cap.dimmers, dimNames),
            rgb = mk("rgb", cap.rgb, listOf("RGB Aydinlatma")),
            motors = mk("motor", cap.motors, (1..cap.motors).map { "Motor $it" }),
            sensors = defaultPlatformSensors(cap.sensors),
            water = defaultWaterChannels(cap.water),
        ),
    )
}

private fun defaultPlatformIcon(type: String, index: Int): String = when (type) {
    "relay" -> if (index % 2 == 0) "tavan_aydinlatma" else "priz_220v"
    "dimmer" -> "spot_aydinlatma"
    "rgb" -> "led_serit"
    "motor" -> "fan"
    "sensor" -> "sicaklik"
    "water" -> defaultWaterIcon(index)
    else -> "tavan_aydinlatma"
}