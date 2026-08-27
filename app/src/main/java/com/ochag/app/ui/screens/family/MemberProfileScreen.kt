package com.ochag.app.ui.screens.family

import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.ochag.app.data.ServerRealtime

import java.util.Locale


private val OnlineGreen =
    Color(
        0xFF2E7D32
    )


@Composable
fun MemberProfileScreen(

    name: String,

    status: String,

    onBackClick: () -> Unit,

    onMessageClick: () -> Unit

) {

    val onlineUsers by
    ServerRealtime
        .onlineUsers
        .collectAsState()


    val normalizedOnlineUsers =
        remember(
            onlineUsers
        ) {

            onlineUsers
                .map {
                    normalizePresenceName(
                        it
                    )
                }
                .filter {
                    it.isNotBlank()
                }
                .toSet()
        }


    val isOnline =
        normalizedOnlineUsers
            .contains(
                normalizePresenceName(
                    name
                )
            )


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    20.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Text(

            text =
                "← Назад",

            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick =
                            onBackClick
                    ),

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )


        Spacer(
            modifier =
                Modifier.height(
                    34.dp
                )
        )


        Box(

            modifier =
                Modifier.size(
                    124.dp
                )
        ) {


            Surface(

                modifier =
                    Modifier.fillMaxSize(),

                shape =
                    RoundedCornerShape(
                        34.dp
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {


                Box(

                    contentAlignment =
                        Alignment.Center
                ) {


                    Text(

                        text =
                            name
                                .firstOrNull()
                                ?.uppercase()
                                ?: "?",

                        style =
                            MaterialTheme
                                .typography
                                .displayMedium,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            Box(

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomEnd
                        )
                        .size(
                            24.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .background
                        )
                        .padding(
                            4.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(

                            if (
                                isOnline
                            ) {

                                OnlineGreen

                            } else {

                                MaterialTheme
                                    .colorScheme
                                    .outlineVariant
                            }
                        )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )


        Text(

            text =
                name,

            style =
                MaterialTheme
                    .typography
                    .headlineMedium,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )


        Text(

            text =

                if (
                    isOnline
                ) {

                    "В сети"

                } else {

                    "Не в сети"
                },

            style =
                MaterialTheme
                    .typography
                    .bodyLarge,

            fontWeight =

                if (
                    isOnline
                ) {

                    FontWeight.SemiBold

                } else {

                    FontWeight.Normal
                },

            color =

                if (
                    isOnline
                ) {

                    OnlineGreen

                } else {

                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                }
        )


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Text(

            text =
                status,

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(
                    44.dp
                )
        )


        Button(

            onClick =
                onMessageClick,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        60.dp
                    ),

            shape =
                RoundedCornerShape(
                    30.dp
                )
        ) {


            Text(
                "💬 Написать сообщение"
            )
        }
    }
}


private fun normalizePresenceName(

    value: String

): String {

    val decoded =
        try {

            Uri.decode(
                value
            )

        } catch (_: Exception) {

            value
        }


    return decoded
        .trim()
        .lowercase(
            Locale.getDefault()
        )
        .replace(
            Regex(
                "[^\\p{L}\\p{N}]+"
            ),
            ""
        )
}
