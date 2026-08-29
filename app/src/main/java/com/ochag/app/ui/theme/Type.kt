package com.ochag.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun headingFamily(): FontFamily = when (OchagThemeController.fontStyle) {
    OchagFontStyle.WARM -> FontFamily.Serif
    OchagFontStyle.MODERN -> FontFamily.SansSerif
    OchagFontStyle.CLASSIC -> FontFamily.Serif
}

private fun bodyFamily(): FontFamily = when (OchagThemeController.fontStyle) {
    OchagFontStyle.WARM -> FontFamily.SansSerif
    OchagFontStyle.MODERN -> FontFamily.SansSerif
    OchagFontStyle.CLASSIC -> FontFamily.Serif
}

private fun appFontScale(): Float = 1f

private fun scaledSp(value: Int) = (value * appFontScale()).sp

fun ochagTypography(): Typography {
    val heading = headingFamily()
    val body = bodyFamily()

    return Typography(
        displaySmall = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(42),
            lineHeight = scaledSp(46),
            letterSpacing = (-0.7).sp,
            color = BrownTextPrimary
        ),
        headlineLarge = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(36),
            lineHeight = scaledSp(40),
            letterSpacing = (-0.45).sp,
            color = BrownTextPrimary
        ),
        headlineMedium = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(30),
            lineHeight = scaledSp(35),
            letterSpacing = (-0.25).sp,
            color = BrownTextPrimary
        ),
        headlineSmall = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(25),
            lineHeight = scaledSp(30),
            color = BrownTextPrimary
        ),
        titleLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(21),
            lineHeight = scaledSp(27),
            color = BrownTextPrimary
        ),
        titleMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(17),
            lineHeight = scaledSp(23),
            color = BrownTextPrimary
        ),
        titleSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(15),
            lineHeight = scaledSp(21),
            color = BrownTextPrimary
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(16),
            lineHeight = scaledSp(24),
            color = BrownTextPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(14),
            lineHeight = scaledSp(21),
            color = BrownTextSecondary
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(13),
            lineHeight = scaledSp(19),
            color = BrownTextSecondary
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(14),
            lineHeight = scaledSp(19),
            color = BrownTextPrimary
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = scaledSp(12),
            lineHeight = scaledSp(17),
            color = BrownTextSecondary
        ),
        labelSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = scaledSp(11),
            lineHeight = scaledSp(15),
            color = BrownTextSecondary
        )
    )
}

val OchagTypography: Typography
    get() = ochagTypography()
