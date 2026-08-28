package com.ochag.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat

private fun lightOchagColorScheme() = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = OutgoingText,
    primaryContainer = TerracottaLight,
    onPrimaryContainer = BrownTextPrimary,
    secondary = SoftGreen,
    onSecondary = CardWhite,
    secondaryContainer = SoftGreenLight,
    onSecondaryContainer = BrownTextPrimary,
    background = CreamBackground,
    onBackground = BrownTextPrimary,
    surface = CardWhite,
    onSurface = BrownTextPrimary,
    surfaceVariant = CardCream,
    onSurfaceVariant = BrownTextSecondary,
    outline = BorderSoft,
    outlineVariant = DividerSoft,
    error = TerracottaDark,
    onError = OutgoingText
)

private fun darkOchagColorScheme() = darkColorScheme(
    primary = TerracottaPrimary,
    onPrimary = OutgoingText,
    primaryContainer = TerracottaSoft,
    onPrimaryContainer = BrownTextPrimary,
    secondary = SoftGreen,
    onSecondary = BrownTextPrimary,
    secondaryContainer = SoftGreenLight,
    onSecondaryContainer = BrownTextPrimary,
    background = CreamBackground,
    onBackground = BrownTextPrimary,
    surface = CardWhite,
    onSurface = BrownTextPrimary,
    surfaceVariant = CardCream,
    onSurfaceVariant = BrownTextSecondary,
    outline = BorderSoft,
    outlineVariant = DividerSoft,
    error = ColorErrorDark,
    onError = OutgoingText
)

private val ColorErrorDark = androidx.compose.ui.graphics.Color(0xFFE09A83)

@Composable
fun OchagTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = OchagThemeController.isDarkTheme
    val view = LocalView.current
    val currentDensity = LocalDensity.current
    val textScale = when (OchagThemeController.messageFontSizeLevel) {
        1 -> 0.80f
        2 -> 0.90f
        3 -> 1.00f
        4 -> 1.15f
        else -> 1.30f
    }
    val scaledDensity = remember(
        currentDensity.density,
        currentDensity.fontScale,
        textScale
    ) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * textScale
        )
    }
    val backgroundColor = CreamBackground

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = backgroundColor.toArgb()
            window.navigationBarColor = backgroundColor.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkOchagColorScheme() else lightOchagColorScheme(),
            typography = OchagTypography,
            shapes = OchagShapes,
            content = content
        )
    }
}
