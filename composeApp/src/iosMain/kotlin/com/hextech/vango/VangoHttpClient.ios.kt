package com.hextech.vango

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createVangoHttpClient(): HttpClient = HttpClient(Darwin) {
    installVangoDefaults()
}
