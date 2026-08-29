package com.ochag.app.ui.screens.family

import com.ochag.app.ui.localization.ochagText
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagPrimaryButton
import com.ochag.app.ui.theme.*
import java.util.Locale

@Composable
fun MemberProfileScreen(
    name: String,
    status: String,
    onBackClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()

    val normalizedOnlineUsers = remember(onlineUsers) {
        onlineUsers
            .map(::normalizePresenceName)
            .filter { it.isNotBlank() }
            .toSet()
    }

    val isOnline = normalizedOnlineUsers.contains(normalizePresenceName(name))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onBackClick),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = ochagText("Профиль"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(22.dp))

            OchagCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 5.dp,
                contentPadding = PaddingValues(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OchagAvatar(
                        name = name,
                        size = 112.dp,
                        online = isOnline,
                        backgroundColor = TerracottaSoft
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = if (isOnline) ochagText("В сети") else ochagText("Не в сети"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = TerracottaSoft
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = TerracottaDark
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OchagCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Column {
                    Text(
                        text = ochagText("Участник семьи"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = ochagText("Личная переписка доступна только внутри вашего семейного «Очага»."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            OchagPrimaryButton(
                text = ochagText("Написать сообщение"),
                onClick = onMessageClick
            )
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
