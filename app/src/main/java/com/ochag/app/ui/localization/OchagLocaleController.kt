package com.ochag.app.ui.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object OchagLocaleController {
    private const val PREFS_NAME = "ochag_language"
    private const val KEY_LANGUAGE = "language_tag"
    const val DEFAULT_LANGUAGE = "ru"

    fun currentLanguage(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE)
            ?: DEFAULT_LANGUAGE

    fun saveLanguage(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageTag)
            .apply()
    }

    fun apply(context: Context): Context {
        val languageTag = currentLanguage(context)
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
