package com.ochag.app.ui.screens.chats

import com.ochag.app.ui.localization.ochagText
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.data.Chat
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.ChatType
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagBottomBar
import com.ochag.app.ui.components.OchagTextField
import com.ochag.app.ui.theme.*
import java.util.Locale

@Composable
fun ChatListScreen(
    onFilesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
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
        .sortedWith(
            compareByDescending<Chat> { it.id == ChatRepository.FAVORITES_CHAT_ID }
                .thenByDescending { it.lastActivityAt }
                .thenBy { it.id }
        )

    val currentUserName = ServerRealtime.userName.ifBlank { ochagText("Очаг") }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            OchagBottomBar(
                items = listOf(ochagText("Чаты"), ochagText("Контакты"), ochagText("Звонки"), ochagText("Файлы"), ochagText("Профиль")),
                selectedIndex = 0,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) { index ->
                when (index) {
                    1 -> onContactsClick()
                    2 -> onCallsClick()
                    3 -> onFilesClick()
                    4 -> onProfileClick()
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
                    .padding(top = 8.dp, end = 6.dp),
                fontSize = 74.sp,
                color = SoftGreen.copy(alpha = 0.12f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = TerracottaSoft
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "⌂",
                                color = TerracottaDark,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = ochagText("Очаг"),
                        style = MaterialTheme.typography.titleMedium,
                        color = BrownTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.weight(1f))

                    Surface(
                        onClick = onProfileClick,
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = AvatarPeach,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currentUserName.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: ochagText("О"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrownTextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = ochagText("Чаты"),
                    style = MaterialTheme.typography.displaySmall,
                    color = BrownTextPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = when (connectionState) {
                        ServerRealtime.ConnectionState.ONLINE -> ochagText("Только свои люди ♥")
                        ServerRealtime.ConnectionState.CONNECTING -> ochagText("Собираем близких рядом…")
                        ServerRealtime.ConnectionState.OFFLINE -> ochagText("Сообщения останутся рядом")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrownTextSecondary
                )

                Spacer(Modifier.height(12.dp))

                OchagTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = ochagText("Поиск"),
                    placeholder = ochagText("Чат, человек или сообщение")
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { unreadOnly = !unreadOnly },
                        shape = RoundedCornerShape(18.dp),
                        color = if (unreadOnly) TerracottaSoft else CardWhite,
                        border = BorderStroke(1.dp, if (unreadOnly) TerracottaSoft else BorderSoft)
                    ) {
                        Text(
                            text = if (unreadOnly) ochagText("● Непрочитанные") else ochagText("Непрочитанные"),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (unreadOnly) TerracottaDark else BrownTextSecondary
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (searchText.isNotBlank() || unreadOnly) {
                        TextButton(
                            onClick = {
                                searchText = ""
                                unreadOnly = false
                            }
                        ) {
                            Text(ochagText("Сбросить"), color = TerracottaPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                if (sortedChats.isEmpty()) {
                    EmptyChatsState(filtered = searchText.isNotBlank() || unreadOnly)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
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
}

@Composable
private fun EmptyChatsState(filtered: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = RoundedCornerShape(28.dp),
                color = TerracottaSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (filtered) "⌕" else "♡",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TerracottaPrimary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (filtered) ochagText("Ничего не нашли") else ochagText("Здесь будут ваши чаты"),
                style = MaterialTheme.typography.titleLarge,
                color = BrownTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (filtered) ochagText("Попробуй изменить поиск") else ochagText("Начни разговор с близкими"),
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chat.id == ChatRepository.FAVORITES_CHAT_ID) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = AvatarSand
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("★", fontSize = 24.sp, color = TerracottaPrimary)
                    }
                }
            } else if (chat.type == ChatType.PRIVATE) {
                OchagAvatar(
                    name = chat.title,
                    size = 52.dp,
                    online = isOnline,
                    backgroundColor = avatarColor(chat.id)
                )
            } else {
                GroupAvatar(chat)
            }

            Spacer(Modifier.width(11.dp))

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
                        Text(
                            text = chat.lastTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = BrownTextMuted
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = presenceText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (chat.type == ChatType.PRIVATE && isOnline) OnlineGreen else BrownTextMuted
                )

                Spacer(Modifier.height(5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage.ifBlank {
                            when (chat.type) {
                                ChatType.FAMILY -> ochagText("Семейный чат")
                                ChatType.PRIVATE -> ochagText("Личный разговор")
                                ChatType.GROUP -> ochagText("Групповой чат")
                                ChatType.THEME -> ochagText("Тематический чат")
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
    val context = LocalContext.current
    val avatarUri = context.getSharedPreferences(
        "ochag_group_avatars",
        android.content.Context.MODE_PRIVATE
    ).getString("group_avatar_${chat.id}", "").orEmpty()

    if (avatarUri.isNotBlank()) {
        GroupAvatarImage(
            uri = Uri.parse(avatarUri),
            modifier = Modifier.size(52.dp)
        )
        return
    }

    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = when (chat.type) {
            ChatType.FAMILY -> AvatarPeach
            ChatType.GROUP -> AvatarSage
            ChatType.THEME -> AvatarSand
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

@Composable
private fun GroupAvatarImage(
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
            contentDescription = ochagText("Аватар группы"),
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(modifier = modifier, shape = CircleShape, color = AvatarSage) {}
    }
}

private fun avatarColor(id: Long): Color = when ((id % 4).toInt()) {
    0 -> AvatarPeach
    1 -> AvatarSage
    2 -> AvatarSand
    else -> AvatarRose
}

private fun buildPresenceText(chat: Chat, onlineUsers: Set<String>): String {
    if (chat.id == ChatRepository.FAVORITES_CHAT_ID) {
        return ochagText("Только для вас")
    }

    if (chat.type == ChatType.PRIVATE) {
        return if (isPrivateChatOnline(chat, onlineUsers)) ochagText("● В сети") else ochagText("Не в сети")
    }

    val normalizedMembers = chat.members
        .map(::normalizePresenceName)
        .filter { it.isNotBlank() }
        .distinct()

    val onlineCount = normalizedMembers.count { onlineUsers.contains(it) }
    val totalCount = normalizedMembers.size

    return if (totalCount > 0) "$onlineCount из $totalCount сейчас в сети" else ochagText("Пока никого в сети")
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
    ChatType.PRIVATE -> chat.title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: ochagText("О")
}
