package app.farvater.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.farvater.ui.theme.Ios
import app.farvater.ui.theme.IosType
import kotlinx.coroutines.launch

// документы из папки legal
object LegalDocs {
    const val TERMS = "terms.txt"
    const val PRIVACY = "privacy.txt"
    const val LICENSES = "licenses.txt"
}

private val SectionTitle = Regex("""^\d{1,2}\. \S.*""")

@Composable
fun LegalDocument(asset: String, onClose: () -> Unit) {
    val c = Ios.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lines = remember(asset) {
        runCatching { context.assets.open(asset).bufferedReader().use { it.readText() } }
            .getOrDefault("Не удалось открыть документ.")
            .trim()
            .lines()
    }
    // лист выезжает снизу и уезжает обратно при закрытии
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
                        lines.firstOrNull().orEmpty(),
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
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
                ) {
                    lines.forEachIndexed { index, line -> DocumentLine(line, isTitle = index == 0) }
                }
            }
        }
    }
}

// строка документа: заголовок, раздел, пункт или абзац
@Composable
private fun DocumentLine(line: String, isTitle: Boolean) {
    val c = Ios.colors
    when {
        isTitle -> {
            Text(line, style = IosType.title2, color = c.label)
            Spacer(Modifier.height(4.dp))
        }
        line.matches(SectionTitle) && line.length < 80 && !line.endsWith('.') -> {
            Spacer(Modifier.height(18.dp))
            Text(line, style = IosType.headline, color = c.label)
        }
        line.startsWith("• ") -> Row(Modifier.padding(start = 4.dp, top = 2.dp)) {
            Text("•", style = IosType.body, color = c.tint)
            Spacer(Modifier.width(10.dp))
            Text(line.removePrefix("• "), style = IosType.body, color = c.label)
        }
        line.isBlank() -> Spacer(Modifier.height(10.dp))
        else -> Text(line, style = IosType.body, color = c.label)
    }
}
