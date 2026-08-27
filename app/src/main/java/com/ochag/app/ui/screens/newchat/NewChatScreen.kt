package com.ochag.app.ui.screens.newchat

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.ui.theme.*

enum class ChatType {
    PERSONAL,
    GROUP,
    THEMATIC
}

@Composable
fun NewChatScreen(
    onBackClick: () -> Unit = {},
    onCreateChatClick: () -> Unit = {}
) {

    var chatName by remember {
        mutableStateOf("Ремонт дома")
    }

    var selectedType by remember {
        mutableStateOf(ChatType.GROUP)
    }

    var confirmMembers by remember {
        mutableStateOf(true)
    }

    var allowMedia by remember {
        mutableStateOf(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {

        DecorativeLeaves(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 38.dp, y = 45.dp)
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
                start = 20.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 105.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ------------------------------------------------
            // ЗАГОЛОВОК
            // ------------------------------------------------

            item {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                onBackClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {

                        BackIcon(
                            modifier = Modifier.size(27.dp),
                            color = BrownTextPrimary
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column {

                        Text(
                            text = "Новый чат",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Normal,
                            color = BrownTextPrimary
                        )

                        Text(
                            text = "Создай отдельное пространство для общения",
                            fontSize = 13.sp,
                            color = BrownTextSecondary
                        )
                    }
                }
            }

            // ------------------------------------------------
            // ОБЛОЖКА ЧАТА
            // ------------------------------------------------

            item {

                SectionCard {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Обложка чата",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrownTextPrimary
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Выбери обложку или иконку,\nкоторая будет отображаться\nв списке чатов",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = BrownTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .shadow(
                                    elevation = 7.dp,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .background(
                                    Color(0xFFF7E7DA)
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            HomeFlameLogo(
                                modifier = Modifier.size(67.dp)
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp),
                                shape = CircleShape,
                                color = CardWhite,
                                shadowElevation = 4.dp
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {

                                    Text(
                                        text = "✎",
                                        fontSize = 16.sp,
                                        color = TerracottaDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------
            // НАЗВАНИЕ
            // ------------------------------------------------

            item {

                SectionCard {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Название чата",
                            fontSize = 15.sp,
                            color = BrownTextPrimary
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        OutlinedTextField(
                            value = chatName,
                            onValueChange = {
                                chatName = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerracottaPrimary,
                                unfocusedBorderColor = BorderSoft,
                                focusedContainerColor = CardWhite,
                                unfocusedContainerColor = CardWhite,
                                cursorColor = TerracottaPrimary,
                                focusedTextColor = BrownTextPrimary,
                                unfocusedTextColor = BrownTextPrimary
                            )
                        )
                    }
                }
            }

            // ------------------------------------------------
            // ТИП ЧАТА
            // ------------------------------------------------

            item {

                SectionCard {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Text(
                            text = "Тип чата",
                            fontSize = 15.sp,
                            color = BrownTextPrimary
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            ChatTypeButton(
                                modifier = Modifier.weight(1f),
                                title = "Личный",
                                symbol = "○",
                                selected =
                                    selectedType == ChatType.PERSONAL,
                                onClick = {
                                    selectedType = ChatType.PERSONAL
                                }
                            )

                            ChatTypeButton(
                                modifier = Modifier.weight(1f),
                                title = "Групповой",
                                symbol = "◉",
                                selected =
                                    selectedType == ChatType.GROUP,
                                onClick = {
                                    selectedType = ChatType.GROUP
                                }
                            )

                            ChatTypeButton(
                                modifier = Modifier.weight(1f),
                                title = "Тематический",
                                symbol = "◇",
                                selected =
                                    selectedType == ChatType.THEMATIC,
                                onClick = {
                                    selectedType = ChatType.THEMATIC
                                }
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------
            // УЧАСТНИКИ
            // ------------------------------------------------

            item {

                SectionCard {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 15.dp,
                            vertical = 12.dp
                        )
                    ) {

                        Text(
                            text = "Участники",
                            fontSize = 15.sp,
                            color = BrownTextPrimary
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ParticipantRow(
                            avatarText = "С",
                            title = "Супруга",
                            subtitle = "владелец чата",
                            badge = "Владелец"
                        )

                        HorizontalDivider(
                            color = BorderSoft.copy(alpha = 0.65f)
                        )

                        ParticipantRow(
                            avatarText = "Д",
                            title = "Дети",
                            subtitle = "2 участника",
                            badge = null
                        )

                        HorizontalDivider(
                            color = BorderSoft.copy(alpha = 0.65f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(
                                    vertical = 11.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        TerracottaLight.copy(
                                            alpha = 0.45f
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = "+",
                                    fontSize = 27.sp,
                                    color = TerracottaDark
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Text(
                                text = "Добавить участника",
                                fontSize = 15.sp,
                                color = TerracottaDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------
            // НАСТРОЙКИ
            // ------------------------------------------------

            item {

                SectionCard {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 15.dp,
                            vertical = 11.dp
                        )
                    ) {

                        Text(
                            text = "Настройки чата",
                            fontSize = 15.sp,
                            color = BrownTextPrimary
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        SettingsArrowRow(
                            icon = "▣",
                            title = "Доступ: только по приглашению"
                        )

                        HorizontalDivider(
                            color = BorderSoft.copy(alpha = 0.65f)
                        )

                        SettingsSwitchRow(
                            icon = "♙",
                            title = "Подтверждение новых участников",
                            checked = confirmMembers,
                            onCheckedChange = {
                                confirmMembers = it
                            }
                        )

                        HorizontalDivider(
                            color = BorderSoft.copy(alpha = 0.65f)
                        )

                        SettingsSwitchRow(
                            icon = "▧",
                            title = "Можно отправлять фото, файлы, голосовые",
                            checked = allowMedia,
                            onCheckedChange = {
                                allowMedia = it
                            }
                        )
                    }
                }
            }

            // ------------------------------------------------
            // ШАБЛОНЫ
            // ------------------------------------------------

            item {

                SectionCard {

                    Column(
                        modifier = Modifier.padding(
                            vertical = 13.dp
                        )
                    ) {

                        Text(
                            text = "Готовые шаблоны",
                            modifier = Modifier.padding(
                                horizontal = 15.dp
                            ),
                            fontSize = 15.sp,
                            color = BrownTextPrimary
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 10.dp
                                ),
                            horizontalArrangement =
                                Arrangement.spacedBy(7.dp)
                        ) {

                            TemplateButton(
                                modifier = Modifier.weight(1f),
                                icon = "♥",
                                title = "Семья",
                                onClick = {
                                    chatName = "Семья"
                                    selectedType = ChatType.GROUP
                                }
                            )

                            TemplateButton(
                                modifier = Modifier.weight(1f),
                                icon = "♡",
                                title = "Супруга",
                                onClick = {
                                    chatName = "Супруга"
                                    selectedType = ChatType.PERSONAL
                                }
                            )

                            TemplateButton(
                                modifier = Modifier.weight(1f),
                                icon = "⌂",
                                title = "Ремонт\nдома",
                                onClick = {
                                    chatName = "Ремонт дома"
                                    selectedType = ChatType.THEMATIC
                                }
                            )

                            TemplateButton(
                                modifier = Modifier.weight(1f),
                                icon = "▣",
                                title = "Поездка",
                                onClick = {
                                    chatName = "Поездка"
                                    selectedType = ChatType.THEMATIC
                                }
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------
            // СОЗДАТЬ ЧАТ
            // ------------------------------------------------

            item {

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(61.dp)
                        .shadow(
                            elevation = 7.dp,
                            shape = RoundedCornerShape(30.dp)
                        )
                        .clickable {
                            onCreateChatClick()
                        },
                    shape = RoundedCornerShape(30.dp),
                    color = TerracottaPrimary
                ) {

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement =
                            Arrangement.Center,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Surface(
                            modifier = Modifier.size(39.dp),
                            shape = CircleShape,
                            color = CardWhite
                        ) {

                            Box(
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = "+",
                                    fontSize = 28.sp,
                                    color = TerracottaPrimary
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.width(11.dp)
                        )

                        Text(
                            text = "Создать чат",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// ОБЩАЯ КАРТОЧКА
// ============================================================

@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(23.dp),
                ambientColor =
                    BrownTextPrimary.copy(alpha = 0.07f),
                spotColor =
                    BrownTextPrimary.copy(alpha = 0.07f)
            ),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardWhite
        )
    ) {

        Column(
            content = content
        )
    }
}


// ============================================================
// ТИП ЧАТА
// ============================================================

@Composable
private fun ChatTypeButton(
    modifier: Modifier,
    title: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .height(61.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(15.dp),
        color =
            if (selected) {
                TerracottaPrimary
            } else {
                CardWhite
            },
        border =
            if (selected) {
                null
            } else {
                BorderStroke(
                    1.dp,
                    BorderSoft
                )
            }
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = symbol,
                fontSize = 21.sp,
                color =
                    if (selected) {
                        Color.White
                    } else {
                        BrownTextSecondary
                    }
            )

            Text(
                text = title,
                fontSize = 11.sp,
                color =
                    if (selected) {
                        Color.White
                    } else {
                        BrownTextPrimary
                    },
                maxLines = 1
            )
        }
    }
}


// ============================================================
// УЧАСТНИК
// ============================================================

@Composable
private fun ParticipantRow(
    avatarText: String,
    title: String,
    subtitle: String,
    badge: String?
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 9.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Surface(
            modifier = Modifier.size(43.dp),
            shape = CircleShape,
            color = Color(0xFFE9CBB9)
        ) {

            Box(
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = avatarText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrownTextPrimary
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
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = BrownTextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = BrownTextSecondary
            )
        }

        if (badge != null) {

            Surface(
                shape = RoundedCornerShape(15.dp),
                color = TerracottaLight.copy(
                    alpha = 0.45f
                )
            ) {

                Text(
                    text = badge,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    fontSize = 10.sp,
                    color = TerracottaDark
                )
            }
        }
    }
}


// ============================================================
// НАСТРОЙКИ
// ============================================================

@Composable
private fun SettingsArrowRow(
    icon: String,
    title: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 10.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = icon,
            fontSize = 20.sp,
            color = BrownTextSecondary
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = BrownTextPrimary
        )

        Text(
            text = "›",
            fontSize = 26.sp,
            color = BrownTextSecondary
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: String,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = icon,
            fontSize = 20.sp,
            color = BrownTextSecondary
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = BrownTextPrimary
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CardWhite,
                checkedTrackColor = TerracottaPrimary,
                uncheckedThumbColor = CardWhite,
                uncheckedTrackColor = BorderSoft
            )
        )
    }
}


// ============================================================
// ШАБЛОН
// ============================================================

@Composable
private fun TemplateButton(
    modifier: Modifier,
    icon: String,
    title: String,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = CreamBackground.copy(
            alpha = 0.55f
        ),
        border = BorderStroke(
            1.dp,
            BorderSoft.copy(alpha = 0.8f)
        )
    ) {

        Column(
            modifier = Modifier.padding(
                vertical = 7.dp
            ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = icon,
                fontSize = 22.sp,
                color = TerracottaPrimary
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = title,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                color = BrownTextPrimary
            )
        }
    }
}


// ============================================================
// ЛОГОТИП ДОМ + ОГОНЬ
// ============================================================

@Composable
private fun HomeFlameLogo(
    modifier: Modifier
) {

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
            color = CardWhite,
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
// НАЗАД
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


// ============================================================
// ВЕТОЧКА
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
                alpha = 0.30f
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
            strokeWidth = 1.5.dp.toPx()
        )

        repeat(5) { index ->

            val x =
                size.width *
                        (0.23f + index * 0.12f)

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