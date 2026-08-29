package com.ochag.app.ui.screens.chats

import com.ochag.app.ui.localization.ochagText
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
    onKeeperClick: () -> Unit = {},
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
            onKeeperClick = onKeeperClick,
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
                            ochagText("Закреплено сообщений: ${pinnedMessages.size}"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TerracottaDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            messagePreview(pinned),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
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
            title = { Text(ochagText("Закреплённые сообщения (${pinnedMessages.size})")) },
            text = {
                if (pinnedMessages.isEmpty()) {
                    Text(ochagText("Закреплённых сообщений нет"))
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
                                color = MaterialTheme.colorScheme.surfaceVariant
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
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${pinned.sender} • ${pinned.time}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                ) { Text(ochagText("Открепить выбранные")) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedForUnpin.clear()
                        showPinnedMessages = false
                    }
                ) { Text(ochagText("Закрыть")) }
            }
        )
    }

    actionMessage?.let { message ->
        ModalBottomSheet(
            onDismissRequest = { MessageSelectionController.closeActions() },
            containerColor = MaterialTheme.colorScheme.surface,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (message.isMine) ochagText("Ваше сообщение") else ochagText("Сообщение от ${message.sender}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                if (message.isMine &&
                    message.messageType == MessageType.TEXT.name &&
                    !message.text.startsWith("__STICKER_EMOJI__:")) {
                    ActionRow(
                        icon = "✎",
                        title = ochagText("Редактировать")
                    ) {
                        editingMessage = message
                        editText = message.text
                        MessageSelectionController.closeActions()
                    }
                }

                ActionRow(
                    icon = "✓",
                    title = ochagText("Выделить")
                ) {
                    MessageSelectionController.startSelection(message.id)
                }

                ActionRow(
                    icon = "📌",
                    title = if (pinnedMessageIds.contains(message.id)) {
                        ochagText("Уже закреплено")
                    } else {
                        ochagText("Закрепить в чате")
                    }
                ) {
                    savePinned(pinnedMessageIds + message.id)
                    MessageSelectionController.closeActions()
                }

                ActionRow(
                    icon = "⌫",
                    title = ochagText("Удалить"),
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
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text(ochagText("Редактировать сообщение"), color = MaterialTheme.colorScheme.onSurface) },
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
                    Text(ochagText("Сохранить"), color = TerracottaPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text(ochagText("Отмена"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    deletingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text(ochagText("Удалить сообщение?"), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    if (message.isMine) {
                        ochagText("Сообщение будет удалено на этом устройстве. Серверное удаление у второго участника подключим отдельно.")
                    } else {
                        ochagText("Полученное сообщение будет скрыто на этом устройстве.")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(ochagText("Удалить"), color = TerracottaDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) {
                    Text(ochagText("Отмена"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (confirmBulkDelete) {
        val selectedMessages = messages.filter { selectedIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text(ochagText("Удалить выбранные?"), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    ochagText("Будет удалено сообщений: ${selectedMessages.size}."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(ochagText("Удалить"), color = TerracottaDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }) {
                    Text(ochagText("Отмена"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text(ochagText("Удалить все сообщения?"), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    ochagText("Вся история этого чата будет удалена с этого устройства."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(ochagText("Удалить все"), color = TerracottaDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text(ochagText("Отмена"), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        color = MaterialTheme.colorScheme.surface,
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
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("×", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ochagText("Выбрано: $count"),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ochagText("Нажимай на другие сообщения"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onSelectAll) {
                Text(if (allSelected) ochagText("Снять все") else ochagText("Все"), color = TerracottaPrimary)
            }

            TextButton(onClick = onDeleteAll) {
                Text(ochagText("Очистить"), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge,
                color = if (destructive) TerracottaDark else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (destructive) TerracottaDark else MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

private fun messagePreview(message: Message): String = when (message.messageType) {
    MessageType.TEXT.name -> message.text.removePrefix("__STICKER_EMOJI__:")
    MessageType.IMAGE.name -> if (message.text == "__STICKER_IMAGE__") "Стикер" else ochagText("Фото ${message.text}").trim()
    MessageType.FILE.name -> message.fileName.ifBlank { "Файл" }
    MessageType.AUDIO.name -> "Голосовое сообщение"
    MessageType.VIDEO_CIRCLE.name -> "Видеокружок"
    else -> "Сообщение"
}
