package com.hextech.vango

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PanelConfigMapperTest {
    @Test
    fun platformRelayEnabledMapsToDevConfigMask() {
        val platform = defaultPanelPlatformConfig("vango_mini").copy(
            channels = defaultPanelPlatformConfig("vango_mini").channels.copy(
                relays = listOf(
                    PanelPlatformChannel(id = 0, enabled = true, icon = "lightbulb"),
                    PanelPlatformChannel(id = 1, enabled = false, icon = "plug"),
                ),
            ),
        )
        val dev = PanelConfigMapper.applyPlatformConfigToDevConfig(platform, uiState.value.devConfig)
        assertEquals(1, dev.OutputsNameBuffer[0].toInt())
        assertEquals(0, dev.OutputsNameBuffer[1].toInt())
        assertEquals(1, dev.relayEnableMask)
    }

    @Test
    fun devConfigRoundTripPreservesEnabledRelay() {
        val base = defaultPanelPlatformConfig("vango_medium")
        val dev = PanelConfigMapper.applyPlatformConfigToDevConfig(base, uiState.value.devConfig)
        val out = PanelConfigMapper.devConfigToPlatformConfig(dev, "vango_medium", base)
        assertTrue(out.channels.relays.first().enabled)
    }
}