package com.hextech.vango

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteMqttRouteTest {
    @Test
    fun buildsTelemetryAndCommandTopics() {
        val route = RemoteMqttRoute(companyId = 7, deviceId = 42)

        assertEquals("hexnet/v1/7/42/telemetry", route.telemetryTopic)
        assertEquals("hexnet/v1/7/42/command", route.commandTopic)
    }

    @Test
    fun parsesRouteFromTelemetryTopic() {
        val route = RemoteMqttRoute.fromTopics(
            telemetry = "hexnet/v1/7/42/telemetry",
            command = "hexnet/v1/7/42/command",
        )

        assertEquals(RemoteMqttRoute(companyId = 7, deviceId = 42), route)
    }

    @Test
    fun rejectsInvalidTelemetryTopic() {
        assertNull(RemoteMqttRoute.fromTopics("hexnet/v1/not-a-device/telemetry", null))
    }
}
