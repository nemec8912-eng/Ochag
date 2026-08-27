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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

import com.ochag.app.R
import com.ochag.app.data.Chat
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.ChatType
import com.ochag.app.data.Message
import com.ochag.app.data.MessageType
import com.ochag.app.data.ServerRealtime
import com.ochag.app.data.MessageStatus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean


private enum class MediaButtonMode {
    AUDIO,
    VIDEO
}


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
    onBackClick: () -> Unit = {}
) {

    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = context as LifecycleOwner
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showChatSettings by remember {
        mutableStateOf(false)
    }


    val onlineUsers by
    ServerRealtime
        .onlineUsers
        .collectAsState()


    val typingUsers by
    ServerRealtime
        .typingUsers
        .collectAsState()

    val typingNames =
        typingUsers[
            chatId
        ]
            .orEmpty()
            .filter {
                !it.equals(
                    ServerRealtime.userName,
                    ignoreCase = true
                )
            }

    val currentChat =
        ChatRepository.findChat(
            chatId
        )

    val headerStatusText =
        remember(
            onlineUsers,
            typingNames,
            currentChat,
            chatId
        ) {

            if (
                typingNames.isNotEmpty()
            ) {

                if (
                    typingNames.size == 1
                ) {
                    "${typingNames.first()} печатает…"
                } else {
                    "${typingNames.size} печатают…"
                }

            } else {

                buildChatHeaderPresenceText(
                    chat =
                        currentChat,
                    onlineUsers =
                        onlineUsers
                )
            }
        }

    var messageText by remember {
        mutableStateOf("")
    }


    LaunchedEffect(
        chatId,
        messageText
    ) {

        if (
            messageText.isBlank()
        ) {

            ServerRealtime
                .sendTyping(
                    chatId =
                        chatId,
                    isTyping =
                        false
                )

            return@LaunchedEffect
        }

        ServerRealtime
            .sendTyping(
                chatId =
                    chatId,
                isTyping =
                    true
            )

        delay(
            1200L
        )

        ServerRealtime
            .sendTyping(
                chatId =
                    chatId,
                isTyping =
                    false
            )
    }

    var showAttachmentMenu by remember {
        mutableStateOf(false)
    }

    var pendingAttachment by remember {
        mutableStateOf<PendingAttachment?>(null)
    }

    var mediaButtonMode by remember {
        mutableStateOf(MediaButtonMode.AUDIO)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // РАЗРЕШЕНИЯ
    // =========================================================

    var microphoneGranted by remember {

        mutableStateOf(

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }


    var cameraGranted by remember {

        mutableStateOf(

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }


    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            result[Manifest.permission.RECORD_AUDIO]?.let {
                microphoneGranted = it
            }

            result[Manifest.permission.CAMERA]?.let {
                cameraGranted = it
            }
        }


    // =========================================================
    // АУДИО
    // =========================================================

    var audioRecorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    var audioFile by remember {
        mutableStateOf<File?>(null)
    }

    var audioStartedAt by remember {
        mutableLongStateOf(0L)
    }


    // =========================================================
    // ВИДЕО
    // =========================================================

    var videoCapture by remember {
        mutableStateOf<VideoCapture<Recorder>?>(null)
    }

    var activeVideoRecording by remember {
        mutableStateOf<ActiveVideoRecording?>(null)
    }

    var useFrontCamera by remember {
        mutableStateOf(true)
    }


    // =========================================================
    // СООБЩЕНИЯ
    // =========================================================

    val messages by ChatRepository
        .getMessages(
            context = appContext,
            chatId = chatId
        )
        .collectAsState(
            initial = emptyList()
        )


    val serverUsers by
    ServerRealtime
        .serverUsers
        .collectAsState()



    // =========================================================
    // ПРОЧИТАНО: КОГДА ЧАТ ОТКРЫТ, СООБЩАЕМ СЕРВЕРУ
    // О ВХОДЯЩИХ СООБЩЕНИЯХ
    // =========================================================

    LaunchedEffect(
        chatId,
        messages.map { it.id to it.isMine }
    ) {

        val incomingIds =
            messages
                .filter { !it.isMine }
                .map { it.id }
                .filter { it > 0L }
                .distinct()

        if (incomingIds.isNotEmpty()) {

            ServerRealtime.sendReadReceipt(
                chatId = chatId,
                messageIds = incomingIds
            )
        }
    }


    // =========================================================
    // ВЫБОР ФОТО
    // =========================================================

    val imageLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                takeReadPermission(
                    context,
                    uri
                )

                pendingAttachment =
                    PendingAttachment(
                        uri = uri,
                        fileName = getFileName(
                            context,
                            uri
                        ),
                        type = MessageType.IMAGE
                    )
            }
        }


    // =========================================================
    // ВЫБОР ФАЙЛА
    // =========================================================

    val fileLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                takeReadPermission(
                    context,
                    uri
                )

                pendingAttachment =
                    PendingAttachment(
                        uri = uri,
                        fileName = getFileName(
                            context,
                            uri
                        ),
                        type = MessageType.FILE
                    )
            }
        }


    // =========================================================
    // НАЧАЛЬНЫЕ СООБЩЕНИЯ
    // =========================================================

    LaunchedEffect(chatId) {

        ChatRepository.seedInitialMessages(
            context = appContext,
            chatId = chatId
        )
    }


    // =========================================================
    // ПРОКРУТКА ВНИЗ
    // =========================================================

    LaunchedEffect(messages.size) {

        if (messages.isNotEmpty()) {

            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }


    // =========================================================
    // СТАРТ АУДИО
    // =========================================================

    fun startAudioRecording(): Boolean {

        if (!microphoneGranted) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                )
            )

            return false
        }


        return try {

            val directory =
                File(
                    appContext.filesDir,
                    "voice_messages"
                )

            directory.mkdirs()


            val file =
                File(
                    directory,
                    "voice_${System.currentTimeMillis()}.m4a"
                )


            @Suppress("DEPRECATION")
            val newRecorder =

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {

                    MediaRecorder(appContext)

                } else {

                    MediaRecorder()
                }


            newRecorder.apply {

                setAudioSource(
                    MediaRecorder.AudioSource.MIC
                )

                setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
                )

                setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
                )

                setAudioEncodingBitRate(
                    128000
                )

                setAudioSamplingRate(
                    44100
                )

                setOutputFile(
                    file.absolutePath
                )

                prepare()

                start()
            }


            audioRecorder = newRecorder
            audioFile = file

            audioStartedAt =
                System.currentTimeMillis()

            isRecording = true

            true


        } catch (_: Exception) {

            try {
                audioRecorder?.release()
            } catch (_: Exception) {
            }

            audioFile?.delete()

            audioRecorder = null
            audioFile = null
            isRecording = false

            false
        }
    }


    // =========================================================
    // СТОП АУДИО
    // =========================================================

    fun finishAudioRecording(
        send: Boolean
    ) {

        val recorder = audioRecorder
        val file = audioFile
        val startedAt = audioStartedAt

        var valid = true


        try {

            recorder?.stop()

        } catch (_: Exception) {

            valid = false
        }


        try {

            recorder?.release()

        } catch (_: Exception) {
        }


        audioRecorder = null
        audioFile = null
        audioStartedAt = 0L
        isRecording = false


        if (file == null) {
            return
        }


        val duration =
            System.currentTimeMillis() -
                    startedAt


        if (
            !send ||
            !valid ||
            duration < 500L
        ) {

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


    // =========================================================
    // СТАРТ ВИДЕО
    // =========================================================

    fun startVideoRecording(): Boolean {

        if (
            !cameraGranted ||
            !microphoneGranted
        ) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )

            return false
        }


        val capture =
            videoCapture
                ?: return false


        return try {

            val directory =
                File(
                    appContext.filesDir,
                    "video_circles"
                )

            directory.mkdirs()


            val file =
                File(
                    directory,
                    "circle_${System.currentTimeMillis()}.mp4"
                )


            val outputOptions =
                FileOutputOptions
                    .Builder(file)
                    .build()


            val shouldSend =
                AtomicBoolean(false)


            val pendingRecording =
                capture
                    .output
                    .prepareRecording(
                        appContext,
                        outputOptions
                    )
                    .withAudioEnabled()


            val recording =
                pendingRecording.start(

                    ContextCompat.getMainExecutor(
                        appContext
                    )

                ) { event ->


                    when (event) {

                        is VideoRecordEvent.Start -> {

                            isRecording = true
                        }


                        is VideoRecordEvent.Finalize -> {

                            isRecording = false


                            val durationMs =
                                (
                                        event
                                            .recordingStats
                                            .recordedDurationNanos /
                                                1_000_000L
                                        )
                                    .coerceAtLeast(1L)


                            val canSend =
                                shouldSend.get() &&
                                        !event.hasError() &&
                                        file.exists() &&
                                        durationMs >= 500L


                            activeVideoRecording = null


                            if (canSend) {

                                coroutineScope.launch {

                                    ChatRepository
                                        .sendVideoCircleMessage(
                                            context = appContext,
                                            chatId = chatId,
                                            videoPath =
                                                file.absolutePath,
                                            durationMs =
                                                durationMs
                                        )
                                }

                            } else {

                                file.delete()
                            }
                        }
                    }
                }


            activeVideoRecording =
                ActiveVideoRecording(
                    recording = recording,
                    file = file,
                    shouldSend = shouldSend
                )


            true


        } catch (_: Exception) {

            isRecording = false

            false
        }
    }


    // =========================================================
    // СТОП ВИДЕО
    // =========================================================

    fun finishVideoRecording(
        send: Boolean
    ) {

        val active =
            activeVideoRecording
                ?: return


        active.shouldSend.set(send)


        try {

            active.recording.stop()

        } catch (_: Exception) {

            active.file.delete()

            activeVideoRecording = null

            isRecording = false
        }
    }


    // =========================================================
    // ОСВОБОЖДЕНИЕ РЕСУРСОВ
    // =========================================================

    DisposableEffect(Unit) {

        onDispose {

            try {

                audioRecorder?.stop()

            } catch (_: Exception) {
            }


            try {

                audioRecorder?.release()

            } catch (_: Exception) {
            }


            audioFile?.delete()


            activeVideoRecording
                ?.shouldSend
                ?.set(false)


            try {

                activeVideoRecording
                    ?.recording
                    ?.stop()

            } catch (_: Exception) {
            }
        }
    }


    // =========================================================
    // GROUP SETTINGS
    // =========================================================

    if (
        showChatSettings &&
        currentChat != null &&
        currentChat.type !=
        ChatType.PRIVATE
    ) {

        ChatGroupSettingsDialog(
            chat =
                currentChat,
            serverUsers =
                serverUsers,
            onlineUsers =
                onlineUsers,
            onDismiss = {
                showChatSettings =
                    false
            },
            onSave = {
                    newTitle,
                    newMembers ->

                coroutineScope.launch {

                    ChatRepository
                        .updateChatDetails(
                            context =
                                appContext,
                            chatId =
                                chatId,
                            title =
                                newTitle,
                            members =
                                newMembers
                        )

                    showChatSettings =
                        false
                }
            }
        )
    }


    // =========================================================
    // UI
    // =========================================================

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {


        // =====================================================
        // ШАПКА
        // =====================================================

        ChatHeader(
            chatTitle =
                currentChat
                    ?.title
                    ?: chatTitle,
            statusText =
                headerStatusText,
            canOpenSettings =
                currentChat != null &&
                        currentChat.type !=
                        ChatType.PRIVATE,
            onSettingsClick = {

                ServerRealtime
                    .requestServerUsers()

                showChatSettings =
                    true
            },
            onBackClick =
                onBackClick
        )


        // =====================================================
        // СООБЩЕНИЯ
        // =====================================================

        LazyColumn(
            state = listState,

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp
                    ),

            contentPadding =
                PaddingValues(
                    top = 12.dp,
                    bottom = 12.dp
                )
        ) {

            items(
                items = messages,
                key = { it.id }
            ) { message ->

                MessageBubble(
                    message = message
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )
            }
        }


        // =====================================================
        // КРУГЛАЯ КАМЕРА
        // =====================================================

        if (
            mediaButtonMode ==
            MediaButtonMode.VIDEO &&
            cameraGranted
        ) {

            CameraCirclePreview(
                context = context,
                lifecycleOwner =
                    lifecycleOwner,
                useFrontCamera =
                    useFrontCamera,
                isRecording =
                    isRecording,

                onCaptureReady = {

                    videoCapture = it
                },

                onSwitchCamera = {

                    if (!isRecording) {

                        useFrontCamera =
                            !useFrontCamera
                    }
                }
            )
        }


        // =====================================================
        // СТАТУС ЗАПИСИ
        // =====================================================

        if (isRecording) {

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        ),

                shape =
                    RoundedCornerShape(16.dp),

                color =
                    MaterialTheme
                        .colorScheme
                        .errorContainer
            ) {

                Text(
                    text =

                        if (
                            mediaButtonMode ==
                            MediaButtonMode.AUDIO
                        ) {

                            "● Записывается голосовое..."

                        } else {

                            "● Записывается видеокружок..."
                        },

                    modifier =
                        Modifier.padding(12.dp),

                    color =
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer
                )
            }
        }


        // =====================================================
        // ПРЕДПРОСМОТР ВЛОЖЕНИЯ
        // =====================================================

        pendingAttachment?.let {

            PendingAttachmentPreview(
                attachment = it,

                onRemove = {

                    pendingAttachment = null
                }
            )
        }


        // =====================================================
        // НИЖНЯЯ ПАНЕЛЬ
        // =====================================================

        Surface(
            modifier =
                Modifier.fillMaxWidth(),

            tonalElevation = 6.dp
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),

                verticalAlignment =
                    Alignment.Bottom
            ) {


                // =================================================
                // КНОПКА +
                // =================================================

                Box {

                    FilledTonalButton(
                        modifier =
                            Modifier.size(52.dp),

                        contentPadding =
                            PaddingValues(0.dp),

                        shape =
                            CircleShape,

                        onClick = {

                            showAttachmentMenu = true
                        }
                    ) {

                        Text("＋")
                    }


                    DropdownMenu(
                        expanded =
                            showAttachmentMenu,

                        onDismissRequest = {

                            showAttachmentMenu = false
                        }
                    ) {


                        DropdownMenuItem(
                            text = {

                                Text("📷 Фото")
                            },

                            onClick = {

                                showAttachmentMenu = false

                                imageLauncher.launch(
                                    arrayOf("image/*")
                                )
                            }
                        )


                        DropdownMenuItem(
                            text = {

                                Text("📎 Файл")
                            },

                            onClick = {

                                showAttachmentMenu = false

                                fileLauncher.launch(
                                    arrayOf("*/*")
                                )
                            }
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                // =================================================
                // ПОЛЕ ВВОДА
                // =================================================

                OutlinedTextField(
                    value = messageText,

                    onValueChange = {

                        messageText = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    placeholder = {

                        Text(

                            if (
                                pendingAttachment != null
                            ) {

                                "Добавить подпись..."

                            } else {

                                "Сообщение..."
                            }
                        )
                    },

                    shape =
                        RoundedCornerShape(22.dp),

                    maxLines = 4
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                // =================================================
                // КНОПКА ОТПРАВКИ
                // =================================================

                if (
                    messageText.isNotBlank() ||
                    pendingAttachment != null
                ) {

                    Button(
                        modifier =
                            Modifier.size(52.dp),

                        contentPadding =
                            PaddingValues(0.dp),

                        shape =
                            CircleShape,

                        onClick = {

                            val text =
                                messageText.trim()

                            val attachment =
                                pendingAttachment


                            messageText = ""
                            pendingAttachment = null


                            coroutineScope.launch {

                                if (
                                    attachment != null
                                ) {

                                    ChatRepository
                                        .sendAttachment(
                                            context =
                                                appContext,

                                            chatId =
                                                chatId,

                                            uri =
                                                attachment
                                                    .uri
                                                    .toString(),

                                            fileName =
                                                attachment
                                                    .fileName,

                                            messageType =
                                                attachment
                                                    .type,

                                            caption =
                                                text
                                        )

                                } else {

                                    ChatRepository
                                        .sendMessage(
                                            context =
                                                appContext,

                                            chatId =
                                                chatId,

                                            text =
                                                text
                                        )
                                }
                            }
                        }
                    ) {

                        Text("➤")
                    }

                } else {


                    // =================================================
                    // КНОПКА АУДИО / ВИДЕО
                    // =================================================

                    Surface(
                        modifier =
                            Modifier
                                .size(52.dp)
                                .pointerInput(
                                    mediaButtonMode,
                                    microphoneGranted,
                                    cameraGranted,
                                    videoCapture
                                ) {

                                    awaitEachGesture {

                                        awaitFirstDown(
                                            requireUnconsumed =
                                                false
                                        )


                                        // =================================
                                        // КОРОТКОЕ НАЖАТИЕ
                                        // =================================

                                        val quickRelease =
                                            withTimeoutOrNull(
                                                220L
                                            ) {

                                                waitForUpOrCancellation()
                                            }


                                        if (
                                            quickRelease != null
                                        ) {

                                            mediaButtonMode =

                                                if (
                                                    mediaButtonMode ==
                                                    MediaButtonMode.AUDIO
                                                ) {

                                                    MediaButtonMode.VIDEO

                                                } else {

                                                    MediaButtonMode.AUDIO
                                                }


                                            if (
                                                mediaButtonMode ==
                                                MediaButtonMode.VIDEO &&
                                                (
                                                        !cameraGranted ||
                                                                !microphoneGranted
                                                        )
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


                                        // =================================
                                        // УДЕРЖИВАНИЕ
                                        // =================================

                                        val started =

                                            when (
                                                mediaButtonMode
                                            ) {

                                                MediaButtonMode.AUDIO -> {

                                                    startAudioRecording()
                                                }

                                                MediaButtonMode.VIDEO -> {

                                                    startVideoRecording()
                                                }
                                            }


                                        if (!started) {

                                            waitForUpOrCancellation()

                                            return@awaitEachGesture
                                        }


                                        val released =
                                            waitForUpOrCancellation()


                                        val send =
                                            released != null


                                        when (
                                            mediaButtonMode
                                        ) {

                                            MediaButtonMode.AUDIO -> {

                                                finishAudioRecording(
                                                    send
                                                )
                                            }

                                            MediaButtonMode.VIDEO -> {

                                                finishVideoRecording(
                                                    send
                                                )
                                            }
                                        }
                                    }
                                },

                        shape =
                            CircleShape,

                        color =

                            if (isRecording) {

                                MaterialTheme
                                    .colorScheme
                                    .error

                            } else {

                                MaterialTheme
                                    .colorScheme
                                    .primary
                            }
                    ) {


                        Box(
                            modifier =
                                Modifier.fillMaxSize(),

                            contentAlignment =
                                Alignment.Center
                        ) {


                            // =========================================
                            // ВЫБРАННЫЕ ИКОНКИ ОЧАГ
                            // =========================================

                            if (isRecording) {

                                Text(
                                    text = "●",

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary
                                )

                            } else {

                                Icon(
                                    painter =
                                        painterResource(
                                            id =

                                                if (
                                                    mediaButtonMode ==
                                                    MediaButtonMode.AUDIO
                                                ) {

                                                    R.drawable.ic_audio_retro

                                                } else {

                                                    R.drawable.ic_video_camera
                                                }
                                        ),

                                    contentDescription =

                                        if (
                                            mediaButtonMode ==
                                            MediaButtonMode.AUDIO
                                        ) {

                                            "Аудио"

                                        } else {

                                            "Видео"
                                        },

                                    modifier =
                                        Modifier.size(28.dp),

                                    tint =
                                        androidx.compose.ui.graphics.Color.Unspecified
                                )
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
    onDismiss: () -> Unit,
    onSave: (
        String,
        List<String>
    ) -> Unit
) {

    var title by remember(
        chat.id,
        chat.title
    ) {
        mutableStateOf(
            chat.title
        )
    }

    var searchText by remember {
        mutableStateOf("")
    }

    val selectedMembers =
        remember(
            chat.id,
            chat.members
        ) {
            mutableStateListOf<String>()
                .apply {
                    addAll(
                        chat.members
                    )
                }
        }

    val myName =
        ServerRealtime
            .userName
            .trim()

    val availableMembers =
        remember(
            serverUsers,
            chat.members,
            searchText,
            onlineUsers
        ) {
            (
                    serverUsers +
                            chat.members
                    )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinctBy {
                    it.lowercase()
                }
                .filter {
                    searchText.isBlank() ||
                            it.contains(
                                searchText.trim(),
                                ignoreCase = true
                            )
                }
                .sortedWith(
                    compareByDescending<String> { member ->
                        onlineUsers.any {
                            it.equals(
                                member,
                                ignoreCase = true
                            )
                        }
                    }
                        .thenBy {
                            it.lowercase()
                        }
                )
        }


    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {

            Text(
                text =
                    when (
                        chat.type
                    ) {
                        ChatType.FAMILY ->
                            "Семья"

                        ChatType.GROUP ->
                            "Настройки группы"

                        ChatType.THEME ->
                            "Настройки темы"

                        ChatType.PRIVATE ->
                            "Чат"
                    }
            )
        },
        text = {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 560.dp
                        )
            ) {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true,
                    label = {
                        Text(
                            "Название"
                        )
                    }
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                Text(
                    text =
                        "${selectedMembers.size} участников",
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                OutlinedTextField(
                    value =
                        searchText,
                    onValueChange = {
                        searchText = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true,
                    label = {
                        Text(
                            "Найти участника"
                        )
                    }
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max = 320.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {

                    items(
                        items =
                            availableMembers,
                        key = {
                            it.lowercase()
                        }
                    ) { member ->

                        val selected =
                            selectedMembers.any {
                                it.equals(
                                    member,
                                    ignoreCase = true
                                )
                            }

                        val isMe =
                            member.equals(
                                myName,
                                ignoreCase = true
                            )

                        val isOnline =
                            onlineUsers.any {
                                it.equals(
                                    member,
                                    ignoreCase = true
                                )
                            }


                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            10.dp
                                        ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Surface(
                                    modifier =
                                        Modifier.size(
                                            38.dp
                                        ),
                                    shape =
                                        CircleShape,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primaryContainer
                                ) {

                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Text(
                                            text =
                                                member
                                                    .firstOrNull {
                                                        it.isLetterOrDigit()
                                                    }
                                                    ?.uppercase()
                                                    ?: "О",
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                                }


                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            10.dp
                                        )
                                )


                                Column(
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                ) {

                                    Text(
                                        text =
                                            if (
                                                isMe
                                            ) {
                                                "$member • вы"
                                            } else {
                                                member
                                            },
                                        maxLines =
                                            1
                                    )

                                    Text(
                                        text =
                                            if (
                                                isOnline
                                            ) {
                                                "● В сети"
                                            } else {
                                                "○ Не в сети"
                                            },
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }


                                Checkbox(
                                    checked =
                                        selected,
                                    enabled =
                                        !isMe,
                                    onCheckedChange = {

                                        if (
                                            isMe
                                        ) {
                                            return@Checkbox
                                        }

                                        if (
                                            selected
                                        ) {
                                            selectedMembers
                                                .removeAll {
                                                    it.equals(
                                                        member,
                                                        ignoreCase = true
                                                    )
                                                }

                                        } else {
                                            selectedMembers
                                                .add(
                                                    member
                                                )
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

                    val cleanTitle =
                        title
                            .trim()

                    if (
                        cleanTitle.isNotBlank()
                    ) {

                        val finalMembers =
                            selectedMembers
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

                        if (
                            myName.isNotBlank() &&
                            finalMembers.none {
                                it.equals(
                                    myName,
                                    ignoreCase = true
                                )
                            }
                        ) {
                            finalMembers.add(
                                0,
                                myName
                            )
                        }

                        onSave(
                            cleanTitle,
                            finalMembers
                        )
                    }
                }
            ) {

                Text(
                    "Сохранить"
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Отмена"
                )
            }
        }
    )
}


private fun buildChatHeaderPresenceText(
    chat: com.ochag.app.data.Chat?,
    onlineUsers: Set<String>
): String {

    if (chat == null) {
        return if (
            ServerRealtime.isConnected
        ) {
            "● Подключено"
        } else {
            "○ Нет соединения"
        }
    }

    if (
        chat.type ==
        ChatType.PRIVATE
    ) {

        val myName =
            ServerRealtime
                .userName
                .trim()

        val otherMember =
            chat
                .members
                .firstOrNull {
                    !it.equals(
                        myName,
                        ignoreCase = true
                    )
                }

        val isOnline =
            otherMember != null &&
                    onlineUsers.any {
                        it.equals(
                            otherMember,
                            ignoreCase = true
                        )
                    }

        return if (
            isOnline
        ) {
            "● В сети"
        } else {
            "○ Не в сети"
        }
    }

    val cleanMembers =
        chat
            .members
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinctBy {
                it.lowercase()
            }

    val onlineCount =
        cleanMembers.count {
                member ->
            onlineUsers.any {
                it.equals(
                    member,
                    ignoreCase = true
                )
            }
        }

    return if (
        cleanMembers.isNotEmpty()
    ) {
        "$onlineCount/${cleanMembers.size} в сети"
    } else {
        "0 в сети"
    }
}


// ============================================================
// ШАПКА ЧАТА
// ============================================================

@Composable
private fun ChatHeader(
    chatTitle: String,
    statusText: String,
    canOpenSettings: Boolean,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        tonalElevation = 4.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 10.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick =
                    onBackClick
            ) {

                Text("←")
            }


            Surface(
                modifier =
                    Modifier.size(44.dp),

                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            chatTitle
                                .firstOrNull()
                                ?.uppercase()
                                ?: "О",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            Spacer(
                Modifier.width(12.dp)
            )


            Column(
                modifier =
                    if (
                        canOpenSettings
                    ) {
                        Modifier.clickable {
                            onSettingsClick()
                        }
                    } else {
                        Modifier
                    }
            ) {

                Text(
                    text = chatTitle,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Text(
                    text = statusText,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }
    }
}


// ============================================================
// КРУГЛАЯ КАМЕРА
// ============================================================

@Composable
private fun CameraCirclePreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    useFrontCamera: Boolean,
    isRecording: Boolean,
    onCaptureReady: (VideoCapture<Recorder>) -> Unit,
    onSwitchCamera: () -> Unit
) {

    val previewView =
        remember {

            PreviewView(context).apply {

                scaleType =
                    PreviewView
                        .ScaleType
                        .FILL_CENTER
            }
        }


    DisposableEffect(
        useFrontCamera
    ) {

        val future =
            ProcessCameraProvider
                .getInstance(context)


        var provider:
                ProcessCameraProvider? =
            null


        future.addListener(

            {

                try {

                    provider =
                        future.get()


                    val preview =
                        Preview
                            .Builder()
                            .build()


                    preview.setSurfaceProvider(
                        previewView
                            .surfaceProvider
                    )


                    val recorder =
                        Recorder
                            .Builder()
                            .setQualitySelector(

                                QualitySelector.from(
                                    Quality.SD
                                )
                            )
                            .build()


                    val capture =
                        VideoCapture
                            .withOutput(recorder)


                    val selector =

                        if (useFrontCamera) {

                            CameraSelector
                                .DEFAULT_FRONT_CAMERA

                        } else {

                            CameraSelector
                                .DEFAULT_BACK_CAMERA
                        }


                    provider?.unbindAll()


                    provider?.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture
                    )


                    onCaptureReady(
                        capture
                    )


                } catch (_: Exception) {
                }
            },

            ContextCompat
                .getMainExecutor(context)
        )


        onDispose {

            try {

                provider?.unbindAll()

            } catch (_: Exception) {
            }
        }
    }


    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 8.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Box(
            modifier =
                Modifier.size(220.dp)
        ) {


            AndroidView(
                factory = {

                    previewView
                },

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
            )


            FilledTonalButton(
                onClick =
                    onSwitchCamera,

                enabled =
                    !isRecording,

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomEnd
                        )
                        .size(48.dp),

                contentPadding =
                    PaddingValues(0.dp),

                shape =
                    CircleShape
            ) {

                Text("↻")
            }


            if (isRecording) {

                Surface(
                    modifier =
                        Modifier
                            .align(
                                Alignment.TopCenter
                            )
                            .padding(8.dp),

                    shape =
                        RoundedCornerShape(20.dp),

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                ) {

                    Text(
                        text = "● REC",

                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            ),

                        color =
                            MaterialTheme
                                .colorScheme
                                .onError
                    )
                }
            }
        }
    }
}


// ============================================================
// ПРЕДПРОСМОТР ВЛОЖЕНИЯ
// ============================================================

@Composable
private fun PendingAttachmentPreview(
    attachment: PendingAttachment,
    onRemove: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(10.dp),

        shape =
            RoundedCornerShape(16.dp),

        tonalElevation = 2.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            if (
                attachment.type ==
                MessageType.IMAGE
            ) {

                UriImage(
                    uri =
                        attachment.uri,

                    modifier =
                        Modifier.size(70.dp)
                )


                Spacer(
                    Modifier.width(12.dp)
                )
            }


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    if (
                        attachment.type ==
                        MessageType.IMAGE
                    ) {

                        "📷 Фото"

                    } else {

                        "📎 Файл"
                    }
                )


                Text(
                    attachment
                        .fileName
                        .ifBlank {

                            "Без имени"
                        }
                )
            }


            TextButton(
                onClick =
                    onRemove
            ) {

                Text("✕")
            }
        }
    }
}


// ============================================================
// СООБЩЕНИЕ
// ============================================================

@Composable
fun MessageBubble(
    message: Message
) {

    val context =
        LocalContext.current


    val isImage =
        message.messageType ==
                MessageType.IMAGE.name


    val isFile =
        message.messageType ==
                MessageType.FILE.name


    val isAudio =
        message.messageType ==
                MessageType.AUDIO.name


    val isVideo =
        message.messageType ==
                MessageType.VIDEO_CIRCLE.name


    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =

            if (message.isMine) {

                Arrangement.End

            } else {

                Arrangement.Start
            },

        verticalAlignment =
            Alignment.Bottom
    ) {


        if (!message.isMine) {

            Surface(
                modifier =
                    Modifier.size(34.dp),

                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .secondaryContainer
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        message
                            .sender
                            .firstOrNull()
                            ?.uppercase()
                            ?: "?"
                    )
                }
            }


            Spacer(
                Modifier.width(6.dp)
            )
        }


        Surface(
            modifier =
                Modifier.widthIn(
                    min = 80.dp,
                    max = 290.dp
                ),

            shape =
                RoundedCornerShape(18.dp),

            color =

                if (message.isMine) {

                    MaterialTheme
                        .colorScheme
                        .primaryContainer

                } else {

                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                }
        ) {

            Column(
                modifier =
                    Modifier.padding(10.dp)
            ) {


                if (!message.isMine) {

                    Text(
                        text =
                            message.sender,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        Modifier.height(4.dp)
                    )
                }


                // =================================================
                // ФОТО
                // =================================================

                if (isImage) {

                    UriImage(
                        uri =
                            Uri.parse(
                                message.attachmentUri
                            ),

                        modifier =
                            Modifier
                                .width(240.dp)
                                .height(180.dp)
                    )


                    if (
                        message.text.isNotBlank()
                    ) {

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            message.text
                        )
                    }
                }


                // =================================================
                // ФАЙЛ
                // =================================================

                if (isFile) {

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    try {

                                        val uri =
                                            Uri.parse(
                                                message
                                                    .attachmentUri
                                            )


                                        val intent =
                                            Intent(
                                                Intent.ACTION_VIEW
                                            ).apply {

                                                setDataAndType(
                                                    uri,
                                                    "*/*"
                                                )

                                                addFlags(
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                )
                                            }


                                        context.startActivity(
                                            intent
                                        )


                                    } catch (_: Exception) {
                                    }
                                },

                        shape =
                            RoundedCornerShape(12.dp)
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(12.dp)
                        ) {

                            Text("📎 ")

                            Text(
                                message
                                    .fileName
                                    .ifBlank {

                                        "Файл"
                                    }
                            )
                        }
                    }


                    if (
                        message.text.isNotBlank()
                    ) {

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            message.text
                        )
                    }
                }


                // =================================================
                // ГОЛОСОВОЕ
                // =================================================

                if (isAudio) {

                    VoiceMessagePlayer(
                        message = message
                    )
                }


                // =================================================
                // ВИДЕОКРУЖОК
                // =================================================

                if (isVideo) {

                    VideoCirclePlayer(
                        message = message
                    )
                }


                // =================================================
                // ТЕКСТ
                // =================================================

                if (
                    message.messageType ==
                    MessageType.TEXT.name
                ) {

                    Text(
                        message.text
                    )
                }


                Spacer(
                    Modifier.height(6.dp)
                )


                Row(
                    modifier =
                        Modifier.align(
                            Alignment.End
                        )
                ) {

                    Text(
                        text =
                            message.time,

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )


                    if (message.isMine) {

                        Spacer(
                            Modifier.width(4.dp)
                        )

                        Text(
                            text =
                                when (message.status) {

                                    MessageStatus.SENDING.name ->
                                        "◷"

                                    MessageStatus.SENT.name ->
                                        "✓"

                                    MessageStatus.DELIVERED.name ->
                                        "✓✓"

                                    MessageStatus.READ.name ->
                                        "✓✓"

                                    MessageStatus.FAILED.name ->
                                        "!"

                                    else ->
                                        "✓"
                                },
                            color =
                                when (message.status) {

                                    MessageStatus.READ.name ->
                                        MaterialTheme.colorScheme.primary

                                    MessageStatus.FAILED.name ->
                                        MaterialTheme.colorScheme.error

                                    else ->
                                        LocalContentColor.current
                                }
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// ГОЛОСОВОЕ
// ============================================================

@Composable
private fun VoiceMessagePlayer(
    message: Message
) {

    var isPlaying by remember(
        message.id
    ) {

        mutableStateOf(false)
    }


    var player by remember(
        message.id
    ) {

        mutableStateOf<MediaPlayer?>(null)
    }


    DisposableEffect(
        message.id
    ) {

        onDispose {

            try {

                player?.release()

            } catch (_: Exception) {
            }
        }
    }


    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        FilledTonalButton(
            modifier =
                Modifier.size(44.dp),

            contentPadding =
                PaddingValues(0.dp),

            shape =
                CircleShape,

            onClick = {

                if (isPlaying) {

                    try {

                        player?.stop()

                    } catch (_: Exception) {
                    }


                    try {

                        player?.release()

                    } catch (_: Exception) {
                    }


                    player = null
                    isPlaying = false


                } else {

                    try {

                        val newPlayer =
                            MediaPlayer()


                        newPlayer.setDataSource(
                            message.attachmentUri
                        )


                        newPlayer.prepare()


                        newPlayer
                            .setOnCompletionListener {

                                try {

                                    it.release()

                                } catch (_: Exception) {
                                }


                                player = null
                                isPlaying = false
                            }


                        player =
                            newPlayer


                        isPlaying =
                            true


                        newPlayer.start()


                    } catch (_: Exception) {

                        player = null
                        isPlaying = false
                    }
                }
            }
        ) {

            Text(

                if (isPlaying) {

                    "■"

                } else {

                    "▶"
                }
            )
        }


        Spacer(
            Modifier.width(10.dp)
        )


        Column {

            Text(
                "🎤 Голосовое"
            )


            Text(
                text =
                    message.text,

                style =
                    MaterialTheme
                        .typography
                        .labelSmall
            )
        }
    }
}


// ============================================================
// ВИДЕОКРУЖОК
// ============================================================

@Composable
private fun VideoCirclePlayer(
    message: Message
) {

    var videoView by remember(
        message.id
    ) {

        mutableStateOf<VideoView?>(null)
    }


    var isPlaying by remember(
        message.id
    ) {

        mutableStateOf(false)
    }


    DisposableEffect(
        message.id
    ) {

        onDispose {

            try {

                videoView?.stopPlayback()

            } catch (_: Exception) {
            }
        }
    }


    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Box(
            modifier =
                Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .clickable {

                        val view =
                            videoView


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

            contentAlignment =
                Alignment.Center
        ) {


            AndroidView(
                modifier =
                    Modifier.fillMaxSize(),

                factory = {

                    VideoView(it).apply {

                        videoView = this


                        setVideoPath(
                            message.attachmentUri
                        )


                        setOnPreparedListener {

                            seekTo(1)
                        }


                        setOnCompletionListener {

                            isPlaying = false

                            seekTo(1)
                        }
                    }
                },

                update = {

                    if (
                        it.tag !=
                        message.attachmentUri
                    ) {

                        it.tag =
                            message.attachmentUri


                        it.setVideoPath(
                            message.attachmentUri
                        )
                    }
                }
            )


            if (!isPlaying) {

                Surface(
                    modifier =
                        Modifier.size(54.dp),

                    shape =
                        CircleShape,

                    color =
                        MaterialTheme
                            .colorScheme
                            .surface
                            .copy(
                                alpha = 0.8f
                            )
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text("▶")
                    }
                }
            }
        }


        Spacer(
            Modifier.height(4.dp)
        )


        Text(
            text =
                "📹 ${message.text}",

            style =
                MaterialTheme
                    .typography
                    .labelSmall
        )
    }
}


// ============================================================
// ФОТО
// ============================================================

@Composable
private fun UriImage(
    uri: Uri,
    modifier: Modifier = Modifier
) {

    val context =
        LocalContext.current


    val bitmap by
    produceState<ImageBitmap?>(
        initialValue = null,
        uri
    ) {

        value =
            withContext(
                Dispatchers.IO
            ) {

                try {

                    val scheme =
                        uri.scheme
                            ?.lowercase()


                    if (
                        scheme == "http" ||
                        scheme == "https"
                    ) {

                        URL(
                            uri.toString()
                        )
                            .openStream()
                            .use { input ->

                                BitmapFactory
                                    .decodeStream(
                                        input
                                    )
                                    ?.asImageBitmap()
                            }

                    } else {

                        context
                            .contentResolver
                            .openInputStream(
                                uri
                            )
                            ?.use { input ->

                                BitmapFactory
                                    .decodeStream(
                                        input
                                    )
                                    ?.asImageBitmap()
                            }
                    }


                } catch (
                    error: Exception
                ) {

                    error.printStackTrace()

                    null
                }
            }
    }


    if (
        bitmap != null
    ) {

        Image(
            bitmap =
                bitmap!!,

            contentDescription =
                null,

            modifier =
                modifier
                    .clip(
                        RoundedCornerShape(
                            14.dp
                        )
                    ),

            contentScale =
                ContentScale.Crop
        )

    } else {

        Surface(
            modifier =
                modifier,

            shape =
                RoundedCornerShape(
                    14.dp
                ),

            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
        ) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "📷"
                )
            }
        }
    }
}


// ============================================================
// СОХРАНЕНИЕ ДОСТУПА К URI
// ============================================================

private fun takeReadPermission(
    context: Context,
    uri: Uri
) {

    try {

        context
            .contentResolver
            .takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

    } catch (_: Exception) {
    }
}


// ============================================================
// ИМЯ ФАЙЛА
// ============================================================

private fun getFileName(
    context: Context,
    uri: Uri
): String {

    var result =
        "Файл"


    context
        .contentResolver
        .query(
            uri,
            null,
            null,
            null,
            null
        )
        ?.use {

            val index =
                it.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )


            if (
                index >= 0 &&
                it.moveToFirst()
            ) {

                result =
                    it.getString(index)
                        ?: result
            }
        }


    return result
}
