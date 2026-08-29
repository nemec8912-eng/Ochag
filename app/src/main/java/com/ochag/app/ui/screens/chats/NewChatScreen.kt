package com.ochag.app.ui.screens.chats

import com.ochag.app.ui.localization.ochagText
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ochag.app.data.Chat
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.ChatType
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.PhoneContact
import com.ochag.app.data.PhoneContactsRepository
import com.ochag.app.data.ServerRealtime
import com.ochag.app.data.ServerUserProfile
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagPrimaryButton
import com.ochag.app.ui.components.OchagSectionHeader
import com.ochag.app.ui.components.OchagTextField
import com.ochag.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MemberSource {
    OCHAG,
    PHONE
}

private fun splitGluedParticipantName(value: String): List<String> {
    val clean = value.trim()
    if (clean.isBlank()) return emptyList()

    val result = mutableListOf<String>()
    val current = StringBuilder()

    clean.forEachIndexed { index, char ->
        val previous = if (index > 0) clean[index - 1] else null
        val shouldSplit = char.isUpperCase() && previous != null &&
                (previous.isLowerCase() || previous.isDigit())

        if (shouldSplit && current.isNotEmpty()) {
            result.add(current.toString().trim())
            current.clear()
        }
        current.append(char)
    }

    if (current.isNotEmpty()) result.add(current.toString().trim())

    val cleaned = result
        .map { it.trim(' ', ',', ';', '|', '/', '\\') }
        .filter { it.isNotBlank() }

    return if (cleaned.size > 1) cleaned else listOf(clean)
}

@Composable
fun NewChatScreen(
    onBackClick: () -> Unit,
    onChatCreated: (Chat) -> Unit
) {
    val uiContext = LocalContext.current
    val context = uiContext.applicationContext
    val coroutineScope = rememberCoroutineScope()

    val currentUserName = OchagAppState.currentUser?.name?.trim().orEmpty().ifBlank { ochagText("Михаил") }
    val serverUsers by ServerRealtime.serverUsers.collectAsState()
    val serverProfiles by ServerRealtime.serverUserProfiles.collectAsState()
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()

    var selectedType by remember { mutableStateOf(ChatType.PRIVATE) }
    var selectedSource by remember { mutableStateOf(MemberSource.OCHAG) }
    var title by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var groupAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val groupAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                uiContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            groupAvatarUri = uri
        }
    }

    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(uiContext, Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var phoneContacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    val selectedMembers = remember { mutableStateListOf<String>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsGranted = granted
        if (granted) {
            coroutineScope.launch {
                phoneContacts = withContext(Dispatchers.IO) {
                    PhoneContactsRepository.loadContacts(context)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        ServerRealtime.requestServerUsers()
        if (contactsGranted) {
            phoneContacts = withContext(Dispatchers.IO) {
                PhoneContactsRepository.loadContacts(context)
            }
        }
    }

    val availableMembers = remember(
        serverUsers,
        onlineUsers,
        ChatRepository.chats.size,
        currentUserName,
        searchText
    ) {
        val familyMembers = ChatRepository.findChat(1L)
            ?.members
            ?.flatMap { splitGluedParticipantName(it) }
            ?: emptyList()

        (serverUsers.flatMap { splitGluedParticipantName(it) } + familyMembers)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals(currentUserName, ignoreCase = true) }
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

    val filteredPhoneContacts = remember(phoneContacts, searchText) {
        phoneContacts.filter {
            searchText.isBlank() ||
                    it.name.contains(searchText.trim(), ignoreCase = true) ||
                    it.phone.contains(searchText.trim())
        }
    }

    fun serverProfileFor(contact: PhoneContact): ServerUserProfile? =
        serverProfiles.firstOrNull {
            it.normalizedPhone.isNotBlank() && it.normalizedPhone == contact.normalizedPhone
        }

    fun toggleMember(member: String) {
        if (selectedType == ChatType.PRIVATE) {
            selectedMembers.clear()
            selectedMembers.add(member)
        } else {
            val exists = selectedMembers.any { it.equals(member, ignoreCase = true) }
            if (exists) {
                selectedMembers.removeAll { it.equals(member, ignoreCase = true) }
            } else {
                selectedMembers.add(member)
            }
        }
        errorText = ""
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CreamBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onBackClick),
                    shape = CircleShape,
                    color = CardWhite,
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("←", color = BrownTextPrimary)
                    }
                }

                Spacer(Modifier.width(12.dp))

                OchagSectionHeader(
                    title = ochagText("Новый чат"),
                    subtitle = if (selectedType == ChatType.PRIVATE) {
                        ochagText("Выбери человека для личного диалога")
                    } else {
                        ochagText("Собери своё пространство для общения")
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            OchagCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypePill(
                        modifier = Modifier.weight(1f),
                        title = ochagText("Личный"),
                        selected = selectedType == ChatType.PRIVATE
                    ) {
                        selectedType = ChatType.PRIVATE
                        selectedMembers.clear()
                        errorText = ""
                    }
                    TypePill(
                        modifier = Modifier.weight(1f),
                        title = ochagText("Группа"),
                        selected = selectedType == ChatType.GROUP
                    ) {
                        selectedType = ChatType.GROUP
                        selectedMembers.clear()
                        errorText = ""
                    }
                    TypePill(
                        modifier = Modifier.weight(1f),
                        title = ochagText("Тема"),
                        selected = selectedType == ChatType.THEME
                    ) {
                        selectedType = ChatType.THEME
                        selectedMembers.clear()
                        errorText = ""
                    }
                }
            }

            if (selectedType != ChatType.PRIVATE) {
                Spacer(Modifier.height(12.dp))
                OchagTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = ""
                    },
                    label = if (selectedType == ChatType.GROUP) ochagText("Название группы") else ochagText("Название темы"),
                    placeholder = ochagText("Например, Поездка летом")
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { groupAvatarLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (groupAvatarUri == null) {
                            ochagText("Добавить аватарку группы")
                        } else {
                            ochagText("Изменить аватарку группы ✓")
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourcePill(
                    title = ochagText("В Очаге"),
                    selected = selectedSource == MemberSource.OCHAG
                ) {
                    selectedSource = MemberSource.OCHAG
                }

                SourcePill(
                    title = ochagText("Контакты"),
                    selected = selectedSource == MemberSource.PHONE
                ) {
                    selectedSource = MemberSource.PHONE
                    if (!contactsGranted) {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    } else {
                        coroutineScope.launch {
                            phoneContacts = withContext(Dispatchers.IO) {
                                PhoneContactsRepository.loadContacts(context)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OchagTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = if (selectedSource == MemberSource.PHONE) ochagText("Поиск по контактам") else ochagText("Поиск участника"),
                placeholder = ochagText("Имя или номер")
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMembers.isEmpty()) ochagText("Участники") else "Выбрано: ${selectedMembers.size}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )

                Text(
                    text = if (selectedSource == MemberSource.PHONE) {
                        ochagText("${filteredPhoneContacts.size} контактов")
                    } else {
                        ochagText("${availableMembers.size} доступно")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = BrownTextSecondary
                )
            }

            Spacer(Modifier.height(8.dp))

            if (selectedSource == MemberSource.OCHAG) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(availableMembers, key = { it.lowercase() }) { member ->
                        val selected = selectedMembers.any { it.equals(member, ignoreCase = true) }
                        val isOnline = onlineUsers.any { it.equals(member, ignoreCase = true) }

                        MemberCard(
                            name = member,
                            subtitle = if (isOnline) ochagText("В сети") else ochagText("Не в сети"),
                            online = isOnline,
                            selected = selected,
                            onClick = { toggleMember(member) }
                        )
                    }
                }
            } else {
                if (!contactsGranted) {
                    OchagCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column {
                            Text(
                                text = ochagText("Нужен доступ к контактам телефона"),
                                style = MaterialTheme.typography.titleMedium,
                                color = BrownTextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = ochagText("Разреши доступ, чтобы находить близких по номеру телефона."),
                                color = BrownTextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            OchagPrimaryButton(text = ochagText("Разрешить доступ")) {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(filteredPhoneContacts, key = { it.normalizedPhone }) { contact ->
                            val profile = serverProfileFor(contact)
                            val registered = profile != null
                            val memberName = profile?.name.orEmpty()
                            val selected = registered && selectedMembers.any {
                                it.equals(memberName, ignoreCase = true)
                            }

                            PhoneContactCard(
                                contact = contact,
                                registeredName = memberName,
                                selected = selected,
                                onSelect = {
                                    if (profile != null) toggleMember(profile.name)
                                },
                                onInvite = {
                                    PhoneContactsRepository.inviteBySms(context, contact)
                                }
                            )
                        }
                    }
                }
            }

            if (errorText.isNotBlank()) {
                Text(
                    text = errorText,
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            OchagPrimaryButton(
                text = when (selectedType) {
                    ChatType.PRIVATE -> ochagText("Открыть личный чат")
                    ChatType.GROUP -> ochagText("Создать группу")
                    ChatType.THEME -> ochagText("Создать тему")
                    ChatType.FAMILY -> ochagText("Создать чат")
                }
            ) {
                when (selectedType) {
                    ChatType.PRIVATE -> {
                        if (selectedMembers.isEmpty()) {
                            errorText = ochagText("Выберите пользователя «Очага»")
                            return@OchagPrimaryButton
                        }
                        val member = selectedMembers.first()
                        coroutineScope.launch {
                            val chat = ChatRepository.createPrivateChat(
                                context = context,
                                memberName = member
                            )
                            onChatCreated(chat)
                        }
                    }

                    ChatType.GROUP -> {
                        if (title.isBlank()) {
                            errorText = ochagText("Введите название группы")
                            return@OchagPrimaryButton
                        }
                        if (selectedMembers.isEmpty()) {
                            errorText = ochagText("Выберите участников")
                            return@OchagPrimaryButton
                        }
                        coroutineScope.launch {
                            val chat = ChatRepository.createCustomChat(
                                context = context,
                                title = title.trim(),
                                type = ChatType.GROUP,
                                selectedMembers = selectedMembers.toList()
                            )
                            groupAvatarUri?.let { uri ->
                                context.getSharedPreferences(
                                    "ochag_group_avatars",
                                    android.content.Context.MODE_PRIVATE
                                ).edit()
                                    .putString("group_avatar_${chat.id}", uri.toString())
                                    .apply()
                            }
                            onChatCreated(chat)
                        }
                    }

                    ChatType.THEME -> {
                        if (title.isBlank()) {
                            errorText = ochagText("Введите название темы")
                            return@OchagPrimaryButton
                        }
                        coroutineScope.launch {
                            val chat = ChatRepository.createCustomChat(
                                context = context,
                                title = title.trim(),
                                type = ChatType.THEME,
                                selectedMembers = selectedMembers.toList()
                            )
                            onChatCreated(chat)
                        }
                    }

                    ChatType.FAMILY -> Unit
                }
            }
        }
    }
}

@Composable
private fun TypePill(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) TerracottaPrimary else CreamBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (selected) Color.White else BrownTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SourcePill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) TerracottaSoft else CardWhite,
        shadowElevation = if (selected) 0.dp else 2.dp
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) TerracottaDark else BrownTextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun MemberCard(
    name: String,
    subtitle: String,
    online: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    OchagCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (selected) 2.dp else 4.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OchagAvatar(
                name = name,
                size = 50.dp,
                online = online,
                backgroundColor = if (selected) TerracottaSoft else SoftGreenLight
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (online) OnlineGreen else BrownTextSecondary
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TerracottaPrimary)
            )
        }
    }
}

@Composable
private fun PhoneContactCard(
    contact: PhoneContact,
    registeredName: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onInvite: () -> Unit
) {
    val registered = registeredName.isNotBlank()

    OchagCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OchagAvatar(
                name = contact.name,
                size = 50.dp,
                backgroundColor = SoftGreenLight
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrownTextSecondary
                )
                Text(
                    text = if (registered) "В Очаге: $registeredName" else ochagText("Ещё не в Очаге"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (registered) OnlineGreen else BrownTextSecondary
                )
            }

            if (registered) {
                RadioButton(
                    selected = selected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = TerracottaPrimary)
                )
            } else {
                TextButton(onClick = onInvite) {
                    Text(ochagText("Пригласить"), color = TerracottaDark)
                }
            }
        }
    }
}
