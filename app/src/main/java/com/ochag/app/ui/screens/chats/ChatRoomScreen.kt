package com.ochag.app.ui.screens.chats

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.location.LocationManager
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ochag.app.R
import com.ochag.app.data.Chat
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.ChatType
import com.ochag.app.data.Message
import com.ochag.app.data.MessageStatus
import com.ochag.app.data.MessageType
import com.ochag.app.data.ServerRealtime
import com.ochag.app.data.StickerStore
import com.ochag.app.ui.theme.BorderSoft
import com.ochag.app.ui.theme.BrownTextPrimary
import com.ochag.app.ui.theme.BrownTextSecondary
import com.ochag.app.ui.theme.CardCream
import com.ochag.app.ui.theme.CardWhite
import com.ochag.app.ui.theme.CreamBackground
import com.ochag.app.ui.theme.OnlineGreen
import com.ochag.app.ui.theme.SoftGreenLight
import com.ochag.app.ui.theme.TerracottaDark
import com.ochag.app.ui.theme.TerracottaLight
import com.ochag.app.ui.theme.TerracottaPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

private const val STICKER_EMOJI_PREFIX = "__STICKER_EMOJI__:"
private const val STICKER_IMAGE_CAPTION = "__STICKER_IMAGE__"

private enum class MediaButtonMode { AUDIO, VIDEO }

private enum class MessageFontStyle(val title: String, val family: FontFamily) {
    NORMAL("Обычный", FontFamily.Default),
    MODERN("Современный", FontFamily.SansSerif),
    CLASSIC("Классический", FontFamily.Serif),
    HANDWRITTEN("Рукописный", FontFamily.Cursive),
    MONO("Печатный", FontFamily.Monospace)
}

private enum class MessageFontSize(val title: String, val sizeSp: Int) {
    VERY_SMALL("Очень маленький", 12),
    SMALL("Маленький", 14),
    MEDIUM("Средний", 16),
    LARGE("Большой", 19),
    VERY_LARGE("Очень большой", 23)
}

private const val FONT_PREFS = "ochag_message_font"
private const val FONT_STYLE_KEY = "font_style"
private const val FONT_SIZE_KEY = "font_size"

private data class PendingAttachment(
    val uri: Uri,
    val fileName: String,
    val type: MessageType
)

private data class ActiveVideoRecording(
    val recording: Recording,
    val file: File,
    val shouldSend: AtomicBoolean
)

@Composable
fun ChatRoomScreen(
    chatId: Long,
    chatTitle: String,
    onKeeperClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = context as LifecycleOwner
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var currentGroupAvatarUri by remember(chatId) {
        mutableStateOf(
            appContext.getSharedPreferences(
                "ochag_group_avatars",
                Context.MODE_PRIVATE
            ).getString("group_avatar_$chatId", "").orEmpty()
        )
    }

    var showChatSettings by remember { mutableStateOf(false) }
    var showFontSettings by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }

    val fontPreferences = remember {
        appContext.getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE)
    }
    var messageFontStyle by remember {
        mutableStateOf(
            runCatching {
                MessageFontStyle.valueOf(
                    fontPreferences.getString(
                        FONT_STYLE_KEY,
                        MessageFontStyle.NORMAL.name
                    ) ?: MessageFontStyle.NORMAL.name
                )
            }.getOrDefault(MessageFontStyle.NORMAL)
        )
    }
    var messageFontSize by remember {
        mutableStateOf(
            runCatching {
                MessageFontSize.valueOf(
                    fontPreferences.getString(
                        FONT_SIZE_KEY,
                        MessageFontSize.MEDIUM.name
                    ) ?: MessageFontSize.MEDIUM.name
                )
            }.getOrDefault(MessageFontSize.MEDIUM)
        )
    }
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()
    val typingUsers by ServerRealtime.typingUsers.collectAsState()

    val typingNames = typingUsers[chatId]
        .orEmpty()
        .filter { !it.equals(ServerRealtime.userName, ignoreCase = true) }

    val currentChat = ChatRepository.findChat(chatId)

    val headerStatusText = remember(onlineUsers, typingNames, currentChat, chatId) {
        if (typingNames.isNotEmpty()) {
            if (typingNames.size == 1) "${typingNames.first()} печатает…"
            else "${typingNames.size} печатают…"
        } else {
            buildChatHeaderPresenceText(currentChat, onlineUsers)
        }
    }

    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(chatId, messageText) {
        if (messageText.isBlank()) {
            ServerRealtime.sendTyping(chatId, false)
            return@LaunchedEffect
        }
        ServerRealtime.sendTyping(chatId, true)
        delay(1200L)
        ServerRealtime.sendTyping(chatId, false)
    }

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var attachmentStatus by remember { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }
    var mediaButtonMode by remember { mutableStateOf(MediaButtonMode.AUDIO) }
    var isRecording by remember { mutableStateOf(false) }

    var microphoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result[Manifest.permission.RECORD_AUDIO]?.let { microphoneGranted = it }
        result[Manifest.permission.CAMERA]?.let { cameraGranted = it }
        result[Manifest.permission.ACCESS_FINE_LOCATION]?.let { locationGranted = it }
    }

    fun sendCurrentLocation() {
        if (!locationGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            attachmentStatus = "Разрешите геолокацию и нажмите «Локация» ещё раз"
            return
        }
        try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location == null) {
                attachmentStatus = "Включите GPS и попробуйте ещё раз"
                return
            }
            coroutineScope.launch {
                ChatRepository.sendMessage(
                    context = appContext,
                    chatId = chatId,
                    text = "📍 Геолокация\nhttps://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}"
                )
            }
            attachmentStatus = ""
        } catch (_: SecurityException) {
            attachmentStatus = "Нет разрешения на геолокацию"
        }
    }

    var audioRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var audioStartedAt by remember { mutableLongStateOf(0L) }

    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeVideoRecording by remember { mutableStateOf<ActiveVideoRecording?>(null) }
    var useFrontCamera by remember { mutableStateOf(true) }

    val messages by ChatRepository.getMessages(
        context = appContext,
        chatId = chatId
    ).collectAsState(initial = emptyList())

    val serverUsers by ServerRealtime.serverUsers.collectAsState()

    LaunchedEffect(chatId, messages.map { it.id to it.isMine }) {
        val incomingIds = messages
            .filter { !it.isMine }
            .map { it.id }
            .filter { it > 0L }
            .distinct()

        if (incomingIds.isNotEmpty()) {
            ServerRealtime.sendReadReceipt(chatId, incomingIds)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takeReadPermission(context, uri)
            pendingAttachment = PendingAttachment(
                uri = uri,
                fileName = getFileName(context, uri),
                type = MessageType.IMAGE
            )
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takeReadPermission(context, uri)
            pendingAttachment = PendingAttachment(
                uri = uri,
                fileName = getFileName(context, uri),
                type = MessageType.FILE
            )
        }
    }

    val contactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0).orEmpty()
                    val number = cursor.getString(1).orEmpty()
                    coroutineScope.launch {
                        ChatRepository.sendMessage(
                            context = appContext,
                            chatId = chatId,
                            text = "📇 $name\n$number"
                        )
                    }
                }
            }
        } catch (_: Exception) {
            attachmentStatus = "Не удалось открыть контакт"
        }
    }

    LaunchedEffect(chatId) {
        ChatRepository.seedInitialMessages(appContext, chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val requestedMessageId = MessageSelectionController.scrollToMessageId
    LaunchedEffect(requestedMessageId, messages) {
        val targetId = requestedMessageId ?: return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
        MessageSelectionController.clearScrollRequest()
    }

    fun startAudioRecording(): Boolean {
        if (!microphoneGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return false
        }

        return try {
            val directory = File(appContext.filesDir, "voice_messages").apply { mkdirs() }
            val file = File(directory, "voice_${System.currentTimeMillis()}.m4a")

            @Suppress("DEPRECATION")
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            audioRecorder = newRecorder
            audioFile = file
            audioStartedAt = System.currentTimeMillis()
            isRecording = true
            true
        } catch (_: Exception) {
            try { audioRecorder?.release() } catch (_: Exception) {}
            audioFile?.delete()
            audioRecorder = null
            audioFile = null
            isRecording = false
            false
        }
    }

    fun finishAudioRecording(send: Boolean) {
        val recorder = audioRecorder
        val file = audioFile
        val startedAt = audioStartedAt
        var valid = true

        try { recorder?.stop() } catch (_: Exception) { valid = false }
        try { recorder?.release() } catch (_: Exception) {}

        audioRecorder = null
        audioFile = null
        audioStartedAt = 0L
        isRecording = false

        if (file == null) return

        val duration = System.currentTimeMillis() - startedAt
        if (!send || !valid || duration < 500L) {
            file.delete()
            return
        }

        coroutineScope.launch {
            ChatRepository.sendVoiceMessage(
                context = appContext,
                chatId = chatId,
                audioPath = file.absolutePath,
                durationMs = duration
            )
        }
    }

    fun startVideoRecording(): Boolean {
        if (!cameraGranted || !microphoneGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
            return false
        }

        val capture = videoCapture ?: return false

        return try {
            val directory = File(appContext.filesDir, "video_circles").apply { mkdirs() }
            val file = File(directory, "circle_${System.currentTimeMillis()}.mp4")
            val outputOptions = FileOutputOptions.Builder(file).build()
            val shouldSend = AtomicBoolean(false)

            val recording = capture.output
                .prepareRecording(appContext, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(appContext)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> isRecording = true
                        is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            val durationMs =
                                (event.recordingStats.recordedDurationNanos / 1_000_000L)
                                    .coerceAtLeast(1L)

                            val canSend = shouldSend.get() &&
                                !event.hasError() &&
                                file.exists() &&
                                durationMs >= 500L

                            activeVideoRecording = null

                            if (canSend) {
                                mediaButtonMode = MediaButtonMode.AUDIO
                                videoCapture = null

                                coroutineScope.launch {
                                    ChatRepository.sendVideoCircleMessage(
                                        context = appContext,
                                        chatId = chatId,
                                        videoPath = file.absolutePath,
                                        durationMs = durationMs
                                    )
                                }
                            } else {
                                file.delete()
                            }
                        }
                    }
                }

            activeVideoRecording = ActiveVideoRecording(recording, file, shouldSend)
            true
        } catch (_: Exception) {
            isRecording = false
            false
        }
    }

    fun finishVideoRecording(send: Boolean) {
        val active = activeVideoRecording ?: return
        active.shouldSend.set(send)
        try {
            active.recording.stop()
        } catch (_: Exception) {
            active.file.delete()
            activeVideoRecording = null
            isRecording = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { audioRecorder?.stop() } catch (_: Exception) {}
            try { audioRecorder?.release() } catch (_: Exception) {}
            audioFile?.delete()
            activeVideoRecording?.shouldSend?.set(false)
            try { activeVideoRecording?.recording?.stop() } catch (_: Exception) {}
        }
    }

    if (showChatSettings && currentChat != null && currentChat.type != ChatType.PRIVATE) {
        ChatGroupSettingsDialog(
            chat = currentChat,
            serverUsers = serverUsers,
            onlineUsers = onlineUsers,
            onAvatarChanged = { uri ->
                currentGroupAvatarUri = uri
            },
            onDismiss = { showChatSettings = false },
            onSave = { newTitle, newMembers ->
                coroutineScope.launch {
                    ChatRepository.updateChatDetails(
                        context = appContext,
                        chatId = chatId,
                        title = newTitle,
                        members = newMembers
                    )
                    showChatSettings = false
                }
            }
        )
    }

    if (showFontSettings) {
        MessageFontSettingsDialog(
            selectedStyle = messageFontStyle,
            selectedSize = messageFontSize,
            onDismiss = { showFontSettings = false },
            onSave = { style, size ->
                messageFontStyle = style
                messageFontSize = size
                fontPreferences.edit()
                    .putString(FONT_STYLE_KEY, style.name)
                    .putString(FONT_SIZE_KEY, size.name)
                    .apply()
                showFontSettings = false
            }
        )
    }

    if (showPollDialog) {
        AlertDialog(
            onDismissRequest = { showPollDialog = false },
            title = { Text("Создать опрос") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Название опроса") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    pollOptions.forEachIndexed { index, option ->
                        OutlinedTextField(
                            value = option,
                            onValueChange = { value ->
                                pollOptions = pollOptions.toMutableList().also { it[index] = value }
                            },
                            label = { Text("Вариант ${index + 1}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (pollOptions.size < 6) {
                        TextButton(
                            onClick = { pollOptions = pollOptions + "" }
                        ) { Text("＋ Добавить вариант") }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = pollQuestion.isNotBlank() &&
                        pollOptions.count { it.isNotBlank() } >= 2,
                    onClick = {
                        val options = pollOptions.filter { it.isNotBlank() }
                        val pollText = buildString {
                            append("📊 ОПРОС: ")
                            append(pollQuestion.trim())
                            options.forEachIndexed { index, option ->
                                append("\n${index + 1}. ${option.trim()}")
                            }
                        }
                        coroutineScope.launch {
                            ChatRepository.sendMessage(appContext, chatId, pollText)
                        }
                        pollQuestion = ""
                        pollOptions = listOf("", "")
                        showPollDialog = false
                    }
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showPollDialog = false }) { Text("Отмена") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(
                chatId = chatId,
                avatarUri = currentGroupAvatarUri,
                chatTitle = currentChat?.title ?: chatTitle,
                statusText = headerStatusText,
                canOpenSettings = currentChat != null && currentChat.type != ChatType.PRIVATE,
                onSettingsClick = {
                    ServerRealtime.requestServerUsers()
                    showChatSettings = true
                },
                onFontSettingsClick = { showFontSettings = true },
                onBackClick = onBackClick
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        fontStyle = messageFontStyle,
                        fontSize = messageFontSize
                    )
                }
            }

            if (mediaButtonMode == MediaButtonMode.VIDEO && cameraGranted) {
                CameraCirclePreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    useFrontCamera = useFrontCamera,
                    isRecording = isRecording,
                    onCaptureReady = { videoCapture = it },
                    onSwitchCamera = {
                        if (!isRecording) useFrontCamera = !useFrontCamera
                    }
                )
            }

            if (isRecording) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = TerracottaLight
                ) {
                    Text(
                        text = if (mediaButtonMode == MediaButtonMode.AUDIO) {
                            "● Записывается голосовое…"
                        } else {
                            "● Записывается видеокружок…"
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = TerracottaDark,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            pendingAttachment?.let { attachment ->
                PendingAttachmentPreview(
                    attachment = attachment,
                    onRemove = { pendingAttachment = null }
                )
            }

            if (attachmentStatus.isNotBlank()) {
                Text(
                    text = attachmentStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showStickerPicker) {
                StickerPickerPanel(
                    onEmojiClick = { emoji ->
                        showStickerPicker = false
                        coroutineScope.launch {
                            ChatRepository.sendMessage(
                                context = appContext,
                                chatId = chatId,
                                text = STICKER_EMOJI_PREFIX + emoji
                            )
                        }
                    },
                    onImageClick = { uri ->
                        showStickerPicker = false
                        coroutineScope.launch {
                            ChatRepository.sendAttachment(
                                context = appContext,
                                chatId = chatId,
                                uri = uri,
                                fileName = "sticker_${System.currentTimeMillis()}.png",
                                messageType = MessageType.IMAGE,
                                caption = STICKER_IMAGE_CAPTION
                            )
                        }
                    }
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                shape = RoundedCornerShape(30.dp),
                color = CardWhite,
                shadowElevation = 14.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                    showStickerPicker = false
                                    showAttachmentMenu = true
                                },
                            shape = CircleShape,
                            color = TerracottaLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "＋",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TerracottaDark
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false },
                            containerColor = CardWhite,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("📷  Фото", color = BrownTextPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    imageLauncher.launch(arrayOf("image/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📎  Файл", color = BrownTextPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    fileLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📊  Опрос", color = BrownTextPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    showPollDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📍  Локация", color = BrownTextPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    sendCurrentLocation()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📇  Контакт", color = BrownTextPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    val intent = Intent(
                                        Intent.ACTION_PICK,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                    )
                                    contactLauncher.launch(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("✨  Хранитель Очага", color = BrownTextPrimary) },
                                onClick = {
                                    showAttachmentMenu = false
                                    onKeeperClick()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                showAttachmentMenu = false
                                showStickerPicker = !showStickerPicker
                            },
                        shape = CircleShape,
                        color = if (showStickerPicker) TerracottaLight else CardCream
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("☺", fontSize = 24.sp, color = TerracottaDark)
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (pendingAttachment != null) "Добавить подпись…"
                                else "Сообщение…",
                                color = BrownTextSecondary
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = CardCream,
                            unfocusedContainerColor = CardCream,
                            focusedTextColor = BrownTextPrimary,
                            unfocusedTextColor = BrownTextPrimary,
                            cursorColor = TerracottaPrimary
                        )
                    )

                    Spacer(Modifier.width(6.dp))

                    if (messageText.isNotBlank() || pendingAttachment != null) {
                        Button(
                            modifier = Modifier.size(50.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TerracottaPrimary,
                                contentColor = Color.White
                            ),
                            onClick = {
                                val text = messageText.trim()
                                val attachment = pendingAttachment
                                messageText = ""
                                pendingAttachment = null
                                showStickerPicker = false

                                coroutineScope.launch {
                                    if (attachment != null) {
                                        ChatRepository.sendAttachment(
                                            context = appContext,
                                            chatId = chatId,
                                            uri = attachment.uri.toString(),
                                            fileName = attachment.fileName,
                                            messageType = attachment.type,
                                            caption = text
                                        )
                                    } else {
                                        ChatRepository.sendMessage(
                                            context = appContext,
                                            chatId = chatId,
                                            text = text
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("➤", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .pointerInput(
                                    mediaButtonMode,
                                    microphoneGranted,
                                    cameraGranted,
                                    videoCapture
                                ) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)

                                        val quickRelease = withTimeoutOrNull(220L) {
                                            waitForUpOrCancellation()
                                        }

                                        if (quickRelease != null) {
                                            mediaButtonMode =
                                                if (mediaButtonMode == MediaButtonMode.AUDIO) {
                                                    MediaButtonMode.VIDEO
                                                } else {
                                                    MediaButtonMode.AUDIO
                                                }

                                            if (
                                                mediaButtonMode == MediaButtonMode.VIDEO &&
                                                (!cameraGranted || !microphoneGranted)
                                            ) {
                                                permissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.CAMERA,
                                                        Manifest.permission.RECORD_AUDIO
                                                    )
                                                )
                                            }
                                            return@awaitEachGesture
                                        }

                                        showStickerPicker = false

                                        val started = when (mediaButtonMode) {
                                            MediaButtonMode.AUDIO -> startAudioRecording()
                                            MediaButtonMode.VIDEO -> startVideoRecording()
                                        }

                                        if (!started) {
                                            waitForUpOrCancellation()
                                            return@awaitEachGesture
                                        }

                                        val released = waitForUpOrCancellation()
                                        val send = released != null

                                        when (mediaButtonMode) {
                                            MediaButtonMode.AUDIO -> finishAudioRecording(send)
                                            MediaButtonMode.VIDEO -> finishVideoRecording(send)
                                        }
                                    }
                                },
                            shape = CircleShape,
                            color = if (isRecording) TerracottaDark else TerracottaPrimary
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRecording) {
                                    Text("●", color = Color.White)
                                } else {
                                    Icon(
                                        painter = painterResource(
                                            id = if (mediaButtonMode == MediaButtonMode.AUDIO) {
                                                R.drawable.ic_audio_retro
                                            } else {
                                                R.drawable.ic_video_camera
                                            }
                                        ),
                                        contentDescription = if (mediaButtonMode == MediaButtonMode.AUDIO) {
                                            "Аудио"
                                        } else {
                                            "Видео"
                                        },
                                        modifier = Modifier.size(27.dp),
                                        tint = Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerPickerPanel(
    onEmojiClick: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = CardWhite,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 310.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Стикеры",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrownTextPrimary
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(StickerStore.packs, key = { it.id }) { pack ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = pack.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = BrownTextSecondary
                        )

                        if (pack.emojiStickers.isNotEmpty()) {
                            pack.emojiStickers.chunked(6).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { emoji ->
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clickable { onEmojiClick(emoji) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = CardCream
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(emoji, fontSize = 28.sp)
                                            }
                                        }
                                    }
                                    repeat(6 - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (pack.imageUris.isNotEmpty()) {
                            pack.imageUris.chunked(4).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { uri ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clickable { onImageClick(uri) }
                                        ) {
                                            UriImage(
                                                uri = Uri.parse(uri),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    repeat(4 - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatGroupSettingsDialog(
    chat: Chat,
    serverUsers: List<String>,
    onlineUsers: Set<String>,
    onAvatarChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    val context = LocalContext.current
    val avatarPrefs = remember {
        context.getSharedPreferences("ochag_group_avatars", Context.MODE_PRIVATE)
    }
    var avatarUri by remember(chat.id) {
        mutableStateOf(
            avatarPrefs.getString("group_avatar_${chat.id}", "").orEmpty()
        )
    }
    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takeReadPermission(context, uri)
            avatarUri = uri.toString()
            avatarPrefs.edit()
                .putString("group_avatar_${chat.id}", avatarUri)
                .apply()
            onAvatarChanged(avatarUri)
        }
    }
    var title by remember(chat.id, chat.title) { mutableStateOf(chat.title) }
    var searchText by remember { mutableStateOf("") }
    val selectedMembers = remember(chat.id, chat.members) {
        mutableStateListOf<String>().apply { addAll(chat.members) }
    }
    val myName = ServerRealtime.userName.trim()

    val availableMembers = remember(serverUsers, chat.members, searchText, onlineUsers) {
        (serverUsers + chat.members)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .filter {
                searchText.isBlank() || it.contains(searchText.trim(), ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<String> { member ->
                    onlineUsers.any { it.equals(member, ignoreCase = true) }
                }.thenBy { it.lowercase() }
            )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                when (chat.type) {
                    ChatType.FAMILY -> "Семья"
                    ChatType.GROUP -> "Настройки группы"
                    ChatType.THEME -> "Настройки темы"
                    ChatType.PRIVATE -> "Чат"
                },
                color = BrownTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название") },
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (avatarUri.isNotBlank()) {
                        UriImage(
                            uri = Uri.parse(avatarUri),
                            modifier = Modifier.size(56.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    OutlinedButton(
                        onClick = { avatarLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (avatarUri.isBlank()) {
                                "Добавить аватарку группы"
                            } else {
                                "Изменить аватарку группы"
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "${selectedMembers.size} участников",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrownTextSecondary
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Найти участника") },
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = availableMembers, key = { it.lowercase() }) { member ->
                        val selectedMember = selectedMembers.any {
                            it.equals(member, ignoreCase = true)
                        }
                        val isMe = member.equals(myName, ignoreCase = true)
                        val isOnline = onlineUsers.any {
                            it.equals(member, ignoreCase = true)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = CardCream
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = TerracottaLight
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            member.firstOrNull { it.isLetterOrDigit() }
                                                ?.uppercase() ?: "О",
                                            fontWeight = FontWeight.Bold,
                                            color = BrownTextPrimary
                                        )
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isMe) "$member • вы" else member,
                                        maxLines = 1,
                                        color = BrownTextPrimary
                                    )
                                    Text(
                                        if (isOnline) "● В сети" else "○ Не в сети",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOnline) OnlineGreen else BrownTextSecondary
                                    )
                                }

                                Checkbox(
                                    checked = selectedMember,
                                    enabled = !isMe,
                                    onCheckedChange = {
                                        if (isMe) return@Checkbox
                                        if (selectedMember) {
                                            selectedMembers.removeAll {
                                                it.equals(member, ignoreCase = true)
                                            }
                                        } else {
                                            selectedMembers.add(member)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isNotBlank()) {
                        val finalMembers = selectedMembers
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                            .toMutableList()

                        if (
                            myName.isNotBlank() &&
                            finalMembers.none { it.equals(myName, ignoreCase = true) }
                        ) {
                            finalMembers.add(0, myName)
                        }
                        onSave(cleanTitle, finalMembers)
                    }
                }
            ) { Text("Сохранить", color = TerracottaPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = BrownTextSecondary)
            }
        }
    )
}

private fun buildChatHeaderPresenceText(
    chat: Chat?,
    onlineUsers: Set<String>
): String {
    if (chat == null) {
        return if (ServerRealtime.isConnected) "● Подключено" else "○ Нет соединения"
    }

    if (chat.type == ChatType.PRIVATE) {
        val myName = ServerRealtime.userName.trim()
        val otherMember = chat.members.firstOrNull {
            !it.equals(myName, ignoreCase = true)
        }
        val isOnline = otherMember != null && onlineUsers.any {
            it.equals(otherMember, ignoreCase = true)
        }
        return if (isOnline) "● В сети" else "○ Не в сети"
    }

    val cleanMembers = chat.members
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

    val onlineCount = cleanMembers.count { member ->
        onlineUsers.any { it.equals(member, ignoreCase = true) }
    }

    return if (cleanMembers.isNotEmpty()) {
        "$onlineCount/${cleanMembers.size} в сети"
    } else {
        "0 в сети"
    }
}

@Composable
private fun ChatHeader(
    chatId: Long,
    avatarUri: String,
    chatTitle: String,
    statusText: String,
    canOpenSettings: Boolean,
    onSettingsClick: () -> Unit,
    onFontSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
        color = CardWhite,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBackClick),
                shape = CircleShape,
                color = CardCream
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "‹",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BrownTextPrimary
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            if (avatarUri.isNotBlank()) {
                UriImage(
                    uri = Uri.parse(avatarUri),
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = SoftGreenLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            chatTitle.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "О",
                            fontWeight = FontWeight.Bold,
                            color = BrownTextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (canOpenSettings) Modifier.clickable(onClick = onSettingsClick)
                        else Modifier
                    )
            ) {
                Text(
                    text = chatTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrownTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (
                        statusText.contains("В сети") || statusText.contains("печатает")
                    ) OnlineGreen else BrownTextSecondary
                )
            }

            Surface(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(42.dp)
                    .clickable(onClick = onFontSettingsClick),
                shape = CircleShape,
                color = CardCream
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Аа",
                        color = TerracottaDark,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            if (canOpenSettings) {
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onSettingsClick),
                    shape = CircleShape,
                    color = CardCream
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("•••", color = BrownTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageFontSettingsDialog(
    selectedStyle: MessageFontStyle,
    selectedSize: MessageFontSize,
    onDismiss: () -> Unit,
    onSave: (MessageFontStyle, MessageFontSize) -> Unit
) {
    var style by remember(selectedStyle) { mutableStateOf(selectedStyle) }
    var size by remember(selectedSize) { mutableStateOf(selectedSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Шрифт сообщений", color = BrownTextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Стиль", fontWeight = FontWeight.Bold, color = BrownTextPrimary)
                MessageFontStyle.values().forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { style = item }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = style == item,
                            onClick = { style = item }
                        )
                        Text(item.title, fontFamily = item.family, color = BrownTextPrimary)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Размер — ${size.title}",
                    fontWeight = FontWeight.Bold,
                    color = BrownTextPrimary
                )
                Slider(
                    value = size.ordinal.toFloat(),
                    onValueChange = { value ->
                        size = MessageFontSize.values()[value.toInt().coerceIn(0, 4)]
                    },
                    valueRange = 0f..4f,
                    steps = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Маленький", style = MaterialTheme.typography.labelSmall)
                    Text("Большой", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = TerracottaPrimary
                ) {
                    Text(
                        text = "Привет! Так будут выглядеть сообщения.",
                        modifier = Modifier.padding(14.dp),
                        color = Color.White,
                        fontFamily = style.family,
                        fontSize = size.sizeSp.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(style, size) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = CreamBackground
    )
}

@Composable
private fun CameraCirclePreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    useFrontCamera: Boolean,
    isRecording: Boolean,
    onCaptureReady: (VideoCapture<Recorder>) -> Unit,
    onSwitchCamera: () -> Unit
) {
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(useFrontCamera) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        future.addListener({
            try {
                provider = future.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD))
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                onCaptureReady(capture)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try { provider?.unbindAll() } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(220.dp)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp)
                    .clickable(enabled = !isRecording, onClick = onSwitchCamera),
                shape = CircleShape,
                color = CardWhite,
                shadowElevation = 5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("↻", color = BrownTextPrimary)
                }
            }

            if (isRecording) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = TerracottaDark
                ) {
                    Text(
                        "● REC",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingAttachmentPreview(
    attachment: PendingAttachment,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (attachment.type == MessageType.IMAGE) {
                UriImage(attachment.uri, Modifier.size(68.dp))
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (attachment.type == MessageType.IMAGE) "Фото" else "Файл",
                    color = BrownTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    attachment.fileName.ifBlank { "Без имени" },
                    color = BrownTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clickable(onClick = onRemove),
                shape = CircleShape,
                color = CardCream
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✕", color = BrownTextSecondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    fontStyle: MessageFontStyle,
    fontSize: MessageFontSize
) {
    val context = LocalContext.current
    val bubbleScope = rememberCoroutineScope()
    val isImage = message.messageType == MessageType.IMAGE.name
    val isFile = message.messageType == MessageType.FILE.name
    val isAudio = message.messageType == MessageType.AUDIO.name
    val isVideo = message.messageType == MessageType.VIDEO_CIRCLE.name
    val isEmojiSticker = message.messageType == MessageType.TEXT.name &&
        message.text.startsWith(STICKER_EMOJI_PREFIX)
    val isImageSticker = isImage && message.text == STICKER_IMAGE_CAPTION
    val isPoll = message.messageType == MessageType.TEXT.name &&
        message.text.startsWith("📊 ОПРОС:")
    val selected = MessageSelectionController.selectedIds.contains(message.id)
    val bubbleColor = when {
        selected && message.isMine -> TerracottaDark
        selected -> TerracottaLight
        message.isMine -> TerracottaPrimary
        else -> CardWhite
    }
    val bubbleText = if (message.isMine) Color.White else BrownTextPrimary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (MessageSelectionController.isSelecting) {
            Surface(
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .size(26.dp)
                    .clickable { MessageSelectionController.toggle(message.id) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) TerracottaPrimary else CardWhite,
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = SolidColor(
                        if (selected) TerracottaPrimary else BorderSoft
                    )
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!message.isMine) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = SoftGreenLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            message.sender.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                            color = BrownTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(7.dp))
            }

            Surface(
                modifier = Modifier
                    .widthIn(min = 82.dp, max = 292.dp)
                    .combinedClickable(
                        onClick = {
                            if (MessageSelectionController.isSelecting) {
                                MessageSelectionController.toggle(message.id)
                            }
                        },
                        onLongClick = {
                            if (MessageSelectionController.isSelecting) {
                                MessageSelectionController.toggle(message.id)
                            } else {
                                MessageSelectionController.openActions(message)
                            }
                        }
                    ),
                shape = if (message.isMine) {
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 8.dp,
                        bottomStart = 24.dp,
                        bottomEnd = 24.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 24.dp,
                        bottomStart = 24.dp,
                        bottomEnd = 24.dp
                    )
                },
                color = bubbleColor,
                contentColor = bubbleText,
                shadowElevation = if (message.isMine) 0.dp else 4.dp,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                    if (!message.isMine) {
                        Text(
                            message.sender,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaDark,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(5.dp))
                    }

                    if (isEmojiSticker) {
                        Text(
                            text = message.text.removePrefix(STICKER_EMOJI_PREFIX),
                            fontSize = 64.sp,
                            lineHeight = 72.sp
                        )
                    } else if (isImage) {
                        UriImage(
                            uri = Uri.parse(message.attachmentUri),
                            modifier = if (isImageSticker) {
                                Modifier.size(190.dp)
                            } else {
                                Modifier.width(240.dp).height(180.dp)
                            }
                        )
                        if (message.text.isNotBlank() && !isImageSticker) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                message.text,
                                color = bubbleText,
                                fontFamily = fontStyle.family,
                                fontSize = fontSize.sizeSp.sp
                            )
                        }
                    }

                    if (isFile) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !MessageSelectionController.isSelecting) {
                                    try {
                                        val uri = Uri.parse(message.attachmentUri)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "*/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (message.isMine) {
                                Color.White.copy(alpha = 0.16f)
                            } else {
                                CardCream
                            },
                            contentColor = bubbleText
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Text("📎 ", color = bubbleText)
                                Text(
                                    message.fileName.ifBlank { "Файл" },
                                    color = bubbleText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (message.text.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                message.text,
                                color = bubbleText,
                                fontFamily = fontStyle.family,
                                fontSize = fontSize.sizeSp.sp
                            )
                        }
                    }

                    if (isAudio) VoiceMessagePlayer(message, message.isMine)
                    if (isVideo) VideoCirclePlayer(message)

                    if (message.messageType == MessageType.TEXT.name && !isEmojiSticker) {
                        if (isPoll) {
                            PollMessageContent(
                                message = message,
                                textColor = bubbleText,
                                onVote = { option ->
                                    bubbleScope.launch {
                                        ChatRepository.sendMessage(
                                            context = context.applicationContext,
                                            chatId = message.chatId,
                                            text = "🗳 ${message.sender}: $option"
                                        )
                                    }
                                }
                            )
                        } else {
                            Text(
                                message.text,
                                color = bubbleText,
                                fontFamily = fontStyle.family,
                                fontSize = fontSize.sizeSp.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(modifier = Modifier.align(Alignment.End)) {
                        Text(
                            message.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.isMine) {
                                Color.White.copy(alpha = 0.78f)
                            } else {
                                BrownTextSecondary
                            }
                        )

                        if (message.isMine) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                when (message.status) {
                                    MessageStatus.SENDING.name -> "◷"
                                    MessageStatus.SENT.name -> "✓"
                                    MessageStatus.DELIVERED.name -> "✓✓"
                                    MessageStatus.READ.name -> "✓✓"
                                    MessageStatus.FAILED.name -> "!"
                                    else -> "✓"
                                },
                                color = when (message.status) {
                                    MessageStatus.READ.name -> Color.White
                                    MessageStatus.FAILED.name -> Color.White
                                    else -> Color.White.copy(alpha = 0.82f)
                                },
                                fontWeight = if (message.status == MessageStatus.READ.name) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PollMessageContent(
    message: Message,
    textColor: Color,
    onVote: (String) -> Unit
) {
    val lines = remember(message.text) { message.text.lines() }
    val question = lines.firstOrNull()
        ?.removePrefix("📊 ОПРОС:")
        ?.trim()
        .orEmpty()
    val options = remember(message.text) {
        lines.drop(1)
            .map { it.replace(Regex("^\\d+\\.\\s*"), "").trim() }
            .filter { it.isNotBlank() }
    }
    var selectedOption by remember(message.id) { mutableStateOf<String?>(null) }
    var voted by remember(message.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "📊 $question",
            color = textColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        options.forEach { option ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !voted) { selectedOption = option },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedOption == option) {
                    TerracottaLight
                } else {
                    CardCream.copy(alpha = 0.9f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == option,
                        enabled = !voted,
                        onClick = { selectedOption = option }
                    )
                    Text(option, color = BrownTextPrimary)
                }
            }
        }

        Button(
            onClick = {
                selectedOption?.let {
                    onVote(it)
                    voted = true
                }
            },
            enabled = selectedOption != null && !voted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (voted) "Голос отправлен" else "Проголосовать")
        }
    }
}

@Composable
private fun VoiceMessagePlayer(
    message: Message,
    isMine: Boolean
) {
    var isPlaying by remember(message.id) { mutableStateOf(false) }
    var player by remember(message.id) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(message.id) {
        onDispose {
            try { player?.release() } catch (_: Exception) {}
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable(enabled = !MessageSelectionController.isSelecting) {
                    if (isPlaying) {
                        try { player?.stop() } catch (_: Exception) {}
                        try { player?.release() } catch (_: Exception) {}
                        player = null
                        isPlaying = false
                    } else {
                        try {
                            val newPlayer = MediaPlayer()
                            newPlayer.setDataSource(message.attachmentUri)
                            newPlayer.prepare()
                            newPlayer.setOnCompletionListener {
                                try { it.release() } catch (_: Exception) {}
                                player = null
                                isPlaying = false
                            }
                            player = newPlayer
                            isPlaying = true
                            newPlayer.start()
                        } catch (_: Exception) {
                            player = null
                            isPlaying = false
                        }
                    }
                },
            shape = CircleShape,
            color = if (isMine) Color.White.copy(alpha = 0.18f) else TerracottaLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (isPlaying) "■" else "▶",
                    color = if (isMine) Color.White else TerracottaDark
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                "Голосовое сообщение",
                color = if (isMine) Color.White else BrownTextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                message.text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isMine) Color.White.copy(alpha = 0.75f) else BrownTextSecondary
            )
        }
    }
}

@Composable
private fun VideoCirclePlayer(message: Message) {
    var videoView by remember(message.id) { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember(message.id) { mutableStateOf(false) }

    DisposableEffect(message.id) {
        onDispose {
            try { videoView?.stopPlayback() } catch (_: Exception) {}
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .clickable(enabled = !MessageSelectionController.isSelecting) {
                    val view = videoView
                    if (view != null) {
                        if (view.isPlaying) {
                            view.pause()
                            isPlaying = false
                        } else {
                            view.start()
                            isPlaying = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    VideoView(it).apply {
                        videoView = this
                        setVideoPath(message.attachmentUri)
                        setOnPreparedListener { seekTo(1) }
                        setOnCompletionListener {
                            isPlaying = false
                            seekTo(1)
                        }
                    }
                },
                update = {
                    if (it.tag != message.attachmentUri) {
                        it.tag = message.attachmentUri
                        it.setVideoPath(message.attachmentUri)
                    }
                }
            )

            if (!isPlaying) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = CardWhite.copy(alpha = 0.88f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("▶", color = TerracottaPrimary)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            message.text,
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.76f)
        )
    }
}

@Composable
private fun UriImage(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val scheme = uri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") {
                    URL(uri.toString()).openStream().use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }
            } catch (error: Exception) {
                error.printStackTrace()
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            color = CardCream
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("📷")
            }
        }
    }
}

private fun takeReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: Exception) {}
}

private fun getFileName(context: Context, uri: Uri): String {
    var result = "Файл"
    context.contentResolver.query(uri, null, null, null, null)?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) {
            result = it.getString(index) ?: result
        }
    }
    return result
}
