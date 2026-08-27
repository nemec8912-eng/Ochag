package com.ochag.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OchagColorScheme = lightColorScheme(
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
    onError = CardWhite
)

@Composable
fun OchagTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = CreamBackground.toArgb()
            window.navigationBarColor = CreamBackground.toArgb()

            WindowCompat.getInsetsController(
                window,
                view
            ).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = OchagColorScheme,
        typography = OchagTypography,
        shapes = OchagShapes,
        content = content
    )
}
