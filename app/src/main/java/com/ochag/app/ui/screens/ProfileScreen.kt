package com.ochag.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerAccountApi
import com.ochag.app.data.ServerRealtime


@Composable
fun ProfileScreen(
    onChatsClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onFamilyClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {

    val user =
        OchagAppState.currentUser

    val userName =
        user
            ?.name
            ?: "Гость"

    val profiles by
    ServerRealtime
        .serverUserProfiles
        .collectAsState()

    val connectionState by
    ServerRealtime
        .connectionState
        .collectAsState()

    val serverPhone =
        profiles
            .firstOrNull {
                it.name.equals(
                    userName,
                    ignoreCase = true
                )
            }
            ?.phone
            .orEmpty()

    var phone by remember(
        serverPhone
    ) {
        mutableStateOf(
            serverPhone
        )
    }

    var saveStatus by remember {
        mutableStateOf("")
    }

    var saving by remember {
        mutableStateOf(false)
    }


    LaunchedEffect(
        Unit
    ) {

        ServerRealtime
            .requestServerUsers()
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
                        18.dp
                    )
        ) {

            Text(
                text =
                    "Профиль",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        24.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                20.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Surface(
                        modifier =
                            Modifier.size(
                                92.dp
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
                                    userName
                                        .firstOrNull {
                                            it.isLetterOrDigit()
                                        }
                                        ?.uppercase()
                                        ?: "О",
                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineLarge,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    Text(
                        text =
                            userName,
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Text(
                        text =
                            when (
                                connectionState
                            ) {

                                ServerRealtime.ConnectionState.ONLINE ->
                                    "● В сети"

                                ServerRealtime.ConnectionState.CONNECTING ->
                                    "◌ Подключение..."

                                ServerRealtime.ConnectionState.OFFLINE ->
                                    "○ Не в сети"
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )


                    Text(
                        text =
                            "Номер для поиска по контактам",
                        modifier =
                            Modifier.fillMaxWidth(),
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )


                    OutlinedTextField(
                        value =
                            phone,
                        onValueChange = {
                            phone = it
                            saveStatus = ""
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        singleLine =
                            true,
                        label = {
                            Text(
                                "Телефон"
                            )
                        },
                        placeholder = {
                            Text(
                                "+7 999 123-45-67"
                            )
                        }
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),
                        enabled =
                            !saving &&
                                    userName !=
                                    "Гость",
                        onClick = {

                            saving =
                                true

                            saveStatus =
                                ""

                            ServerAccountApi
                                .updatePhone(
                                    userName =
                                        userName,
                                    phone =
                                        phone
                                ) {
                                        result ->

                                    saving =
                                        false

                                    saveStatus =
                                        if (
                                            result.success
                                        ) {
                                            "Номер сохранён"
                                        } else {
                                            result.error
                                        }

                                    if (
                                        result.success
                                    ) {

                                        ServerRealtime
                                            .requestServerUsers()
                                    }
                                }
                        }
                    ) {

                        Text(
                            if (
                                saving
                            ) {
                                "Сохраняю..."
                            } else {
                                "Сохранить номер"
                            }
                        )
                    }


                    if (
                        saveStatus.isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )

                        Text(
                            text =
                                saveStatus,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                if (
                                    saveStatus ==
                                    "Номер сохранён"
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                }
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            ProfileAction(
                title =
                    "Чаты",
                onClick =
                    onChatsClick
            )

            ProfileAction(
                title =
                    "Семья",
                onClick =
                    onFamilyClick
            )

            ProfileAction(
                title =
                    "Файлы",
                onClick =
                    onFilesClick
            )

            ProfileAction(
                title =
                    "Пригласить близких",
                onClick =
                    onInviteClick
            )


            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick =
                    onLogoutClick
            ) {

                Text(
                    "Выйти"
                )
            }
        }
    }
}


@Composable
private fun ProfileAction(
    title: String,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                ),
        onClick =
            onClick
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    title,
                modifier =
                    Modifier.weight(
                        1f
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                "›"
            )
        }
    }
}
