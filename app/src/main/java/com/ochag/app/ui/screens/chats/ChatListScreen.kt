package com.ochag.app.ui.screens.chats

import android.net.Uri

import androidx.compose.foundation.background
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
import com.ochag.app.data.ServerRealtime

import java.util.Locale


private val OnlineGreen =
    Color(0xFF2E7D32)


@Composable
fun ChatListScreen(
    onProfileClick: () -> Unit = {},
    onChatClick: (
        Long,
        String
    ) -> Unit = { _, _ -> }
) {

    val onlineUsers by
    ServerRealtime
        .onlineUsers
        .collectAsState()


    val connectionState by
    ServerRealtime
        .connectionState
        .collectAsState()

    var searchText by remember {
        mutableStateOf("")
    }

    var unreadOnly by remember {
        mutableStateOf(false)
    }

    val normalizedOnlineUsers =
        remember(
            onlineUsers
        ) {
            onlineUsers
                .map {
                    normalizePresenceName(
                        it
                    )
                }
                .filter {
                    it.isNotBlank()
                }
                .toSet()
        }

    val sortedChats =
        ChatRepository
            .chats
            .filter {
                    chat ->

                val matchesSearch =
                    searchText.isBlank() ||
                            chat.title.contains(
                                searchText.trim(),
                                ignoreCase = true
                            ) ||
                            chat.lastMessage.contains(
                                searchText.trim(),
                                ignoreCase = true
                            ) ||
                            chat.members.any {
                                it.contains(
                                    searchText.trim(),
                                    ignoreCase = true
                                )
                            }

                val matchesUnread =
                    !unreadOnly ||
                            chat.unreadCount > 0

                matchesSearch &&
                        matchesUnread
            }
            .sortedWith(
                compareByDescending<Chat> {
                    it.lastActivityAt
                }
                    .thenBy {
                        it.id
                    }
            )

    val profileLetter =
        ServerRealtime
            .userName
            .firstOrNull {
                it.isLetterOrDigit()
            }
            ?.uppercase()
            ?: "О"


    Surface(
        modifier =
            Modifier.fillMaxSize(),
        color =
            MaterialTheme
                .colorScheme
                .background
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 16.dp
                    )
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text = "Очаг",
                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            when (
                                connectionState
                            ) {

                                ServerRealtime.ConnectionState.ONLINE ->
                                    "Все чаты • сервер онлайн"

                                ServerRealtime.ConnectionState.CONNECTING ->
                                    "Все чаты • подключение…"

                                ServerRealtime.ConnectionState.OFFLINE ->
                                    "Все чаты • офлайн"
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                FilledTonalButton(
                    onClick =
                        onProfileClick,
                    modifier =
                        Modifier.size(
                            48.dp
                        ),
                    shape =
                        CircleShape,
                    contentPadding =
                        PaddingValues(
                            0.dp
                        )
                ) {

                    Text(
                        profileLetter
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            OutlinedTextField(
                value =
                    searchText,
                onValueChange = {
                    searchText = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine =
                    true,
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                label = {
                    Text(
                        "Поиск по чатам"
                    )
                },
                placeholder = {
                    Text(
                        "Название, участник или сообщение"
                    )
                }
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                FilterChip(
                    selected =
                        unreadOnly,
                    onClick = {
                        unreadOnly =
                            !unreadOnly
                    },
                    label = {
                        Text(
                            "Только непрочитанные"
                        )
                    }
                )


                Spacer(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                if (
                    searchText.isNotBlank() ||
                    unreadOnly
                ) {

                    TextButton(
                        onClick = {
                            searchText = ""
                            unreadOnly = false
                        }
                    ) {

                        Text(
                            "Сбросить"
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    ),
                contentPadding =
                    PaddingValues(
                        bottom = 20.dp
                    )
            ) {

                items(
                    items =
                        sortedChats,
                    key = {
                        it.id
                    }
                ) { chat ->

                    val isOnline =
                        isPrivateChatOnline(
                            chat =
                                chat,
                            onlineUsers =
                                normalizedOnlineUsers
                        )

                    val presenceText =
                        buildPresenceText(
                            chat =
                                chat,
                            onlineUsers =
                                normalizedOnlineUsers
                        )

                    ChatCard(
                        chat =
                            chat,
                        isOnline =
                            isOnline,
                        presenceText =
                            presenceText,
                        onClick = {
                            onChatClick(
                                chat.id,
                                chat.title
                            )
                        }
                    )
                }
            }
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
        onClick =
            onClick,
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        15.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier.size(
                        54.dp
                    )
            ) {

                Surface(
                    modifier =
                        Modifier.fillMaxSize(),
                    shape =
                        CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                avatarText(
                                    chat
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }


                if (
                    chat.type ==
                    ChatType.PRIVATE
                ) {

                    Box(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.BottomEnd
                                )
                                .size(
                                    15.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    if (
                                        isOnline
                                    ) {
                                        OnlineGreen
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .outlineVariant
                                    }
                                )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            chat.title,
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            if (
                                chat.unreadCount > 0
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.SemiBold
                            }
                    )


                    if (
                        chat.lastTime.isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )

                        Text(
                            text =
                                chat.lastTime,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        presenceText,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        if (
                            chat.type ==
                            ChatType.PRIVATE &&
                            isOnline
                        ) {
                            OnlineGreen
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            chat
                                .lastMessage
                                .ifBlank {
                                    when (
                                        chat.type
                                    ) {
                                        ChatType.FAMILY ->
                                            "Семейный чат"

                                        ChatType.PRIVATE ->
                                            "Личный чат"

                                        ChatType.GROUP ->
                                            "Групповой чат"

                                        ChatType.THEME ->
                                            "Тематический чат"
                                    }
                                },
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    if (
                        chat.unreadCount > 0
                    ) {

                        Spacer(
                            modifier =
                                Modifier.width(
                                    10.dp
                                )
                        )

                        Surface(
                            shape =
                                CircleShape,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        ) {

                            Text(
                                text =
                                    if (
                                        chat.unreadCount > 99
                                    ) {
                                        "99+"
                                    } else {
                                        chat
                                            .unreadCount
                                            .toString()
                                    },
                                modifier =
                                    Modifier.padding(
                                        horizontal = 9.dp,
                                        vertical = 4.dp
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary,
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun buildPresenceText(
    chat: Chat,
    onlineUsers: Set<String>
): String {

    if (
        chat.type ==
        ChatType.PRIVATE
    ) {

        return if (
            isPrivateChatOnline(
                chat,
                onlineUsers
            )
        ) {
            "● В сети"
        } else {
            "○ Не в сети"
        }
    }


    val normalizedMembers =
        chat
            .members
            .map {
                normalizePresenceName(
                    it
                )
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()

    val onlineCount =
        normalizedMembers.count {
            onlineUsers.contains(
                it
            )
        }

    val totalCount =
        normalizedMembers.size

    return if (
        totalCount > 0
    ) {
        "$onlineCount/$totalCount в сети • $totalCount участников"
    } else {
        "0 в сети"
    }
}


private fun isPrivateChatOnline(
    chat: Chat,
    onlineUsers: Set<String>
): Boolean {

    if (
        chat.type !=
        ChatType.PRIVATE
    ) {
        return false
    }

    val myName =
        normalizePresenceName(
            ServerRealtime.userName
        )

    return chat
        .members
        .map {
            normalizePresenceName(
                it
            )
        }
        .filter {
            it.isNotBlank() &&
                    it != myName
        }
        .any {
            onlineUsers.contains(
                it
            )
        }
}


private fun normalizePresenceName(
    value: String
): String {

    val decoded =
        try {
            Uri.decode(
                value
            )
        } catch (_: Exception) {
            value
        }

    return decoded
        .trim()
        .lowercase(
            Locale.getDefault()
        )
        .replace(
            Regex(
                "[^\\p{L}\\p{N}]+"
            ),
            ""
        )
}


private fun avatarText(
    chat: Chat
): String {

    return when (
        chat.type
    ) {

        ChatType.FAMILY ->
            "🔥"

        ChatType.THEME ->
            "🔨"

        ChatType.GROUP ->
            "👥"

        ChatType.PRIVATE ->
            chat
                .title
                .firstOrNull {
                    it.isLetterOrDigit()
                }
                ?.uppercase()
                ?: "О"
    }
}
