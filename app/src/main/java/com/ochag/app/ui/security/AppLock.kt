package com.ochag.app.ui.security

import android.content.Context
import java.security.MessageDigest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

enum class AppLockMode { PIN, PASSWORD, PATTERN }

object AppLockController {
    private const val PREFS = "ochag_app_lock"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_MODE = "mode"
    private const val KEY_SECRET_HASH = "secret_hash"
    private const val KEY_TIMEOUT = "timeout_ms"
    private const val KEY_BACKGROUND_AT = "background_at"
    const val NEVER_TIMEOUT = -1L

    private var context: Context? = null
    var enabled by mutableStateOf(false)
        private set
    var locked by mutableStateOf(false)
        private set
    var mode by mutableStateOf(AppLockMode.PIN)
        private set
    var timeoutMs by mutableLongStateOf(0L)
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(KEY_ENABLED, false)
        mode = runCatching {
            AppLockMode.valueOf(prefs.getString(KEY_MODE, AppLockMode.PIN.name).orEmpty())
        }.getOrDefault(AppLockMode.PIN)
        timeoutMs = prefs.getLong(KEY_TIMEOUT, 0L)
        locked = enabled
    }

    fun enable(mode: AppLockMode, secret: String) {
        val appContext = context ?: return
        this.mode = mode
        enabled = true
        locked = false
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_MODE, mode.name)
            .putString(KEY_SECRET_HASH, hash(secret))
            .apply()
    }

    fun disable() {
        val appContext = context ?: return
        enabled = false
        locked = false
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun setTimeout(timeout: Long) {
        timeoutMs = timeout
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putLong(KEY_TIMEOUT, timeout)?.apply()
    }

    fun onAppBackground() {
        if (!enabled) return
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putLong(KEY_BACKGROUND_AT, System.currentTimeMillis())?.apply()
    }

    fun onAppForeground() {
        if (!enabled || timeoutMs == NEVER_TIMEOUT) return
        val prefs = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        val backgroundAt = prefs.getLong(KEY_BACKGROUND_AT, 0L)
        if (backgroundAt > 0L && System.currentTimeMillis() - backgroundAt >= timeoutMs) {
            locked = true
        }
    }

    fun verify(secret: String): Boolean {
        val appContext = context ?: return false
        val saved = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SECRET_HASH, "").orEmpty()
        val correct = saved.isNotBlank() && saved == hash(secret)
        if (correct) locked = false
        return correct
    }

    private fun hash(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    if (!AppLockController.enabled || !AppLockController.locked) {
        content()
        return
    }

    var secret by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Очаг", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                when (AppLockController.mode) {
                    AppLockMode.PIN -> "Введите PIN-код"
                    AppLockMode.PASSWORD -> "Введите пароль"
                    AppLockMode.PATTERN -> "Введите графический ключ"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(18.dp))
            if (AppLockController.mode == AppLockMode.PATTERN) {
                PatternPad(
                    value = secret,
                    onValueChange = {
                        secret = it
                        error = false
                    }
                )
            } else {
                OutlinedTextField(
                    value = secret,
                    onValueChange = {
                        secret = if (AppLockController.mode == AppLockMode.PIN) {
                            it.filter(Char::isDigit).take(12)
                        } else it
                        error = false
                    },
                    singleLine = true,
                    label = { Text("Код доступа") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
            }
            if (error) {
                Text("Неверный код или пароль", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { error = !AppLockController.verify(secret) },
                enabled = secret.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Открыть") }
        }
    }
}

@Composable
fun PatternPad(
    value: String,
    onValueChange: (String) -> Unit
) {
    var fingerPosition by remember { mutableStateOf<Offset?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.outline

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    fun centers(): List<Offset> {
                        val gap = size.width / 4f
                        return (0..2).flatMap { row ->
                            (0..2).map { column ->
                                Offset(gap * (column + 1), gap * (row + 1))
                            }
                        }
                    }

                    fun nodeAt(position: Offset): Int? {
                        val radius = size.width / 8f
                        return centers().indexOfFirst { center ->
                            (center - position).getDistance() <= radius
                        }.takeIf { it >= 0 }?.plus(1)
                    }

                    var currentPattern = value
                    detectDragGestures(
                        onDragStart = { position ->
                            currentPattern = ""
                            nodeAt(position)?.let { node ->
                                currentPattern = node.toString()
                                onValueChange(currentPattern)
                            }
                            fingerPosition = position
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            fingerPosition = change.position
                            nodeAt(change.position)?.let { node ->
                                val digit = node.toString()
                                if (!currentPattern.contains(digit)) {
                                    currentPattern += digit
                                    onValueChange(currentPattern)
                                }
                            }
                        },
                        onDragEnd = { fingerPosition = null },
                        onDragCancel = { fingerPosition = null }
                    )
                }
        ) {
            val gap = size.width / 4f
            val centers = (0..2).flatMap { row ->
                (0..2).map { column ->
                    Offset(gap * (column + 1), gap * (row + 1))
                }
            }
            val selectedCenters = value.mapNotNull { digit ->
                digit.digitToIntOrNull()?.takeIf { it in 1..9 }?.let { centers[it - 1] }
            }

            selectedCenters.zipWithNext().forEach { (start, end) ->
                drawLine(primaryColor, start, end, strokeWidth = 8f)
            }
            if (selectedCenters.isNotEmpty() && fingerPosition != null) {
                drawLine(
                    primaryColor.copy(alpha = 0.65f),
                    selectedCenters.last(),
                    fingerPosition!!,
                    strokeWidth = 7f
                )
            }

            centers.forEachIndexed { index, center ->
                val selected = value.contains((index + 1).toString())
                drawCircle(
                    color = if (selected) primaryColor else emptyColor,
                    radius = if (selected) 18f else 15f,
                    center = center,
                    style = if (selected) Stroke(width = 9f) else Stroke(width = 5f)
                )
                if (selected) {
                    drawCircle(primaryColor, radius = 7f, center = center)
                }
            }
        }
        TextButton(onClick = { onValueChange("") }) {
            Text("Сбросить рисунок")
        }
    }
}
