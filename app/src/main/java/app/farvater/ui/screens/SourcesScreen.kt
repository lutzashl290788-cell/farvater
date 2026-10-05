package app.farvater.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import app.farvater.ui.LocalBottomInset
import app.farvater.ui.SourceUi
import app.farvater.ui.UiState
import app.farvater.ui.formatAgo
import app.farvater.ui.ios.AlertAction
import app.farvater.ui.ios.AlertRole
import app.farvater.ui.ios.IosAlert
import app.farvater.ui.ios.IosCircleButton
import app.farvater.ui.ios.IosCompactBar
import app.farvater.ui.ios.IosDivider
import app.farvater.ui.ios.IosLargeTitle
import app.farvater.ui.ios.IosRow
import app.farvater.ui.ios.IosSection
import app.farvater.ui.ios.IosSpinner
import app.farvater.ui.ios.IosSwitch
import app.farvater.ui.ios.IosTextField
import app.farvater.ui.theme.Ios

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun SourcesScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onAddSubscription: (String) -> Unit,
    onPaste: (String) -> Unit,
    onRemoveUserSource: (String) -> Unit,
    onEnableCommunity: () -> Unit,
    onRefreshSource: (String) -> Unit = {},
) {
    val c = Ios.colors
    var showAdd by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<SourceUi?>(null) }
    var menuFor by remember { mutableStateOf<SourceUi?>(null) }
    val uri = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val community = state.sources.filter { it.community }
    val own = state.sources.filterNot { it.community }
    val list = rememberLazyListState()
    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 70 } }

    // потянуть список вниз, чтобы обновить все подписки
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp + LocalBottomInset.current),
        ) {
            item(key = "title") {
                val total = state.sources.filter { it.enabled }.sumOf { it.nodeCount }
                IosLargeTitle("Источники", subtitle = "Узлов из включённых: $total") {
                    if (state.refreshing) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { IosSpinner() }
                    } else {
                        IosCircleButton(Icons.Rounded.Refresh, "Обновить все", onRefresh)
                    }
                }
            }

            item(key = "add") {
                IosSection {
                    IosRow(
                        title = "Добавить подписку",
                        titleColor = c.tint,
                        icon = Icons.Rounded.Add,
                        iconTint = c.tint,
                        onClick = { showAdd = true },
                    )
                    IosDivider(start = 59.dp)
                    IosRow(
                        title = "Вставить из буфера",
                        titleColor = c.tint,
                        icon = Icons.Rounded.ContentPaste,
                        iconTint = c.blue,
                        onClick = { onPaste(clipboard.getText()?.text.orEmpty()) },
                    )
                }
            }

            if (own.isNotEmpty()) {
                item(key = "own") {
                    IosSection(header = "Мои подписки") {
                        own.forEachIndexed { i, source ->
                            if (i > 0) IosDivider(start = 59.dp)
                            IosRow(
                                title = source.title,
                                subtitle = statusText(source),
                                subtitleColor = statusColor(source),
                                subtitleLines = 3,
                                icon = Icons.Rounded.Link,
                                iconTint = c.indigo,
                                chevron = !source.loading,
                                onClick = { menuFor = source },
                                trailing = { if (source.loading) IosSpinner() },
                            )
                        }
                    }
                }
            }

            item(key = "community") {
                if (!state.settings.communityEnabled) {
                    IosSection(
                        header = "Публичные подписки",
                        footer = "Это бесплатные чужие серверы. Удобно при белых списках, но для паролей и банков они не годятся.",
                    ) {
                        IosRow(
                            title = "Включить публичные подписки",
                            titleColor = c.tint,
                            icon = Icons.Rounded.Public,
                            iconTint = c.tint,
                            onClick = onEnableCommunity,
                        )
                    }
                } else {
                    IosSection(
                        header = "Публичные подписки",
                        footer = "Фарватер хранит только ссылки. Серверы принадлежат авторам, поддержите их на страницах проектов.",
                    ) {
                        community.forEachIndexed { i, source ->
                            if (i > 0) IosDivider(start = 59.dp)
                            CommunityRow(source, onToggle) { menuFor = source }
                        }
                    }
                }
            }
        }
        IosCompactBar("Источники", visible = collapsed)
    }

    menuFor?.let { source ->
        IosAlert(
            title = source.title,
            message = statusText(source),
            onDismiss = { menuFor = null },
            actions = buildList {
                add(AlertAction("Обновить", AlertRole.Preferred) {
                    onRefreshSource(source.id)
                    menuFor = null
                })
                if (source.community) {
                    add(AlertAction("Страница проекта") {
                        runCatching { uri.openUri(source.homepage) }
                        menuFor = null
                    })
                } else {
                    add(AlertAction("Удалить", AlertRole.Destructive) {
                        removing = source
                        menuFor = null
                    })
                }
                add(AlertAction("Отмена", AlertRole.Cancel) { menuFor = null })
            },
        )
    }

    if (showAdd) {
        var url by remember { mutableStateOf("") }
        IosAlert(
            title = "Добавить подписку",
            message = "Вставьте ссылку, которую дал ваш провайдер или автор подписки.",
            onDismiss = { showAdd = false },
            content = { IosTextField(url, { url = it }, "https://…") },
            actions = listOf(
                AlertAction("Отмена", AlertRole.Default) { showAdd = false },
                AlertAction("Добавить", AlertRole.Preferred) {
                    if (url.isNotBlank()) onAddSubscription(url)
                    showAdd = false
                },
            ),
        )
    }

    removing?.let { source ->
        IosAlert(
            title = "Удалить подписку?",
            message = source.homepage,
            onDismiss = { removing = null },
            actions = listOf(
                AlertAction("Отмена", AlertRole.Cancel) { removing = null },
                AlertAction("Удалить", AlertRole.Destructive) {
                    onRemoveUserSource(source.id)
                    removing = null
                },
            ),
        )
    }
}

@Composable
private fun CommunityRow(source: SourceUi, onToggle: (String, Boolean) -> Unit, onOpen: () -> Unit) {
    val c = Ios.colors
    val alpha by animateFloatAsState(if (source.enabled) 1f else 0.55f, tween(220), label = "dim")
    IosRow(
        title = source.title,
        subtitle = "${source.author} · ${source.license}\n" + statusText(source),
        subtitleColor = if (source.error != null && source.nodeCount == 0 && source.enabled) c.red else null,
        subtitleLines = 4,
        icon = Icons.Rounded.Public,
        iconTint = if (source.enabled) c.tint else c.gray,
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
        onClick = onOpen,
        trailing = {
            if (source.loading) {
                Box(Modifier.size(51.dp, 31.dp), contentAlignment = Alignment.Center) { IosSpinner() }
            } else {
                IosSwitch(checked = source.enabled, onCheckedChange = { onToggle(source.id, it) })
            }
        },
    )
}

// строка состояния источника
private fun statusText(source: SourceUi): String = when {
    source.loading -> "Обновляю…"
    source.error != null && source.nodeCount > 0 -> "Из кэша: ${source.nodeCount} узлов, ${source.error}"
    source.error != null -> source.error.replaceFirstChar { it.uppercase() }
    source.updatedAt > 0 -> "${source.nodeCount} узлов, ${formatAgo(source.updatedAt).removePrefix("обновлён ")}"
    else -> formatAgo(0).replaceFirstChar { it.uppercase() }
} + (source.hidden?.let { "\nСкрыто: $it" } ?: "")

@Composable
private fun statusColor(source: SourceUi): Color? {
    val c = Ios.colors
    return when {
        source.error != null && source.nodeCount == 0 -> c.red
        else -> null
    }
}
