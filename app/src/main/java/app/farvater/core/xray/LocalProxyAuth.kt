package app.farvater.core.xray

import java.security.SecureRandom

object LocalProxyAuth {
    private val random = SecureRandom()

    val user: String = token(12)
    val pass: String = token(24)

    private fun token(length: Int): String {
        val alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(length) { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }
}
