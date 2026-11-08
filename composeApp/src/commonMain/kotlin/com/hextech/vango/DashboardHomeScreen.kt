package com.hextech.vango

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.hextech.vango.profile.rememberProfileViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.dashboard_brand_tagline
import testvango.composeapp.generated.resources.dashboard_conn_ble
import testvango.composeapp.generated.resources.dashboard_conn_offline
import testvango.composeapp.generated.resources.dashboard_conn_remote
import testvango.composeapp.generated.resources.dashboard_gas
import testvango.composeapp.generated.resources.dashboard_gas_not_measured
import testvango.composeapp.generated.resources.dashboard_instant_power
import testvango.composeapp.generated.resources.dashboard_power_not_measured
import testvango.composeapp.generated.resources.metric_no_data
import testvango.composeapp.generated.resources.dashboard_month_1
import testvango.composeapp.generated.resources.dashboard_month_2
import testvango.composeapp.generated.resources.dashboard_month_3
import testvango.composeapp.generated.resources.dashboard_month_4
import testvango.composeapp.generated.resources.dashboard_month_5
import testvango.composeapp.generated.resources.dashboard_month_6
import testvango.composeapp.generated.resources.dashboard_month_7
import testvango.composeapp.generated.resources.dashboard_month_8
import testvango.composeapp.generated.resources.dashboard_month_9
import testvango.composeapp.generated.resources.dashboard_month_10
import testvango.composeapp.generated.resources.dashboard_month_11
import testvango.composeapp.generated.resources.dashboard_month_12
import testvango.composeapp.generated.resources.dashboard_nav_climate
import testvango.composeapp.generated.resources.dashboard_nav_dim
import testvango.composeapp.generated.resources.settings_rd_panel_dimmer_label
import testvango.composeapp.generated.resources.settings_rd_panel_rgb_label
import testvango.composeapp.generated.resources.dashboard_nav_energy
import testvango.composeapp.generated.resources.dashboard_nav_home
import testvango.composeapp.generated.resources.dashboard_nav_motor
import testvango.composeapp.generated.resources.dashboard_nav_settings
import testvango.composeapp.generated.resources.dashboard_nav_water
import testvango.composeapp.generated.resources.dashboard_nav_water_short
import testvango.composeapp.generated.resources.dashboard_temp_humidity_zone
import testvango.composeapp.generated.resources.hexnet_mark
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val DashBg: Color @Composable get() = LocalAppChrome.current.bg
private val DashPanel: Color @Composable get() = LocalAppChrome.current.card
private val DashCard: Color @Composable get() = LocalAppChrome.current.tile
private val DashCyan = Color(0xFF2EC5FF)
private val DashCyanSoft = Color(0x332EC5FF)
private val DashTrack: Color @Composable get() = LocalAppChrome.current.track
private val DashText: Color @Composable get() = LocalAppChrome.current.text
private val DashMuted: Color @Composable get() = LocalAppChrome.current.muted
private val DashGreen = Color(0xFF3DDC84)
private val DashWater = Color(0xFF3B82F6)
private val DashGrayWater = Color(0xFF94A3B8)
private val DashDirty = Color(0xFFD97706)
private val DashCardBorder: Color @Composable get() = LocalAppChrome.current.border

private data class DashShellColors(
    val accent: Color,
    val bg: Color,
    val panel: Color,
)

@Composable
private fun rememberDashShellColors(): DashShellColors {
    val chrome = LocalAppChrome.current
    val accent = accentColorFromId(customAppAccentId)
    return DashShellColors(accent = accent, bg = chrome.bg, panel = chrome.panel)
}

private const val LIVE_MS = 90L

@Composable
private fun DashSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(DashPanel, RoundedCornerShape(16.dp))
            .border(1.dp, DashCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

enum class DashboardSection {
    Home, Dim, Water, Climate, Motor, Energy, Settings
}

/** Swipeable pages (Settings opens separately from footer). */
private val DashboardSwipeSections = listOf(
    DashboardSection.Home,
    DashboardSection.Dim,
    DashboardSection.Water,
    DashboardSection.Climate,
    DashboardSection.Motor,
    DashboardSection.Energy,
)

/** Soft pager settle — used for footer taps and fling snap. */
private val DashboardPageScrollSpec = tween<Float>(
    durationMillis = 480,
    easing = FastOutSlowInEasing,
)

@Composable
fun DashboardHomeScreen(
    onOpenSettings: () -> Unit = {},
) {
    val data by vanGoData.collectAsState()
    val env by bleEnvSensors.collectAsState()
    val scope = rememberCoroutineScope()
    var pageScrollJob by remember { mutableStateOf<Job?>(null) }
    var showSettings by remember { mutableStateOf(dashboardShowSettings) }
    var showNotificationsInbox by remember { mutableStateOf(dashboardShowNotificationsInbox) }
    var showCaravanPage by remember { mutableStateOf(dashboardShowCaravanPage) }
    var showHexon by remember { mutableStateOf(dashboardShowHexon) }
    var phoneDrawerOpen by remember { mutableStateOf(false) }
    var hexonFabOnHome by remember { mutableStateOf(HexonFabPreferences.isFabVisibleOnHome()) }
    val profileVm = rememberProfileViewModel()
    val profileState by profileVm.state.collectAsState()
    val appConnForGuest by appConnectionState.collectAsState()
    val guestWarnTick = GuestBleWarn.dismissedTick
    val bleLinkedForGuest = appConnForGuest == AppConnectionState.BLE_CONNECTED || isBleGattLinkReady()
    var wasBleLinked by remember { mutableStateOf(false) }
    LaunchedEffect(bleLinkedForGuest, profileState.isLoggedIn) {
        if (bleLinkedForGuest && !wasBleLinked) {
            if (!profileState.isLoggedIn) {
                GuestBleWarn.onGuestBleLinked()
            }
        } else if (!bleLinkedForGuest && wasBleLinked) {
            GuestBleWarn.onBleLinkLost()
        }
        wasBleLinked = bleLinkedForGuest
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { DashboardSwipeSections.size },
    )
    val section = DashboardSwipeSections[
        pagerState.currentPage.coerceIn(0, DashboardSwipeSections.lastIndex)
    ]
    val footerSelected = if (showSettings) DashboardSection.Settings else section

    fun softScrollToPage(idx: Int) {
        if (idx < 0 || idx == pagerState.currentPage && !pagerState.isScrollInProgress) return
        pageScrollJob?.cancel()
        pageScrollJob = scope.launch {
            pagerState.animateScrollToPage(
                page = idx,
                animationSpec = DashboardPageScrollSpec,
            )
        }
    }

    LaunchedEffect(showSettings) {
        dashboardShowSettings = showSettings
    }
    LaunchedEffect(dashboardShowSettings) {
        if (dashboardShowSettings != showSettings) {
            showSettings = dashboardShowSettings
        }
    }
    LaunchedEffect(showNotificationsInbox) {
        dashboardShowNotificationsInbox = showNotificationsInbox
    }
    LaunchedEffect(dashboardShowNotificationsInbox) {
        if (dashboardShowNotificationsInbox != showNotificationsInbox) {
            showNotificationsInbox = dashboardShowNotificationsInbox
        }
    }
    LaunchedEffect(showCaravanPage) {
        dashboardShowCaravanPage = showCaravanPage
    }
    LaunchedEffect(dashboardShowCaravanPage) {
        if (dashboardShowCaravanPage != showCaravanPage) {
            showCaravanPage = dashboardShowCaravanPage
        }
    }
    LaunchedEffect(showHexon) {
        dashboardShowHexon = showHexon
    }
    LaunchedEffect(dashboardShowHexon) {
        if (dashboardShowHexon != showHexon) {
            showHexon = dashboardShowHexon
        }
    }

    val shell = rememberDashShellColors()
    val phoneUi = isPhoneUi()
    val drawerSelected = when {
        showNotificationsInbox -> PhoneDrawerDestination.Notifications
        showSettings && dashboardSettingsNav == DashboardSettingsNav.About ->
            PhoneDrawerDestination.About
        showSettings -> PhoneDrawerDestination.Settings
        section == DashboardSection.Dim -> PhoneDrawerDestination.Dim
        section == DashboardSection.Water -> PhoneDrawerDestination.Water
        section == DashboardSection.Climate -> PhoneDrawerDestination.Climate
        section == DashboardSection.Motor -> PhoneDrawerDestination.Motor
        section == DashboardSection.Energy -> PhoneDrawerDestination.Energy
        else -> PhoneDrawerDestination.Home
    }

    fun openSettingsNav(nav: DashboardSettingsNav, detail: Boolean) {
        showNotificationsInbox = false
        dashboardShowNotificationsInbox = false
        showCaravanPage = false
        dashboardShowCaravanPage = false
        showHexon = false
        dashboardShowHexon = false
        dashboardSettingsNav = nav
        dashboardSettingsDetailOpen = detail
        showSettings = true
        dashboardShowSettings = true
        phoneDrawerOpen = false
        onOpenSettings()
    }

    fun openNotificationsInbox() {
        showSettings = false
        dashboardShowSettings = false
        dashboardSettingsDetailOpen = false
        showCaravanPage = false
        dashboardShowCaravanPage = false
        showHexon = false
        dashboardShowHexon = false
        showNotificationsInbox = true
        dashboardShowNotificationsInbox = true
        phoneDrawerOpen = false
    }

    fun openCaravanPage() {
        showSettings = false
        dashboardShowSettings = false
        dashboardSettingsDetailOpen = false
        showNotificationsInbox = false
        dashboardShowNotificationsInbox = false
        showHexon = false
        dashboardShowHexon = false
        showCaravanPage = true
        dashboardShowCaravanPage = true
        phoneDrawerOpen = false
    }

    fun openHexon() {
        showSettings = false
        dashboardShowSettings = false
        dashboardSettingsDetailOpen = false
        showNotificationsInbox = false
        dashboardShowNotificationsInbox = false
        showCaravanPage = false
        dashboardShowCaravanPage = false
        showHexon = true
        dashboardShowHexon = true
        phoneDrawerOpen = false
    }

    fun goSection(target: DashboardSection) {
        showSettings = false
        dashboardShowSettings = false
        dashboardSettingsDetailOpen = false
        showNotificationsInbox = false
        dashboardShowNotificationsInbox = false
        showCaravanPage = false
        dashboardShowCaravanPage = false
        showHexon = false
        dashboardShowHexon = false
        phoneDrawerOpen = false
        val idx = DashboardSwipeSections.indexOf(target)
        if (idx >= 0) softScrollToPage(idx)
    }

    LaunchedEffect(dashboardNavigateToSection) {
        val target = dashboardNavigateToSection ?: return@LaunchedEffect
        dashboardNavigateToSection = null
        goSection(target)
    }

    fun openDimPage(focus: PhoneDimPageFocus) {
        showSettings = false
        dashboardShowSettings = false
        dashboardSettingsDetailOpen = false
        showNotificationsInbox = false
        dashboardShowNotificationsInbox = false
        showCaravanPage = false
        dashboardShowCaravanPage = false
        showHexon = false
        dashboardShowHexon = false
        phoneDrawerOpen = false
        dashboardDimPageFocus = focus
        softScrollToPage(DashboardSwipeSections.indexOf(DashboardSection.Dim))
    }

    val canHandleBack =
        phoneDrawerOpen ||
            showHexon ||
            showNotificationsInbox ||
            showCaravanPage ||
            showSettings ||
            section != DashboardSection.Home

    PlatformBackHandler(enabled = canHandleBack) {
        when {
            phoneDrawerOpen -> phoneDrawerOpen = false
            showHexon -> {
                showHexon = false
                dashboardShowHexon = false
            }
            showCaravanPage -> {
                showCaravanPage = false
                dashboardShowCaravanPage = false
            }
            showNotificationsInbox -> {
                showNotificationsInbox = false
                dashboardShowNotificationsInbox = false
            }
            showSettings && phoneUi && dashboardSettingsDetailOpen -> {
                dashboardSettingsDetailOpen = false
            }
            showSettings -> {
                showSettings = false
                dashboardShowSettings = false
                dashboardSettingsDetailOpen = false
            }
            section != DashboardSection.Home -> goSection(DashboardSection.Home)
        }
    }

    ProvideAppLayout {
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shell.bg)
    ) {
        if (!showNotificationsInbox && !showCaravanPage && !showHexon) {
            if (isPhoneUi()) {
                PhoneAppTopBar(
                    accent = shell.accent,
                    hasAlerts = remember(data, guestWarnTick, bleLinkedForGuest, profileState.isLoggedIn) {
                        countActiveInboxAlerts(data) > 0
                    },
                    onMenu = { phoneDrawerOpen = true },
                    onNotifications = { openNotificationsInbox() },
                )
            } else {
                DashboardHeaderBar(
                    data = data,
                    shell = shell,
                )
            }
        }
        if (showHexon) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                HexonAssistantScreen(
                    onBack = {
                        showHexon = false
                        dashboardShowHexon = false
                    },
                    onNavigate = { dest ->
                        showHexon = false
                        dashboardShowHexon = false
                        navigateHexonDestination(dest)
                        // Re-apply local mirrors for overlays opened via global flags.
                        when (dest) {
                            HexonDestination.NotificationsInbox -> {
                                showNotificationsInbox = true
                            }
                            HexonDestination.CaravanType -> {
                                showCaravanPage = true
                            }
                            HexonDestination.SettingsList,
                            HexonDestination.SettingsPanel,
                            HexonDestination.SettingsConnection,
                            HexonDestination.SettingsTheme,
                            HexonDestination.SettingsLanguage,
                            HexonDestination.SettingsProfile,
                            HexonDestination.SettingsNotificationPrefs,
                            HexonDestination.SettingsAbout,
                            -> {
                                showSettings = true
                            }
                            else -> Unit
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else if (showCaravanPage) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                PhoneCaravanTypeScreen(
                    onBack = {
                        showCaravanPage = false
                        dashboardShowCaravanPage = false
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else if (showNotificationsInbox) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                NotificationsInboxScreen(
                    data = data,
                    onBack = {
                        showNotificationsInbox = false
                        dashboardShowNotificationsInbox = false
                    },
                    onGuestSignIn = { openSettingsNav(DashboardSettingsNav.Profile, detail = true) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else if (showSettings) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isPhoneUi()) 14.dp else 0.dp,
                        vertical = if (isPhoneUi()) 6.dp else 0.dp,
                    ),
            ) {
                SettingsRedesignScreen()
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isPhoneUi()) 14.dp else 10.dp,
                        vertical = 6.dp,
                    ),
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = DashboardPageScrollSpec,
                ),
            ) { page ->
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).absoluteValue
                // Skip tiny residual offsets (common after nested vertical scroll) to avoid scale jitter.
                val settledOffset = if (pageOffset < 0.01f) 0f else pageOffset
                val pageAlpha = (1f - settledOffset * 0.22f).coerceIn(0.78f, 1f)
                val pageScale = (1f - settledOffset * 0.04f).coerceIn(0.96f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = pageAlpha
                            scaleX = pageScale
                            scaleY = pageScale
                        },
                ) {
                when (DashboardSwipeSections[page]) {
                    DashboardSection.Dim -> PhoneDimScreen(
                        data = data,
                        focus = dashboardDimPageFocus,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DashboardSection.Water -> WaterSystemScreen(
                        data = data,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DashboardSection.Climate -> ClimateControlScreen(
                        data = data,
                        env = env,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DashboardSection.Motor -> MotorControlScreen(
                        data = data,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DashboardSection.Energy -> EnergyConsumptionScreen(
                        data = data,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> DashboardMainGrid(
                        data = data,
                        env = env,
                        focus = DashboardSection.Home,
                        onOpenWater = {
                            softScrollToPage(
                                DashboardSwipeSections.indexOf(DashboardSection.Water),
                            )
                        },
                        onOpenDim = { openDimPage(PhoneDimPageFocus.Dimmers) },
                        onOpenRelays = { openDimPage(PhoneDimPageFocus.Relays) },
                        onOpenCaravan = { openCaravanPage() },
                        onOpenHexon = { openHexon() },
                        showHexonFab = hexonFabOnHome,
                    )
                }
                }
            }
            }
        }
        if (!showHexon) {
            DashboardFooterBar(
                selected = footerSelected,
                data = data,
                env = env,
                onSelect = { next ->
                    if (next == DashboardSection.Settings) {
                        openSettingsNav(DashboardSettingsNav.Panel, detail = false)
                    } else {
                        goSection(next)
                    }
                },
            )
        }
    }
    if (phoneUi) {
        PhoneNavDrawerOverlay(
            open = phoneDrawerOpen,
            selected = drawerSelected,
            onClose = { phoneDrawerOpen = false },
            hexonFabVisible = hexonFabOnHome,
            onHexonFabVisibleChange = { visible ->
                hexonFabOnHome = visible
                HexonFabPreferences.setFabVisibleOnHome(visible)
            },
            onNavigate = { dest ->
                when (dest) {
                    PhoneDrawerDestination.Home -> goSection(DashboardSection.Home)
                    PhoneDrawerDestination.Dim -> {
                        dashboardDimPageFocus = PhoneDimPageFocus.Dimmers
                        goSection(DashboardSection.Dim)
                    }
                    PhoneDrawerDestination.Water -> goSection(DashboardSection.Water)
                    PhoneDrawerDestination.Climate -> goSection(DashboardSection.Climate)
                    PhoneDrawerDestination.Motor -> goSection(DashboardSection.Motor)
                    PhoneDrawerDestination.Energy -> goSection(DashboardSection.Energy)
                    PhoneDrawerDestination.Settings ->
                        openSettingsNav(DashboardSettingsNav.Panel, detail = false)
                    PhoneDrawerDestination.Notifications -> openNotificationsInbox()
                    PhoneDrawerDestination.About ->
                        openSettingsNav(DashboardSettingsNav.About, detail = true)
                    PhoneDrawerDestination.Logout -> {
                        phoneDrawerOpen = false
                        profileVm.signOut()
                    }
                }
            },
        )
    }
    }
    }
}

@Composable
private fun DashboardHeaderBar(
    data: VanGoData,
    shell: DashShellColors,
) {
    val monthNames = listOf(
        stringResource(Res.string.dashboard_month_1),
        stringResource(Res.string.dashboard_month_2),
        stringResource(Res.string.dashboard_month_3),
        stringResource(Res.string.dashboard_month_4),
        stringResource(Res.string.dashboard_month_5),
        stringResource(Res.string.dashboard_month_6),
        stringResource(Res.string.dashboard_month_7),
        stringResource(Res.string.dashboard_month_8),
        stringResource(Res.string.dashboard_month_9),
        stringResource(Res.string.dashboard_month_10),
        stringResource(Res.string.dashboard_month_11),
        stringResource(Res.string.dashboard_month_12),
    )
    var clock by remember { mutableStateOf("--:--") }
    var dateStr by remember { mutableStateOf("") }
    var wifiOk by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            clock = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
            val month = monthNames[(now.monthNumber - 1).coerceIn(0, 11)]
            dateStr = "${now.year} $month ${now.dayOfMonth.toString().padStart(2, '0')}"
            wifiOk = isWifiConnected()
            delay(2_000)
        }
    }
    val noData = stringResource(Res.string.metric_no_data)
    val voltText = if (data.volt > 0f) {
        val v = ((data.volt * 100).roundToInt() / 100.0)
        "$v V"
    } else {
        noData
    }
    val batPct = if (data.volt > 0f) {
        (((data.volt - 11.5f) / (13.0f - 11.5f)) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        0
    }
    val appConn by appConnectionState.collectAsState()
    val bleGlobal by vanGoGlobal.collectAsState()
    val lastTel by lastSuccessfulTelemetryEpochSec.collectAsState()
    var linkClock by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            linkClock++
        }
    }
    val bleOk = remember(bleGlobal, lastTel, linkClock) { isBleGattLinkReady() }
    val remoteOk = appConn == AppConnectionState.REMOTE_MQTT_CONNECTED || isAppRemoteMqttConnected()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(shell.panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Left: brand
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.36f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.hexnet_mark),
                contentDescription = "HEXNET",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Text(
                "VanGO",
                color = shell.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                stringResource(Res.string.dashboard_brand_tagline),
                color = DashText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        // Center: battery + clock
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            HeaderBattery(voltText = voltText, percent = batPct)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = shell.accent,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    "$clock  ${dateStr.ifBlank { "---- --- --" }}",
                    color = DashText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        // Right: connection icons
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HeaderLinkIcon(
                icon = Icons.Filled.Bluetooth,
                active = bleOk,
                activeTint = shell.accent,
                label = stringResource(Res.string.dashboard_conn_ble),
            )
            HeaderLinkIcon(
                icon = Icons.Filled.Wifi,
                active = wifiOk,
                activeTint = shell.accent,
                label = "WiFi",
            )
            HeaderLinkIcon(
                icon = Icons.Filled.Cloud,
                active = remoteOk,
                activeTint = DashGreen,
                label = stringResource(Res.string.dashboard_conn_remote),
            )
        }
    }
}

@Composable
private fun HeaderLinkIcon(
    icon: ImageVector,
    active: Boolean,
    activeTint: Color,
    label: String,
    size: androidx.compose.ui.unit.Dp = 28.dp,
) {
    Icon(
        icon,
        contentDescription = label,
        tint = if (active) activeTint else DashMuted.copy(alpha = 0.45f),
        modifier = Modifier.size(size),
    )
}

@Composable
private fun HeaderBattery(voltText: String, percent: Int, compact: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "header-bat-live")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "header-bat-pulse",
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "header-bat-shimmer",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
    ) {
    val trackCol = DashTrack
    Canvas(modifier = Modifier.size(if (compact) 44.dp else 62.dp, if (compact) 22.dp else 30.dp)) {
            val body = Size(size.width * 0.80f, size.height)
            drawRoundRect(
                color = DashGreen.copy(alpha = 0.30f + pulse * 0.35f),
                size = body,
                cornerRadius = CornerRadius(5f, 5f),
                style = Stroke(width = 2.4f),
            )
            val segs = 4
            val filled = ((percent / 100f) * segs).roundToInt().coerceIn(0, segs)
            val gap = 3f
            val segW = (body.width - 8f - gap * (segs - 1)) / segs
            for (i in 0 until segs) {
                val on = i < filled
                val base = if (on) DashGreen else trackCol
                val alpha = if (on) (0.70f + pulse * 0.30f) else 1f
                drawRoundRect(
                    color = base.copy(alpha = alpha),
                    topLeft = Offset(4f + i * (segW + gap), 5f),
                    size = Size(segW, body.height - 10f),
                    cornerRadius = CornerRadius(2f, 2f),
                )
                if (on) {
                    val shineX = 4f + i * (segW + gap) + segW * shimmer
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.18f * pulse),
                        topLeft = Offset(shineX.coerceIn(4f, 4f + i * (segW + gap) + segW * 0.6f), 5f),
                        size = Size(segW * 0.28f, body.height - 10f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                }
            }
            drawRoundRect(
                color = DashGreen.copy(alpha = 0.65f + pulse * 0.35f),
                topLeft = Offset(body.width, size.height * 0.28f),
                size = Size(size.width - body.width, size.height * 0.44f),
                cornerRadius = CornerRadius(2f, 2f),
            )
        }
        Text(
            "%$percent",
            color = DashText,
            fontSize = if (compact) 12.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (!compact) {
            Text(voltText, color = DashMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
@Composable
private fun DashboardMainGrid(
    data: VanGoData,
    env: BleEnvSensors,
    focus: DashboardSection,
    onOpenWater: () -> Unit = {},
    onOpenDim: () -> Unit = {},
    onOpenRelays: () -> Unit = {},
    onOpenCaravan: () -> Unit = {},
    onOpenHexon: () -> Unit = {},
    showHexonFab: Boolean = true,
) {
    if (isPhoneUi()) {
        PhoneHomeDashboard(
            data = data,
            modifier = Modifier.fillMaxSize(),
            onOpenWater = onOpenWater,
            onOpenDim = onOpenDim,
            onOpenRelays = onOpenRelays,
            onOpenCaravan = onOpenCaravan,
            onOpenHexon = onOpenHexon,
            showHexonFab = showHexonFab,
        )
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gridMaxWidth = maxWidth
        val landscape = gridMaxWidth > maxHeight * 0.95f
        // Shared row weights keep Dimmer/Controls bottoms and Water/Sensors bottoms aligned
        val topRowWeight = 2.35f
        val bottomRowWeight = 1f
        val leftColumnWidth = (gridMaxWidth.value * 0.36f).coerceIn(300f, 440f).dp
        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(leftColumnWidth)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(topRowWeight)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SectionTitle("DIMMERLER (8 ADET)", emphasize = focus == DashboardSection.Home)
                        DashSectionCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            DashboardDimmerList(
                                data = data,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                    DashSectionCard(
                        modifier = Modifier
                            .weight(bottomRowWeight)
                            .fillMaxWidth(),
                    ) {
                        DashboardWaterRow(
                            data = data,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DashSectionCard(
                        modifier = Modifier
                            .weight(topRowWeight)
                            .fillMaxWidth(),
                    ) {
                        SectionTitle(
                            "KONTROLLER",
                            emphasize = focus == DashboardSection.Home,
                        )
                        DashboardRelayGrid(
                            data = data,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                    DashSectionCard(
                        modifier = Modifier
                            .weight(bottomRowWeight)
                            .fillMaxWidth(),
                    ) {
                        SectionTitle(
                            "Sensorler",
                            emphasize = focus == DashboardSection.Climate,
                        )
                        DashboardSensorsRow(
                            data = data,
                            env = env,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashSectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("DIMMERLER (8 ADET)")
                    Spacer(Modifier.height(4.dp))
                    DashboardDimmerList(
                        data = data,
                        modifier = Modifier.fillMaxWidth().height(if (isCompactLayout()) 320.dp else 420.dp),
                    )
                }
                DashSectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("KONTROLLER")
                    DashboardRelayGrid(
                        data = data,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                when {
                                    isCompactLayout() -> 360.dp
                                    isTablet() -> 280.dp
                                    else -> 520.dp
                                },
                            ),
                    )
                }
                DashSectionCard(modifier = Modifier.fillMaxWidth()) {
                    DashboardWaterRow(
                        data = data,
                        modifier = Modifier.fillMaxWidth().height(if (isCompactLayout()) 150.dp else 180.dp),
                    )
                }
                DashSectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("Sensorler")
                    DashboardSensorsRow(
                        data = data,
                        env = env,
                        modifier = Modifier.fillMaxWidth().height(if (isCompactLayout()) 120.dp else 140.dp),
                    )
                }
            }
        }
    }
}
@Composable
private fun SectionTitle(text: String, emphasize: Boolean = true) {
    Text(
        text = text,
        color = if (emphasize) DashText else DashMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.0.sp,
        maxLines = 1,
    )
}

@Composable
private fun DashboardDimmerList(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
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
            (0 until 8).toList()
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        dimmerIds.forEachIndexed { index, id ->
            val enabledSlot = channels?.dimmers?.firstOrNull { it.id == id }?.enabled
                ?: (data.numOfD > id)
            DashboardDimmerRow(
                index = id,
                label = dimmerDisplayLabel(id, channels, data),
                enabled = enabledSlot,
                devicePercent = if (enabledSlot) {
                    (data.getSafeDimsData(id).toInt() and 0xFF).coerceIn(0, 100)
                } else {
                    0
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                showDivider = index < dimmerIds.lastIndex || rgbRows.isNotEmpty(),
            )
        }
        rgbRows.forEachIndexed { index, channel ->
            DashboardRgbHomeRow(
                data = data,
                label = rgbDisplayLabel(channel),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                showDivider = index < rgbRows.lastIndex,
            )
        }
    }
}

@Composable
internal fun dimmerDisplayLabel(
    index: Int,
    channels: PanelPlatformChannels?,
    data: VanGoData,
): String {
    val channel = channels?.dimmers?.firstOrNull { it.id == index }
    channel?.name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val typeId = data.dimNB.getOrElse(index) { 0 }.toInt() and 0xFF
    if (typeId != 0) {
        return runCatching {
            stringResource(getAppContext().vanGO.typeOfStringDim(typeId) as StringResource)
        }.getOrElse {
            stringResource(Res.string.settings_rd_panel_dimmer_label, index + 1)
        }
    }
    return stringResource(Res.string.settings_rd_panel_dimmer_label, index + 1)
}

@Composable
internal fun rgbDisplayLabel(channel: PanelPlatformChannel?): String {
    channel?.name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    return stringResource(Res.string.settings_rd_panel_rgb_label)
}

internal fun enabledPanelRgbChannels(channels: PanelPlatformChannels?): List<PanelPlatformChannel> {
    return channels?.rgb
        ?.filter { it.enabled }
        ?.sortedWith(compareBy<PanelPlatformChannel> { it.sortOrder }.thenBy { it.id })
        .orEmpty()
}

@Composable
internal fun DashboardRgbHomeRow(
    data: VanGoData,
    label: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    val (deviceR, deviceG, deviceB) = data.rgb888ForUi()
    val lightning = data.getSafeRGBData(3) > 0
    val swatch = if (lightning) {
        Color(
            red = deviceR.coerceIn(0, 255) / 255f,
            green = deviceG.coerceIn(0, 255) / 255f,
            blue = deviceB.coerceIn(0, 255) / 255f,
        )
    } else {
        DashTrack
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { showDialog = true }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(swatch, RoundedCornerShape(10.dp))
                    .border(1.dp, DashCardBorder, RoundedCornerShape(10.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = if (lightning) DashCyan else DashMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (lightning) "RGB" else "OFF",
                    color = if (lightning) DashText else DashMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(if (lightning) DashCyanSoft else DashTrack, CircleShape)
                    .clickable {
                        scope.launch {
                            runCatching {
                                val (r, g, b) = data.rgb888ForUi()
                                getAppContext().vanGO.writeRGBPort(r, g, b, if (lightning) 0 else 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    null,
                    tint = if (lightning) DashCyan else DashMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DashCardBorder),
            )
        }
    }

    if (showDialog) {
        ColorPaletteDialog(
            lightning = lightning,
            deviceR = deviceR,
            deviceG = deviceG,
            deviceB = deviceB,
            onColorChange = { _, _, _ -> },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
internal fun DashboardDimmerRow(
    index: Int,
    label: String,
    enabled: Boolean,
    devicePercent: Int,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    // Single source of truth for Home + Dim (pager keeps both pages composed).
    val lineOnMap = DimmerLineUiSync.lineOn
    val stashMap = DimmerLineUiSync.stash
    val levelMap = DimmerLineUiSync.level

    LaunchedEffect(index) {
        if (!lineOnMap.containsKey(index)) {
            lineOnMap[index] = devicePercent > 0
        }
        if (!levelMap.containsKey(index)) {
            val stashed = stashMap[index]
            levelMap[index] = when {
                lineOnMap[index] == false && stashed != null -> stashed
                else -> (devicePercent / 100f).coerceIn(0f, 1f)
            }
        }
    }

    val lineOn = lineOnMap[index] ?: (devicePercent > 0)
    val value = levelMap[index]
        ?: when {
            !lineOn && stashMap[index] != null -> stashMap[index]!!
            else -> (devicePercent / 100f).coerceIn(0f, 1f)
        }

    var dragging by remember { mutableStateOf(false) }
    var lastSent by remember { mutableStateOf<Int?>(null) }
    var lastAt by remember { mutableStateOf(0L) }
    var pending by remember { mutableStateOf<Job?>(null) }

    fun markUserOverride() {
        DimmerLineUiSync.ignoreDeviceUntil[index] =
            Clock.System.now().toEpochMilliseconds() + DimmerLineUiSync.DEVICE_GRACE_MS
    }

    // Device telemetry only — never fight a fresh user toggle (grace window).
    LaunchedEffect(devicePercent, index) {
        if (dragging) return@LaunchedEffect
        val now = Clock.System.now().toEpochMilliseconds()
        if (now < (DimmerLineUiSync.ignoreDeviceUntil[index] ?: 0L)) return@LaunchedEffect
        if (devicePercent > 0) {
            lineOnMap[index] = true
            stashMap.remove(index)
            levelMap[index] = (devicePercent / 100f).coerceIn(0f, 1f)
        } else {
            val wasOn = lineOnMap[index] == true
            if (wasOn) {
                val cur = (levelMap[index] ?: 0f).coerceIn(0f, 1f)
                if (cur > 0f) stashMap[index] = cur
            } else {
                stashMap[index]?.let { levelMap[index] = it }
            }
            lineOnMap[index] = false
        }
    }

    fun writePct(pct: Int) {
        scope.launch {
            runCatching { getAppContext().vanGO.writeDimmerPort(index, pct.coerceIn(0, 100)) }
        }
    }

    fun sendLive(pct: Int, force: Boolean) {
        if (!enabled || !lineOn) return
        val c = pct.coerceIn(0, 100)
        if (lastSent == c) return
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - lastAt
        if (!force && lastAt > 0 && elapsed < LIVE_MS) {
            pending?.cancel()
            pending = scope.launch {
                delay(LIVE_MS - elapsed)
                if (lastSent != c) {
                    lastSent = c
                    lastAt = Clock.System.now().toEpochMilliseconds()
                    writePct(c)
                }
            }
            return
        }
        pending?.cancel()
        pending = null
        lastSent = c
        lastAt = now
        writePct(c)
    }

    val shown = value
    val bulbBright = if (lineOn) shown.coerceIn(0f, 1f) else 0f
    // Dim = muted; bright = warm yellow glow matching light intensity
    val bulbTint = lerp(DashMuted, Color(0xFFFFF176), bulbBright)
    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.WbIncandescent,
                null,
                tint = bulbTint,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(if (lineOn) 0.28f + bulbBright * 0.72f else 0.28f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label,
                    color = if (lineOn) DashCyan else DashMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Slider(
                    value = shown,
                    onValueChange = {
                        if (!enabled) return@Slider
                        dragging = true
                        markUserOverride()
                        levelMap[index] = it
                        if (lineOn) sendLive((it * 100f).roundToInt(), false)
                    },
                    onValueChangeFinished = {
                        pending?.cancel()
                        pending = null
                        val end = (levelMap[index] ?: value).coerceIn(0f, 1f)
                        if (enabled && (lineOnMap[index] == true)) {
                            sendLive((end * 100f).roundToInt(), true)
                        }
                        dragging = false
                    },
                    enabled = enabled && lineOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .alpha(if (lineOn) 1f else 0.55f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DashCyan,
                        inactiveTrackColor = DashTrack,
                        disabledThumbColor = DashMuted,
                        disabledActiveTrackColor = DashCyan.copy(alpha = 0.35f),
                        disabledInactiveTrackColor = DashTrack,
                    ),
                )
            }
            Text(
                "%${(shown * 100f).roundToInt()}",
                color = if (lineOn) DashText else DashMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.End,
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(if (lineOn) DashCyanSoft else DashTrack, CircleShape)
                    .clickable(enabled = enabled) {
                        markUserOverride()
                        if (lineOn) {
                            // Keep bar at last level; only send 0 to device.
                            val cur = value.coerceIn(0f, 1f)
                            if (cur > 0f) stashMap[index] = cur
                            levelMap[index] = cur
                            lineOnMap[index] = false
                            pending?.cancel()
                            pending = null
                            lastSent = 0
                            writePct(0)
                        } else {
                            val restore = (stashMap[index] ?: value).coerceIn(0f, 1f)
                            stashMap.remove(index)
                            levelMap[index] = restore
                            lineOnMap[index] = true
                            lastSent = null
                            writePct((restore * 100f).roundToInt())
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    null,
                    tint = if (lineOn) DashCyan else DashMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DashCardBorder),
            )
        }
    }
}

/**
 * Home ↔ Dim power/level/stash senkronu.
 * Pager her iki sayfayı ayrı composition'da tuttuğu için state burada ortak.
 * Kullanıcı toggle sonrası kısa süre cihaz telemetrisini yoksayar (echo yarışı).
 */
internal object DimmerLineUiSync {
    const val DEVICE_GRACE_MS = 3500L
    val lineOn = mutableStateMapOf<Int, Boolean>()
    val stash = mutableStateMapOf<Int, Float>()
    val level = mutableStateMapOf<Int, Float>()
    val ignoreDeviceUntil = mutableMapOf<Int, Long>()
}

@Composable
private fun DashboardRelayGrid(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
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
    // Phone: 3 per row (portrait-friendly). Tablet: keep 6 per row.
    val columnsPerRow = if (isTablet()) 6 else 3
    val rows = orderedIds.chunked(columnsPerRow)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (isTablet()) 10.dp else 8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet()) 10.dp else 8.dp),
            ) {
                row.forEach { index ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DashboardRelayTile(index = index, data = data)
                    }
                }
                repeat((columnsPerRow - row.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun isRgbRelayIcon(icon: String?): Boolean {
    val key = icon?.trim()?.lowercase().orEmpty()
    return key == "rgb_aydinlatma" || key == "palette" || key.startsWith("rgb")
}

@Composable
internal fun DashboardRelayTile(index: Int, data: VanGoData) {
    val scope = rememberCoroutineScope()
    val channels by panelPlatformChannels.collectAsState()
    val channel = channels?.relays?.firstOrNull { it.id == index }
    val rgbTile = isRgbRelayIcon(channel?.icon)
    val typeId = when {
        channel != null -> PanelConfigMapper.iconToPreset(channel.icon)
        else -> data.outNB.getOrElse(index) { 0 }.toInt() and 0xFF
    }
    val deviceOn = if (rgbTile) {
        data.getSafeRGBData(3).toInt() != 0
    } else {
        data.outDB.getOrElse(index) { 0 }.toInt() != 0
    }
    /* Telemetri echo aninda eski OFF/ON gelince titreme — hold bitene kadar lokal tut. */
    var heldOn by remember(index) { mutableStateOf<Boolean?>(null) }
    var heldUntil by remember(index) { mutableStateOf(0L) }
    LaunchedEffect(deviceOn, heldOn, heldUntil) {
        val h = heldOn ?: return@LaunchedEffect
        val nowMs = Clock.System.now().toEpochMilliseconds()
        if (nowMs >= heldUntil || deviceOn == h) {
            heldOn = null
            heldUntil = 0L
        }
    }
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val holdActive = heldOn != null && nowMs < heldUntil && heldOn != deviceOn
    val on = when {
        holdActive -> heldOn!!
        else -> OutputUiHold.relayOverride(index) ?: deviceOn
    }
    val visible = channel?.enabled ?: (typeId != 0 || index < data.numOfO)
    val displayName = channel?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: if (typeId == 0) {
            "CH ${index + 1}"
        } else {
            runCatching {
                stringResource(getAppContext().vanGO.typeOfString(typeId) as StringResource)
            }.getOrElse { "CH ${index + 1}" }
        }
    val catalogIcon = channel?.icon?.let { PanelIconCatalog.find(it) }
    val legacyIconRes = if (catalogIcon == null) {
        runCatching {
            getAppContext().vanGO.typeOfIcon(if (typeId == 0) 1 else typeId) as DrawableResource
        }.getOrNull()
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (visible) 1f else 0.35f)
            .background(
                if (on) DashGreen.copy(alpha = 0.28f) else DashCard,
                RoundedCornerShape(14.dp),
            )
            .border(
                1.5.dp,
                if (on) DashGreen.copy(alpha = 0.85f) else DashCardBorder,
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = visible) {
                fun runToggle() {
                    val targetOn = !on
                    heldOn = targetOn
                    heldUntil = Clock.System.now().toEpochMilliseconds() + OutputUiHold.GRACE_MS
                    scope.launch {
                        try {
                            if (rgbTile) {
                                val (r, g, b) = data.rgb888ForUi()
                                println("Dashboard relay tap RGB idx=$index -> ${if (targetOn) "ON" else "OFF"}")
                                getAppContext().vanGO.writeRGBPort(r, g, b, if (targetOn) 1 else 0)
                            } else {
                                println("Dashboard relay tap DIGITAL idx=$index -> ${if (targetOn) "ON" else "OFF"}")
                                getAppContext().vanGO.writeDigitalPort(index, targetOn)
                            }
                        } finally {
                            vanGoGlobal.value.unlockOperation()
                        }
                    }
                }
                if (vanGoGlobal.value.lockOperation(Operations.WriteDataDevice)) {
                    runToggle()
                } else {
                    // Stale lock (weather/config) — force unlock once and retry
                    println("Dashboard relay lock busy idx=$index — forcing unlock")
                    vanGoGlobal.value.unlockOperation()
                    if (vanGoGlobal.value.lockOperation(Operations.WriteDataDevice)) {
                        runToggle()
                    } else {
                        println("Dashboard relay lock still busy idx=$index")
                    }
                }
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // More bottom weight → icon+label sit a bit higher and visually centered.
        Spacer(Modifier.weight(0.22f))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val phone = !isTablet()
            val iconSize = minOf(maxWidth.value * 0.84f, maxHeight.value * 0.86f)
                .coerceIn(if (phone) 48f else 64f, if (phone) 96f else 110f)
                .dp
            if (catalogIcon != null) {
                PanelCatalogIcon(
                    def = catalogIcon,
                    size = iconSize,
                    tint = if (on) catalogIcon.tint else catalogIcon.tint.copy(alpha = 0.88f),
                )
            } else if (legacyIconRes != null) {
                Image(
                    painter = painterResource(legacyIconRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(if (on) DashCyan else DashMuted),
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(if (on) 1f else 0.82f),
                )
            } else {
                Icon(
                    Icons.Filled.Bolt,
                    null,
                    tint = if (on) DashCyan else DashMuted,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Text(
            displayName,
            color = if (on) DashText else DashMuted,
            fontSize = if (isTablet()) 15.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = if (isTablet()) 16.sp else 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 2.dp),
        )
        Spacer(Modifier.weight(0.52f))
    }
}

@Composable
private fun DashboardWaterRow(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
    val channels by panelPlatformChannels.collectAsState()
    LaunchedEffect(Unit) { ensurePanelChannelsLoadedFromCache() }
    val linked = isBleGattLinkReady() || isAppRemoteMqttConnected()
    val tanks = listOf(
        Triple(0, "Temiz Su", DashWater),
        Triple(1, "Gri Su", DashGrayWater),
        Triple(2, "Kirli Su", DashDirty),
    ).filter { isWaterTankShownOnHome(it.first, channels) }
    if (tanks.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tanks.forEach { (id, title, fill) ->
            val pct = if (linked) data.getSafeSensorsDataUnsigned(id + 2).coerceIn(0, 100) else null
            val capacity = waterTankCapacityLiters(id, channels)
            DashboardTank(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = title,
                pct = pct,
                remainingLiters = pct?.let { waterRemainingLiters(it, capacity) } ?: 0,
                capacityLiters = capacity,
                enabled = pct != null,
                fill = fill,
            )
        }
    }
}

@Composable
private fun DashboardTank(
    modifier: Modifier,
    title: String,
    pct: Int?,
    remainingLiters: Int = 0,
    capacityLiters: Int = 0,
    enabled: Boolean,
    fill: Color,
) {
    val noData = stringResource(Res.string.metric_no_data)
    val p = (pct ?: 0).coerceIn(0, 100) / 100f
    val showPctLabel = pct?.let { "%${it.coerceIn(0, 100)}" } ?: noData
    var timeSec by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos).coerceIn(0L, 32_000_000L)) / 1_000_000_000f
                    timeSec += dt
                }
                lastNanos = now
            }
        }
    }
    val twoPi = (2f * PI).toFloat()
    val phase = timeSec * (twoPi / 2.2f)
    val bubblePhase = timeSec / 3.0f
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            title,
            color = fill.copy(alpha = 0.95f),
            fontSize = 12.sp,
            maxLines = 1,
            fontWeight = FontWeight.SemiBold,
            overflow = TextOverflow.Ellipsis,
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Mockup: upright rounded rectangle tank (not trapezoid)
            val tankW = minOf(maxWidth.value * 0.72f, maxHeight.value * 0.55f).coerceIn(46f, 90f).dp
            val tankH = minOf(maxHeight.value * 0.98f, tankW.value * 1.55f).coerceAtLeast(58f).dp
            val glass = LocalAppChrome.current.glass
            Canvas(modifier = Modifier.size(tankW, tankH)) {
                val w = size.width
                val h = size.height
                val stroke = 3f
                val inset = stroke + 1f
                // Taslak: üst hafif yuvarlak, alt köşeler dik
                val topR = CornerRadius(w * 0.16f, w * 0.16f)
                val bottomR = CornerRadius.Zero
                val body = RoundRect(
                    left = inset,
                    top = inset,
                    right = w - inset,
                    bottom = h - inset,
                    topLeftCornerRadius = topR,
                    topRightCornerRadius = topR,
                    bottomRightCornerRadius = bottomR,
                    bottomLeftCornerRadius = bottomR,
                )
                val bodyPath = Path().apply { addRoundRect(body) }

                // Empty tank glass
                drawPath(bodyPath, color = glass)

                val innerTop = inset + 2f
                val innerBottom = h - inset - 2f
                val fillH = (innerBottom - innerTop) * p.coerceIn(0.04f, 1f)
                val waterTopY = innerBottom - fillH

                if (enabled && fillH > 2f) {
                    clipPath(bodyPath) {
                        val waveAmp = (h * 0.035f).coerceIn(3.5f, 7.5f)
                        val water = Path().apply {
                            moveTo(0f, h)
                            lineTo(0f, waterTopY)
                            var x = 0f
                            while (x <= w + 2f) {
                                val y = waterTopY +
                                    sin((x / w) * 2.2f * PI.toFloat() + phase).toFloat() * waveAmp +
                                    sin((x / w) * 4.6f * PI.toFloat() - phase).toFloat() * (waveAmp * 0.4f)
                                lineTo(x, y)
                                x += 2.5f
                            }
                            lineTo(w + 2f, h)
                            close()
                        }
                        drawPath(
                            water,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    fill.copy(alpha = 0.50f),
                                    fill.copy(alpha = 0.88f),
                                    fill,
                                ),
                                startY = waterTopY - waveAmp,
                                endY = innerBottom,
                            ),
                        )
                        val highlight = Path().apply {
                            var x = inset
                            moveTo(
                                x,
                                waterTopY + sin((x / w) * 2.2f * PI.toFloat() + phase).toFloat() * waveAmp,
                            )
                            while (x <= w - inset) {
                                val y = waterTopY +
                                    sin((x / w) * 2.2f * PI.toFloat() + phase).toFloat() * waveAmp +
                                    sin((x / w) * 4.6f * PI.toFloat() - phase).toFloat() * (waveAmp * 0.4f)
                                lineTo(x, y)
                                x += 2.5f
                            }
                        }
                        drawPath(
                            highlight,
                            color = Color.White.copy(alpha = 0.55f),
                            style = Stroke(width = 2.2f, cap = StrokeCap.Round),
                        )
                        val bubbles = listOf(
                            Triple(0.30f, 0.25f, 2.4f),
                            Triple(0.52f, 0.55f, 3.2f),
                            Triple(0.70f, 0.35f, 2.2f),
                            Triple(0.42f, 0.78f, 1.8f),
                        )
                        bubbles.forEachIndexed { i, (xr, yr, r) ->
                            val rise = ((bubblePhase + i * 0.23f) % 1f)
                            val fade = sin(rise * PI).toFloat().coerceIn(0f, 1f)
                            val bx = w * xr + sin(phase + i).toFloat() * 2.5f
                            val by = innerBottom - fillH * (0.12f + yr * 0.7f) - rise * fillH * 0.5f
                            if (by in (waterTopY + 3f)..(innerBottom - 3f)) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.28f * fade),
                                    radius = r,
                                    center = Offset(bx, by),
                                )
                            }
                        }
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                            ),
                            topLeft = Offset(inset + 2f, inset + 2f),
                            size = Size(w * 0.22f, h - inset * 2f - 4f),
                        )
                    }
                }

                // Outline: üst yuvarlak, alt dik köşe
                val outline = RoundRect(
                    left = inset * 0.35f,
                    top = inset * 0.35f,
                    right = w - inset * 0.35f,
                    bottom = h - inset * 0.35f,
                    topLeftCornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                    topRightCornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                    bottomRightCornerRadius = CornerRadius.Zero,
                    bottomLeftCornerRadius = CornerRadius.Zero,
                )
                drawPath(
                    Path().apply { addRoundRect(outline) },
                    color = fill.copy(alpha = 0.95f),
                    style = Stroke(width = stroke),
                )
            }
        }
        Text(
            showPctLabel,
            color = DashText,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
        )
        if (pct != null && capacityLiters > 0) {
            Text(
                "$remainingLiters / $capacityLiters L",
                color = DashMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}
@Composable
private fun DashboardSensorsRow(
    data: VanGoData,
    env: BleEnvSensors,
    modifier: Modifier = Modifier,
) {
    val t1 = BleEnvSensorParser.resolveDisplayTempC(env, data)
    val h1 = BleEnvSensorParser.resolveDisplayHumidityPct(env, data)
    val zone1Has = t1 > 0 || h1 > 0
    val kw = stringResource(Res.string.dashboard_power_not_measured)
    val gasStatus = stringResource(Res.string.dashboard_gas_not_measured)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TempHumidityCard(
            Modifier.weight(1f).fillMaxHeight(),
            zone = 1,
            tempC = t1.coerceAtLeast(0),
            humidityPct = h1.coerceAtLeast(0),
            hasReading = zone1Has,
        )
        TempHumidityCard(
            Modifier.weight(1f).fillMaxHeight(),
            zone = 2,
            tempC = 0,
            humidityPct = 0,
            hasReading = false,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(DashCard, RoundedCornerShape(14.dp))
                .border(1.dp, DashCardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f),
                contentAlignment = Alignment.Center,
            ) {
                val iconH = (maxHeight.value * 0.72f).coerceIn(28f, 72f).dp
                Icon(
                    Icons.Filled.Whatshot,
                    null,
                    tint = DashMuted,
                    modifier = Modifier.size(iconH),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    stringResource(Res.string.dashboard_gas),
                    color = DashMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    gasStatus,
                    color = DashMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(DashCard, RoundedCornerShape(14.dp))
                .border(1.dp, DashCardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f),
                contentAlignment = Alignment.Center,
            ) {
                val iconH = (maxHeight.value * 0.62f).coerceIn(28f, 64f).dp
                Icon(
                    Icons.Filled.Bolt,
                    null,
                    tint = DashMuted,
                    modifier = Modifier.size(iconH * 0.72f),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    stringResource(Res.string.dashboard_instant_power),
                    color = DashMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    kw,
                    color = DashMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TempHumidityCard(
    modifier: Modifier,
    zone: Int,
    tempC: Int,
    humidityPct: Int,
    hasReading: Boolean = true,
) {
    val noData = stringResource(Res.string.metric_no_data)
    // Live color by temperature: cold → cool → mild → warm → hot
    val tempTint = when {
        !hasReading -> DashMuted
        tempC <= 5 -> Color(0xFF60A5FA)
        tempC <= 15 -> Color(0xFF38BDF8)
        tempC <= 22 -> DashCyan
        tempC <= 28 -> Color(0xFF4ADE80)
        tempC <= 34 -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }
    val transition = rememberInfiniteTransition(label = "temp-live-$zone")
    val glow by transition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "temp-glow-$zone",
    )
    Row(
        modifier = modifier
            .background(DashCard, RoundedCornerShape(14.dp))
            .border(1.dp, tempTint.copy(alpha = 0.20f + glow * 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.42f),
            contentAlignment = Alignment.Center,
        ) {
            val iconH = (maxHeight.value * 0.90f).coerceIn(44f, 100f).dp
            val iconW = (iconH.value * 0.42f).dp
            val mercury = if (hasReading) ((tempC - 0f) / 45f).coerceIn(0.12f, 1f) else 0.08f
            val strokeTint = tempTint.copy(alpha = 0.75f + glow * 0.25f)
            Canvas(modifier = Modifier.size(iconW, iconH)) {
                val w = size.width
                val h = size.height
                val tubeW = w * 0.22f
                val bulbR = w * 0.30f
                val cx = w / 2f
                val tubeLeft = cx - tubeW / 2f
                val tubeTop = h * 0.06f
                val bulbCy = h - bulbR - h * 0.04f
                val tubeBottom = bulbCy - bulbR * 0.42f
                val stroke = (w * 0.085f).coerceIn(1.6f, 2.8f)

                // Mercury level
                val fillTop = tubeBottom - (tubeBottom - tubeTop) * mercury
                drawRoundRect(
                    color = tempTint.copy(alpha = 0.85f),
                    topLeft = Offset(tubeLeft + stroke * 0.6f, fillTop),
                    size = Size(tubeW - stroke * 1.2f, tubeBottom - fillTop + 2f),
                    cornerRadius = CornerRadius(tubeW / 2f, tubeW / 2f),
                )
                drawCircle(
                    color = tempTint.copy(alpha = 0.90f),
                    radius = bulbR - stroke * 1.1f,
                    center = Offset(cx, bulbCy),
                )

                // Thin outline tube
                drawRoundRect(
                    color = strokeTint,
                    topLeft = Offset(tubeLeft, tubeTop),
                    size = Size(tubeW, tubeBottom - tubeTop + bulbR * 0.35f),
                    cornerRadius = CornerRadius(tubeW / 2f, tubeW / 2f),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = strokeTint,
                    radius = bulbR,
                    center = Offset(cx, bulbCy),
                    style = Stroke(width = stroke),
                )
                // Scale ticks (thin)
                for (i in 1..4) {
                    val ty = tubeTop + (tubeBottom - tubeTop) * (i / 5f)
                    drawLine(
                        color = strokeTint.copy(alpha = 0.55f),
                        start = Offset(tubeLeft + tubeW + 1.5f, ty),
                        end = Offset(tubeLeft + tubeW + w * 0.18f, ty),
                        strokeWidth = stroke * 0.7f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                stringResource(Res.string.dashboard_temp_humidity_zone, zone),
                color = DashMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (hasReading) "$tempC°C  %$humidityPct" else noData,
                color = DashText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
@Composable
fun DashboardFooterBar(
    selected: DashboardSection,
    data: VanGoData,
    env: BleEnvSensors,
    onSelect: (DashboardSection) -> Unit,
) {
    val shell = rememberDashShellColors()
    val climateIcon = rememberClimateNavIcon(data, env)
    val phoneUi = isPhoneUi()
    // Daily footer: Home / Dim / Water / Climate / Motor / Settings.
    // Energy stays in the side menu only (not everyday controls).
    val items = listOf(
        FooterNavSpec(DashboardSection.Home, stringResource(Res.string.dashboard_nav_home), Icons.Outlined.Home),
        FooterNavSpec(DashboardSection.Dim, stringResource(Res.string.dashboard_nav_dim), Icons.Filled.WbIncandescent),
        FooterNavSpec(DashboardSection.Water, stringResource(Res.string.dashboard_nav_water_short), Icons.Outlined.WaterDrop),
        FooterNavSpec(DashboardSection.Climate, stringResource(Res.string.dashboard_nav_climate), climateIcon),
        FooterNavSpec(DashboardSection.Motor, stringResource(Res.string.dashboard_nav_motor), MotorEngineIcon),
        FooterNavSpec(DashboardSection.Settings, stringResource(Res.string.dashboard_nav_settings), Icons.Outlined.Settings),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(shell.bg)
            .padding(horizontal = 6.dp, vertical = if (phoneUi) 4.dp else 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (phoneUi) 64.dp else 60.dp)
                .background(shell.panel, RoundedCornerShape(14.dp))
                .border(1.dp, DashCardBorder, RoundedCornerShape(14.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                FooterItem(
                    label = item.label,
                    icon = item.icon,
                    selected = selected == item.section,
                    circledIcon = item.circledIcon,
                    accent = shell.accent,
                    compact = true,
                    onClick = { onSelect(item.section) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                if (!phoneUi && index < items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.52f)
                            .background(DashCardBorder),
                    )
                }
            }
        }
    }
}

private data class FooterNavSpec(
    val section: DashboardSection,
    val label: String,
    val icon: ImageVector,
    val circledIcon: Boolean = false,
)

/**
 * Climate footer icon by season (fetched from device date):
 * - Summer → snowflake
 * - Winter → thermometer
 * - Spring / Autumn → by temperature when available, else snowflake
 * - Default → snowflake
 */
@Composable
private fun rememberClimateNavIcon(data: VanGoData, env: BleEnvSensors): ImageVector {
    var month by remember {
        mutableStateOf(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber,
        )
    }
    LaunchedEffect(Unit) {
        while (true) {
            month = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber
            delay(60_000)
        }
    }
    val tempC = BleEnvSensorParser.resolveDisplayTempC(env, data)
    return remember(month, tempC) {
        climateNavIconFor(month, tempC.takeIf { it > 0 })
    }
}

private fun climateNavIconFor(month: Int, tempC: Int?): ImageVector = when (month) {
    6, 7, 8 -> Icons.Filled.AcUnit // Yaz → kar
    12, 1, 2 -> Icons.Filled.Thermostat // Kış → termometre
    else -> when {
        // İlkbahar / Sonbahar — duruma göre; veri yoksa varsayılan kar
        tempC != null && tempC < 12 -> Icons.Filled.Thermostat
        tempC != null && tempC >= 22 -> Icons.Filled.AcUnit
        else -> Icons.Filled.AcUnit
    }
}

@Composable
private fun FooterItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circledIcon: Boolean = false,
    compact: Boolean = false,
) {
    val tint = if (selected) accent else LocalAppChrome.current.muted
    val iconSize = when {
        compact && selected -> 28.dp
        compact -> 26.dp
        selected -> 34.dp
        else -> 32.dp
    }
    val boltSize = if (compact) 14.dp else if (selected) 20.dp else 19.dp

    Box(
        modifier = modifier
            .padding(horizontal = if (compact) 1.dp else 3.dp, vertical = if (compact) 1.dp else 5.dp)
            .background(
                if (selected) accent.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 2.dp else 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (compact) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 4.dp, bottom = 3.dp),
            ) {
                if (circledIcon) {
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .border(1.4.dp, tint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(boltSize))
                    }
                } else {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    label,
                    color = tint,
                    fontSize = 9.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                if (circledIcon) {
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .border(1.6.dp, tint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(boltSize))
                    }
                } else {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    color = tint,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .height(2.5.dp)
                    .fillMaxWidth(0.72f)
                    .background(
                        if (selected) accent.copy(alpha = 0.85f) else Color.Transparent,
                        RoundedCornerShape(1.5.dp),
                    ),
            )
        }
    }
}
