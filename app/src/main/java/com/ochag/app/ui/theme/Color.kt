package com.ochag.app.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================
// OCHAG UI v3.1
// Динамическая светлая / тёмная палитра
// =====================================================

private val LightCreamBackground = Color(0xFFF7F1E8)
private val LightCreamBackgroundDeep = Color(0xFFF1E7DA)
private val LightCardWhite = Color(0xFFFFFCF8)
private val LightCardCream = Color(0xFFF9F3EB)
private val LightBrownTextPrimary = Color(0xFF3B2B24)
private val LightBrownTextSecondary = Color(0xFF8D7869)
private val LightBrownTextMuted = Color(0xFFB5A396)
private val LightBorderSoft = Color(0xFFE6D9CD)
private val LightDividerSoft = Color(0xFFEEE4DB)
private val LightIncomingBubble = Color(0xFFFFFCF8)

private val DarkCreamBackground = Color(0xFF171412)
private val DarkCreamBackgroundDeep = Color(0xFF211C19)
private val DarkCardWhite = Color(0xFF26211E)
private val DarkCardCream = Color(0xFF2E2824)
private val DarkBrownTextPrimary = Color(0xFFF4E9DF)
private val DarkBrownTextSecondary = Color(0xFFC7B5A8)
private val DarkBrownTextMuted = Color(0xFF94847A)
private val DarkBorderSoft = Color(0xFF4A3F38)
private val DarkDividerSoft = Color(0xFF3B332E)
private val DarkIncomingBubble = Color(0xFF2B2521)

val CreamBackground: Color
    get() = if (OchagThemeController.isDarkTheme) DarkCreamBackground else LightCreamBackground

val CreamBackgroundDeep: Color
    get() = if (OchagThemeController.isDarkTheme) DarkCreamBackgroundDeep else LightCreamBackgroundDeep

val CardWhite: Color
    get() = if (OchagThemeController.isDarkTheme) DarkCardWhite else LightCardWhite

val CardCream: Color
    get() = if (OchagThemeController.isDarkTheme) DarkCardCream else LightCardCream

val BrownTextPrimary: Color
    get() = if (OchagThemeController.isDarkTheme) DarkBrownTextPrimary else LightBrownTextPrimary

val BrownTextSecondary: Color
    get() = if (OchagThemeController.isDarkTheme) DarkBrownTextSecondary else LightBrownTextSecondary

val BrownTextMuted: Color
    get() = if (OchagThemeController.isDarkTheme) DarkBrownTextMuted else LightBrownTextMuted

// Главный терракотовый акцент
val TerracottaPrimary = Color(0xFFC9785D)
val TerracottaDark = Color(0xFFAE624A)
val TerracottaSoft: Color
    get() = if (OchagThemeController.isDarkTheme) Color(0xFF4B332B) else Color(0xFFF0D3C7)
val TerracottaLight: Color
    get() = if (OchagThemeController.isDarkTheme) Color(0xFF5A3A31) else Color(0xFFF6E4DC)

// Зелёные акценты
val SoftGreen = Color(0xFF93A998)
val SoftGreenLight: Color
    get() = if (OchagThemeController.isDarkTheme) Color(0xFF2F3D33) else Color(0xFFDDE7DE)
val OnlineGreen = Color(0xFF6E9B76)

// Нейтральные
val BorderSoft: Color
    get() = if (OchagThemeController.isDarkTheme) DarkBorderSoft else LightBorderSoft
val DividerSoft: Color
    get() = if (OchagThemeController.isDarkTheme) DarkDividerSoft else LightDividerSoft
val ShadowWarm = Color(0x1F6E4B3B)

// Чат
val IncomingBubble: Color
    get() = if (OchagThemeController.isDarkTheme) DarkIncomingBubble else LightIncomingBubble
val OutgoingBubble = Color(0xFFC9785D)
val OutgoingText = Color(0xFFFFFAF6)

// Пастельные аватары
val AvatarPeach: Color
    get() = if (OchagThemeController.isDarkTheme) Color(0xFF51372E) else Color(0xFFF3D1C2)
val AvatarSage: Color
    get() = if (OchagThemeController.isDarkTheme) Color(0xFF334237) else Color(0xFFD8E3D7)
val AvatarSand: Color
    get() = if (OchagThemeController.isDarkTheme) Color(0xFF493F31) else Color(0xFFE8DDC9)
val AvatarRose: Color
    get() = if (OchagThemeController.isDarkTheme) Color(0xFF4B3434) else Color(0xFFEFD8D6)
