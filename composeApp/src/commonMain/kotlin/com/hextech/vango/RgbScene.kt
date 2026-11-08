package com.hextech.vango

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

const val RGB_SCENE_REST = "rest"
const val RGB_SCENE_CINEMA = "cinema"
const val RGB_SCENE_PARTY = "party"

@Serializable
enum class RgbTransitionEffect {
    Smooth,
    Fade,
    Slide,
    Bounce,
}

@Serializable
data class RgbSceneColorStep(
    val presetSlotIndex: Int,
    val durationSec: Int = 5,
    val brightnessPercent: Int = 100,
)

@Serializable
data class RgbScene(
    val id: String,
    val name: String,
    val groupKey: String,
    val emoji: String,
    val displayNumber: Int,
    val colors: List<RgbSceneColorStep>,
    val transitionEffect: RgbTransitionEffect = RgbTransitionEffect.Smooth,
    val transitionDurationSec: Float = 2f,
    val loopMode: String = "continuous",
)

expect object RgbSceneStore {
    fun getJson(): String?
    fun setJson(json: String)
}

private val rgbSceneJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val scenesMapSerializer = MapSerializer(String.serializer(), RgbScene.serializer())

fun seedDefaultRgbScenes(): Map<String, RgbScene> = mapOf(
    RGB_SCENE_REST to RgbScene(
        id = RGB_SCENE_REST,
        name = "Dinlenme",
        groupKey = RGB_SCENE_REST,
        emoji = "🌙",
        displayNumber = 1,
        colors = listOf(
            RgbSceneColorStep(0, durationSec = 5, brightnessPercent = 100),
            RgbSceneColorStep(5, durationSec = 5, brightnessPercent = 80),
            RgbSceneColorStep(6, durationSec = 5, brightnessPercent = 60),
            RgbSceneColorStep(4, durationSec = 5, brightnessPercent = 70),
        ),
        transitionEffect = RgbTransitionEffect.Smooth,
        transitionDurationSec = 2f,
        loopMode = "continuous",
    ),
    RGB_SCENE_CINEMA to RgbScene(
        id = RGB_SCENE_CINEMA,
        name = "Sinema",
        groupKey = RGB_SCENE_CINEMA,
        emoji = "🎬",
        displayNumber = 2,
        colors = listOf(
            RgbSceneColorStep(1, durationSec = 5, brightnessPercent = 100),
            RgbSceneColorStep(6, durationSec = 5, brightnessPercent = 70),
        ),
        transitionEffect = RgbTransitionEffect.Fade,
        transitionDurationSec = 2f,
        loopMode = "continuous",
    ),
    RGB_SCENE_PARTY to RgbScene(
        id = RGB_SCENE_PARTY,
        name = "Parti",
        groupKey = RGB_SCENE_PARTY,
        emoji = "🪩",
        displayNumber = 3,
        colors = listOf(
            RgbSceneColorStep(11, durationSec = 4, brightnessPercent = 100),
            RgbSceneColorStep(4, durationSec = 4, brightnessPercent = 90),
        ),
        transitionEffect = RgbTransitionEffect.Bounce,
        transitionDurationSec = 1.5f,
        loopMode = "continuous",
    ),
)

fun loadRgbScenes(): Map<String, RgbScene> {
    val raw = RgbSceneStore.getJson() ?: return seedDefaultRgbScenes()
    return try {
        rgbSceneJson.decodeFromString(scenesMapSerializer, raw)
    } catch (_: Exception) {
        seedDefaultRgbScenes()
    }
}

fun ensureRgbScenes(): Map<String, RgbScene> {
    if (RgbSceneStore.getJson() == null) {
        val seeded = seedDefaultRgbScenes()
        saveRgbScenes(seeded)
        return seeded
    }
    return loadRgbScenes()
}

fun saveRgbScenes(scenes: Map<String, RgbScene>) {
    RgbSceneStore.setJson(rgbSceneJson.encodeToString(scenesMapSerializer, scenes))
}

fun saveRgbScene(scene: RgbScene) {
    val map = loadRgbScenes().toMutableMap()
    map[scene.id] = scene
    saveRgbScenes(map)
}

fun resetRgbSceneToDefault(id: String) {
    val defaults = seedDefaultRgbScenes()
    val default = defaults[id] ?: return
    saveRgbScene(default)
}

fun sceneTotalDurationSec(scene: RgbScene): Int =
    scene.colors.sumOf { it.durationSec.coerceAtLeast(1) }
