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

        withContext(Dispatchers.IO) {
            OchagDatabase
                .getInstance(context.applicationContext)
                .messageDao()
                .deleteMessage(message.id)
        }

        return true
    }
}
