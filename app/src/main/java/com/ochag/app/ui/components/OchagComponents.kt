package com.ochag.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ochag.app.ui.theme.BorderSoft
import com.ochag.app.ui.theme.BrownTextPrimary
import com.ochag.app.ui.theme.BrownTextSecondary
import com.ochag.app.ui.theme.CardWhite
import com.ochag.app.ui.theme.OnlineGreen
import com.ochag.app.ui.theme.TerracottaPrimary
import com.ochag.app.ui.theme.TerracottaSoft

@Composable
fun OchagCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = clickableModifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun OchagPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TerracottaPrimary,
            contentColor = Color.White,
            disabledContainerColor = TerracottaSoft,
            disabledContentColor = Color.White.copy(alpha = 0.75f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OchagTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable (() -> Unit))? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(text = placeholder, color = BrownTextSecondary)
            }
        },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        trailingIcon = trailingContent,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TerracottaPrimary,
            unfocusedBorderColor = BorderSoft,
            focusedLabelColor = TerracottaPrimary,
            unfocusedLabelColor = BrownTextSecondary,
            focusedTextColor = BrownTextPrimary,
            unfocusedTextColor = BrownTextPrimary,
            cursorColor = TerracottaPrimary,
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite
        )
    )
}

@Composable
fun OchagAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    backgroundColor: Color = TerracottaSoft,
    online: Boolean = false
) {
    Box(modifier = modifier.size(size)) {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            shape = CircleShape,
            color = backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "О",
                    color = BrownTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(CardWhite)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(OnlineGreen)
                )
            }
        }
    }
}

@Composable
fun OchagSectionHeader(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = BrownTextPrimary
        )

        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BrownTextSecondary
            )
        }
    }
}

@Composable
fun OchagBottomBar(
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp),
        shape = RoundedCornerShape(28.dp),
        color = CardWhite,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val selected = index == selectedIndex

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(index) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) TerracottaPrimary else BrownTextSecondary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Box(
                        modifier = Modifier
                            .width(if (selected) 34.dp else 18.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) TerracottaPrimary
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}
