package com.ochag.app.ui.screens.family

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagSectionHeader
import com.ochag.app.ui.theme.*
import java.util.Locale

@Composable
fun FamilyManagementScreen(
    onMemberClick: (String) -> Unit
) {
    val members = listOf(
        "Михаил",
        "Анна",
        "Дочь",
        "Сын",
        "Бабушка"
    )

    val currentUserName = OchagAppState.currentUser?.name.orEmpty()
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()

    val normalizedOnlineUsers = remember(onlineUsers) {
        onlineUsers
            .map(::normalizePresenceName)
            .filter { it.isNotBlank() }
            .toSet()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CreamBackground
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OchagSectionHeader(
                    title = "Семья и приглашения",
                    subtitle = "Только свои люди — доступ под твоим контролем"
                )
            }

            item {
                val owner = currentUserName.ifBlank { "Михаил" }
                val ownerOnline = normalizedOnlineUsers.contains(normalizePresenceName(owner))

                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 5.dp,
                    contentPadding = PaddingValues(18.dp),
                    onClick = { onMemberClick(owner) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OchagAvatar(
                            name = owner,
                            size = 72.dp,
                            online = ownerOnline,
                            backgroundColor = TerracottaSoft
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = owner,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BrownTextPrimary
                            )

                            Spacer(Modifier.height(4.dp))

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TerracottaSoft
                            ) {
                                Text(
                                    text = "Владелец семьи",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TerracottaDark
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = if (ownerOnline) "В сети" else "Не в сети",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (ownerOnline) OnlineGreen else BrownTextSecondary
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

            item {
                Text(
                    text = "Участники семьи",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )
            }

            items(
                items = members,
                key = { it }
            ) { member ->
                val isOnline = normalizedOnlineUsers.contains(normalizePresenceName(member))
                val isOwner = member.equals(currentUserName, ignoreCase = true)

                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
                    onClick = { onMemberClick(member) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OchagAvatar(
                            name = member,
                            size = 54.dp,
                            online = isOnline,
                            backgroundColor = if (isOwner) TerracottaSoft else SoftGreenLight
                        )

                        Spacer(Modifier.width(13.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = member,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrownTextPrimary
                                )

                                if (isOwner) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Вы",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TerracottaDark
                                    )
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = if (isOnline) "В сети" else "Не в сети",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOnline) OnlineGreen else BrownTextSecondary
                            )
                        }

                        Text(
                            text = "⋮",
                            style = MaterialTheme.typography.titleLarge,
                            color = BrownTextSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Приглашения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )
            }

            item {
                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OchagAvatar(
                            name = "+",
                            size = 52.dp,
                            backgroundColor = TerracottaSoft
                        )

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Пригласить близких",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BrownTextPrimary
                            )
                            Text(
                                text = "Коды приглашения подключим отдельным шагом",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrownTextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = TerracottaSoft
                        ) {
                            Text(
                                text = "Скоро",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TerracottaDark
                            )
                        }
                    }
                }
            }

            item {
                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 3.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Безопасность семьи",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BrownTextPrimary
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = "Вход новых участников будет происходить только после приглашения и подтверждения владельца.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrownTextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun normalizePresenceName(value: String): String {
    val decoded = try {
        Uri.decode(value)
    } catch (_: Exception) {
        value
    }

    return decoded
        .trim()
        .lowercase(Locale.getDefault())
        .replace(Regex("[^\\p{L}\\p{N}]+"), "")
}
