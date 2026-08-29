package com.ochag.app.calls

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ochag.app.ui.localization.ochagText
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import kotlinx.coroutines.delay

@Composable
fun OchagCallOverlay() {
    val call = OchagWebRtcCallController.state ?: return
    var microphoneEnabled by remember(call.callId) { mutableStateOf(true) }
    var cameraEnabled by remember(call.callId) { mutableStateOf(call.video) }
    var elapsedSeconds by remember(call.callId) { mutableLongStateOf(0L) }

    LaunchedEffect(call.callId, call.connected, call.connectedAtMillis) {
        if (!call.connected) {
            delay(45_000L)
            val active = OchagWebRtcCallController.state
            if (active?.callId == call.callId && !active.connected) {
                OchagWebRtcCallController.endCall()
            }
        } else {
            while (OchagWebRtcCallController.state?.callId == call.callId) {
                elapsedSeconds =
                    ((System.currentTimeMillis() - call.connectedAtMillis) / 1000L)
                        .coerceAtLeast(0L)
                delay(1_000L)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == false) {
            OchagWebRtcCallController.rejectIncoming()
        } else if (call.incoming) {
            OchagWebRtcCallController.acceptIncoming()
        }
    }

    if (call.incoming) {
        AlertDialog(
            onDismissRequest = { OchagWebRtcCallController.rejectIncoming() },
            title = {
                Text(
                    if (call.video) ochagText("Входящий видеозвонок")
                    else ochagText("Входящий аудиозвонок")
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                call.caller.firstOrNull()?.uppercase() ?: "О",
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(call.caller, style = MaterialTheme.typography.titleLarge)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val permissions = if (call.video) {
                            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                        } else {
                            arrayOf(Manifest.permission.RECORD_AUDIO)
                        }
                        permissionLauncher.launch(permissions)
                    }
                ) {
                    Text(ochagText("Принять"))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { OchagWebRtcCallController.rejectIncoming() }
                ) {
                    Text(ochagText("Отклонить"))
                }
            }
        )
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        color = Color.Black
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(
                text = if (call.connected) {
                    formatCallDuration(elapsedSeconds)
                } else {
                    ochagText("Соединение…")
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            if (call.video) {
                val remote = OchagWebRtcCallController.remoteVideoTrack
                if (remote != null) {
                    WebRtcVideoView(
                        track = remote,
                        mirror = false,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (call.connected) ochagText("Ожидание видео…")
                            else ochagText("Подключение к звонку…"),
                            color = Color.White
                        )
                    }
                }

                val local = OchagWebRtcCallController.localVideoTrack()
                if (local != null) {
                    WebRtcVideoView(
                        track = local,
                        mirror = true,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .width(116.dp)
                            .height(164.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(112.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                call.chatTitle.firstOrNull()?.uppercase() ?: "О",
                                style = MaterialTheme.typography.displayMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(call.chatTitle, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (call.connected) ochagText("Соединено")
                        else ochagText("Подключение к звонку…"),
                        color = Color.LightGray
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        microphoneEnabled = !microphoneEnabled
                        OchagWebRtcCallController.setMicrophoneEnabled(microphoneEnabled)
                    },
                    modifier = Modifier.size(58.dp)
                ) {
                    Text(if (microphoneEnabled) "🎙" else "×🎙")
                }
                if (call.video) {
                    FilledTonalIconButton(
                        onClick = {
                            cameraEnabled = !cameraEnabled
                            OchagWebRtcCallController.setCameraEnabled(cameraEnabled)
                        },
                        modifier = Modifier.size(58.dp)
                    ) {
                        Text(if (cameraEnabled) "▰" else "×▰")
                    }
                }
                Button(
                    onClick = { OchagWebRtcCallController.endCall() },
                    modifier = Modifier.size(66.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("☎", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun WebRtcVideoView(
    track: VideoTrack,
    mirror: Boolean,
    modifier: Modifier
) {
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).also { view ->
                view.init(OchagWebRtcCallController.eglContext(), null)
                view.setMirror(mirror)
                view.setEnableHardwareScaler(true)
                renderer = view
                track.addSink(view)
            }
        },
        update = { it.setMirror(mirror) }
    )
    DisposableEffect(track, renderer) {
        onDispose {
            renderer?.let {
                track.removeSink(it)
                it.release()
            }
        }
    }
}


private fun formatCallDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
