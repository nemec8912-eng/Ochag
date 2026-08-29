package com.ochag.app.ui.screens.family

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerAccountApi
import com.ochag.app.data.ServerJoinRequest
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagSectionHeader
import com.ochag.app.ui.localization.ochagText
import com.ochag.app.ui.theme.*
import java.util.Locale

@Composable
fun FamilyManagementScreen(
    onMemberClick: (String) -> Unit
) {
    val currentUser = OchagAppState.currentUser
    val currentUserName = currentUser?.name.orEmpty()
    val isOwner = currentUser?.isOwner == true
    val onlineUsers by ServerRealtime.onlineUsers.collectAsState()
    val profiles by ServerRealtime.serverUserProfiles.collectAsState()

    var requests by remember { mutableStateOf<List<ServerJoinRequest>>(emptyList()) }
    var requestsLoading by remember { mutableStateOf(false) }
    var requestsError by remember { mutableStateOf("") }
    var processingId by remember { mutableStateOf<Long?>(null) }

    val normalizedOnlineUsers = remember(onlineUsers) {
        onlineUsers.map(::normalizePresenceName).filter { it.isNotBlank() }.toSet()
    }

    val members = remember(profiles, currentUserName) {
        (profiles.map { it.name } + currentUserName)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
    }

    fun reloadRequests() {
        if (!isOwner || currentUserName.isBlank()) return
        requestsLoading = true
        requestsError = ""
        ServerAccountApi.loadJoinRequests(currentUserName) { result ->
            requestsLoading = false
            if (result.success) requests = result.requests else requestsError = result.error
        }
    }

    LaunchedEffect(currentUserName, isOwner) {
        ServerRealtime.requestServerUsers()
        reloadRequests()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OchagSectionHeader(
                    title = ochagText("Семья и приглашения"),
                    subtitle = ochagText("Только свои люди — доступ под твоим контролем")
                )
            }

            item {
                Text(
                    text = ochagText("Участники семьи"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (members.isEmpty()) {
                item {
                    Text(
                        text = ochagText("Участники пока не найдены"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(members, key = { "member_$it" }) { member ->
                val online = normalizedOnlineUsers.contains(normalizePresenceName(member))
                val owner = member.equals(currentUserName, ignoreCase = true) && isOwner
                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 3.dp,
                    contentPadding = PaddingValues(15.dp),
                    onClick = { onMemberClick(member) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OchagAvatar(
                            name = member,
                            size = 54.dp,
                            online = online,
                            backgroundColor = if (owner) TerracottaSoft else SoftGreenLight
                        )
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = member,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    owner -> ochagText("Владелец семьи")
                                    online -> ochagText("В сети")
                                    else -> ochagText("Не в сети")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (online) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ochagText("Заявки на вступление"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isOwner) {
                        TextButton(onClick = { reloadRequests() }, enabled = !requestsLoading) {
                            Text(if (requestsLoading) ochagText("Загрузка…") else ochagText("Обновить"))
                        }
                    }
                }
            }

            if (!isOwner) {
                item {
                    Text(
                        text = ochagText("Подтверждать заявки может только владелец семьи"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (requestsError.isNotBlank()) {
                item {
                    Text(
                        text = requestsError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (!requestsLoading && requests.isEmpty()) {
                item {
                    OchagCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Text(
                            text = ochagText("Новых заявок пока нет"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(requests, key = { "request_${it.requestId}" }) { request ->
                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OchagAvatar(
                                name = request.name,
                                size = 52.dp,
                                backgroundColor = TerracottaSoft
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = request.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "@${request.login}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    processingId = request.requestId
                                    requestsError = ""
                                    ServerAccountApi.approveJoinRequest(
                                        request.requestId,
                                        currentUserName
                                    ) { result ->
                                        processingId = null
                                        if (result.success) {
                                            requests = requests.filterNot { it.requestId == request.requestId }
                                            ServerRealtime.requestServerUsers()
                                        } else requestsError = result.error
                                    }
                                },
                                enabled = processingId == null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                            ) {
                                Text(ochagText("Принять"))
                            }
                            OutlinedButton(
                                onClick = {
                                    processingId = request.requestId
                                    requestsError = ""
                                    ServerAccountApi.declineJoinRequest(
                                        request.requestId,
                                        currentUserName
                                    ) { result ->
                                        processingId = null
                                        if (result.success) {
                                            requests = requests.filterNot { it.requestId == request.requestId }
                                        } else requestsError = result.error
                                    }
                                },
                                enabled = processingId == null,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(ochagText("Отклонить"))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun normalizePresenceName(value: String): String {
    val decoded = try { Uri.decode(value) } catch (_: Exception) { value }
    return decoded.trim().lowercase(Locale.getDefault())
        .replace(Regex("[^\\p{L}\\p{N}]+"), "")
}
