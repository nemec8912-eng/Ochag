package com.ochag.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object OchagThemeController {

    private const val PREFS_NAME = "ochag_theme"
    private const val KEY_DARK_THEME = "dark_theme"

    var isDarkTheme by mutableStateOf(false)
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs?.getBoolean(KEY_DARK_THEME, false) ?: false
    }

    fun setDarkTheme(enabled: Boolean) {
        isDarkTheme = enabled
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_DARK_THEME, enabled)
            ?.apply()
    }

    fun toggle() {
        setDarkTheme(!isDarkTheme)
    }
}
