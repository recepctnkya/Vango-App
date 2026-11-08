package com.hextech.vango

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

/**
 * Tek paylaşımlı Ktor istemcisi. Yavaş mobil ağ ve multipart (kayıt, fotoğraf) için süre sınırları yüksektir.
 *
 * [HttpRedirect] ile `checkHttpMethod = false`: 302 vb. yönlendirmede POST gövdesi korunur; aksi halde bazı
 * sunucular ikinci isteği GET yapar ve PHP "Method not allowed" döner.
 *
 * [User-Agent]: Bazı CDN/WAF’ler istemci olmayan istekleri keser; mobil tarayıcı benzeri tanımlı istek.
 *
 * Motor: Android OkHttp (NetworkSecurityConfig / hostname-aware TLS), iOS CIO.
 */
object VangoHttpClient {
    val instance: HttpClient by lazy { createVangoHttpClient() }
}

expect fun createVangoHttpClient(): HttpClient

internal fun HttpClientConfig<*>.installVangoDefaults() {
    install(DefaultRequest) {
        header(HttpHeaders.UserAgent, "VanGO/1.0 (Hexnet; Ktor-Client; Android-iOS)")
    }
    install(HttpRedirect) {
        checkHttpMethod = false
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 45_000
        socketTimeoutMillis = 120_000
    }
}
