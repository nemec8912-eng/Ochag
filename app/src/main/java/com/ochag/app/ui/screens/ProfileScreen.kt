package com.ochag.app.ui.screens.profile

import com.ochag.app.ui.localization.ochagText
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerAccountApi
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.components.OchagAvatar
import com.ochag.app.ui.components.OchagBottomBar
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagPrimaryButton
import com.ochag.app.ui.components.OchagTextField
import com.ochag.app.ui.theme.*

@Composable
fun ProfileScreen(
    onChatsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onFamilyClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onStickersClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val user = OchagAppState.currentUser
    val userName = user?.name ?: ochagText("Гость")
    val profiles by ServerRealtime.serverUserProfiles.collectAsState()
    val connectionState by ServerRealtime.connectionState.collectAsState()

    val serverPhone = profiles
        .firstOrNull { it.name.equals(userName, ignoreCase = true) }
        ?.phone
        .orEmpty()

    var phone by remember(serverPhone) { mutableStateOf(serverPhone) }
    var saveStatus by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val profilePreferences = remember(userName) {
        context.getSharedPreferences(
            "ochag_profile_settings_$userName",
            android.content.Context.MODE_PRIVATE
        )
    }
    var profileAvatarUri by remember(userName) {
        mutableStateOf(
            profilePreferences.getString("profile_avatar", "").orEmpty()
        )
    }
    val profileAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            profileAvatarUri = uri.toString()
            profilePreferences.edit()
                .putString("profile_avatar", profileAvatarUri)
                .apply()
        }
    }

    LaunchedEffect(Unit) { ServerRealtime.requestServerUsers() }

    val online = connectionState == ServerRealtime.ConnectionState.ONLINE
    val statusText = when (connectionState) {
        ServerRealtime.ConnectionState.ONLINE -> ochagText("В сети")
        ServerRealtime.ConnectionState.CONNECTING -> ochagText("Подключение…")
        ServerRealtime.ConnectionState.OFFLINE -> ochagText("Не в сети")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            OchagBottomBar(
                items = listOf(ochagText("Чаты"), ochagText("Контакты"), ochagText("Звонки"), ochagText("Файлы"), ochagText("Профиль")),
                selectedIndex = 4,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) { index ->
                when (index) {
                    0 -> onChatsClick()
                    1 -> onContactsClick()
                    2 -> onCallsClick()
                    3 -> onFilesClick()
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "❧",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp),
                fontSize = 80.sp,
                color = SoftGreen.copy(alpha = 0.12f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ochagText("Профиль"),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onSettingsClick),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⚙", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ochagText("Личное пространство в «Очаге»"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(22.dp))

                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 3.dp,
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            if (profileAvatarUri.isNotBlank()) {
                                ProfileAvatarImage(
                                    uri = Uri.parse(profileAvatarUri),
                                    modifier = Modifier.size(104.dp)
                                )
                            } else {
                                OchagAvatar(
                                    name = userName,
                                    size = 104.dp,
                                    online = online,
                                    backgroundColor = AvatarPeach
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(34.dp)
                                    .clickable {
                                        profileAvatarLauncher.launch(arrayOf("image/*"))
                                    },
                                shape = CircleShape,
                                color = TerracottaPrimary,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✎", color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(15.dp))

                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = CircleShape,
                                color = if (online) OnlineGreen else MaterialTheme.colorScheme.outline
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (online) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (user?.isOwner == true) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = TerracottaSoft
                            ) {
                                Text(
                                    text = ochagText("Владелец «Очага»"),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TerracottaDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionTitle(ochagText("Семья"), ochagText("Люди и всё общее"))
                Spacer(Modifier.height(8.dp))

                ProfileAction("♡", ochagText("Семья"), ochagText("Участники, роли и доступ"), AvatarSage, onFamilyClick)
                ProfileAction("＋", ochagText("Пригласить близких"), ochagText("Добавить нового участника"), AvatarRose, onInviteClick)

                Spacer(Modifier.height(18.dp))
                SectionTitle(ochagText("Контакты"), ochagText("Чтобы близкие могли найти тебя"))
                Spacer(Modifier.height(10.dp))

                OchagCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column {
                        OchagTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                saveStatus = ""
                            },
                            label = ochagText("Телефон"),
                            placeholder = "+7 999 123-45-67"
                        )

                        Spacer(Modifier.height(12.dp))

                        OchagPrimaryButton(
                            text = if (saving) ochagText("Сохраняю…") else ochagText("Сохранить номер"),
                            enabled = !saving && userName != ochagText("Гость")
                        ) {
                            saving = true
                            saveStatus = ""
                            ServerAccountApi.updatePhone(
                                userName = userName,
                                phone = phone
                            ) { result ->
                                saving = false
                                saveStatus = if (result.success) ochagText("Номер сохранён") else result.error
                                if (result.success) ServerRealtime.requestServerUsers()
                            }
                        }

                        if (saveStatus.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = saveStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (saveStatus == ochagText("Номер сохранён")) OnlineGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerracottaDark)
                ) {
                    Text(ochagText("Выйти из «Очага»"), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProfileAvatarImage(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = ochagText("Аватар профиля"),
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(modifier = modifier, shape = CircleShape, color = AvatarPeach) {}
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileAction(
    symbol: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    OchagCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        elevation = 2.dp,
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 15.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(17.dp),
                color = accent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = symbol,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
