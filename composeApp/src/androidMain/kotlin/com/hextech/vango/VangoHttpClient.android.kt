package com.hextech.vango

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createVangoHttpClient(): HttpClient = HttpClient(OkHttp) {
    installVangoDefaults()
}
