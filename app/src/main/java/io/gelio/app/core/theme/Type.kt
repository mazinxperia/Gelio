package io.gelio.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.gelio.app.R

// Using the project's variable font for both UI and Expressive Display.
// We tune letter spacing and weight heavily to separate the two moods.
private val ShowcaseFontFamily = FontFamily(
    Font(R.font.gflex_variable, weight = FontWeight.W400),
    Font(R.font.gflex_variable, weight = FontWeight.W500),
    Font(R.font.gflex_variable, weight = FontWeight.W600),
    Font(R.font.gflex_variable, weight = FontWeight.W700),
    Font(R.font.gflex_variable, weight = FontWeight.W800),
)

val ShowcaseTypography = Typography(
    // Expressive Display Scale
    displayLarge = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 60.sp,
        lineHeight = 64.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 50.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.0).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    // Standard UI Hero moments
    headlineLarge = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    // Standard UI Text
    titleLarge = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Button and action labels
    labelLarge = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = ShowcaseFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
