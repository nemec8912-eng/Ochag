package com.ochag.app.ui.screens.chats

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ochag.app.data.Chat
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.ChatType
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagBottomBar
import com.ochag.app.ui.components.OchagTextField
import com.ochag.app.ui.theme.BorderSoft
import com.ochag.app.ui.theme.BrownTextPrimary
import com.ochag.app.ui.theme.BrownTextSecondary
import com.ochag.app.ui.theme.CardWhite
import com.ochag.app.ui.theme.CreamBackground
import com.ochag.app.ui.theme.OnlineGreen
import com.ochag.app.ui.theme.TerracottaPrimary
import com.ochag.app.ui.theme.TerracottaSoft
import java.util.Locale

@Composable
fun ChatListScreen(
    onFilesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onChatClick: (Long, String) -> Unit = { _, _ -> }
) {
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()
    val connectionState by ServerRealtime.connectionState.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var unreadOnly by remember { mutableStateOf(false) }

    val normalizedOnlineUsers = remember(onlineUsers) {
        onlineUsers.map(::normalizePresenceName).filter { it.isNotBlank() }.toSet()
    }

    val sortedChats = ChatRepository.chats
        .filter { chat ->
            val query = searchText.trim()
            val matchesSearch = query.isBlank() ||
                chat.title.contains(query, ignoreCase = true) ||
                chat.lastMessage.contains(query, ignoreCase = true) ||
                chat.members.any { it.contains(query, ignoreCase = true) }
            val matchesUnread = !unreadOnly || chat.unreadCount > 0
            matchesSearch && matchesUnread
        }
        .sortedWith(compareByDescending<Chat> { it.lastActivityAt }.thenBy { it.id })

    val currentUserName = ServerRealtime.userName.ifBlank { "Очаг" }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            OchagBottomBar(
                items = listOf("Чаты", "Файлы", "Профиль"),
                selectedIndex = 0,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            ) { index ->
                when (index) {
                    1 -> onFilesClick()
                    2 -> onProfileClick()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Чаты",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrownTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when (connectionState) {
                            ServerRealtime.ConnectionState.ONLINE -> "Только свои люди ♥"
                            ServerRealtime.ConnectionState.CONNECTING -> "Подключаемся к близким…"
                            ServerRealtime.ConnectionState.OFFLINE -> "Сейчас без соединения"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrownTextSecondary
                    )
                }

                Surface(
                    onClick = onProfileClick,
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = TerracottaSoft,
                    shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentUserName.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "О",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OchagTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = "Поиск",
                placeholder = "Чат, участник или сообщение"
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = unreadOnly,
                    onClick = { unreadOnly = !unreadOnly },
                    label = {
                        Text(
                            text = "Непрочитанные",
                            color = if (unreadOnly) TerracottaPrimary else BrownTextSecondary
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CardWhite,
                        selectedContainerColor = TerracottaSoft
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = unreadOnly,
                        borderColor = BorderSoft,
                        selectedBorderColor = TerracottaSoft
                    )
                )

                Spacer(Modifier.weight(1f))

                if (searchText.isNotBlank() || unreadOnly) {
                    TextButton(onClick = {
                        searchText = ""
                        unreadOnly = false
                    }) {
                        Text("Сбросить", color = TerracottaPrimary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (sortedChats.isEmpty()) {
                EmptyChatsState(filtered = searchText.isNotBlank() || unreadOnly)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 22.dp)
                ) {
                    items(sortedChats, key = { it.id }) { chat ->
                        val isOnline = isPrivateChatOnline(chat, normalizedOnlineUsers)
                        val presenceText = buildPresenceText(chat, normalizedOnlineUsers)
                        ChatCard(
                            chat = chat,
                            isOnline = isOnline,
                            presenceText = presenceText,
                            onClick = { onChatClick(chat.id, chat.title) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsState(filtered: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(76.dp), shape = CircleShape, color = TerracottaSoft) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (filtered) "⌕" else "♡",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TerracottaPrimary
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (filtered) "Ничего не нашли" else "Здесь будут ваши чаты",
                style = MaterialTheme.typography.titleLarge,
                color = BrownTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (filtered) "Попробуй изменить поиск" else "Начни разговор с близкими",
                style = MaterialTheme.typography.bodyMedium,
                color = BrownTextSecondary
            )
        }
    }
}

@Composable
private fun ChatCard(
    chat: Chat,
    isOnline: Boolean,
    presenceText: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chat.type == ChatType.PRIVATE) {
                OchagAvatar(name = chat.title, size = 58.dp, online = isOnline)
            } else {
                GroupAvatar(chat)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        color = BrownTextPrimary
                    )
                    if (chat.lastTime.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(chat.lastTime, style = MaterialTheme.typography.labelMedium, color = BrownTextSecondary)
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = presenceText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (chat.type == ChatType.PRIVATE && isOnline) OnlineGreen else BrownTextSecondary
                )

                Spacer(Modifier.height(7.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage.ifBlank {
                            when (chat.type) {
                                ChatType.FAMILY -> "Семейный чат"
                                ChatType.PRIVATE -> "Личный разговор"
                                ChatType.GROUP -> "Групповой чат"
                                ChatType.THEME -> "Тематический чат"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrownTextSecondary
                    )
                    if (chat.unreadCount > 0) {
                        Spacer(Modifier.width(10.dp))
                        Surface(shape = CircleShape, color = TerracottaPrimary) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupAvatar(chat: Chat) {
    Surface(
        modifier = Modifier.size(58.dp),
        shape = RoundedCornerShape(20.dp),
        color = when (chat.type) {
            ChatType.FAMILY -> Color(0xFFF6DCCF)
            ChatType.GROUP -> Color(0xFFE4EEE8)
            ChatType.THEME -> Color(0xFFF1E6D1)
            ChatType.PRIVATE -> TerracottaSoft
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = avatarText(chat), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun buildPresenceText(chat: Chat, onlineUsers: Set<String>): String {
    if (chat.type == ChatType.PRIVATE) {
        return if (isPrivateChatOnline(chat, onlineUsers)) "● В сети" else "Не в сети"
    }

    val normalizedMembers = chat.members
        .map(::normalizePresenceName)
        .filter { it.isNotBlank() }
        .distinct()

    val onlineCount = normalizedMembers.count { onlineUsers.contains(it) }
    val totalCount = normalizedMembers.size

    return if (totalCount > 0) "$onlineCount из $totalCount сейчас в сети" else "Пока никого в сети"
}

private fun isPrivateChatOnline(chat: Chat, onlineUsers: Set<String>): Boolean {
    if (chat.type != ChatType.PRIVATE) return false
    val myName = normalizePresenceName(ServerRealtime.userName)
    return chat.members
        .map(::normalizePresenceName)
        .filter { it.isNotBlank() && it != myName }
        .any { onlineUsers.contains(it) }
}

private fun normalizePresenceName(value: String): String {
    val decoded = try { Uri.decode(value) } catch (_: Exception) { value }
    return decoded
        .trim()
        .lowercase(Locale.getDefault())
        .replace(Regex("[^\\p{L}\\p{N}]+"), "")
}

private fun avatarText(chat: Chat): String = when (chat.type) {
    ChatType.FAMILY -> "⌂"
    ChatType.THEME -> "✦"
    ChatType.GROUP -> "👥"
    ChatType.PRIVATE -> chat.title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "О"
}
