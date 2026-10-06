package app.farvater.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.farvater.BuildConfig
import app.farvater.R
import app.farvater.data.AppSettings
import app.farvater.engine.TunBridge
import app.farvater.engine.XrayEngine
import app.farvater.ui.LocalBottomInset
import app.farvater.ui.ios.IosButton
import app.farvater.ui.ios.IosButtonStyle
import app.farvater.ui.ios.IosCompactBar
import app.farvater.ui.ios.IosDivider
import app.farvater.ui.ios.IosLargeTitle
import app.farvater.ui.ios.IosRow
import app.farvater.ui.ios.IosSection
import app.farvater.ui.ios.IosSegmented
import app.farvater.ui.ios.IosSpinner
import app.farvater.ui.ios.IosSwitch
import app.farvater.ui.theme.Ios
import app.farvater.ui.theme.IosType
import kotlinx.coroutines.launch

// варианты одновременных проверок
private val ConcurrencyOptions = listOf(8 to "Бережно", 16 to "Обычно", 32 to "Быстро")

@Suppress("DEPRECATION")
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onOpenDocument: (String) -> Unit,
    hwid: String = "",
    onCopied: () -> Unit = {},
    updateVersion: String? = null,
    checkingUpdates: Boolean = false,
    onCheckUpdates: () -> Unit = {},
) {
    val c = Ios.colors
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val collapsed by remember { derivedStateOf { scroll.value > 70 } }
    val divider = 59.dp
    var showApps by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            IosLargeTitle("Настройки")

            IosSection(
                header = "Подключение",
                footer = "Сайты Госуслуг, банков, Яндекса, VK и маркетплейсов открываются напрямую, как без VPN.",
            ) {
                IosRow(
                    title = "Банки и госсервисы",
                    icon = Icons.Rounded.AccountBalance,
                    iconTint = c.green,
                    trailing = { IosSwitch(settings.directRuServices, { v -> onChange { it.copy(directRuServices = v) } }) },
                )
                IosDivider(start = divider)
                IosRow(
                    title = "Приложения мимо VPN",
                    icon = Icons.Rounded.Apps,
                    iconTint = c.indigo,
                    value = if (settings.bypassApps.isEmpty()) "нет" else settings.bypassApps.size.toString(),
                    chevron = true,
                    onClick = { showApps = true },
                )
                IosDivider(start = divider)
                IosRow(
                    title = "Переключаться при сбое",
                    subtitle = "Если узел трижды подряд не ответил",
                    icon = Icons.Rounded.Autorenew,
                    iconTint = c.tint,
                    trailing = { IosSwitch(settings.autoSwitch, { v -> onChange { it.copy(autoSwitch = v) } }) },
                )
            }

            IosSection(
                header = "Безопасность",
                footer = "Владелец сервера видит, на какие сайты вы заходите. Содержимое защищает HTTPS: " +
                    "не вводите пароли на сайтах без замочка и через публичные узлы.",
            ) {
                IosRow(
                    title = "Только защищённые узлы",
                    subtitle = "В публичных подписках скрывать узлы без шифрования, со старым шифрованием или без проверки сертификата",
                    icon = Icons.Rounded.GppGood,
                    iconTint = c.green,
                    trailing = { IosSwitch(settings.safeMode, { v -> onChange { it.copy(safeMode = v) } }) },
                )
                IosDivider(start = divider)
                IosRow(
                    title = "Зашифрованный DNS",
                    subtitle = "Адреса сайтов узнаются по HTTPS, сервер не сможет подменить их",
                    icon = Icons.Rounded.Dns,
                    iconTint = c.blue,
                    trailing = { IosSwitch(settings.encryptedDns, { v -> onChange { it.copy(encryptedDns = v) } }) },
                )
                IosDivider(start = divider)
                IosRow(
                    title = "Прокси под паролем",
                    subtitle = "Другие приложения на телефоне не смогут пользоваться туннелем",
                    icon = Icons.Rounded.Key,
                    iconTint = c.tint,
                    value = "всегда",
                )
            }

            IosSection(
                header = "Подписки",
                footer = "HWID нужен подпискам с лимитом устройств, иначе панель может выдать урезанный список. " +
                    "Это случайный номер этой установки, не связанный с телефоном. Его получают только ваши подписки.",
            ) {
                IosRow(
                    title = "Отправлять HWID",
                    icon = Icons.Rounded.PhoneAndroid,
                    iconTint = c.indigo,
                    trailing = { IosSwitch(settings.sendHwid, { v -> onChange { it.copy(sendHwid = v) } }) },
                )
                IosDivider(start = divider)
                IosRow(
                    title = "HWID",
                    icon = Icons.Rounded.Fingerprint,
                    iconTint = c.gray,
                    value = if (hwid.length > 12) hwid.take(8) + "…" + hwid.takeLast(4) else hwid,
                    onClick = {
                        clipboard.setText(AnnotatedString(hwid))
                        onCopied()
                    },
                )
            }

            IosSection(header = "Проверка узлов", footer = "Быстрая проверка нагружает сеть: оператор может начать сбрасывать соединения.") {
                IosRow(
                    title = "Скрывать неработающие",
                    icon = Icons.Rounded.VisibilityOff,
                    iconTint = c.gray,
                    trailing = { IosSwitch(settings.hideDead, { v -> onChange { it.copy(hideDead = v) } }) },
                )
                IosDivider(start = divider)
                IosRow(title = "Скорость проверки", icon = Icons.Rounded.Speed, iconTint = c.blue)
                val current = ConcurrencyOptions.minBy { kotlin.math.abs(it.first - settings.concurrency) }.first
                IosSegmented(
                    options = ConcurrencyOptions.map { it.second },
                    selected = ConcurrencyOptions.indexOfFirst { it.first == current },
                    onSelect = { i -> onChange { it.copy(concurrency = ConcurrencyOptions[i].first) } },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                )
            }

            IosSection(header = "Защита") {
                var unrestricted by remember { mutableStateOf(context.isBatteryUnrestricted()) }
                LifecycleResumeEffect(Unit) {
                    unrestricted = context.isBatteryUnrestricted()
                    onPauseOrDispose { }
                }
                IosRow(
                    title = "Работа в фоне",
                    subtitle = "Чтобы Android не останавливал VPN. В настройках Фарватера откройте «Батарея» и выберите «Без ограничений»",
                    subtitleLines = 3,
                    icon = Icons.Rounded.BatteryChargingFull,
                    iconTint = c.green,
                    value = if (unrestricted) "разрешена" else "ограничена",
                    chevron = !unrestricted,
                    onClick = { runCatching { context.startActivity(context.appSettingsIntent()) } },
                )
            }

            IosSection(header = "Документы") {
                IosRow(
                    "Условия использования",
                    icon = Icons.Rounded.Description,
                    iconTint = c.gray,
                    chevron = true,
                    onClick = { onOpenDocument(LegalDocs.TERMS) },
                )
                IosDivider(start = divider)
                IosRow(
                    "Политика конфиденциальности",
                    icon = Icons.Rounded.Lock,
                    iconTint = c.gray,
                    chevron = true,
                    onClick = { onOpenDocument(LegalDocs.PRIVACY) },
                )
                IosDivider(start = divider)
                IosRow(
                    "Лицензии открытого ПО",
                    icon = Icons.Rounded.Code,
                    iconTint = c.gray,
                    chevron = true,
                    onClick = { onOpenDocument(LegalDocs.LICENSES) },
                )
            }

            val complete = XrayEngine.isAvailable && TunBridge.isAvailable
            IosSection(
                header = "О приложении",
                footer = if (complete) null else "Неполная сборка: каталог и проверка работают, а подключиться нельзя. Скачайте официальную сборку.",
                footerColor = c.red,
            ) {
                IosRow("Версия", icon = Icons.Rounded.Verified, iconTint = c.tint, value = BuildConfig.VERSION_NAME)
                IosDivider(start = divider)
                IosRow(
                    title = "Обновления",
                    icon = Icons.Rounded.SystemUpdate,
                    iconTint = c.blue,
                    value = when {
                        checkingUpdates -> null
                        updateVersion != null -> "есть $updateVersion"
                        else -> "проверить"
                    },
                    valueColor = if (updateVersion != null) c.blue else null,
                    chevron = !checkingUpdates,
                    onClick = onCheckUpdates,
                    trailing = { if (checkingUpdates) IosSpinner() },
                )
                IosDivider(start = divider)
                IosRow(
                    title = "Проверять автоматически",
                    subtitle = "Уведомлять о новых версиях и срочных исправлениях",
                    icon = Icons.Rounded.NotificationsActive,
                    iconTint = c.red,
                    trailing = { IosSwitch(settings.autoUpdates, { v -> onChange { it.copy(autoUpdates = v) } }) },
                )
                IosDivider(start = divider)
                IosRow(
                    "Ядро Xray",
                    icon = Icons.Rounded.Memory,
                    iconTint = c.purple,
                    value = XrayEngine.version ?: if (XrayEngine.isAvailable) "встроено" else "нет",
                    valueColor = if (XrayEngine.isAvailable) null else c.red,
                )
                IosDivider(start = divider)
                IosRow(
                    "TUN-мост",
                    icon = Icons.Rounded.SwapVert,
                    iconTint = c.teal,
                    value = if (TunBridge.isAvailable) "встроен" else "нет",
                    valueColor = if (TunBridge.isAvailable) null else c.red,
                )
            }
            Spacer(Modifier.height(20.dp + LocalBottomInset.current))
        }
        IosCompactBar("Настройки", visible = collapsed)
    }

    if (showApps) {
        BypassAppsSheet(
            selected = settings.bypassApps,
            onChange = { apps -> onChange { it.copy(bypassApps = apps) } },
            onClose = { showApps = false },
        )
    }
}

// приветствие: лист iOS выезжает снизу
@Composable
fun OnboardingDialog(onChoice: (enableCommunity: Boolean) -> Unit, onOpenDocument: (String) -> Unit) {
    val c = Ios.colors
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, spring(dampingRatio = 0.88f, stiffness = 280f)) }
    fun close(enable: Boolean) {
        scope.launch {
            slide.animateTo(1f, spring(dampingRatio = 1f, stiffness = 500f))
            onChoice(enable)
        }
    }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f * (1f - slide.value)))) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 10.dp)
                    .graphicsLayer { translationY = size.height * slide.value }
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(c.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(36.dp))
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(20.dp)).background(c.tint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_beacon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text("Добро пожаловать\nв Фарватер", style = IosType.title1, color = c.label, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                Feature(Icons.Rounded.Lock, c.green, "Ваши подписки", "VLESS, Trojan, Shadowsocks и VMess. Банки и госсервисы идут мимо VPN.")
                Feature(Icons.Rounded.Public, c.tint, "Публичные подписки", "Бесплатные серверы сообщества помогают при белых списках.")
                Feature(Icons.Rounded.Shield, c.red, "Осторожно с паролями", "Владельцы чужих серверов видят, куда вы ходите. Не вводите через них пароли.")
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Условия",
                        style = IosType.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = c.tint,
                        modifier = Modifier.clickable { onOpenDocument(LegalDocs.TERMS) }.padding(8.dp),
                    )
                    Text(
                        "Конфиденциальность",
                        style = IosType.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = c.tint,
                        modifier = Modifier.clickable { onOpenDocument(LegalDocs.PRIVACY) }.padding(8.dp),
                    )
                }
                Text(
                    "Продолжая, вы принимаете условия использования и политику конфиденциальности.",
                    style = IosType.caption1,
                    color = c.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                IosButton("Включить публичные", onClick = { close(true) }, modifier = Modifier.fillMaxWidth())
                IosButton(
                    "Только мои подписки",
                    onClick = { close(false) },
                    style = IosButtonStyle.Plain,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, tint: Color, title: String, text: String) {
    val c = Ios.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, style = IosType.headline, color = c.label)
            Text(text, style = IosType.subheadline, color = c.secondaryLabel)
        }
    }
}

// система не ограничивает Фарватер в фоне
private fun Context.isBatteryUnrestricted(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

// страница Фарватера в настройках Android, там раздел батареи
private fun Context.appSettingsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
