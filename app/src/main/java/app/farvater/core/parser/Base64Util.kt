package app.farvater.core.parser

import java.util.Base64

internal object Base64Util {
    // декодирует обычный и url-safe base64, иначе null
    fun decodeToString(input: String): String? {
        val clean = input.filterNot { it.isWhitespace() }
        if (clean.isEmpty()) return null
        val normalized = clean.replace('-', '+').replace('_', '/').trimEnd('=')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return runCatching { String(Base64.getDecoder().decode(padded), Charsets.UTF_8) }.getOrNull()
    }
}
