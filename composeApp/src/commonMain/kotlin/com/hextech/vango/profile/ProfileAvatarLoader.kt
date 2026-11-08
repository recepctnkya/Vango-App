package com.hextech.vango.profile

import com.hextech.vango.VangoHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

private const val HEXNET_BASE_URL = HexnetApi.BASE_HOST
private const val HEXNET_WEB_BASE_PATH = "/myhexnet/"
private const val HEXNET_PANEL_BASE_PATH = "/myhexnet/panel/"
private const val MAX_AVATAR_BYTES = 4 * 1024 * 1024

private fun resolveAvatarCandidates(raw: String?): List<String> {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return emptyList()
    if (value.startsWith("http://") || value.startsWith("https://")) return listOf(value)
    if (value.startsWith("/")) return listOf(HEXNET_BASE_URL + value)

    val rel = value.trimStart('/')
    val out = linkedSetOf<String>()
    // v0.12 auth/avatar-upload.php returns "assets/uploads/user-avatar/..."
    if (rel.startsWith("assets/")) {
        out += HEXNET_BASE_URL + HEXNET_PANEL_BASE_PATH + rel
    }
    // Legacy/admin paths
    out += HEXNET_BASE_URL + HEXNET_WEB_BASE_PATH + rel
    out += HEXNET_BASE_URL + "/" + rel
    return out.toList()
}

object ProfileAvatarLoader {
    suspend fun loadBytes(rawUrl: String?): ByteArray? = runCatching {
        val urls = resolveAvatarCandidates(rawUrl)
        for (url in urls) {
            val response = VangoHttpClient.instance.get(url)
            if (response.status != HttpStatusCode.OK) continue
            val ct = response.headers[HttpHeaders.ContentType].orEmpty().lowercase()
            if (ct.contains("text/html") || ct.contains("application/json")) continue
            val bytes: ByteArray = response.body()
            if (bytes.isNotEmpty() && bytes.size <= MAX_AVATAR_BYTES) {
                return@runCatching bytes
            }
        }
        null
    }.getOrNull()
}
