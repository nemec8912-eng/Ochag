package com.ochag.app.ui.screens.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.Message
import com.ochag.app.data.MessageActions
import com.ochag.app.data.MessageType
import com.ochag.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatRoomWithMessageActions(
    chatId: Long,
    chatTitle: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val messages by ChatRepository.getMessages(context, chatId)
        .collectAsState(initial = emptyList())

    var showManager by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var deletingMessage by remember { mutableStateOf<Message?>(null) }
    var editText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatRoomScreen(
            chatId = chatId,
            chatTitle = chatTitle,
            onBackClick = onBackClick
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 62.dp)
                .size(40.dp)
                .clickable { showManager = true },
            shape = CircleShape,
            color = CardCream,
            shadowElevation = 3.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("✎", color = TerracottaDark, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showManager) {
        ModalBottomSheet(
            onDismissRequest = { showManager = false },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Мои сообщения",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrownTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Редактируй текст или удаляй отправленное",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrownTextSecondary
                )

                Spacer(Modifier.height(14.dp))

                val ownMessages = messages
                    .filter { it.isMine }
                    .sortedByDescending { it.createdAt }

                if (ownMessages.isEmpty()) {
                    Text(
                        "Пока нет отправленных сообщений",
                        modifier = Modifier.padding(vertical = 28.dp),
                        color = BrownTextSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(ownMessages, key = { it.id }) { message ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = CardCream
                            ) {
                                Row(
                                    modifier = Modifier.padding(13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = messagePreview(message),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = BrownTextPrimary
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = message.time,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BrownTextSecondary
                                        )
                                    }

                                    if (message.messageType == MessageType.TEXT.name &&
                                        !message.text.startsWith("__STICKER_EMOJI__:")) {
                                        TextButton(
                                            onClick = {
                                                editingMessage = message
                                                editText = message.text
                                                showManager = false
                                            }
                                        ) {
                                            Text("Изменить", color = TerracottaPrimary)
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            deletingMessage = message
                                            showManager = false
                                        }
                                    ) {
                                        Text("Удалить", color = TerracottaDark)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }

    editingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            containerColor = CardWhite,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Редактировать сообщение", color = BrownTextPrimary) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 6,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerracottaPrimary,
                        unfocusedBorderColor = BorderSoft,
                        focusedContainerColor = CardCream,
                        unfocusedContainerColor = CardCream,
                        focusedTextColor = BrownTextPrimary,
                        unfocusedTextColor = BrownTextPrimary,
                        cursorColor = TerracottaPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editText.isNotBlank(),
                    onClick = {
                        scope.launch {
                            MessageActions.editOwnTextMessage(
                                context = context,
                                message = message,
                                newText = editText
                            )
                            editingMessage = null
                        }
                    }
                ) {
                    Text("Сохранить", color = TerracottaPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Отмена", color = BrownTextSecondary)
                }
            }
        )
    }

    deletingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            containerColor = CardWhite,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Удалить сообщение?", color = BrownTextPrimary) },
            text = {
                Text(
                    "Сообщение исчезнет с этого устройства. Синхронизацию удаления для второго участника подключим следующим шагом.",
                    color = BrownTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            MessageActions.deleteOwnMessage(context, message)
                            deletingMessage = null
                        }
                    }
                ) {
                    Text("Удалить", color = TerracottaDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) {
                    Text("Отмена", color = BrownTextSecondary)
                }
            }
        )
    }
}

private fun messagePreview(message: Message): String = when (message.messageType) {
    MessageType.TEXT.name -> message.text.removePrefix("__STICKER_EMOJI__:")
    MessageType.IMAGE.name -> if (message.text == "__STICKER_IMAGE__") "Стикер" else "Фото ${message.text}".trim()
    MessageType.FILE.name -> message.fileName.ifBlank { "Файл" }
    MessageType.AUDIO.name -> "Голосовое сообщение"
    MessageType.VIDEO_CIRCLE.name -> "Видеокружок"
    else -> "Сообщение"
}
