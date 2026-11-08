package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.Curtains
import androidx.compose.material.icons.filled.Desk
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Dry
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FireExtinguisher
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.HeatPump
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Hvac
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.KingBed
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Light
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wash
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.panel_icon_ambiyans_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_ayna_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_banyo_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_dis_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_dolap_alti_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_dolap_ici_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_duvar_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_giris_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_led_serit
import testvango.composeapp.generated.resources.panel_icon_merdiven_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_mutfak_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_okuma_lambasi
import testvango.composeapp.generated.resources.panel_icon_rgb_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_sarkit_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_spot_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_tavan_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_dus
import testvango.composeapp.generated.resources.panel_icon_gri_su_tanki
import testvango.composeapp.generated.resources.panel_icon_musluk
import testvango.composeapp.generated.resources.panel_icon_pis_su_tanki
import testvango.composeapp.generated.resources.panel_icon_sicak_su
import testvango.composeapp.generated.resources.panel_icon_su_cikisi
import testvango.composeapp.generated.resources.panel_icon_su_filtresi
import testvango.composeapp.generated.resources.panel_icon_su_girisi
import testvango.composeapp.generated.resources.panel_icon_su_pompasi
import testvango.composeapp.generated.resources.panel_icon_su_pompasi_elektrik
import testvango.composeapp.generated.resources.panel_icon_su_seviye
import testvango.composeapp.generated.resources.panel_icon_temiz_su_tanki
import testvango.composeapp.generated.resources.panel_icon_tavan_havalandirma_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_tente_alti_aydinlatma
import testvango.composeapp.generated.resources.panel_icon_yatak_basi
import testvango.composeapp.generated.resources.panel_icon_zemin_aydinlatma

enum class PanelIconCategory {
    Lighting, Electric, Water, Living, Climate, Security, Other
}

fun PanelIconCategory.tint(): Color = when (this) {
    PanelIconCategory.Lighting -> Color(0xFFFFB020)
    PanelIconCategory.Electric -> Color(0xFF8BCF4A)
    PanelIconCategory.Water -> Color(0xFF4EA8FF)
    PanelIconCategory.Living -> Color(0xFFB07CFF)
    PanelIconCategory.Climate -> Color(0xFF40D0E0)
    PanelIconCategory.Security -> Color(0xFFFF5A5A)
    PanelIconCategory.Other -> Color(0xFFE8EEF7)
}

data class PanelIconDef(
    val key: String,
    val category: PanelIconCategory,
    val vector: ImageVector,
    val labelTr: String,
    val labelEn: String,
    val drawable: DrawableResource? = null,
    /** False for multi-color artwork (e.g. RGB) so ColorFilter is not applied. */
    val tintDrawable: Boolean = true,
) {
    val tint: Color get() = category.tint()
}

/**
 * Panel icons: lighting set from custom sheet artwork; other categories Material Icons.
 */
object PanelIconCatalog {
    val all: List<PanelIconDef> = listOf(
        // Aydınlatma (sheet artwork)
        light("tavan_aydinlatma", Res.drawable.panel_icon_tavan_aydinlatma, Icons.Filled.WbIncandescent, "Tavan Aydınlatma", "Ceiling Light"),
        light("spot_aydinlatma", Res.drawable.panel_icon_spot_aydinlatma, Icons.Filled.Highlight, "Spot Aydınlatma", "Spotlight"),
        light("led_serit", Res.drawable.panel_icon_led_serit, Icons.Filled.LinearScale, "Led Şerit", "LED Strip"),
        // LED şerit ile aynı aile; R/G/B noktaları PNG'de (tint uygulanmaz)
        light("rgb_aydinlatma", Res.drawable.panel_icon_rgb_aydinlatma, Icons.Filled.LinearScale, "RGB Aydınlatma", "RGB Light", tintDrawable = false),
        light("sarkit_aydinlatma", Res.drawable.panel_icon_sarkit_aydinlatma, Icons.Filled.Light, "Sarkıt Aydınlatma", "Pendant Light"),
        light("duvar_aydinlatma", Res.drawable.panel_icon_duvar_aydinlatma, Icons.Filled.LightMode, "Duvar Aydınlatma", "Wall Light"),
        light("okuma_lambasi", Res.drawable.panel_icon_okuma_lambasi, Icons.Filled.Desk, "Okuma Lambası", "Reading Lamp"),
        light("dolap_alti_aydinlatma", Res.drawable.panel_icon_dolap_alti_aydinlatma, Icons.Filled.Light, "Dolap Altı Aydınlatma", "Under Cabinet Light"),
        light("dolap_ici_aydinlatma", Res.drawable.panel_icon_dolap_ici_aydinlatma, Icons.Filled.Checkroom, "Dolap İçi Aydınlatma", "Closet Light"),
        light("ayna_aydinlatma", Res.drawable.panel_icon_ayna_aydinlatma, Icons.Filled.LightMode, "Ayna Aydınlatma", "Mirror Light"),
        light("dis_aydinlatma", Res.drawable.panel_icon_dis_aydinlatma, Icons.Filled.WbSunny, "Dış Aydınlatma", "Outdoor Light"),
        light("giris_aydinlatma", Res.drawable.panel_icon_giris_aydinlatma, Icons.Filled.Light, "Giriş Aydınlatma", "Entrance Light"),
        light("merdiven_aydinlatma", Res.drawable.panel_icon_merdiven_aydinlatma, Icons.Filled.FlashlightOn, "Merdiven Aydınlatma", "Stair Light"),
        light("zemin_aydinlatma", Res.drawable.panel_icon_zemin_aydinlatma, Icons.Filled.FlashlightOn, "Basamak / Zemin Aydınlatma", "Step / Floor Light"),
        light("ambiyans_aydinlatma", Res.drawable.panel_icon_ambiyans_aydinlatma, Icons.Filled.LinearScale, "Ambiyans Aydınlatma", "Ambient Light"),
        light("yatak_basi", Res.drawable.panel_icon_yatak_basi, Icons.Filled.NightsStay, "Gece / Yatak Başı", "Night / Bedside"),
        light("mutfak_aydinlatma", Res.drawable.panel_icon_mutfak_aydinlatma, Icons.Filled.Light, "Mutfak Tezgah Aydınlatma", "Kitchen Counter Light"),
        light("tavan_havalandirma_aydinlatma", Res.drawable.panel_icon_tavan_havalandirma_aydinlatma, Icons.Filled.Hvac, "Tavan Havalandırma Aydınlatması", "Roof Vent Light"),
        light("banyo_aydinlatma", Res.drawable.panel_icon_banyo_aydinlatma, Icons.Filled.LightMode, "Banyo Aydınlatma", "Bathroom Light"),
        light("tente_alti_aydinlatma", Res.drawable.panel_icon_tente_alti_aydinlatma, Icons.Filled.WbSunny, "Tente Altı Aydınlatma", "Awning Light"),
        // Elektrik
        def("priz_220v", PanelIconCategory.Electric, Icons.Filled.Outlet, "Priz 220V", "Outlet 220V"),
        def("usb_priz", PanelIconCategory.Electric, Icons.Filled.Usb, "USB Priz", "USB Outlet"),
        def("inverter", PanelIconCategory.Electric, Icons.Filled.Bolt, "İnverter", "Inverter"),
        def("jenerator", PanelIconCategory.Electric, Icons.Filled.EvStation, "Jeneratör", "Generator"),
        def("kompresor", PanelIconCategory.Electric, Icons.Filled.Speed, "Kompresör", "Compressor"),
        def("elektrikli_isitici", PanelIconCategory.Electric, Icons.Filled.Fireplace, "Elektrikli Isıtıcı", "Electric Heater"),
        def("fan", PanelIconCategory.Electric, Icons.Filled.Toys, "Fan", "Fan"),
        def("tv", PanelIconCategory.Electric, Icons.Filled.Tv, "TV", "TV"),
        def("mikrodalga", PanelIconCategory.Electric, Icons.Filled.Microwave, "Mikrodalga", "Microwave"),
        def("sac_kurutma", PanelIconCategory.Electric, Icons.Filled.DryCleaning, "Saç Kurutma", "Hair Dryer"),
        // Su (sheet artwork)
        art("temiz_su_tanki", PanelIconCategory.Water, Res.drawable.panel_icon_temiz_su_tanki, Icons.Filled.WaterDrop, "Temiz Su Tankı", "Fresh Water Tank"),
        art("gri_su_tanki", PanelIconCategory.Water, Res.drawable.panel_icon_gri_su_tanki, Icons.Filled.Water, "Gri Su Tankı", "Grey Water Tank"),
        art("pis_su_tanki", PanelIconCategory.Water, Res.drawable.panel_icon_pis_su_tanki, Icons.Filled.WaterDamage, "Pis Su Tankı", "Waste Water Tank"),
        art("su_pompasi", PanelIconCategory.Water, Res.drawable.panel_icon_su_pompasi, Icons.Filled.HeatPump, "Su Pompası", "Water Pump"),
        art("su_pompasi_elektrik", PanelIconCategory.Electric, Res.drawable.panel_icon_su_pompasi_elektrik, Icons.Filled.HeatPump, "Su Pompası", "Water Pump"),
        art("su_girisi", PanelIconCategory.Water, Res.drawable.panel_icon_su_girisi, Icons.Filled.Input, "Su Girişi", "Water Inlet"),
        art("su_cikisi", PanelIconCategory.Water, Res.drawable.panel_icon_su_cikisi, Icons.Filled.Output, "Su Çıkışı", "Water Outlet"),
        art("su_filtresi", PanelIconCategory.Water, Res.drawable.panel_icon_su_filtresi, Icons.Filled.FilterAlt, "Su Filtresi", "Water Filter"),
        art("musluk", PanelIconCategory.Water, Res.drawable.panel_icon_musluk, Icons.Filled.Plumbing, "Musluk", "Faucet"),
        art("dus", PanelIconCategory.Water, Res.drawable.panel_icon_dus, Icons.Filled.Shower, "Duş", "Shower"),
        art("sicak_su", PanelIconCategory.Water, Res.drawable.panel_icon_sicak_su, Icons.Filled.HotTub, "Sıcak Su", "Hot Water"),
        art("su_seviye", PanelIconCategory.Water, Res.drawable.panel_icon_su_seviye, Icons.Filled.Opacity, "Su Seviye", "Water Level"),
        // Yaşam
        def("yatak_odasi", PanelIconCategory.Living, Icons.Filled.KingBed, "Yatak Odası", "Bedroom"),
        def("oturma_alani", PanelIconCategory.Living, Icons.Filled.Weekend, "Oturma Alanı", "Living Area"),
        def("mutfak", PanelIconCategory.Living, Icons.Filled.Countertops, "Mutfak", "Kitchen"),
        def("banyo", PanelIconCategory.Living, Icons.Filled.Bathtub, "Banyo", "Bathroom"),
        def("klozet", PanelIconCategory.Living, Icons.Filled.Wc, "Klozet", "Toilet"),
        def("masa", PanelIconCategory.Living, Icons.Filled.TableRestaurant, "Masa", "Table"),
        def("koltuk", PanelIconCategory.Living, Icons.Filled.Chair, "Koltuk", "Armchair"),
        def("dolap", PanelIconCategory.Living, Icons.Filled.Checkroom, "Dolap", "Closet"),
        def("kapi", PanelIconCategory.Living, Icons.Filled.DoorFront, "Kapı", "Door"),
        def("pencere", PanelIconCategory.Living, Icons.Filled.Window, "Pencere", "Window"),
        def("perde", PanelIconCategory.Living, Icons.Filled.Curtains, "Perde", "Curtain"),
        // İklim
        def("klima", PanelIconCategory.Climate, Icons.Filled.Hvac, "Klima", "AC"),
        def("isitma", PanelIconCategory.Climate, Icons.Filled.Whatshot, "Isıtma", "Heating"),
        def("havalandirma", PanelIconCategory.Climate, Icons.Filled.Air, "Havalandırma", "Ventilation"),
        def("hava_cikisi", PanelIconCategory.Climate, Icons.Filled.Waves, "Hava Çıkışı", "Air Outlet"),
        def("sicaklik", PanelIconCategory.Climate, Icons.Filled.DeviceThermostat, "Sıcaklık", "Temperature"),
        def("sogutma", PanelIconCategory.Climate, Icons.Filled.AcUnit, "Soğutma", "Cooling"),
        def("nem", PanelIconCategory.Climate, Icons.Filled.WaterDrop, "Nem", "Humidity"),
        def("gaz_dedektoru", PanelIconCategory.Climate, Icons.Filled.GasMeter, "Gaz Dedektörü", "Gas Detector"),
        def("duman_dedektoru", PanelIconCategory.Climate, Icons.Filled.Sensors, "Duman Dedektörü", "Smoke Detector"),
        def("co_dedektoru", PanelIconCategory.Climate, Icons.Filled.Co2, "CO Dedektörü", "CO Detector"),
        def("yangin_alarmi", PanelIconCategory.Climate, Icons.Filled.FireExtinguisher, "Yangın Alarmı", "Fire Alarm"),
        // Güvenlik
        def("ac_kapat", PanelIconCategory.Security, Icons.Filled.PowerSettingsNew, "Aç / Kapat", "Power"),
        def("kilit", PanelIconCategory.Security, Icons.Filled.Lock, "Kilit", "Lock"),
        def("alarm", PanelIconCategory.Security, Icons.Filled.NotificationsActive, "Alarm", "Alarm"),
        def("kamera", PanelIconCategory.Security, Icons.Filled.Videocam, "Kamera", "Camera"),
        def("hareket_sensoru", PanelIconCategory.Security, Icons.Filled.DirectionsWalk, "Hareket Sensörü", "Motion Sensor"),
        def("kapi_sensoru", PanelIconCategory.Security, Icons.Filled.SensorDoor, "Kapı Sensörü", "Door Sensor"),
        def("uzaktan_kumanda", PanelIconCategory.Security, Icons.Filled.SettingsRemote, "Uzaktan Kumanda", "Remote"),
        def("bluetooth", PanelIconCategory.Security, Icons.Filled.Bluetooth, "Bluetooth", "Bluetooth"),
        def("wifi", PanelIconCategory.Security, Icons.Filled.Wifi, "Wi-Fi", "Wi-Fi"),
        def("gps", PanelIconCategory.Security, Icons.Filled.LocationOn, "GPS", "GPS"),
        def("acil_durum", PanelIconCategory.Security, Icons.Filled.Sos, "Acil Durum", "Emergency"),
        // Diğer
        def("buzdolabi", PanelIconCategory.Other, Icons.Filled.Kitchen, "Buzdolabı", "Fridge"),
        def("dondurucu", PanelIconCategory.Other, Icons.Filled.AcUnit, "Dondurucu", "Freezer"),
        def("bulasik_makinesi", PanelIconCategory.Other, Icons.Filled.Wash, "Bulaşık Makinesi", "Dishwasher"),
        def("camasir_makinesi", PanelIconCategory.Other, Icons.Filled.LocalLaundryService, "Çamaşır Makinesi", "Washing Machine"),
        def("kurutma_makinesi", PanelIconCategory.Other, Icons.Filled.Dry, "Kurutma Makinesi", "Dryer"),
        def("ocak", PanelIconCategory.Other, Icons.Filled.OutdoorGrill, "Ocak", "Stove"),
        def("firin", PanelIconCategory.Other, Icons.Filled.SoupKitchen, "Fırın", "Oven"),
        def("davlumbaaz", PanelIconCategory.Other, Icons.Filled.Hvac, "Davlumbaz", "Range Hood"),
        def("sarj_cihazi", PanelIconCategory.Other, Icons.Filled.BatteryChargingFull, "Şarj Cihazı", "Charger"),
        def("gunes_paneli", PanelIconCategory.Other, Icons.Filled.SolarPower, "Güneş Paneli", "Solar Panel"),
        def("aku", PanelIconCategory.Other, Icons.Filled.BatteryFull, "Akü", "Battery"),
    )

    private fun light(
        key: String,
        drawable: DrawableResource,
        fallback: ImageVector,
        labelTr: String,
        labelEn: String,
        tintDrawable: Boolean = true,
    ) = PanelIconDef(key, PanelIconCategory.Lighting, fallback, labelTr, labelEn, drawable, tintDrawable)

    private fun art(
        key: String,
        category: PanelIconCategory,
        drawable: DrawableResource,
        fallback: ImageVector,
        labelTr: String,
        labelEn: String,
        tintDrawable: Boolean = true,
    ) = PanelIconDef(key, category, fallback, labelTr, labelEn, drawable, tintDrawable)

    private fun def(
        key: String,
        category: PanelIconCategory,
        vector: ImageVector,
        labelTr: String,
        labelEn: String,
    ) = PanelIconDef(key, category, vector, labelTr, labelEn)

    private val byKey = all.associateBy { it.key }

    fun find(key: String): PanelIconDef? {
        val k = key.trim().lowercase()
        if (k.isEmpty()) return null
        byKey[k]?.let { return it }
        val stripped = k.removePrefix("panel_icon_")
        byKey[stripped]?.let { return it }
        val legacy = when (k) {
            "lightbulb" -> "tavan_aydinlatma"
            "sun" -> "spot_aydinlatma"
            "plug", "bolt" -> "priz_220v"
            "droplet" -> "temiz_su_tanki"
            "pump" -> "su_pompasi"
            "faucet", "shower" -> "dus"
            "toilet" -> "klozet"
            "sink" -> "mutfak"
            "fridge" -> "buzdolabi"
            "camera" -> "kamera"
            "door", "garage", "person-walking" -> "kapi"
            "heater" -> "elektrikli_isitici"
            "ac", "wind" -> "klima"
            "wifi" -> "wifi"
            "palette" -> "led_serit"
            "gauge" -> "sicaklik"
            "circle" -> "tavan_aydinlatma"
            "fan" -> "fan"
            else -> null
        }
        return legacy?.let { byKey[it] }
    }

    fun resolveKeyFromLegacyPreset(presetId: Int): String = when (presetId) {
        1 -> "tavan_aydinlatma"
        2 -> "klozet"
        3 -> "mutfak_aydinlatma"
        4 -> "yatak_basi"
        7 -> "klima"
        8 -> "usb_priz"
        9 -> "buzdolabi"
        10 -> "su_pompasi"
        11 -> "priz_220v"
        12 -> "firin"
        13 -> "tv"
        14 -> "dis_aydinlatma"
        15 -> "priz_220v"
        16 -> "elektrikli_isitici"
        17 -> "spot_aydinlatma"
        18 -> "okuma_lambasi"
        else -> "tavan_aydinlatma"
    }
}

@Composable
fun PanelCatalogIcon(
    def: PanelIconDef,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = def.tint,
) {
    val drawable = def.drawable
    // Asset already has margin; tiny inset avoids clip on clipped parents.
    val box = Modifier
        .then(modifier)
        .size(size)
    if (drawable != null) {
        Image(
            painter = painterResource(drawable),
            contentDescription = def.labelTr,
            contentScale = ContentScale.Fit,
            colorFilter = if (def.tintDrawable) ColorFilter.tint(tint) else null,
            modifier = box,
        )
    } else {
        Icon(
            imageVector = def.vector,
            contentDescription = def.labelTr,
            tint = tint,
            modifier = box.padding(size * 0.02f),
        )
    }
}

@Composable
fun panelIconCategoryTitle(category: PanelIconCategory): String = when (category) {
    PanelIconCategory.Lighting -> "Aydınlatma"
    PanelIconCategory.Electric -> "Elektrik / Cihazlar"
    PanelIconCategory.Water -> "Su Sistemi"
    PanelIconCategory.Living -> "Yaşam Alanı"
    PanelIconCategory.Climate -> "İklimlendirme"
    PanelIconCategory.Security -> "Kontrol / Güvenlik"
    PanelIconCategory.Other -> "Diğer"
}
