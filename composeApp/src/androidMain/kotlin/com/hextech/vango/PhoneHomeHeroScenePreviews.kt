package com.hextech.vango

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Preview helper — toggles [HeroSceneDebugLayers] without shipping controls in the app UI. */
@Composable
private fun HeroPreviewLayers(layers: HeroSceneLayerFlags, content: @Composable () -> Unit) {
    val previous = remember { HeroSceneDebugLayers.flags }
    DisposableEffect(layers) {
        HeroSceneDebugLayers.flags = layers
        onDispose { HeroSceneDebugLayers.flags = previous }
    }
    content()
}

@Preview(name = "Sky Dawn Clear", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyDawn() {
    PhoneHomeHeroSceneCard(
        clockLabel = "06:42",
        temperatureLabel = "12°C",
        weatherLabel = "Açık",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.dawnClear(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Noon Clear", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyNoon() {
    PhoneHomeHeroSceneCard(
        clockLabel = "13:10",
        temperatureLabel = "24°C",
        weatherLabel = "Açık",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.noonClear(),
        isLightTheme = false,
        onClick = {},
        vehicleType = HeroVehicleType.Motorhome,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Noon Partly", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyNoonPartly() {
    PhoneHomeHeroSceneCard(
        clockLabel = "13:10",
        temperatureLabel = "22°C",
        weatherLabel = "Parçalı bulutlu",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.noonPartly(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Dusk Partly", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyDusk() {
    PhoneHomeHeroSceneCard(
        clockLabel = "19:05",
        temperatureLabel = "18°C",
        weatherLabel = "Parçalı bulutlu",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.duskPartly(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Night Clear", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyNight() {
    PhoneHomeHeroSceneCard(
        clockLabel = "22:55",
        temperatureLabel = "9°C",
        weatherLabel = "Açık",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.nightClear(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Rain", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyRain() {
    PhoneHomeHeroSceneCard(
        clockLabel = "14:20",
        temperatureLabel = "15°C",
        weatherLabel = "Yağmurlu",
        statusLabel = "Bağlanıyor…",
        linkVisual = HeroLinkVisual.Connecting,
        sky = HeroSkyPreviewFactory.rainy(),
        isLightTheme = false,
        onClick = {},
        vehicleType = HeroVehicleType.Motorhome,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Snow", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkySnow() {
    PhoneHomeHeroSceneCard(
        clockLabel = "21:40",
        temperatureLabel = "-2°C",
        weatherLabel = "Karlı",
        statusLabel = "Sistem bağlı değil",
        linkVisual = HeroLinkVisual.Disconnected,
        sky = HeroSkyPreviewFactory.snowy(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Fog", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyFog() {
    PhoneHomeHeroSceneCard(
        clockLabel = "08:15",
        temperatureLabel = "7°C",
        weatherLabel = "Sisli",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.foggy(),
        isLightTheme = false,
        onClick = {},
        vehicleType = HeroVehicleType.Motorhome,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Sky Overcast", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroSkyOvercast() {
    PhoneHomeHeroSceneCard(
        clockLabel = "11:30",
        temperatureLabel = "16°C",
        weatherLabel = "Kapalı",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.overcast(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Closeup Towable Wheels", showBackground = true, widthDp = 400, heightDp = 230)
@Composable
fun PreviewCloseupTowableWheels() {
    HeroVehicleCloseupPreview(
        vehicleType = HeroVehicleType.Towable,
        nightWindowsLit = true,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    )
}

@Preview(name = "Closeup Motorhome Wheels", showBackground = true, widthDp = 400, heightDp = 230)
@Composable
fun PreviewCloseupMotorhomeWheels() {
    HeroVehicleCloseupPreview(
        vehicleType = HeroVehicleType.Motorhome,
        nightWindowsLit = true,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    )
}

@Preview(name = "Layer Mountains Only", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroLayerMountains() {
    HeroPreviewLayers(HeroSceneLayerFlags.None.copy(sky = true, mountains = true)) {
        PhoneHomeHeroSceneCard(
            clockLabel = "13:10",
            temperatureLabel = "24°C",
            weatherLabel = "Açık",
            statusLabel = "Sistem bağlı",
            linkVisual = HeroLinkVisual.Connected,
            sky = HeroSkyPreviewFactory.noonClear(),
            isLightTheme = true,
            onClick = {},
            vehicleType = HeroVehicleType.Towable,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}

@Preview(name = "Layer Trees Only", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroLayerTrees() {
    HeroPreviewLayers(
        HeroSceneLayerFlags.None.copy(sky = true, treesFar = true, treesNear = true),
    ) {
        PhoneHomeHeroSceneCard(
            clockLabel = "13:10",
            temperatureLabel = "24°C",
            weatherLabel = "Açık",
            statusLabel = "Sistem bağlı",
            linkVisual = HeroLinkVisual.Connected,
            sky = HeroSkyPreviewFactory.noonClear(),
            isLightTheme = true,
            onClick = {},
            vehicleType = HeroVehicleType.Towable,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}

@Preview(name = "Layer Vehicle Only", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroLayerVehicle() {
    HeroPreviewLayers(HeroSceneLayerFlags.None.copy(sky = true, vehicle = true, phoneLink = true)) {
        PhoneHomeHeroSceneCard(
            clockLabel = "13:10",
            temperatureLabel = "24°C",
            weatherLabel = "Açık",
            statusLabel = "Sistem bağlı",
            linkVisual = HeroLinkVisual.Connected,
            sky = HeroSkyPreviewFactory.noonClear(),
            isLightTheme = true,
            onClick = {},
            vehicleType = HeroVehicleType.Motorhome,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}

@Preview(name = "Layer Lake + Shore", showBackground = true, widthDp = 360)
@Composable
fun PreviewHeroLayerLakeShore() {
    HeroPreviewLayers(
        HeroSceneLayerFlags.None.copy(
            sky = true,
            lake = true,
            wetShore = true,
            farShore = true,
            park = true,
            waterLife = true,
        ),
    ) {
        PhoneHomeHeroSceneCard(
            clockLabel = "13:10",
            temperatureLabel = "24°C",
            weatherLabel = "Açık",
            statusLabel = "Sistem bağlı",
            linkVisual = HeroLinkVisual.Connected,
            sky = HeroSkyPreviewFactory.noonClear(),
            isLightTheme = true,
            onClick = {},
            vehicleType = HeroVehicleType.Towable,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}

@Preview(name = "Season Sunny Summer", showBackground = true, widthDp = 360)
@Composable
fun PreviewSeasonSunnySummer() {
    PhoneHomeHeroSceneCard(
        clockLabel = "14:05",
        temperatureLabel = "28°C",
        weatherLabel = "Güneşli",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.sunnySummer(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Season Windy Autumn", showBackground = true, widthDp = 360)
@Composable
fun PreviewSeasonWindyAutumn() {
    PhoneHomeHeroSceneCard(
        clockLabel = "16:20",
        temperatureLabel = "14°C",
        weatherLabel = "Rüzgârlı",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.windyAutumn(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Motorhome,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Season Rainy Spring", showBackground = true, widthDp = 360)
@Composable
fun PreviewSeasonRainySpring() {
    PhoneHomeHeroSceneCard(
        clockLabel = "10:40",
        temperatureLabel = "11°C",
        weatherLabel = "Yağmurlu",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.rainySpring(),
        isLightTheme = false,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Season Snowy Winter", showBackground = true, widthDp = 360)
@Composable
fun PreviewSeasonSnowyWinter() {
    PhoneHomeHeroSceneCard(
        clockLabel = "12:15",
        temperatureLabel = "-1°C",
        weatherLabel = "Karlı",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.snowyWinter(),
        isLightTheme = true,
        onClick = {},
        vehicleType = HeroVehicleType.Towable,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}

@Preview(name = "Season Foggy Morning", showBackground = true, widthDp = 360)
@Composable
fun PreviewSeasonFoggyMorning() {
    PhoneHomeHeroSceneCard(
        clockLabel = "07:05",
        temperatureLabel = "8°C",
        weatherLabel = "Sisli",
        statusLabel = "Sistem bağlı",
        linkVisual = HeroLinkVisual.Connected,
        sky = HeroSkyPreviewFactory.foggyMorning(),
        isLightTheme = false,
        onClick = {},
        vehicleType = HeroVehicleType.Motorhome,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}
