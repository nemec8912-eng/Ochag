package com.ochag.app.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ochag.app.data.Chat
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.ChatType
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagPrimaryButton
import com.ochag.app.ui.theme.*
import java.util.Locale

private data class ChatPresence(
    val onlineMembers: List<String>,
    val text: String
) {
    val hasOnlineMembers: Boolean get() = onlineMembers.isNotEmpty()
}

@Composable
fun HomeScreen(
    onChatClick: (Long) -> Unit,
    onChatsClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onFamilyClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()
    val connectionState by ServerRealtime.connectionState.collectAsState()

    val normalizedOnlineUsers = remember(onlineUsers) {
        onlineUsers.map(::normalizePresenceName).filter { it.isNotBlank() }.toSet()
    }

    val sortedChats = ChatRepository.chats
        .sortedWith(compareByDescending<Chat> { it.lastActivityAt }.thenBy { it.id })
        .take(5)

    val userName = OchagAppState.currentUser?.name
        ?.takeIf { it.isNotBlank() }
        ?: ServerRealtime.userName.ifBlank { "Михаил" }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            HomeBottomBar(
                onChatsClick = onChatsClick,
                onFamilyClick = onFamilyClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBackground)
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeHeader(
                    userName = userName,
                    online = connectionState == ServerRealtime.ConnectionState.ONLINE,
                    onProfileClick = onProfileClick
                )
            }

            item {
                Text(
                    text = "Свои рядом ♥",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrownTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (connectionState) {
                        ServerRealtime.ConnectionState.ONLINE -> "Семья на связи"
                        ServerRealtime.ConnectionState.CONNECTING -> "Подключаемся к близким…"
                        ServerRealtime.ConnectionState.OFFLINE -> "Сообщения останутся рядом и без сети"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrownTextSecondary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickHomeCard(
                        modifier = Modifier.weight(1f),
                        symbol = "♡",
                        title = "Семья",
                        subtitle = "Участники",
                        tint = SoftGreenLight,
                        onClick = onFamilyClick
                    )
                    QuickHomeCard(
                        modifier = Modifier.weight(1f),
                        symbol = "+",
                        title = "Новый чат",
                        subtitle = "Начать разговор",
                        tint = TerracottaSoft,
                        onClick = onNewChatClick
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Последние чаты",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = BrownTextPrimary
                    )
                    TextButton(onClick = onChatsClick) {
                        Text("Все чаты", color = TerracottaDark)
                    }
                }
            }

            if (sortedChats.isEmpty()) {
                item {
                    OchagCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = TerracottaSoft
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("♡", style = MaterialTheme.typography.headlineMedium, color = TerracottaPrimary)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Здесь появятся разговоры",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BrownTextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Начни чат с кем-то из близких",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrownTextSecondary
                            )
                            Spacer(Modifier.height(16.dp))
                            OchagPrimaryButton(text = "Новый чат", onClick = onNewChatClick)
                        }
                    }
                }
            } else {
                items(sortedChats, key = { it.id }) { chat ->
                    HomeChatCard(
                        chat = chat,
                        presence = chatPresence(chat, normalizedOnlineUsers),
                        onClick = { onChatClick(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    online: Boolean,
    onProfileClick: () -> Unit
) {
    OchagCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(15.dp),
        elevation = 4.dp,
        onClick = onProfileClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(18.dp),
                color = TerracottaSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⌂",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TerracottaPrimary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Очаг",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrownTextPrimary
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrownTextSecondary
                )
            }

            OchagAvatar(
                name = userName,
                size = 48.dp,
                online = online,
                backgroundColor = Color(0xFFF4E0D3)
            )
        }
    }
}

@Composable
private fun QuickHomeCard(
    modifier: Modifier,
    symbol: String,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    OchagCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        elevation = 3.dp,
        onClick = onClick
    ) {
        Column {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = tint
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleLarge,
                        color = TerracottaDark
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
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
    }
}

@Composable
private fun HomeChatCard(
    chat: Chat,
    presence: ChatPresence,
    onClick: () -> Unit
) {
    OchagCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(15.dp),
        elevation = 4.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chat.type == ChatType.PRIVATE) {
                OchagAvatar(
                    name = chat.title,
                    size = 56.dp,
                    online = presence.hasOnlineMembers,
                    backgroundColor = TerracottaSoft
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = when (chat.type) {
                        ChatType.FAMILY -> Color(0xFFF5DDD0)
                        ChatType.GROUP -> SoftGreenLight
                        ChatType.THEME -> Color(0xFFF0E4CE)
                        ChatType.PRIVATE -> TerracottaSoft
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = avatarText(chat),
                            style = MaterialTheme.typography.headlineSmall,
                            color = BrownTextPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Text(
                            text = chat.lastTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = BrownTextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    text = chat.lastMessage.ifBlank {
                        when (chat.type) {
                            ChatType.FAMILY -> "Семейный чат"
                            ChatType.PRIVATE -> "Личный разговор"
                            ChatType.GROUP -> "Групповой чат"
                            ChatType.THEME -> "Тематический чат"
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrownTextSecondary
                )

                Spacer(Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = presence.text,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (presence.hasOnlineMembers) OnlineGreen else BrownTextSecondary
                    )

                    if (chat.unreadCount > 0) {
                        Surface(shape = CircleShape, color = TerracottaPrimary) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    onChatsClick: () -> Unit,
    onFamilyClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = CardWhite,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeBottomItem("Чаты", "◯", true, Modifier.weight(1f), onChatsClick)
            HomeBottomItem("Семья", "♡", false, Modifier.weight(1f), onFamilyClick)
            HomeBottomItem("Профиль", "○", false, Modifier.weight(1f), onProfileClick)
        }
    }
}

@Composable
private fun HomeBottomItem(
    title: String,
    symbol: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) TerracottaPrimary else BrownTextSecondary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TerracottaPrimary else BrownTextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) TerracottaPrimary else Color.Transparent)
        )
    }
}

private fun chatPresence(chat: Chat, onlineUsers: Set<String>): ChatPresence {
    val myName = normalizePresenceName(ServerRealtime.userName)

    val onlineMembers = chat.members
        .filter {
            val normalized = normalizePresenceName(it)
            normalized.isNotBlank() && normalized != myName && onlineUsers.contains(normalized)
        }
        .distinctBy(::normalizePresenceName)
        .toMutableList()

    if (chat.type == ChatType.PRIVATE) {
        val titleCandidate = normalizePresenceName(chat.title)
        if (
            titleCandidate.isNotBlank() &&
            titleCandidate != myName &&
            onlineUsers.contains(titleCandidate) &&
            onlineMembers.none { normalizePresenceName(it) == titleCandidate }
        ) {
            onlineMembers.add(chat.title)
        }
    }

    return when (chat.type) {
        ChatType.PRIVATE -> if (onlineMembers.isNotEmpty()) {
            ChatPresence(onlineMembers, "В сети")
        } else {
            ChatPresence(emptyList(), "Не в сети")
        }

        ChatType.FAMILY,
        ChatType.GROUP,
        ChatType.THEME -> if (onlineMembers.isNotEmpty()) {
            ChatPresence(onlineMembers, "${onlineMembers.size} сейчас в сети")
        } else {
            ChatPresence(emptyList(), "Сейчас никого в сети")
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

private fun avatarText(chat: Chat): String = when (chat.type) {
    ChatType.FAMILY -> "⌂"
    ChatType.THEME -> "✦"
    ChatType.GROUP -> "♡"
    ChatType.PRIVATE -> chat.title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "О"
}
