package one.crinklecod.razrguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import one.crinklecod.razrguard.R

// Nunito (SIL OFL 1.1, license in assets/licenses) is bundled as a single variable font,
// so it works offline without Play Services. Each weight pins the font's wght axis.
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: FontWeight) = Font(
    resId = R.font.nunito,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

private val Dreamy = FontFamily(
    listOf(
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
        FontWeight.ExtraBold,
        FontWeight.Black,
    ).map(::nunito)
)

// Styles the dashboard doesn't tune still get Nunito instead of falling back to Roboto
private val Base = Typography()

val DreamyTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Dreamy, color = PlumInk),
    displayMedium = Base.displayMedium.copy(fontFamily = Dreamy, color = PlumInk),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Dreamy, color = PlumInk),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Dreamy, color = PlumInk),
    displaySmall = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        color = PlumInk
    ),
    headlineSmall = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        color = PlumInk
    ),
    titleLarge = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        color = PlumInk
    ),
    titleMedium = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        color = PlumInk
    ),
    titleSmall = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
        color = PlumInkSoft
    ),
    bodyLarge = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = PlumInk
    ),
    bodyMedium = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = PlumInkSoft
    ),
    bodySmall = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
        color = PlumInkSoft
    ),
    labelLarge = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = PlumInk
    ),
    labelMedium = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = PlumInk
    ),
    labelSmall = TextStyle(
        fontFamily = Dreamy,
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.3.sp,
        color = PlumInkSoft
    )
)
