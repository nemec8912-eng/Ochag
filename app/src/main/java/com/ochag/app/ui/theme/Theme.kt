package com.ochag.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
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

    MaterialTheme(
        colorScheme = if (darkTheme) darkOchagColorScheme() else lightOchagColorScheme(),
        typography = OchagTypography,
        shapes = OchagShapes,
        content = content
    )
}
