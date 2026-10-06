package app.farvater.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.farvater.core.model.SourceMode
import app.farvater.ui.SourceUi
import app.farvater.ui.ios.IosDivider
import app.farvater.ui.ios.IosRow
import app.farvater.ui.ios.IosSection
import app.farvater.ui.ios.IosSegmented
import app.farvater.ui.theme.Ios
import app.farvater.ui.theme.IosType
import kotlinx.coroutines.launch

private val Modes = listOf(SourceMode.ANY, SourceMode.WHITE, SourceMode.BLACK)

private val Intervals = listOf(
    null to "Авто",
    1 to "Каждый час",
    3 to "Каждые 3 часа",
    6 to "Каждые 6 часов",
    12 to "Каждые 12 часов",
    24 to "Раз в сутки",
    0 to "Только вручную",
)

fun modeShort(mode: SourceMode): String = when (mode) {
    SourceMode.ANY -> "БС и ЧС"
    SourceMode.WHITE -> "БС"
    SourceMode.BLACK -> "ЧС"
}

fun intervalLabel(source: SourceUi): String = when (val h = source.intervalHours) {
    0 -> "вручную"
    null -> "авто, раз в ${hours(source.ownIntervalHours?.takeIf { it > 0 } ?: 6)}"
    else -> "раз в ${hours(h)}"
}

private fun hours(h: Int): String = when {
    h == 1 -> "час"
    h == 24 -> "сутки"
    h % 10 in 2..4 && h % 100 !in 12..14 -> "$h часа"
    else -> "$h часов"
}

@Composable
fun SourceSheet(
    source: SourceUi,
    status: String,
    onMode: (SourceMode) -> Unit,
    onInterval: (Int?) -> Unit,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
) {
    val c = Ios.colors
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 300f)) }
    val close: () -> Unit = {
        scope.launch {
            slide.animateTo(1f, spring(dampingRatio = 1f, stiffness = 520f))
            onClose()
        }
    }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f * (1f - slide.value)))) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 10.dp)
                    .graphicsLayer { translationY = size.height * slide.value }
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(c.background),
            ) {
                Box(Modifier.fillMaxWidth().height(52.dp)) {
                    Text(
                        source.title,
                        style = IosType.headline,
                        color = c.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp),
                    )
                    Text(
                        "Готово",
                        style = IosType.body.copy(fontWeight = FontWeight.SemiBold),
                        color = c.tint,
                        modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = close).padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 40.dp),
                ) {
                    Text(
                        status,
                        style = IosType.footnote,
                        color = c.secondaryLabel,
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 14.dp),
                    )

                    IosSection(
                        header = "Режим сети",
                        footer = "БС — белые списки, когда работают только разрешённые сайты. ЧС — обычные блокировки. Подписка работает только в выбранном режиме.",
                    ) {
                        IosSegmented(
                            options = Modes.map(::modeShort),
                            selected = Modes.indexOf(source.mode),
                            onSelect = { onMode(Modes[it]) },
                            modifier = Modifier.padding(12.dp),
                        )
                    }

                    IosSection(
                        header = "Обновлять",
                        footer = "Авто — как просит сама подписка, а если она молчит, раз в 6 часов.",
                    ) {
                        Intervals.forEachIndexed { i, (value, label) ->
                            if (i > 0) IosDivider()
                            IosRow(
                                title = label,
                                onClick = { onInterval(value) },
                                trailing = {
                                    if (source.intervalHours == value) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Выбрано", tint = c.tint, modifier = Modifier.size(20.dp))
                                    }
                                },
                            )
                        }
                    }

                    IosSection {
                        IosRow(
                            title = "Обновить сейчас",
                            titleColor = c.tint,
                            icon = Icons.Rounded.Refresh,
                            iconTint = c.tint,
                            onClick = onRefresh,
                        )
                        if (source.community) {
                            IosDivider(start = 59.dp)
                            IosRow(
                                title = "Страница проекта",
                                titleColor = c.tint,
                                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                                iconTint = c.blue,
                                onClick = { runCatching { uri.openUri(source.homepage) } },
                            )
                        } else {
                            IosDivider(start = 59.dp)
                            IosRow(
                                title = "Удалить подписку",
                                titleColor = c.red,
                                icon = Icons.Rounded.DeleteOutline,
                                iconTint = c.red,
                                onClick = onRemove,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
