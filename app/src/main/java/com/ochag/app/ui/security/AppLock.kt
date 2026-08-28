package com.ochag.app.ui.security

import android.content.Context
import java.security.MessageDigest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

enum class AppLockMode { PIN, PASSWORD, PATTERN }

object AppLockController {
    private const val PREFS = "ochag_app_lock"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_MODE = "mode"
    private const val KEY_SECRET_HASH = "secret_hash"

    private var context: Context? = null
    var enabled by mutableStateOf(false)
        private set
    var locked by mutableStateOf(false)
        private set
    var mode by mutableStateOf(AppLockMode.PIN)
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(KEY_ENABLED, false)
        mode = runCatching {
            AppLockMode.valueOf(prefs.getString(KEY_MODE, AppLockMode.PIN.name).orEmpty())
        }.getOrDefault(AppLockMode.PIN)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        (0..2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                (1..3).forEach { column ->
                    val number = row * 3 + column
                    val selected = value.contains(number.toString())
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                if (!selected) onValueChange(value + number)
                            },
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                if (selected) "●" else "○",
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        TextButton(onClick = { onValueChange("") }) {
            Text("Сбросить рисунок")
        }
    }
}
