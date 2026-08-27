package com.ochag.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
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

    var isDarkTheme by mutableStateOf(false)
        private set

    var fontStyle by mutableStateOf(OchagFontStyle.WARM)
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
    }

    fun setDarkTheme(enabled: Boolean) {
        isDarkTheme = enabled
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_DARK_THEME, enabled)
            ?.apply()
    }

    fun setFontStyle(style: OchagFontStyle) {
        fontStyle = style
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_FONT_STYLE, style.name)
            ?.apply()
    }

    fun toggle() {
        setDarkTheme(!isDarkTheme)
    }
}
