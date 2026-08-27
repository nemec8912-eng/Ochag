package com.ochag.app.ui.screens.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochag.app.data.OchagAppState
import com.ochag.app.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onInviteRegistrationClick: () -> Unit = {}
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        DecorativeLeaves(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-22).dp, y = 24.dp)
                .size(150.dp, 115.dp)
        )

        DecorativeLeaves(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 38.dp, y = 18.dp)
                .size(145.dp, 105.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(126.dp)
                    .shadow(10.dp, RoundedCornerShape(34.dp)),
                shape = RoundedCornerShape(34.dp),
                color = CardWhite
            ) {
                Box(contentAlignment = Alignment.Center) {
                    HomeFlameLogo(Modifier.size(86.dp))
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Очаг",
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = BrownTextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Тёплое пространство\nтолько для своих",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = BrownTextSecondary
            )

            Spacer(Modifier.height(30.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(30.dp)),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    OutlinedTextField(
                        value = login,
                        onValueChange = {
                            login = it
                            errorText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Логин") },
                        leadingIcon = {
                            Text("○", fontSize = 20.sp, color = BrownTextSecondary)
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = loginFieldColors()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Пароль") },
                        leadingIcon = {
                            Text("□", fontSize = 18.sp, color = BrownTextSecondary)
                        },
                        trailingIcon = {
                            Text(
                                text = if (passwordVisible) "◉" else "◎",
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { passwordVisible = !passwordVisible }
                                    .padding(8.dp),
                                fontSize = 20.sp,
                                color = BrownTextSecondary
                            )
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(18.dp),
                        colors = loginFieldColors()
                    )

                    if (errorText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TerracottaDark
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (login.isBlank() || password.isBlank()) {
                                errorText = "Введите логин и пароль"
                                return@Button
                            }

                            val result = OchagAppState.login(
                                login = login.trim(),
                                password = password
                            )

                            if (result) {
                                errorText = ""
                                onLoginSuccess()
                            } else {
                                errorText = "Неверный логин или пароль"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerracottaPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Войти",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onInviteRegistrationClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerracottaDark)
                    ) {
                        Text(
                            text = "Регистрация по приглашению",
                            color = TerracottaDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Новый участник может войти\nтолько по приглашению владельца",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = BrownTextSecondary
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardWhite.copy(alpha = 0.72f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSoft)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("♡", fontSize = 22.sp, color = TerracottaDark)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Переписки и данные семьи защищены",
                        fontSize = 11.sp,
                        color = BrownTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TerracottaPrimary,
    unfocusedBorderColor = BorderSoft,
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite,
    cursorColor = TerracottaPrimary,
    focusedTextColor = BrownTextPrimary,
    unfocusedTextColor = BrownTextPrimary,
    focusedPlaceholderColor = BrownTextSecondary,
    unfocusedPlaceholderColor = BrownTextSecondary
)

@Composable
private fun HomeFlameLogo(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val house = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.43f)
            lineTo(size.width * 0.50f, size.height * 0.13f)
            lineTo(size.width * 0.86f, size.height * 0.43f)
            lineTo(size.width * 0.77f, size.height * 0.50f)
            lineTo(size.width * 0.77f, size.height * 0.83f)
            lineTo(size.width * 0.23f, size.height * 0.83f)
            lineTo(size.width * 0.23f, size.height * 0.50f)
            close()
        }

        drawPath(house, color = TerracottaPrimary)
        drawCircle(
            color = CardWhite,
            radius = size.width * 0.20f,
            center = Offset(size.width * 0.50f, size.height * 0.60f)
        )

        val flame = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.74f)
            cubicTo(
                size.width * 0.36f, size.height * 0.67f,
                size.width * 0.40f, size.height * 0.53f,
                size.width * 0.50f, size.height * 0.44f
            )
            cubicTo(
                size.width * 0.49f, size.height * 0.55f,
                size.width * 0.61f, size.height * 0.57f,
                size.width * 0.60f, size.height * 0.66f
            )
            cubicTo(
                size.width * 0.59f, size.height * 0.72f,
                size.width * 0.54f, size.height * 0.75f,
                size.width * 0.50f, size.height * 0.74f
            )
            close()
        }

        drawPath(flame, color = TerracottaDark)
    }
}

@Composable
private fun DecorativeLeaves(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val color = SoftGreen.copy(alpha = 0.22f)

        drawLine(
            color = color,
            start = Offset(size.width * 0.08f, size.height * 0.84f),
            end = Offset(size.width * 0.88f, size.height * 0.14f),
            strokeWidth = 1.5.dp.toPx()
        )

        repeat(5) { index ->
            val x = size.width * (0.18f + index * 0.13f)
            val y = size.height * (0.69f - index * 0.11f)
            drawOval(
                color = color,
                topLeft = Offset(x, y),
                size = Size(31.dp.toPx(), 13.dp.toPx())
            )
        }
    }
}
