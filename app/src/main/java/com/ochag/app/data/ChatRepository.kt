package com.ochag.app.data

import android.content.Context
import android.net.Uri

import androidx.compose.runtime.mutableStateListOf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object ChatRepository {

    const val FAVORITES_CHAT_ID = 999_999L


    val chats =
        mutableStateListOf<Chat>()


    @Volatile
    private var activeChatId:
            Long? = null


    private fun messageDao(
        context: Context
    ): MessageDao {

        return OchagDatabase
            .getInstance(
                context.applicationContext
            )
            .messageDao()
    }


    private fun chatDao(
        context: Context
    ): ChatDao {

        return OchagDatabase
            .getInstance(
                context.applicationContext
            )
            .chatDao()
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    suspend fun initialize(
        context: Context
    ) {

        val appContext =
            context.applicationContext


        val entities =
            withContext(
                Dispatchers.IO
            ) {

                val dao =
                    chatDao(
                        appContext
                    )


                if (
                    dao.countChats() == 0
                ) {

                    dao.insertChats(
                        defaultChats()
                            .map {
                                it.toEntity()
                            }
                    )
                }


                var savedChats = dao.getAllChats()

                if (savedChats.none { it.id == FAVORITES_CHAT_ID }) {
                    val favorites = Chat(
                        id = FAVORITES_CHAT_ID,
                        title = "Избранные",
                        type = ChatType.PRIVATE,
                        lastMessage = "Личные сообщения",
                        lastTime = "",
                        unreadCount = 0,
                        members = emptyList()
                    )
                    dao.insertChat(favorites.toEntity())
                    savedChats = dao.getAllChats()
                }

                savedChats
            }


        chats.clear()


        chats.addAll(
            entities.map {
                it.toChat()
            }
        )
    }


    private fun defaultChats():
            List<Chat> {

        return listOf(

            Chat(
                id = 1L,
                title = "Семья",
                type = ChatType.FAMILY,
                lastMessage = "",
                lastTime = "",
                unreadCount = 0,
                members = listOf(
                    "Михаил",
                    "Анна",
                    "Дочь",
                    "Сын",
                    "Бабушка"
                )
            ),

            Chat(
                id = 2L,
                title = "Анна ❤️",
                type = ChatType.PRIVATE,
                lastMessage = "",
                lastTime = "",
                unreadCount = 0,
                members = listOf(
                    "Михаил",
                    "Анна"
                )
            ),

            Chat(
                id = 3L,
                title = "Ремонт дома 🔨",
                type = ChatType.THEME,
                lastMessage = "",
                lastTime = "",
                unreadCount = 0,
                members = listOf(
                    "Михаил",
                    "Анна"
                )
            )
        )
    }


    fun findChat(
        chatId: Long
    ): Chat? {

        return chats.firstOrNull {
            it.id == chatId
        }
    }


    fun getChats():
            List<Chat> {

        return chats.toList()
    }


    fun getMessages(
        context: Context,
        chatId: Long
    ): Flow<List<Message>> {

        return messageDao(context)
            .observeMessages(
                chatId
            )
    }


    suspend fun seedInitialMessages(
        context: Context,
        chatId: Long
    ) {

        withContext(
            Dispatchers.IO
        ) {

            messageDao(context)
                .countMessages(
                    chatId
                )
        }
    }


    // =========================================================
    // OPEN / CLOSE CHAT
    // =========================================================

    suspend fun markChatOpened(
        context: Context,
        chatId: Long
    ) {

        activeChatId =
            chatId


        val ids =
            withContext(
                Dispatchers.IO
            ) {

                messageDao(context)
                    .getUnreadIncomingIds(
                        chatId
                    )
            }


        if (
            ids.isNotEmpty()
        ) {

            withContext(
                Dispatchers.IO
            ) {

                messageDao(context)
                    .markIncomingMessages(
                        chatId = chatId,
                        status =
                            MessageStatus.READ.name
                    )
            }


            ServerRealtime
                .sendReadReceipt(
                    chatId = chatId,
                    messageIds = ids
                )
        }


        setUnreadCount(
            context = context,
            chatId = chatId,
            unreadCount = 0
        )
    }


    fun markChatClosed(
        chatId: Long
    ) {

        if (
            activeChatId ==
            chatId
        ) {

            activeChatId =
                null
        }
    }


    // =========================================================
    // SEND TEXT
    // =========================================================

    suspend fun sendMessage(
        context: Context,
        chatId: Long,
        text: String
    ) {

        val cleanText =
            text.trim()


        if (
            cleanText.isBlank()
        ) {

            return
        }


        val messageId =
            generateMessageId()


        val currentTime =
            getCurrentTime()


        val message =
            Message(
                id = messageId,
                chatId = chatId,
                sender = currentUserName(),
                text = cleanText,
                time = currentTime,
                isMine = true,
                messageType =
                    MessageType.TEXT.name,
                attachmentUri = "",
                fileName = "",
                status =
                    MessageStatus.SENDING.name,
                createdAt =
                    System.currentTimeMillis()
            )


        saveMessage(
            context,
            message
        )


        updateChatAfterMessage(
            context = context,
            chatId = chatId,
            message = cleanText,
            time = currentTime
        )

        if (chatId == FAVORITES_CHAT_ID) {
            updateOutgoingStatus(
                context = context,
                messageId = messageId,
                status = MessageStatus.READ
            )
            return
        }


        val sent =
            ServerRealtime
                .sendTextMessage(
                    messageId =
                        messageId,
                    chatId =
                        chatId,
                    text =
                        cleanText
                )


        if (!sent) {

            updateOutgoingStatus(
                context = context,
                messageId =
                    messageId,
                status =
                    MessageStatus.FAILED
            )
        }
    }


    // =========================================================
    // RECEIVE TEXT
    // =========================================================

    suspend fun receiveTextMessage(
        context: Context,
        messageId: Long,
        chatId: Long,
        sender: String,
        text: String
    ) {

        val cleanText =
            text.trim()


        if (
            cleanText.isBlank()
        ) {

            return
        }


        val finalId =
            if (
                messageId > 0L
            ) {

                messageId

            } else {

                generateMessageId()
            }


        val isOpen =
            activeChatId ==
                    chatId


        val currentTime =
            getCurrentTime()


        val message =
            Message(
                id = finalId,
                chatId = chatId,
                sender = sender,
                text = cleanText,
                time = currentTime,
                isMine = false,
                messageType =
                    MessageType.TEXT.name,
                attachmentUri = "",
                fileName = "",
                status =
                    if (isOpen) {
                        MessageStatus.READ.name
                    } else {
                        MessageStatus.DELIVERED.name
                    },
                createdAt =
                    System.currentTimeMillis()
            )


        saveMessage(
            context,
            message
        )


        updateChatAfterMessage(
            context = context,
            chatId = chatId,
            message = cleanText,
            time = currentTime
        )


        if (isOpen) {

            ServerRealtime
                .sendReadReceipt(
                    chatId =
                        chatId,
                    messageIds =
                        listOf(
                            finalId
                        )
                )

            setUnreadCount(
                context,
                chatId,
                0
            )

        } else {

            incrementUnread(
                context,
                chatId
            )
        }
    }


    // =========================================================
    // RECEIVE MEDIA
    // =========================================================

    suspend fun receiveMediaMessage(
        context: Context,
        messageId: Long,
        chatId: Long,
        sender: String,
        mediaType: MessageType,
        mediaUrl: String,
        fileName: String,
        caption: String = ""
    ) {

        if (
            mediaUrl.isBlank()
        ) {

            return
        }


        val finalId =
            if (
                messageId > 0L
            ) {

                messageId

            } else {

                generateMessageId()
            }


        val isOpen =
            activeChatId ==
                    chatId


        val currentTime =
            getCurrentTime()


        val message =
            Message(
                id = finalId,
                chatId = chatId,
                sender = sender,
                text = caption.trim(),
                time = currentTime,
                isMine = false,
                messageType =
                    mediaType.name,
                attachmentUri =
                    makeServerUrl(
                        mediaUrl
                    ),
                fileName =
                    fileName,
                status =
                    if (isOpen) {
                        MessageStatus.READ.name
                    } else {
                        MessageStatus.DELIVERED.name
                    },
                createdAt =
                    System.currentTimeMillis()
            )


        saveMessage(
            context,
            message
        )


        updateChatAfterMessage(
            context = context,
            chatId = chatId,
            message =
                buildPreview(
                    messageType =
                        mediaType,
                    fileName =
                        fileName,
                    caption =
                        caption
                ),
            time = currentTime
        )


        if (isOpen) {

            ServerRealtime
                .sendReadReceipt(
                    chatId =
                        chatId,
                    messageIds =
                        listOf(
                            finalId
                        )
                )

            setUnreadCount(
                context,
                chatId,
                0
            )

        } else {

            incrementUnread(
                context,
                chatId
            )
        }
    }


    // =========================================================
    // ATTACHMENT
    // =========================================================

    suspend fun sendAttachment(
        context: Context,
        chatId: Long,
        uri: String,
        fileName: String,
        messageType: MessageType,
        caption: String = ""
    ) {

        if (
            uri.isBlank()
        ) {

            return
        }


        val safeFileName =
            fileName.ifBlank {
                "attachment_${System.currentTimeMillis()}"
            }


        val tempFile =
            copyUriToCache(
                context =
                    context,
                uri =
                    Uri.parse(uri),
                fileName =
                    safeFileName
            )
                ?: return


        val messageId =
            generateMessageId()


        val currentTime =
            getCurrentTime()


        val message =
            Message(
                id = messageId,
                chatId = chatId,
                sender =
                    currentUserName(),
                text =
                    caption.trim(),
                time =
                    currentTime,
                isMine =
                    true,
                messageType =
                    messageType.name,
                attachmentUri =
                    uri,
                fileName =
                    safeFileName,
                status =
                    MessageStatus.SENDING.name,
                createdAt =
                    System.currentTimeMillis()
            )


        saveMessage(
            context,
            message
        )


        updateChatAfterMessage(
            context = context,
            chatId = chatId,
            message =
                buildPreview(
                    messageType =
                        messageType,
                    fileName =
                        safeFileName,
                    caption =
                        caption
                ),
            time =
                currentTime
        )


        ServerRealtime
            .uploadFile(
                file =
                    tempFile
            ) { uploadedUrl ->


                if (
                    uploadedUrl ==
                    null
                ) {

                    launchStatusUpdate(
                        context,
                        messageId,
                        MessageStatus.FAILED
                    )

                    return@uploadFile
                }


                val sent =
                    ServerRealtime
                        .sendMediaMessage(
                            messageId =
                                messageId,
                            chatId =
                                chatId,
                            mediaType =
                                messageType,
                            mediaUrl =
                                uploadedUrl,
                            fileName =
                                safeFileName,
                            caption =
                                caption
                        )


                if (!sent) {

                    launchStatusUpdate(
                        context,
                        messageId,
                        MessageStatus.FAILED
                    )
                }
            }
    }


    // =========================================================
    // VOICE
    // =========================================================

    suspend fun sendVoiceMessage(
        context: Context,
        chatId: Long,
        audioPath: String,
        durationMs: Long
    ) {

        val file =
            File(
                audioPath
            )


        if (
            !file.exists() ||
            file.length() <= 0L
        ) {

            return
        }


        val messageId =
            generateMessageId()


        val duration =
            formatMediaDuration(
                durationMs
            )


        val currentTime =
            getCurrentTime()


        val message =
            Message(
                id = messageId,
                chatId = chatId,
                sender =
                    currentUserName(),
                text =
                    duration,
                time =
                    currentTime,
                isMine =
                    true,
                messageType =
                    MessageType.AUDIO.name,
                attachmentUri =
                    audioPath,
                fileName =
                    file.name,
                status =
                    MessageStatus.SENDING.name,
                createdAt =
                    System.currentTimeMillis()
            )


        saveMessage(
            context,
            message
        )


        updateChatAfterMessage(
            context = context,
            chatId = chatId,
            message =
                "🎤 Голосовое сообщение",
            time =
                currentTime
        )


        ServerRealtime
            .uploadFile(
                file =
                    file
            ) { uploadedUrl ->


                if (
                    uploadedUrl ==
                    null
                ) {

                    launchStatusUpdate(
                        context,
                        messageId,
                        MessageStatus.FAILED
                    )

                    return@uploadFile
                }


                val sent =
                    ServerRealtime
                        .sendMediaMessage(
                            messageId =
                                messageId,
                            chatId =
                                chatId,
                            mediaType =
                                MessageType.AUDIO,
                            mediaUrl =
                                uploadedUrl,
                            fileName =
                                file.name,
                            caption =
                                duration
                        )


                if (!sent) {

                    launchStatusUpdate(
                        context,
                        messageId,
                        MessageStatus.FAILED
                    )
                }
            }
    }


    // =========================================================
    // VIDEO CIRCLE
    // =========================================================

    suspend fun sendVideoCircleMessage(
        context: Context,
        chatId: Long,
        videoPath: String,
        durationMs: Long
    ) {

        val file =
            File(
                videoPath
            )


        if (
            !file.exists() ||
            file.length() <= 0L
        ) {

            return
        }


        val messageId =
            generateMessageId()


        val duration =
            formatMediaDuration(
                durationMs
            )


        val currentTime =
            getCurrentTime()


        val message =
            Message(
                id = messageId,
                chatId = chatId,
                sender =
                    currentUserName(),
                text =
                    duration,
                time =
                    currentTime,
                isMine =
                    true,
                messageType =
                    MessageType.VIDEO_CIRCLE.name,
                attachmentUri =
                    videoPath,
                fileName =
                    file.name,
                status =
                    MessageStatus.SENDING.name,
                createdAt =
                    System.currentTimeMillis()
            )


        saveMessage(
            context,
            message
        )


        updateChatAfterMessage(
            context = context,
            chatId = chatId,
            message =
                "📹 Видеосообщение",
            time =
                currentTime
        )


        ServerRealtime
            .uploadFile(
                file =
                    file
            ) { uploadedUrl ->


                if (
                    uploadedUrl ==
                    null
                ) {

                    launchStatusUpdate(
                        context,
                        messageId,
                        MessageStatus.FAILED
                    )

                    return@uploadFile
                }


                val sent =
                    ServerRealtime
                        .sendMediaMessage(
                            messageId =
                                messageId,
                            chatId =
                                chatId,
                            mediaType =
                                MessageType.VIDEO_CIRCLE,
                            mediaUrl =
                                uploadedUrl,
                            fileName =
                                file.name,
                            caption =
                                duration
                        )


                if (!sent) {

                    launchStatusUpdate(
                        context,
                        messageId,
                        MessageStatus.FAILED
                    )
                }
            }
    }


    // =========================================================
    // STATUS
    // =========================================================

    suspend fun updateOutgoingStatus(
        context: Context,
        messageId: Long,
        status: MessageStatus
    ) {

        withContext(
            Dispatchers.IO
        ) {

            messageDao(context)
                .updateMessageStatus(
                    messageId =
                        messageId,
                    status =
                        status.name
                )
        }
    }


    private fun launchStatusUpdate(
        context: Context,
        messageId: Long,
        status: MessageStatus
    ) {

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            updateOutgoingStatus(
                context =
                    context,
                messageId =
                    messageId,
                status =
                    status
            )
        }
    }


    // =========================================================
    // UNREAD
    // =========================================================

    private suspend fun incrementUnread(
        context: Context,
        chatId: Long
    ) {

        val count =
            withContext(
                Dispatchers.IO
            ) {

                messageDao(context)
                    .countUnreadIncoming(
                        chatId
                    )
            }


        setUnreadCount(
            context =
                context,
            chatId =
                chatId,
            unreadCount =
                count
        )
    }


    private suspend fun setUnreadCount(
        context: Context,
        chatId: Long,
        unreadCount: Int
    ) {

        val safeCount =
            unreadCount
                .coerceAtLeast(
                    0
                )


        withContext(
            Dispatchers.IO
        ) {

            chatDao(context)
                .updateUnreadCount(
                    chatId =
                        chatId,
                    unreadCount =
                        safeCount
                )
        }


        val index =
            chats.indexOfFirst {
                it.id ==
                        chatId
            }


        if (
            index >= 0
        ) {

            chats[index] =
                chats[index]
                    .copy(
                        unreadCount =
                            safeCount
                    )
        }
    }


    // =========================================================
    // CREATE CHATS
    // =========================================================

    suspend fun saveNewChat(
        context: Context,
        title: String,
        type: ChatType,
        members: List<String>
    ): Chat {

        val cleanTitle =
            title.trim()
                .ifBlank {
                    "Новый чат"
                }


        val currentUser =
            OchagAppState
                .currentUser
                ?.name


        val finalMembers =
            buildList {

                if (
                    !currentUser.isNullOrBlank()
                ) {

                    add(
                        currentUser
                    )
                }


                members.forEach {
                        member ->

                    if (
                        member.isNotBlank() &&
                        !contains(
                            member
                        )
                    ) {

                        add(
                            member
                        )
                    }
                }
            }


        val newId =
            withContext(
                Dispatchers.IO
            ) {

                val databaseId =
                    chatDao(context)
                        .getMaxId()
                        ?: 0L


                val memoryId =
                    chats.maxOfOrNull {
                        it.id
                    }
                        ?: 0L


                maxOf(
                    databaseId,
                    memoryId
                ) + 1L
            }


        val chat =
            Chat(
                id =
                    newId,
                title =
                    cleanTitle,
                type =
                    type,
                lastMessage =
                    "",
                lastTime =
                    "",
                unreadCount =
                    0,
                members =
                    finalMembers,
                lastActivityAt =
                    System.currentTimeMillis()
            )


        withContext(
            Dispatchers.IO
        ) {

            chatDao(context)
                .insertChat(
                    chat.toEntity()
                )
        }


        chats.add(
            0,
            chat
        )


        ServerRealtime.sendChatUpsert(
            chat
        )


        return chat
    }


    suspend fun saveNewChat(
        context: Context,
        chat: Chat
    ): Chat {

        var chatToSave =
            chat


        if (
            chatToSave.id <= 0L ||
            findChat(
                chatToSave.id
            ) != null
        ) {

            val maxId =
                withContext(
                    Dispatchers.IO
                ) {

                    maxOf(
                        chatDao(context)
                            .getMaxId()
                            ?: 0L,

                        chats.maxOfOrNull {
                            it.id
                        }
                            ?: 0L
                    )
                }


            chatToSave =
                chatToSave.copy(
                    id =
                        maxId + 1L
                )
        }


        withContext(
            Dispatchers.IO
        ) {

            chatDao(context)
                .insertChat(
                    chatToSave.toEntity()
                )
        }


        chats.removeAll {
            it.id ==
                    chatToSave.id
        }


        chats.add(
            chatToSave
        )


        ServerRealtime.sendChatUpsert(
            chatToSave
        )


        return chatToSave
    }


    suspend fun createChat(
        context: Context,
        title: String,
        type: ChatType,
        members: List<String>
    ): Chat {

        return saveNewChat(
            context,
            title,
            type,
            members
        )
    }


    suspend fun createPrivateChat(
        context: Context,
        memberName: String
    ): Chat {

        val currentUser =
            currentUserName()


        val existing =
            chats.firstOrNull {
                    chat ->

                chat.type ==
                        ChatType.PRIVATE &&

                        chat.members.contains(
                            currentUser
                        ) &&

                        chat.members.contains(
                            memberName
                        )
            }


        if (
            existing != null
        ) {

            return existing
        }


        return saveNewChat(
            context =
                context,
            title =
                memberName,
            type =
                ChatType.PRIVATE,
            members =
                listOf(
                    currentUser,
                    memberName
                )
        )
    }


    suspend fun createCustomChat(
        context: Context,
        title: String,
        type: ChatType,
        selectedMembers: List<String>
    ): Chat {

        return saveNewChat(
            context,
            title,
            type,
            selectedMembers
        )
    }



    // =========================================================
    // UPDATE GROUP / THEME / FAMILY METADATA
    // =========================================================

    suspend fun updateChatDetails(
        context: Context,
        chatId: Long,
        title: String,
        members: List<String>
    ): Chat? {

        val current =
            findChat(
                chatId
            )
                ?: return null

        val cleanTitle =
            title
                .trim()
                .ifBlank {
                    current.title
                }

        val cleanMembers =
            members
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinctBy {
                    it.lowercase()
                }
                .toMutableList()

        val currentUser =
            currentUserName()

        if (
            currentUser.isNotBlank() &&
            cleanMembers.none {
                it.equals(
                    currentUser,
                    ignoreCase = true
                )
            }
        ) {
            cleanMembers.add(
                0,
                currentUser
            )
        }

        val updated =
            current.copy(
                title =
                    cleanTitle,
                members =
                    cleanMembers,
                lastActivityAt =
                    System.currentTimeMillis()
            )

        withContext(
            Dispatchers.IO
        ) {
            chatDao(context)
                .insertChat(
                    updated.toEntity()
                )
        }

        val index =
            chats.indexOfFirst {
                it.id ==
                        chatId
            }

        if (
            index >= 0
        ) {
            chats[index] =
                updated
        } else {
            chats.add(
                0,
                updated
            )
        }

        ServerRealtime
            .sendChatUpsert(
                updated
            )

        return updated
    }


    // =========================================================
    // SERVER CHAT METADATA SYNC
    // =========================================================

    suspend fun syncServerChatMetadata(
        context: Context,
        chatId: Long,
        title: String,
        type: ChatType,
        members: List<String>,
        updatedAt: Long = System.currentTimeMillis()
    ) {

        if (chatId <= 0L) {
            return
        }

        val current =
            findChat(
                chatId
            )

        val cleanMembers =
            members
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinctBy {
                    it.lowercase()
                }

        val currentUser =
            currentUserName()

        val serverHasMembers =
            cleanMembers.isNotEmpty()

        val currentUserStillMember =
            !serverHasMembers ||
                    cleanMembers.any {
                        it.equals(
                            currentUser,
                            ignoreCase = true
                        )
                    }

        if (
            current != null &&
            serverHasMembers &&
            !currentUserStillMember
        ) {

            withContext(
                Dispatchers.IO
            ) {
                chatDao(context)
                    .deleteChat(
                        chatId
                    )

                messageDao(context)
                    .deleteChatMessages(
                        chatId
                    )
            }

            chats.removeAll {
                it.id ==
                        chatId
            }

            return
        }

        val finalMembers =
            if (
                serverHasMembers
            ) {
                cleanMembers
            } else {
                buildList {

                    current
                        ?.members
                        ?.forEach {
                            add(
                                it
                            )
                        }

                    if (
                        currentUser.isNotBlank() &&
                        none {
                            it.equals(
                                currentUser,
                                ignoreCase = true
                            )
                        }
                    ) {
                        add(
                            currentUser
                        )
                    }
                }
            }

        val privateCompanion =
            if (type == ChatType.PRIVATE && finalMembers.size > 1) {
                finalMembers.firstOrNull {
                    !it.equals(currentUser, ignoreCase = true)
                }
            } else {
                null
            }

        val safeTitle =
            privateCompanion
                ?: title
                    .trim()
                    .ifBlank {
                        current
                            ?.title
                            ?: when (type) {
                                ChatType.FAMILY -> "Семья"
                                ChatType.PRIVATE -> "Личный чат"
                                ChatType.GROUP -> "Группа"
                                ChatType.THEME -> "Тематический чат"
                            }
                    }

        val merged =
            if (
                current != null
            ) {
                current.copy(
                    title =
                        safeTitle,
                    type =
                        type,
                    members =
                        finalMembers.ifEmpty {
                            current.members
                        },
                    lastActivityAt =
                        maxOf(
                            current.lastActivityAt,
                            updatedAt
                        )
                )

            } else {

                Chat(
                    id =
                        chatId,
                    title =
                        safeTitle,
                    type =
                        type,
                    lastMessage =
                        "",
                    lastTime =
                        "",
                    unreadCount =
                        0,
                    members =
                        finalMembers,
                    lastActivityAt =
                        updatedAt
                )
            }

        withContext(
            Dispatchers.IO
        ) {
            chatDao(context)
                .insertChat(
                    merged.toEntity()
                )
        }

        chats.removeAll {
            it.id ==
                    chatId
        }

        chats.add(
            merged
        )

        val sorted =
            chats.sortedWith(
                compareByDescending<Chat> {
                    it.lastActivityAt
                }
                    .thenBy {
                        it.id
                    }
            )

        chats.clear()
        chats.addAll(
            sorted
        )
    }


    // =========================================================
    // SERVER HISTORY / OFFLINE SYNC
    // =========================================================

    suspend fun syncServerMessage(
        context: Context,
        messageId: Long,
        chatId: Long,
        sender: String,
        messageType: MessageType,
        text: String = "",
        mediaUrl: String = "",
        fileName: String = "",
        caption: String = "",
        createdAt: Long = System.currentTimeMillis(),
        serverStatus: MessageStatus = MessageStatus.DELIVERED,
        members: List<String> = emptyList(),
        chatTitle: String = "",
        chatType: ChatType = ChatType.PRIVATE
    ) {

        if (
            messageId <= 0L ||
            chatId <= 0L ||
            sender.isBlank()
        ) {
            return
        }

        ensureServerChat(
            context = context,
            chatId = chatId,
            sender = sender,
            members = members,
            chatTitle = chatTitle,
            chatType = chatType,
            createdAt = createdAt
        )

        val alreadyExists =
            withContext(
                Dispatchers.IO
            ) {
                messageDao(context)
                    .messageExists(
                        messageId
                    ) > 0
            }

        val isMine =
            sender.trim()
                .equals(
                    currentUserName().trim(),
                    ignoreCase = true
                )

        if (alreadyExists) {

            if (isMine) {
                updateOutgoingStatus(
                    context = context,
                    messageId = messageId,
                    status = serverStatus
                )
            }

            return
        }

        val safeCreatedAt =
            createdAt.takeIf {
                it > 0L
            }
                ?: System.currentTimeMillis()

        val displayTime =
            formatTime(
                safeCreatedAt
            )

        val isOpen =
            activeChatId ==
                    chatId

        val incomingStatus =
            if (isMine) {
                serverStatus
            } else if (isOpen) {
                MessageStatus.READ
            } else {
                MessageStatus.DELIVERED
            }

        val cleanCaption =
            caption.trim()

        val message =
            Message(
                id = messageId,
                chatId = chatId,
                sender = sender,
                text =
                    if (
                        messageType == MessageType.TEXT
                    ) {
                        text.trim()
                    } else {
                        cleanCaption
                    },
                time = displayTime,
                isMine = isMine,
                messageType = messageType.name,
                attachmentUri =
                    if (
                        mediaUrl.isBlank()
                    ) {
                        ""
                    } else {
                        makeServerUrl(
                            mediaUrl
                        )
                    },
                fileName = fileName,
                status = incomingStatus.name,
                createdAt = safeCreatedAt
            )

        saveMessage(
            context,
            message
        )

        val preview =
            if (
                messageType == MessageType.TEXT
            ) {
                text.trim()
            } else {
                buildPreview(
                    messageType = messageType,
                    fileName = fileName,
                    caption = cleanCaption
                )
            }

        updateChatAfterMessage(
            context = context,
            chatId = chatId,
            message = preview,
            time = displayTime,
            activityAt = safeCreatedAt
        )

        if (!isMine) {

            if (isOpen) {

                ServerRealtime
                    .sendReadReceipt(
                        chatId = chatId,
                        messageIds = listOf(
                            messageId
                        )
                    )

                setUnreadCount(
                    context = context,
                    chatId = chatId,
                    unreadCount = 0
                )

            } else {

                incrementUnread(
                    context = context,
                    chatId = chatId
                )
            }
        }
    }


    private suspend fun ensureServerChat(
        context: Context,
        chatId: Long,
        sender: String,
        members: List<String>,
        chatTitle: String,
        chatType: ChatType,
        createdAt: Long
    ) {

        if (
            findChat(
                chatId
            ) != null
        ) {
            return
        }

        val currentUser =
            currentUserName()

        val finalMembers =
            buildList {

                if (
                    currentUser.isNotBlank()
                ) {
                    add(
                        currentUser
                    )
                }

                members.forEach {
                        member ->

                    val clean =
                        member.trim()

                    if (
                        clean.isNotBlank() &&
                        none {
                            it.equals(
                                clean,
                                ignoreCase = true
                            )
                        }
                    ) {
                        add(
                            clean
                        )
                    }
                }

                if (
                    sender.isNotBlank() &&
                    none {
                        it.equals(
                            sender,
                            ignoreCase = true
                        )
                    }
                ) {
                    add(
                        sender
                    )
                }
            }

        val fallbackTitle =
            when (
                chatType
            ) {

                ChatType.PRIVATE ->
                    finalMembers
                        .firstOrNull {
                            !it.equals(
                                currentUser,
                                ignoreCase = true
                            )
                        }
                        ?: sender

                ChatType.FAMILY ->
                    "Семья"

                ChatType.GROUP ->
                    "Группа"

                ChatType.THEME ->
                    "Тематический чат"
            }

        val newChat =
            Chat(
                id = chatId,
                title =
                    chatTitle
                        .trim()
                        .ifBlank {
                            fallbackTitle
                        },
                type = chatType,
                lastMessage = "",
                lastTime = "",
                unreadCount = 0,
                members = finalMembers,
                lastActivityAt =
                    createdAt.takeIf {
                        it > 0L
                    }
                        ?: System.currentTimeMillis()
            )

        withContext(
            Dispatchers.IO
        ) {
            chatDao(context)
                .insertChat(
                    newChat.toEntity()
                )
        }

        chats.removeAll {
            it.id ==
                    chatId
        }

        chats.add(
            0,
            newChat
        )
    }


    private fun formatTime(
        millis: Long
    ): String {

        return SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(
            Date(
                millis
            )
        )
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private suspend fun saveMessage(
        context: Context,
        message: Message
    ) {

        withContext(
            Dispatchers.IO
        ) {

            messageDao(context)
                .insertMessage(
                    message
                )
        }
    }


    private suspend fun copyUriToCache(
        context: Context,
        uri: Uri,
        fileName: String
    ): File? {

        return withContext(
            Dispatchers.IO
        ) {

            try {

                val safeName =
                    fileName
                        .replace(
                            "/",
                            "_"
                        )
                        .replace(
                            "\\",
                            "_"
                        )


                val file =
                    File(
                        context.cacheDir,
                        safeName
                    )


                context
                    .contentResolver
                    .openInputStream(
                        uri
                    )
                    ?.use {
                            input ->

                        file
                            .outputStream()
                            .use {
                                    output ->

                                input.copyTo(
                                    output
                                )
                            }
                    }
                    ?: return@withContext null


                if (
                    file.exists() &&
                    file.length() > 0L
                ) {

                    file

                } else {

                    null
                }


            } catch (
                error: Exception
            ) {

                error.printStackTrace()

                null
            }
        }
    }


    private fun makeServerUrl(
        value: String
    ): String {

        return if (
            value.startsWith(
                "http://"
            ) ||
            value.startsWith(
                "https://"
            )
        ) {

            value

        } else {

            "http://192.168.1.21:8000$value"
        }
    }


    private fun buildPreview(
        messageType: MessageType,
        fileName: String = "",
        caption: String = ""
    ): String {

        return when (
            messageType
        ) {

            MessageType.IMAGE ->

                if (
                    caption.isNotBlank()
                ) {

                    "📷 ${caption.trim()}"

                } else {

                    "📷 Фото"
                }


            MessageType.FILE ->

                if (
                    fileName.isNotBlank()
                ) {

                    "📎 $fileName"

                } else {

                    "📎 Файл"
                }


            MessageType.AUDIO ->

                "🎤 Голосовое сообщение"


            MessageType.VIDEO_CIRCLE ->

                "📹 Видеосообщение"


            MessageType.TEXT ->

                caption
        }
    }


    private suspend fun updateChatAfterMessage(
        context: Context,
        chatId: Long,
        message: String,
        time: String,
        activityAt: Long =
            System.currentTimeMillis()
    ) {

        withContext(
            Dispatchers.IO
        ) {

            chatDao(context)
                .updateLastMessage(
                    chatId =
                        chatId,
                    lastMessage =
                        message,
                    lastTime =
                        time,
                    lastActivityAt =
                        activityAt
                )
        }


        val index =
            chats.indexOfFirst {
                it.id ==
                        chatId
            }


        if (
            index >= 0
        ) {

            val updatedChat =
                chats[index]
                    .copy(
                        lastMessage =
                            message,
                        lastTime =
                            time,
                        lastActivityAt =
                            activityAt
                    )


            // Не ждём перезапуска экрана: физически переносим
            // обновившийся чат на первое место списка.
            chats.removeAt(
                index
            )

            chats.add(
                0,
                updatedChat
            )
        }
    }


    private fun Chat.toEntity():
            ChatEntity {

        return ChatEntity(
            id =
                id,
            title =
                title,
            type =
                type.name,
            lastMessage =
                lastMessage,
            lastTime =
                lastTime,
            unreadCount =
                unreadCount,
            members =
                members.joinToString(
                    "|"
                ),
            lastActivityAt =
                lastActivityAt
        )
    }


    private fun ChatEntity.toChat():
            Chat {

        return Chat(
            id =
                id,
            title =
                title,

            type =
                try {

                    ChatType.valueOf(
                        type
                    )

                } catch (_: Exception) {

                    ChatType.GROUP
                },

            lastMessage =
                lastMessage,
            lastTime =
                lastTime,
            unreadCount =
                unreadCount,
            members =
                parseMembers(
                    members
                ),
            lastActivityAt =
                lastActivityAt
        )
    }


    private fun parseMembers(
        value: String
    ): List<String> {

        if (
            value.isBlank()
        ) {

            return emptyList()
        }


        return value
            .split(
                "|",
                ",",
                ";",
                "\n"
            )
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }


    private fun currentUserName():
            String {

        return OchagAppState
            .currentUser
            ?.name
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Михаил"
    }


    private fun getCurrentTime():
            String {

        return SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(
            Date()
        )
    }


    private fun formatMediaDuration(
        durationMs: Long
    ): String {

        val totalSeconds =
            (
                    durationMs /
                            1000L
                    )
                .coerceAtLeast(
                    0L
                )


        val minutes =
            totalSeconds /
                    60L


        val seconds =
            totalSeconds %
                    60L


        return String.format(
            Locale.getDefault(),
            "%d:%02d",
            minutes,
            seconds
        )
    }


    private fun generateMessageId():
            Long {

        return (
                System.currentTimeMillis() *
                        1000L
                ) +
                (
                        System.nanoTime() %
                                1000L
                        )
    }
}
