package com.ochag.app.ui.screens.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagBottomBar
import com.ochag.app.ui.localization.ochagText
import com.ochag.app.ui.theme.CreamBackground

@Composable
fun ContactsScreen(
    onChatsClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onContactClick: (String) -> Unit = {}
) {
    val profiles by ServerRealtime.serverUserProfiles.collectAsState()
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()
    var search by remember { mutableStateOf("") }

    val filtered = profiles.filter {
        search.isBlank() ||
            it.name.contains(search, ignoreCase = true) ||
            it.phone.contains(search, ignoreCase = true)
    }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            OchagBottomBar(
                items = listOf(
                    ochagText("Чаты"), ochagText("Контакты"), ochagText("Звонки"),
                    ochagText("Файлы"), ochagText("Профиль")
                ),
                selectedIndex = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) { index ->
                when (index) {
                    0 -> onChatsClick()
                    2 -> onCallsClick()
                    3 -> onFilesClick()
                    4 -> onProfileClick()
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Text(ochagText("Контакты"), style = MaterialTheme.typography.headlineLarge)
            Text(
                ochagText("Участники вашего семейного Очага"),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(ochagText("Поиск контакта")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ochagText("Контакты не найдены"))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.name }) { profile ->
                        val online = onlineUsers.any {
                            it.equals(profile.name, ignoreCase = true)
                        }
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactClick(profile.name) },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OchagAvatar(
                                    name = profile.name,
                                    size = 52.dp,
                                    online = online
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        profile.phone.ifBlank { ochagText("Номер телефона не указан") },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        if (online) ochagText("В сети") else ochagText("Не в сети"),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text("›", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        ServerRealtime.requestServerUsers()
    }
}
