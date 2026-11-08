package com.hextech.vango

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.ble_pair_connecting
import testvango.composeapp.generated.resources.dashboard_home_dimmers
import testvango.composeapp.generated.resources.dashboard_home_platform_down
import testvango.composeapp.generated.resources.dashboard_home_platform_ok_vehicle_down
import testvango.composeapp.generated.resources.dashboard_home_relays
import testvango.composeapp.generated.resources.dashboard_home_see_all
import testvango.composeapp.generated.resources.dashboard_home_system_connected
import testvango.composeapp.generated.resources.dashboard_home_system_offline
import testvango.composeapp.generated.resources.dashboard_home_water_tanks
import testvango.composeapp.generated.resources.metric_no_data
import testvango.composeapp.generated.resources.water_tank_clean
import testvango.composeapp.generated.resources.water_tank_dirty
import testvango.composeapp.generated.resources.water_tank_gray
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

private val PhoneHomeCard: Color @Composable get() = LocalAppChrome.current.card
private val PhoneHomeBorder: Color @Composable get() = LocalAppChrome.current.border
private val PhoneHomeText: Color @Composable get() = LocalAppChrome.current.text
private val PhoneHomeMuted: Color @Composable get() = LocalAppChrome.current.muted
private val PhoneHomeGreen = Color(0xFF3DDC84)
private val PhoneHomeWater = Color(0xFF3B82F6)
private val PhoneHomeGrayWater = Color(0xFF94A3B8)
private val PhoneHomeDirty = Color(0xFFD97706)
private val PhoneHomeAccent: Color @Composable get() = accentColorFromId(customAppAccentId)

/**
 * Phone-only home board matching the mobile mockup:
 * top bar + hero status + water tanks + dimmers + relay carousel.
 */
@Composable
fun PhoneHomeDashboard(
    data: VanGoData,
    modifier: Modifier = Modifier,
    onOpenWater: () -> Unit = {},
    onOpenDim: () -> Unit = {},
    onOpenRelays: () -> Unit = {},
    onOpenCaravan: () -> Unit = {},
    onOpenHexon: () -> Unit = {},
    showHexonFab: Boolean = true,
) {
    val green = PhoneHomeGreen
    val relayCount = rememberRelayCount(data)
    val link = rememberHeroLinkStatus()
    val linked = link.fullyLinked
    val scrollState = rememberScrollState()
    // Prevent leftover vertical scroll/fling from fighting the parent HorizontalPager.
    val nestVertical = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (abs(available.y) > abs(available.x)) Offset(0f, available.y) else Offset.Zero

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (abs(available.y) > abs(available.x)) Velocity(0f, available.y) else Velocity.Zero
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestVertical)
                .verticalScroll(scrollState)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PhoneHomeHeroCard(
                data = data,
                link = link,
                accent = green,
                onOpenCaravan = onOpenCaravan,
            )
            PhoneHomeSectionHeader(
                title = stringResource(Res.string.dashboard_home_dimmers),
                onSeeAll = onOpenDim,
            )
            PhoneHomeDimmerCard(data = data)
            PhoneHomeSectionHeader(
                title = stringResource(Res.string.dashboard_home_relays, relayCount),
                onSeeAll = onOpenRelays,
            )
            PhoneHomeRelayCarousel(data = data)
            val channels by panelPlatformChannels.collectAsState()
            LaunchedEffect(Unit) { ensurePanelChannelsLoadedFromCache() }
            val homeWaterTankIds = listOf(0, 1, 2).filter { isWaterTankShownOnHome(it, channels) }
            if (homeWaterTankIds.isNotEmpty()) {
                PhoneHomeSectionHeader(
                    title = stringResource(Res.string.dashboard_home_water_tanks),
                    onSeeAll = onOpenWater,
                )
                PhoneHomeWaterTanks(data = data, tankIds = homeWaterTankIds, linked = linked)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (showHexonFab) {
            HexonAskFab(
                onClick = onOpenHexon,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun rememberHeroLinkStatus(): HeroLinkStatus {
    var status by remember {
        mutableStateOf(HeroLinkStatus.snapshot())
    }
    LaunchedEffect(Unit) {
        while (true) {
            status = HeroLinkStatus.snapshot()
            delay(800)
        }
    }
    return status
}

private data class HeroLinkStatus(
    val remoteMode: Boolean,
    val bleOk: Boolean,
    val platformOk: Boolean,
    val vehicleOk: Boolean,
) {
    val fullyLinked: Boolean
        get() = if (remoteMode) platformOk && vehicleOk else bleOk

    companion object {
        fun snapshot(): HeroLinkStatus {
            val bleOk = isBleGattLinkReady()
            val remoteMode = remoteModeEnabled.value && !bleOk
            val platformOk = isAppRemoteMqttConnected()
            val vehicleOk = isRemoteVehicleReachable()
            return HeroLinkStatus(
                remoteMode = remoteMode,
                bleOk = bleOk,
                platformOk = platformOk,
                vehicleOk = vehicleOk,
            )
        }
    }
}

private val HeroErrorRed = Color(0xFFFF4D4D)

@Composable
private fun rememberRelayCount(data: VanGoData): Int {
    val channels by panelPlatformChannels.collectAsState()
    return remember(channels, data.numOfO, data.outNB.size) {
        val fromPlatform = channels?.relays?.count { it.enabled } ?: 0
        if (fromPlatform > 0) fromPlatform else min(12, maxOf(data.numOfO, data.outNB.size, 0))
    }
}

@Composable
private fun PhoneHomeHeroCard(
    data: VanGoData,
    link: HeroLinkStatus,
    accent: Color,
    onOpenCaravan: () -> Unit,
) {
    val global by vanGoGlobal.collectAsState()
    val weather by HeroWeatherRepository.snapshot.collectAsState()
    val weatherBlock by HeroWeatherRepository.blockReason.collectAsState()
    val weatherLoading by HeroWeatherRepository.loading.collectAsState()
    val scope = rememberCoroutineScope()
    var showLocationHelp by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val appResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val debugBuild = remember { isAppDebugBuild() }
    val simActive by HeroSceneSimulation.active.collectAsState()
    val simRunning by HeroSceneSimulation.running.collectAsState()
    val simMode by HeroSceneSimulation.mode.collectAsState()
    val simLocal by HeroSceneSimulation.local.collectAsState()
    val simWeatherForce by HeroSceneSimulation.weatherForce.collectAsState()
    var simSnap by remember { mutableStateOf(HeroSceneSimulation.snapshot()) }

    // Cache first → optional permission → refresh (no BLE required).
    // Skip network while simulation owns the scene clock.
    LaunchedEffect(simActive) {
        if (simActive) return@LaunchedEffect
        HeroWeatherRepository.hydrateFromDisk()
        if (HeroWeatherRepository.shouldAutoRequestLocation()) {
            HeroWeatherRepository.markLocationPrompted()
            requestLocationPermissions()
            delay(400L)
        }
        runCatching { HeroWeatherRepository.refreshIfNeeded(force = true) }
        while (true) {
            delay(HeroWeatherRefreshIntervalMs)
            if (HeroSceneSimulation.active.value) continue
            runCatching { HeroWeatherRepository.refreshIfNeeded() }
        }
    }
    // Retry when user returns from settings / permission dialog
    LaunchedEffect(lifecycleState, simActive) {
        if (simActive) return@LaunchedEffect
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            delay(300L)
            runCatching { HeroWeatherRepository.refreshIfNeeded(force = HeroWeatherRepository.current() == null) }
        }
    }

    // Single sim timer: 2 real seconds → +1 virtual hour; pause in background (no catch-up)
    LaunchedEffect(simActive, simRunning, appResumed) {
        if (!simActive || !simRunning || !appResumed) return@LaunchedEffect
        while (true) {
            delay(HeroSceneSimulation.StepRealMs)
            if (!HeroSceneSimulation.active.value ||
                !HeroSceneSimulation.running.value ||
                !appResumed
            ) {
                break
            }
            HeroSceneSimulation.tickOneHour()
        }
    }
    LaunchedEffect(simActive, simRunning, simMode, simLocal, simWeatherForce) {
        simSnap = HeroSceneSimulation.snapshot()
    }

    var clockLabel by remember { mutableStateOf(formatHeroClockLabel()) }
    var sky by remember {
        mutableStateOf(
            computeHeroSkySnapshot(
                weather = HeroWeatherRepository.current(),
                weatherStale = HeroWeatherRepository.isStale(HeroWeatherRepository.current()),
            ),
        )
    }
    // Season-jump ease when sim month changes (preview chips / next month / FastSeason wrap)
    var simSeasonEaseMs by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(simActive, simLocal, simWeatherForce) {
        if (simActive) {
            val snap = HeroSceneSimulation.snapshot()
            val month = snap.local.monthNumber
            val jumped = HeroSceneSimulation.lastEaseMonth >= 0 &&
                HeroSceneSimulation.lastEaseMonth != month
            HeroSceneSimulation.lastEaseMonth = month
            simSeasonEaseMs = if (jumped) HeroSceneSimulation.SeasonEaseMs else null
            clockLabel = snap.clockLabel
            sky = computeHeroSkySnapshot(
                nowInstant = HeroSceneSimulation.instant(),
                timeZone = HeroSceneSimulation.IstanbulTz,
                weather = snap.weather,
                weatherStale = false,
            )
            return@LaunchedEffect
        }
        simSeasonEaseMs = null
        HeroSceneSimulation.lastEaseMonth = -1
        // Live: refresh sky every second so light/season track continuous time
        // (no per-minute animation restart — values move with the clock)
        while (true) {
            clockLabel = formatHeroClockLabel()
            val info = HeroWeatherRepository.current()
            sky = computeHeroSkySnapshot(
                weather = info,
                weatherStale = HeroWeatherRepository.isStale(info),
            )
            delay(1_000L)
        }
    }
    LaunchedEffect(simActive) {
        if (simActive) return@LaunchedEffect
        while (true) {
            val info = HeroWeatherRepository.current()
            sky = computeHeroSkySnapshot(
                weather = info,
                weatherStale = HeroWeatherRepository.isStale(info),
            )
            delay(15_000L)
        }
    }
    LaunchedEffect(weather?.fetchedAtEpochMs, simActive) {
        if (simActive) return@LaunchedEffect
        val info = weather ?: HeroWeatherRepository.current()
        sky = computeHeroSkySnapshot(
            weather = info,
            weatherStale = HeroWeatherRepository.isStale(info),
        )
    }

    val badgeUi = remember(
        weather, weatherBlock, weatherLoading,
        sky.weatherLabel, sky.weatherBadgeIcon, sky.windy, sky.hasWeatherData, simActive, simLocal,
    ) {
        if (simActive) {
            HeroWeatherBadgeUi(
                primary = sky.weatherLabel ?: simSnap.scenarioLabel,
                windy = sky.windy,
                icon = sky.weatherBadgeIcon,
                action = HeroWeatherBadgeAction.None,
            )
        } else {
            resolveHeroWeatherBadgeUi(
                info = weather ?: HeroWeatherRepository.current(),
                block = weatherBlock,
                loading = weatherLoading,
                skyLabel = sky.weatherLabel,
                skyIcon = sky.weatherBadgeIcon,
                windy = sky.windy,
                hasData = sky.hasWeatherData,
            )
        }
    }

    val connecting = isBleHandshakeInProgress() ||
        global.connectionState == BleGattConnectionChain.ConnectingGattServer ||
        global.connectionState == BleGattConnectionChain.DiscoveringServices ||
        (link.remoteMode && !link.platformOk && remoteModeEnabled.value)
    val linkVisual = when {
        link.fullyLinked -> HeroLinkVisual.Connected
        connecting -> HeroLinkVisual.Connecting
        else -> HeroLinkVisual.Disconnected
    }
    val statusText = when (linkVisual) {
        HeroLinkVisual.Connected -> stringResource(Res.string.dashboard_home_system_connected)
        HeroLinkVisual.Connecting -> stringResource(Res.string.ble_pair_connecting)
        HeroLinkVisual.Disconnected -> when {
            link.remoteMode && link.platformOk && !link.vehicleOk ->
                stringResource(Res.string.dashboard_home_platform_ok_vehicle_down)
            link.remoteMode && !link.platformOk ->
                stringResource(Res.string.dashboard_home_platform_down)
            else -> stringResource(Res.string.dashboard_home_system_offline)
        }
    }
    @Suppress("UNUSED_VARIABLE")
    val unusedAccent = accent
    @Suppress("UNUSED_VARIABLE")
    val unusedData = data

    val caravanRevision = caravanTypeRevision
    val vehicleType = remember(caravanRevision) {
        heroVehicleTypeFromCaravan(resolvedCaravanType())
    }

    if (showLocationHelp) {
        AlertDialog(
            onDismissRequest = { showLocationHelp = false },
            title = { Text("Konum gerekli", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Dış hava durumu için yaklaşık konum kullanılır. " +
                        "Konumu açabilir veya şehir seçebilirsiniz. " +
                        "Karavan bağlantısı gerekmez.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationHelp = false
                        if (!isLocationServicesEnabled()) {
                            openLocationSettings()
                        } else if (!hasLocationPermissions()) {
                            requestLocationPermissions()
                        } else {
                            openAppSettings()
                        }
                        scope.launch {
                            delay(600L)
                            HeroWeatherRepository.refreshIfNeeded(force = true)
                        }
                    },
                ) { Text("Konumu etkinleştir") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationHelp = false
                        showCityPicker = true
                    },
                ) { Text("Şehir seç") }
            },
        )
    }
    if (showCityPicker) {
        AlertDialog(
            onDismissRequest = { showCityPicker = false },
            title = { Text("Şehir seç", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(HeroWeatherCityChoices) { city ->
                        Text(
                            text = city.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCityPicker = false
                                    scope.launch {
                                        HeroWeatherRepository.selectCity(city)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            fontSize = 16.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityPicker = false }) { Text("Kapat") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PhoneHomeHeroSceneCard(
            clockLabel = clockLabel,
            temperatureLabel = sky.temperatureLabel,
            weatherLabel = badgeUi.primary,
            statusLabel = statusText,
            linkVisual = linkVisual,
            sky = sky,
            isLightTheme = LocalAppChrome.current.isLight,
            vehicleType = vehicleType,
            connectionKind = if (link.remoteMode) HeroConnectionKind.Remote else HeroConnectionKind.Ble,
            onClick = onOpenCaravan,
            weatherBadge = badgeUi,
            onWeatherBadgeClick = {
                if (!simActive) {
                    when (badgeUi.action) {
                        HeroWeatherBadgeAction.RequestLocation -> showLocationHelp = true
                        HeroWeatherBadgeAction.PickCity -> showCityPicker = true
                        HeroWeatherBadgeAction.None -> Unit
                    }
                }
            },
            sceneEaseOverrideMs = if (simActive) HeroSceneSimulation.SceneEaseMs else null,
            sceneSeasonEaseOverrideMs = if (simActive) {
                simSeasonEaseMs ?: HeroSceneSimulation.SceneEaseMs
            } else {
                null
            },
        )
        if (debugBuild) {
            HeroSceneSimControls(
                snap = simSnap,
                onToggleRun = { HeroSceneSimulation.toggleRun() },
                onReset = { HeroSceneSimulation.reset() },
                onPrevMonth = { HeroSceneSimulation.prevMonth() },
                onNextMonth = { HeroSceneSimulation.nextMonth() },
                onToggleMode = { HeroSceneSimulation.toggleMode() },
                onSpringPreview = { HeroSceneSimulation.jumpToSpringPreview() },
                onSummerPreview = { HeroSceneSimulation.jumpToSummerPreview() },
                onAutumnPreview = { HeroSceneSimulation.jumpToAutumnPreview() },
                onWinterPreview = { HeroSceneSimulation.jumpToWinterPreview() },
                onWeatherForce = { HeroSceneSimulation.setWeatherForce(it) },
                onStop = {
                    HeroSceneSimulation.stop()
                    // Resume live sky from repository (no cache write from sim)
                    val info = HeroWeatherRepository.current()
                    sky = computeHeroSkySnapshot(
                        weather = info,
                        weatherStale = HeroWeatherRepository.isStale(info),
                    )
                    clockLabel = formatHeroClockLabel()
                    scope.launch {
                        runCatching { HeroWeatherRepository.refreshIfNeeded(force = info == null) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

private fun resolveHeroWeatherBadgeUi(
    info: WeatherInfo?,
    block: HeroWeatherBlockReason,
    loading: Boolean,
    skyLabel: String?,
    skyIcon: HeroWeatherBadgeIcon,
    windy: Boolean,
    hasData: Boolean,
): HeroWeatherBadgeUi {
    if (info != null && hasData) {
        return HeroWeatherBadgeUi(
            primary = skyLabel ?: "Hava bilgisi yok",
            windy = windy,
            icon = skyIcon,
            action = HeroWeatherBadgeAction.None,
        )
    }
    if (loading || block == HeroWeatherBlockReason.Loading) {
        return HeroWeatherBadgeUi(
            primary = "Hava yükleniyor…",
            windy = false,
            icon = HeroWeatherBadgeIcon.Loading,
            action = HeroWeatherBadgeAction.None,
        )
    }
    return when (block) {
        HeroWeatherBlockReason.NeedsLocationPermission,
        HeroWeatherBlockReason.LocationServicesOff,
        HeroWeatherBlockReason.NoCoordinates,
        -> HeroWeatherBadgeUi(
            primary = "Konum gerekli",
            windy = false,
            icon = HeroWeatherBadgeIcon.NeedsLocation,
            action = HeroWeatherBadgeAction.RequestLocation,
        )
        HeroWeatherBlockReason.NoInternet -> HeroWeatherBadgeUi(
            primary = "Hava bilgisi yok",
            windy = false,
            icon = HeroWeatherBadgeIcon.Unknown,
            action = HeroWeatherBadgeAction.None,
        )
        HeroWeatherBlockReason.ApiError -> HeroWeatherBadgeUi(
            primary = "Hava bilgisi yok",
            windy = false,
            icon = HeroWeatherBadgeIcon.Unknown,
            action = HeroWeatherBadgeAction.PickCity,
        )
        else -> HeroWeatherBadgeUi(
            primary = "Hava bilgisi yok",
            windy = false,
            icon = HeroWeatherBadgeIcon.Unknown,
            action = HeroWeatherBadgeAction.RequestLocation,
        )
    }
}

@Composable
private fun PhoneHomeSectionHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            color = PhoneHomeText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        if (onSeeAll != null) {
            Text(
                stringResource(Res.string.dashboard_home_see_all),
                color = PhoneHomeAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
        }
    }
}

@Composable
private fun PhoneHomeWaterTanks(data: VanGoData, tankIds: List<Int>, linked: Boolean) {
    val channels by panelPlatformChannels.collectAsState()
    val noData = stringResource(Res.string.metric_no_data)
    val titles = mapOf(
        0 to stringResource(Res.string.water_tank_clean),
        1 to stringResource(Res.string.water_tank_gray),
        2 to stringResource(Res.string.water_tank_dirty),
    )
    val colors = mapOf(
        0 to PhoneHomeWater,
        1 to PhoneHomeGrayWater,
        2 to PhoneHomeDirty,
    )
    if (tankIds.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tankIds.forEach { id ->
            val pct = if (linked) data.getSafeSensorsDataUnsigned(id + 2).coerceIn(0, 100) else null
            val capacity = waterTankCapacityLiters(id, channels)
            PhoneHomeTankCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = titles[id].orEmpty(),
                pctLabel = pct?.let { "%$it" } ?: noData,
                pctFill = pct ?: 0,
                remainingLiters = pct?.let { waterRemainingLiters(it, capacity) } ?: 0,
                capacityLiters = if (pct != null) capacity else 0,
                fill = colors[id] ?: PhoneHomeWater,
            )
        }
    }
}

@Composable
private fun PhoneHomeTankCard(
    modifier: Modifier,
    title: String,
    pctLabel: String,
    pctFill: Int,
    remainingLiters: Int,
    capacityLiters: Int,
    fill: Color,
) {
    val wave = rememberInfiniteTransition(label = "phone-tank-wave")
    val phase by wave.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phone-tank-phase",
    )
    val level = (pctFill / 100f).coerceIn(0.08f, 1f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PhoneHomeCard)
            .border(1.dp, PhoneHomeBorder, RoundedCornerShape(16.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val waterTop = h * (1f - level)
            val wavePath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, waterTop)
                var x = 0f
                while (x <= w + 2f) {
                    val y = waterTop +
                        sin((x / w) * 2.4f * PI.toFloat() + phase).toFloat() * 5f +
                        sin((x / w) * 5f * PI.toFloat() - phase).toFloat() * 2.5f
                    lineTo(x, y)
                    x += 3f
                }
                lineTo(w, h)
                close()
            }
            drawPath(wavePath, color = fill.copy(alpha = 0.55f))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title.uppercase(),
                color = PhoneHomeMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Icon(Icons.Filled.WaterDrop, null, tint = fill, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                pctLabel,
                color = PhoneHomeText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            if (capacityLiters > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "$remainingLiters / $capacityLiters L",
                    color = PhoneHomeMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PhoneHomeDimmerCard(data: VanGoData) {
    LaunchedEffect(Unit) { ensurePanelChannelsLoadedFromCache() }
    val channels by panelPlatformChannels.collectAsState()
    val dimmerIds = remember(channels, data.numOfD) {
        val fromPanel = channels?.dimmers
            ?.filter { it.enabled }
            ?.sortedWith(compareBy<PanelPlatformChannel> { it.sortOrder }.thenBy { it.id })
            ?.map { it.id }
            .orEmpty()
        if (fromPanel.isNotEmpty()) {
            fromPanel.take(8)
        } else {
            val show = if (data.numOfD > 0) min(8, data.numOfD) else 0
            (0 until show).toList()
        }
    }
    val pages = remember(dimmerIds) { dimmerIds.chunked(4).ifEmpty { listOf(emptyList()) } }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val rgbRows = remember(channels, data.RGBE) {
        val enabled = enabledPanelRgbChannels(channels)
        when {
            enabled.isNotEmpty() -> enabled
            channels == null && data.RGBE.equals("yes", ignoreCase = true) -> {
                listOf(
                    PanelPlatformChannel(
                        id = 0,
                        enabled = true,
                        name = "RGB",
                        icon = "rgb_aydinlatma",
                    ),
                )
            }
            else -> emptyList()
        }
    }
    val pageHeight = (min(4, pages.maxOfOrNull { it.size } ?: 4).coerceAtLeast(1) * 52).dp + 8.dp

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeight),
            contentPadding = if (pages.size > 1) PaddingValues(end = 28.dp) else PaddingValues(0.dp),
            pageSpacing = 10.dp,
        ) { page ->
            val pageIds = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PhoneHomeCard, RoundedCornerShape(16.dp))
                    .border(1.dp, PhoneHomeBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                pageIds.forEachIndexed { index, id ->
                    val enabled = channels?.dimmers?.firstOrNull { it.id == id }?.enabled
                        ?: (data.numOfD > id)
                    DashboardDimmerRow(
                        index = id,
                        label = dimmerDisplayLabel(id, channels, data),
                        enabled = enabled,
                        devicePercent = if (enabled) {
                            (data.getSafeDimsData(id).toInt() and 0xFF).coerceIn(0, 100)
                        } else {
                            0
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        showDivider = index < pageIds.lastIndex,
                    )
                }
            }
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .background(
                                if (selected) PhoneHomeAccent else PhoneHomeMuted.copy(alpha = 0.35f),
                                CircleShape,
                            ),
                    )
                }
            }
        }
        if (rgbRows.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PhoneHomeCard, RoundedCornerShape(16.dp))
                    .border(1.dp, PhoneHomeBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                rgbRows.forEachIndexed { index, channel ->
                    DashboardRgbHomeRow(
                        data = data,
                        label = rgbDisplayLabel(channel),
                        modifier = Modifier.fillMaxWidth(),
                        showDivider = index < rgbRows.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneHomeRelayCarousel(data: VanGoData) {
    LaunchedEffect(Unit) { ensurePanelChannelsLoadedFromCache() }
    val channels by panelPlatformChannels.collectAsState()
    val orderedIds = remember(channels, data.outNB.size, data.numOfO) {
        val fromPlatform = channels?.relays
            ?.filter { it.enabled }
            ?.sortedWith(compareBy<PanelPlatformChannel> { it.sortOrder }.thenBy { it.id })
            ?.map { it.id }
            .orEmpty()
        if (fromPlatform.isNotEmpty()) {
            fromPlatform.take(12)
        } else {
            (0 until min(12, maxOf(data.outNB.size, data.numOfO, 12))).toList()
        }
    }
    // Home relays: fixed 3 columns x up to 4 rows (no pager).
    val rows = orderedIds.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { id ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DashboardRelayTile(index = id, data = data)
                    }
                }
                repeat((3 - row.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Full dimmer or relay list for phone (footer Dim / home “Tümü >”).
 */
@Composable
fun PhoneDimScreen(
    data: VanGoData,
    modifier: Modifier = Modifier,
    focus: PhoneDimPageFocus = dashboardDimPageFocus,
) {
    val relayCount = rememberRelayCount(data)
    val scrollState = rememberScrollState()
    val nestVertical = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (abs(available.y) > abs(available.x)) {
                Offset(0f, available.y)
            } else {
                Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (abs(available.y) > abs(available.x)) {
                    Velocity(0f, available.y)
                } else {
                    Velocity.Zero
                }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestVertical)
                .verticalScroll(scrollState)
                .padding(vertical = if (isPhoneUi()) 0.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        if (focus == PhoneDimPageFocus.Dimmers) {
            PhoneHomeSectionHeader(
                title = stringResource(Res.string.dashboard_home_dimmers),
                onSeeAll = null,
            )
            LaunchedEffect(Unit) { ensurePanelChannelsLoadedFromCache() }
            val channels by panelPlatformChannels.collectAsState()
            val dimmerIds = remember(channels, data.numOfD) {
                val fromPanel = channels?.dimmers
                    ?.filter { it.enabled }
                    ?.sortedWith(compareBy<PanelPlatformChannel> { it.sortOrder }.thenBy { it.id })
                    ?.map { it.id }
                    .orEmpty()
                if (fromPanel.isNotEmpty()) {
                    fromPanel.take(8)
                } else {
                    val dimCount = if (data.numOfD > 0) min(8, data.numOfD) else 8
                    (0 until dimCount).toList()
                }
            }
            val rgbRows = remember(channels, data.RGBE) {
                val enabled = enabledPanelRgbChannels(channels)
                when {
                    enabled.isNotEmpty() -> enabled
                    channels == null && data.RGBE.equals("yes", ignoreCase = true) -> {
                        listOf(
                            PanelPlatformChannel(
                                id = 0,
                                enabled = true,
                                name = "RGB",
                                icon = "rgb_aydinlatma",
                            ),
                        )
                    }
                    else -> emptyList()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PhoneHomeCard, RoundedCornerShape(16.dp))
                    .border(1.dp, PhoneHomeBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                dimmerIds.forEachIndexed { index, id ->
                    val enabled = channels?.dimmers?.firstOrNull { it.id == id }?.enabled
                        ?: (data.numOfD > id)
                    DashboardDimmerRow(
                        index = id,
                        label = dimmerDisplayLabel(id, channels, data),
                        enabled = enabled,
                        devicePercent = if (enabled) {
                            (data.getSafeDimsData(id).toInt() and 0xFF).coerceIn(0, 100)
                        } else {
                            0
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        showDivider = index < dimmerIds.lastIndex || rgbRows.isNotEmpty(),
                    )
                }
                rgbRows.forEachIndexed { index, channel ->
                    DashboardRgbHomeRow(
                        data = data,
                        label = rgbDisplayLabel(channel),
                        modifier = Modifier.fillMaxWidth(),
                        showDivider = index < rgbRows.lastIndex,
                    )
                }
            }
        } else {
            PhoneHomeSectionHeader(
                title = stringResource(Res.string.dashboard_home_relays, relayCount),
                onSeeAll = null,
            )
            PhoneDimRelayGrid(data = data)
        }
        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PhoneDimRelayGrid(data: VanGoData) {
    LaunchedEffect(Unit) { ensurePanelChannelsLoadedFromCache() }
    val channels by panelPlatformChannels.collectAsState()
    val orderedIds = remember(channels, data.outNB.size, data.numOfO) {
        val fromPlatform = channels?.relays
            ?.filter { it.enabled }
            ?.sortedWith(compareBy<PanelPlatformChannel> { it.sortOrder }.thenBy { it.id })
            ?.map { it.id }
            .orEmpty()
        if (fromPlatform.isNotEmpty()) {
            fromPlatform.take(12)
        } else {
            (0 until min(12, maxOf(data.outNB.size, data.numOfO, 12))).toList()
        }
    }
    val rows = orderedIds.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { id ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DashboardRelayTile(index = id, data = data)
                    }
                }
                repeat((3 - row.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatHeroClockLabel(): String {
    val local = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return local.hour.toString().padStart(2, '0') +
        ":" +
        local.minute.toString().padStart(2, '0')
}
