package com.ochag.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerAccountApi
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagBottomBar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagPrimaryButton
import com.ochag.app.ui.components.OchagTextField
import com.ochag.app.ui.theme.*

@Composable
fun ProfileScreen(
    onChatsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onFamilyClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onStickersClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val user = OchagAppState.currentUser
    val userName = user?.name ?: "Гость"
    val profiles by ServerRealtime.serverUserProfiles.collectAsState()
    val connectionState by ServerRealtime.connectionState.collectAsState()
    val darkTheme = OchagThemeController.isDarkTheme
    val fontStyle = OchagThemeController.fontStyle
    val messageFontSizeLevel = OchagThemeController.messageFontSizeLevel

    val serverPhone = profiles
        .firstOrNull { it.name.equals(userName, ignoreCase = true) }
        ?.phone
        .orEmpty()

    var phone by remember(serverPhone) { mutableStateOf(serverPhone) }
    var saveStatus by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val profilePreferences = remember(userName) {
        context.getSharedPreferences(
            "ochag_profile_settings_$userName",
            android.content.Context.MODE_PRIVATE
        )
    }
    var selectedLanguage by remember(userName) {
        mutableStateOf(
            profilePreferences.getString("app_language", "Русский") ?: "Русский"
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Изменить тему") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Режим приложения", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !darkTheme,
                            onClick = { OchagThemeController.setDarkTheme(false) }
                        )
                        Text("Светлая")
                        Spacer(Modifier.width(14.dp))
                        RadioButton(
                            selected = darkTheme,
                            onClick = { OchagThemeController.setDarkTheme(true) }
                        )
                        Text("Тёмная")
                    }

                    HorizontalDivider()
                    Text("Стиль приложения и чатов", fontWeight = FontWeight.Bold)
                    OchagFontStyle.values().forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { OchagThemeController.setFontStyle(style) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fontStyle == style,
                                onClick = { OchagThemeController.setFontStyle(style) }
                            )
                            Text(
                                when (style) {
                                    OchagFontStyle.WARM -> "Тёплая"
                                    OchagFontStyle.MODERN -> "Современная"
                                    OchagFontStyle.CLASSIC -> "Классическая"
                                }
                            )
                        }
                    }

                    Text("Обои и дополнительные цвета будут добавлены в следующем этапе.")
                }
            },
            confirmButton = {
                Button(onClick = { showThemeDialog = false }) { Text("Готово") }
            }
        )
    }

    if (showLanguageDialog) {
        val languages = listOf(
            "Русский", "English", "Deutsch", "Français",
            "Español", "中文", "日本語"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Язык приложения") },
            text = {
                Column {
                    languages.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLanguage = language }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == language,
                                onClick = { selectedLanguage = language }
                            )
                            Text(language)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        profilePreferences.edit()
                            .putString("app_language", selectedLanguage)
                            .apply()
                        showLanguageDialog = false
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    LaunchedEffect(Unit) { ServerRealtime.requestServerUsers() }

    val online = connectionState == ServerRealtime.ConnectionState.ONLINE
    val statusText = when (connectionState) {
        ServerRealtime.ConnectionState.ONLINE -> "В сети"
        ServerRealtime.ConnectionState.CONNECTING -> "Подключение…"
        ServerRealtime.ConnectionState.OFFLINE -> "Не в сети"
    }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            OchagBottomBar(
                items = listOf("Чаты", "Контакты", "Звонки", "Файлы", "Профиль"),
                selectedIndex = 4,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) { index ->
                when (index) {
                    0 -> onChatsClick()
                    1 -> onContactsClick()
                    2 -> onCallsClick()
                    3 -> onFilesClick()
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "❧",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp),
                fontSize = 80.sp,
                color = SoftGreen.copy(alpha = 0.12f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Профиль",
                    style = MaterialTheme.typography.displaySmall,
                    color = BrownTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Личное пространство в «Очаге»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrownTextSecondary
                )

                Spacer(Modifier.height(22.dp))

                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 3.dp,
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            OchagAvatar(
                                name = userName,
                                size = 104.dp,
                                online = online,
                                backgroundColor = AvatarPeach
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(34.dp),
                                shape = CircleShape,
                                color = TerracottaPrimary,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✎", color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(15.dp))

                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = BrownTextPrimary
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = CircleShape,
                                color = if (online) OnlineGreen else BorderSoft
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (online) OnlineGreen else BrownTextSecondary
                            )
                        }

                        if (user?.isOwner == true) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = TerracottaSoft
                            ) {
                                Text(
                                    text = "Владелец «Очага»",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TerracottaDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionTitle("Внешний вид", "Настрой «Очаг» под себя")
                Spacer(Modifier.height(10.dp))

                ProfileAction(
                    "◐",
                    "Изменить тему",
                    "Приложение, чаты, сообщения и шрифт",
                    AvatarPeach
                ) { showThemeDialog = true }

                Spacer(Modifier.height(8.dp))

                ProfileAction(
                    "文",
                    "Язык приложения",
                    selectedLanguage,
                    AvatarSage
                ) { showLanguageDialog = true }

                Spacer(Modifier.height(10.dp))

                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(17.dp),
                                color = if (darkTheme) TerracottaSoft else SoftGreenLight
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (darkTheme) "☾" else "☀",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = BrownTextPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (darkTheme) "Тёмная тема" else "Светлая тема",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrownTextPrimary
                                )
                                Text(
                                    text = if (darkTheme) "Мягкая тёмная палитра" else "Тёплая молочная палитра",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrownTextSecondary
                                )
                            }

                            ThemeToggle(
                                checked = darkTheme,
                                onClick = { OchagThemeController.setDarkTheme(!darkTheme) }
                            )
                        }

                        HorizontalDivider(color = DividerSoft)

                        Text(
                            text = "Стиль шрифта",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrownTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            FontChoice("Тёплый", fontStyle == OchagFontStyle.WARM, Modifier.weight(1f)) {
                                OchagThemeController.setFontStyle(OchagFontStyle.WARM)
                            }
                            FontChoice("Современный", fontStyle == OchagFontStyle.MODERN, Modifier.weight(1f)) {
                                OchagThemeController.setFontStyle(OchagFontStyle.MODERN)
                            }
                            FontChoice("Классика", fontStyle == OchagFontStyle.CLASSIC, Modifier.weight(1f)) {
                                OchagThemeController.setFontStyle(OchagFontStyle.CLASSIC)
                            }
                        }

                        HorizontalDivider(color = DividerSoft)

                        Text(
                            text = "Размер текста сообщений",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrownTextPrimary
                        )
                        Text(
                            text = when (messageFontSizeLevel) {
                                1 -> "Очень маленький"
                                2 -> "Маленький"
                                3 -> "Средний"
                                4 -> "Большой"
                                else -> "Очень большой"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = BrownTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..5).forEach { level ->
                                FontSizeChoice(
                                    level = level,
                                    selected = messageFontSizeLevel == level,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OchagThemeController.setMessageFontSizeLevel(level)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = CardCream
                        ) {
                            Text(
                                text = "Так будет выглядеть сообщение в чате",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = BrownTextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionTitle("Семья", "Люди и всё общее")
                Spacer(Modifier.height(8.dp))

                ProfileAction("♡", "Семья", "Участники, роли и доступ", AvatarSage, onFamilyClick)
                ProfileAction("✦", "Стикеры", "Встроенные и свои стикерпаки", AvatarSand, onStickersClick)
                ProfileAction("＋", "Пригласить близких", "Добавить нового участника", AvatarRose, onInviteClick)

                Spacer(Modifier.height(18.dp))
                SectionTitle("Контакты", "Чтобы близкие могли найти тебя")
                Spacer(Modifier.height(10.dp))

                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column {
                        OchagTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                saveStatus = ""
                            },
                            label = "Телефон",
                            placeholder = "+7 999 123-45-67"
                        )

                        Spacer(Modifier.height(12.dp))

                        OchagPrimaryButton(
                            text = if (saving) "Сохраняю…" else "Сохранить номер",
                            enabled = !saving && userName != "Гость"
                        ) {
                            saving = true
                            saveStatus = ""
                            ServerAccountApi.updatePhone(
                                userName = userName,
                                phone = phone
                            ) { result ->
                                saving = false
                                saveStatus = if (result.success) "Номер сохранён" else result.error
                                if (result.success) ServerRealtime.requestServerUsers()
                            }
                        }

                        if (saveStatus.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = saveStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (saveStatus == "Номер сохранён") OnlineGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BorderSoft),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerracottaDark)
                ) {
                    Text("Выйти из «Очага»", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = BrownTextPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = BrownTextSecondary
        )
    }
}

@Composable
private fun ThemeToggle(
    checked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(58.dp)
            .height(34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (checked) TerracottaPrimary else CardCream,
        border = BorderStroke(1.dp, if (checked) TerracottaPrimary else BorderSoft)
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (checked) Color.White else CardWhite,
                shadowElevation = 2.dp
            ) {}
        }
    }
}

@Composable
private fun FontChoice(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) TerracottaSoft else CardCream,
        border = BorderStroke(1.dp, if (selected) TerracottaSoft else BorderSoft)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) TerracottaDark else BrownTextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FontSizeChoice(
    level: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) TerracottaPrimary else CardCream,
        border = BorderStroke(1.dp, if (selected) TerracottaPrimary else BorderSoft)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = level.toString(),
                color = if (selected) Color.White else BrownTextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProfileAction(
    symbol: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    OchagCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        elevation = 2.dp,
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 15.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(17.dp),
                color = accent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = symbol,
                        color = BrownTextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrownTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrownTextSecondary
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = BrownTextMuted
            )
        }
    }
}
