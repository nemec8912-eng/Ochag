package com.ochag.app.ui.screens.familychat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.ui.theme.*

@Composable
fun FamilyChatScreen(
    onBackClick: () -> Unit = {},
    onAudioCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {}
) {

    var message by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CreamBackground,

        topBar = {
            FamilyChatHeader(
                onBackClick = onBackClick,
                onAudioCallClick = onAudioCallClick,
                onVideoCallClick = onVideoCallClick
            )
        },

        bottomBar = {
            MessageComposer(
                message = message,
                onMessageChange = { message = it },
                onSendClick = {
                    message = ""
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBackground)
                .padding(innerPadding)
        ) {

            DecorativeLeaves(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-25).dp, y = 10.dp)
                    .size(120.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 18.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {

                item {
                    IncomingMessage(
                        name = "Мама",
                        text = "Всем привет! Что готовим на ужин сегодня? 🍽",
                        time = "18:01",
                        avatarText = "М"
                    )
                }

                item {
                    OutgoingMessage(
                        text = "Давайте пасту с лососем?\nБыстро и вкусно 😊",
                        time = "18:02"
                    )
                }

                item {
                    IncomingMessage(
                        name = "Папа",
                        text = "Отличная идея! Я заеду за рыбой по пути домой.",
                        time = "18:03",
                        avatarText = "П"
                    )
                }

                item {
                    PhotoMessage()
                }

                item {
                    IncomingMessage(
                        name = "Сын",
                        text = "Уже делаю уроки 👍",
                        time = "18:12",
                        avatarText = "С"
                    )
                }

                item {
                    VoiceMessage()
                }

                item {
                    OutgoingMessage(
                        text = "Молодец! Если что — позови 🙌",
                        time = "18:14"
                    )
                }

                item {
                    VideoCircleMessage()
                }

                item {
                    StickerMessage()
                }

                item {
                    FileMessage()
                }

                item {
                    OutgoingMessage(
                        text = "Спасибо! Посмотрим вечером.",
                        time = "18:21"
                    )
                }
            }
        }
    }
}


// ============================================================
// ШАПКА ЧАТА
// ============================================================

@Composable
private fun FamilyChatHeader(
    onBackClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 8.dp,
                bottom = 6.dp
            )
            .height(72.dp)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                BackIcon(
                    modifier = Modifier.size(25.dp),
                    color = BrownTextPrimary
                )
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF5E4D8)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    FamilyLogo(
                        modifier = Modifier.size(31.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Семья",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )

                Text(
                    text = "4 участника  •  2 онлайн",
                    fontSize = 12.sp,
                    color = BrownTextSecondary
                )
            }

            RoundHeaderButton(
                onClick = onAudioCallClick
            ) {
                PhoneIcon(
                    modifier = Modifier.size(22.dp),
                    color = BrownTextSecondary
                )
            }

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            RoundHeaderButton(
                onClick = onVideoCallClick
            ) {
                VideoIcon(
                    modifier = Modifier.size(23.dp),
                    color = BrownTextSecondary
                )
            }
        }
    }
}

@Composable
private fun RoundHeaderButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {

    Surface(
        modifier = Modifier
            .size(41.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = CreamBackground
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}


// ============================================================
// ОБЫЧНЫЕ СООБЩЕНИЯ
// ============================================================

@Composable
private fun IncomingMessage(
    name: String,
    text: String,
    time: String,
    avatarText: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {

        Avatar(
            text = avatarText
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Surface(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .shadow(
                    3.dp,
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd = 18.dp,
                        bottomStart = 5.dp
                    )
                ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = 18.dp,
                bottomStart = 5.dp
            ),
            color = CardWhite
        ) {

            Column(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
            ) {

                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerracottaDark
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = text,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = BrownTextPrimary
                )

                Text(
                    text = time,
                    modifier = Modifier.align(Alignment.End),
                    fontSize = 10.sp,
                    color = BrownTextSecondary
                )
            }
        }
    }
}

@Composable
private fun OutgoingMessage(
    text: String,
    time: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {

        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(
                    3.dp,
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 5.dp
                    )
                ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 5.dp
            ),
            color = TerracottaPrimary
        ) {

            Column(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
            ) {

                Text(
                    text = text,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = time,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = "✓✓",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}


// ============================================================
// ФОТО
// ============================================================

@Composable
private fun PhotoMessage() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {

        Surface(
            modifier = Modifier
                .width(285.dp)
                .height(170.dp)
                .shadow(
                    4.dp,
                    RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFD8B691)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFFD9B48B)
                    )
            ) {

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "🍝",
                        fontSize = 70.sp
                    )

                    Text(
                        text = "Паста с лососем",
                        fontSize = 14.sp,
                        color = BrownTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "18:10  ✓✓",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
    }
}


// ============================================================
// ГОЛОСОВОЕ
// ============================================================

@Composable
private fun VoiceMessage() {

    Row(
        verticalAlignment = Alignment.Bottom
    ) {

        Avatar(
            text = "М"
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Surface(
            modifier = Modifier
                .width(260.dp)
                .height(66.dp),
            shape = RoundedCornerShape(18.dp),
            color = CardWhite
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = TerracottaPrimary
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "▶",
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(9.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    VoiceWave()

                    Text(
                        text = "0:18",
                        fontSize = 10.sp,
                        color = BrownTextSecondary
                    )
                }

                Text(
                    text = "18:13",
                    fontSize = 10.sp,
                    color = BrownTextSecondary
                )
            }
        }
    }
}

@Composable
private fun VoiceWave() {

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {

        val heights = listOf(
            0.25f, 0.5f, 0.8f, 0.4f,
            0.7f, 0.9f, 0.55f, 0.35f,
            0.8f, 0.6f, 0.3f, 0.7f,
            0.45f, 0.25f, 0.55f
        )

        heights.forEachIndexed { index, height ->

            val x =
                size.width *
                        index /
                        (heights.size - 1)

            val center =
                size.height / 2

            drawLine(
                color = TerracottaLight,
                start = Offset(
                    x,
                    center - size.height * height / 2
                ),
                end = Offset(
                    x,
                    center + size.height * height / 2
                ),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}


// ============================================================
// ВИДЕОКРУЖОК
// ============================================================

@Composable
private fun VideoCircleMessage() {

    Row(
        verticalAlignment = Alignment.Bottom
    ) {

        Avatar(
            text = "Д"
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(18.dp),
            color = CardWhite
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Surface(
                    modifier = Modifier.size(82.dp),
                    shape = CircleShape,
                    color = Color(0xFFE6C9B9)
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "👧",
                            fontSize = 46.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.85f)
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "▶",
                            fontSize = 13.sp,
                            color = TerracottaDark
                        )
                    }
                }

                Text(
                    text = "0:06",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    fontSize = 10.sp,
                    color = BrownTextSecondary
                )
            }
        }
    }
}


// ============================================================
// СТИКЕР
// ============================================================

@Composable
private fun StickerMessage() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {

        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = "🧸❤️🦊",
                fontSize = 60.sp
            )

            Text(
                text = "18:16 ✓✓",
                fontSize = 10.sp,
                color = BrownTextSecondary
            )
        }
    }
}


// ============================================================
// ФАЙЛ
// ============================================================

@Composable
private fun FileMessage() {

    Row(
        verticalAlignment = Alignment.Bottom
    ) {

        Avatar(
            text = "П"
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Surface(
            modifier = Modifier.width(265.dp),
            shape = RoundedCornerShape(18.dp),
            color = CardWhite
        ) {

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFFF1D1C3)
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "PDF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaDark
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "План_ремонта_ванной.pdf",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrownTextPrimary
                    )

                    Text(
                        text = "1.2 МБ • PDF",
                        fontSize = 11.sp,
                        color = BrownTextSecondary
                    )
                }

                Text(
                    text = "18:20",
                    fontSize = 10.sp,
                    color = BrownTextSecondary
                )
            }
        }
    }
}


// ============================================================
// ПОЛЕ ВВОДА
// ============================================================

@Composable
private fun MessageComposer(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = CreamBackground
    ) {

        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = CardWhite
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "📎",
                            fontSize = 23.sp
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        TextField(
                            value = message,
                            onValueChange = onMessageChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = "Напишите сообщение...",
                                    fontSize = 14.sp,
                                    color = BrownTextSecondary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization =
                                    KeyboardCapitalization.Sentences
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Text(
                            text = "☺",
                            fontSize = 23.sp,
                            color = BrownTextSecondary
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "🎙",
                            fontSize = 21.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .clickable {
                            onSendClick()
                        },
                    shape = CircleShape,
                    color = TerracottaPrimary
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        SendIcon(
                            modifier = Modifier.size(25.dp),
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 15.dp,
                        vertical = 8.dp
                    ),
                horizontalArrangement =
                    Arrangement.SpaceAround
            ) {

                AttachmentItem(
                    icon = "▧",
                    title = "Галерея"
                )

                AttachmentItem(
                    icon = "◉",
                    title = "Камера"
                )

                AttachmentItem(
                    icon = "□",
                    title = "Файл"
                )

                AttachmentItem(
                    icon = "⌖",
                    title = "Геолокация"
                )
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    icon: String,
    title: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = icon,
            fontSize = 21.sp,
            color = BrownTextSecondary
        )

        Text(
            text = title,
            fontSize = 10.sp,
            color = BrownTextSecondary
        )
    }
}


// ============================================================
// АВАТАР
// ============================================================

@Composable
private fun Avatar(
    text: String
) {

    Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = Color(0xFFE8C6B5)
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BrownTextPrimary
            )
        }
    }
}


// ============================================================
// ЛОГОТИП СЕМЬИ
// ============================================================

@Composable
private fun FamilyLogo(
    modifier: Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        drawCircle(
            color = TerracottaPrimary,
            radius = size.width * 0.13f,
            center = Offset(
                size.width * 0.5f,
                size.height * 0.3f
            )
        )

        drawCircle(
            color = TerracottaPrimary,
            radius = size.width * 0.10f,
            center = Offset(
                size.width * 0.25f,
                size.height * 0.4f
            )
        )

        drawCircle(
            color = TerracottaPrimary,
            radius = size.width * 0.10f,
            center = Offset(
                size.width * 0.75f,
                size.height * 0.4f
            )
        )

        drawArc(
            color = TerracottaPrimary,
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(
                size.width * 0.25f,
                size.height * 0.48f
            ),
            size = Size(
                size.width * 0.50f,
                size.height * 0.37f
            ),
            style = Stroke(
                width = 2.5.dp.toPx()
            )
        )
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
                size.width * 0.80f,
                size.height * 0.20f
            ),
            end = Offset(
                size.width * 0.25f,
                size.height * 0.50f
            ),
            strokeWidth = 2.dp.toPx()
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.25f,
                size.height * 0.50f
            ),
            end = Offset(
                size.width * 0.80f,
                size.height * 0.80f
            ),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun PhoneIcon(
    modifier: Modifier,
    color: Color
) {

    Canvas(
        modifier = modifier
    ) {

        val stroke = Stroke(
            width = 2.dp.toPx()
        )

        val path = Path().apply {

            moveTo(
                size.width * 0.30f,
                size.height * 0.14f
            )

            cubicTo(
                size.width * 0.15f,
                size.height * 0.18f,
                size.width * 0.20f,
                size.height * 0.50f,
                size.width * 0.48f,
                size.height * 0.76f
            )

            cubicTo(
                size.width * 0.68f,
                size.height * 0.94f,
                size.width * 0.88f,
                size.height * 0.82f,
                size.width * 0.86f,
                size.height * 0.68f
            )
        }

        drawPath(
            path = path,
            color = color,
            style = stroke
        )
    }
}

@Composable
private fun VideoIcon(
    modifier: Modifier,
    color: Color
) {

    Canvas(
        modifier = modifier
    ) {

        val stroke = Stroke(
            width = 2.dp.toPx()
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(
                size.width * 0.10f,
                size.height * 0.25f
            ),
            size = Size(
                size.width * 0.56f,
                size.height * 0.50f
            ),
            style = stroke
        )

        val triangle = Path().apply {

            moveTo(
                size.width * 0.68f,
                size.height * 0.40f
            )

            lineTo(
                size.width * 0.92f,
                size.height * 0.26f
            )

            lineTo(
                size.width * 0.92f,
                size.height * 0.74f
            )

            lineTo(
                size.width * 0.68f,
                size.height * 0.60f
            )

            close()
        }

        drawPath(
            path = triangle,
            color = color,
            style = stroke
        )
    }
}

@Composable
private fun SendIcon(
    modifier: Modifier,
    color: Color
) {

    Canvas(
        modifier = modifier
    ) {

        val path = Path().apply {

            moveTo(
                size.width * 0.08f,
                size.height * 0.47f
            )

            lineTo(
                size.width * 0.90f,
                size.height * 0.12f
            )

            lineTo(
                size.width * 0.64f,
                size.height * 0.90f
            )

            lineTo(
                size.width * 0.47f,
                size.height * 0.58f
            )

            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx()
            )
        )
    }
}


// ============================================================
// ДЕКОРАТИВНЫЕ ЛИСТЬЯ
// ============================================================

@Composable
private fun DecorativeLeaves(
    modifier: Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val color =
            SoftGreen.copy(
                alpha = 0.18f
            )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.05f,
                size.height * 0.80f
            ),
            end = Offset(
                size.width * 0.88f,
                size.height * 0.12f
            ),
            strokeWidth = 2.dp.toPx()
        )

        repeat(5) { index ->

            val x =
                size.width *
                        (0.18f + index * 0.14f)

            val y =
                size.height *
                        (0.68f - index * 0.11f)

            drawOval(
                color = color,
                topLeft = Offset(
                    x,
                    y
                ),
                size = Size(
                    28.dp.toPx(),
                    12.dp.toPx()
                )
            )
        }
    }
}