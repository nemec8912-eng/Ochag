package com.ochag.app.ui.screens.chats

import androidx.compose.ui.graphics.Color

/**
 * Локальный алиас для цвета активной кнопки стикеров.
 * Нужен, чтобы ChatRoomScreen мог использовать TerracottaSoft
 * без дополнительного импорта.
 */
val TerracottaSoft: Color
    get() = com.ochag.app.ui.theme.TerracottaSoft
