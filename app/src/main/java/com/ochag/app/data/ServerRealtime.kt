package com.ochag.app.data

import android.content.Context
import android.net.Uri

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

import org.json.JSONArray
import org.json.JSONObject

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


data class ServerUserProfile(
    val userId: Long,
    val name: String,
    val phone: String = "",
    val normalizedPhone: String = ""
)

data class CallSignal(
    val type: String,
    val callId: String,
    val senderName: String,
    val chatId: Long,
    val video: Boolean,
    val sdp: String = "",
    val sdpType: String = "",
    val candidate: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0
)

object ServerRealtime {


    enum class ConnectionState {
        OFFLINE,
        CONNECTING,
        ONLINE
    }




    private const val SERVER_IP =
        "192.168.1.21"


    private const val SERVER_PORT =
        8000
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )


    private val client =
        OkHttpClient
            .Builder()
            .pingInterval(
                20,
                TimeUnit.SECONDS
            )
            .connectTimeout(
                15,
                TimeUnit.SECONDS
            )
            .readTimeout(
                0,
                TimeUnit.MILLISECONDS
            )
            .build()


    private var socket:
            WebSocket? = null


    private var appContext:
            Context? = null


    private var currentUserName =
        ""


    val userName:
            String
        get() =
            currentUserName


    private var manualDisconnect =
        false


    private var reconnectAttempts =
        0


    private var reconnectScheduled =
        false


    private var reconnectJob:
            Job? = null


    private var onlineRefreshJob:
            Job? = null


    private var healthFailureCount =
        0


    private val connectionLock =
        Any()


    private val _connectionState =
        MutableStateFlow(
            ConnectionState.OFFLINE
        )


    val connectionState:
            StateFlow<ConnectionState> =
        _connectionState
            .asStateFlow()


    @Volatile
    var isConnected =
        false

        private set


    // =====================================================
    // ONLINE USERS
    // =====================================================

    private val _onlineUsers =
        MutableStateFlow<Set<String>>(
            emptySet()
        )


    val onlineUsers:
            StateFlow<Set<String>> =
        _onlineUsers
            .asStateFlow()


    private val _serverUsers =
        MutableStateFlow<List<String>>(
            emptyList()
        )


    val serverUsers:
            StateFlow<List<String>> =
        _serverUsers
            .asStateFlow()


    private val _serverUserProfiles =
        MutableStateFlow<List<ServerUserProfile>>(
            emptyList()
        )


    val serverUserProfiles:
            StateFlow<List<ServerUserProfile>> =
        _serverUserProfiles
            .asStateFlow()


    private val _typingUsers =
        MutableStateFlow<Map<Long, Set<String>>>(
            emptyMap()
        )


    val typingUsers:
            StateFlow<Map<Long, Set<String>>> =
        _typingUsers
            .asStateFlow()

    val callSignals = MutableSharedFlow<CallSignal>(
        extraBufferCapacity = 64
    )


    // =====================================================
    // CONNECT
    // =====================================================

    fun connect(
        context: Context,
        userName: String
    ) {

        val cleanName =
            userName.trim()


        if (
            cleanName.isBlank()
        ) {

            return
        }


        appContext =
            context.applicationContext


        currentUserName =
            cleanName


        manualDisconnect =
            false


        synchronized(
            connectionLock
        ) {

            if (
                socket != null &&
                (
                        isConnected ||
                                _connectionState.value ==
                                ConnectionState.CONNECTING
                        )
            ) {

                if (
                    isConnected
                ) {

                    refreshOnlineUsersFromHealth()
                }

                return
            }


            _connectionState.value =
                ConnectionState.CONNECTING
        }


        try {

            socket?.cancel()

        } catch (_: Exception) {
        }


        socket =
            null


        openSocket(
            cleanName
        )
    }


    private fun openSocket(
        cleanName: String
    ) {

        if (
            manualDisconnect ||
            cleanName.isBlank()
        ) {

            return
        }


        _connectionState.value =
            ConnectionState.CONNECTING


        val request =
            Request
                .Builder()
                .url(
                    "ws://$SERVER_IP:$SERVER_PORT/ws/${
                        Uri.encode(cleanName)
                    }"
                )
                .build()


        socket =
            client.newWebSocket(
                request,
                listener
            )
    }


    fun ensureConnected(
        context: Context
    ) {

        val user =
            currentUserName.trim()


        if (
            user.isBlank()
        ) {

            return
        }


        appContext =
            context.applicationContext


        if (
            isConnected
        ) {

            refreshOnlineUsersFromHealth()

            return
        }


        scheduleReconnect(
            immediate = true
        )
    }


    // =====================================================
    // DISCONNECT
    // =====================================================

    fun disconnect() {

        manualDisconnect =
            true


        reconnectAttempts =
            0


        reconnectScheduled =
            false


        reconnectJob
            ?.cancel()


        reconnectJob =
            null


        onlineRefreshJob
            ?.cancel()


        onlineRefreshJob =
            null


        try {

            socket?.close(
                1000,
                "App disconnected"
            )

        } catch (_: Exception) {
        }


        try {

            socket?.cancel()

        } catch (_: Exception) {
        }


        socket =
            null


        isConnected =
            false


        healthFailureCount =
            0


        _connectionState.value =
            ConnectionState.OFFLINE


        _onlineUsers.value =
            emptySet()

        _typingUsers.value =
            emptyMap()
    }


    // =====================================================
    // TYPING
    // =====================================================

    fun sendTyping(
        chatId: Long,
        isTyping: Boolean
    ): Boolean {

        val currentSocket =
            socket
                ?: return false

        if (
            !isConnected ||
            chatId <= 0L
        ) {
            return false
        }

        val chat =
            ChatRepository.findChat(
                chatId
            )

        val json =
            JSONObject()

        json.put(
            "type",
            "typing"
        )

        json.put(
            "chat_id",
            chatId
        )

        json.put(
            "sender_name",
            currentUserName
        )

        json.put(
            "is_typing",
            isTyping
        )

        json.put(
            "members",
            JSONArray(
                chat
                    ?.members
                    ?: emptyList<String>()
            )
        )

        return currentSocket.send(
            json.toString()
        )
    }


    fun sendCallSignal(
        type: String,
        callId: String,
        chatId: Long,
        targets: List<String>,
        video: Boolean,
        sdp: String = "",
        sdpType: String = "",
        candidate: String = "",
        sdpMid: String = "",
        sdpMLineIndex: Int = 0
    ): Boolean {
        val currentSocket = socket ?: return false
        if (!isConnected || callId.isBlank() || targets.isEmpty()) return false

        val json = JSONObject()
            .put("type", type)
            .put("call_id", callId)
            .put("chat_id", chatId)
            .put("targets", JSONArray(targets))
            .put("video", video)

        if (sdp.isNotBlank()) json.put("sdp", sdp)
        if (sdpType.isNotBlank()) json.put("sdp_type", sdpType)
        if (candidate.isNotBlank()) {
            json.put("candidate", candidate)
            json.put("sdp_mid", sdpMid)
            json.put("sdp_mline_index", sdpMLineIndex)
        }

        return currentSocket.send(json.toString())
    }


    // =====================================================
    // TEXT
    // =====================================================

    fun sendTextMessage(
        messageId: Long,
        chatId: Long,
        text: String
    ): Boolean {

        val currentSocket =
            socket
                ?: return false


        if (
            !isConnected ||
            text.isBlank()
        ) {

            return false
        }


        val chat =
            ChatRepository.findChat(
                chatId
            )


        val json =
            JSONObject()


        json.put(
            "type",
            "chat_message"
        )


        json.put(
            "message_id",
            messageId
        )


        json.put(
            "chat_id",
            chatId
        )


        json.put(
            "sender_name",
            currentUserName
        )


        json.put(
            "text",
            text.trim()
        )


        json.put(
            "members",
            JSONArray(
                chat?.members
                    ?: emptyList<String>()
            )
        )


        json.put(
            "chat_title",
            chat?.title
                ?: ""
        )


        json.put(
            "chat_type",
            chat?.type
                ?.name
                ?: ChatType.PRIVATE.name
        )


        return currentSocket.send(
            json.toString()
        )
    }


    // =====================================================
    // UPLOAD FILE
    // =====================================================

    fun uploadFile(
        file: File,
        onResult: (String?) -> Unit
    ) {

        if (
            !file.exists() ||
            file.length() <= 0L
        ) {

            onResult(
                null
            )

            return
        }


        val requestBody =
            file.asRequestBody(
                guessMimeType(
                    file.name
                )
                    .toMediaTypeOrNull()
            )


        val multipart =
            MultipartBody
                .Builder()
                .setType(
                    MultipartBody.FORM
                )
                .addFormDataPart(
                    "file",
                    file.name,
                    requestBody
                )
                .build()


        val request =
            Request
                .Builder()
                .url(
                    "http://$SERVER_IP:$SERVER_PORT/upload"
                )
                .post(
                    multipart
                )
                .build()


        client
            .newCall(
                request
            )
            .enqueue(

                object : Callback {


                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        e.printStackTrace()

                        onResult(
                            null
                        )
                    }


                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        response.use {

                            if (
                                !response.isSuccessful
                            ) {

                                onResult(
                                    null
                                )

                                return
                            }


                            val body =
                                response
                                    .body
                                    ?.string()


                            if (
                                body.isNullOrBlank()
                            ) {

                                onResult(
                                    null
                                )

                                return
                            }


                            try {

                                val json =
                                    JSONObject(
                                        body
                                    )


                                val url =
                                    json.optString(
                                        "url"
                                    )


                                onResult(
                                    url.takeIf {
                                        it.isNotBlank()
                                    }
                                )


                            } catch (
                                error: Exception
                            ) {

                                error.printStackTrace()

                                onResult(
                                    null
                                )
                            }
                        }
                    }
                }
            )
    }


    // =====================================================
    // MEDIA
    // =====================================================

    fun sendMediaMessage(
        messageId: Long,
        chatId: Long,
        mediaType: MessageType,
        mediaUrl: String,
        fileName: String,
        caption: String = ""
    ): Boolean {

        val currentSocket =
            socket
                ?: return false


        if (
            !isConnected ||
            mediaUrl.isBlank()
        ) {

            return false
        }


        val chat =
            ChatRepository.findChat(
                chatId
            )


        val json =
            JSONObject()


        json.put(
            "type",
            "media_message"
        )


        json.put(
            "message_id",
            messageId
        )


        json.put(
            "chat_id",
            chatId
        )


        json.put(
            "sender_name",
            currentUserName
        )


        json.put(
            "media_type",
            mediaType.name
        )


        json.put(
            "media_url",
            mediaUrl
        )


        json.put(
            "file_name",
            fileName
        )


        json.put(
            "caption",
            caption
        )


        json.put(
            "members",
            JSONArray(
                chat?.members
                    ?: emptyList<String>()
            )
        )


        json.put(
            "chat_title",
            chat?.title
                ?: ""
        )


        json.put(
            "chat_type",
            chat?.type
                ?.name
                ?: ChatType.PRIVATE.name
        )


        return currentSocket.send(
            json.toString()
        )
    }


    // =====================================================
    // SERVER USERS / CHAT METADATA
    // =====================================================

    fun requestServerUsers(): Boolean {

        val currentSocket =
            socket
                ?: return false

        if (!isConnected) {
            return false
        }

        val json =
            JSONObject()

        json.put(
            "type",
            "users_request"
        )

        return currentSocket.send(
            json.toString()
        )
    }


    fun sendChatUpsert(
        chat: Chat
    ): Boolean {

        val currentSocket =
            socket
                ?: return false

        if (!isConnected) {
            return false
        }

        val json =
            JSONObject()

        json.put(
            "type",
            "chat_upsert"
        )

        json.put(
            "chat_id",
            chat.id
        )

        json.put(
            "chat_title",
            chat.title
        )

        json.put(
            "chat_type",
            chat.type.name
        )

        json.put(
            "members",
            JSONArray(chat.members)
        )

        return currentSocket.send(
            json.toString()
        )
    }


    // =====================================================
    // READ RECEIPT
    // =====================================================

    fun sendReadReceipt(
        chatId: Long,
        messageIds: List<Long>
    ): Boolean {

        val currentSocket =
            socket
                ?: return false


        if (
            !isConnected ||
            messageIds.isEmpty()
        ) {

            return false
        }


        val cleanIds =
            messageIds
                .filter {
                    it > 0L
                }
                .distinct()


        if (
            cleanIds.isEmpty()
        ) {

            return false
        }


        val chat =
            ChatRepository.findChat(
                chatId
            )


        val idsArray =
            JSONArray()


        cleanIds.forEach { id ->

            idsArray.put(
                id
            )
        }


        val json =
            JSONObject()


        json.put(
            "type",
            "read_receipt"
        )


        json.put(
            "chat_id",
            chatId
        )


        json.put(
            "reader_name",
            currentUserName
        )


        json.put(
            "message_ids",
            idsArray
        )


        json.put(
            "members",
            JSONArray(
                chat?.members
                    ?: emptyList<String>()
            )
        )


        return currentSocket.send(
            json.toString()
        )
    }


    // =====================================================
    // ПОЛУЧИТЬ ONLINE USERS ЧЕРЕЗ /health
    // =====================================================

    private fun refreshOnlineUsersFromHealth() {

        val request =
            Request
                .Builder()
                .url(
                    "http://$SERVER_IP:$SERVER_PORT/health"
                )
                .get()
                .build()


        client
            .newCall(
                request
            )
            .enqueue(

                object : Callback {


                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        healthFailureCount++


                        if (
                            isConnected &&
                            healthFailureCount >= 3
                        ) {

                            forceReconnect()
                        }
                    }


                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        response.use {

                            if (
                                !response.isSuccessful
                            ) {

                                healthFailureCount++


                                if (
                                    isConnected &&
                                    healthFailureCount >= 3
                                ) {

                                    forceReconnect()
                                }

                                return
                            }


                            healthFailureCount =
                                0


                            val body =
                                response
                                    .body
                                    ?.string()
                                    ?: return


                            try {

                                val json =
                                    JSONObject(
                                        body
                                    )


                                val array =
                                    json.optJSONArray(
                                        "connected_users"
                                    )
                                        ?: return


                                val result =
                                    mutableSetOf<String>()


                                for (
                                index in
                                0 until
                                        array.length()
                                ) {

                                    val name =
                                        array.optString(
                                            index
                                        )
                                            .trim()


                                    if (
                                        name.isNotBlank()
                                    ) {

                                        result.add(
                                            name
                                        )
                                    }
                                }


                                _onlineUsers.value =
                                    result


                            } catch (
                                error: Exception
                            ) {

                                error.printStackTrace()
                            }
                        }
                    }
                }
            )
    }


    // =====================================================
    // PERIODIC ONLINE REFRESH
    // =====================================================

    private fun startOnlineRefreshLoop() {

        onlineRefreshJob
            ?.cancel()


        onlineRefreshJob =
            scope.launch {

                while (
                    isConnected &&
                    !manualDisconnect
                ) {

                    refreshOnlineUsersFromHealth()


                    delay(
                        5000
                    )
                }
            }
    }


    // =====================================================
    // PRESENCE FROM WEBSOCKET
    // =====================================================

    private fun applyPresence(
        json: JSONObject
    ) {

        val array =
            json.optJSONArray(
                "online_users"
            )
                ?: return


        val result =
            mutableSetOf<String>()


        for (
        index in
        0 until
                array.length()
        ) {

            val name =
                array
                    .optString(
                        index
                    )
                    .trim()


            if (
                name.isNotBlank()
            ) {

                result.add(
                    name
                )
            }
        }


        _onlineUsers.value =
            result
    }


    // =====================================================
    // RECONNECT
    // =====================================================

    private fun scheduleReconnect(
        immediate: Boolean = false
    ) {

        if (
            manualDisconnect
        ) {

            return
        }


        val user =
            currentUserName.trim()


        if (
            appContext == null ||
            user.isBlank()
        ) {

            return
        }


        synchronized(
            connectionLock
        ) {

            if (
                reconnectJob
                    ?.isActive ==
                true
            ) {

                return
            }


            reconnectScheduled =
                true


            reconnectAttempts++


            _connectionState.value =
                ConnectionState.CONNECTING


            val attempt =
                reconnectAttempts


            val reconnectDelay =
                if (
                    immediate
                ) {

                    250L

                } else {

                    when {

                        attempt <= 1 ->
                            1000L

                        attempt == 2 ->
                            2000L

                        attempt == 3 ->
                            4000L

                        attempt == 4 ->
                            8000L

                        else ->
                            15000L
                    }
                }


            reconnectJob =
                scope.launch {

                    delay(
                        reconnectDelay
                    )


                    reconnectScheduled =
                        false


                    if (
                        manualDisconnect ||
                        isConnected
                    ) {

                        return@launch
                    }


                    try {

                        socket?.cancel()

                    } catch (_: Exception) {
                    }


                    socket =
                        null


                    openSocket(
                        user
                    )
                }
        }
    }


    private fun forceReconnect() {

        if (
            manualDisconnect
        ) {

            return
        }


        isConnected =
            false


        _connectionState.value =
            ConnectionState.OFFLINE


        _onlineUsers.value =
            emptySet()


        onlineRefreshJob
            ?.cancel()


        onlineRefreshJob =
            null


        try {

            socket?.cancel()

        } catch (_: Exception) {
        }


        socket =
            null


        scheduleReconnect(
            immediate = true
        )
    }


    // =====================================================
    // WEBSOCKET LISTENER
    // =====================================================

    private val listener =
        object : WebSocketListener() {


            override fun onOpen(
                webSocket: WebSocket,
                response: Response
            ) {

                socket =
                    webSocket


                isConnected =
                    true


                _connectionState.value =
                    ConnectionState.ONLINE


                reconnectAttempts =
                    0


                reconnectScheduled =
                    false


                reconnectJob
                    ?.cancel()


                reconnectJob =
                    null


                healthFailureCount =
                    0


                // Сразу спрашиваем сервер,
                // кто сейчас онлайн.
                refreshOnlineUsersFromHealth()


                // И потом обновляем раз в 5 секунд.
                startOnlineRefreshLoop()
            }


            override fun onMessage(
                webSocket: WebSocket,
                text: String
            ) {

                try {

                    val json =
                        JSONObject(
                            text
                        )


                    when (
                        json.optString(
                            "type"
                        )
                    ) {


                        // =====================================
                        // CONNECTED
                        // =====================================

                        "connected" -> {

                            isConnected =
                                true


                            _connectionState.value =
                                ConnectionState.ONLINE


                            // Если сервер когда-нибудь
                            // начнёт передавать online_users
                            // прямо здесь — тоже обработаем.
                            if (
                                json.has(
                                    "online_users"
                                )
                            ) {

                                applyPresence(
                                    json
                                )
                            }


                            refreshOnlineUsersFromHealth()

                            requestServerUsers()
                        }


                        // =====================================
                        // PRESENCE
                        // =====================================

                        "presence" -> {

                            applyPresence(
                                json
                            )
                        }


                        // =====================================
                        // TYPING
                        // =====================================

                        "typing" -> {

                            val chatId =
                                json.optLong(
                                    "chat_id",
                                    -1L
                                )

                            val sender =
                                json
                                    .optString(
                                        "sender_name"
                                    )
                                    .trim()

                            val isTyping =
                                json.optBoolean(
                                    "is_typing",
                                    false
                                )

                            if (
                                chatId > 0L &&
                                sender.isNotBlank() &&
                                !sender.equals(
                                    currentUserName,
                                    ignoreCase = true
                                )
                            ) {

                                val current =
                                    _typingUsers
                                        .value
                                        .toMutableMap()

                                val names =
                                    current[
                                        chatId
                                    ]
                                        ?.toMutableSet()
                                        ?: mutableSetOf()

                                if (
                                    isTyping
                                ) {
                                    names.add(
                                        sender
                                    )
                                } else {
                                    names.removeAll {
                                        it.equals(
                                            sender,
                                            ignoreCase = true
                                        )
                                    }
                                }

                                if (
                                    names.isEmpty()
                                ) {
                                    current.remove(
                                        chatId
                                    )
                                } else {
                                    current[
                                        chatId
                                    ] =
                                        names
                                }

                                _typingUsers.value =
                                    current
                            }
                        }


                        "call_invite",
                        "call_accept",
                        "call_reject",
                        "call_end",
                        "webrtc_offer",
                        "webrtc_answer",
                        "ice_candidate",
                        "call_mute",
                        "call_camera" -> {
                            callSignals.tryEmit(
                                CallSignal(
                                    type = json.optString("type"),
                                    callId = json.optString("call_id"),
                                    senderName = json.optString("sender_name"),
                                    chatId = json.optLong("chat_id", 0L),
                                    video = json.optBoolean("video", false),
                                    sdp = json.optString("sdp"),
                                    sdpType = json.optString("sdp_type"),
                                    candidate = json.optString("candidate"),
                                    sdpMid = json.optString("sdp_mid"),
                                    sdpMLineIndex = json.optInt("sdp_mline_index", 0)
                                )
                            )
                        }


                        // =====================================
                        // TEXT
                        // =====================================

                        "chat_message" -> {

                            val messageId =
                                json.optLong(
                                    "message_id",
                                    -1L
                                )


                            val chatId =
                                json.optLong(
                                    "chat_id",
                                    -1L
                                )


                            val sender =
                                json.optString(
                                    "sender_name"
                                )


                            if (
                                chatId > 0L &&
                                sender.isNotBlank()
                            ) {

                                val currentTyping =
                                    _typingUsers
                                        .value
                                        .toMutableMap()

                                val names =
                                    currentTyping[
                                        chatId
                                    ]
                                        ?.toMutableSet()

                                if (
                                    names != null
                                ) {

                                    names.removeAll {
                                        it.equals(
                                            sender,
                                            ignoreCase = true
                                        )
                                    }

                                    if (
                                        names.isEmpty()
                                    ) {
                                        currentTyping.remove(
                                            chatId
                                        )
                                    } else {
                                        currentTyping[
                                            chatId
                                        ] =
                                            names
                                    }

                                    _typingUsers.value =
                                        currentTyping
                                }
                            }


                            val messageText =
                                json.optString(
                                    "text"
                                )


                            val context =
                                appContext


                            if (
                                context != null &&
                                chatId > 0L &&
                                sender.isNotBlank() &&
                                messageText.isNotBlank() &&
                                sender != currentUserName
                            ) {

                                scope.launch {

                                    ChatRepository
                                        .receiveTextMessage(
                                            context =
                                                context,

                                            messageId =
                                                messageId,

                                            chatId =
                                                chatId,

                                            sender =
                                                sender,

                                            text =
                                                messageText
                                        )
                                }
                            }
                        }


                        // =====================================
                        // MEDIA
                        // =====================================

                        "media_message" -> {

                            val messageId =
                                json.optLong(
                                    "message_id",
                                    -1L
                                )


                            val chatId =
                                json.optLong(
                                    "chat_id",
                                    -1L
                                )


                            val sender =
                                json.optString(
                                    "sender_name"
                                )


                            val mediaUrl =
                                json.optString(
                                    "media_url"
                                )


                            val fileName =
                                json.optString(
                                    "file_name"
                                )


                            val caption =
                                json.optString(
                                    "caption"
                                )


                            val type =
                                try {

                                    MessageType.valueOf(
                                        json.optString(
                                            "media_type",
                                            MessageType.FILE.name
                                        )
                                    )

                                } catch (_: Exception) {

                                    MessageType.FILE
                                }


                            val context =
                                appContext


                            if (
                                context != null &&
                                chatId > 0L &&
                                sender.isNotBlank() &&
                                mediaUrl.isNotBlank() &&
                                sender != currentUserName
                            ) {

                                scope.launch {

                                    ChatRepository
                                        .receiveMediaMessage(
                                            context =
                                                context,

                                            messageId =
                                                messageId,

                                            chatId =
                                                chatId,

                                            sender =
                                                sender,

                                            mediaType =
                                                type,

                                            mediaUrl =
                                                mediaUrl,

                                            fileName =
                                                fileName,

                                            caption =
                                                caption
                                        )
                                }
                            }
                        }


                        // =====================================
                        // SERVER USERS
                        // =====================================

                        "users_sync" -> {

                            val users =
                                json.optJSONArray(
                                    "users"
                                )
                                    ?: JSONArray()

                            val names =
                                mutableListOf<String>()

                            val profiles =
                                mutableListOf<ServerUserProfile>()

                            for (
                            index in 0 until users.length()
                            ) {

                                val item =
                                    users.optJSONObject(index)
                                        ?: continue

                                val name =
                                    item
                                        .optString(
                                            "name"
                                        )
                                        .trim()

                                val phone =
                                    item
                                        .optString(
                                            "phone"
                                        )
                                        .trim()

                                val normalizedPhone =
                                    item
                                        .optString(
                                            "phone_normalized"
                                        )
                                        .trim()
                                        .ifBlank {
                                            PhoneContactsRepository
                                                .normalizePhone(
                                                    phone
                                                )
                                        }

                                if (
                                    name.isNotBlank() &&
                                    names.none {
                                        it.equals(
                                            name,
                                            ignoreCase = true
                                        )
                                    }
                                ) {

                                    names.add(
                                        name
                                    )
                                }

                                if (
                                    name.isNotBlank() &&
                                    profiles.none {
                                        it.name.equals(
                                            name,
                                            ignoreCase = true
                                        )
                                    }
                                ) {

                                    profiles.add(
                                        ServerUserProfile(
                                            userId =
                                                item.optLong(
                                                    "user_id",
                                                    0L
                                                ),
                                            name =
                                                name,
                                            phone =
                                                phone,
                                            normalizedPhone =
                                                normalizedPhone
                                        )
                                    )
                                }
                            }

                            _serverUsers.value =
                                names

                            _serverUserProfiles.value =
                                profiles
                        }


                        // =====================================
                        // SERVER CHAT LIST SYNC
                        // =====================================

                        "chat_sync" -> {

                            val serverChats =
                                json.optJSONArray(
                                    "chats"
                                )
                                    ?: JSONArray()

                            val context =
                                appContext

                            if (context != null) {

                                scope.launch {

                                    for (
                                    index in 0 until serverChats.length()
                                    ) {

                                        val item =
                                            serverChats.optJSONObject(index)
                                                ?: continue

                                        applyServerChatMetadata(
                                            context,
                                            item
                                        )
                                    }
                                }
                            }
                        }


                        "chat_updated" -> {

                            val context =
                                appContext

                            if (context != null) {

                                scope.launch {
                                    applyServerChatMetadata(
                                        context,
                                        json
                                    )
                                }
                            }
                        }


                        // =====================================
                        // SERVER HISTORY / OFFLINE SYNC
                        // =====================================

                        "history_sync" -> {

                            val messages =
                                json.optJSONArray(
                                    "messages"
                                )
                                    ?: JSONArray()


                            val context =
                                appContext


                            if (
                                context != null
                            ) {

                                scope.launch {

                                    for (
                                    index in
                                    0 until
                                            messages.length()
                                    ) {

                                        val item =
                                            messages.optJSONObject(
                                                index
                                            )
                                                ?: continue


                                        val messageId =
                                            item.optLong(
                                                "message_id",
                                                -1L
                                            )


                                        val chatId =
                                            item.optLong(
                                                "chat_id",
                                                -1L
                                            )


                                        val sender =
                                            item.optString(
                                                "sender_name"
                                            )


                                        val rawType =
                                            item.optString(
                                                "media_type",
                                                MessageType.TEXT.name
                                            )


                                        val messageType =
                                            try {

                                                MessageType.valueOf(
                                                    rawType
                                                )

                                            } catch (_: Exception) {

                                                MessageType.TEXT
                                            }


                                        val rawStatus =
                                            item.optString(
                                                "status",
                                                MessageStatus.DELIVERED.name
                                            )


                                        val status =
                                            try {

                                                MessageStatus.valueOf(
                                                    rawStatus
                                                )

                                            } catch (_: Exception) {

                                                MessageStatus.DELIVERED
                                            }


                                        val membersArray =
                                            item.optJSONArray(
                                                "members"
                                            )


                                        val members =
                                            mutableListOf<String>()


                                        if (
                                            membersArray != null
                                        ) {

                                            for (
                                            memberIndex in
                                            0 until
                                                    membersArray.length()
                                            ) {

                                                val member =
                                                    membersArray
                                                        .optString(
                                                            memberIndex
                                                        )
                                                        .trim()


                                                if (
                                                    member.isNotBlank()
                                                ) {

                                                    members.add(
                                                        member
                                                    )
                                                }
                                            }
                                        }


                                        ChatRepository
                                            .syncServerMessage(
                                                context = context,
                                                messageId = messageId,
                                                chatId = chatId,
                                                sender = sender,
                                                messageType = messageType,
                                                text =
                                                    item.optString(
                                                        "text"
                                                    ),
                                                mediaUrl =
                                                    item.optString(
                                                        "media_url"
                                                    ),
                                                fileName =
                                                    item.optString(
                                                        "file_name"
                                                    ),
                                                caption =
                                                    item.optString(
                                                        "caption"
                                                    ),
                                                createdAt =
                                                    item.optLong(
                                                        "created_at",
                                                        System.currentTimeMillis()
                                                    ),
                                                serverStatus =
                                                    status,
                                                members =
                                                    members,
                                                chatTitle =
                                                    item.optString(
                                                        "chat_title"
                                                    ),
                                                chatType =
                                                    try {

                                                        ChatType.valueOf(
                                                            item.optString(
                                                                "chat_type",
                                                                ChatType.PRIVATE.name
                                                            )
                                                        )

                                                    } catch (_: Exception) {

                                                        ChatType.PRIVATE
                                                    }
                                            )
                                    }
                                }
                            }
                        }


                        // =====================================
                        // DELIVERY RECEIPT FROM OFFLINE SYNC
                        // =====================================

                        "delivery_receipt" -> {

                            val ids =
                                json.optJSONArray(
                                    "message_ids"
                                )


                            val context =
                                appContext


                            if (
                                context != null &&
                                ids != null
                            ) {

                                scope.launch {

                                    for (
                                    index in
                                    0 until
                                            ids.length()
                                    ) {

                                        val messageId =
                                            ids.optLong(
                                                index,
                                                -1L
                                            )


                                        if (
                                            messageId > 0L
                                        ) {

                                            ChatRepository
                                                .updateOutgoingStatus(
                                                    context = context,
                                                    messageId = messageId,
                                                    status =
                                                        MessageStatus.DELIVERED
                                                )
                                        }
                                    }
                                }
                            }
                        }


                        // =====================================
                        // SERVER ACK
                        // =====================================

                        "server_ack" -> {

                            val messageId =
                                json.optLong(
                                    "message_id",
                                    -1L
                                )


                            val delivered =
                                json.optInt(
                                    "delivered_online",
                                    0
                                )


                            val context =
                                appContext


                            if (
                                context != null &&
                                messageId > 0L
                            ) {

                                scope.launch {

                                    ChatRepository
                                        .updateOutgoingStatus(
                                            context =
                                                context,

                                            messageId =
                                                messageId,

                                            status =

                                                if (
                                                    delivered > 0
                                                ) {

                                                    MessageStatus.DELIVERED

                                                } else {

                                                    MessageStatus.SENT
                                                }
                                        )
                                }
                            }
                        }


                        // =====================================
                        // READ RECEIPT
                        // =====================================

                        "read_receipt" -> {

                            val readerName =
                                json.optString(
                                    "reader_name"
                                )


                            val ids =
                                json.optJSONArray(
                                    "message_ids"
                                )


                            val context =
                                appContext


                            if (
                                context != null &&
                                readerName.isNotBlank() &&
                                readerName != currentUserName &&
                                ids != null
                            ) {

                                scope.launch {

                                    for (
                                    index in
                                    0 until
                                            ids.length()
                                    ) {

                                        val messageId =
                                            ids.optLong(
                                                index,
                                                -1L
                                            )


                                        if (
                                            messageId > 0L
                                        ) {

                                            ChatRepository
                                                .updateOutgoingStatus(
                                                    context =
                                                        context,

                                                    messageId =
                                                        messageId,

                                                    status =
                                                        MessageStatus.READ
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }


                } catch (
                    error: Exception
                ) {

                    error.printStackTrace()
                }
            }


            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {

                if (
                    socket === webSocket
                ) {

                    socket =
                        null
                }


                isConnected =
                    false


                _connectionState.value =
                    ConnectionState.OFFLINE


                _onlineUsers.value =
                    emptySet()


                onlineRefreshJob
                    ?.cancel()


                onlineRefreshJob =
                    null


                if (
                    !manualDisconnect
                ) {

                    scheduleReconnect()
                }
            }


            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {

                if (
                    socket === webSocket
                ) {

                    socket =
                        null
                }


                isConnected =
                    false


                _connectionState.value =
                    ConnectionState.OFFLINE


                _onlineUsers.value =
                    emptySet()


                onlineRefreshJob
                    ?.cancel()


                onlineRefreshJob =
                    null


                t.printStackTrace()


                if (
                    !manualDisconnect
                ) {

                    scheduleReconnect()
                }
            }
        }


    private suspend fun applyServerChatMetadata(
        context: Context,
        json: JSONObject
    ) {

        val chatId =
            json.optLong(
                "chat_id",
                -1L
            )

        if (chatId <= 0L) {
            return
        }

        val membersArray =
            json.optJSONArray(
                "members"
            )

        val members =
            mutableListOf<String>()

        if (membersArray != null) {

            for (
            index in 0 until membersArray.length()
            ) {

                val member =
                    membersArray
                        .optString(index)
                        .trim()

                if (
                    member.isNotBlank() &&
                    members.none {
                        it.equals(
                            member,
                            ignoreCase = true
                        )
                    }
                ) {
                    members.add(member)
                }
            }
        }

        val chatType =
            try {
                ChatType.valueOf(
                    json.optString(
                        "chat_type",
                        ChatType.GROUP.name
                    )
                )
            } catch (_: Exception) {
                ChatType.GROUP
            }

        ChatRepository.syncServerChatMetadata(
            context = context,
            chatId = chatId,
            title = json.optString(
                "chat_title",
                json.optString("title")
            ),
            type = chatType,
            members = members,
            updatedAt = json.optLong(
                "updated_at",
                System.currentTimeMillis()
            )
        )
    }


    // =====================================================
    // MIME
    // =====================================================

    private fun guessMimeType(
        fileName: String
    ): String {

        val name =
            fileName.lowercase()


        return when {

            name.endsWith(".jpg") ||
                    name.endsWith(".jpeg") ->

                "image/jpeg"


            name.endsWith(".png") ->

                "image/png"


            name.endsWith(".webp") ->

                "image/webp"


            name.endsWith(".gif") ->

                "image/gif"


            name.endsWith(".m4a") ->

                "audio/mp4"


            name.endsWith(".mp3") ->

                "audio/mpeg"


            name.endsWith(".wav") ->

                "audio/wav"


            name.endsWith(".mp4") ->

                "video/mp4"


            name.endsWith(".pdf") ->

                "application/pdf"


            name.endsWith(".txt") ->

                "text/plain"


            name.endsWith(".zip") ->

                "application/zip"


            else ->

                "application/octet-stream"
        }
    }
}