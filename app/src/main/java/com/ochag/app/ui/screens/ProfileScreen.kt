package com.ochag.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerAccountApi
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagBottomBar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagPrimaryButton
import com.ochag.app.ui.components.OchagSectionHeader
import com.ochag.app.ui.components.OchagTextField
import com.ochag.app.ui.theme.*

@Composable
fun ProfileScreen(
    onChatsClick: () -> Unit = {},
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

    val serverPhone = profiles
        .firstOrNull { it.name.equals(userName, ignoreCase = true) }
        ?.phone
        .orEmpty()

    var phone by remember(serverPhone) { mutableStateOf(serverPhone) }
    var saveStatus by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ServerRealtime.requestServerUsers()
    }

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
                items = listOf("Чаты", "Файлы", "Профиль"),
                selectedIndex = 2,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            ) { index ->
                when (index) {
                    0 -> onChatsClick()
                    1 -> onFilesClick()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            OchagSectionHeader(
                title = "Профиль",
                subtitle = "Личное пространство в «Очаге»"
            )

            Spacer(Modifier.height(18.dp))

            OchagCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OchagAvatar(
                        name = userName,
                        size = 92.dp,
                        online = online,
                        backgroundColor = TerracottaSoft
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextPrimary
                    )

                    Spacer(Modifier.height(4.dp))

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
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = TerracottaSoft
                        ) {
                            Text(
                                text = "Владелец «Очага»",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TerracottaDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OchagCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column {
                    Text(
                        text = "Внешний вид",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrownTextPrimary
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
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
                                text = "Тёмная тема",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrownTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (darkTheme) "Включена" else "Светлая тема включена",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrownTextSecondary
                            )
                        }

                        Switch(
                            checked = darkTheme,
                            onCheckedChange = OchagThemeController::setDarkTheme,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = TerracottaPrimary,
                                checkedThumbColor = OutgoingText,
                                uncheckedTrackColor = BorderSoft,
                                uncheckedThumbColor = CardWhite,
                                uncheckedBorderColor = BorderSoft
                            )
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Шрифт",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrownTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        FontChoice(
                            title = "Тёплый",
                            selected = fontStyle == OchagFontStyle.WARM,
                            modifier = Modifier.weight(1f)
                        ) { OchagThemeController.setFontStyle(OchagFontStyle.WARM) }

                        FontChoice(
                            title = "Современный",
                            selected = fontStyle == OchagFontStyle.MODERN,
                            modifier = Modifier.weight(1f)
                        ) { OchagThemeController.setFontStyle(OchagFontStyle.MODERN) }

                        FontChoice(
                            title = "Классика",
                            selected = fontStyle == OchagFontStyle.CLASSIC,
                            modifier = Modifier.weight(1f)
                        ) { OchagThemeController.setFontStyle(OchagFontStyle.CLASSIC) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OchagCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column {
                    Text(
                        text = "Контакты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrownTextPrimary
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Номер нужен, чтобы близкие могли найти тебя через контакты телефона.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrownTextSecondary
                    )

                    Spacer(Modifier.height(14.dp))

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

            Spacer(Modifier.height(12.dp))

            ProfileAction("Стикеры", "Встроенные и свои стикерпаки", onStickersClick)
            ProfileAction("Семья", "Участники, роли и доступ", onFamilyClick)
            ProfileAction("Пригласить близких", "Добавить нового участника", onInviteClick)

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerracottaDark)
            ) {
                Text("Выйти", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = title,
                maxLines = 1,
                color = if (selected) TerracottaDark else BrownTextSecondary
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CardCream,
            selectedContainerColor = TerracottaSoft
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BorderSoft,
            selectedBorderColor = TerracottaSoft
        )
    )
}

@Composable
private fun ProfileAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OchagCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 3.dp,
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = TerracottaSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = title.firstOrNull()?.uppercase() ?: "О",
                        color = TerracottaDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrownTextSecondary
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = BrownTextSecondary
            )
        }
    }
}
