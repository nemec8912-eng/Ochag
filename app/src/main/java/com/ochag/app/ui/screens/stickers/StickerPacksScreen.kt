package com.ochag.app.ui.screens.stickers

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.data.StickerPack
import com.ochag.app.data.StickerStore
import com.ochag.app.ui.components.OchagCard
import com.ochag.app.ui.components.OchagPrimaryButton
import com.ochag.app.ui.components.OchagSectionHeader
import com.ochag.app.ui.components.OchagTextField
import com.ochag.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StickerPacksScreen(
    onBackClick: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingPackId by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val packId = pendingPackId
        if (packId != null && uris.isNotEmpty()) {
            StickerStore.addImages(packId, uris)
        }
        pendingPackId = null
    }

    Scaffold(containerColor = CreamBackground) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(onClick = onBackClick),
                        shape = CircleShape,
                        color = CardWhite,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("‹", fontSize = 30.sp, color = BrownTextPrimary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    OchagSectionHeader(
                        title = "Стикеры",
                        subtitle = "Свои стикерпаки для семейных чатов"
                    )
                }
            }

            item {
                OchagPrimaryButton(text = "＋ Новый стикерпак") {
                    showCreateDialog = true
                }
            }

            items(
                items = StickerStore.packs,
                key = { it.id }
            ) { pack ->
                StickerPackCard(
                    pack = pack,
                    onAddImages = {
                        pendingPackId = pack.id
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    onDelete = {
                        StickerStore.deletePack(pack.id)
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateStickerPackDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                val id = StickerStore.createPack(title)
                showCreateDialog = false
                pendingPackId = id
                imagePicker.launch(arrayOf("image/*"))
            }
        )
    }
}

@Composable
private fun StickerPackCard(
    pack: StickerPack,
    onAddImages: () -> Unit,
    onDelete: () -> Unit
) {
    OchagCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pack.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = BrownTextPrimary
                    )
                    Text(
                        text = if (pack.builtIn) {
                            "Встроенный набор"
                        } else {
                            "${pack.imageUris.size} стикеров"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BrownTextSecondary
                    )
                }

                if (!pack.builtIn) {
                    TextButton(onClick = onAddImages) {
                        Text("Добавить", color = TerracottaPrimary)
                    }
                    TextButton(onClick = onDelete) {
                        Text("Удалить", color = TerracottaDark)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (pack.builtIn) {
                StickerEmojiGrid(pack.emojiStickers)
            } else if (pack.imageUris.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CardCream
                ) {
                    Text(
                        text = "Добавь PNG, JPG или WEBP — они появятся здесь и в чате.",
                        modifier = Modifier.padding(16.dp),
                        color = BrownTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pack.imageUris.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { uri ->
                                Box(modifier = Modifier.weight(1f)) {
                                    StickerImage(
                                        uri = Uri.parse(uri),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerEmojiGrid(stickers: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stickers.chunked(6).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { sticker ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = CardCream
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(sticker, fontSize = 28.sp)
                        }
                    }
                }
                repeat(6 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CreateStickerPackDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("Новый стикерпак", color = BrownTextPrimary)
        },
        text = {
            OchagTextField(
                value = title,
                onValueChange = { title = it },
                label = "Название",
                placeholder = "Например: Наш кот"
            )
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onCreate(title) }
            ) {
                Text("Создать", color = TerracottaPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = BrownTextSecondary)
            }
        }
    )
}

@Composable
private fun StickerImage(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardCream
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
