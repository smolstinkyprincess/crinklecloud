package one.crinklecod.cc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val DreamyPastelColorScheme = lightColorScheme(
    primary = CottonCandyPink,
    onPrimary = PlumInk,
    primaryContainer = BubblegumPink,
    onPrimaryContainer = PlumInk,
    secondary = LavenderHaze,
    onSecondary = PlumInk,
    secondaryContainer = DreamLavender,
    onSecondaryContainer = PlumInk,
    tertiary = MintSparkle,
    onTertiary = PlumInk,
    tertiaryContainer = MintMilk,
    onTertiaryContainer = PlumInk,
    background = SkyCream,
    onBackground = PlumInk,
    surface = CloudWhite,
    onSurface = PlumInk,
    surfaceVariant = MarshmallowMist,
    onSurfaceVariant = PlumInkSoft,
    surfaceContainer = WhippedCream,
    surfaceContainerHigh = MarshmallowMist,
    outline = OutlineLilac,
    outlineVariant = OutlinePink,
    error = SleepyRose,
    onError = CloudWhite,
)

/** Pillowy corners everywhere: nothing smaller than 16dp, cards at 32dp+. */
val DreamyShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(42.dp),
)

@Composable
fun RazrGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DreamyPastelColorScheme,
        typography = DreamyTypography,
        shapes = DreamyShapes,
        content = content
    )
}
