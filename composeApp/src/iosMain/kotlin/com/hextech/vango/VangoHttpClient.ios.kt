package com.hextech.vango

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createVangoHttpClient(): HttpClient = HttpClient(CIO) {
    installVangoDefaults()
}
