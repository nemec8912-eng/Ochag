package com.ochag.app.ui.screens.calls

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagBottomBar
import com.ochag.app.ui.localization.ochagText
import com.ochag.app.ui.theme.CreamBackground

@Composable
fun CallsScreen(
    onChatsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val profiles by ServerRealtime.serverUserProfiles.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            OchagBottomBar(
                items = listOf(
                    ochagText("Чаты"), ochagText("Контакты"), ochagText("Звонки"),
                    ochagText("Файлы"), ochagText("Профиль")
                ),
                selectedIndex = 2,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) { index ->
                when (index) {
                    0 -> onChatsClick()
                    1 -> onContactsClick()
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
            Text(ochagText("Звонки"), style = MaterialTheme.typography.headlineLarge)
            Text(
                ochagText("Выберите близкого человека для звонка"),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(18.dp))

            if (message != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(text = message!!, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(10.dp))
            }

            if (profiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ochagText("Список контактов пока пуст"))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(profiles, key = { it.name }) { profile ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OchagAvatar(name = profile.name, size = 48.dp, online = true)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        profile.phone.ifBlank { ochagText("Номер телефона не указан") },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (profile.phone.isBlank()) {
                                            message = ochagText("Сначала добавьте номер телефона в профиле")
                                        } else {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.parse("tel:" + profile.phone)
                                                )
                                            )
                                        }
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                ) {
                                    Text("☎")
                                }
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
