package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import testvango.composeapp.generated.resources.onboarding_perm_location_why
import testvango.composeapp.generated.resources.onboarding_perm_usage
import testvango.composeapp.generated.resources.perm_conn_status_granted
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.CaravanType
import com.hextech.vango.profile.LoginPreferences
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.caravan_type_motor
import testvango.composeapp.generated.resources.caravan_type_tow
import testvango.composeapp.generated.resources.onboarding_ble_hint
import testvango.composeapp.generated.resources.onboarding_ble_unavailable_ios
import testvango.composeapp.generated.resources.onboarding_continue
import testvango.composeapp.generated.resources.onboarding_feat_climate
import testvango.composeapp.generated.resources.onboarding_feat_lighting
import testvango.composeapp.generated.resources.onboarding_feat_water
import testvango.composeapp.generated.resources.onboarding_finish
import testvango.composeapp.generated.resources.onboarding_got_it
import testvango.composeapp.generated.resources.onboarding_lets_go
import testvango.composeapp.generated.resources.onboarding_location_body
import testvango.composeapp.generated.resources.onboarding_location_title
import testvango.composeapp.generated.resources.onboarding_mode_ble_title
import testvango.composeapp.generated.resources.onboarding_mode_body
import testvango.composeapp.generated.resources.onboarding_mode_remote_title
import testvango.composeapp.generated.resources.onboarding_mode_title
import testvango.composeapp.generated.resources.onboarding_remote_hint
import testvango.composeapp.generated.resources.onboarding_skip
import testvango.composeapp.generated.resources.onboarding_start_title
import testvango.composeapp.generated.resources.onboarding_step_of
import testvango.composeapp.generated.resources.onboarding_type_body
import testvango.composeapp.generated.resources.onboarding_type_title
import testvango.composeapp.generated.resources.onboarding_welcome_body
import testvango.composeapp.generated.resources.onboarding_welcome_hero
import testvango.composeapp.generated.resources.onboarding_mode_hero
import testvango.composeapp.generated.resources.onboarding_location_hero
import testvango.composeapp.generated.resources.onboarding_caravan_motor
import testvango.composeapp.generated.resources.onboarding_caravan_tow
import org.jetbrains.compose.resources.DrawableResource

private val ObBg: Color @Composable get() = LocalAppChrome.current.bg
private val ObCard: Color @Composable get() = LocalAppChrome.current.card
private val ObText: Color @Composable get() = LocalAppChrome.current.text
private val ObMuted: Color @Composable get() = LocalAppChrome.current.muted
private val ObBorder: Color @Composable get() = LocalAppChrome.current.border
private val ObHeroText = Color.White
private val ObHeroMuted = Color(0xFFB8C0CC)

private enum class WizardStep { Welcome, Mode, Location, Caravan }

@Composable
fun FirstUseWizardScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bleOk = platformSupportsBle()
    var step by remember { mutableStateOf(WizardStep.Welcome) }
    var preferRemote by remember {
        mutableStateOf(!bleOk || LoginPreferences.getPreferredConnectionMode() == "remote")
    }
    var caravan by remember {
        mutableStateOf(
            when (LoginPreferences.getCachedCaravanType()?.trim()) {
                CaravanType.MotorCaravan.name, "Motor", "motor" -> CaravanType.MotorCaravan
                else -> CaravanType.TowCaravan
            },
        )
    }
    var nearbyOk by remember { mutableStateOf(hasNearbyDevicesPermissions()) }
    var locationOk by remember { mutableStateOf(hasLocationPermissions()) }
    var awaitingPermGrant by remember { mutableStateOf(false) }
    val accent = accentColorFromId(customAppAccentId)
    val stepIndex = when (step) {
        WizardStep.Welcome -> 1
        WizardStep.Mode -> 2
        WizardStep.Location -> 3
        WizardStep.Caravan -> 4
    }
    val totalSteps = if (bleOk) 4 else 3
    val permsReady = nearbyOk && locationOk

    LaunchedEffect(step, awaitingPermGrant) {
        if (step != WizardStep.Location) return@LaunchedEffect
        while (isActive && step == WizardStep.Location) {
            nearbyOk = hasNearbyDevicesPermissions()
            locationOk = hasLocationPermissions()
            if (awaitingPermGrant && nearbyOk && locationOk) {
                delay(300)
                awaitingPermGrant = false
                step = WizardStep.Caravan
                return@LaunchedEffect
            }
            delay(450)
        }
    }

    fun finish() {
        LoginPreferences.setPreferredConnectionMode(if (preferRemote) "remote" else "ble")
        persistCaravanType(caravan)
        LoginPreferences.setCompletedFirstUseWizard(true)
        if (preferRemote) {
            runCatching { activateRemoteModeExclusive() }
        } else if (bleOk) {
            runCatching { activateBleModeExclusive() }
        }
        onFinished()
    }

    fun goBack() {
        step = when (step) {
            WizardStep.Welcome -> WizardStep.Welcome
            WizardStep.Mode -> WizardStep.Welcome
            WizardStep.Location -> WizardStep.Mode
            WizardStep.Caravan -> if (bleOk) WizardStep.Location else WizardStep.Mode
        }
    }

    val headerText = ObHeroText
    val headerMuted = ObHeroMuted

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (step) {
            WizardStep.Welcome -> {
                Image(
                    painter = painterResource(Res.drawable.onboarding_welcome_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                WizardHeroScrim()
            }
            WizardStep.Mode -> {
                Image(
                    painter = painterResource(Res.drawable.onboarding_mode_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                )
                // Üst metin + alt seçenekler okunaklı; orta karavan görünsün
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xB3070B12),
                                    0.22f to Color(0x33070B12),
                                    0.48f to Color.Transparent,
                                    0.68f to Color(0x66070B12),
                                    0.82f to Color(0xD1070B12),
                                    1.00f to Color(0xF2070B12),
                                ),
                            ),
                        ),
                )
            }
            WizardStep.Location -> {
                Image(
                    painter = painterResource(Res.drawable.onboarding_location_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF070B12)),
                )
                WizardHeroScrim()
            }
            WizardStep.Caravan -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF070B12)),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step != WizardStep.Welcome) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = headerText,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = ::goBack),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                "VanGO",
                color = accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.onboarding_step_of, stepIndex.coerceAtMost(totalSteps), totalSteps),
                color = headerMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(10.dp))
        WizardStepDots(
            current = when (step) {
                WizardStep.Welcome -> 0
                WizardStep.Mode -> 1
                WizardStep.Location -> 2
                WizardStep.Caravan -> if (bleOk) 3 else 2
            },
            total = totalSteps,
            accent = accent,
        )
        Spacer(Modifier.height(18.dp))

        when (step) {
            WizardStep.Welcome -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(accent.copy(alpha = 0.16f), CircleShape)
                            .border(1.5.dp, accent.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.WavingHand,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Text(
                        stringResource(Res.string.onboarding_start_title),
                        color = ObHeroText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                    )
                    Text(
                        stringResource(Res.string.onboarding_welcome_body),
                        color = ObHeroMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        WizardFeatureChip(
                            icon = Icons.Filled.Lightbulb,
                            label = stringResource(Res.string.onboarding_feat_lighting),
                            accent = accent,
                        )
                        WizardFeatureChip(
                            icon = Icons.Filled.WaterDrop,
                            label = stringResource(Res.string.onboarding_feat_water),
                            accent = accent,
                        )
                        WizardFeatureChip(
                            icon = Icons.Filled.Thermostat,
                            label = stringResource(Res.string.onboarding_feat_climate),
                            accent = accent,
                        )
                    }
                }
            }
            WizardStep.Mode -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(Res.string.onboarding_mode_title),
                        color = ObHeroText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                    )
                    Text(
                        stringResource(Res.string.onboarding_mode_body),
                        color = ObHeroMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                    if (!bleOk) {
                        Text(
                            stringResource(Res.string.onboarding_ble_unavailable_ios),
                            color = ObHeroMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    // Karavan/hero görseli için orta alan — seçenekler bunun altında
                    Spacer(Modifier.weight(1f))
                    if (bleOk) {
                        WizardModeCard(
                            selected = !preferRemote,
                            icon = Icons.Filled.Bluetooth,
                            title = stringResource(Res.string.onboarding_mode_ble_title),
                            hint = stringResource(Res.string.onboarding_ble_hint),
                            accent = accent,
                            onHero = true,
                            onClick = { preferRemote = false },
                        )
                    }
                    WizardModeCard(
                        selected = preferRemote || !bleOk,
                        icon = Icons.Filled.Cloud,
                        title = stringResource(Res.string.onboarding_mode_remote_title),
                        hint = stringResource(Res.string.onboarding_remote_hint),
                        accent = accent,
                        onHero = true,
                        onClick = { preferRemote = true },
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            WizardStep.Location -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(Res.string.onboarding_location_title),
                        color = ObHeroText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                    )
                    Text(
                        stringResource(Res.string.onboarding_location_body),
                        color = ObHeroMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                    // 2. sayfa ile aynı: orta alan boş, kartlar Devam butonunun hemen üstünde
                    Spacer(Modifier.weight(1f))
                    WizardPermStatusCard(
                        icon = Icons.Filled.Bluetooth,
                        text = stringResource(Res.string.onboarding_perm_usage),
                        done = nearbyOk,
                        doneLabel = stringResource(Res.string.perm_conn_status_granted),
                        accent = accent,
                    )
                    WizardPermStatusCard(
                        icon = Icons.Filled.LocationOn,
                        text = stringResource(Res.string.onboarding_perm_location_why),
                        done = locationOk,
                        doneLabel = stringResource(Res.string.perm_conn_status_granted),
                        accent = accent,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            WizardStep.Caravan -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(Res.string.onboarding_type_title),
                        color = ObHeroText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                    )
                    Text(
                        stringResource(Res.string.onboarding_type_body),
                        color = ObHeroMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WizardCaravanOption(
                            modifier = Modifier.weight(1f),
                            image = Res.drawable.onboarding_caravan_motor,
                            label = stringResource(Res.string.caravan_type_motor),
                            selected = caravan == CaravanType.MotorCaravan,
                            accent = accent,
                            onClick = { caravan = CaravanType.MotorCaravan },
                        )
                        WizardCaravanOption(
                            modifier = Modifier.weight(1f),
                            image = Res.drawable.onboarding_caravan_tow,
                            label = stringResource(Res.string.caravan_type_tow),
                            selected = caravan == CaravanType.TowCaravan,
                            accent = accent,
                            onClick = { caravan = CaravanType.TowCaravan },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (step) {
                WizardStep.Welcome -> {
                    WizardPrimaryButton(
                        label = stringResource(Res.string.onboarding_lets_go),
                        accent = accent,
                        onClick = { step = WizardStep.Mode },
                    )
                    TextButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.onboarding_skip), color = ObHeroMuted)
                    }
                }
                WizardStep.Mode -> {
                    WizardPrimaryButton(
                        label = stringResource(Res.string.onboarding_continue),
                        accent = accent,
                        onClick = {
                            // İlk kurulumda izin sayfası atlanmasın (BLE destekliyorsa her zaman)
                            step = if (bleOk) WizardStep.Location else WizardStep.Caravan
                        },
                    )
                }
                WizardStep.Location -> {
                    WizardPrimaryButton(
                        label = if (permsReady) {
                            stringResource(Res.string.onboarding_continue)
                        } else {
                            stringResource(Res.string.onboarding_got_it)
                        },
                        accent = accent,
                        onClick = {
                            nearbyOk = hasNearbyDevicesPermissions()
                            locationOk = hasLocationPermissions()
                            if (nearbyOk && locationOk) {
                                step = WizardStep.Caravan
                                return@WizardPrimaryButton
                            }
                            awaitingPermGrant = true
                            requestMissingConnectionPermissions()
                            if (!isBluetoothAdapterEnabled()) {
                                requestEnableBluetooth()
                            }
                        },
                    )
                }
                WizardStep.Caravan -> {
                    WizardPrimaryButton(
                        label = stringResource(Res.string.onboarding_finish),
                        accent = accent,
                        onClick = { finish() },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun WizardHeroScrim(soft: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = if (soft) {
                        arrayOf(
                            0.00f to Color(0x88070B12),
                            0.16f to Color(0x22070B12),
                            0.35f to Color.Transparent,
                            0.82f to Color.Transparent,
                            1.00f to Color(0xA6070B12),
                        )
                    } else {
                        arrayOf(
                            0.00f to Color(0x88070B12),
                            0.18f to Color(0x22070B12),
                            0.42f to Color.Transparent,
                            0.78f to Color(0x33070B12),
                            1.00f to Color(0xC2070B12),
                        )
                    },
                ),
            ),
    )
}

@Composable
private fun WizardCaravanOption(
    modifier: Modifier,
    image: DrawableResource,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) accent else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp),
            )
            .background(Color(0xFF0B1018)),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = label,
            // BottomCenter: üst boşluk kırpılır, alt stand (mavi halka) görünür kalır
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x99070B12)),
                    ),
                ),
        )
        Text(
            text = label,
            color = ObHeroText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(28.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun WizardPermStatusCard(
    icon: ImageVector,
    text: String,
    done: Boolean,
    doneLabel: String,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC0B1018), RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Text(
            text = text,
            color = ObHeroMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
        if (done) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFF2ECC71), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
                Text(doneLabel, color = Color(0xFF2ECC71), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WizardFeatureChip(
    icon: ImageVector,
    label: String,
    accent: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(96.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(1.5.dp, accent.copy(alpha = 0.7f), CircleShape)
                .background(accent.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            color = ObMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun WizardStepDots(current: Int, total: Int, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (i <= current) accent else ObBorder),
            )
        }
    }
}

@Composable
private fun WizardPrimaryButton(label: String, accent: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = Color.Black),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun WizardModeCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    hint: String,
    accent: Color,
    onClick: () -> Unit,
    onHero: Boolean = false,
) {
    val cardBg = if (onHero) Color(0xCC0B1018) else ObCard
    val border = if (selected) accent else if (onHero) accent.copy(alpha = 0.35f) else ObBorder
    val titleColor = if (onHero) ObHeroText else ObText
    val hintColor = if (onHero) ObHeroMuted else ObMuted
    val iconMute = if (onHero) ObHeroMuted else ObMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = border,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = if (onHero) 0.08f else 0.06f),
                    CircleShape,
                )
                .border(1.5.dp, if (selected) accent else accent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (selected) accent else iconMute, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, color = hintColor, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }
    }
}
