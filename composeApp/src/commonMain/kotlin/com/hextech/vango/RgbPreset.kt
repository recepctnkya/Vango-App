package com.hextech.vango

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

const val RGB_SLOT_COUNT = 12

@Serializable
data class RgbPreset(
    val name: String,
    val r: Int,
    val g: Int,
    val b: Int,
    val favorite: Boolean = false,
) {
    fun hex(): String {
        val rr = r.coerceIn(0, 255)
        val gg = g.coerceIn(0, 255)
        val bb = b.coerceIn(0, 255)
        fun h(n: Int) = n.toString(16).uppercase().padStart(2, '0')
        return "#${h(rr)}${h(gg)}${h(bb)}"
    }
}

expect object RgbPresetStore {
    fun getJson(): String?
    fun setJson(json: String)
}

private val rgbPresetJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val rgbSlotsSerializer = ListSerializer(RgbPreset.serializer().nullable)

fun seedDefaultRgbSlots(): Array<RgbPreset?> = arrayOf(
    RgbPreset("Mor Rüya", 128, 0, 255, favorite = true),
    RgbPreset("Okyanus", 0, 123, 255),
    RgbPreset("Doğa Yeşili", 34, 197, 94),
    RgbPreset("Gün Batımı", 255, 140, 0, favorite = true),
    RgbPreset("Pembe Şeker", 255, 105, 180),
    RgbPreset("Buz Mavisi", 0, 206, 209),
    RgbPreset("Sıcak Beyaz", 255, 244, 200, favorite = true),
    RgbPreset("Soğuk Beyaz", 240, 248, 255),
    RgbPreset("Ateş Kırmızısı", 220, 38, 38),
    RgbPreset("Gece Moru", 88, 28, 135),
    RgbPreset("Nane Yeşili", 110, 231, 183),
    RgbPreset("Parti Modu", 236, 72, 153, favorite = true),
)

fun loadRgbSlots(): Array<RgbPreset?> {
    val raw = RgbPresetStore.getJson() ?: return seedDefaultRgbSlots()
    return try {
        val list = rgbPresetJson.decodeFromString(rgbSlotsSerializer, raw)
        Array(RGB_SLOT_COUNT) { i -> list.getOrNull(i) }
    } catch (_: Exception) {
        seedDefaultRgbSlots()
    }
}

fun saveRgbSlots(slots: Array<RgbPreset?>) {
    val list = slots.toList()
    RgbPresetStore.setJson(rgbPresetJson.encodeToString(rgbSlotsSerializer, list))
}

fun ensureRgbSlotsInitialized(): Array<RgbPreset?> {
    if (RgbPresetStore.getJson() == null) {
        val seeded = seedDefaultRgbSlots()
        saveRgbSlots(seeded)
        return seeded
    }
    return loadRgbSlots()
}
