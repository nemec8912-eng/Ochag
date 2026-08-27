package com.ochag.app.ui.screens.registration

import com.ochag.app.data.OchagAppState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.ui.theme.*

@Composable
fun InviteRegistrationScreen(
    onBackClick: () -> Unit = {},
    onRequestSent: () -> Unit = {}
) {

    var inviteCode by remember {
        mutableStateOf("")
    }

    var name by remember {
        mutableStateOf("")
    }

    var login by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var repeatPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var repeatPasswordVisible by remember {
        mutableStateOf(false)
    }

    var requestSent by remember {
        mutableStateOf(false)
    }

    val codeValid =
        inviteCode.trim().uppercase().startsWith("OCHAG-") &&
                inviteCode.trim().length >= 10

    val passwordValid =
        password.length >= 6

    val passwordsMatch =
        password == repeatPassword &&
                repeatPassword.isNotEmpty()

    val formValid =
        codeValid &&
                name.isNotBlank() &&
                login.isNotBlank() &&
                passwordValid &&
                passwordsMatch

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {

        DecorativeBranch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 42.dp, y = 42.dp)
                .size(
                    width = 145.dp,
                    height = 105.dp
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = 14.dp,
                    bottom = 30.dp
                )
        ) {

            // =====================================================
            // ШАПКА
            // =====================================================

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
                    modifier = Modifier.width(9.dp)
                )

                Column {

                    Text(
                        text = "Присоединиться",
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Normal,
                        color = BrownTextPrimary
                    )

                    Text(
                        text = "Вход в семейный «Очаг»",
                        fontSize = 13.sp,
                        color = BrownTextSecondary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )


            // =====================================================
            // ЛОГОТИП И ТЕКСТ
            // =====================================================

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    modifier = Modifier
                        .size(92.dp)
                        .shadow(
                            elevation = 7.dp,
                            shape = RoundedCornerShape(25.dp)
                        ),
                    shape = RoundedCornerShape(25.dp),
                    color = CardWhite
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        HomeFlameLogo(
                            modifier = Modifier.size(63.dp)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                Text(
                    text = "Добро пожаловать в «Очаг»",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrownTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Регистрация доступна только\nпо приглашению владельца семьи",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = BrownTextSecondary
                )
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )


            // =====================================================
            // ЕСЛИ ЗАЯВКА УЖЕ ОТПРАВЛЕНА
            // =====================================================

            if (requestSent) {

                SuccessCard()

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        TerracottaDark
                    )
                ) {

                    Text(
                        text = "Вернуться ко входу",
                        fontSize = 15.sp,
                        color = TerracottaDark,
                        fontWeight = FontWeight.Medium
                    )
                }

            } else {

                // =================================================
                // КОД ПРИГЛАШЕНИЯ
                // =================================================

                FormCard(
                    title = "Код приглашения"
                ) {

                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = {
                            inviteCode = it.uppercase()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Например: OCHAG-4821",
                                color = BrownTextSecondary.copy(
                                    alpha = 0.65f
                                )
                            )
                        },
                        leadingIcon = {
                            Text(
                                text = "✉",
                                fontSize = 20.sp,
                                color = TerracottaDark
                            )
                        },
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

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    if (inviteCode.isNotEmpty()) {

                        Text(
                            text =
                                if (codeValid) {
                                    "Формат кода правильный"
                                } else {
                                    "Код должен выглядеть как OCHAG-4821"
                                },
                            fontSize = 11.sp,
                            color =
                                if (codeValid) {
                                    Color(0xFF76957D)
                                } else {
                                    TerracottaDark
                                }
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // =================================================
                // ЛИЧНЫЕ ДАННЫЕ
                // =================================================

                FormCard(
                    title = "Ваши данные"
                ) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Имя",
                                color = BrownTextSecondary.copy(
                                    alpha = 0.65f
                                )
                            )
                        },
                        leadingIcon = {
                            Text(
                                text = "○",
                                fontSize = 21.sp,
                                color = BrownTextSecondary
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors()
                    )

                    Spacer(
                        modifier = Modifier.height(11.dp)
                    )

                    OutlinedTextField(
                        value = login,
                        onValueChange = {
                            login = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Логин",
                                color = BrownTextSecondary.copy(
                                    alpha = 0.65f
                                )
                            )
                        },
                        leadingIcon = {
                            Text(
                                text = "@",
                                fontSize = 19.sp,
                                color = BrownTextSecondary
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors()
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // =================================================
                // ПАРОЛЬ
                // =================================================

                FormCard(
                    title = "Пароль"
                ) {

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Придумайте пароль",
                                color = BrownTextSecondary.copy(
                                    alpha = 0.65f
                                )
                            )
                        },
                        leadingIcon = {
                            Text(
                                text = "□",
                                fontSize = 19.sp,
                                color = BrownTextSecondary
                            )
                        },
                        trailingIcon = {

                            Text(
                                text =
                                    if (passwordVisible) {
                                        "◉"
                                    } else {
                                        "◎"
                                    },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        passwordVisible =
                                            !passwordVisible
                                    }
                                    .padding(9.dp),
                                fontSize = 20.sp,
                                color = BrownTextSecondary
                            )
                        },
                        visualTransformation =
                            if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors()
                    )

                    Spacer(
                        modifier = Modifier.height(11.dp)
                    )

                    OutlinedTextField(
                        value = repeatPassword,
                        onValueChange = {
                            repeatPassword = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Повторите пароль",
                                color = BrownTextSecondary.copy(
                                    alpha = 0.65f
                                )
                            )
                        },
                        leadingIcon = {
                            Text(
                                text = "□",
                                fontSize = 19.sp,
                                color = BrownTextSecondary
                            )
                        },
                        trailingIcon = {

                            Text(
                                text =
                                    if (repeatPasswordVisible) {
                                        "◉"
                                    } else {
                                        "◎"
                                    },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        repeatPasswordVisible =
                                            !repeatPasswordVisible
                                    }
                                    .padding(9.dp),
                                fontSize = 20.sp,
                                color = BrownTextSecondary
                            )
                        },
                        visualTransformation =
                            if (repeatPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors()
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    Text(
                        text = "Минимум 6 символов",
                        fontSize = 10.sp,
                        color =
                            if (
                                password.isNotEmpty() &&
                                !passwordValid
                            ) {
                                TerracottaDark
                            } else {
                                BrownTextSecondary
                            }
                    )

                    if (
                        repeatPassword.isNotEmpty() &&
                        !passwordsMatch
                    ) {

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Пароли не совпадают",
                            fontSize = 10.sp,
                            color = TerracottaDark
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(15.dp)
                )


                // =================================================
                // ОБЪЯСНЕНИЕ
                // =================================================

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(21.dp),
                    color = Color(0xFFF7EFE7),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        BorderSoft
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 15.dp,
                            vertical = 14.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        ShieldIcon(
                            modifier = Modifier.size(36.dp),
                            color = TerracottaDark
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Text(
                            text = "После регистрации владелец семьи получит заявку. Войти в «Очаг» получится только после его подтверждения.",
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = BrownTextSecondary
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )


                // =================================================
                // ОТПРАВИТЬ ЗАЯВКУ
                // =================================================

                Button(
                    onClick = {

                        OchagAppState.submitJoinRequest(
                            name = name,
                            login = login,
                            inviteCode = inviteCode,
                            password = password
                        )

                        requestSent = true

                        onRequestSent()
                    },
                    enabled = formValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = BorderSoft,
                        disabledContentColor = BrownTextSecondary
                    )
                ) {
                    Text(
                        text = "Отправить заявку",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Никто не получит доступ к семейным чатам без разрешения владельца",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = BrownTextSecondary
                )
            }
        }
    }
}


// ============================================================
// КАРТОЧКА ФОРМЫ
// ============================================================

@Composable
private fun FormCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(23.dp)
            ),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardWhite
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 15.dp
            )
        ) {

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrownTextPrimary
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            content()
        }
    }
}


// ============================================================
// КАРТОЧКА "ЗАЯВКА ОТПРАВЛЕНА"
// ============================================================

@Composable
private fun SuccessCard() {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardWhite
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 25.dp,
                    vertical = 29.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = Color(0xFFE6EBDD)
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "✓",
                        fontSize = 39.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF718467)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(17.dp)
            )

            Text(
                text = "Заявка отправлена",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrownTextPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Теперь нужно дождаться подтверждения владельца семьи.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = BrownTextSecondary
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TerracottaLight.copy(
                    alpha = 0.35f
                )
            ) {

                Text(
                    text = "Ожидает подтверждения",
                    modifier = Modifier.padding(
                        horizontal = 15.dp,
                        vertical = 7.dp
                    ),
                    fontSize = 11.sp,
                    color = TerracottaDark
                )
            }
        }
    }
}


// ============================================================
// ЦВЕТА ПОЛЕЙ
// ============================================================

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TerracottaPrimary,
        unfocusedBorderColor = BorderSoft,
        focusedContainerColor = CardWhite,
        unfocusedContainerColor = CardWhite,
        cursorColor = TerracottaPrimary,
        focusedTextColor = BrownTextPrimary,
        unfocusedTextColor = BrownTextPrimary
    )


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
// ЛОГОТИП
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
                size.height * 0.74f
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
                size.height * 0.75f,
                size.width * 0.50f,
                size.height * 0.74f
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
// ЩИТ
// ============================================================

@Composable
private fun ShieldIcon(
    modifier: Modifier,
    color: Color
) {

    Canvas(
        modifier = modifier
    ) {

        val shield = Path().apply {

            moveTo(
                size.width * 0.50f,
                size.height * 0.07f
            )

            lineTo(
                size.width * 0.81f,
                size.height * 0.20f
            )

            lineTo(
                size.width * 0.76f,
                size.height * 0.60f
            )

            cubicTo(
                size.width * 0.72f,
                size.height * 0.78f,
                size.width * 0.58f,
                size.height * 0.88f,
                size.width * 0.50f,
                size.height * 0.94f
            )

            cubicTo(
                size.width * 0.42f,
                size.height * 0.88f,
                size.width * 0.28f,
                size.height * 0.78f,
                size.width * 0.24f,
                size.height * 0.60f
            )

            lineTo(
                size.width * 0.19f,
                size.height * 0.20f
            )

            close()
        }

        drawPath(
            path = shield,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx()
            )
        )
    }
}


// ============================================================
// ВЕТОЧКА
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
                alpha = 0.25f
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
                size = androidx.compose.ui.geometry.Size(
                    30.dp.toPx(),
                    13.dp.toPx()
                )
            )
        }
    }
}