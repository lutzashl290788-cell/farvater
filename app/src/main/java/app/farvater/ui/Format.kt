package app.farvater.ui

import java.util.Locale

fun formatSpeed(bytesPerSecond: Long): String = formatBytes(bytesPerSecond) + "/с"

fun formatBytes(bytes: Long): String {
    val b = bytes.coerceAtLeast(0).toDouble()
    return when {
        b < 1024 -> "${b.toLong()} Б"
        b < 1024 * 1024 -> String.format(Locale.US, "%.0f КБ", b / 1024)
        b < 1024.0 * 1024 * 1024 -> String.format(Locale.US, "%.1f МБ", b / 1024 / 1024)
        else -> String.format(Locale.US, "%.2f ГБ", b / 1024 / 1024 / 1024)
    }
}

fun formatDuration(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

fun formatAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0) return "ещё не загружался"
    val minutes = (now - timestamp) / 60_000
    return when {
        minutes < 1 -> "обновлён только что"
        minutes < 60 -> "обновлён $minutes мин назад"
        minutes < 60 * 24 -> "обновлён ${minutes / 60} ч назад"
        else -> "обновлён ${minutes / 60 / 24} дн назад"
    }
}

private val LeadingFlag = Regex("""^\s*([\x{1F1E6}-\x{1F1FF}]{2})\s*""")

fun splitFlag(name: String): Pair<String?, String> {
    val match = LeadingFlag.find(name) ?: return null to name.trim()
    return match.groupValues[1] to name.substring(match.range.last + 1).trim().ifBlank { name.trim() }
}
