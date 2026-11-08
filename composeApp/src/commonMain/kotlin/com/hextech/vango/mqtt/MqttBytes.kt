package com.hextech.vango.mqtt

internal fun String.toMqttPayload(): UByteArray =
    encodeToByteArray().map { it.toUByte() }.toUByteArray()

internal fun UByteArray?.decodeToPayloadString(): String? =
    this?.map { it.toByte() }?.toByteArray()?.decodeToString()

internal fun String?.toMqttPassword(): UByteArray? =
    this?.takeIf { it.isNotEmpty() }?.toMqttPayload()
