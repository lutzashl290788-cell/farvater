package app.farvater

import app.farvater.net.ImportLink
import app.farvater.net.SafeHttp
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SecurityTest {
    @Test
    fun localHostsAreRejected() {
        listOf("localhost", "LOCALHOST.", "api.localhost", "127.0.0.1", "127.1.2.3", "0.0.0.0", "::1", "[::1]", "::", "169.254.169.254", "fe80::1", "224.0.0.1")
            .forEach { assertTrue(it, SafeHttp.isLocalHost(it)) }
        listOf("example.com", "1.1.1.1", "192.168.1.10", "2606:4700:4700::1111", "hub.mos.ru")
            .forEach { assertFalse(it, SafeHttp.isLocalHost(it)) }
    }

    @Test
    fun parseRejectsLocalAndOddUrls() {
        assertNull(SafeHttp.parse("http://127.0.0.1:10808/"))
        assertNull(SafeHttp.parse("http://[::1]/sub"))
        assertNull(SafeHttp.parse("file:///sdcard/sub.txt"))
        assertNull(SafeHttp.parse("content://app/sub"))
        assertNull(SafeHttp.parse("https://example.com/" + "a".repeat(5000)))
        assertEquals("example.com", SafeHttp.parse("https://example.com/sub")?.host)
    }

    @Test
    fun redirectsCannotDowngradeOrGoLocal() {
        val from = "https://example.com/sub".toHttpUrl()
        assertThrows(IOException::class.java) { SafeHttp.redirectTarget(from, "http://example.com/sub") }
        assertThrows(IOException::class.java) { SafeHttp.redirectTarget(from, "https://127.0.0.1/sub") }
        assertThrows(IOException::class.java) { SafeHttp.redirectTarget(from, "https://localhost/sub") }
        assertThrows(IOException::class.java) { SafeHttp.redirectTarget(from, null) }
        assertEquals("cdn.example.org", SafeHttp.redirectTarget(from, "https://cdn.example.org/x").host)
        assertEquals("/other", SafeHttp.redirectTarget(from, "/other").encodedPath)
    }

    @Test
    fun importLinksAreFiltered() {
        assertEquals("https://example.com/sub", ImportLink.sanitize(" https://example.com/sub "))
        assertEquals("vless://id@host:443", ImportLink.sanitize("vless://id@host:443"))
        assertNull(ImportLink.sanitize(null))
        assertNull(ImportLink.sanitize(""))
        assertNull(ImportLink.sanitize("file:///data/data/app.farvater/shared_prefs/prefs.xml"))
        assertNull(ImportLink.sanitize("content://com.example/sub"))
        assertNull(ImportLink.sanitize("javascript:alert(1)"))
        assertNull(ImportLink.sanitize("http://localhost:10808/"))
        assertNull(ImportLink.sanitize("vless://a@b:1\nvless://c@d:2"))
        assertNull(ImportLink.sanitize("vless://" + "a".repeat(5000)))
    }
}
