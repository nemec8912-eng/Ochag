package com.ochag.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerAccountApi
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
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
    onLogoutClick: () -> Unit = {}
) {
    val user = OchagAppState.currentUser
    val userName = user?.name ?: "Гость"
    val profiles by ServerRealtime.serverUserProfiles.collectAsState()
    val connectionState by ServerRealtime.connectionState.collectAsState()

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CreamBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            OchagSectionHeader(
                title = "Профиль",
                subtitle = "Личное пространство внутри семьи"
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
                            saveStatus = if (result.success) {
                                "Номер сохранён"
                            } else {
                                result.error
                            }

                            if (result.success) {
                                ServerRealtime.requestServerUsers()
                            }
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

            Spacer(Modifier.height(14.dp))

            ProfileAction("Чаты", "Переписка с близкими", onChatsClick)
            ProfileAction("Семья", "Участники и роли", onFamilyClick)
            ProfileAction("Файлы", "Фото и документы семьи", onFilesClick)
            ProfileAction("Пригласить близких", "Добавить нового участника", onInviteClick)

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TerracottaDark
                )
            ) {
                Text(
                    text = "Выйти",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
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
