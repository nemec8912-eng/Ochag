package com.ochag.app.ui.screens.chats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ochag.app.data.Message

object MessageSelectionController {
    val selectedIds = mutableStateListOf<Long>()

    var actionMessage by mutableStateOf<Message?>(null)
        private set

    var activeChatId by mutableStateOf<Long?>(null)
        private set

    val isSelecting: Boolean
        get() = selectedIds.isNotEmpty()

    fun attach(chatId: Long) {
        if (activeChatId != chatId) {
            clear()
            activeChatId = chatId
        }
    }

    fun detach(chatId: Long) {
        if (activeChatId == chatId) {
            clear()
            activeChatId = null
        }
    }

    fun openActions(message: Message) {
        actionMessage = message
    }

    fun closeActions() {
        actionMessage = null
    }

    fun startSelection(messageId: Long) {
        actionMessage = null
        selectedIds.clear()
        selectedIds.add(messageId)
    }

    fun toggle(messageId: Long) {
        if (selectedIds.contains(messageId)) {
            selectedIds.remove(messageId)
        } else {
            selectedIds.add(messageId)
        }
    }

    fun selectAll(messages: List<Message>) {
        selectedIds.clear()
        selectedIds.addAll(messages.map { it.id }.distinct())
    }

    fun clear() {
        selectedIds.clear()
        actionMessage = null
    }
}
