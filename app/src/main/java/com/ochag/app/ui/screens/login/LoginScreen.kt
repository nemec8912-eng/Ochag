package com.ochag.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.ochag.app.data.OchagAppState
import com.ochag.app.ui.theme.BrownTextPrimary
import com.ochag.app.ui.theme.BrownTextSecondary
import com.ochag.app.ui.theme.CardWhite
import com.ochag.app.ui.theme.CreamBackground
import com.ochag.app.ui.theme.TerracottaPrimary


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}
) {

    var login by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var errorText by remember {
        mutableStateOf("")
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // =========================
            // ЛОГОТИП
            // =========================

            Text(
                text = "Очаг",
                fontSize = 42.sp,
                color = BrownTextPrimary
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            Text(
                text = "Тёплое пространство\nтолько для своих",
                fontSize = 16.sp,
                color = BrownTextSecondary
            )


            Spacer(
                modifier = Modifier.height(40.dp)
            )


            // =========================
            // КАРТОЧКА ВХОДА
            // =========================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardWhite
                )
            ) {

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    Text(
                        text = "Вход",
                        fontSize = 24.sp,
                        color = BrownTextPrimary
                    )


                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )


                    // =========================
                    // ЛОГИН
                    // =========================

                    OutlinedTextField(
                        value = login,

                        onValueChange = {
                            login = it
                            errorText = ""
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("Логин")
                        },

                        singleLine = true,

                        shape = RoundedCornerShape(16.dp)
                    )


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    // =========================
                    // ПАРОЛЬ
                    // =========================

                    OutlinedTextField(
                        value = password,

                        onValueChange = {
                            password = it
                            errorText = ""
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("Пароль")
                        },

                        singleLine = true,

                        visualTransformation =
                            if (passwordVisible) {

                                VisualTransformation.None

                            } else {

                                PasswordVisualTransformation()
                            },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),

                        trailingIcon = {

                            TextButton(
                                onClick = {

                                    passwordVisible =
                                        !passwordVisible
                                }
                            ) {

                                Text(
                                    text =
                                        if (passwordVisible) {
                                            "Скрыть"
                                        } else {
                                            "Показать"
                                        }
                                )
                            }
                        },

                        shape = RoundedCornerShape(16.dp)
                    )


                    // =========================
                    // ОШИБКА
                    // =========================

                    if (errorText.isNotEmpty()) {

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = errorText,
                            color = Color.Red
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(25.dp)
                    )


                    // =========================
                    // КНОПКА ВХОДА
                    // =========================

                    Button(
                        onClick = {

                            if (
                                login.isBlank() ||
                                password.isBlank()
                            ) {

                                errorText =
                                    "Введите логин и пароль"

                                return@Button
                            }


                            val result =
                                OchagAppState.login(
                                    login = login.trim(),
                                    password = password
                                )


                            if (result) {

                                errorText = ""

                                onLoginSuccess()

                            } else {

                                errorText =
                                    "Неверный логин или пароль"
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),

                        shape = RoundedCornerShape(18.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerracottaPrimary
                        )
                    ) {

                        Text(
                            text = "Войти",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}