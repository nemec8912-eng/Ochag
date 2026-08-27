package com.ochag.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = "chats"
)
data class ChatEntity(

    @PrimaryKey
    val id: Long,

    val title: String,

    val type: String,

    val lastMessage: String,

    val lastTime: String,

    val unreadCount: Int,

    val members: String,

    val lastActivityAt: Long = 0L
)
