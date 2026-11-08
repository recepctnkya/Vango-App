package com.hextech.vango.profile

import com.hextech.vango.VangoHttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RegisterRequestResponse(
    val ok: Boolean = false,
    val message: String = "",
    val request_id: Int? = null,
    val request_status: String? = null,
)

@Serializable
private data class RegisterRequestJsonPayload(
    val first_name: String,
    val last_name: String,
    val full_name: String,
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val address: String? = null,
    val device_mac: String? = null,
    val device_uid: String? = null,
)

object RegisterRequestApi {
    private val client get() = VangoHttpClient.instance
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun submit(input: RegisterRequestInput): Result<RegisterRequestResult> =
        runCatching { submitOnce(input) }.recoverCatching { firstError ->
            if (firstError.shouldRetryRegisterRequest()) {
                submitOnce(input)
            } else {
                throw firstError
            }
        }

    private suspend fun submitOnce(input: RegisterRequestInput): RegisterRequestResult {
            val firstName = input.firstName.trim()
            val lastName = input.lastName.trim()
            val username = input.username.trim()
            val email = input.email.trim()
            val phone = input.phone?.trim()?.takeIf { it.isNotEmpty() }
            val address = input.address?.trim()?.takeIf { it.isNotEmpty() }
            val deviceMac = input.deviceMac?.trim()?.takeIf { it.isNotEmpty() }
            val deviceUid = input.deviceUid?.trim()?.takeIf { it.isNotEmpty() }
            val fullName = "$firstName $lastName".trim()
            val imageBytes = input.profileImage?.takeIf { it.isNotEmpty() }

            val response = client.post(HexnetApi.Auth.registerRequest) {
                header(HttpHeaders.Accept, "application/json")
                if (imageBytes == null) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        json.encodeToString(
                            RegisterRequestJsonPayload.serializer(),
                            RegisterRequestJsonPayload(
                                first_name = firstName,
                                last_name = lastName,
                                full_name = fullName,
                                username = username,
                                email = email,
                                password = input.password,
                                phone = phone,
                                address = address,
                                device_mac = deviceMac,
                                device_uid = deviceUid,
                            ),
                        ),
                    )
                } else {
                    // Do not set Content-Type: Ktor sets multipart boundary on the body.
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("first_name", firstName)
                                append("last_name", lastName)
                                append("full_name", fullName)
                                append("username", username)
                                append("email", email)
                                append("password", input.password)
                                phone?.let { append("phone", it) }
                                address?.let { append("address", it) }
                                deviceMac?.let { append("device_mac", it) }
                                deviceUid?.let { append("device_uid", it) }
                                val sniff = sniffRegisterProfileImage(imageBytes)
                                    ?: error("Profil fotoğrafı biçimi desteklenmiyor.")
                                val (contentType, fileName) = sniff
                                append(
                                    "profile_image",
                                    imageBytes,
                                    Headers.build {
                                        append(
                                            HttpHeaders.ContentDisposition,
                                            "form-data; name=\"profile_image\"; filename=\"$fileName\"",
                                        )
                                        append(HttpHeaders.ContentType, contentType)
                                    },
                                )
                            },
                        ),
                    )
                }
            }
            val raw = response.readTextBodyOrThrow()
            val body = json.decodeFromStringOrNull<RegisterRequestResponse>(raw)
            return if (response.status == HttpStatusCode.OK && body?.ok == true) {
                RegisterRequestResult(
                    message = body.message,
                    requestId = body.request_id,
                    requestStatus = body.request_status,
                )
            } else {
                val fallback = when (response.status) {
                    HttpStatusCode.MethodNotAllowed ->
                        "Kayıt adresi POST bekliyor; yönlendirme veya sunucu yapılandırması POST’u engelliyor olabilir."
                    HttpStatusCode.BadRequest -> "Zorunlu alan eksik veya format hatalı."
                    HttpStatusCode.Conflict -> "Bu e-posta veya bekleyen başvuru ile çakışma var."
                    HttpStatusCode.InternalServerError -> "Sunucu hatası."
                    HttpStatusCode.ServiceUnavailable -> "Kayıt servisi şu an hazır değil."
                    else -> "Başvuru sırasında beklenmeyen bir hata oluştu."
                }
                val msg = when {
                    body != null -> body.message.ifBlank { null } ?: fallback
                    raw.isBlank() -> "Sunucu boş yanıt döndü (HTTP ${response.status.value}). Lütfen tekrar deneyin."
                    else -> "Sunucu yanıtı beklenen biçimde değil (HTTP ${response.status.value})."
                }
                error(msg)
            }
        }
}

private fun Throwable.shouldRetryRegisterRequest(): Boolean {
    val msg = message?.trim().orEmpty()
    if (msg.isEmpty()) return false
    return msg.contains("unexpected eof", ignoreCase = true) ||
        msg.contains("failed to parse http response", ignoreCase = true) ||
        msg.contains("sunucu yanıtı okunamadı", ignoreCase = true)
}
