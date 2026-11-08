package com.hextech.vango.profile

data class ParsedDeviceQr(
    val mac: String?,
    val uid: String? = null,
    val modelName: String? = null,
)

private val macLabelRegex = Regex(
    """(?i)mac\s*[:=]\s*([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5})""",
)
private val uidLabelRegex = Regex(
    """(?i)uid\s*[:=]\s*([A-Za-z0-9][A-Za-z0-9_-]{2,})""",
)
private val modelLabelRegex = Regex(
    """(?i)model\s*[:=]\s*([^\r\n,;|]+)""",
)
private val bareMacRegex = Regex("""^([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5})$""")
private val anyMacInTextRegex = Regex(
    """(?i)([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5})""",
)

private fun findMacInText(text: String): String? =
    anyMacInTextRegex.findAll(text)
        .mapNotNull { match -> match.groupValues.getOrNull(1)?.let { normalizeMacCandidate(it) } }
        .firstOrNull()

private fun normalizeMacCandidate(raw: String): String? {
    val normalized = normalizeMacForDisplay(raw.trim())
    return normalized.takeIf { it.length == 17 && isValidMacFormat(it) }
}

private fun macFromDelimitedSegments(text: String): String? {
    val separators = charArrayOf(',', ';', '|', '\n', '\r')
    return text.split(*separators)
        .map { it.trim() }
        .firstNotNullOfOrNull { segment ->
            normalizeMacCandidate(segment) ?: anyMacInTextRegex.find(segment)?.groupValues?.getOrNull(1)
                ?.let { normalizeMacCandidate(it) }
        }
}

fun parseDeviceQrContent(raw: String): ParsedDeviceQr {
    val text = raw.trim()
    if (text.isEmpty()) return ParsedDeviceQr(null)

    val macFromLabel = macLabelRegex.find(text)?.groupValues?.getOrNull(1)?.let { normalizeMacCandidate(it) }
    val uidFromLabel = uidLabelRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    val modelFromLabel = modelLabelRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

    val mac = macFromLabel
        ?: findMacInText(text)
        ?: bareMacRegex.find(text.replace(" ", ""))?.groupValues?.getOrNull(1)?.let { normalizeMacCandidate(it) }
        ?: macFromDelimitedSegments(text)
        ?: run {
            val hex = text.uppercase().filter { ch -> ch in '0'..'9' || ch in 'A'..'F' }
            if (hex.length == 12) normalizeMacCandidate(hex) else null
        }

    val modelName = modelFromLabel ?: run {
        if (',' in text || ';' in text || '|' in text) {
            text.split(',', ';', '|').map { it.trim() }.firstOrNull { segment ->
                normalizeMacCandidate(segment) == null &&
                    segment.isNotEmpty() &&
                    !segment.contains(':') &&
                    segment.length >= 3
            }
        } else {
            null
        }
    }

    return ParsedDeviceQr(mac = mac, uid = uidFromLabel, modelName = modelName)
}
