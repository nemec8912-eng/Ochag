package com.ochag.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class OchagFontStyle {
    WARM,
    MODERN,
    CLASSIC
}

object OchagThemeController {

    private const val PREFS_NAME = "ochag_theme"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_FONT_STYLE = "font_style"
    private const val KEY_MESSAGE_FONT_SIZE = "message_font_size"

    var isDarkTheme by mutableStateOf(false)
        private set

    var fontStyle by mutableStateOf(OchagFontStyle.WARM)
        private set

    var messageFontSizeLevel by mutableIntStateOf(3)
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs?.getBoolean(KEY_DARK_THEME, false) ?: false
        fontStyle = runCatching {
            OchagFontStyle.valueOf(
                prefs?.getString(KEY_FONT_STYLE, OchagFontStyle.WARM.name)
                    ?: OchagFontStyle.WARM.name
            )
        }.getOrDefault(OchagFontStyle.WARM)
        messageFontSizeLevel = (prefs?.getInt(KEY_MESSAGE_FONT_SIZE, 3) ?: 3).coerceIn(1, 5)
    }

    @JvmName("applyDarkTheme")
    fun setDarkTheme(enabled: Boolean) {
        isDarkTheme = enabled
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_DARK_THEME, enabled)
            ?.apply()
    }

    @JvmName("applyFontStyle")
    fun setFontStyle(style: OchagFontStyle) {
        fontStyle = style
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_FONT_STYLE, style.name)
            ?.apply()
    }

    fun setMessageFontSizeLevel(level: Int) {
        val safeLevel = level.coerceIn(1, 5)
        messageFontSizeLevel = safeLevel
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putInt(KEY_MESSAGE_FONT_SIZE, safeLevel)
            ?.apply()
    }

    fun toggle() {
        setDarkTheme(!isDarkTheme)
    }
}
