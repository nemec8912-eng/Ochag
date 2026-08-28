package com.ochag.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query(
        """
        SELECT * FROM messages
        WHERE chatId = :chatId
        ORDER BY createdAt ASC, id ASC
        """
    )
    fun observeMessages(chatId: Long): Flow<List<Message>>

    @Query(
        """
        SELECT * FROM messages
        WHERE messageType IN ('IMAGE', 'FILE', 'AUDIO', 'VIDEO_CIRCLE')
          AND attachmentUri != ''
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun observeMediaMessages(): Flow<List<Message>>

    @Query("SELECT COUNT(*) FROM messages WHERE id = :messageId")
    suspend fun messageExists(messageId: Long): Int

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun countMessages(chatId: Long): Int

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: String)

    @Query(
        """
        UPDATE messages
        SET status = :status
        WHERE chatId = :chatId
          AND isMine = 0
          AND status != :status
        """
    )
    suspend fun markIncomingMessages(chatId: Long, status: String)

    @Query(
        """
        SELECT id FROM messages
        WHERE chatId = :chatId
          AND isMine = 0
          AND status != 'READ'
        ORDER BY createdAt ASC, id ASC
        """
    )
    suspend fun getUnreadIncomingIds(chatId: Long): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE chatId = :chatId
          AND isMine = 0
          AND status != 'READ'
        """
    )
    suspend fun countUnreadIncoming(chatId: Long): Int

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteChatMessages(chatId: Long)
}
