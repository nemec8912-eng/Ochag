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
import androidx.compose.ui.unit.dp
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.Message
import com.ochag.app.data.MessageActions
import com.ochag.app.data.MessageType
import com.ochag.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    val selectedIds = MessageSelectionController.selectedIds
    val actionMessage = MessageSelectionController.actionMessage

    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var deletingMessage by remember { mutableStateOf<Message?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val pinPreferences = remember(chatId) {
        context.getSharedPreferences("ochag_pinned_messages", android.content.Context.MODE_PRIVATE)
    }
    var pinnedMessageIds by remember(chatId) {
        mutableStateOf(
            pinPreferences.getStringSet("pinned_$chatId", emptySet())
                .orEmpty()
                .mapNotNull { it.toLongOrNull() }
                .toSet()
        )
    }
    var showPinnedMessages by remember { mutableStateOf(false) }
    val selectedForUnpin = remember { mutableStateListOf<Long>() }
    val pinnedMessages = messages.filter { pinnedMessageIds.contains(it.id) }
    val latestPinnedMessage = pinnedMessages.maxByOrNull { it.createdAt }

    fun savePinned(ids: Set<Long>) {
        pinnedMessageIds = ids
        pinPreferences.edit()
            .putStringSet("pinned_$chatId", ids.map { it.toString() }.toSet())
            .apply()
    }

    DisposableEffect(chatId) {
        MessageSelectionController.attach(chatId)
        onDispose { MessageSelectionController.detach(chatId) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatRoomScreen(
            chatId = chatId,
            chatTitle = chatTitle,
            onBackClick = {
                if (selectedIds.isNotEmpty()) {
                    MessageSelectionController.clear()
                } else {
                    onBackClick()
                }
            }
        )

        latestPinnedMessage?.let { pinned ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 78.dp, start = 14.dp, end = 14.dp)
                    .fillMaxWidth()
                    .clickable { showPinnedMessages = true },
                shape = RoundedCornerShape(16.dp),
                color = TerracottaLight,
                shadowElevation = 5.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📌", modifier = Modifier.padding(end = 8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Закреплено сообщений: ${pinnedMessages.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TerracottaDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            messagePreview(pinned),
                            maxLines = 1,
                            color = BrownTextPrimary
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        if (selectedIds.isNotEmpty()) {
            SelectionToolbar(
                count = selectedIds.size,
                allSelected = messages.isNotEmpty() && selectedIds.size == messages.size,
                onClose = { MessageSelectionController.clear() },
                onSelectAll = {
                    if (messages.isNotEmpty() && selectedIds.size == messages.size) {
                        MessageSelectionController.clear()
                    } else {
                        MessageSelectionController.selectAll(messages)
                    }
                },
                onDelete = { confirmBulkDelete = true },
                onDeleteAll = { confirmDeleteAll = true }
            )
        }
    }

    if (showPinnedMessages) {
        AlertDialog(
            onDismissRequest = {
                selectedForUnpin.clear()
                showPinnedMessages = false
            },
            title = { Text("Закреплённые сообщения (${pinnedMessages.size})") },
            text = {
                if (pinnedMessages.isEmpty()) {
                    Text("Закреплённых сообщений нет")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pinnedMessages, key = { it.id }) { pinned ->
                            val selected = selectedForUnpin.contains(pinned.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        MessageSelectionController.requestScrollTo(pinned.id)
                                        showPinnedMessages = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = CardCream
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = {
                                            if (selected) selectedForUnpin.remove(pinned.id)
                                            else selectedForUnpin.add(pinned.id)
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            messagePreview(pinned),
                                            maxLines = 2,
                                            color = BrownTextPrimary
                                        )
                                        Text(
                                            "${pinned.sender} • ${pinned.time}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrownTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedForUnpin.isNotEmpty(),
                    onClick = {
                        savePinned(pinnedMessageIds - selectedForUnpin.toSet())
                        selectedForUnpin.clear()
                        if (pinnedMessageIds.isEmpty()) showPinnedMessages = false
                    }
                ) { Text("Открепить выбранные") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedForUnpin.clear()
                        showPinnedMessages = false
                    }
                ) { Text("Закрыть") }
            }
        )
    }

    actionMessage?.let { message ->
        ModalBottomSheet(
            onDismissRequest = { MessageSelectionController.closeActions() },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = messagePreview(message),
                    style = MaterialTheme.typography.titleMedium,
                    color = BrownTextPrimary,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (message.isMine) "Ваше сообщение" else "Сообщение от ${message.sender}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrownTextSecondary
                )

                Spacer(Modifier.height(16.dp))

                if (message.isMine &&
                    message.messageType == MessageType.TEXT.name &&
                    !message.text.startsWith("__STICKER_EMOJI__:")) {
                    ActionRow(
                        icon = "✎",
                        title = "Редактировать"
                    ) {
                        editingMessage = message
                        editText = message.text
                        MessageSelectionController.closeActions()
                    }
                }

                ActionRow(
                    icon = "✓",
                    title = "Выделить"
                ) {
                    MessageSelectionController.startSelection(message.id)
                }

                ActionRow(
                    icon = "📌",
                    title = if (pinnedMessageIds.contains(message.id)) {
                        "Уже закреплено"
                    } else {
                        "Закрепить в чате"
                    }
                ) {
                    savePinned(pinnedMessageIds + message.id)
                    MessageSelectionController.closeActions()
                }

                ActionRow(
                    icon = "⌫",
                    title = "Удалить",
                    destructive = true
                ) {
                    deletingMessage = message
                    MessageSelectionController.closeActions()
                }

                Spacer(Modifier.height(8.dp))
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
                    if (message.isMine) {
                        "Сообщение будет удалено на этом устройстве. Серверное удаление у второго участника подключим отдельно."
                    } else {
                        "Полученное сообщение будет скрыто на этом устройстве."
                    },
                    color = BrownTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            MessageActions.deleteLocalMessage(context, message)
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

    if (confirmBulkDelete) {
        val selectedMessages = messages.filter { selectedIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            containerColor = CardWhite,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Удалить выбранные?", color = BrownTextPrimary) },
            text = {
                Text(
                    "Будет удалено сообщений: ${selectedMessages.size}.",
                    color = BrownTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            MessageActions.deleteLocalMessages(context, selectedMessages)
                            MessageSelectionController.clear()
                            confirmBulkDelete = false
                        }
                    }
                ) {
                    Text("Удалить", color = TerracottaDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }) {
                    Text("Отмена", color = BrownTextSecondary)
                }
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            containerColor = CardWhite,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Удалить все сообщения?", color = BrownTextPrimary) },
            text = {
                Text(
                    "Вся история этого чата будет удалена с этого устройства.",
                    color = BrownTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            MessageActions.deleteAllLocalMessages(context, chatId)
                            MessageSelectionController.clear()
                            confirmDeleteAll = false
                        }
                    }
                ) {
                    Text("Удалить все", color = TerracottaDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text("Отмена", color = BrownTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SelectionToolbar(
    count: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onClose),
                shape = CircleShape,
                color = CardCream
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("×", color = BrownTextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Выбрано: $count",
                    color = BrownTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Нажимай на другие сообщения",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrownTextSecondary
                )
            }

            TextButton(onClick = onSelectAll) {
                Text(if (allSelected) "Снять все" else "Все", color = TerracottaPrimary)
            }

            TextButton(onClick = onDeleteAll) {
                Text("Очистить", color = BrownTextSecondary)
            }

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clickable(onClick = onDelete),
                shape = CircleShape,
                color = TerracottaSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⌫", color = TerracottaDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: String,
    title: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = CardCream
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge,
                color = if (destructive) TerracottaDark else BrownTextPrimary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (destructive) TerracottaDark else BrownTextPrimary
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

private fun messagePreview(message: Message): String = when (message.messageType) {
    MessageType.TEXT.name -> message.text.removePrefix("__STICKER_EMOJI__:")
    MessageType.IMAGE.name -> if (message.text == "__STICKER_IMAGE__") "Стикер" else "Фото ${message.text}".trim()
    MessageType.FILE.name -> message.fileName.ifBlank { "Файл" }
    MessageType.AUDIO.name -> "Голосовое сообщение"
    MessageType.VIDEO_CIRCLE.name -> "Видеокружок"
    else -> "Сообщение"
}
