package com.hextech.vango

/**
 * Fixed [HeroSkySnapshot] samples for Compose previews — not live astronomy.
 */
object HeroSkyPreviewFactory {
    fun dawnClear() = base(
        period = HeroSkyPeriod.Dawn,
        sun = HeroCelestialPos(0.18f, 0.48f, true),
        moon = null,
        starVis = 0.2f,
        weather = HeroSkyWeather.Clear,
        cloudPct = 5,
        label = "Güneşli",
        icon = HeroWeatherBadgeIcon.Sun,
        temp = "12°C",
    )

    fun noonClear() = base(
        period = HeroSkyPeriod.Day,
        sun = HeroCelestialPos(0.50f, 0.20f, true),
        moon = HeroMoonState(HeroCelestialPos(0.82f, 0.55f, true), 0.35f, true),
        starVis = 0f,
        weather = HeroSkyWeather.Clear,
        cloudPct = 8,
        label = "Güneşli",
        icon = HeroWeatherBadgeIcon.Sun,
        temp = "24°C",
        season = SeasonState.Summer,
    )

    /** Sunny summer — clear, calm–light breeze, full foliage. */
    fun sunnySummer() = noonClear().copy(
        season = SeasonState.Summer,
        foliageAmount = 1f,
        wind = WindState(dirX = 0.25f, speedNorm = 0.18f),
        windy = false,
        snowCover = 0f,
    )

    /** Windy autumn with falling leaves. */
    fun windyAutumn() = noonClear().copy(
        season = SeasonState.Autumn,
        foliageAmount = 0.42f,
        weather = HeroSkyWeather.PartlyCloudy,
        cloudDensity = 0.45f,
        weatherLabel = "Rüzgârlı",
        weatherBadgeIcon = HeroWeatherBadgeIcon.PartlyCloudy,
        wind = WindState(dirX = 0.85f, speedNorm = 0.72f, gustHz = 0.55f),
        windy = true,
        snowCover = 0f,
        temperatureLabel = "14°C",
    )

    /** Rainy spring. */
    fun rainySpring() = rainy().copy(
        season = SeasonState.Spring,
        foliageAmount = 0.88f,
        period = HeroSkyPeriod.Day,
        wind = WindState(dirX = 0.45f, speedNorm = 0.40f),
        snowCover = 0f,
        weatherLabel = "Yağmurlu",
        temperatureLabel = "11°C",
    )

    /** Snowy winter with mid accumulation. */
    fun snowyWinter() = snowy().copy(
        season = SeasonState.Winter,
        foliageAmount = 0.04f,
        period = HeroSkyPeriod.Day,
        sun = HeroCelestialPos(0.45f, 0.32f, true),
        nightWindowsLit = false,
        wind = WindState(dirX = -0.35f, speedNorm = 0.22f),
        snowCover = 0.42f,
        mountainSnowCover = 0.72f,
        iceCoverage = 0.12f,
        snowfallIntensity = 0.55f,
        weatherLabel = "Karlı",
        temperatureLabel = "-2°C",
        starVisibility = 0f,
    )

    /** Foggy morning. */
    fun foggyMorning() = foggy().copy(
        season = SeasonState.Autumn,
        period = HeroSkyPeriod.Dawn,
        foliageAmount = 0.55f,
        sun = HeroCelestialPos(0.22f, 0.50f, true),
        wind = WindState(dirX = 0.12f, speedNorm = 0.12f),
        snowCover = 0f,
        weatherLabel = "Sisli",
        temperatureLabel = "8°C",
        farHaze = 0.90f,
    )

    fun noonPartly() = base(
        period = HeroSkyPeriod.Day,
        sun = HeroCelestialPos(0.50f, 0.20f, true),
        moon = null,
        starVis = 0f,
        weather = HeroSkyWeather.PartlyCloudy,
        cloudPct = 40,
        label = "Parçalı Bulutlu",
        icon = HeroWeatherBadgeIcon.PartlyCloudy,
        temp = "22°C",
    )

    fun duskPartly() = base(
        period = HeroSkyPeriod.Dusk,
        sun = HeroCelestialPos(0.86f, 0.50f, true),
        moon = HeroMoonState(HeroCelestialPos(0.22f, 0.28f, true), 0.7f, false),
        starVis = 0.45f,
        weather = HeroSkyWeather.PartlyCloudy,
        cloudPct = 45,
        label = "Parçalı Bulutlu",
        icon = HeroWeatherBadgeIcon.PartlyCloudy,
        temp = "18°C",
    )

    fun nightClear() = base(
        period = HeroSkyPeriod.Night,
        sun = HeroCelestialPos(0.1f, 0.72f, false),
        moon = HeroMoonState(HeroCelestialPos(0.62f, 0.22f, true), 0.85f, false),
        starVis = 1f,
        weather = HeroSkyWeather.Clear,
        cloudPct = 5,
        label = "Açık",
        icon = HeroWeatherBadgeIcon.Moon,
        temp = "9°C",
    )

    fun rainy() = noonClear().copy(
        weather = HeroSkyWeather.Rain,
        windy = true,
        cloudDensity = 0.85f,
        reflectionStrength = 0.05f,
        weatherLabel = "Yağmurlu",
        weatherBadgeIcon = HeroWeatherBadgeIcon.Rain,
        starVisibility = 0f,
        celestialDiskAlpha = 0f,
        celestialGlowAlpha = 0.05f,
        lightStrengthMul = sceneLightMultiplier(HeroSkyWeather.Rain, 0.85f),
        shadowSoftness = 0.90f,
        farHaze = 0.40f,
    )

    fun snowy() = nightClear().copy(
        weather = HeroSkyWeather.Snow,
        cloudDensity = 0.75f,
        reflectionStrength = 0.05f,
        weatherLabel = "Karlı",
        weatherBadgeIcon = HeroWeatherBadgeIcon.Snow,
        starVisibility = 0f,
        celestialDiskAlpha = 0f,
        celestialGlowAlpha = 0.05f,
        lightStrengthMul = sceneLightMultiplier(HeroSkyWeather.Snow, 0.75f),
        shadowSoftness = 0.88f,
        farHaze = 0.40f,
    )

    fun foggy() = duskPartly().copy(
        weather = HeroSkyWeather.Fog,
        cloudDensity = 0.55f,
        reflectionStrength = 0.08f,
        weatherLabel = "Sisli",
        weatherBadgeIcon = HeroWeatherBadgeIcon.Fog,
        starVisibility = 0.1f,
        celestialDiskAlpha = 0.25f,
        celestialGlowAlpha = 0.12f,
        lightStrengthMul = sceneLightMultiplier(HeroSkyWeather.Fog, 0.55f),
        shadowSoftness = 0.70f,
        farHaze = 0.85f,
    )

    fun overcast() = noonClear().copy(
        weather = HeroSkyWeather.Overcast,
        cloudDensity = 0.92f,
        reflectionStrength = 0.05f,
        weatherLabel = "Bulutlu",
        weatherBadgeIcon = HeroWeatherBadgeIcon.Cloudy,
        sun = HeroCelestialPos(0.48f, 0.28f, true),
        celestialDiskAlpha = 0f,
        celestialGlowAlpha = 0.06f,
        lightStrengthMul = sceneLightMultiplier(HeroSkyWeather.Overcast, 0.92f),
        shadowSoftness = 0.88f,
        farHaze = 0.32f,
    )

    private fun base(
        period: HeroSkyPeriod,
        sun: HeroCelestialPos?,
        moon: HeroMoonState?,
        starVis: Float,
        weather: HeroSkyWeather,
        cloudPct: Int,
        label: String,
        icon: HeroWeatherBadgeIcon,
        temp: String,
        season: SeasonState = SeasonState.Summer,
    ): HeroSkySnapshot {
        val density = when (weather) {
            HeroSkyWeather.Clear -> (cloudPct / 100f) * 0.15f
            HeroSkyWeather.PartlyCloudy -> maxOf(0.35f, cloudPct / 100f * 0.9f)
            HeroSkyWeather.Overcast -> maxOf(0.80f, cloudPct / 100f * 0.95f)
            HeroSkyWeather.Rain, HeroSkyWeather.Snow -> maxOf(0.75f, cloudPct / 100f * 0.9f)
            HeroSkyWeather.Fog -> maxOf(0.45f, cloudPct / 100f * 0.7f)
        }.coerceIn(0f, 1f)
        val (disk, glow) = celestialVisibility(cloudPct, weather)
        return HeroSkySnapshot(
            period = period,
            sun = sun,
            moon = moon,
            starVisibility = starVis,
            weather = weather,
            windy = false,
            cloudDensity = density,
            reflectionStrength = if (sun?.aboveHorizon == true || moon?.pos?.aboveHorizon == true) {
                when (weather) {
                    HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0.08f
                    HeroSkyWeather.Fog -> 0.12f
                    else -> 0.85f * (0.2f + disk * 0.8f)
                }
            } else {
                0.2f
            },
            bodyXFrac = sun?.takeIf { it.aboveHorizon }?.xFrac
                ?: moon?.pos?.takeIf { it.aboveHorizon }?.xFrac
                ?: 0.55f,
            nightWindowsLit = period == HeroSkyPeriod.Night || period == HeroSkyPeriod.Dusk,
            weatherStale = false,
            weatherLabel = label,
            temperatureLabel = temp,
            weatherBadgeIcon = icon,
            hasWeatherData = true,
            celestialDiskAlpha = disk,
            celestialGlowAlpha = glow,
            lightStrengthMul = sceneLightMultiplier(weather, density),
            shadowSoftness = sceneShadowSoftness(weather, density),
            farHaze = sceneFarHaze(weather, null),
            season = season,
            wind = WindState.Calm,
            foliageAmount = seasonFoliageAmount(season, 15),
            snowCover = 0f,
        )
    }
}
