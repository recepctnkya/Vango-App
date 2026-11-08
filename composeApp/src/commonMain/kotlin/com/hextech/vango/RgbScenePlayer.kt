package com.hextech.vango

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

data class RgbScenePlaybackState(
    val sceneId: String? = null,
    val sceneName: String? = null,
    val isPlaying: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentRgb: Triple<Int, Int, Int>? = null,
)

data class ResolvedSceneStep(
    val step: RgbSceneColorStep,
    val rgb: Triple<Int, Int, Int>,
    val isLight: Int,
)

fun applyRgbBrightness(r: Int, g: Int, b: Int, percent: Int): Triple<Int, Int, Int> {
    val f = percent.coerceIn(1, 100) / 100f
    return Triple(
        (r * f).roundToInt().coerceIn(0, 255),
        (g * f).roundToInt().coerceIn(0, 255),
        (b * f).roundToInt().coerceIn(0, 255),
    )
}

fun resolveSceneSteps(scene: RgbScene, slots: Array<RgbPreset?>): List<ResolvedSceneStep> =
    scene.colors.mapNotNull { step ->
        val preset = slots.getOrNull(step.presetSlotIndex) ?: return@mapNotNull null
        val rgb = applyRgbBrightness(preset.r, preset.g, preset.b, step.brightnessPercent)
        ResolvedSceneStep(
            step = step,
            rgb = rgb,
            isLight = if (rgb.first + rgb.second + rgb.third > 8) 1 else 0,
        )
    }

/**
 * Mobile-side RGB scene sequencer: transitions + hold times, then [VanGO.writeRGBPort]
 * (BLE or remote `rgb_set`). IO firmware has no built-in scene player.
 */
object RgbScenePlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null

    private val _state = MutableStateFlow(RgbScenePlaybackState())
    val state: StateFlow<RgbScenePlaybackState> = _state.asStateFlow()

    fun isPlayingScene(sceneId: String): Boolean =
        _state.value.isPlaying && _state.value.sceneId == sceneId

    fun toggleScene(scene: RgbScene, slots: Array<RgbPreset?> = ensureRgbSlotsInitialized()) {
        if (isPlayingScene(scene.id)) {
            stop()
        } else {
            play(scene, slots)
        }
    }

    fun play(sceneId: String, slots: Array<RgbPreset?> = ensureRgbSlotsInitialized()) {
        val scene = loadRgbScenes()[sceneId] ?: return
        play(scene, slots)
    }

    fun play(scene: RgbScene, slots: Array<RgbPreset?> = ensureRgbSlotsInitialized()) {
        if (vanGoData.value.RGBE != "yes") return
        val resolved = resolveSceneSteps(scene, slots)
        if (resolved.isEmpty()) return

        playbackJob?.cancel()
        playbackJob = scope.launch {
            runPlayback(scene, resolved)
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        _state.value = RgbScenePlaybackState()
    }

    private suspend fun runPlayback(scene: RgbScene, resolved: List<ResolvedSceneStep>) {
        _state.value = RgbScenePlaybackState(
            sceneId = scene.id,
            sceneName = scene.name,
            isPlaying = true,
            currentStepIndex = 0,
            currentRgb = resolved.first().rgb,
        )

        var currentRgb = resolved.first().rgb
        var currentLight = resolved.first().isLight
        sendRgb(currentRgb, currentLight)
        holdStep(resolved.first(), scene.id, 0)

        try {
            if (scene.loopMode == "once") {
                playForwardPass(scene, resolved, startIndex = 1, currentRgb, currentLight)
            } else {
                while (currentCoroutineContext().isActive) {
                    currentRgb = playForwardPass(scene, resolved, startIndex = 1, currentRgb, currentLight)
                    val last = resolved.last()
                    currentLight = last.isLight
                    if (scene.loopMode == "pingpong") {
                        currentRgb = playBackwardPass(scene, resolved, currentRgb, currentLight)
                        currentLight = resolved.first().isLight
                    } else {
                        currentRgb = transitionAndHold(scene, currentRgb, currentLight, resolved.first(), 0)
                        currentLight = resolved.first().isLight
                    }
                }
            }
        } finally {
            if (_state.value.sceneId == scene.id) {
                _state.value = RgbScenePlaybackState()
            }
        }
    }

    /** @return RGB after pass */
    private suspend fun playForwardPass(
        scene: RgbScene,
        resolved: List<ResolvedSceneStep>,
        startIndex: Int,
        fromRgb: Triple<Int, Int, Int>,
        fromLight: Int,
    ): Triple<Int, Int, Int> {
        var current = fromRgb
        var light = fromLight
        for (i in startIndex until resolved.size) {
            if (!currentCoroutineContext().isActive) break
            val target = resolved[i]
            current = transitionAndHold(scene, current, light, target, i)
            light = target.isLight
        }
        return current
    }

    private suspend fun playBackwardPass(
        scene: RgbScene,
        resolved: List<ResolvedSceneStep>,
        fromRgb: Triple<Int, Int, Int>,
        fromLight: Int,
    ): Triple<Int, Int, Int> {
        var current = fromRgb
        var light = fromLight
        for (i in resolved.lastIndex - 1 downTo 0) {
            if (!currentCoroutineContext().isActive) break
            val target = resolved[i]
            current = transitionAndHold(scene, current, light, target, i)
            light = target.isLight
        }
        return current
    }

    private suspend fun transitionAndHold(
        scene: RgbScene,
        from: Triple<Int, Int, Int>,
        fromLight: Int,
        target: ResolvedSceneStep,
        stepIndex: Int,
    ): Triple<Int, Int, Int> {
        val transitionMs = (scene.transitionDurationSec.coerceIn(0.1f, 10f) * 1000).toLong()
        if (transitionMs > 0L && (from != target.rgb || fromLight != target.isLight)) {
            animateTransition(
                from = from,
                to = target.rgb,
                fromLight = fromLight,
                toLight = target.isLight,
                effect = scene.transitionEffect,
                durationMs = transitionMs,
                sceneId = scene.id,
                stepIndex = stepIndex,
            )
        } else {
            sendRgb(target.rgb, target.isLight)
            _state.value = _state.value.copy(
                currentStepIndex = stepIndex,
                currentRgb = target.rgb,
            )
        }
        holdStep(target, scene.id, stepIndex)
        return target.rgb
    }

    private suspend fun holdStep(step: ResolvedSceneStep, sceneId: String, stepIndex: Int) {
        val holdMs = step.step.durationSec.coerceIn(1, 120) * 1000L
        val tick = 200L
        var elapsed = 0L
        while (elapsed < holdMs && currentCoroutineContext().isActive && _state.value.sceneId == sceneId) {
            delay(tick)
            elapsed += tick
        }
    }

    private suspend fun animateTransition(
        from: Triple<Int, Int, Int>,
        to: Triple<Int, Int, Int>,
        fromLight: Int,
        toLight: Int,
        effect: RgbTransitionEffect,
        durationMs: Long,
        sceneId: String,
        stepIndex: Int,
    ) {
        val frameMs = 40L
        val frames = (durationMs / frameMs).coerceIn(1, 80).toInt()
        for (frame in 0..frames) {
            if (!currentCoroutineContext().isActive || _state.value.sceneId != sceneId) return
            val t = frame.toFloat() / frames.toFloat()
            val rgb = sampleTransitionRgb(from, to, effect, t)
            val light = if (t >= 0.5f) toLight else fromLight
            sendRgb(rgb, light)
            _state.value = _state.value.copy(currentStepIndex = stepIndex, currentRgb = rgb)
            if (frame < frames) delay(frameMs)
        }
        sendRgb(to, toLight)
        _state.value = _state.value.copy(currentStepIndex = stepIndex, currentRgb = to)
    }

    private fun sampleTransitionRgb(
        from: Triple<Int, Int, Int>,
        to: Triple<Int, Int, Int>,
        effect: RgbTransitionEffect,
        t: Float,
    ): Triple<Int, Int, Int> = when (effect) {
        RgbTransitionEffect.Fade -> {
            if (t < 0.5f) {
                lerpRgb(from, Triple(0, 0, 0), (t * 2f).coerceIn(0f, 1f))
            } else {
                lerpRgb(Triple(0, 0, 0), to, ((t - 0.5f) * 2f).coerceIn(0f, 1f))
            }
        }
        RgbTransitionEffect.Slide -> {
            if (t < 0.92f) from else to
        }
        RgbTransitionEffect.Bounce -> {
            val eased = bounceEaseOut(t)
            lerpRgb(from, to, eased)
        }
        RgbTransitionEffect.Smooth -> lerpRgb(from, to, smoothStep(t))
    }

    private fun lerpRgb(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>, t: Float): Triple<Int, Int, Int> {
        val u = t.coerceIn(0f, 1f)
        return Triple(
            lerpChannel(a.first, b.first, u),
            lerpChannel(a.second, b.second, u),
            lerpChannel(a.third, b.third, u),
        )
    }

    private fun lerpChannel(a: Int, b: Int, t: Float): Int =
        (a + (b - a) * t).roundToInt().coerceIn(0, 255)

    private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

    private fun bounceEaseOut(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return if (x < 0.5f) {
            2f * x * x
        } else {
            val v = 2f * x - 1f
            1f - (1f - v).pow(2) * cos(v * 3.14159265f * 0.5f).coerceIn(-1f, 1f) * 0.15f
        }.coerceIn(0f, 1f)
    }

    private fun sendRgb(rgb: Triple<Int, Int, Int>, isLight: Int) {
        getAppContext().vanGO.writeRGBPort(rgb.first, rgb.second, rgb.third, isLight)
    }
}
