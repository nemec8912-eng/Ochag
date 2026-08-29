package com.ochag.app.ui.screens.profile

import com.ochag.app.ui.localization.ochagText
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.ochag.app.ui.localization.OchagLocaleController
import com.ochag.app.ui.security.AppLockController
import com.ochag.app.ui.security.AppLockMode
import com.ochag.app.ui.security.PatternPad
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
    val userName = user?.name ?: ochagText("Гость")
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
    var showSecurityDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val profilePreferences = remember(userName) {
        context.getSharedPreferences(
            "ochag_profile_settings_$userName",
            android.content.Context.MODE_PRIVATE
        )
    }
    val languageNames = mapOf(
        "ru" to ochagText("Русский"),
        "en" to "English",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "zh" to "中文",
        "ja" to "日本語"
    )
    var selectedLanguage by remember {
        mutableStateOf(OchagLocaleController.currentLanguage(context))
    }
    var profileAvatarUri by remember(userName) {
        mutableStateOf(
            profilePreferences.getString("profile_avatar", "").orEmpty()
        )
    }
    val profileAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            profileAvatarUri = uri.toString()
            profilePreferences.edit()
                .putString("profile_avatar", profileAvatarUri)
                .apply()
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(ochagText("Изменить тему")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(ochagText("Режим приложения"), fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !darkTheme,
                            onClick = { OchagThemeController.setDarkTheme(false) }
                        )
                        Text(ochagText("Светлая"))
                        Spacer(Modifier.width(14.dp))
                        RadioButton(
                            selected = darkTheme,
                            onClick = { OchagThemeController.setDarkTheme(true) }
                        )
                        Text(ochagText("Тёмная"))
                    }

                    HorizontalDivider()
                    Text(ochagText("Стиль приложения и чатов"), fontWeight = FontWeight.Bold)
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
                                    OchagFontStyle.WARM -> ochagText("Тёплая")
                                    OchagFontStyle.MODERN -> ochagText("Современная")
                                    OchagFontStyle.CLASSIC -> ochagText("Классическая")
                                }
                            )
                        }
                    }

                    Text(ochagText("Обои и дополнительные цвета будут добавлены в следующем этапе."))
                }
            },
            confirmButton = {
                Button(onClick = { showThemeDialog = false }) { Text(ochagText("Готово")) }
            }
        )
    }

    if (showLanguageDialog) {
        val languages = languageNames.toList()
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(ochagText("Язык приложения")) },
            text = {
                Column {
                    languages.forEach { (languageCode, languageName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLanguage = languageCode }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == languageCode,
                                onClick = { selectedLanguage = languageCode }
                            )
                            Text(languageName)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        OchagLocaleController.saveLanguage(context, selectedLanguage)
                        profilePreferences.edit()
                            .putString("app_language", selectedLanguage)
                            .apply()
                        showLanguageDialog = false
                        (context as? Activity)?.recreate()
                    }
                ) { Text(ochagText("Сохранить")) }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(ochagText("Отмена"))
                }
            }
        )
    }

    if (showSecurityDialog) {
        var lockMode by remember { mutableStateOf(AppLockController.mode) }
        var lockSecret by remember { mutableStateOf("") }
        var lockSecretRepeat by remember { mutableStateOf("") }
        val validSecret = lockSecret.length >= 4 && lockSecret == lockSecretRepeat

        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text(ochagText("Блокировка приложения")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (AppLockController.enabled) {
                        Text(ochagText("Блокировка включена"))
                        Text(
                            when (AppLockController.mode) {
                                AppLockMode.PIN -> ochagText("Способ: цифровой PIN-код")
                                AppLockMode.PASSWORD -> ochagText("Способ: пароль")
                                AppLockMode.PATTERN -> ochagText("Способ: графический ключ")
                            }
                        )
                        Text(ochagText("Автоматическая блокировка"), fontWeight = FontWeight.Bold)
                        listOf(
                            0L to ochagText("Сразу после сворачивания"),
                            60_000L to ochagText("Через 1 минуту"),
                            300_000L to ochagText("Через 5 минут"),
                            1_800_000L to ochagText("Через 30 минут"),
                            AppLockController.NEVER_TIMEOUT to ochagText("Только после полного закрытия")
                        ).forEach { (timeout, title) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { AppLockController.setTimeout(timeout) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = AppLockController.timeoutMs == timeout,
                                    onClick = { AppLockController.setTimeout(timeout) }
                                )
                                Text(title)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                AppLockController.disable()
                                showSecurityDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(ochagText("Отключить блокировку")) }
                    } else {
                        Text(ochagText("Выберите способ защиты"), fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = lockMode == AppLockMode.PIN,
                                onClick = { lockMode = AppLockMode.PIN }
                            )
                            Text(ochagText("PIN-код"))
                            Spacer(Modifier.width(14.dp))
                            RadioButton(
                                selected = lockMode == AppLockMode.PASSWORD,
                                onClick = { lockMode = AppLockMode.PASSWORD }
                            )
                            Text(ochagText("Пароль"))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { lockMode = AppLockMode.PATTERN },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lockMode == AppLockMode.PATTERN,
                                onClick = { lockMode = AppLockMode.PATTERN }
                            )
                            Text(ochagText("Графический ключ"))
                        }

                        if (lockMode == AppLockMode.PATTERN) {
                            Text(ochagText("Нарисуйте ключ"))
                            PatternPad(lockSecret) { lockSecret = it }
                            Text(ochagText("Повторите ключ"))
                            PatternPad(lockSecretRepeat) { lockSecretRepeat = it }
                        } else {
                            OutlinedTextField(
                                value = lockSecret,
                                onValueChange = {
                                    lockSecret = if (lockMode == AppLockMode.PIN) {
                                        it.filter(Char::isDigit).take(12)
                                    } else it
                                },
                                label = { Text(ochagText("Введите код или пароль")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = lockSecretRepeat,
                                onValueChange = {
                                    lockSecretRepeat = if (lockMode == AppLockMode.PIN) {
                                        it.filter(Char::isDigit).take(12)
                                    } else it
                                },
                                label = { Text(ochagText("Повторите")) },
                                singleLine = true,
                                isError = lockSecretRepeat.isNotBlank() &&
                                    lockSecretRepeat != lockSecret,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(ochagText("Минимум 4 точки или символа"))
                    }
                }
            },
            confirmButton = {
                if (!AppLockController.enabled) {
                    Button(
                        enabled = validSecret,
                        onClick = {
                            AppLockController.enable(lockMode, lockSecret)
                            showSecurityDialog = false
                        }
                    ) { Text(ochagText("Включить")) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityDialog = false }) {
                    Text(ochagText("Закрыть"))
                }
            }
        )
    }

    LaunchedEffect(Unit) { ServerRealtime.requestServerUsers() }

    val online = connectionState == ServerRealtime.ConnectionState.ONLINE
    val statusText = when (connectionState) {
        ServerRealtime.ConnectionState.ONLINE -> ochagText("В сети")
        ServerRealtime.ConnectionState.CONNECTING -> ochagText("Подключение…")
        ServerRealtime.ConnectionState.OFFLINE -> ochagText("Не в сети")
    }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            OchagBottomBar(
                items = listOf(ochagText("Чаты"), ochagText("Контакты"), ochagText("Звонки"), ochagText("Файлы"), ochagText("Профиль")),
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
                    text = ochagText("Профиль"),
                    style = MaterialTheme.typography.displaySmall,
                    color = BrownTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ochagText("Личное пространство в «Очаге»"),
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
                            if (profileAvatarUri.isNotBlank()) {
                                ProfileAvatarImage(
                                    uri = Uri.parse(profileAvatarUri),
                                    modifier = Modifier.size(104.dp)
                                )
                            } else {
                                OchagAvatar(
                                    name = userName,
                                    size = 104.dp,
                                    online = online,
                                    backgroundColor = AvatarPeach
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(34.dp)
                                    .clickable {
                                        profileAvatarLauncher.launch(arrayOf("image/*"))
                                    },
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
                                    text = ochagText("Владелец «Очага»"),
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
                SectionTitle(ochagText("Внешний вид"), ochagText("Настрой «Очаг» под себя"))
                Spacer(Modifier.height(10.dp))

                ProfileAction(
                    "◐",
                    ochagText("Изменить тему"),
                    ochagText("Приложение, чаты, сообщения и шрифт"),
                    AvatarPeach
                ) { showThemeDialog = true }

                Spacer(Modifier.height(8.dp))

                ProfileAction(
                    "文",
                    ochagText("Язык приложения"),
                    languageNames[selectedLanguage] ?: ochagText("Русский"),
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
                                    text = if (darkTheme) ochagText("Тёмная тема") else ochagText("Светлая тема"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrownTextPrimary
                                )
                                Text(
                                    text = if (darkTheme) ochagText("Мягкая тёмная палитра") else ochagText("Тёплая молочная палитра"),
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
                            text = ochagText("Стиль шрифта"),
                            style = MaterialTheme.typography.titleMedium,
                            color = BrownTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            FontChoice(ochagText("Тёплый"), fontStyle == OchagFontStyle.WARM, Modifier.weight(1f)) {
                                OchagThemeController.setFontStyle(OchagFontStyle.WARM)
                            }
                            FontChoice(ochagText("Современный"), fontStyle == OchagFontStyle.MODERN, Modifier.weight(1f)) {
                                OchagThemeController.setFontStyle(OchagFontStyle.MODERN)
                            }
                            FontChoice(ochagText("Классика"), fontStyle == OchagFontStyle.CLASSIC, Modifier.weight(1f)) {
                                OchagThemeController.setFontStyle(OchagFontStyle.CLASSIC)
                            }
                        }

                        HorizontalDivider(color = DividerSoft)

                        Text(
                            text = ochagText("Размер текста всего приложения"),
                            style = MaterialTheme.typography.titleMedium,
                            color = BrownTextPrimary
                        )
                        Text(
                            text = when (messageFontSizeLevel) {
                                1 -> ochagText("Очень маленький")
                                2 -> ochagText("Маленький")
                                3 -> ochagText("Средний")
                                4 -> ochagText("Большой")
                                else -> ochagText("Очень большой")
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
                                text = ochagText("Так будет выглядеть текст во всём приложении"),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = BrownTextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionTitle(ochagText("Семья"), ochagText("Люди и всё общее"))
                Spacer(Modifier.height(8.dp))

                ProfileAction("♡", ochagText("Семья"), ochagText("Участники, роли и доступ"), AvatarSage, onFamilyClick)
                ProfileAction("✦", ochagText("Стикеры"), ochagText("Встроенные и свои стикерпаки"), AvatarSand, onStickersClick)
                ProfileAction("＋", ochagText("Пригласить близких"), ochagText("Добавить нового участника"), AvatarRose, onInviteClick)

                Spacer(Modifier.height(18.dp))
                SectionTitle(ochagText("Контакты"), ochagText("Чтобы близкие могли найти тебя"))
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
                            label = ochagText("Телефон"),
                            placeholder = "+7 999 123-45-67"
                        )

                        Spacer(Modifier.height(12.dp))

                        OchagPrimaryButton(
                            text = if (saving) ochagText("Сохраняю…") else ochagText("Сохранить номер"),
                            enabled = !saving && userName != ochagText("Гость")
                        ) {
                            saving = true
                            saveStatus = ""
                            ServerAccountApi.updatePhone(
                                userName = userName,
                                phone = phone
                            ) { result ->
                                saving = false
                                saveStatus = if (result.success) ochagText("Номер сохранён") else result.error
                                if (result.success) ServerRealtime.requestServerUsers()
                            }
                        }

                        if (saveStatus.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = saveStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (saveStatus == ochagText("Номер сохранён")) OnlineGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                SectionTitle(ochagText("Безопасность"), ochagText("Защита личных переписок"))
                Spacer(Modifier.height(8.dp))
                ProfileAction(
                    "🔒",
                    ochagText("Блокировка приложения"),
                    if (AppLockController.enabled) ochagText("Включена") else ochagText("Не включена"),
                    AvatarSand
                ) { showSecurityDialog = true }

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
                    Text(ochagText("Выйти из «Очага»"), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProfileAvatarImage(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = ochagText("Аватар профиля"),
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(modifier = modifier, shape = CircleShape, color = AvatarPeach) {}
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
