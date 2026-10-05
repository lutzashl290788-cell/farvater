package app.farvater.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.farvater.ui.ios.IosGlass
import app.farvater.ui.theme.Ios
import app.farvater.ui.theme.IosType

internal enum class Tab(val title: String, val icon: ImageVector, val iconIdle: ImageVector) {
    Home("Фарватер", Icons.Rounded.Home, Icons.Outlined.Home),
    Servers("Узлы", Icons.Rounded.Dns, Icons.Outlined.Dns),
    Sources("Источники", Icons.Rounded.Layers, Icons.Outlined.Layers),
    Settings("Настройки", Icons.Rounded.Settings, Icons.Outlined.Settings),
}

// сколько места снизу занимает панель вкладок, экраны добавляют его к своему отступу
val LocalBottomInset = staticCompositionLocalOf { 0.dp }

private val BarHeight = 62.dp
private val BarGap = 10.dp

// оболочка с плавающей панелью вкладок
@Composable
internal fun FarvaterFrame(
    tab: Tab,
    onTab: (Tab) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit,
) {
    val c = Ios.colors
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset: Dp = navInset + BarHeight + BarGap * 2
    // Surface задаёт цвет текста по умолчанию под тему
    Surface(color = c.background, contentColor = c.label, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalBottomInset provides bottomInset) {
                content(WindowInsets.statusBars.asPaddingValues())
            }
            // контент мягко уходит под панель
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomInset + 12.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, c.background.copy(alpha = 0.9f)))),
            )
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset)) { data ->
                IosGlass(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        data.visuals.message,
                        style = IosType.subheadline,
                        color = c.label,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    )
                }
            }
            TabBar(
                current = tab,
                onTab = onTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = navInset + BarGap),
            )
        }
    }
}

// панель вкладок как в iOS 26: стеклянная капсула и скользящая подсветка
@Composable
private fun TabBar(current: Tab, onTab: (Tab) -> Unit, modifier: Modifier) {
    val c = Ios.colors
    val haptics = LocalHapticFeedback.current
    IosGlass(modifier = modifier.fillMaxWidth().height(BarHeight)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val item = maxWidth / Tab.entries.size
            val lensX by animateDpAsState(
                item * current.ordinal,
                spring(dampingRatio = 0.72f, stiffness = 380f),
                label = "lens",
            )
            Box(
                Modifier
                    .offset { IntOffset(lensX.roundToPx(), 0) }
                    .width(item)
                    .fillMaxHeight()
                    .background(c.fill.copy(alpha = if (c.dark) 0.75f else 0.85f), CircleShape),
            )
            Row(Modifier.fillMaxSize()) {
                Tab.entries.forEach { t ->
                    TabItem(t, selected = t == current, modifier = Modifier.weight(1f)) {
                        if (t != current) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onTab(t)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Ios.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tint by animateColorAsState(if (selected) c.tint else c.label, spring(stiffness = 500f), label = "tint")
    val scale by animateFloatAsState(
        when {
            pressed -> 0.86f
            selected -> 1f
            else -> 0.96f
        },
        spring(dampingRatio = 0.5f, stiffness = 520f),
        label = "scale",
    )
    Column(
        modifier
            .fillMaxHeight()
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            if (selected) tab.icon else tab.iconIdle,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            tab.title,
            style = IosType.caption2.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = tint,
            maxLines = 1,
        )
    }
}
