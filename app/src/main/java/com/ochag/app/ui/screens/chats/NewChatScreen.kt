package com.ochag.app.ui.screens.chats

import android.Manifest
import android.content.pm.PackageManager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private enum class MemberSource {
    OCHAG,
    PHONE
}


private fun splitGluedParticipantName(
    value: String
): List<String> {

    val clean =
        value.trim()

    if (
        clean.isBlank()
    ) {
        return emptyList()
    }

    val result =
        mutableListOf<String>()

    val current =
        StringBuilder()

    clean.forEachIndexed {
            index,
            char ->

        val previous =
            if (
                index > 0
            ) {
                clean[index - 1]
            } else {
                null
            }

        val shouldSplit =
            char.isUpperCase() &&
                    previous != null &&
                    (
                            previous.isLowerCase() ||
                                    previous.isDigit()
                            )

        if (
            shouldSplit &&
            current.isNotEmpty()
        ) {

            result.add(
                current
                    .toString()
                    .trim()
            )

            current.clear()
        }

        current.append(
            char
        )
    }

    if (
        current.isNotEmpty()
    ) {

        result.add(
            current
                .toString()
                .trim()
        )
    }

    val cleaned =
        result
            .map {
                it.trim(
                    ' ',
                    ',',
                    ';',
                    '|',
                    '/',
                    '\\'
                )
            }
            .filter {
                it.isNotBlank()
            }

    return if (
        cleaned.size > 1
    ) {
        cleaned
    } else {
        listOf(
            clean
        )
    }
}


@Composable
fun NewChatScreen(
    onBackClick: () -> Unit,
    onChatCreated: (Chat) -> Unit
) {

    val uiContext =
        LocalContext.current

    val context =
        uiContext.applicationContext

    val coroutineScope =
        rememberCoroutineScope()

    val currentUserName =
        OchagAppState
            .currentUser
            ?.name
            ?.trim()
            .orEmpty()
            .ifBlank {
                "Михаил"
            }

    val serverUsers by
    ServerRealtime
        .serverUsers
        .collectAsState()

    val serverProfiles by
    ServerRealtime
        .serverUserProfiles
        .collectAsState()

    val onlineUsers by
    ServerRealtime
        .onlineUsers
        .collectAsState()

    var selectedType by remember {
        mutableStateOf(
            ChatType.PRIVATE
        )
    }

    var selectedSource by remember {
        mutableStateOf(
            MemberSource.OCHAG
        )
    }

    var title by remember {
        mutableStateOf("")
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var errorText by remember {
        mutableStateOf("")
    }

    var contactsGranted by remember {

        mutableStateOf(
            ContextCompat
                .checkSelfPermission(
                    uiContext,
                    Manifest.permission.READ_CONTACTS
                ) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var phoneContacts by remember {
        mutableStateOf<List<PhoneContact>>(
            emptyList()
        )
    }

    val selectedMembers =
        remember {
            mutableStateListOf<String>()
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) {
                granted ->

            contactsGranted =
                granted

            if (
                granted
            ) {

                coroutineScope.launch {

                    phoneContacts =
                        withContext(
                            Dispatchers.IO
                        ) {
                            PhoneContactsRepository
                                .loadContacts(
                                    context
                                )
                        }
                }
            }
        }


    LaunchedEffect(
        Unit
    ) {

        ServerRealtime
            .requestServerUsers()

        if (
            contactsGranted
        ) {

            phoneContacts =
                withContext(
                    Dispatchers.IO
                ) {
                    PhoneContactsRepository
                        .loadContacts(
                            context
                        )
                }
        }
    }


    val availableMembers =
        remember(
            serverUsers,
            onlineUsers,
            ChatRepository.chats.size,
            currentUserName,
            searchText
        ) {

            val familyMembers =
                ChatRepository
                    .findChat(
                        1L
                    )
                    ?.members
                    ?.flatMap {
                        splitGluedParticipantName(
                            it
                        )
                    }
                    ?: emptyList()

            (
                    serverUsers
                        .flatMap {
                            splitGluedParticipantName(
                                it
                            )
                        } +
                            familyMembers
                    )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank() &&
                            !it.equals(
                                currentUserName,
                                ignoreCase = true
                            )
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
                    compareByDescending<String> {
                            member ->

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


    val filteredPhoneContacts =
        remember(
            phoneContacts,
            searchText
        ) {

            phoneContacts
                .filter {
                    searchText.isBlank() ||
                            it.name.contains(
                                searchText.trim(),
                                ignoreCase = true
                            ) ||
                            it.phone.contains(
                                searchText.trim()
                            )
                }
        }


    fun serverProfileFor(
        contact: PhoneContact
    ): ServerUserProfile? {

        return serverProfiles
            .firstOrNull {

                it.normalizedPhone
                    .isNotBlank() &&
                        it.normalizedPhone ==
                        contact.normalizedPhone
            }
    }


    fun toggleMember(
        member: String
    ) {

        if (
            selectedType ==
            ChatType.PRIVATE
        ) {

            selectedMembers.clear()

            selectedMembers.add(
                member
            )

        } else {

            val exists =
                selectedMembers.any {
                    it.equals(
                        member,
                        ignoreCase = true
                    )
                }

            if (
                exists
            ) {

                selectedMembers
                    .removeAll {
                        it.equals(
                            member,
                            ignoreCase = true
                        )
                    }

            } else {

                selectedMembers.add(
                    member
                )
            }
        }

        errorText = ""
    }


    Surface(
        modifier =
            Modifier.fillMaxSize(),
        color =
            MaterialTheme
                .colorScheme
                .background
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(
                    onClick =
                        onBackClick
                ) {

                    Text(
                        "← Назад"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            6.dp
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
                            "Новый чат",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            if (
                                selectedType ==
                                ChatType.PRIVATE
                            ) {
                                "Выберите человека"
                            } else {
                                "Соберите свой круг"
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
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                FilterChip(
                    selected =
                        selectedType ==
                                ChatType.PRIVATE,
                    onClick = {
                        selectedType =
                            ChatType.PRIVATE
                        selectedMembers.clear()
                        errorText = ""
                    },
                    label = {
                        Text(
                            "Личный"
                        )
                    }
                )


                FilterChip(
                    selected =
                        selectedType ==
                                ChatType.GROUP,
                    onClick = {
                        selectedType =
                            ChatType.GROUP
                        selectedMembers.clear()
                        errorText = ""
                    },
                    label = {
                        Text(
                            "Группа"
                        )
                    }
                )


                FilterChip(
                    selected =
                        selectedType ==
                                ChatType.THEME,
                    onClick = {
                        selectedType =
                            ChatType.THEME
                        selectedMembers.clear()
                        errorText = ""
                    },
                    label = {
                        Text(
                            "Тема"
                        )
                    }
                )
            }


            if (
                selectedType !=
                ChatType.PRIVATE
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )


                OutlinedTextField(
                    value =
                        title,
                    onValueChange = {
                        title = it
                        errorText = ""
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (
                                selectedType ==
                                ChatType.GROUP
                            ) {
                                "Название группы"
                            } else {
                                "Название темы"
                            }
                        )
                    },
                    singleLine =
                        true,
                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                FilterChip(
                    selected =
                        selectedSource ==
                                MemberSource.OCHAG,
                    onClick = {
                        selectedSource =
                            MemberSource.OCHAG
                    },
                    label = {
                        Text(
                            "В Очаге"
                        )
                    }
                )


                FilterChip(
                    selected =
                        selectedSource ==
                                MemberSource.PHONE,
                    onClick = {

                        selectedSource =
                            MemberSource.PHONE

                        if (
                            !contactsGranted
                        ) {

                            permissionLauncher
                                .launch(
                                    Manifest.permission.READ_CONTACTS
                                )

                        } else {

                            coroutineScope.launch {

                                phoneContacts =
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        PhoneContactsRepository
                                            .loadContacts(
                                                context
                                            )
                                    }
                            }
                        }
                    },
                    label = {
                        Text(
                            "Контакты телефона"
                        )
                    }
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
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
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                label = {
                    Text(
                        if (
                            selectedSource ==
                            MemberSource.PHONE
                        ) {
                            "Поиск по контактам"
                        } else {
                            "Поиск участника"
                        }
                    )
                }
            )


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        if (
                            selectedMembers.isEmpty()
                        ) {
                            "Участники"
                        } else {
                            "Выбрано: ${selectedMembers.size}"
                        },
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )

                if (
                    selectedSource ==
                    MemberSource.PHONE
                ) {

                    Text(
                        text =
                            "${filteredPhoneContacts.size} контактов",
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                } else {

                    Text(
                        text =
                            "${availableMembers.size} доступно",
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            if (
                selectedSource ==
                MemberSource.OCHAG
            ) {

                LazyColumn(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        ),
                    contentPadding =
                        PaddingValues(
                            bottom = 12.dp
                        )
                ) {

                    items(
                        items =
                            availableMembers,
                        key = {
                            it.lowercase()
                        }
                    ) {
                            member ->

                        val selected =
                            selectedMembers.any {
                                it.equals(
                                    member,
                                    ignoreCase = true
                                )
                            }

                        val isOnline =
                            onlineUsers.any {
                                it.equals(
                                    member,
                                    ignoreCase = true
                                )
                            }

                        MemberCard(
                            name =
                                member,
                            subtitle =
                                if (
                                    isOnline
                                ) {
                                    "● В сети"
                                } else {
                                    "○ Не в сети"
                                },
                            selected =
                                selected,
                            onClick = {
                                toggleMember(
                                    member
                                )
                            }
                        )
                    }
                }

            } else {

                if (
                    !contactsGranted
                ) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    16.dp
                                )
                        ) {

                            Text(
                                "Нужен доступ к контактам телефона."
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            Button(
                                onClick = {
                                    permissionLauncher
                                        .launch(
                                            Manifest.permission.READ_CONTACTS
                                        )
                                }
                            ) {

                                Text(
                                    "Разрешить доступ"
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                } else {

                    LazyColumn(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            ),
                        contentPadding =
                            PaddingValues(
                                bottom = 12.dp
                            )
                    ) {

                        items(
                            items =
                                filteredPhoneContacts,
                            key = {
                                it.normalizedPhone
                            }
                        ) {
                                contact ->

                            val profile =
                                serverProfileFor(
                                    contact
                                )

                            val registered =
                                profile != null

                            val memberName =
                                profile
                                    ?.name
                                    .orEmpty()

                            val selected =
                                registered &&
                                        selectedMembers.any {
                                            it.equals(
                                                memberName,
                                                ignoreCase = true
                                            )
                                        }

                            PhoneContactCard(
                                contact =
                                    contact,
                                registeredName =
                                    profile
                                        ?.name
                                        .orEmpty(),
                                selected =
                                    selected,
                                onSelect = {

                                    if (
                                        profile != null
                                    ) {

                                        toggleMember(
                                            profile.name
                                        )
                                    }
                                },
                                onInvite = {

                                    PhoneContactsRepository
                                        .inviteBySms(
                                            context,
                                            contact
                                        )
                                }
                            )
                        }
                    }
                }
            }


            if (
                errorText.isNotBlank()
            ) {

                Text(
                    text =
                        errorText,
                    modifier =
                        Modifier.padding(
                            vertical = 6.dp
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }


            Button(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            56.dp
                        ),
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                onClick = {

                    when (
                        selectedType
                    ) {

                        ChatType.PRIVATE -> {

                            if (
                                selectedMembers
                                    .isEmpty()
                            ) {

                                errorText =
                                    "Выберите пользователя «Очага»"

                                return@Button
                            }

                            val member =
                                selectedMembers
                                    .first()

                            coroutineScope.launch {

                                val chat =
                                    ChatRepository
                                        .createPrivateChat(
                                            context =
                                                context,
                                            memberName =
                                                member
                                        )

                                onChatCreated(
                                    chat
                                )
                            }
                        }


                        ChatType.GROUP -> {

                            if (
                                title.isBlank()
                            ) {

                                errorText =
                                    "Введите название группы"

                                return@Button
                            }

                            if (
                                selectedMembers
                                    .isEmpty()
                            ) {

                                errorText =
                                    "Выберите участников"

                                return@Button
                            }

                            coroutineScope.launch {

                                val chat =
                                    ChatRepository
                                        .createCustomChat(
                                            context =
                                                context,
                                            title =
                                                title.trim(),
                                            type =
                                                ChatType.GROUP,
                                            selectedMembers =
                                                selectedMembers
                                                    .toList()
                                        )

                                onChatCreated(
                                    chat
                                )
                            }
                        }


                        ChatType.THEME -> {

                            if (
                                title.isBlank()
                            ) {

                                errorText =
                                    "Введите название темы"

                                return@Button
                            }

                            coroutineScope.launch {

                                val chat =
                                    ChatRepository
                                        .createCustomChat(
                                            context =
                                                context,
                                            title =
                                                title.trim(),
                                            type =
                                                ChatType.THEME,
                                            selectedMembers =
                                                selectedMembers
                                                    .toList()
                                        )

                                onChatCreated(
                                    chat
                                )
                            }
                        }


                        ChatType.FAMILY -> {
                        }
                    }
                }
            ) {

                Text(
                    text =
                        when (
                            selectedType
                        ) {

                            ChatType.PRIVATE ->
                                "Открыть личный чат"

                            ChatType.GROUP ->
                                "Создать группу"

                            ChatType.THEME ->
                                "Создать тему"

                            ChatType.FAMILY ->
                                "Создать чат"
                        }
                )
            }
        }
    }
}


@Composable
private fun MemberCard(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        if (
                            selected
                        ) {
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        } else {
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                        }
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            ContactAvatar(
                name =
                    name
            )


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
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
                        name,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis,
                    fontWeight =
                        FontWeight.Medium
                )

                Text(
                    text =
                        subtitle,
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
                onCheckedChange = {
                    onClick()
                }
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

    val registered =
        registeredName.isNotBlank()

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        if (
                            selected
                        ) {
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        } else {
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                        }
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            ContactAvatar(
                name =
                    contact.name
            )


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
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
                        contact.name,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis,
                    fontWeight =
                        FontWeight.Medium
                )

                Text(
                    text =
                        contact.phone,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        if (
                            registered
                        ) {
                            "✓ В Очаге: $registeredName"
                        } else {
                            "Ещё не в Очаге"
                        },
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        if (
                            registered
                        ) {
                            MaterialTheme
                                .colorScheme
                                .primary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }
                )
            }


            if (
                registered
            ) {

                Checkbox(
                    checked =
                        selected,
                    onCheckedChange = {
                        onSelect()
                    }
                )

            } else {

                TextButton(
                    onClick =
                        onInvite
                ) {

                    Text(
                        "Пригласить"
                    )
                }
            }
        }
    }
}


@Composable
private fun ContactAvatar(
    name: String
) {

    Box(
        modifier =
            Modifier
                .size(
                    46.dp
                )
                .clip(
                    CircleShape
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .secondaryContainer
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                name
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
