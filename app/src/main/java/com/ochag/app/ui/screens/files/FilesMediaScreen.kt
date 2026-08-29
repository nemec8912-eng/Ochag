package com.ochag.app.ui.screens.files

import com.ochag.app.ui.localization.ochagText

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.ui.theme.*

private enum class MediaTab {
    PHOTO,
    VIDEO,
    CIRCLES,
    VOICE,
    DOCUMENTS
}

@Composable
fun FilesMediaScreen(
    onBackClick: () -> Unit = {},
    onChatsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {

    var selectedTab by remember {
        mutableStateOf(MediaTab.PHOTO)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        bottomBar = {
            FilesBottomBar(
                onChatsClick = onChatsClick,
                onContactsClick = onContactsClick,
                onCallsClick = onCallsClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {

            DecorativeBranch(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = 20.dp)
                    .size(
                        width = 135.dp,
                        height = 100.dp
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 10.dp,
                    bottom = 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {

                // =================================================
                // ШАПКА
                // =================================================

                item {

                    FilesHeader(
                        onBackClick = onBackClick
                    )
                }


                // =================================================
                // ВКЛАДКИ
                // =================================================

                item {

                    MediaTabs(
                        selectedTab = selectedTab,
                        onSelected = {
                            selectedTab = it
                        }
                    )
                }


                // =================================================
                // НЕДАВНИЕ ФОТО
                // =================================================

                item {

                    SectionTitle(
                        title = ochagText("Недавние фото")
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    PhotoGrid()
                }


                // =================================================
                // ВИДЕО
                // =================================================

                item {

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    SectionTitle(
                        title = ochagText("Видео и кружки")
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    VideoGrid()
                }


                // =================================================
                // ГОЛОСОВЫЕ
                // =================================================

                item {

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    SectionTitle(
                        title = ochagText("Голосовые сообщения")
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    VoiceMessages()
                }


                // =================================================
                // ДОКУМЕНТЫ
                // =================================================

                item {

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    SectionTitle(
                        title = ochagText("Документы")
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    Documents()
                }


                // =================================================
                // БЫСТРЫЕ ДЕЙСТВИЯ
                // =================================================

                item {

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = ochagText("Быстрые действия"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    QuickActions()

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
            }
        }
    }
}


// ============================================================
// ШАПКА
// ============================================================

@Composable
private fun FilesHeader(
    onBackClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(23.dp)
            ),
        shape = RoundedCornerShape(23.dp),
        color = MaterialTheme.colorScheme.surface
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        onBackClick()
                    },
                contentAlignment = Alignment.Center
            ) {

                BackIcon(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                modifier = Modifier.size(47.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF5E4D8)
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    HomeFlameLogo(
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(11.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = ochagText("Семья"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = ochagText("Файлы и медиа"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    SearchIcon(
                        modifier = Modifier.size(21.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "⋮",
                    fontSize = 27.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// ============================================================
// ВКЛАДКИ
// ============================================================

@Composable
private fun MediaTabs(
    selectedTab: MediaTab,
    onSelected: (MediaTab) -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            MediaTabButton(
                modifier = Modifier.weight(1f),
                title = ochagText("Фото"),
                symbol = "▧",
                selected = selectedTab == MediaTab.PHOTO,
                onClick = {
                    onSelected(MediaTab.PHOTO)
                }
            )

            MediaTabButton(
                modifier = Modifier.weight(1f),
                title = ochagText("Видео"),
                symbol = "▶",
                selected = selectedTab == MediaTab.VIDEO,
                onClick = {
                    onSelected(MediaTab.VIDEO)
                }
            )

            MediaTabButton(
                modifier = Modifier.weight(1f),
                title = ochagText("Кружки"),
                symbol = "◌",
                selected = selectedTab == MediaTab.CIRCLES,
                onClick = {
                    onSelected(MediaTab.CIRCLES)
                }
            )

            MediaTabButton(
                modifier = Modifier.weight(1f),
                title = ochagText("Голос"),
                symbol = "♩",
                selected = selectedTab == MediaTab.VOICE,
                onClick = {
                    onSelected(MediaTab.VOICE)
                }
            )

            MediaTabButton(
                modifier = Modifier.weight(1f),
                title = ochagText("Файлы"),
                symbol = "□",
                selected = selectedTab == MediaTab.DOCUMENTS,
                onClick = {
                    onSelected(MediaTab.DOCUMENTS)
                }
            )
        }
    }
}

@Composable
private fun MediaTabButton(
    modifier: Modifier,
    title: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                Color(0xFFF6E4D7)
            } else {
                Color.Transparent
            }
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = symbol,
                fontSize = 17.sp,
                color =
                    if (selected) {
                        TerracottaPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
            )

            Text(
                text = title,
                fontSize = 9.sp,
                color =
                    if (selected) {
                        TerracottaDark
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1
            )
        }
    }
}


// ============================================================
// ЗАГОЛОВОК СЕКЦИИ
// ============================================================

@Composable
private fun SectionTitle(
    title: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = ochagText("Показать все  ›"),
            fontSize = 12.sp,
            color = TerracottaDark
        )
    }
}


// ============================================================
// ФОТО
// ============================================================

@Composable
private fun PhotoGrid() {

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "👨‍👩‍👧‍👦",
                background1 = Color(0xFFD9B89B),
                background2 = Color(0xFF9AA57E)
            )

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "🍝",
                background1 = Color(0xFFD2A973),
                background2 = Color(0xFFF0D5A3)
            )

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "👧",
                background1 = Color(0xFFE7C9B5),
                background2 = Color(0xFFF4E5D5)
            )

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "🛋",
                background1 = Color(0xFFE2D6C6),
                background2 = Color(0xFFF4EEE4)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "🐕",
                background1 = Color(0xFFD2B07B),
                background2 = Color(0xFFA1B078)
            )

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "🌿",
                background1 = Color(0xFFF0DEBE),
                background2 = Color(0xFFC8C09D)
            )

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "🌅",
                background1 = Color(0xFFE7A568),
                background2 = Color(0xFF8B9A80)
            )

            PhotoTile(
                modifier = Modifier.weight(1f),
                emoji = "☕",
                background1 = Color(0xFFE9D7C6),
                background2 = Color(0xFFF6EEE4)
            )
        }
    }
}

@Composable
private fun PhotoTile(
    modifier: Modifier,
    emoji: String,
    background1: Color,
    background2: Color
) {

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(
                RoundedCornerShape(13.dp)
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        background1,
                        background2
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = emoji,
            fontSize = 34.sp
        )
    }
}


// ============================================================
// ВИДЕО
// ============================================================

@Composable
private fun VideoGrid() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {

        VideoCard(
            modifier = Modifier.weight(1f),
            title = ochagText("Дочка"),
            duration = "0:06",
            emoji = "👧"
        )

        VideoCard(
            modifier = Modifier.weight(1f),
            title = ochagText("Папа"),
            duration = "0:12",
            emoji = "🌅"
        )

        VideoCard(
            modifier = Modifier.weight(1f),
            title = ochagText("Мама"),
            duration = "0:09",
            emoji = "👩"
        )

        VideoCard(
            modifier = Modifier.weight(1f),
            title = ochagText("Вечер"),
            duration = "0:15",
            emoji = "✨"
        )
    }
}

@Composable
private fun VideoCard(
    modifier: Modifier,
    title: String,
    duration: String,
    emoji: String
) {

    Surface(
        modifier = modifier
            .height(125.dp)
            .shadow(
                3.dp,
                RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface
    ) {

        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFE9CFB5),
                                Color(0xFFC99C72)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = emoji,
                    fontSize = 32.sp
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .size(25.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(
                        alpha = 0.45f
                    )
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "▶",
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = duration,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    fontSize = 9.sp,
                    color = Color.White
                )
            }

            Text(
                text = title,
                modifier = Modifier.padding(
                    start = 7.dp,
                    top = 5.dp
                ),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ============================================================
// ГОЛОСОВЫЕ
// ============================================================

@Composable
private fun VoiceMessages() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {

        VoiceCard(
            modifier = Modifier.weight(1f),
            duration = "0:18",
            subtitle = ochagText("Мама • 18:13")
        )

        VoiceCard(
            modifier = Modifier.weight(1f),
            duration = "0:22",
            subtitle = ochagText("Папа • Вчера")
        )

        VoiceCard(
            modifier = Modifier.weight(1f),
            duration = "0:09",
            subtitle = ochagText("Дочка • 3 дня")
        )
    }
}

@Composable
private fun VoiceCard(
    modifier: Modifier,
    duration: String,
    subtitle: String
) {

    Surface(
        modifier = modifier
            .height(82.dp),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {

        Column(
            modifier = Modifier.padding(8.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = TerracottaPrimary
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "▶",
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                MiniVoiceWave(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = duration,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.75f
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MiniVoiceWave(
    modifier: Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val heights = listOf(
            0.3f,
            0.6f,
            0.85f,
            0.45f,
            0.7f,
            0.35f,
            0.9f,
            0.55f,
            0.4f,
            0.7f,
            0.3f
        )

        heights.forEachIndexed { index, value ->

            val x =
                size.width *
                        index /
                        (heights.size - 1)

            drawLine(
                color = TerracottaLight,
                start = Offset(
                    x,
                    size.height / 2 -
                            size.height *
                            value /
                            2
                ),
                end = Offset(
                    x,
                    size.height / 2 +
                            size.height *
                            value /
                            2
                ),
                strokeWidth =
                    1.5.dp.toPx()
            )
        }
    }
}


// ============================================================
// ДОКУМЕНТЫ
// ============================================================

@Composable
private fun Documents() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        DocumentCard(
            modifier = Modifier.weight(1f),
            extension = "PDF",
            fileName = ochagText("План_ремонта_ванной.pdf"),
            info = ochagText("1.2 МБ • PDF"),
            accent = TerracottaPrimary
        )

        DocumentCard(
            modifier = Modifier.weight(1f),
            extension = "DOCX",
            fileName = ochagText("Список_покупок.docx"),
            info = ochagText("120 КБ • DOCX"),
            accent = Color(0xFF6899C7)
        )
    }
}

@Composable
private fun DocumentCard(
    modifier: Modifier,
    extension: String,
    fileName: String,
    info: String,
    accent: Color
) {

    Surface(
        modifier = modifier
            .height(85.dp),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(37.dp),
                shape = RoundedCornerShape(8.dp),
                color = accent.copy(
                    alpha = 0.18f
                )
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = extension,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(7.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = fileName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = info,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "↓",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ============================================================
// БЫСТРЫЕ ДЕЙСТВИЯ
// ============================================================

@Composable
private fun QuickActions() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {

        QuickAction(
            modifier = Modifier.weight(1f),
            icon = "▧",
            title = ochagText("Открыть\nгалерею")
        )

        QuickAction(
            modifier = Modifier.weight(1f),
            icon = "◉",
            title = ochagText("Снять\nфото")
        )

        QuickAction(
            modifier = Modifier.weight(1f),
            icon = "◌",
            title = ochagText("Записать\nкружок")
        )

        QuickAction(
            modifier = Modifier.weight(1f),
            icon = "⌕",
            title = ochagText("Прикрепить\nфайл")
        )
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: String,
    title: String
) {

    Surface(
        modifier = modifier
            .height(82.dp)
            .clickable { },
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = icon,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = title,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ============================================================
// НИЖНЕЕ МЕНЮ
// ============================================================

@Composable
private fun FilesBottomBar(
    onChatsClick: () -> Unit,
    onContactsClick: () -> Unit,
    onCallsClick: () -> Unit,
    onProfileClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .padding(
                horizontal = 20.dp,
                vertical = 8.dp
            )
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            BottomItem(
                title = ochagText("Чаты"),
                symbol = "◯",
                selected = false,
                onClick = onChatsClick
            )

            BottomItem(
                title = ochagText("Контакты"),
                symbol = "♧",
                selected = false,
                onClick = onContactsClick
            )

            BottomItem(
                title = ochagText("Звонки"),
                symbol = "☎",
                selected = false,
                onClick = onCallsClick
            )

            BottomItem(
                title = ochagText("Файлы"),
                symbol = "▰",
                selected = true,
                onClick = {}
            )

            BottomItem(
                title = ochagText("Профиль"),
                symbol = "♙",
                selected = false,
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun BottomItem(
    title: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val color =
        if (selected) {
            TerracottaPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier = Modifier
            .width(62.dp)
            .clickable {
                onClick()
            },
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = symbol,
            fontSize = 22.sp,
            color = color
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = title,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false,
            color = color
        )

        if (selected) {

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(2.dp)
                    .clip(
                        RoundedCornerShape(4.dp)
                    )
                    .background(
                        TerracottaPrimary
                    )
            )
        }
    }
}


// ============================================================
// ИКОНКИ
// ============================================================

@Composable
private fun BackIcon(
    modifier: Modifier,
    color: Color
) {

    Canvas(
        modifier = modifier
    ) {

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.78f,
                size.height * 0.18f
            ),
            end = Offset(
                size.width * 0.24f,
                size.height * 0.50f
            ),
            strokeWidth = 2.dp.toPx()
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.24f,
                size.height * 0.50f
            ),
            end = Offset(
                size.width * 0.78f,
                size.height * 0.82f
            ),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun SearchIcon(
    modifier: Modifier,
    color: Color
) {

    Canvas(
        modifier = modifier
    ) {

        val stroke =
            Stroke(
                width = 2.dp.toPx()
            )

        drawCircle(
            color = color,
            radius = size.width * 0.28f,
            center = Offset(
                size.width * 0.42f,
                size.height * 0.42f
            ),
            style = stroke
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.62f,
                size.height * 0.62f
            ),
            end = Offset(
                size.width * 0.88f,
                size.height * 0.88f
            ),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun HomeFlameLogo(
    modifier: Modifier
) {
    val logoCutoutColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
    ) {

        val house = Path().apply {

            moveTo(
                size.width * 0.14f,
                size.height * 0.43f
            )

            lineTo(
                size.width * 0.50f,
                size.height * 0.13f
            )

            lineTo(
                size.width * 0.86f,
                size.height * 0.43f
            )

            lineTo(
                size.width * 0.77f,
                size.height * 0.50f
            )

            lineTo(
                size.width * 0.77f,
                size.height * 0.83f
            )

            lineTo(
                size.width * 0.23f,
                size.height * 0.83f
            )

            lineTo(
                size.width * 0.23f,
                size.height * 0.50f
            )

            close()
        }

        drawPath(
            path = house,
            color = TerracottaPrimary
        )

        drawCircle(
            color = logoCutoutColor,
            radius = size.width * 0.20f,
            center = Offset(
                size.width * 0.50f,
                size.height * 0.60f
            )
        )

        val flame = Path().apply {

            moveTo(
                size.width * 0.50f,
                size.height * 0.73f
            )

            cubicTo(
                size.width * 0.36f,
                size.height * 0.67f,
                size.width * 0.40f,
                size.height * 0.53f,
                size.width * 0.50f,
                size.height * 0.44f
            )

            cubicTo(
                size.width * 0.49f,
                size.height * 0.55f,
                size.width * 0.61f,
                size.height * 0.57f,
                size.width * 0.60f,
                size.height * 0.66f
            )

            cubicTo(
                size.width * 0.59f,
                size.height * 0.72f,
                size.width * 0.54f,
                size.height * 0.74f,
                size.width * 0.50f,
                size.height * 0.73f
            )

            close()
        }

        drawPath(
            path = flame,
            color = TerracottaDark
        )
    }
}


// ============================================================
// ДЕКОРАТИВНАЯ ВЕТКА
// ============================================================

@Composable
private fun DecorativeBranch(
    modifier: Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val color =
            SoftGreen.copy(
                alpha = 0.27f
            )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.10f,
                size.height * 0.85f
            ),
            end = Offset(
                size.width * 0.86f,
                size.height * 0.15f
            ),
            strokeWidth =
                1.5.dp.toPx()
        )

        repeat(5) { index ->

            val x =
                size.width *
                        (0.22f + index * 0.12f)

            val y =
                size.height *
                        (0.70f - index * 0.11f)

            drawOval(
                color = color,
                topLeft = Offset(
                    x,
                    y
                ),
                size = Size(
                    29.dp.toPx(),
                    12.dp.toPx()
                )
            )
        }
    }
}
