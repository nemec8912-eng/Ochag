package com.ochag.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey


enum class ChatType {
    FAMILY,
    PRIVATE,
    GROUP,
    THEME
}


enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    AUDIO,
    VIDEO_CIRCLE
}


enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}


data class Chat(
    val id: Long,
    val title: String,
    val type: ChatType,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int = 0,
    val members: List<String>,
    val lastActivityAt: Long = 0L
)


@Entity(
    tableName = "messages"
)
data class Message(

    @PrimaryKey
    val id: Long,

    val chatId: Long,

    val sender: String,

    val text: String,

    val time: String,

    val isMine: Boolean,

    val messageType: String =
        MessageType.TEXT.name,

    val attachmentUri: String = "",

    val fileName: String = "",

    val status: String =
        MessageStatus.SENT.name,

    val createdAt: Long =
        System.currentTimeMillis()
)
