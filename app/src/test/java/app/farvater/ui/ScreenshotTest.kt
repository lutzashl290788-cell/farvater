package app.farvater.ui

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.farvater.core.model.NetMode
import app.farvater.core.model.Protocol
import app.farvater.core.model.ProxyNode
import app.farvater.core.model.SourceMode
import app.farvater.data.AppSettings
import app.farvater.data.UiStyle
import app.farvater.engine.TestMethod
import app.farvater.engine.TestResult
import app.farvater.ui.screens.HomeScreen
import app.farvater.ui.screens.OnboardingDialog
import app.farvater.ui.screens.ServersScreen
import app.farvater.ui.screens.SettingsScreen
import app.farvater.ui.screens.SourcesScreen
import app.farvater.ui.theme.FarvaterTheme
import app.farvater.vpn.Traffic
import app.farvater.vpn.VpnState
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi", application = Application::class)
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun homeIdleDark() = shot("01_home_idle_dark", dark = true) { Home(VpnState.Idle) }
    @Test fun homeIdleLight() = shot("01_home_idle_light", dark = false) { Home(VpnState.Idle) }
    @Test fun homeConnectedDark() = shot("02_home_connected_dark", dark = true) { Home(Fake.connected) }
    @Test fun homeConnectedLight() = shot("02_home_connected_light", dark = false) { Home(Fake.connected) }
    @Test fun homeSearchingDark() = shot("03_home_searching_dark", dark = true) {
        Home(VpnState.Idle, Fake.state.copy(progress = TestProgress(48, 120, 9)))
    }
    @Test fun serversDark() = shot("04_servers_dark", dark = true) { Servers() }
    @Test fun serversLight() = shot("04_servers_light", dark = false) { Servers() }
    @Test fun sourcesDark() = shot("05_sources_dark", dark = true) { Sources() }
    @Test fun sourcesLight() = shot("05_sources_light", dark = false) { Sources() }
    @Test fun settingsDark() = shot("06_settings_dark", dark = true) { Settings() }
    @Test fun settingsLight() = shot("06_settings_light", dark = false) { Settings() }
    @Test fun onboardingDark() = shot("07_onboarding_dark", dark = true, screen = true) {
        OnboardingDialog(onChoice = {}, onOpenDocument = {})
    }

    @Test fun materialHomeIdleLight() = shot("11_material_home_idle_light", dark = false, material = true) { Home(VpnState.Idle) }
    @Test fun materialHomeConnectedDark() = shot("12_material_home_connected_dark", dark = true, material = true) { Home(Fake.connected) }
    @Test fun materialServersLight() = shot("14_material_servers_light", dark = false, material = true) { Servers() }
    @Test fun materialServersDark() = shot("14_material_servers_dark", dark = true, material = true) { Servers() }
    @Test fun materialSourcesLight() = shot("15_material_sources_light", dark = false, material = true) { Sources() }
    @Test fun materialSettingsLight() = shot("16_material_settings_light", dark = false, material = true) { Settings() }
    @Test fun materialSettingsDark() = shot("16_material_settings_dark", dark = true, material = true) { Settings() }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shot(name: String, dark: Boolean, screen: Boolean = false, material: Boolean = false, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            FarvaterTheme(
                darkTheme = dark,
                style = if (material) UiStyle.MATERIAL else UiStyle.IOS,
                dynamicColor = false,
            ) { content() }
        }
        rule.mainClock.advanceTimeBy(1_500)
        val path = "build/outputs/roborazzi/$name.png"
        if (screen) captureScreenRoboImage(path) else rule.onRoot().captureRoboImage(path)
    }

    @Composable
    private fun Home(vpn: VpnState, state: UiState = Fake.state) = FarvaterFrame(tab = Tab.Home, onTab = {}) { p ->
        androidx.compose.foundation.layout.Box(Modifier.padding(p)) {
            HomeScreen(
                state = state, vpn = vpn, traffic = Fake.traffic, history = Fake.history,
                onToggle = {}, onFindWorking = {}, onCancelTest = {}, onOpenServers = {},
            )
        }
    }

    @Composable
    private fun Servers() = FarvaterFrame(tab = Tab.Servers, onTab = {}) { p ->
        androidx.compose.foundation.layout.Box(Modifier.padding(p)) {
            ServersScreen(
                state = Fake.state, onSelect = {}, onTestAll = {}, onCancelTest = {},
                onDeleteManual = {}, onCopied = {},
            )
        }
    }

    @Composable
    private fun Sources() = FarvaterFrame(tab = Tab.Sources, onTab = {}) { p ->
        androidx.compose.foundation.layout.Box(Modifier.padding(p)) {
            SourcesScreen(
                state = Fake.state, onRefresh = {}, onToggle = { _, _ -> }, onAddSubscription = {},
                onPaste = {}, onRemoveUserSource = {}, onEnableCommunity = {},
            )
        }
    }

    @Composable
    private fun Settings() = FarvaterFrame(tab = Tab.Settings, onTab = {}) { p ->
        androidx.compose.foundation.layout.Box(Modifier.padding(p)) {
            SettingsScreen(settings = Fake.state.settings, onChange = {}, onOpenDocument = {})
        }
    }
}

private object Fake {
    private fun node(id: String, name: String, protocol: Protocol, params: Map<String, String>, source: String) =
        ProxyNode(id, protocol, name, "203.0.113.${id.length * 7}", 443, "secret", params, "vless://$id", source)

    private val reality = mapOf("security" to "reality", "type" to "tcp")
    val nodes = listOf(
        node("a1", "🇷🇺 Москва, VK Cloud", Protocol.VLESS, reality, "igareck-mobile"),
        node("b22", "🇷🇺 Санкт-Петербург, Selectel", Protocol.VLESS, reality + ("type" to "xhttp"), "zieng2-universal"),
        node("c333", "🇫🇮 Хельсинки", Protocol.VLESS, mapOf("security" to "tls", "type" to "ws"), "rjsxrd-bypass"),
        node("d4444", "🇳🇱 Амстердам", Protocol.TROJAN, mapOf("security" to "tls"), "rkp-wl"),
        node("e55555", "🇩🇪 Франкфурт, Join Telegram @SolVPN", Protocol.VMESS, mapOf("type" to "ws"), "rjsxrd-bypass"),
        node("f666666", "🇹🇷 Стамбул", Protocol.SHADOWSOCKS, mapOf("method" to "chacha20-ietf-poly1305"), "manual"),
        node("g7777777", "🇰🇿 Алматы", Protocol.VLESS, mapOf("security" to "none"), "byewhitelists2"),
        node("h88888888", "🇺🇸 Нью-Йорк", Protocol.VLESS, reality + ("type" to "grpc"), "byewhitelists2"),
    )
    val results = mapOf(
        "a1" to TestResult("a1", 84, TestMethod.REAL),
        "b22" to TestResult("b22", 142, TestMethod.REAL),
        "c333" to TestResult("c333", 388, TestMethod.REAL),
        "d4444" to TestResult("d4444", 912, TestMethod.REAL),
        "e55555" to TestResult("e55555", 96, TestMethod.TCP),
        "g7777777" to TestResult("g7777777", -1, TestMethod.REAL),
    )
    private fun source(
        id: String, title: String, author: String, license: String, count: Int,
        error: String? = null, mode: SourceMode = SourceMode.WHITE,
    ) =
        SourceUi(
            id = id, title = title, author = author, license = license, homepage = "https://example.org",
            description = "Белые списки, проверенные подсети. Обновляется раз в час.", community = true,
            enabled = error == null, nodeCount = count, updatedAt = System.currentTimeMillis() - 23 * 60_000,
            error = error, loading = false, mode = mode, inMode = mode.fits(NetMode.WHITE),
        )
    val sources = listOf(
        source("zieng2-universal", "WL Universal", "zieng2", "не указана", 85),
        source("igareck-mobile", "Белые списки, мобильные", "igareck", "GPL-3.0", 20),
        source("igareck-cidr", "Белые списки, проверенные подсети", "igareck", "GPL-3.0", 0, "в подписке сейчас нет серверов"),
        source("rjsxrd-bypass", "rjsxrd", "whoahaow", "MIT", 475),
        source("rkp-wl", "РКП: белые списки", "RKP", "не указана", 64),
        source("rkp-bl", "РКП: чёрные списки", "RKP", "не указана", 112, mode = SourceMode.BLACK),
        SourceUi(
            id = "user-1", title = "sub.example.net", author = "ваша подписка", license = "",
            homepage = "https://sub.example.net/abc", description = "https://sub.example.net/abc", community = false,
            enabled = true, nodeCount = 12, updatedAt = System.currentTimeMillis() - 3 * 3_600_000, error = null, loading = false,
            intervalHours = 3,
        ),
    )
    val state = UiState(
        settings = AppSettings(onboardingDone = true, communityEnabled = true, acceptedLegalVersion = 1),
        sources = sources,
        nodes = nodes,
        results = results,
        selectedId = "a1",
        netMode = NetMode.WHITE,
        detectedMode = NetMode.WHITE,
        announcements = listOf(
            Announcement("РКП: белые списки", "⚠️ - Конфиг без шифрования трафика.\n🏴 - Конфиг с неизвестным выходным трафиком."),
            Announcement("rjsxrd", "t.me/rjsxrd · 475 configs · last update: 14:27 05/10/2026"),
        ),
    )
    val connected = VpnState.Connected(nodes[0], System.currentTimeMillis() - 754_000)
    val traffic = Traffic(upBps = 18_400, downBps = 1_240_000, upTotal = 4_200_000, downTotal = 96_000_000)
    val history = listOf(120, 340, 900, 1500, 1200, 800, 1900, 2400, 2100, 1300, 1600, 2200, 2600, 1800, 1240)
        .map { it * 1000L }
}
