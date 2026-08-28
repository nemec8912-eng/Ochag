package com.ochag.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MessageActions {

    suspend fun editOwnTextMessage(
        context: Context,
        message: Message,
        newText: String
    ): Boolean {
        val cleanText = newText.trim()
        if (!message.isMine || message.messageType != MessageType.TEXT.name || cleanText.isBlank()) {
            return false
        }

        withContext(Dispatchers.IO) {
            OchagDatabase
                .getInstance(context.applicationContext)
                .messageDao()
                .updateMessageText(message.id, cleanText)
        }

        return true
    }

    suspend fun deleteOwnMessage(
        context: Context,
        message: Message
    ): Boolean {
        if (!message.isMine) return false
        return deleteLocalMessage(context, message)
    }

    suspend fun deleteLocalMessage(
        context: Context,
        message: Message
    ): Boolean {
        withContext(Dispatchers.IO) {
            OchagDatabase
                .getInstance(context.applicationContext)
                .messageDao()
                .deleteMessage(message.id)
        }
        return true
    }

    suspend fun deleteLocalMessages(
        context: Context,
        messages: Collection<Message>
    ): Int {
        val unique = messages.distinctBy { it.id }
        withContext(Dispatchers.IO) {
            val dao = OchagDatabase
                .getInstance(context.applicationContext)
                .messageDao()
            unique.forEach { dao.deleteMessage(it.id) }
        }
        return unique.size
    }

    suspend fun deleteAllLocalMessages(
        context: Context,
        chatId: Long
    ) {
        withContext(Dispatchers.IO) {
            OchagDatabase
                .getInstance(context.applicationContext)
                .messageDao()
                .deleteChatMessages(chatId)
        }
    }
}
