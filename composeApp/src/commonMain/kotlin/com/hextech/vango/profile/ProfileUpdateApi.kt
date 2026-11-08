package com.hextech.vango.profile

import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Hexnet IoT: profil metni JSON ile [PROFILE_JSON_ENDPOINT], isteğe bağlı fotoğraf multipart ile
 * [AVATAR_UPLOAD_ENDPOINT] (alan adı `avatar`, max 2 MB).
 */
@Serializable
private data class GenericOkResponse(
    val ok: Boolean = false,
    val message: String = "",
)

@Serializable
private data class AvatarUploadResponse(
    val ok: Boolean = false,
    val url: String? = null,
    val message: String = "",
)

object ProfileUpdateApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    /** Başarıda yüklenen avatar göreli URL (varsa); yalnızca metin güncellemesi ise null. */
    suspend fun update(profile: UserProfile, authToken: String): Result<String?> = runCatching {
        val token = authToken.trim()
        if (token.isEmpty()) error("Oturum bulunamadı.")

        val email = profile.email.trim()
        if (email.isEmpty()) error("E-posta zorunludur.")

        val fullName = profile.fullName.trim()
        if (fullName.length < 2) error("Ad soyad zorunludur.")

        var avatarRelativeUrl: String? = null
        val bytes = profile.avatarBytes
        if (bytes != null && bytes.isNotEmpty()) {
            if (bytes.size > AVATAR_UPLOAD_MAX_BYTES) {
                error("Profil fotoğrafı en fazla 4 MB olabilir.")
            }
            val sniff = sniffAvatarUploadImage(bytes)
                ?: error("Profil fotoğrafı biçimi desteklenmiyor (JPEG, PNG, GIF, WebP).")
            avatarRelativeUrl = uploadAvatar(bytes, sniff, token)
        }

        postProfileJson(
            fullName = fullName,
            email = email,
            phone = profile.phone.trim().ifBlank { null },
            profileLocation = profile.address.trim().ifBlank { null },
            profileAvatarUrl = avatarRelativeUrl,
            token = token,
        )
        avatarRelativeUrl
    }

    private suspend fun uploadAvatar(
        bytes: ByteArray,
        sniff: Pair<String, String>,
        token: String,
    ): String {
        val (contentType, fileName) = sniff
        val response = client.post(HexnetApi.Auth.avatarUpload) {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "avatar",
                            bytes,
                            Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"avatar\"; filename=\"$fileName\"",
                                )
                                append(HttpHeaders.ContentType, contentType)
                            },
                        )
                    },
                ),
            )
        }
        val raw = response.readTextBodyOrThrow()
        val body = json.decodeFromStringOrNull<AvatarUploadResponse>(raw)
        return when {
            response.status == HttpStatusCode.OK && body?.ok == true && !body.url.isNullOrBlank() ->
                body.url.trim()
            response.status == HttpStatusCode.Unauthorized ->
                error("Oturum süresi doldu veya yetkisiz. Lütfen tekrar giriş yapın.")
            else -> {
                val msg = body?.message?.ifBlank { null }
                    ?: when {
                        raw.isBlank() -> "Fotoğraf yanıtı boş (HTTP ${response.status})."
                        else -> when (response.status) {
                            HttpStatusCode.PayloadTooLarge -> "Profil fotoğrafı çok büyük."
                            HttpStatusCode.BadRequest -> "Profil fotoğrafı yüklenemedi: geçersiz veri."
                            HttpStatusCode.InternalServerError -> "Sunucu hatası."
                            else -> "Profil fotoğrafı yüklenirken hata oluştu."
                        }
                    }
                error(msg)
            }
        }
    }

    private suspend fun postProfileJson(
        fullName: String,
        email: String,
        phone: String?,
        profileLocation: String?,
        profileAvatarUrl: String?,
        token: String,
    ) {
        val payload = buildJsonObject {
            put("full_name", fullName)
            put("email", email)
            phone?.let { put("phone", it) }
            profileLocation?.let { put("profile_location", it) }
            profileAvatarUrl?.let { put("profile_avatar_url", it) }
        }
        val response = client.post(HexnetApi.Auth.profile) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.ContentType, "application/json")
            setBody(payload.toString())
        }
        val raw = response.readTextBodyOrThrow()
        val body = json.decodeFromStringOrNull<GenericOkResponse>(raw)
        when {
            response.status == HttpStatusCode.OK && body?.ok == true -> Unit
            response.status == HttpStatusCode.Unauthorized ->
                error("Oturum süresi doldu veya yetkisiz. Lütfen tekrar giriş yapın.")
            response.status == HttpStatusCode.Forbidden ->
                error(body?.message?.ifBlank { null } ?: "Bu işlem için yetkiniz yok.")
            response.status == HttpStatusCode.TooManyRequests ->
                error(body?.message?.ifBlank { null } ?: "Çok fazla istek. Lütfen sonra tekrar deneyin.")
            else -> {
                val msg = body?.message?.ifBlank { null }
                    ?: when {
                        raw.isBlank() -> "Profil yanıtı boş (HTTP ${response.status})."
                        else -> when (response.status) {
                            HttpStatusCode.BadRequest -> "Profil güncellenemedi: geçersiz veri."
                            HttpStatusCode.InternalServerError -> "Sunucu hatası."
                            else -> "Profil güncellenirken hata oluştu."
                        }
                    }
                error(msg)
            }
        }
    }
}
