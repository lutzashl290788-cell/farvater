package app.farvater.core.parser

import app.farvater.core.model.ProxyNode

data class ParsedSubscription(
    val title: String?,
    val updateIntervalHours: Int?,
    val announce: String?,
    val nodes: List<ProxyNode>,
    // заглушки с адресом 0.0.0.0 показываются как объявления
    val notices: List<String>,
    // строки со ссылками, которые Фарватер пока не умеет разбирать: схема и сколько раз встретилась
    val skipped: Map<String, Int> = emptyMap(),
)

object SubscriptionParser {

    fun parse(body: String, sourceId: String, headers: Map<String, String> = emptyMap()): ParsedSubscription {
        var text = body.trim().removePrefix("\uFEFF")
        if (!text.contains("://")) {
            Base64Util.decodeToString(text)?.takeIf { it.contains("://") }?.let { text = it }
        }

        // заголовки HTTP важнее строк в теле
        val meta = headers.mapKeys { it.key.lowercase() }.toMutableMap()
        val nodes = LinkedHashMap<String, ProxyNode>()
        val notices = mutableListOf<String>()
        val skipped = HashMap<String, Int>()
        // объявление может занимать несколько строк
        val announceLines = mutableListOf<String>()

        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            if (line.startsWith("#")) {
                val kv = line.removePrefix("#").trim()
                val key = kv.substringBefore(':', "").trim().lowercase()
                if (key == "announce") {
                    maybeBase64(kv.substringAfter(':').trim()).takeIf { it.isNotBlank() }?.let(announceLines::add)
                } else if (key.isNotEmpty() && ' ' !in key) {
                    meta.putIfAbsent(key, kv.substringAfter(':').trim())
                }
                return@forEach
            }
            val node = LinkParser.parse(line, sourceId)
            if (node == null) {
                val scheme = line.substringBefore("://", "").lowercase()
                if (scheme.isNotEmpty() && scheme.length <= 16) skipped[scheme] = (skipped[scheme] ?: 0) + 1
                return@forEach
            }
            if (node.address == "0.0.0.0" || node.address == "127.0.0.1") {
                notices += node.name
                return@forEach
            }
            nodes.putIfAbsent(node.id, node)
        }

        return ParsedSubscription(
            title = meta["profile-title"]?.let(::maybeBase64),
            updateIntervalHours = meta["profile-update-interval"]?.trim()?.toIntOrNull(),
            announce = (meta["announce"]?.let(::maybeBase64) ?: announceLines.joinToString("\n")).takeIf { it.isNotBlank() },
            nodes = nodes.values.toList(),
            notices = notices,
            skipped = skipped,
        )
    }

    private fun maybeBase64(value: String): String =
        if (value.startsWith("base64:")) Base64Util.decodeToString(value.removePrefix("base64:")) ?: value else value
}
