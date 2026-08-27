package com.ochag.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface ChatDao {

    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertChat(
        chat: ChatEntity
    )


    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertChats(
        chats: List<ChatEntity>
    )


    @Query(
        """
        SELECT * FROM chats
        ORDER BY lastActivityAt DESC, id ASC
        """
    )
    suspend fun getAllChats():
            List<ChatEntity>


    @Query(
        "SELECT COUNT(*) FROM chats"
    )
    suspend fun countChats():
            Int


    @Query(
        "SELECT MAX(id) FROM chats"
    )
    suspend fun getMaxId():
            Long?


    @Query(
        """
        UPDATE chats
        SET lastMessage = :lastMessage,
            lastTime = :lastTime,
            lastActivityAt = :lastActivityAt
        WHERE id = :chatId
        """
    )
    suspend fun updateLastMessage(
        chatId: Long,
        lastMessage: String,
        lastTime: String,
        lastActivityAt: Long
    )


    @Query(
        """
        UPDATE chats
        SET unreadCount = :unreadCount
        WHERE id = :chatId
        """
    )
    suspend fun updateUnreadCount(
        chatId: Long,
        unreadCount: Int
    )


    @Query(
        """
        DELETE FROM chats
        WHERE id = :chatId
        """
    )
    suspend fun deleteChat(
        chatId: Long
    )
}
