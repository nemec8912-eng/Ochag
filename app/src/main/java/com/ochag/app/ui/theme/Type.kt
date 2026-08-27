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

fun ochagTypography(): Typography {
    val heading = headingFamily()
    val body = bodyFamily()

    return Typography(
        displaySmall = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            lineHeight = 44.sp,
            color = BrownTextPrimary
        ),
        headlineLarge = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            color = BrownTextPrimary
        ),
        headlineMedium = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = BrownTextPrimary
        ),
        headlineSmall = TextStyle(
            fontFamily = heading,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            color = BrownTextPrimary
        ),
        titleLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            color = BrownTextPrimary
        ),
        titleMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = BrownTextPrimary
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = BrownTextPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = BrownTextSecondary
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = BrownTextPrimary
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = BrownTextSecondary
        )
    )
}

val OchagTypography: Typography
    get() = ochagTypography()
