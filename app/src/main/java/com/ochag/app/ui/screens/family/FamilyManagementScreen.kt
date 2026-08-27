package com.ochag.app.ui.screens.family

import android.net.Uri

import androidx.compose.foundation.background
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
fun FamilyManagementScreen(

    onMemberClick: (String) -> Unit

) {

    val members =
        listOf(
            "Михаил",
            "Анна",
            "Дочь",
            "Сын",
            "Бабушка"
        )


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


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp
                )
    ) {


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        Text(

            text =
                "Моя семья",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium,

            fontWeight =
                FontWeight.Bold
        )


        Text(

            text =
                "Участники Очаг",

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
                    18.dp
                )
        )


        LazyColumn(

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                ),

            contentPadding =
                PaddingValues(
                    bottom = 20.dp
                )
        ) {


            items(

                items =
                    members,

                key = {
                    it
                }

            ) { member ->


                val isOnline =
                    normalizedOnlineUsers
                        .contains(
                            normalizePresenceName(
                                member
                            )
                        )


                Card(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {

                                onMemberClick(
                                    member
                                )
                            },

                    shape =
                        RoundedCornerShape(
                            22.dp
                        ),

                    colors =
                        CardDefaults
                            .cardColors(

                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                            )
                ) {


                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    16.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {


                        Box(

                            modifier =
                                Modifier.size(
                                    54.dp
                                )
                        ) {


                            Surface(

                                modifier =
                                    Modifier.fillMaxSize(),

                                shape =
                                    CircleShape,

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
                                            member
                                                .firstOrNull()
                                                ?.uppercase()
                                                ?: "?",

                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleLarge,

                                        fontWeight =
                                            FontWeight.Bold
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
                                            15.dp
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
                                Modifier.width(
                                    14.dp
                                )
                        )


                        Column(

                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {


                            Text(

                                text =
                                    member,

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,

                                fontWeight =
                                    FontWeight.SemiBold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        3.dp
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
                                        .bodyMedium,

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
                        }


                        Text(

                            text =
                                "›",

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
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
