package app.farvater.net

object ImportLink {
    private val schemes = setOf("http", "https", "vless", "vmess", "trojan", "ss", "hysteria2", "hy2")

    fun sanitize(raw: String?): String? {
        val text = raw?.trim() ?: return null
        if (text.isEmpty() || text.length > SafeHttp.MAX_URL_LENGTH) return null
        if (text.any { it == '\n' || it == '\r' || it.isISOControl() }) return null
        val scheme = text.substringBefore("://", "").lowercase()
        if (scheme !in schemes) return null
        if (scheme.startsWith("http") && SafeHttp.parse(text) == null) return null
        return text
    }
}
