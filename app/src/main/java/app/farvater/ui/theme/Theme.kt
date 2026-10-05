package app.farvater.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// системные цвета iOS
@Immutable
data class IosColors(
    val dark: Boolean,
    val background: Color,
    val cell: Color,
    val cellRaised: Color,
    val fill: Color,
    val highlight: Color,
    val segmentThumb: Color,
    val bar: Color,
    val barBorder: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val tint: Color,
    val green: Color,
    val red: Color,
    val blue: Color,
    val teal: Color,
    val indigo: Color,
    val purple: Color,
    val gray: Color,
    val yellow: Color,
    val pink: Color,
)

val IosDark = IosColors(
    dark = true,
    background = Color(0xFF000000),
    cell = Color(0xFF1C1C1E),
    cellRaised = Color(0xFF2C2C2E),
    fill = Color(0xFF39393D),
    highlight = Color(0xFF3A3A3C),
    segmentThumb = Color(0xFF636366),
    bar = Color(0xF71C1C1E),
    barBorder = Color(0x1FFFFFFF),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF98989F),
    tertiaryLabel = Color(0xFF5A5A5F),
    separator = Color(0xFF38383A),
    tint = Color(0xFFFF9F0A),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    blue = Color(0xFF0A84FF),
    teal = Color(0xFF40C8E0),
    indigo = Color(0xFF5E5CE6),
    purple = Color(0xFFBF5AF2),
    gray = Color(0xFF8E8E93),
    yellow = Color(0xFFFFD60A),
    pink = Color(0xFFFF375F),
)

val IosLight = IosColors(
    dark = false,
    background = Color(0xFFF2F2F7),
    cell = Color(0xFFFFFFFF),
    cellRaised = Color(0xFFFFFFFF),
    fill = Color(0xFFE3E3E8),
    highlight = Color(0xFFD1D1D6),
    segmentThumb = Color(0xFFFFFFFF),
    bar = Color(0xF7FBFBFD),
    barBorder = Color(0x14000000),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF8A8A8E),
    tertiaryLabel = Color(0xFFC4C4C7),
    separator = Color(0xFFC6C6C8),
    tint = Color(0xFFFF9500),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    blue = Color(0xFF007AFF),
    teal = Color(0xFF30B0C7),
    indigo = Color(0xFF5856D6),
    purple = Color(0xFFAF52DE),
    gray = Color(0xFF8E8E93),
    yellow = Color(0xFFFFCC00),
    pink = Color(0xFFFF2D55),
)

val LocalIos = staticCompositionLocalOf { IosDark }

// шрифтовая шкала iOS
object IosType {
    val largeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    val title1 = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    val title2 = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    val title3 = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
    val headline = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val callout = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.15).sp)
    val subheadline = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp)
    val footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.05).sp)
    val caption1 = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val caption2 = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.05.sp)
}

object Ios {
    val colors: IosColors
        @Composable @ReadOnlyComposable get() = LocalIos.current
}

private fun scheme(c: IosColors) = if (c.dark) {
    darkColorScheme(
        primary = c.tint, onPrimary = Color.White,
        primaryContainer = Color(0xFF3D2A0B), onPrimaryContainer = Color(0xFFFFD9A8),
        secondary = c.gray, onSecondary = Color.White,
        secondaryContainer = c.cellRaised, onSecondaryContainer = c.label,
        tertiary = c.green, onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF0E3A1A), onTertiaryContainer = Color(0xFFB7F5C6),
        background = c.background, onBackground = c.label,
        surface = c.background, onSurface = c.label,
        surfaceVariant = c.cellRaised, onSurfaceVariant = c.secondaryLabel,
        surfaceTint = Color.Transparent,
        inverseSurface = c.label, inverseOnSurface = c.cell, inversePrimary = c.tint,
        error = c.red, onError = Color.White,
        errorContainer = Color(0xFF4A1512), onErrorContainer = Color(0xFFFFDAD6),
        outline = c.gray, outlineVariant = c.separator, scrim = Color.Black,
        surfaceBright = c.cellRaised, surfaceDim = c.background,
        surfaceContainerLowest = c.background, surfaceContainerLow = c.cell,
        surfaceContainer = c.cell, surfaceContainerHigh = c.cellRaised, surfaceContainerHighest = c.fill,
    )
} else {
    lightColorScheme(
        primary = c.tint, onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE8CC), onPrimaryContainer = Color(0xFF3A2200),
        secondary = c.gray, onSecondary = Color.White,
        secondaryContainer = c.fill, onSecondaryContainer = c.label,
        tertiary = c.green, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD3F5DC), onTertiaryContainer = Color(0xFF00210A),
        background = c.background, onBackground = c.label,
        surface = c.background, onSurface = c.label,
        surfaceVariant = c.fill, onSurfaceVariant = c.secondaryLabel,
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFF1C1C1E), inverseOnSurface = Color.White, inversePrimary = c.tint,
        error = c.red, onError = Color.White,
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        outline = c.gray, outlineVariant = c.separator, scrim = Color.Black,
        surfaceBright = c.cell, surfaceDim = c.fill,
        surfaceContainerLowest = c.cell, surfaceContainerLow = c.cell,
        surfaceContainer = c.cell, surfaceContainerHigh = c.cell, surfaceContainerHighest = c.fill,
    )
}

private val Type = Typography(
    displaySmall = IosType.largeTitle,
    headlineLarge = IosType.largeTitle,
    headlineMedium = IosType.title1,
    headlineSmall = IosType.title2,
    titleLarge = IosType.title3,
    titleMedium = IosType.headline,
    titleSmall = IosType.subheadline.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = IosType.body,
    bodyMedium = IosType.subheadline,
    bodySmall = IosType.footnote,
    labelLarge = IosType.headline,
    labelMedium = IosType.caption1.copy(fontWeight = FontWeight.Medium),
    labelSmall = IosType.caption2.copy(fontWeight = FontWeight.Medium),
)

// пружины в духе iOS: мягкая для переходов и упругая для нажатий
object IosMotion {
    const val SoftDamping = 0.86f
    const val SoftStiffness = 300f
    const val SnappyDamping = 0.7f
    const val SnappyStiffness = 520f
}

// кривые для коротких переходов
val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

// цифры одинаковой ширины
val Numeric = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun FarvaterTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val ios = if (darkTheme) IosDark else IosLight
    CompositionLocalProvider(LocalIos provides ios) {
        MaterialTheme(
            colorScheme = scheme(ios),
            typography = Type,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(7.dp),
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(14.dp),
                extraLarge = RoundedCornerShape(20.dp),
            ),
            content = content,
        )
    }
}
