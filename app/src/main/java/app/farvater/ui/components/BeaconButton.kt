package app.farvater.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class BeaconMode { Idle, Searching, Connecting, Connected, Error }

// кнопка-маяк
@Composable
fun BeaconButton(
    mode: BeaconMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 208.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current

    var previous by remember { mutableStateOf(mode) }
    LaunchedEffect(mode) {
        if (previous != mode) {
            when (mode) {
                BeaconMode.Connected -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                BeaconMode.Error -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
                else -> Unit
            }
        }
        previous = mode
    }

    val lamp by animateColorAsState(
        targetValue = when (mode) {
            BeaconMode.Idle -> scheme.surfaceContainerHigh
            BeaconMode.Searching, BeaconMode.Connecting -> scheme.primary
            BeaconMode.Connected -> scheme.tertiary
            BeaconMode.Error -> scheme.error
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "lamp",
    )
    val idleRing = scheme.primary
    val glyph by animateColorAsState(
        targetValue = if (mode == BeaconMode.Idle) scheme.primary else scheme.background,
        label = "glyph",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )

    val beamStrength by animateFloatAsState(
        targetValue = if (mode == BeaconMode.Searching || mode == BeaconMode.Connecting) 1f else 0f,
        animationSpec = tween(450),
        label = "beam",
    )
    // после подключения круги расходятся несколько раз и затихают, чтобы экран не перерисовывался без конца
    var calm by remember { mutableStateOf(false) }
    LaunchedEffect(mode) {
        calm = false
        if (mode == BeaconMode.Connected) {
            delay(PULSE_MS * 3L)
            calm = true
        }
    }
    val ringStrength by animateFloatAsState(
        targetValue = when (mode) {
            BeaconMode.Connected -> if (calm) 0f else 1f
            BeaconMode.Idle -> 0f
            else -> 0.5f
        },
        animationSpec = tween(600),
        label = "rings",
    )
    // бесконечные анимации крутятся, только пока их видно
    val spinning = mode == BeaconMode.Searching || mode == BeaconMode.Connecting || beamStrength > 0.01f
    val angle by loop(spinning, 1800, LinearEasing, "angle")
    val pulse by loop(ringStrength > 0.01f, PULSE_MS, FastOutSlowInEasing, "pulse")

    val description = when (mode) {
        BeaconMode.Idle -> "Подключиться"
        BeaconMode.Searching -> "Идёт поиск рабочего узла"
        BeaconMode.Connecting -> "Подключение"
        BeaconMode.Connected -> "Отключиться"
        BeaconMode.Error -> "Повторить подключение"
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val radius = this.size.minDimension / 2
            val lampRadius = radius * 0.6f

            // свечение лампы
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lamp.copy(alpha = 0.28f), Color.Transparent),
                    radius = radius,
                ),
                radius = radius,
            )

            // расходящиеся круги
            if (ringStrength > 0f) {
                for (i in 0..1) {
                    val p = (pulse + i * 0.5f) % 1f
                    drawCircle(
                        color = lamp.copy(alpha = (1f - p) * 0.45f * ringStrength),
                        radius = lampRadius + (radius - lampRadius) * p,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            // вращающийся луч
            if (beamStrength > 0f) {
                rotate(angle * 360f) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.80f to Color.Transparent,
                            0.97f to lamp.copy(alpha = 0.6f * beamStrength),
                            1f to Color.Transparent,
                        ),
                        radius = radius,
                    )
                }
            }

            // лампа погашена: тонкое янтарное кольцо зовёт нажать
            if (mode == BeaconMode.Idle) {
                drawCircle(
                    color = idleRing.copy(alpha = 0.45f),
                    radius = lampRadius + 7.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            drawCircle(color = lamp, radius = lampRadius)
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = lampRadius,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        Box(
            modifier = Modifier
                .size(size * 0.6f)
                .clip(CircleShape)
                .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.75f)) togetherWith
                        (fadeOut(tween(140)) + scaleOut(targetScale = 1.15f))
                },
                label = "glyph",
            ) { m ->
                Icon(
                    imageVector = when (m) {
                        BeaconMode.Searching -> Icons.Rounded.Radar
                        BeaconMode.Error -> Icons.Rounded.ErrorOutline
                        else -> Icons.Rounded.PowerSettingsNew
                    },
                    contentDescription = null,
                    tint = glyph,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
    }
}

private const val PULSE_MS = 2600

// повторяющееся значение от 0 до 1, без анимации возвращает 0
@Composable
private fun loop(active: Boolean, durationMs: Int, easing: Easing, label: String): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = easing), RepeatMode.Restart),
        label = label,
    )
}

