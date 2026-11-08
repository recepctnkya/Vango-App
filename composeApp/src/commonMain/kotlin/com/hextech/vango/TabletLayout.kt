package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.footer_cancel
import testvango.composeapp.generated.resources.footer_home
import testvango.composeapp.generated.resources.footer_save
import testvango.composeapp.generated.resources.footer_menu
import testvango.composeapp.generated.resources.footer_profile
import testvango.composeapp.generated.resources.panel_sensor_humidity
import testvango.composeapp.generated.resources.panel_sensor_in_temp

/**
 * Single VanGO app entry. [DashboardHomeScreen] picks phone or tablet UI via [isPhoneUi].
 */
@Composable
fun VanGoAppShell() {
    DashboardHomeScreen()
}

/** @see VanGoAppShell */
@Composable
fun TabletLayout() = VanGoAppShell()

@Composable
private fun TabletInfoSummaryBlock() {
    val data by vanGoData.collectAsState()
    val env by bleEnvSensors.collectAsState()
    val (rgbR, rgbG, rgbB) = data.rgb888ForUi()
    fun formatTempC(v: Int): String = "$v °C"
    fun formatHumidity(v: Int): String = "$v %"
    fun formatSensorOrDash(v: Int, fmt: (Int) -> String): String = if (v <= 0) "—" else fmt(v)
    val tempVal = BleEnvSensorParser.resolveDisplayTempC(env, data)
    val humidityVal = BleEnvSensorParser.resolveDisplayHumidityPct(env, data)
    val platformChannels by panelPlatformChannels.collectAsState()
    val tempHumEnabled = shouldShowCombinedTempHumidityCard(data, env, platformChannels)
    DeviceOverviewCard()
    Spacer(Modifier.height(16.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CombinedTempHumidityMetricCard(
            enabled = tempHumEnabled,
            tempText = formatSensorOrDash(tempVal, ::formatTempC),
            humidityText = formatSensorOrDash(humidityVal, ::formatHumidity),
        )
        ColorPalette(
            data.RGBE == "yes",
            data.getSafeRGBData(3) > 0,
            rgbR,
            rgbG,
            rgbB
        ) { r, g, b ->
            println("RGB color updated: R=$r, G=$g, B=$b")
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val channels by panelPlatformChannels.collectAsState()
        if (isWaterTankInstalled(0, channels)) {
            CircularProgressBox(
                true,
                "Clean Water",
                data.getSafeSensorsDataUnsigned(2).toFloat() / 100.0f
            )
        }
        if (isWaterTankInstalled(1, channels)) {
            CircularProgressBox(
                true,
                "Gray Water",
                data.getSafeSensorsDataUnsigned(3).toFloat() / 100.0f
            )
        }
        if (isWaterTankInstalled(2, channels)) {
            CircularProgressBox(
                true,
                "Dirty Water",
                data.getSafeSensorsDataUnsigned(4).toFloat() / 100.0f
            )
        }
    }
}

@Composable
private fun TabletDimmerBlock() {
    VerticalSliderController(0)
    VerticalSliderController(1)
    VerticalSliderController(2)
    VerticalSliderController(3)
}

@Composable
fun TabletLeftContent(deviceInLandscape: Boolean = false) {
    if (deviceInLandscape) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val showInfoStrip = isTablet() && deviceInLandscape

            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    TabletInfoSummaryBlock()
                }

                if (showInfoStrip) {
                    TabletLandscapeInfoBar()
                }
            }
        }
    } else {
        // Portrait stack: outer column handles max height + scroll
        Column(modifier = Modifier.fillMaxWidth()) {
            TabletInfoSummaryBlock()
        }
    }
}

@Composable
fun TabletRightContent() {
    val data by vanGoData.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TabletDimmerBlock()
        InputOutputPanel(data.outDB, data.outNB)
    }
}


@Composable
fun TabletHeader() {
    AppTopBar(
        barHeight = 80.dp,
        logoSize = 76.dp,
        statusIconSize = 28.dp
    )
}

@Composable
fun TabletFooter(
    selectedTab: SelectedPage,
    onTabSelected: (SelectedPage) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val w = maxWidth.value.coerceAtLeast(280f)
        val scale = (w / 420f).coerceIn(0.95f, 1.42f) * if (isTablet()) 1.08f else 1f
        val barH = (64f * scale).dp.coerceIn(56.dp, 92.dp)
        val iconSz = (24f * scale).dp.coerceIn(22.dp, 36.dp)
        val padH = (24f * scale).dp.coerceIn(16.dp, 32.dp)
        val padV = (8f * scale).dp.coerceIn(6.dp, 14.dp)
        val itemPadH = (24f * scale).dp.coerceIn(16.dp, 36.dp)
        val itemPadV = (4f * scale).dp.coerceIn(2.dp, 10.dp)
        val labelSp = (11f * scale).coerceIn(10f, 15f).sp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barH)
                .background(MaterialTheme.colors.primary)
                .padding(horizontal = padH, vertical = padV)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabletFooterItem(
                    icon = Icons.Default.Home,
                    label = stringResource(Res.string.footer_home),
                    isSelected = selectedTab == SelectedPage.UserInfo,
                    onClick = { onTabSelected(SelectedPage.UserInfo) },
                    iconSize = iconSz,
                    horizontalPadding = itemPadH,
                    verticalPadding = itemPadV,
                    labelFontSize = labelSp,
                    modifier = Modifier.weight(1f),
                )

                TabletFooterItem(
                    icon = Icons.Default.Menu,
                    label = stringResource(Res.string.footer_menu),
                    isSelected = selectedTab == SelectedPage.Menu,
                    onClick = { onTabSelected(SelectedPage.Menu) },
                    iconSize = iconSz,
                    horizontalPadding = itemPadH,
                    verticalPadding = itemPadV,
                    labelFontSize = labelSp,
                    modifier = Modifier.weight(1f),
                )

                TabletFooterItem(
                    icon = Icons.Default.Person,
                    label = stringResource(Res.string.footer_profile),
                    isSelected = selectedTab == SelectedPage.Profile,
                    onClick = { onTabSelected(SelectedPage.Profile) },
                    iconSize = iconSz,
                    horizontalPadding = itemPadH,
                    verticalPadding = itemPadV,
                    labelFontSize = labelSp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun TabletFooterItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = 20.dp,
    horizontalPadding: Dp = 24.dp,
    verticalPadding: Dp = 4.dp,
    labelFontSize: androidx.compose.ui.unit.TextUnit? = null,
    modifier: Modifier = Modifier,
) {
    val captionStyle = if (labelFontSize != null) {
        MaterialTheme.typography.caption.copy(fontSize = labelFontSize)
    } else {
        MaterialTheme.typography.caption
    }
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(iconSize),
            tint = if (isSelected)
                MaterialTheme.colors.onPrimary
            else
                MaterialTheme.colors.onPrimary.copy(alpha = 0.6f)
        )
        Text(
            text = label,
            style = captionStyle,
            color = if (isSelected)
                MaterialTheme.colors.onPrimary
            else
                MaterialTheme.colors.onPrimary.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
