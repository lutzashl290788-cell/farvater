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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.farvater.ui.ios.glassRim
import app.farvater.ui.theme.Ios
import kotlinx.coroutines.delay

enum class ConnectMode { Idle, Searching, Connecting, Connected, Error }

@Composable
fun ConnectButton(
    mode: ConnectMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 184.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val dark = Ios.colors.dark
    val rim = glassRim(dark)

    var previous by remember { mutableStateOf(mode) }
    LaunchedEffect(mode) {
        if (previous != mode) {
            when (mode) {
                ConnectMode.Connected -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                ConnectMode.Error -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
                else -> Unit
            }
        }
        previous = mode
    }

    val lamp by animateColorAsState(
        targetValue = when (mode) {
            ConnectMode.Idle -> scheme.surfaceContainerHigh
            ConnectMode.Searching, ConnectMode.Connecting -> scheme.primary
            ConnectMode.Connected -> scheme.tertiary
            ConnectMode.Error -> scheme.error
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "lamp",
    )
    val idleRing = scheme.primary
    val glyph by animateColorAsState(
        targetValue = if (mode == ConnectMode.Idle) scheme.primary else scheme.background,
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
        targetValue = if (mode == ConnectMode.Searching || mode == ConnectMode.Connecting) 1f else 0f,
        animationSpec = tween(450),
        label = "beam",
    )
    var calm by remember { mutableStateOf(false) }
    LaunchedEffect(mode) {
        calm = false
        if (mode == ConnectMode.Connected) {
            delay(PULSE_MS * 3L)
            calm = true
        }
    }
    val ringStrength by animateFloatAsState(
        targetValue = when (mode) {
            ConnectMode.Connected -> if (calm) 0f else 1f
            ConnectMode.Idle -> 0f
            else -> 0.5f
        },
        animationSpec = tween(600),
        label = "rings",
    )
    val spinning = mode == ConnectMode.Searching || mode == ConnectMode.Connecting || beamStrength > 0.01f
    val angle by loop(spinning, 1800, LinearEasing, "angle")
    val pulse by loop(ringStrength > 0.01f, PULSE_MS, FastOutSlowInEasing, "pulse")

    val description = when (mode) {
        ConnectMode.Idle -> "Подключиться"
        ConnectMode.Searching -> "Идёт поиск рабочего узла"
        ConnectMode.Connecting -> "Подключение"
        ConnectMode.Connected -> "Отключиться"
        ConnectMode.Error -> "Повторить подключение"
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

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lamp.copy(alpha = 0.28f), Color.Transparent),
                    radius = radius,
                ),
                radius = radius,
            )

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

            if (mode == ConnectMode.Idle) {
                drawCircle(
                    color = idleRing.copy(alpha = 0.45f),
                    radius = lampRadius + 7.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            val c = center
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(lerp(lamp, Color.White, 0.22f), lamp, lerp(lamp, Color.Black, 0.14f)),
                    center = c + Offset(-lampRadius * 0.35f, -lampRadius * 0.45f),
                    radius = lampRadius * 1.7f,
                ),
                radius = lampRadius,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = c + Offset(0f, lampRadius * 0.62f),
                    radius = lampRadius * 0.62f,
                ),
                radius = lampRadius,
            )
            drawOval(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.30f else 0.55f), Color.Transparent),
                    startY = c.y - lampRadius * 0.94f,
                    endY = c.y - lampRadius * 0.05f,
                ),
                topLeft = Offset(c.x - lampRadius * 0.7f, c.y - lampRadius * 0.94f),
                size = Size(lampRadius * 1.4f, lampRadius * 0.86f),
            )
            drawCircle(
                brush = Brush.linearGradient(
                    rim,
                    start = Offset(c.x - lampRadius, c.y - lampRadius),
                    end = Offset(c.x + lampRadius, c.y + lampRadius),
                ),
                radius = lampRadius - 0.75.dp.toPx(),
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
                        ConnectMode.Searching -> Icons.Rounded.Radar
                        ConnectMode.Error -> Icons.Rounded.ErrorOutline
                        else -> Icons.Rounded.PowerSettingsNew
                    },
                    contentDescription = null,
                    tint = glyph,
                    modifier = Modifier.size(50.dp),
                )
            }
        }
    }
}

private const val PULSE_MS = 2600

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
