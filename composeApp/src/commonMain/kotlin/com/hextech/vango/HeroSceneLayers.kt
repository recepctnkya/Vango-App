package com.hextech.vango

import kotlin.concurrent.Volatile

/**
 * Dev-only layer toggles for hero scene previews / debugging.
 * Not exposed in the user-facing UI — change here or from Preview composables.
 */
data class HeroSceneLayerFlags(
    val sky: Boolean = true,
    val celestial: Boolean = true,
    val clouds: Boolean = true,
    val mountains: Boolean = true,
    val lake: Boolean = true,
    val wetShore: Boolean = true,
    val farShore: Boolean = true,
    val treesFar: Boolean = true,
    val treesNear: Boolean = true,
    val park: Boolean = true,
    val vehicle: Boolean = true,
    val waterLife: Boolean = true,
    val phoneLink: Boolean = true,
) {
    companion object {
        val All = HeroSceneLayerFlags()
        val None = HeroSceneLayerFlags(
            sky = false,
            celestial = false,
            clouds = false,
            mountains = false,
            lake = false,
            wetShore = false,
            farShore = false,
            treesFar = false,
            treesNear = false,
            park = false,
            vehicle = false,
            waterLife = false,
            phoneLink = false,
        )
    }
}

/**
 * Mutable debug overrides for Android Studio Previews / local testing.
 * Production card always reads [HeroSceneDebugLayers.flags] which defaults to [All].
 */
object HeroSceneDebugLayers {
    @Volatile
    var flags: HeroSceneLayerFlags = HeroSceneLayerFlags.All

    fun reset() {
        flags = HeroSceneLayerFlags.All
    }
}
