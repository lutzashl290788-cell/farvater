package app.farvater.ui.ios

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.farvater.ui.theme.Ios
import app.farvater.ui.theme.IosMotion
import app.farvater.ui.theme.IosType
import kotlin.math.roundToInt

// нажатие как в iOS: лёгкое сжатие и приглушение
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.iosPress(
    enabled: Boolean = true,
    scaleTo: Float = 0.96f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) scaleTo else 1f,
        spring(dampingRatio = IosMotion.SnappyDamping, stiffness = IosMotion.SnappyStiffness),
        label = "press",
    )
    val alpha by animateFloatAsState(if (pressed) 0.75f else 1f, spring(stiffness = 700f), label = "dim")
    graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

// подсветка строки списка: мгновенно при касании и мягко гаснет
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.iosRowPress(onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val c = Ios.colors
    val bg by animateColorAsState(
        if (pressed) c.highlight else Color.Transparent,
        if (pressed) snap() else tween(320),
        label = "row",
    )
    background(bg).combinedClickable(
        interactionSource = interaction,
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

// крупный заголовок экрана
@Composable
fun IosLargeTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = Ios.colors
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = IosType.largeTitle, color = c.label, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = IosType.subheadline, color = c.secondaryLabel, maxLines = 1)
        }
        trailing()
    }
}

// компактная шапка, проявляется при прокрутке вместо крупного заголовка
@Composable
fun IosCompactBar(title: String, visible: Boolean, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val p by animateFloatAsState(if (visible) 1f else 0f, tween(180), label = "bar")
    if (p == 0f) return
    Column(modifier.fillMaxWidth().graphicsLayer { alpha = p }.background(c.bar)) {
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            Text(
                title,
                style = IosType.headline,
                color = c.label,
                modifier = Modifier.graphicsLayer { translationY = (1f - p) * 8.dp.toPx() },
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = c.separator)
    }
}

// секция сгруппированного списка
@Composable
fun IosSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    footerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ios.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = IosType.footnote,
                color = c.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 7.dp),
            )
        } else {
            Spacer(Modifier.height(20.dp))
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cell),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                style = IosType.footnote,
                color = footerColor ?: c.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
            )
        }
    }
}

// тонкий разделитель с отступом слева
@Composable
fun IosDivider(start: Dp = 16.dp) {
    HorizontalDivider(Modifier.padding(start = start), thickness = 0.5.dp, color = Ios.colors.separator)
}

// цветная квадратная иконка как в «Настройках»
@Composable
fun IosIcon(icon: ImageVector, tint: Color, size: Dp = 29.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)).background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.64f))
    }
}

// строка списка
@Composable
fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    subtitleLines: Int = 2,
    icon: ImageVector? = null,
    iconTint: Color = Ios.colors.tint,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    valueColor: Color? = null,
    chevron: Boolean = false,
    titleColor: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Ios.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.iosRowPress(onLongClick, onClick) else Modifier)
            .heightIn(min = 46.dp)
            .padding(start = 16.dp, end = if (chevron) 10.dp else 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IosIcon(icon, iconTint)
            Spacer(Modifier.width(14.dp))
        }
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = IosType.body, color = titleColor ?: c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = IosType.footnote,
                    color = subtitleColor ?: c.secondaryLabel,
                    maxLines = subtitleLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = IosType.body, color = valueColor ?: c.secondaryLabel, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (chevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = c.tertiaryLabel,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// переключатель iOS: зелёная дорожка и белый бегунок на пружине
@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val progress by animateFloatAsState(
        if (checked) 1f else 0f,
        spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "switch",
    )
    val thumbWidth by animateDpAsState(if (pressed) 33.dp else 27.dp, spring(stiffness = 600f), label = "thumb")
    Box(
        modifier
            .size(51.dp, 31.dp)
            .clip(CircleShape)
            .background(lerp(c.fill, c.green, progress))
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onCheckedChange(it)
                },
            )
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset(((47.dp - thumbWidth).toPx() * progress).roundToInt(), 0) }
                .size(thumbWidth, 27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

// сегментированный выбор с плавающим ползунком
@Composable
fun IosSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(c.fill)
            .padding(2.dp),
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(
            segment * selected,
            spring(dampingRatio = IosMotion.SoftDamping, stiffness = 480f),
            label = "segment",
        )
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(7.dp))
                .background(c.segmentThumb, RoundedCornerShape(7.dp)),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            if (i != selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(i)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = IosType.footnote.copy(fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = c.label,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// поле поиска iOS
@Composable
fun IosSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.fill.copy(alpha = if (c.dark) 0.7f else 1f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = c.secondaryLabel, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = IosType.body, color = c.secondaryLabel, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = IosType.body.copy(color = c.label),
                cursorBrush = SolidColor(c.tint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Cancel,
                contentDescription = "Очистить",
                tint = c.secondaryLabel,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onValueChange("") },
            )
        }
    }
}

// поле ввода для окон
@Composable
fun IosTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(c.cell)
            .border(0.5.dp, c.separator, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, style = IosType.subheadline, color = c.tertiaryLabel, maxLines = 1)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = IosType.subheadline.copy(color = c.label),
            cursorBrush = SolidColor(c.tint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

enum class IosButtonStyle { Filled, Tinted, Plain }

// кнопка iOS: залитая, тонированная или простая
@Composable
fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IosButtonStyle = IosButtonStyle.Filled,
    color: Color = Ios.colors.tint,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val bg = when (style) {
        IosButtonStyle.Filled -> color
        IosButtonStyle.Tinted -> color.copy(alpha = 0.16f)
        IosButtonStyle.Plain -> Color.Transparent
    }
    val fg = if (style == IosButtonStyle.Filled) Color.White else color
    Row(
        modifier
            .heightIn(min = 50.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .iosPress(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = IosType.headline, color = fg, maxLines = 1)
    }
}

// круглая тонированная кнопка для шапки экрана
@Composable
fun IosCircleButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Ios.colors.tint) {
    Box(
        Modifier
            .size(36.dp)
            .iosPress(scaleTo = 0.88f, onClick = onClick)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// индикатор загрузки iOS: восемь лепестков
@Composable
fun IosSpinner(modifier: Modifier = Modifier, size: Dp = 20.dp, color: Color = Ios.colors.secondaryLabel) {
    val step by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "step",
    )
    Canvas(modifier.size(size).rotate(step.toInt() * 45f)) {
        val r = this.size.minDimension / 2
        val stroke = r * 0.22f
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0 - 90)
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            drawLine(
                color = color.copy(alpha = 1f - i * 0.11f),
                start = Offset(center.x + cos * r * 0.45f, center.y + sin * r * 0.45f),
                end = Offset(center.x + cos * (r - stroke / 2), center.y + sin * (r - stroke / 2)),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

// тонкая полоса прогресса
@Composable
fun IosProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Ios.colors.tint) {
    val c = Ios.colors
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(stiffness = 200f), label = "progress")
    Box(modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(c.fill)) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(CircleShape).background(color))
    }
}

enum class AlertRole { Default, Preferred, Cancel, Destructive }

@Immutable
data class AlertAction(val text: String, val role: AlertRole = AlertRole.Default, val onClick: () -> Unit)

// окно в стиле iOS: появляется с лёгким масштабом
@Composable
fun IosAlert(
    title: String,
    onDismiss: () -> Unit,
    actions: List<AlertAction>,
    message: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val c = Ios.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 520f)) }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (c.dark) c.cellRaised else Color(0xFFF2F2F2),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .graphicsLayer {
                    val s = 1.12f - 0.12f * appear.value
                    scaleX = s
                    scaleY = s
                    alpha = appear.value.coerceIn(0f, 1f)
                },
        ) {
            Column {
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 19.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(title, style = IosType.headline, color = c.label, textAlign = TextAlign.Center)
                    if (message != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(message, style = IosType.footnote, color = c.label, textAlign = TextAlign.Center)
                    }
                    if (content != null) {
                        Spacer(Modifier.height(12.dp))
                        content()
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                if (actions.size == 2) {
                    Row(Modifier.height(46.dp)) {
                        AlertButton(actions[0], Modifier.weight(1f).fillMaxHeight())
                        Box(Modifier.width(0.5.dp).fillMaxHeight().background(c.separator))
                        AlertButton(actions[1], Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    actions.forEachIndexed { i, action ->
                        if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                        AlertButton(action, Modifier.fillMaxWidth().height(46.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertButton(action: AlertAction, modifier: Modifier) {
    val c = Ios.colors
    Box(modifier.iosRowPress(onClick = action.onClick), contentAlignment = Alignment.Center) {
        Text(
            action.text,
            style = IosType.body.copy(
                fontWeight = if (action.role == AlertRole.Preferred || action.role == AlertRole.Cancel) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (action.role == AlertRole.Destructive) c.red else c.blue,
            maxLines = 1,
        )
    }
}

// панель-«стекло» для плавающих элементов
@Composable
fun IosGlass(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = CircleShape, content: @Composable () -> Unit) {
    val c = Ios.colors
    Surface(
        modifier = modifier,
        shape = shape,
        color = c.bar,
        border = BorderStroke(0.5.dp, c.barBorder),
        shadowElevation = 16.dp,
        content = content,
    )
}
