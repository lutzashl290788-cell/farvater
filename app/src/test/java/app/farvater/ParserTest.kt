package app.farvater

import app.farvater.core.model.Protocol
import app.farvater.core.parser.LinkParser
import app.farvater.core.parser.SubscriptionParser
import app.farvater.core.xray.XrayConfigBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class ParserTest {

    private val vless = "vless://11111111-2222-3333-4444-555555555555@95.163.1.2:443" +
        "?security=reality&sni=ok.ru&pbk=AbC-dEf_123&sid=ab12&fp=chrome&type=xhttp&path=%2Fapi&flow=" +
        "#%F0%9F%87%B7%F0%9F%87%BA%20Moscow%201"

    @Test fun vlessReality() {
        val n = LinkParser.parse(vless)!!
        assertEquals(Protocol.VLESS, n.protocol)
        assertEquals("95.163.1.2", n.address)
        assertEquals(443, n.port)
        assertEquals("reality", n.security)
        assertEquals("xhttp", n.transport)
        assertEquals("/api", n.params["path"])
        assertEquals("AbC-dEf_123", n.params["pbk"])
        assertTrue(n.name.endsWith("Moscow 1"))
    }

    @Test fun sameServerDifferentNameHasSameId() {
        val a = LinkParser.parse(vless)!!
        val b = LinkParser.parse(vless.substringBefore('#') + "#other")!!
        assertEquals(a.id, b.id)
    }

    @Test fun ipv6Host() {
        val n = LinkParser.parse("trojan://pass@[2001:db8::1]:8443?sni=vk.com#t")!!
        assertEquals("2001:db8::1", n.address)
        assertEquals(8443, n.port)
    }

    @Test fun vmessBase64() {
        val json = """{"v":"2","ps":"vm","add":"1.2.3.4","port":"8080","id":"abc","net":"ws","path":"/ws","tls":"tls","sni":"ya.ru"}"""
        val link = "vmess://" + Base64.getEncoder().encodeToString(json.toByteArray())
        val n = LinkParser.parse(link)!!
        assertEquals(Protocol.VMESS, n.protocol)
        assertEquals(8080, n.port)
        assertEquals("ws", n.transport)
        assertEquals("tls", n.security)
    }

    @Test fun shadowsocksSip002() {
        val user = Base64.getUrlEncoder().withoutPadding().encodeToString("chacha20-ietf-poly1305:secret".toByteArray())
        val n = LinkParser.parse("ss://$user@5.6.7.8:8388#ss")!!
        assertEquals("chacha20-ietf-poly1305", n.params["method"])
        assertEquals("secret", n.secret)
    }

    @Test fun garbageIsNull() {
        assertNull(LinkParser.parse("vless://broken"))
        assertNull(LinkParser.parse("https://example.com"))
    }

    @Test fun base64SubscriptionWithMetaAndStub() {
        val body = "#profile-title: base64:" + Base64.getEncoder().encodeToString("Тест".toByteArray()) + "\n" +
            "#profile-update-interval: 2\n" +
            "vless://x@0.0.0.0:1#Подписка устарела\n" + vless + "\n" + vless + "\n"
        val encoded = Base64.getEncoder().encodeToString(body.toByteArray())
        val s = SubscriptionParser.parse(encoded, "src")
        assertEquals("Тест", s.title)
        assertEquals(2, s.updateIntervalHours)
        assertEquals(1, s.nodes.size)
        assertEquals(listOf("Подписка устарела"), s.notices)
    }

    @Test fun multilineAnnounceIsKeptWhole() {
        val body = "#profile-title: РКП\n#announce: ⚠️ - Конфиг без шифрования трафика.\n" +
            "#announce: 🏴 - Конфиг с неизвестным выходным трафиком.\n" + vless + "\n"
        val s = SubscriptionParser.parse(body, "src")
        assertEquals("⚠️ - Конфиг без шифрования трафика.\n🏴 - Конфиг с неизвестным выходным трафиком.", s.announce)
    }

    @Test fun announceHeaderWinsOverBody() {
        val s = SubscriptionParser.parse("#announce: из тела\n$vless", "src", mapOf("Announce" to "из заголовка"))
        assertEquals("из заголовка", s.announce)
    }

    @Test fun xrayConfigProtectsLocalProxyAndDns() {
        val cfg = XrayConfigBuilder.build(LinkParser.parse(vless)!!, directRuServices = false)!!
        assertTrue(cfg.contains("\"auth\":\"password\""))
        assertTrue(cfg.contains(app.farvater.core.xray.LocalProxyAuth.pass))
        assertTrue(cfg.contains("https://1.1.1.1/dns-query"))
        assertTrue(cfg.contains("\"dns-out\""))
        val plain = XrayConfigBuilder.build(LinkParser.parse(vless)!!, directRuServices = false, encryptedDns = false)!!
        assertTrue(!plain.contains("dns-out"))
    }

    @Test fun insecureNodesAreRecognised() {
        assertEquals("Без шифрования", LinkParser.parse("vless://id@1.2.3.4:443?security=none#x")!!.securityIssue)
        assertEquals("Без проверки сертификата", LinkParser.parse("trojan://p@1.2.3.4:443?allowInsecure=1#x")!!.securityIssue)
        val weak = Base64.getEncoder().encodeToString("rc4-md5:pw".toByteArray())
        assertEquals("Устаревшее шифрование", LinkParser.parse("ss://$weak@1.2.3.4:8388#x")!!.securityIssue)
        assertNull(LinkParser.parse(vless)!!.securityIssue)
    }

    @Test fun unknownSchemesAreCounted() {
        val s = SubscriptionParser.parse("$vless\ntuic://a@1.2.3.4:443#t\ntuic://b@1.2.3.5:443#t\nwireguard://x@1.2.3.6:51820", "src")
        assertEquals(1, s.nodes.size)
        assertEquals(mapOf("tuic" to 2, "wireguard" to 1), s.skipped)
    }

    @Test fun xrayConfigHasRealityAndXhttp() {
        val cfg = XrayConfigBuilder.build(LinkParser.parse(vless)!!, directRuServices = true)
        assertNotNull(cfg)
        assertTrue(cfg!!.contains("\"realitySettings\""))
        assertTrue(cfg.contains("\"xhttpSettings\""))
        assertTrue(cfg.contains("domain:gosuslugi.ru"))
    }
}
