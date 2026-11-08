package com.hextech.vango

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Local Hexon knowledge — keyword match → conversational reply + optional deep link.
 * Q&A corpus: docs/Hexon_1000_Gunluk_Teknik_Soru_Cevap.txt (+ live placeholders when linked).
 */
data class HexonFollowUp(
    val label: String,
    val seedQuery: String,
)

data class HexonMetricRow(
    val id: String,
    val label: String,
    val value: String,
    val detail: String? = null,
    val iconKey: String? = null,
)

sealed class HexonResultCard {
    abstract val title: String
    abstract val ctaLabel: String?
    abstract val navigateTo: HexonDestination?
    abstract val footer: String?

    data class Metrics(
        override val title: String,
        val rows: List<HexonMetricRow>,
        val headerIconKey: String? = null,
        override val ctaLabel: String? = null,
        override val navigateTo: HexonDestination? = null,
        override val footer: String? = null,
        val rowsNavigateTo: HexonDestination? = null,
    ) : HexonResultCard()
}

data class HexonReply(
    val text: String,
    val navigateTo: HexonDestination? = null,
    val ctaLabel: String? = null,
    val card: HexonResultCard? = null,
    val followUps: List<HexonFollowUp> = emptyList(),
)

data class HexonQuickChip(
    val label: String,
    val seedQuery: String,
)

data class HexonLiveContext(
    val linked: Boolean,
    val bleReady: Boolean,
    val remoteReady: Boolean,
    val data: VanGoData,
    val env: BleEnvSensors,
    val channels: PanelPlatformChannels?,
    val profile: DevicePanelProfileState,
    val alertCount: Int,
    val userFirstName: String? = null,
)

val HexonQuickChips = listOf(
    HexonQuickChip("Bağlantı", "Nasıl bağlanırım?"),
    HexonQuickChip("Su tankları", "Su seviyelerini göster"),
    HexonQuickChip("Röleler", "Röle durumlarını göster"),
    HexonQuickChip("Aydınlatma", "Dimmer nasıl kullanılır?"),
)

private const val WATER_LOW_PCT = 15
private const val WATER_DIRTY_HIGH_PCT = 85

private fun hexonIsEnglish(): Boolean =
    customAppLocale?.startsWith("en", ignoreCase = true) == true

fun hexonGreeting(userFirstName: String?, hour: Int): String {
    val en = hexonIsEnglish()
    val name = userFirstName?.takeIf { it.isNotBlank() }
        ?: if (en) "friend" else "dostum"
    val hello = when (hour) {
        in 5..11 -> if (en) "Good morning" else "Günaydın"
        in 12..17 -> if (en) "Good afternoon" else "İyi günler"
        in 18..22 -> if (en) "Good evening" else "İyi akşamlar"
        else -> if (en) "Hello" else "Merhaba"
    }
    return if (en) {
        "$hello $name, I’m Hexon. I help you navigate VanGO and understand your caravan — what should we look at?"
    } else {
        "$hello $name, ben Hexon. VanGO’da gezinmene ve karavanını tanımana yardımcı olurum — neye bakmak istersin?"
    }
}

suspend fun hexonRespond(
    rawQuery: String,
    ctx: HexonLiveContext,
): HexonReply {
    val q = normalize(rawQuery)
    if (q.isBlank()) {
        return HexonReply(
            if (hexonIsEnglish()) {
                "Type something — for example ‘show water levels’ or ‘how do I connect?’"
            } else {
                "Bir şey yazman yeterli — örneğin ‘su seviyelerini göster’ veya ‘nasıl bağlanırım’ de."
            },
        )
    }

    // —— VanGO sorun giderme (aktif oturum veya yeni arıza ifadesi) ——
    HexonIssueGuide.tryHandle(rawQuery, ctx)?.let { return it }

    // —— Genel sorun girişi (konu henüz seçilmedi) ——
    match(
        q,
        "bir sorunum var",
        "sorunum var",
        "problemim var",
        "bir problemim var",
        "bir sikayetim var",
        "yardima ihtiyacim var",
    )?.let {
        return HexonReply(
            text = "Anladım, buradayım. Sorun motor, aydınlatma, su seviyesi veya bağlantı (BLE) ile mi ilgili? " +
                "Kısa yazman yeterli — örneğin ‘motor çalışmıyor’.",
            followUps = listOf(
                HexonFollowUp("Motor", "Motor çalışmıyor"),
                HexonFollowUp("Aydınlatma", "Işık yanmıyor"),
                HexonFollowUp("Su", "Su seviyesi görünmüyor"),
                HexonFollowUp("Bağlantı", "BLE bağlanmıyor"),
            ),
        )
    }

    // —— Selam / yardım ——
    match(q, "neler yapabilirsin", "ne yapabilirsin", "yardim", "help", "nasil kullanirim")?.let {
        return HexonReply(
            "VanGO ekranlarını kullanmana yardımcı olabilir, bağlı cihazların durumunu açıklayabilir " +
                "ve bağlantı sorunlarında yol gösterebilirim.",
        )
    }
    match(q, "bana nasil yardimci", "nasil yardimci olabilirsin")?.let {
        return HexonReply(
            "VanGO kullanımı, bağlantı, su tankları, akü, sensörler ve aydınlatma hakkında sorularını " +
                "yanıtlayabilirim. Canlı değerler için karavana bağlı olmamız gerekir.",
        )
    }

    // —— Bağlantı ——
    match(q, "karavana bagli miyim", "bagli miyim", "baglanti durum")?.let {
        return HexonReply("Şu anki bağlantı durumun: ${connectionLabel(ctx)}.")
    }
    match(q, "nasil baglanirim", "nasil baglan", "baglanti nasil", "ble ile nasil baglanirim")?.let {
        return connectionHelpReply(ctx)
    }
    match(q, "sensorler nerede", "sensor nerede", "sensorler nerede gorunur")?.let {
        return HexonReply(
            text = "Sensör ve tank verileri bağlıyken Ana Sayfa, Su Sistemi ve İklim ekranlarında görünür. " +
                "Bağlantı yoksa canlı değerler gelmez.",
            navigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.Home,
            ctaLabel = if (!ctx.linked) "Bağlantı kur" else "Ana Sayfa’ya git",
            followUps = listOf(
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
                HexonFollowUp("Nasıl bağlanırım?", "Nasıl bağlanırım?"),
            ),
        )
    }
    match(q, "ble ve uzaktan", "ayni anda", "eşzaman", "es zaman")?.let {
        return HexonReply("Hayır. VanGO’da aynı anda yalnızca bir bağlantı modu aktif olur.")
    }
    match(q, "ble baglantisi kurulamiyor", "bluetooth baglanamiyor", "ble neden", "baglanti kurulamiyor")?.let {
        return HexonReply(
            text = "Telefonunda Bluetooth’un açık olduğunu ve karavana yakın olduğunu kontrol et. " +
                "Ardından VanGO bağlantı ekranından tekrar dene. Görünen bir hata varsa bana ilet.",
            navigateTo = HexonDestination.SettingsConnection,
            ctaLabel = "Bağlantı’ya git",
        )
    }
    match(q, "internet yokken", "internetsiz", "offline kullan")?.let {
        return HexonReply(
            "Karavanın yakınındaysan BLE bağlantısını deneyebilirsin. " +
                "Uzaktan kontrol ve bulut üzerinden çalışan sohbet özellikleri internet gerektirir. " +
                "Ben yerel rehber olarak internet olmadan da çalışırım.",
        )
    }
    match(q, "sistem bagli degil", "bagli degil diyor", "neden bagli degil")?.let {
        return HexonReply(
            text = "Telefon ile VanGO cihazı arasında aktif bağlantı yok. Yakındaysan BLE’yi, " +
                "uzaktaysan Uzaktan kontrol bağlantısını deneyelim.",
            navigateTo = HexonDestination.SettingsConnection,
            ctaLabel = "Bağlantı kur",
        )
    }
    match(q, "baglanti", "bluetooth", "ble", "eslestir", "cihaz bagla", "baglan", "offline")?.let {
        return if (ctx.linked) {
            HexonReply(
                text = "Şu an sistem bağlı görünüyor (${connectionLabel(ctx)}). " +
                    "Bağlantı tipini değiştirmek istersen Bağlantı sayfasına bakabiliriz.",
                navigateTo = HexonDestination.SettingsConnection,
                ctaLabel = "Bağlantı sayfasına git",
            )
        } else {
            HexonReply(
                text = "Karavanla henüz bir bağ kuramadık. BLE ile yakındayken yerel bağlanabilirsin; " +
                    "uzaktaysan Uzaktan Kontrol’ü dene.",
                navigateTo = HexonDestination.SettingsConnection,
                ctaLabel = "Bağlantı kur",
            )
        }
    }

    // —— Su ——
    match(q, "su seviyelerini goster", "su seviyesini goster", "tank seviyelerini goster", "su seviyelerini")?.let {
        return waterLevelsReply(ctx)
    }
    match(q, "su seviyelerini neden", "neden goremiyorum", "tank neden", "sensor verisi alinmiyor")?.let {
        return waterWhyMissingReply(ctx)
    }
    match(q, "temiz su az", "temiz su dusuk", "temiz su durumu")?.let {
        return cleanWaterLowReply(ctx)
    }
    match(q, "siyah su", "pis su", "kirli su tank", "siyah su tanki dolu")?.let {
        return dirtyWaterReply(ctx)
    }
    match(q, "su sensoru", "kalibre", "kalibrasyon", "calibrat", "water sensor")?.let {
        return HexonReply(
            text = if (hexonIsEnglish()) {
                "Tank calibration steps are not in the app yet. You can check live levels on the Water screen when connected."
            } else {
                "Tank kalibrasyon adımları henüz uygulamada yok. Bağlıyken Su ekranından canlı seviyeleri kontrol edebilirsin."
            },
            navigateTo = HexonDestination.Water,
            ctaLabel = if (hexonIsEnglish()) "Open Water" else "Su Sistemi’ne git",
        )
    }
    match(q, "su", "tank", "temiz su", "gri su", "pompa", "su seviye")?.let {
        return if (!ctx.linked && (q.contains("goster") || q.contains("seviye") || q.contains("kac"))) {
            waterLevelsReply(ctx)
        } else {
            HexonReply(
                text = "Su Sistemi’nde temiz, gri ve pis su tanklarını ve ilgili kontrolleri bulursun. Oraya geçelim mi?",
                navigateTo = HexonDestination.Water,
                ctaLabel = "Su Sistemi’ne git",
            )
        }
    }

    // —— Akü / enerji ——
    match(q, "akum ne durumda", "aku durumu", "batarya durumu", "aku ne durumda")?.let {
        return batteryStatusReply(ctx)
    }
    match(q, "kalan sure", "ne kadar yeter", "aku suresi")?.let {
        return HexonReply(
            text = "Akım ölçümü bu sürümde canlı akmıyor; kalan süre tahmini güvenilir verilmiyor. " +
                "Gerilim ve yaklaşık doluluk için Enerji İzleme’ye bakabilirsin.",
            navigateTo = HexonDestination.Energy,
            ctaLabel = "Enerji’ye git",
        )
    }
    match(q, "aku neden hizli", "neden hizli bitiyor", "hizli tuket")?.let {
        return batteryDrainReply(ctx)
    }
    match(q, "enerji", "aku", "batarya", "tuketim", "watt", "guc", "volt")?.let {
        return HexonReply(
            text = "Enerji İzleme’de güç ve akü özetine bakabilirsin. " + batteryShort(ctx),
            navigateTo = HexonDestination.Energy,
            ctaLabel = "Enerji’ye git",
        )
    }

    // —— Aydınlatma / dimmer / RGB / komutlar ——
    match(q, "hangi isiklar acik", "acik isik", "acik aydinlat")?.let {
        return openLightsReply(ctx)
    }
    match(q, "tum isiklari kapat", "butun isiklari kapat", "isiklari kapat")?.let {
        val open = openLightNames(ctx)
        val n = open.size.coerceAtLeast(1)
        return HexonReply(
            text = if (open.isEmpty()) {
                "Şu an açık görünen bir aydınlatma yok. Yine de Dim ekranından kontrol edebilirsin."
            } else {
                "$n aydınlatmayı kapatmamı onaylıyor musun? Onaylı komutu buradan otomatik göndermiyorum; " +
                    "Aydınlatmalar ekranından güvenle kapatabilirsin. Açık olanlar: ${open.joinToString(", ")}."
            },
            navigateTo = HexonDestination.DimDimmers,
            ctaLabel = "Aydınlatmalara git",
        )
    }
    match(q, "spot isigi ac", "spot aydinlatma ac", "spot ac")?.let {
        return HexonReply(
            text = "Spot Aydınlatma’yı buradan otomatik açmıyorum — yanlışlıkla tetiklenmesin. " +
                "Butonlar ekranından Spot’a basabilirsin; komut sonrası cihaz geri bildirimi orada görünür.",
            navigateTo = HexonDestination.DimRelays,
            ctaLabel = "Butonlara git",
        )
    }
    match(q, "dimmer", "dimeri", "%", "yuzde")?.let {
        if (q.contains("nasil kullan") || q.contains("nasil ayarla")) {
            return HexonReply(
                text = "Dim ekranında ilgili ışığın kaydırıcısını hareket ettirerek parlaklığı ayarlayabilirsin; " +
                    "güç düğmesiyle açıp kapatabilirsin.",
                navigateTo = HexonDestination.DimDimmers,
                ctaLabel = "Aydınlatmalara git",
            )
        }
        if (Regex("""dimmer?\s*\d""").containsMatchIn(q) || q.contains("%")) {
            return HexonReply(
                text = "Dimmer parlaklığını buradan doğrudan yazmıyorum; değeri cihazdan doğrulamak için " +
                    "Aydınlatmalar ekranındaki kaydırıcıyı kullan. İstediğin yüzdeyi orada ayarlayabilirsin.",
                navigateTo = HexonDestination.DimDimmers,
                ctaLabel = "Aydınlatmalara git",
            )
        }
    }
    match(q, "rgb", "renkli isik", "rengin")?.let {
        return HexonReply(
            text = "Evet. RGB Aydınlatma bölümünden renk seçebilirsin. " +
                "İstersen Aydınlatmalar sayfasına götüreyim.",
            navigateTo = HexonDestination.DimDimmers,
            ctaLabel = "Aydınlatmalara git",
        )
    }
    match(q, "aydinlat", "isik", "lamba", "led", "spot")?.let {
        return HexonReply(
            text = "Aydınlatmalar sayfasında dimmerleri kaydırarak parlaklığı ayarlayabilir, " +
                "RGB’yi de aynı yerden yönetebilirsin.",
            navigateTo = HexonDestination.DimDimmers,
            ctaLabel = "Aydınlatmalara git",
        )
    }
    match(
        q,
        "role durum",
        "role durumlarini goster",
        "role durumlarini",
        "roleleri goster",
        "buton durum",
        "butonlari goster",
        "cikis durum",
        "relay durum",
        "hangi role",
        "hangi buton",
    )?.let {
        return relayStatusesReply(ctx)
    }
    match(q, "role", "buton", "cikis", "priz", "relay", "role adi", "adini nasil")?.let {
        if (q.contains("adini") || q.contains("isim") || q.contains("yeniden ad")) {
            return HexonReply(
                text = "Panel Ayarları → Butonlar’dan ilgili röleyi seçip görünen adını değiştirebilirsin.",
                navigateTo = HexonDestination.SettingsPanel,
                ctaLabel = "Panel ayarlarına git",
            )
        }
        if (q.contains("goster") || q.contains("durum") || q.contains("liste") || q.contains("acik")) {
            return relayStatusesReply(ctx)
        }
        return HexonReply(
            text = "Butonlar (röleler) karavandaki aç/kapa çıkışlar. Hepsi Dim sayfasının Butonlar sekmesinde.",
            navigateTo = HexonDestination.DimRelays,
            ctaLabel = "Butonlara git",
        )
    }

    // —— Sensör / iklim ——
    match(q, "ic sicaklik", "sicaklik kac", "nem kac", "kac derece")?.let {
        return indoorClimateReply(ctx)
    }
    match(q, "sensor degeri neden", "sensor neden", "veri alinamiyor")?.let {
        return HexonReply(
            text = if (!ctx.linked) {
                "Sensörden güncel veri alınamıyor — önce bağlantıyı kuralım."
            } else {
                "İç ortam sensöründen güncel veri alınamıyor olabilir. Bağlantıyı ve sensörün panelde " +
                    "tanımlı olup olmadığını kontrol edelim."
            },
            navigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.Climate,
            ctaLabel = if (!ctx.linked) "Bağlantı kur" else "Sensör’e git",
        )
    }
    match(q, "sensor", "iklim", "sicaklik", "nem", "klima", "fan", "isi")?.let {
        return HexonReply(
            text = "Sensör ekranında iç/dış sıcaklık, nem ve fan gibi iklim kontrollerini görürsün. " +
                indoorClimateShort(ctx),
            navigateTo = HexonDestination.Climate,
            ctaLabel = "Sensör’e git",
        )
    }

    // —— Motor (komut ≠ sorun giderme; arızalar HexonIssueGuide) ——
    match(q, "motoru ac", "tente ac", "tente kapat", "motor ac", "panjur ac")?.let {
        return HexonReply(
            text = "Motor hareketi için Motor ekranından ilgili kontrolü seç. " +
                "Hareket alanının güvenli olduğunu doğrulayıp işlemi oradan onayla — " +
                "ben buradan motoru çalıştırmıyorum.",
            navigateTo = HexonDestination.Motor,
            ctaLabel = "Motor’a git",
        )
    }
    match(q, "motor", "tente", "yatak", "havalandirma motor", "panjur")?.let {
        // Avoid swallowing open troubleshooting phrases if guide missed them.
        if (q.contains("calismiyor") || q.contains("hareket etmiyor") || q.contains("acilmiyor")) {
            HexonIssueGuide.tryHandle(rawQuery, ctx)?.let { return it }
        }
        return HexonReply(
            text = "Motor sayfasında tente, panjur veya benzeri hareketli üniteleri hız ve yön ile kontrol edersin. " +
                "Çalışmıyorsa birlikte adım adım bakabiliriz — ‘motor çalışmıyor’ yazman yeterli.",
            navigateTo = HexonDestination.Motor,
            ctaLabel = "Motor’a git",
        )
    }

    // —— Mini / model ——
    match(q, "vango mini", "mini de bu", "ozellik var mi", "medium")?.let {
        return miniFeatureReply(ctx, q)
    }

    // —— Hata / destek ——
    match(q, "bir hata var mi", "hata var mi", "aktif hata", "alarm var mi")?.let {
        return alertsReply(ctx)
    }
    match(q, "destek almak", "destek istiyorum", "yardim talebi")?.let {
        return HexonReply(
            text = "Sorunun kısa açıklamasını, cihaz modelini (${ctx.profile.displayName}) ve paylaşmayı " +
                "onayladığın hata kayıtlarını destek talebine ekleyebiliriz. Onayın olmadan kayıt göndermem.",
            navigateTo = HexonDestination.SettingsAbout,
            ctaLabel = "Hakkında & Yardım",
        )
    }

    // —— Diğer navigasyon ——
    match(q, "bildirim")?.let {
        if (q.contains("ayar")) {
            return HexonReply(
                text = "Hangi olayların bildirim üreteceğini Ayarlar → Bildirimler’den yönetirsin.",
                navigateTo = HexonDestination.SettingsNotificationPrefs,
                ctaLabel = "Bildirim ayarlarına git",
            )
        }
        return HexonReply(
            text = "Gelen uyarı ve olayları Bildirimler kutusunda topluyoruz.",
            navigateTo = HexonDestination.NotificationsInbox,
            ctaLabel = "Bildirimlere git",
        )
    }
    match(q, "karavan tipi", "motokaravan", "cekme karavan", "motorhome", "karavan sec")?.let {
        return HexonReply(
            text = "Ana sayfadaki kahraman görsel, motokaravan veya çekme karavana göre değişir. Tipi buradan seçebilirsin.",
            navigateTo = HexonDestination.CaravanType,
            ctaLabel = "Karavan tipini seç",
        )
    }
    match(q, "panel ayar", "panel duzen", "kanal", "i/o", "io modul")?.let {
        return HexonReply(
            text = "Panel Ayarları’nda buton, aydınlatma, su, sensör ve motor kanallarını düzenlersin.",
            navigateTo = HexonDestination.SettingsPanel,
            ctaLabel = "Panel ayarlarına git",
        )
    }
    match(q, "tema", "koyu mod", "acik tema", "gorunum")?.let {
        return HexonReply(
            text = "Uygulamanın koyu/açık görünümünü Tema ayarından seçebilirsin.",
            navigateTo = HexonDestination.SettingsTheme,
            ctaLabel = "Tema’ya git",
        )
    }
    if (!q.contains("konus")) {
        match(q, "dil", "language")?.let {
            return HexonReply(
                text = "VanGO Türkçe veya İngilizce çalışabilir — Dil ayarından değiştirirsin.",
                navigateTo = HexonDestination.SettingsLanguage,
                ctaLabel = "Dil ayarına git",
            )
        }
    }
    match(q, "profil", "hesap", "sifre", "kullanici", "giris", "login")?.let {
        return HexonReply(
            text = "Profilinde ad, hesap bilgileri ve şifre yönetimine ulaşırsın.",
            navigateTo = HexonDestination.SettingsProfile,
            ctaLabel = "Profil’e git",
        )
    }
    match(q, "hakkinda", "kilavuz", "sss")?.let {
        return HexonReply(
            text = "Hakkında & Yardım’da uygulama bilgisi ve destek metinleri var.",
            navigateTo = HexonDestination.SettingsAbout,
            ctaLabel = "Hakkında’ya git",
        )
    }
    match(q, "ayar", "settings", "menu ayar")?.let {
        return HexonReply(
            text = "Ayarlar’da panel, bağlantı, tema, dil, profil, bildirimler ve hakkında bölümleri var.",
            navigateTo = HexonDestination.SettingsList,
            ctaLabel = "Ayarlar’a git",
        )
    }
    match(q, "ana sayfa", "home", "dashboard", "ozet")?.let {
        return HexonReply(
            text = "Ana Sayfa karavanın özeti: durum, aydınlatma, butonlar ve tanklara hızlı bakış.",
            navigateTo = HexonDestination.Home,
            ctaLabel = "Ana Sayfa’ya dön",
        )
    }

    // —— Günlük / teknik / arıza (çevrimdışı 1000’lik niyet) ——
    // Canlı metrik ve navigasyondan sonra; ölçüm uydurma yok.
    HexonDailyChatMatcher.match(rawQuery)?.let { hit ->
        return HexonReply(text = hit.answer)
    }

    return unresolvedChatFallback(rawQuery)
}

private suspend fun unresolvedChatFallback(rawQuery: String): HexonReply {
    // Optional Hexnet LLM when local match is weak; endpoint may not exist yet.
    if (isInternetAvailable()) {
        val remote = HexonRemoteChat.ask(rawQuery)
        if (!remote.isNullOrBlank()) {
            return HexonReply(text = remote.trim())
        }
    }
    return HexonReply(
        text = "Bunu yerel olarak eşleştiremedim. Örneğin ‘motor çalışmıyor’, ‘su seviyelerini göster’ " +
            "veya ‘nasıl bağlanırım’ yazabilirsin.",
    )
}

// —— Live helpers ——

private fun connectionLabel(ctx: HexonLiveContext): String = when {
    ctx.bleReady -> "BLE"
    ctx.remoteReady -> "Uzaktan"
    else -> "Bağlı değil"
}

private fun tankPct(data: VanGoData, tankId: Int): Int =
    data.getSafeSensorsDataUnsigned(tankId + 2).coerceIn(0, 100)

/** Bu karavanda tanımlı (enabled) tanklar — sabit 3’lü liste değil. */
private fun installedWaterChannels(ctx: HexonLiveContext): List<PanelPlatformChannel> {
    val configured = ctx.channels?.water.orEmpty()
    if (configured.isNotEmpty()) {
        return configured
            .filter { it.enabled }
            .sortedBy { it.sortOrder }
    }
    val count = ctx.profile.caps.water.coerceIn(1, 4)
    return defaultWaterChannels(count).filter { isWaterTankInstalled(it.id, null) }
}

private fun waterTankRows(ctx: HexonLiveContext): List<HexonMetricRow> {
    return installedWaterChannels(ctx).map { ch ->
        val label = ch.name.trim().ifBlank {
            listOf("Temiz su", "Gri su", "Siyah su", "Yakıt").getOrElse(ch.id) { "Tank ${ch.id + 1}" }
        }
        val iconKey = ch.icon.trim().ifBlank { defaultWaterIcon(ch.id) }
        if (!ctx.linked) {
            HexonMetricRow(
                id = "tank-${ch.id}",
                label = label,
                value = "Veri yok",
                detail = "—",
                iconKey = iconKey,
            )
        } else {
            val pct = tankPct(ctx.data, ch.id)
            val cap = waterTankCapacityLiters(ch.id, ctx.channels)
            val detail = if (cap > 0) {
                "~${waterRemainingLiters(pct, cap)} L / $cap L"
            } else {
                null
            }
            HexonMetricRow(
                id = "tank-${ch.id}",
                label = label,
                value = "%$pct",
                detail = detail,
                iconKey = iconKey,
            )
        }
    }
}

private fun waterLevelsCard(ctx: HexonLiveContext): HexonResultCard.Metrics {
    val rows = waterTankRows(ctx)
    return if (!ctx.linked) {
        HexonResultCard.Metrics(
            title = "SU TANKLARI",
            headerIconKey = "temiz_su_tanki",
            rows = rows.ifEmpty {
                listOf(
                    HexonMetricRow("none", "Tanımlı tank yok", "—", iconKey = "su_seviye"),
                )
            },
            ctaLabel = "Bağlantı kur",
            navigateTo = HexonDestination.SettingsConnection,
            footer = "Canlı verilere erişim için bağlantı gerekli.",
            rowsNavigateTo = HexonDestination.SettingsConnection,
        )
    } else {
        HexonResultCard.Metrics(
            title = "SU TANKLARI",
            headerIconKey = "temiz_su_tanki",
            rows = rows.ifEmpty {
                listOf(
                    HexonMetricRow("none", "Tanımlı tank yok", "—", iconKey = "su_seviye"),
                )
            },
            ctaLabel = "Su Sistemi’ne git",
            navigateTo = HexonDestination.Water,
            footer = if (rows.isEmpty()) {
                "Panel / Su ayarlarından tankları tanımlayabilirsin."
            } else {
                "Yalnızca bu karavanda tanımlı tanklar gösterilir."
            },
            rowsNavigateTo = HexonDestination.Water,
        )
    }
}

private fun waterLevelsReply(ctx: HexonLiveContext): HexonReply {
    val card = waterLevelsCard(ctx)
    val text = if (!ctx.linked) {
        "Şu anda karavana bağlı değilsin. Bağlandığında tank seviyelerini gösterebilirim."
    } else {
        val rows = waterTankRows(ctx)
        if (rows.isEmpty()) {
            "Takılı ve tanımlı bir tank sensörü görünmüyor. Panel / Su ayarlarından tankları kontrol edelim."
        } else {
            "Tanımlı tankların güncel durumu aşağıda."
        }
    }
    return HexonReply(
        text = text,
        card = card,
        followUps = if (!ctx.linked) {
            listOf(
                HexonFollowUp("BLE ile nasıl bağlanırım?", "BLE ile nasıl bağlanırım?"),
                HexonFollowUp("Sensörler nerede görünür?", "Sensörler nerede görünür?"),
            )
        } else {
            listOf(
                HexonFollowUp("Akü durumu", "Aküm ne durumda?"),
                HexonFollowUp("Röle durumları", "Röle durumlarını göster"),
                HexonFollowUp("Açık ışıklar", "Hangi ışıklar açık?"),
            )
        },
    )
}

private fun connectionHelpReply(ctx: HexonLiveContext): HexonReply {
    val card = HexonResultCard.Metrics(
        title = "BAĞLANTI",
        headerIconKey = "bluetooth",
        rows = listOf(
            HexonMetricRow(
                id = "ble",
                label = "BLE",
                value = if (ctx.bleReady) "Bağlı" else "Kapalı",
                detail = if (ctx.bleReady) "Yerel" else "—",
                iconKey = "bluetooth",
            ),
            HexonMetricRow(
                id = "remote",
                label = "Uzaktan",
                value = if (ctx.remoteReady) "Bağlı" else "Kapalı",
                detail = if (ctx.remoteReady) "MQTT" else "—",
                iconKey = "wifi",
            ),
        ),
        ctaLabel = if (ctx.linked) "Bağlantı sayfasına git" else "Bağlantı kur",
        navigateTo = HexonDestination.SettingsConnection,
        footer = if (ctx.linked) {
            "Aktif mod: ${connectionLabel(ctx)}."
        } else {
            "Karavana yakındaysan BLE, uzaktaysan Uzaktan kontrol kullan."
        },
        rowsNavigateTo = HexonDestination.SettingsConnection,
    )
    return HexonReply(
        text = if (ctx.linked) {
            "Şu an sistem bağlı görünüyor (${connectionLabel(ctx)}). Değiştirmek istersen Bağlantı sayfasına bakabiliriz."
        } else {
            "Bağlantı ekranını açalım. Karavanın yanındaysan BLE, uzaktaysan internet üzerinden " +
                "Uzaktan kontrol modunu seçebilirsin."
        },
        card = card,
        followUps = listOf(
            HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
            HexonFollowUp("Akü durumu", "Aküm ne durumda?"),
        ),
    )
}

private fun waterWhyMissingReply(ctx: HexonLiveContext): HexonReply {
    val tanks = installedWaterChannels(ctx)
    val reason = when {
        !ctx.linked -> "bağlantı yok"
        tanks.isEmpty() -> "bu tankların sensörü tanımlı değil"
        else -> "sensör verisi alınmıyor veya güncel değil olabilir"
    }
    return HexonReply(
        text = "Şu anda $reason. Önce ilgili durumu kontrol edelim.",
        card = waterLevelsCard(ctx),
        followUps = if (!ctx.linked) {
            listOf(
                HexonFollowUp("BLE ile nasıl bağlanırım?", "BLE ile nasıl bağlanırım?"),
                HexonFollowUp("Sensörler nerede görünür?", "Sensörler nerede görünür?"),
            )
        } else {
            listOf(
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
                HexonFollowUp("Bağlantı durumu", "Nasıl bağlanırım?"),
            )
        },
    )
}

private fun cleanWaterLowReply(ctx: HexonLiveContext): HexonReply {
    if (!isWaterTankInstalled(0, ctx.channels) && installedWaterChannels(ctx).none { it.id == 0 }) {
        return HexonReply(
            text = "Temiz su tankı bu kurulumda tanımlı görünmüyor.",
            card = waterLevelsCard(ctx),
            followUps = listOf(
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
            ),
        )
    }
    return waterLevelsReply(ctx).copy(
        text = if (!ctx.linked) {
            "Temiz suyu görmek için önce bağlantı kurmalıyız."
        } else {
            val pct = tankPct(ctx.data, 0)
            val state = if (pct <= WATER_LOW_PCT) "düşük" else "normal"
            "Temiz su seviyesi %$pct. Tanımlı düşük seviye eşiği %$WATER_LOW_PCT olduğu için durum $state."
        },
    )
}

private fun dirtyWaterReply(ctx: HexonLiveContext): HexonReply {
    if (!isWaterTankInstalled(2, ctx.channels) && installedWaterChannels(ctx).none { it.id == 2 }) {
        return HexonReply(
            text = "Siyah / pis su tankı bu kurulumda tanımlı görünmüyor.",
            card = waterLevelsCard(ctx),
            followUps = listOf(
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
            ),
        )
    }
    return waterLevelsReply(ctx).copy(
        text = if (!ctx.linked) {
            "Siyah suyu görmek için önce bağlantı kurmalıyız."
        } else {
            val pct = tankPct(ctx.data, 2)
            val warn = if (pct >= WATER_DIRTY_HIGH_PCT) "aktif" else "aktif değil"
            "Siyah su seviyesi %$pct. Doluluk uyarısı $warn."
        },
    )
}

private fun batterySoc(volt: Float): Int? {
    if (volt <= 0f) return null
    return (((volt - 11.5f) / (13.0f - 11.5f)) * 100f).toInt().coerceIn(0, 100)
}

private fun fmtVolt(v: Float, digits: Int = 1): String {
    val factor = if (digits <= 1) 10 else 100
    val scaled = (v * factor).toInt()
    val whole = scaled / factor
    val frac = kotlin.math.abs(scaled % factor)
    return if (digits <= 1) "$whole.${frac}" else {
        val f = frac.toString().padStart(2, '0')
        "$whole.$f"
    }
}

private fun batteryShort(ctx: HexonLiveContext): String {
    if (!ctx.linked || ctx.data.volt <= 0f) return "Canlı akü verisi yok."
    val soc = batterySoc(ctx.data.volt)
    return if (soc != null) {
        "Ölçülen yaklaşık ${fmtVolt(ctx.data.volt)} V (%$soc)."
    } else {
        "Ölçülen yaklaşık ${fmtVolt(ctx.data.volt)} V."
    }
}

private fun batteryStatusReply(ctx: HexonLiveContext): HexonReply {
    val rows = if (!ctx.linked) {
        listOf(
            HexonMetricRow("volt", "Voltaj", "Veri yok", "—", iconKey = "aku"),
            HexonMetricRow("soc", "SOC", "Veri yok", "—", iconKey = "aku"),
        )
    } else if (ctx.data.volt <= 0f) {
        listOf(
            HexonMetricRow("volt", "Voltaj", "Bekleniyor", "—", iconKey = "aku"),
            HexonMetricRow("soc", "SOC", "—", null, iconKey = "aku"),
        )
    } else {
        val soc = batterySoc(ctx.data.volt)
        listOf(
            HexonMetricRow(
                id = "volt",
                label = "Voltaj",
                value = "${fmtVolt(ctx.data.volt, 2)} V",
                iconKey = "aku",
            ),
            HexonMetricRow(
                id = "soc",
                label = "SOC",
                value = if (soc != null) "%$soc" else "—",
                detail = "Yaklaşık",
                iconKey = "aku",
            ),
            HexonMetricRow(
                id = "amps",
                label = "Akım",
                value = "Ölçülmüyor",
                detail = "Bu sürüm",
                iconKey = "aku",
            ),
        )
    }
    val card = HexonResultCard.Metrics(
        title = "AKÜ / ENERJİ",
        headerIconKey = "aku",
        rows = rows,
        ctaLabel = if (!ctx.linked) "Bağlantı kur" else "Enerji’ye git",
        navigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.Energy,
        footer = if (!ctx.linked) {
            "Canlı verilere erişim için bağlantı gerekli."
        } else {
            "Akım (A) bu sürümde canlı okunmuyor."
        },
        rowsNavigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.Energy,
    )
    val text = when {
        !ctx.linked -> "Akü durumunu görmek için önce bağlantı kurmalıyız."
        ctx.data.volt <= 0f -> "Ölçülen akü değeri henüz gelmedi. Enerji ekranından tekrar kontrol edelim."
        else -> {
            val soc = batterySoc(ctx.data.volt)
            buildString {
                append("Ölçülen akü değerleri: ${fmtVolt(ctx.data.volt, 2)} V")
                if (soc != null) append(" ve yaklaşık %$soc SOC")
                append(".")
            }
        }
    }
    return HexonReply(
        text = text,
        card = card,
        followUps = if (!ctx.linked) {
            listOf(
                HexonFollowUp("BLE ile nasıl bağlanırım?", "BLE ile nasıl bağlanırım?"),
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
            )
        } else {
            listOf(
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
                HexonFollowUp("Açık ışıklar", "Hangi ışıklar açık?"),
            )
        },
    )
}

private fun batteryDrainReply(ctx: HexonLiveContext): HexonReply {
    val lights = openLightNames(ctx)
    val loads = if (lights.isEmpty()) "belirgin açık aydınlatma görünmüyor" else "açık yükler: ${lights.joinToString(", ")}"
    return HexonReply(
        text = "Şu anda $loads. Canlı akım (A) ölçümü yok; en yüksek tüketimi görmek için açık cihazları " +
            "birlikte Aydınlatmalar / Butonlar’dan kontrol edebiliriz.",
        navigateTo = HexonDestination.DimDimmers,
        ctaLabel = "Aydınlatmalara git",
    )
}

private fun openLightNames(ctx: HexonLiveContext): List<String> {
    if (!ctx.linked) return emptyList()
    val names = mutableListOf<String>()
    val dimCount = ctx.channels?.dimmers?.count { it.enabled }
        ?: ctx.data.numOfD.coerceAtLeast(0)
    for (i in 0 until dimCount.coerceAtMost(8)) {
        val enabled = ctx.channels?.dimmers?.firstOrNull { it.id == i }?.enabled ?: true
        if (!enabled) continue
        val level = ctx.data.getSafeDimsData(i).toInt() and 0xFF
        if (level > 0) {
            val label = ctx.channels?.dimmers?.firstOrNull { it.id == i }?.name?.takeIf { it.isNotBlank() }
                ?: "Dimmer ${i + 1}"
            names += "$label (%$level)"
        }
    }
    if ((ctx.data.getSafeRGBData(3).toInt() and 0xFF) != 0) {
        names += "RGB Aydınlatma"
    }
    val relayCount = ctx.channels?.relays?.count { it.enabled }
        ?: ctx.data.numOfO.coerceAtMost(12)
    for (i in 0 until relayCount) {
        val enabled = ctx.channels?.relays?.firstOrNull { it.id == i }?.enabled ?: true
        if (!enabled) continue
        val on = ctx.data.getSafeOutputsData(i).toInt() != 0
        if (on) {
            val label = ctx.channels?.relays?.firstOrNull { it.id == i }?.name?.takeIf { it.isNotBlank() }
                ?: "Röle ${i + 1}"
            names += label
        }
    }
    return names
}

private fun relayStatusRows(ctx: HexonLiveContext): List<HexonMetricRow> {
    if (!ctx.linked) return emptyList()
    val configured = ctx.channels?.relays?.filter { it.enabled }?.sortedBy { it.sortOrder }
    if (!configured.isNullOrEmpty()) {
        return configured.map { ch ->
            val on = ctx.data.getSafeOutputsData(ch.id).toInt() != 0
            HexonMetricRow(
                id = "relay-${ch.id}",
                label = ch.name.takeIf { it.isNotBlank() } ?: "Röle ${ch.id + 1}",
                value = if (on) "Açık" else "Kapalı",
                iconKey = ch.icon.takeIf { it.isNotBlank() && it != "circle" } ?: "priz_220v",
            )
        }
    }
    val count = ctx.data.numOfO.coerceIn(0, 16)
    return (0 until count).map { i ->
        val on = ctx.data.getSafeOutputsData(i).toInt() != 0
        HexonMetricRow(
            id = "relay-$i",
            label = "Röle ${i + 1}",
            value = if (on) "Açık" else "Kapalı",
            iconKey = "priz_220v",
        )
    }
}

private fun relayStatusesReply(ctx: HexonLiveContext): HexonReply {
    val rows = relayStatusRows(ctx)
    val card = HexonResultCard.Metrics(
        title = "RÖLE / BUTONLAR",
        headerIconKey = "priz_220v",
        rows = rows.ifEmpty {
            listOf(
                HexonMetricRow(
                    id = "none",
                    label = if (!ctx.linked) "Röle durumu" else "Tanımlı röle yok",
                    value = "—",
                    iconKey = "priz_220v",
                ),
            )
        },
        ctaLabel = if (!ctx.linked) "Bağlantı kur" else "Butonlara git",
        navigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.DimRelays,
        footer = when {
            !ctx.linked -> "Canlı verilere erişim için bağlantı gerekli."
            rows.isEmpty() -> "Panel / Butonlar ayarlarından röleleri tanımlayabilirsin."
            else -> "Yalnızca bu karavanda tanımlı ve etkin röleler listelenir. Komut buradan gönderilmez."
        },
        rowsNavigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.DimRelays,
    )
    val openCount = rows.count { it.value == "Açık" }
    val text = when {
        !ctx.linked -> "Röle durumlarını görmek için önce bağlantı kurmalıyız."
        rows.isEmpty() -> "Takılı ve tanımlı bir röle görünmüyor. Panel / Butonlar ayarlarından kontrol edelim."
        openCount == 0 -> "Tanımlı rölelerin durumu aşağıda. Şu an hiçbiri açık görünmüyor."
        else -> "Tanımlı rölelerin güncel durumu aşağıda. Açık: $openCount / ${rows.size}."
    }
    return HexonReply(
        text = text,
        card = card,
        followUps = if (!ctx.linked) {
            listOf(
                HexonFollowUp("BLE ile nasıl bağlanırım?", "BLE ile nasıl bağlanırım?"),
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
            )
        } else {
            listOf(
                HexonFollowUp("Açık ışıklar", "Hangi ışıklar açık?"),
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
            )
        },
    )
}

private fun openLightsReply(ctx: HexonLiveContext): HexonReply {
    val rows = mutableListOf<HexonMetricRow>()
    if (!ctx.linked) {
        rows += HexonMetricRow("offline", "Aydınlatma", "Veri yok", "—", iconKey = "tavan_aydinlatma")
    } else {
        val dimCount = ctx.channels?.dimmers?.count { it.enabled }
            ?: ctx.data.numOfD.coerceAtLeast(0)
        val dimmers = ctx.channels?.dimmers?.filter { it.enabled }?.sortedBy { it.sortOrder }
        if (dimmers != null && dimmers.isNotEmpty()) {
            for (ch in dimmers) {
                val level = ctx.data.getSafeDimsData(ch.id).toInt() and 0xFF
                val label = ch.name.takeIf { it.isNotBlank() } ?: "Dimmer ${ch.id + 1}"
                rows += HexonMetricRow(
                    id = "dim-${ch.id}",
                    label = label,
                    value = if (level > 0) "Açık %$level" else "Kapalı",
                    iconKey = ch.icon.ifBlank { "tavan_aydinlatma" },
                )
            }
        } else {
            for (i in 0 until dimCount.coerceAtMost(8)) {
                val level = ctx.data.getSafeDimsData(i).toInt() and 0xFF
                rows += HexonMetricRow(
                    id = "dim-$i",
                    label = "Dimmer ${i + 1}",
                    value = if (level > 0) "Açık %$level" else "Kapalı",
                    iconKey = "tavan_aydinlatma",
                )
            }
        }
        val rgbOn = (ctx.data.getSafeRGBData(3).toInt() and 0xFF) != 0
        rows += HexonMetricRow(
            id = "rgb",
            label = "RGB Aydınlatma",
            value = if (rgbOn) "Açık" else "Kapalı",
            iconKey = "rgb_aydinlatma",
        )
    }
    val card = HexonResultCard.Metrics(
        title = "AYDINLATMA",
        headerIconKey = "tavan_aydinlatma",
        rows = rows.ifEmpty {
            listOf(HexonMetricRow("none", "Tanımlı dimmer yok", "—", iconKey = "tavan_aydinlatma"))
        },
        ctaLabel = if (!ctx.linked) "Bağlantı kur" else "Aydınlatmalara git",
        navigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.DimDimmers,
        footer = if (!ctx.linked) {
            "Canlı verilere erişim için bağlantı gerekli."
        } else {
            "Yalnızca bu karavanda tanımlı kanallar listelenir."
        },
        rowsNavigateTo = if (!ctx.linked) HexonDestination.SettingsConnection else HexonDestination.DimDimmers,
    )
    val open = openLightNames(ctx)
    val text = when {
        !ctx.linked -> "Açık ışıkları görmek için önce bağlantı kurmalıyız."
        open.isEmpty() -> "Açık görünen bir aydınlatma / röle yok."
        else -> "Açık görünen aydınlatmalar aşağıda özetlendi."
    }
    return HexonReply(
        text = text,
        card = card,
        followUps = if (!ctx.linked) {
            listOf(
                HexonFollowUp("BLE ile nasıl bağlanırım?", "BLE ile nasıl bağlanırım?"),
                HexonFollowUp("Dimmer nasıl kullanılır?", "Dimmer nasıl kullanılır?"),
            )
        } else {
            listOf(
                HexonFollowUp("Dimmer nasıl kullanılır?", "Dimmer nasıl kullanılır?"),
                HexonFollowUp("Su seviyeleri", "Su seviyelerini göster"),
            )
        },
    )
}

private fun indoorClimateShort(ctx: HexonLiveContext): String {
    if (!ctx.linked) return ""
    val t = BleEnvSensorParser.resolveDisplayTempC(ctx.env, ctx.data).takeIf { it in 1..60 }
    val h = BleEnvSensorParser.resolveDisplayHumidityPct(ctx.env, ctx.data).takeIf { it in 1..100 }
    return when {
        t != null && h != null -> "Şu an yaklaşık ${t}°C, nem %$h."
        t != null -> "Şu an yaklaşık ${t}°C."
        else -> ""
    }
}

private fun indoorClimateReply(ctx: HexonLiveContext): HexonReply {
    if (!ctx.linked) {
        return HexonReply(
            text = "İç sıcaklığı görmek için önce bağlantı kurmalıyız.",
            navigateTo = HexonDestination.SettingsConnection,
            ctaLabel = "Bağlantı kur",
        )
    }
    val t = BleEnvSensorParser.resolveDisplayTempC(ctx.env, ctx.data).takeIf { it in 1..60 }
    val h = BleEnvSensorParser.resolveDisplayHumidityPct(ctx.env, ctx.data).takeIf { it in 1..100 }
    val text = when {
        t != null && h != null -> "İç sıcaklık $t °C, nem %$h."
        t != null -> "İç sıcaklık $t °C. Nem verisi yok."
        h != null -> "Nem %$h. Sıcaklık verisi yok."
        else -> "İç ortam sensöründen güncel değer alınamadı."
    }
    return HexonReply(text = text, navigateTo = HexonDestination.Climate, ctaLabel = "Sensör’e git")
}

private fun miniFeatureReply(ctx: HexonLiveContext, q: String): HexonReply {
    val model = ctx.profile.displayName
    val kind = ctx.profile.kind
    val caps = ctx.profile.caps
    val featureHint = when {
        q.contains("su") -> "su tankı kapasitesi bu profilde ${caps.water} tank"
        q.contains("motor") -> "motor / IO kapsamı modele göre değişir"
        q.contains("dimmer") || q.contains("aydinlat") -> "dimmer sayısı bu profilde ${caps.dimmers}"
        q.contains("role") || q.contains("buton") -> "röle sayısı bu profilde ${caps.relays}"
        else -> "röle ${caps.relays}, dimmer ${caps.dimmers}, su tankı ${caps.water}"
    }
    val avail = when (kind) {
        VangoIoProfileKind.Mini -> "VanGO Mini kurulumunda bazı özellikler sınırlı ($featureHint)."
        VangoIoProfileKind.Medium -> "VanGO Medium kurulumunda $featureHint mevcut."
        VangoIoProfileKind.Unknown -> "Bağlı ürünün modeli net değil; kurulum $featureHint şeklinde görünüyor."
    }
    return HexonReply(
        text = "Bağlı ürünün modeli: $model. $avail Yanıt cihaz kimliğine ve kurulumuna göre verilir.",
        navigateTo = HexonDestination.SettingsAbout,
        ctaLabel = "Hakkında’ya git",
    )
}

private fun alertsReply(ctx: HexonLiveContext): HexonReply {
    if (!ctx.linked) {
        return HexonReply(
            text = "Hata / uyarı listesini görmek için önce bağlantı kurmalıyız.",
            navigateTo = HexonDestination.SettingsConnection,
            ctaLabel = "Bağlantı kur",
        )
    }
    val n = ctx.alertCount
    val text = if (n <= 0) {
        "Sistemin bildirdiği aktif bir hata görünmüyor. Bu, bütün donanımın sorunsuz olduğu anlamına gelmez."
    } else {
        "Şu anda bildirilen yaklaşık $n uyarı / olay var. Detay için Bildirimler’e bakalım."
    }
    return HexonReply(
        text = text,
        navigateTo = HexonDestination.NotificationsInbox,
        ctaLabel = "Bildirimlere git",
    )
}

private fun normalize(s: String): String =
    s.lowercase()
        .replace('ı', 'i')
        .replace('İ', 'i')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ö', 'o')
        .replace('ç', 'c')
        .trim()

private fun match(q: String, vararg keys: String): Unit? {
    val hit = keys.any { key -> q.contains(normalize(key)) }
    return if (hit) Unit else null
}
