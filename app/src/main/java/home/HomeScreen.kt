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
import com.ochag.app.data.ServerRealtime

import java.util.Locale


private val OnlineGreen =
    Color(
        0xFF2E7D32
    )


private data class ChatPresence(
    val onlineMembers: List<String>,
    val text: String
) {

    val hasOnlineMembers: Boolean
        get() =
            onlineMembers.isNotEmpty()
}


@Composable
fun HomeScreen(

    onChatClick: (Long) -> Unit,

    onChatsClick: () -> Unit,

    onNewChatClick: () -> Unit,

    onFamilyClick: () -> Unit,

    onProfileClick: () -> Unit

) {


    val onlineUsers by
    ServerRealtime
        .onlineUsers
        .collectAsState()


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
            .sortedWith(
                compareByDescending<Chat> {
                    it.lastActivityAt
                }
                    .thenBy {
                        it.id
                    }
            )


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp
                )
    ) {


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )


        // =====================================================
        // ВЕРХ
        // =====================================================

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Surface(

                modifier =
                    Modifier.size(
                        80.dp
                    ),

                shape =
                    RoundedCornerShape(
                        25.dp
                    ),

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
                            "М",

                        style =
                            MaterialTheme
                                .typography
                                .headlineLarge,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        20.dp
                    )
            )


            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {


                Text(

                    text =
                        "Очаг",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )


                Text(

                    text =
                        "Михаил",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )


                Row(

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    Box(

                        modifier =
                            Modifier
                                .size(
                                    12.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    OnlineGreen
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                7.dp
                            )
                    )


                    Text(

                        text =
                            "В сети",

                        color =
                            OnlineGreen,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )


        Text(

            text =
                "Последние чаты",

            style =
                MaterialTheme
                    .typography
                    .titleLarge
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        // =====================================================
        // ЧАТЫ
        // =====================================================

        LazyColumn(

            modifier =
                Modifier.weight(
                    1f
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                ),

            contentPadding =
                PaddingValues(
                    bottom = 10.dp
                )
        ) {


            items(

                items =
                    sortedChats,

                key = {
                        chat ->
                    chat.id
                }

            ) { chat ->


                val presence =
                    chatPresence(

                        chat =
                            chat,

                        onlineUsers =
                            normalizedOnlineUsers
                    )


                HomeChatCard(

                    chat =
                        chat,

                    presence =
                        presence,

                    onClick = {

                        onChatClick(
                            chat.id
                        )
                    }
                )
            }
        }


        // =====================================================
        // НОВЫЙ ЧАТ
        // =====================================================

        Button(

            onClick =
                onNewChatClick,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        55.dp
                    ),

            shape =
                RoundedCornerShape(
                    28.dp
                )
        ) {


            Text(
                "＋ Новый чат"
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    15.dp
                )
        )


        // =====================================================
        // НИЖНЕЕ МЕНЮ
        // =====================================================

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceAround
        ) {


            TextButton(

                onClick =
                    onChatsClick
            ) {

                Text(
                    "💬\nЧаты"
                )
            }


            TextButton(

                onClick =
                    onFamilyClick
            ) {

                Text(
                    "👨‍👩‍👧\nСемья"
                )
            }


            TextButton(

                onClick =
                    onProfileClick
            ) {

                Text(
                    "👤\nПрофиль"
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )
    }
}


@Composable
private fun HomeChatCard(

    chat: Chat,

    presence: ChatPresence,

    onClick: () -> Unit

) {


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                ),

        shape =
            RoundedCornerShape(
                24.dp
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
                        16.dp
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
                                    presence.hasOnlineMembers
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


            Spacer(
                modifier =
                    Modifier.width(
                        14.dp
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

                        maxLines =
                            1,

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
                                    .bodySmall,

                            color =

                                if (
                                    chat.unreadCount > 0
                                ) {

                                    MaterialTheme
                                        .colorScheme
                                        .primary

                                } else {

                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                }
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
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

                        maxLines =
                            1,

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


                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                Text(

                    text =
                        presence.text,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis,

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    fontWeight =

                        if (
                            presence.hasOnlineMembers
                        ) {

                            FontWeight.SemiBold

                        } else {

                            FontWeight.Normal
                        },

                    color =

                        if (
                            presence.hasOnlineMembers
                        ) {

                            OnlineGreen

                        } else {

                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }
                )
            }
        }
    }
}


private fun chatPresence(

    chat: Chat,

    onlineUsers: Set<String>

): ChatPresence {


    val myName =
        normalizePresenceName(
            ServerRealtime.userName
        )


    val onlineMembers =
        chat
            .members
            .filter {

                val normalized =
                    normalizePresenceName(
                        it
                    )


                normalized.isNotBlank() &&
                        normalized != myName &&
                        onlineUsers.contains(
                            normalized
                        )
            }
            .distinctBy {
                normalizePresenceName(
                    it
                )
            }
            .toMutableList()


    /*
     * В личном чате используем ещё и название чата как запасной источник.
     * Это важно для старых чатов, где members мог сохраниться неидеально.
     *
     * Например:
     * title = "Анна ❤️"
     * presence = "Анна"
     * После нормализации оба становятся "анна".
     */
    if (
        chat.type ==
        ChatType.PRIVATE
    ) {

        val titleCandidate =
            normalizePresenceName(
                chat.title
            )


        if (
            titleCandidate.isNotBlank() &&
            titleCandidate != myName &&
            onlineUsers.contains(
                titleCandidate
            ) &&
            onlineMembers.none {
                normalizePresenceName(
                    it
                ) == titleCandidate
            }
        ) {

            onlineMembers.add(
                chat.title
            )
        }
    }


    return when (
        chat.type
    ) {


        ChatType.PRIVATE -> {


            if (
                onlineMembers.isNotEmpty()
            ) {


                ChatPresence(

                    onlineMembers =
                        onlineMembers,

                    text =
                        "● В сети"
                )


            } else {


                ChatPresence(

                    onlineMembers =
                        emptyList(),

                    text =
                        "○ Не в сети"
                )
            }
        }


        ChatType.FAMILY,
        ChatType.GROUP,
        ChatType.THEME -> {


            if (
                onlineMembers.isNotEmpty()
            ) {


                ChatPresence(

                    onlineMembers =
                        onlineMembers,

                    text =
                        "● В сети: ${
                            onlineMembers.joinToString(
                                ", "
                            )
                        }"
                )


            } else {


                ChatPresence(

                    onlineMembers =
                        emptyList(),

                    text =
                        "○ Никого нет в сети"
                )
            }
        }
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
