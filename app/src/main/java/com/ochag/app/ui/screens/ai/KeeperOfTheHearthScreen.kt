package com.ochag.app.ui.screens.ai

import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private data class FreeAiService(
    val name: String,
    val description: String,
    val url: String,
    val symbol: String
)

private val freeAiServices = listOf(
    FreeAiService(
        name = "ChatGPT",
        description = "Тексты, идеи, объяснения и помощь с задачами",
        url = "https://chatgpt.com",
        symbol = "◎"
    ),
    FreeAiService(
        name = "Google Gemini",
        description = "Поиск идей, работа с текстом и изображениями",
        url = "https://gemini.google.com",
        symbol = "✦"
    ),
    FreeAiService(
        name = "Microsoft Copilot",
        description = "Ответы, поиск и создание изображений",
        url = "https://copilot.microsoft.com",
        symbol = "◈"
    ),
    FreeAiService(
        name = "Perplexity",
        description = "Поиск ответов в интернете с источниками",
        url = "https://www.perplexity.ai",
        symbol = "P"
    ),
    FreeAiService(
        name = "Claude",
        description = "Работа с большими текстами и документами",
        url = "https://claude.ai",
        symbol = "C"
    )
)

@Composable
fun KeeperOfTheHearthScreen(
    onBackClick: () -> Unit
) {
    var selectedService by remember { mutableStateOf<FreeAiService?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val view = webView
        when {
            selectedService != null && view?.canGoBack() == true -> view.goBack()
            selectedService != null -> {
                webView = null
                selectedService = null
            }
            else -> onBackClick()
        }
    }

    if (selectedService == null) {
        AiServiceChooser(
            onBackClick = onBackClick,
            onOpenInside = { selectedService = it }
        )
    } else {
        AiWebView(
            service = selectedService!!,
            onBackClick = {
                webView = null
                selectedService = null
            },
            onWebViewReady = { webView = it }
        )
    }
}

@Composable
private fun AiServiceChooser(
    onBackClick: () -> Unit,
    onOpenInside: (FreeAiService) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) { Text("‹ Назад") }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Хранитель Очага", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Выберите бесплатную нейросеть",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("✨", style = MaterialTheme.typography.titleLarge)
            }
        }

        Text(
            text = "Нейросеть можно открыть прямо в «Очаге» или отдельно в браузере/её приложении.",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(freeAiServices) { service ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.size(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(service.symbol, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(service.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    service.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onOpenInside(service) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("В Очаге")
                            }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(service.url))
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Открыть отдельно")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiWebView(
    service: FreeAiService,
    onBackClick: () -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) { Text("‹ Выбор") }
                Column(modifier = Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleLarge)
                    Text("Хранитель Очага", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(service.url))
                        )
                    }
                ) {
                    Text("Отдельно")
                }
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.setSupportZoom(false)
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    loadUrl(service.url)
                    onWebViewReady(this)
                }
            },
            update = { onWebViewReady(it) }
        )
    }
}
