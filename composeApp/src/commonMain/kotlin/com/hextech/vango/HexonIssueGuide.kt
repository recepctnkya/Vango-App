package com.hextech.vango

/**
 * Guided troubleshooting for VanGO issues.
 *
 * Rules:
 * - Only cite app-visible facts (connection, motor enable / discovered motors, etc.).
 * - Never invent fault codes, locks, or sensor readings that are not in [HexonLiveContext].
 * - Ask one step at a time; advance on user confirmation — do not restart the flow.
 * - Never drive relays/motors from chat; keep Motor-screen approval / safety.
 */

enum class HexonIssueTopic {
    Motor,
    Lighting,
    WaterLevel,
    BleConnection,
}

data class HexonIssueObservation(
    /** Stable key, e.g. "link", "motor_enabled". */
    val key: String,
    /** Short fact for Hexon to mention — must come from real app state. */
    val summary: String,
)

enum class HexonIssueStepKind {
    /** Ask the user one question / check. */
    Ask,
    /** End with authorized-service guidance (no device command). */
    Service,
    /** Close the guide politely. */
    Close,
}

data class HexonIssueStep(
    val id: String,
    val kind: HexonIssueStepKind = HexonIssueStepKind.Ask,
    val buildPrompt: (HexonLiveContext, List<HexonIssueObservation>) -> String,
)

data class HexonIssueFlow(
    val topic: HexonIssueTopic,
    /** Normalized substrings that start this flow. */
    val startTriggers: List<String>,
    val observe: (HexonLiveContext) -> List<HexonIssueObservation>,
    val steps: List<HexonIssueStep>,
    val navigateTo: HexonDestination? = null,
    val ctaLabel: String? = null,
)

data class HexonIssueSession(
    val topic: HexonIssueTopic,
    val stepIndex: Int,
    val observations: List<HexonIssueObservation>,
)

object HexonIssueGuide {
    var session: HexonIssueSession? = null
        private set

    fun clear() {
        session = null
    }

    /**
     * Handle an active session or start a new guided flow.
     * Returns null if this message is not troubleshooting.
     */
    fun tryHandle(rawQuery: String, ctx: HexonLiveContext): HexonReply? {
        val q = normalizeIssue(rawQuery)
        if (q.isBlank()) return null

        val active = session
        if (active != null) {
            when {
                isCancel(q) -> {
                    clear()
                    return HexonReply("Tamam, bu sorun giderme adımlarını durdurdum. İstersen başka bir konuya geçebiliriz.")
                }
                looksLikeContinuation(q) || stillAboutTopic(q, active.topic) -> {
                    return advance(active, q, ctx)
                }
                startTopicFor(q) != null && startTopicFor(q) != active.topic -> {
                    // Switch to a different issue topic.
                    return start(startTopicFor(q)!!, ctx)
                }
                startTopicFor(q) == active.topic -> {
                    // Same problem restated — continue, do not reset to step 0.
                    return advance(active, q, ctx)
                }
                else -> {
                    // Unrelated smalltalk / other VanGO Q — drop guide and let caller continue.
                    clear()
                    return null
                }
            }
        }

        val topic = startTopicFor(q) ?: return null
        return start(topic, ctx)
    }

    private fun start(topic: HexonIssueTopic, ctx: HexonLiveContext): HexonReply {
        val flow = flowFor(topic) ?: return HexonReply(
            text = "Bu konu için henüz adım adım rehber yok; ilgili VanGO ekranından kontrol edebilirsin.",
        )
        if (flow.steps.isEmpty()) {
            return HexonReply(
                text = "Bu sorun tipi için rehber hazırlanıyor. Şimdilik ilgili ekrandan durumu kontrol edebilirsin.",
                navigateTo = flow.navigateTo,
                ctaLabel = flow.ctaLabel,
            )
        }
        val observations = flow.observe(ctx)
        session = HexonIssueSession(topic = topic, stepIndex = 0, observations = observations)
        return replyForStep(flow, 0, ctx, observations, preface = null)
    }

    private fun advance(
        active: HexonIssueSession,
        q: String,
        ctx: HexonLiveContext,
    ): HexonReply {
        val flow = flowFor(active.topic) ?: run {
            clear()
            return HexonReply("Bu rehberi kapattım.")
        }
        // Cable / hardware intervention → service (do not rewind).
        if (impliesCableOrHardware(q) && active.stepIndex >= 1) {
            val serviceIdx = flow.steps.indexOfFirst { it.kind == HexonIssueStepKind.Service }
                .takeIf { it >= 0 } ?: (flow.steps.lastIndex)
            session = active.copy(stepIndex = serviceIdx)
            return replyForStep(flow, serviceIdx, ctx, active.observations, preface = null)
        }

        val next = (active.stepIndex + 1).coerceAtMost(flow.steps.lastIndex)
        // Already on last step and still stuck → service / close without looping to 0.
        if (active.stepIndex >= flow.steps.lastIndex) {
            val serviceIdx = flow.steps.indexOfFirst { it.kind == HexonIssueStepKind.Service }
            if (serviceIdx >= 0 && active.stepIndex != serviceIdx) {
                session = active.copy(stepIndex = serviceIdx)
                return replyForStep(flow, serviceIdx, ctx, active.observations, preface = null)
            }
            clear()
            return HexonReply(
                text = "Elimdeki uygulama bilgileriyle kesin nedeni söyleyemiyorum. " +
                    "Yetkili servise görünen durumu iletmen en güvenlisi; ben buradan motor veya röle çalıştırmıyorum.",
                navigateTo = flow.navigateTo,
                ctaLabel = flow.ctaLabel,
            )
        }

        session = active.copy(stepIndex = next)
        val preface = when {
            stillBroken(q) -> "Kontrol ettiğini not ettim; sorunun sürdüğünü anladım. "
            else -> null
        }
        return replyForStep(flow, next, ctx, active.observations, preface = preface)
    }

    private fun replyForStep(
        flow: HexonIssueFlow,
        index: Int,
        ctx: HexonLiveContext,
        observations: List<HexonIssueObservation>,
        preface: String?,
    ): HexonReply {
        val step = flow.steps[index]
        val body = buildString {
            if (index == 0 && observations.isNotEmpty()) {
                append(observations.joinToString(" ") { it.summary })
                append(' ')
            }
            if (!preface.isNullOrBlank()) append(preface)
            append(step.buildPrompt(ctx, observations))
        }
        val showCta = step.kind != HexonIssueStepKind.Service || flow.navigateTo != null
        return HexonReply(
            text = body.trim(),
            navigateTo = if (showCta) flow.navigateTo else null,
            ctaLabel = if (showCta) flow.ctaLabel else null,
        )
    }
}

// —— Flow registry (extensible) ——

internal fun flowFor(topic: HexonIssueTopic): HexonIssueFlow? =
    HexonIssueFlows.firstOrNull { it.topic == topic }

private fun startTopicFor(q: String): HexonIssueTopic? {
    for (flow in HexonIssueFlows) {
        if (flow.steps.isEmpty()) continue
        if (flow.startTriggers.any { q.contains(it) }) return flow.topic
    }
    return null
}

val HexonIssueFlows: List<HexonIssueFlow> = listOf(
    motorIssueFlow(),
    // Skeletons for later expansion — triggers reserved, steps filled over time.
    lightingIssueFlow(),
    waterLevelIssueFlow(),
    bleIssueFlow(),
)

private fun motorIssueFlow(): HexonIssueFlow = HexonIssueFlow(
    topic = HexonIssueTopic.Motor,
    startTriggers = listOf(
        "motor calismiyor",
        "motor neden",
        "motor hareket etmiyor",
        "tente hareket etmiyor",
        "tente acilmiyor",
        "tente kapanmiyor",
        "panjur hareket etmiyor",
        "yatak kalkmiyor",
        "motor tepki vermiyor",
        "motor bozuk",
    ),
    observe = { ctx -> motorObservations(ctx) },
    navigateTo = HexonDestination.Motor,
    ctaLabel = "Motor’a git",
    steps = listOf(
        HexonIssueStep("ask_app_warning") { _, _ ->
            "Motorun çalışmadığını anladım. Uygulamada bir hata veya kilit uyarısı görünüyor mu?"
        },
        HexonIssueStep("ask_power") { _, _ ->
            "Motorun enerji beslemesini kontrol edebilir misin?"
        },
        HexonIssueStep("ask_cables") { _, _ ->
            "Motor bağlantısında gevşeklik veya ayrılmış kablo olup olmadığını, enerji kapalıyken kontrol ettir."
        },
        HexonIssueStep("ask_status_report") { _, _ ->
            "Bunlar normalse görünen hata mesajını veya bağlantı durumunu bana yaz; sonraki adımı buna göre belirleyelim."
        },
        HexonIssueStep(
            id = "service",
            kind = HexonIssueStepKind.Service,
        ) { _, _ ->
            "Kablo, besleme veya donanım müdahalesi gerekiyorsa lütfen yetkili servise başvur. " +
                "Güvenlik için motoru sohbetten çalıştırmıyorum; komutları yalnızca Motor ekranındaki onay akışıyla verebilirsin."
        },
    ),
)

private fun lightingIssueFlow(): HexonIssueFlow = HexonIssueFlow(
    topic = HexonIssueTopic.Lighting,
    startTriggers = listOf(
        "isik yanmiyor",
        "lamba yanmiyor",
        "aydinlatma yanmiyor",
        "dimmer calismiyor",
        "led yanmiyor",
    ),
    observe = { ctx ->
        buildList {
            add(linkObservation(ctx))
            if (ctx.linked) {
                // Only names we can actually list from live dim/relay state.
                // (No invented bulb sensors.)
            }
        }
    },
    navigateTo = HexonDestination.DimDimmers,
    ctaLabel = "Aydınlatmalara git",
    steps = listOf(
        HexonIssueStep("ask_which_light") { _, obs ->
            val link = obs.firstOrNull { it.key == "link" }?.summary.orEmpty()
            buildString {
                if (link.isNotBlank()) append(link).append(' ')
                append("Hangi aydınlatma veya dimmer yanmıyor? Uygulamada o kanalın adı görünüyor mu?")
            }
        },
        HexonIssueStep("ask_power_panel") { _, _ ->
            "Besleme / sigorta tarafını kontrol edebilir misin? Sonra uygulamada kanalın açık mı kapalı mı göründüğünü yaz."
        },
        HexonIssueStep(
            id = "service",
            kind = HexonIssueStepKind.Service,
        ) { _, _ ->
            "Donanım veya kablo müdahalesi gerekiyorsa yetkili servise danış. " +
                "Ben buradan röleyi veya dimmeri otomatik zorlamıyorum."
        },
    ),
)

private fun waterLevelIssueFlow(): HexonIssueFlow = HexonIssueFlow(
    topic = HexonIssueTopic.WaterLevel,
    startTriggers = listOf(
        "su seviyesi gorunmuyor",
        "tank seviyesi yok",
        "su sensoru calismiyor",
        "su verisi yok",
        "tank verisi gelmiyor",
    ),
    observe = { ctx ->
        buildList {
            add(linkObservation(ctx))
            if (ctx.linked) {
                val tanks = ctx.channels?.water.orEmpty().filter { it.enabled }
                if (tanks.isEmpty()) {
                    add(
                        HexonIssueObservation(
                            "tanks",
                            "Tanımlı / etkin bir su tankı kanalı görünmüyor.",
                        ),
                    )
                } else {
                    add(
                        HexonIssueObservation(
                            "tanks",
                            "Tanımlı tanklar: ${tanks.joinToString { it.name.ifBlank { "Tank ${it.id + 1}" } }}.",
                        ),
                    )
                }
            }
        }
    },
    navigateTo = HexonDestination.Water,
    ctaLabel = "Su Sistemi’ne git",
    steps = listOf(
        HexonIssueStep("ask_which_tank") { _, obs ->
            val facts = obs.joinToString(" ") { it.summary }
            buildString {
                if (facts.isNotBlank()) append(facts).append(' ')
                append("Hangi tankın seviyesi görünmüyor? Su ekranında o tank satırı var mı?")
            }
        },
        HexonIssueStep("ask_sensor_def") { _, _ ->
            "Tank sensörünün panel / su ayarlarında tanımlı ve etkin olduğunu kontrol edebilir misin?"
        },
        HexonIssueStep(
            id = "service",
            kind = HexonIssueStepKind.Service,
        ) { _, _ ->
            "Sensör kablosu veya kalibrasyon donanımı için yetkili servise başvurmanı öneririm. " +
                "Ölçülmeyen bir değeri uydurmam."
        },
    ),
)

private fun bleIssueFlow(): HexonIssueFlow = HexonIssueFlow(
    topic = HexonIssueTopic.BleConnection,
    startTriggers = listOf(
        "ble baglanmiyor",
        "bluetooth baglanmiyor",
        "ble baglanti kurulamiyor",
        "cihaz bulunamiyor",
        "eslesme olmuyor",
    ),
    observe = { ctx ->
        listOf(
            HexonIssueObservation(
                "ble",
                when {
                    ctx.bleReady -> "BLE şu an bağlı görünüyor."
                    ctx.remoteReady -> "BLE bağlı değil; Uzaktan bağlantı aktif."
                    else -> "Şu an ne BLE ne de Uzaktan bağlantı aktif görünmüyor."
                },
            ),
        )
    },
    navigateTo = HexonDestination.SettingsConnection,
    ctaLabel = "Bağlantı’ya git",
    steps = listOf(
        HexonIssueStep("ask_bt_on") { _, obs ->
            val fact = obs.firstOrNull()?.summary.orEmpty()
            buildString {
                if (fact.isNotBlank()) append(fact).append(' ')
                append("Telefonda Bluetooth açık mı ve karavana yakın mısın?")
            }
        },
        HexonIssueStep("ask_retry") { _, _ ->
            "VanGO bağlantı ekranından yeniden dener misin? Görünen bir hata metni varsa aynen yaz."
        },
        HexonIssueStep(
            id = "service",
            kind = HexonIssueStepKind.Service,
        ) { _, _ ->
            "Yakınlık, Bluetooth ve uygulama izinleri tamamsa cihaz / anten tarafı için yetkili servise danış. " +
                "Ben buradan bağlantıyı zorla kuramam."
        },
    ),
)

private fun motorObservations(ctx: HexonLiveContext): List<HexonIssueObservation> {
    val out = mutableListOf<HexonIssueObservation>()
    out += linkObservation(ctx)
    if (!ctx.linked) return out

    val motors = discoverMotors(ctx.data, ctx.channels)
    when {
        motors.isEmpty() -> out += HexonIssueObservation(
            "motor_cfg",
            "Uygulamada tanımlı / etkin bir motor kanalı görünmüyor.",
        )
        else -> {
            out += HexonIssueObservation(
                "motor_cfg",
                "Tanımlı motorlar: ${motors.joinToString { it.title }}.",
            )
            // Live direction bits only — not a fault code.
            val moving = motors.mapNotNull { m ->
                val dir = motorLiveDir(ctx.data, m)
                if (dir != MotorMoveDir.Idle) "${m.title} (${dir.name})" else null
            }
            if (moving.isNotEmpty()) {
                out += HexonIssueObservation(
                    "motor_live",
                    "Çıkışlarda şu an hareket bit’i görünenler: ${moving.joinToString()}.",
                )
            }
        }
    }
    // Explicitly do NOT invent lock / fault codes — they are not in VanGoData.
    return out
}

private fun linkObservation(ctx: HexonLiveContext): HexonIssueObservation {
    val label = when {
        ctx.bleReady -> "BLE"
        ctx.remoteReady -> "Uzaktan"
        else -> "Bağlı değil"
    }
    return HexonIssueObservation(
        "link",
        "Bağlantı durumu: $label.",
    )
}

private fun normalizeIssue(s: String): String =
    s.lowercase()
        .replace('ı', 'i')
        .replace('İ', 'i')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ö', 'o')
        .replace('ç', 'c')
        .trim()

private fun looksLikeContinuation(q: String): Boolean {
    val keys = listOf(
        "kontrol ettim", "kontrol ettim hala", "yaptim", "baktim", "denedim",
        "hala", "hâlâ", "calismiyor", "ayni", "degismedi", "duzelmedi",
        "evet", "hayir", "yok", "var", "gordum", "gorunmuyor", "uyari yok",
        "normal", "normalse", "tamam", "ok", "devam", "sonraki",
        "besleme", "enerji", "sigorta", "kablo", "gevsek", "servis",
    )
    return keys.any { q.contains(normalizeIssue(it)) }
}

private fun stillBroken(q: String): Boolean =
    listOf("hala", "calismiyor", "etmiyor", "yine", "hala calismiyor", "duzelmedi", "degismedi")
        .any { q.contains(normalizeIssue(it)) }

private fun stillAboutTopic(q: String, topic: HexonIssueTopic): Boolean {
    val keys = when (topic) {
        HexonIssueTopic.Motor -> listOf("motor", "tente", "panjur", "yatak")
        HexonIssueTopic.Lighting -> listOf("isik", "lamba", "dimmer", "aydinlat", "led")
        HexonIssueTopic.WaterLevel -> listOf("su", "tank", "seviye", "sensor")
        HexonIssueTopic.BleConnection -> listOf("ble", "bluetooth", "baglan", "esles")
    }
    return keys.any { q.contains(it) }
}

private fun impliesCableOrHardware(q: String): Boolean =
    listOf("gevsek", "kopuk", "kablo", "ayipli", "yanik", "sigorta atmis", "servis")
        .any { q.contains(normalizeIssue(it)) }

private fun isCancel(q: String): Boolean =
    listOf("vazgec", "iptal", "birak", "durdur", "gerek yok", "istemiyorum")
        .any { q.contains(normalizeIssue(it)) }
