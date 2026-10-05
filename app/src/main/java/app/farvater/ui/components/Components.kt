package app.farvater.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.farvater.core.model.ProxyNode
import app.farvater.engine.TestMethod
import app.farvater.engine.TestResult
import app.farvater.ui.theme.Ios
import app.farvater.ui.theme.IosType
import app.farvater.ui.theme.Numeric

// цвет задержки: зелёный, оранжевый, красный
@Composable
fun delayColor(result: TestResult?): Color {
    val c = Ios.colors
    return when {
        result == null -> c.tertiaryLabel
        !result.alive -> c.red
        result.delayMs <= 350 -> c.green
        result.delayMs <= 900 -> c.tint
        else -> c.red
    }
}

fun delayText(result: TestResult?): String = when {
    result == null -> "—"
    !result.alive -> "нет ответа"
    result.method == TestMethod.TCP -> "${result.delayMs} мс*"
    else -> "${result.delayMs} мс"
}

// задержка узла цветным текстом
@Composable
fun DelayLabel(result: TestResult?, modifier: Modifier = Modifier) {
    val color by animateColorAsState(delayColor(result), label = "delay")
    Text(
        delayText(result),
        style = IosType.subheadline.merge(Numeric).copy(fontWeight = FontWeight.Medium),
        color = color,
        maxLines = 1,
        modifier = modifier,
    )
}

// круглый аватар узла: флаг страны или буквы протокола
@Composable
fun NodeAvatar(flag: String?, node: ProxyNode, size: Dp = 36.dp) {
    val c = Ios.colors
    Box(
        modifier = Modifier.size(size).background(c.fill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (flag != null) {
            Text(flag, fontSize = (size.value * 0.52f).sp)
        } else {
            Text(
                node.protocol.title.take(2).uppercase(),
                style = IosType.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = c.secondaryLabel,
            )
        }
    }
}

// протокол, защита и транспорт одной строкой
fun nodeSummary(node: ProxyNode): String = node.tags.joinToString(" · ")

// компактный график скорости с заливкой
@Composable
fun Sparkline(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = values.max().coerceAtLeast(1).toFloat()
        val step = size.width / (values.size - 1)
        val line = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - (v / max) * size.height * 0.85f
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent), endY = size.height),
            style = Fill,
        )
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val lastY = size.height - (values.last() / max) * size.height * 0.85f
        drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(size.width, lastY))
    }
}

// маленькая плашка-метка
@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = IosType.caption1.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.16f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
