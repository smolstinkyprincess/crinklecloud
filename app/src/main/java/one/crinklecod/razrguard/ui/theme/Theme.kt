package one.crinklecod.razrguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val CozyPastelColorScheme = darkColorScheme(
    primary = CottonCandyPink,
    onPrimary = PlumOledCanvas,
    primaryContainer = PastelPlumLight,
    onPrimaryContainer = CottonCandyPink,
    secondary = FairyLilac,
    onSecondary = PlumOledCanvas,
    secondaryContainer = PastelPlumElevated,
    onSecondaryContainer = WhippedCream,
    tertiary = SoftMatchaMint,
    onTertiary = PlumOledCanvas,
    background = PlumOledCanvas,
    onBackground = WhippedCream,
    surface = PastelPlumCard,
    onSurface = WhippedCream,
    surfaceVariant = PastelPlumElevated,
    onSurfaceVariant = FairyLilac,
    outline = BorderPinkGlow,
)

@Composable
fun RazrGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CozyPastelColorScheme,
        content = content
    )
}
