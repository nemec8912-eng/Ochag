package com.ochag.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ochag.app.ui.localization.OchagLocaleController
import com.ochag.app.ui.localization.ochagText
import com.ochag.app.ui.theme.OchagFontStyle
import com.ochag.app.ui.theme.OchagThemeController

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onStickersClick: () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = OchagThemeController.isDarkTheme
    val fontStyle = OchagThemeController.fontStyle
    val fontSize = OchagThemeController.messageFontSizeLevel
    var languageExpanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "ru" to "Русский",
        "en" to "English",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "zh" to "中文",
        "ja" to "日本語"
    )
    val currentLanguageName = languages
        .firstOrNull { it.first == OchagLocaleController.language }
        ?.second ?: "Русский"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBackClick) {
                Text(ochagText("‹ Назад"))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                ochagText("Настройки"),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            Text("⚙", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(18.dp))

        SettingsCard(title = ochagText("Тема приложения")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (darkTheme) ochagText("Тёмная тема") else ochagText("Светлая тема"),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        ochagText("Изменяет оформление всего приложения"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = darkTheme,
                    onCheckedChange = { OchagThemeController.setDarkTheme(it) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(title = ochagText("Язык приложения")) {
            Box {
                OutlinedButton(
                    onClick = { languageExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(currentLanguageName)
                    Spacer(Modifier.weight(1f))
                    Text("⌄")
                }
                DropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { languageExpanded = false }
                ) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                OchagLocaleController.saveLanguage(context, code)
                                languageExpanded = false
                            },
                            leadingIcon = {
                                RadioButton(
                                    selected = OchagLocaleController.language == code,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(title = ochagText("Стиль шрифта")) {
            OchagFontStyle.values().forEach { style ->
                val title = when (style) {
                    OchagFontStyle.WARM -> ochagText("Тёплый")
                    OchagFontStyle.MODERN -> ochagText("Современный")
                    OchagFontStyle.CLASSIC -> ochagText("Классика")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { OchagThemeController.setFontStyle(style) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = fontStyle == style,
                        onClick = { OchagThemeController.setFontStyle(style) }
                    )
                    Text(title)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(title = ochagText("Размер текста всего приложения")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                (1..5).forEach { level ->
                    FilterChip(
                        selected = fontSize == level,
                        onClick = { OchagThemeController.setMessageFontSizeLevel(level) },
                        label = { Text(level.toString()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when (fontSize) {
                    1 -> ochagText("Очень маленький")
                    2 -> ochagText("Маленький")
                    3 -> ochagText("Средний")
                    4 -> ochagText("Большой")
                    else -> ochagText("Очень большой")
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStickersClick),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("😊", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(ochagText("Стикеры"), fontWeight = FontWeight.SemiBold)
                    Text(
                        ochagText("Открыть и настроить стикерпаки"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            content()
        }
    }
}
