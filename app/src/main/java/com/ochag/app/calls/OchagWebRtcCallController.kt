package com.ochag.app.calls

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ochag.app.data.CallSignal
import com.ochag.app.data.ServerRealtime
import org.webrtc.*
import java.util.UUID

data class CallUiState(
    val callId: String = "",
    val chatId: Long = 0L,
    val chatTitle: String = "",
    val video: Boolean = false,
    val incoming: Boolean = false,
    val caller: String = "",
    val targets: List<String> = emptyList(),
    val connected: Boolean = false
)

object OchagWebRtcCallController {
    var state by mutableStateOf<CallUiState?>(null)
        private set

    private var appContext: Context? = null
    private var factory: PeerConnectionFactory? = null
    private var eglBase: EglBase? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val peers = mutableMapOf<String, PeerConnection>()
    private var microphoneEnabled = true
    private var cameraEnabled = true

    fun initialize(context: Context) {
        if (factory != null) return
        appContext = context.applicationContext
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        eglBase = EglBase.create()
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true)
            )
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)
            )
            .createPeerConnectionFactory()
    }

    fun startCall(
        context: Context,
        chatId: Long,
        chatTitle: String,
        targets: List<String>,
        video: Boolean
    ) {
        initialize(context)
        if (targets.isEmpty()) return
        val callId = UUID.randomUUID().toString()
        state = CallUiState(
            callId = callId,
            chatId = chatId,
            chatTitle = chatTitle,
            video = video,
            targets = targets
        )
        prepareLocalMedia(video)
        ServerRealtime.sendCallSignal(
            type = "call_invite",
            callId = callId,
            chatId = chatId,
            targets = targets,
            video = video
        )
    }

    fun handleSignal(context: Context, signal: CallSignal) {
        initialize(context)
        when (signal.type) {
            "call_invite" -> {
                if (state == null) {
                    state = CallUiState(
                        callId = signal.callId,
                        chatId = signal.chatId,
                        chatTitle = signal.senderName,
                        video = signal.video,
                        incoming = true,
                        caller = signal.senderName,
                        targets = listOf(signal.senderName)
                    )
                } else {
                    ServerRealtime.sendCallSignal(
                        "call_reject", signal.callId, signal.chatId,
                        listOf(signal.senderName), signal.video
                    )
                }
            }
            "call_accept" -> {
                val current = state ?: return
                if (current.callId != signal.callId) return
                createOffer(signal.senderName)
            }
            "call_reject" -> {
                val current = state ?: return
                if (current.callId == signal.callId && current.targets.size <= 1) {
                    closeCall(false)
                }
            }
            "call_end" -> {
                if (state?.callId == signal.callId) closeCall(false)
            }
            "webrtc_offer" -> receiveOffer(signal)
            "webrtc_answer" -> receiveAnswer(signal)
            "ice_candidate" -> receiveIce(signal)
        }
    }

    fun acceptIncoming() {
        val current = state ?: return
        if (!current.incoming) return
        prepareLocalMedia(current.video)
        state = current.copy(incoming = false)
        ServerRealtime.sendCallSignal(
            "call_accept", current.callId, current.chatId,
            listOf(current.caller), current.video
        )
    }

    fun rejectIncoming() {
        val current = state ?: return
        ServerRealtime.sendCallSignal(
            "call_reject", current.callId, current.chatId,
            listOf(current.caller), current.video
        )
        closeCall(false)
    }

    fun endCall() {
        val current = state ?: return
        val recipients = (current.targets + current.caller).filter { it.isNotBlank() }.distinct()
        if (recipients.isNotEmpty()) {
            ServerRealtime.sendCallSignal(
                "call_end", current.callId, current.chatId,
                recipients, current.video
            )
        }
        closeCall(false)
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        microphoneEnabled = enabled
        audioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        cameraEnabled = enabled
        videoTrack?.setEnabled(enabled)
    }

    fun eglContext(): EglBase.Context? = eglBase?.eglBaseContext

    private fun prepareLocalMedia(video: Boolean) {
        val context = appContext ?: return
        val currentFactory = factory ?: return
        if (audioTrack == null) {
            audioSource = currentFactory.createAudioSource(MediaConstraints())
            audioTrack = currentFactory.createAudioTrack("ochag_audio", audioSource)
            audioTrack?.setEnabled(microphoneEnabled)
        }
        if (video && videoTrack == null) {
            val enumerator = Camera2Enumerator(context)
            val deviceName = enumerator.deviceNames.firstOrNull {
                enumerator.isFrontFacing(it)
            } ?: enumerator.deviceNames.firstOrNull()
            videoCapturer = deviceName?.let { enumerator.createCapturer(it, null) }
            val capturer = videoCapturer ?: return
            videoSource = currentFactory.createVideoSource(false)
            surfaceTextureHelper = SurfaceTextureHelper.create(
                "OchagVideoCapture",
                eglBase?.eglBaseContext
            )
            capturer.initialize(
                surfaceTextureHelper,
                context,
                videoSource?.capturerObserver
            )
            runCatching { capturer.startCapture(640, 480, 24) }
            videoTrack = currentFactory.createVideoTrack("ochag_video", videoSource)
            videoTrack?.setEnabled(cameraEnabled)
        }
    }

    private fun createPeer(remoteName: String): PeerConnection? {
        peers[remoteName]?.let { return it }
        val currentFactory = factory ?: return null
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                    .createIceServer()
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val peer = currentFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    if (newState == PeerConnection.IceConnectionState.CONNECTED ||
                        newState == PeerConnection.IceConnectionState.COMPLETED
                    ) {
                        state = state?.copy(connected = true)
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    val current = state ?: return
                    val item = candidate ?: return
                    ServerRealtime.sendCallSignal(
                        type = "ice_candidate",
                        callId = current.callId,
                        chatId = current.chatId,
                        targets = listOf(remoteName),
                        video = current.video,
                        candidate = item.sdp,
                        sdpMid = item.sdpMid ?: "",
                        sdpMLineIndex = item.sdpMLineIndex
                    )
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }
        ) ?: return null
        audioTrack?.let { peer.addTrack(it, listOf("ochag_stream")) }
        videoTrack?.let { peer.addTrack(it, listOf("ochag_stream")) }
        peers[remoteName] = peer
        return peer
    }

    private fun createOffer(remoteName: String) {
        val peer = createPeer(remoteName) ?: return
        peer.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(description: SessionDescription?) {
                val sdp = description ?: return
                peer.setLocalDescription(SimpleSdpObserver(), sdp)
                sendSdp("webrtc_offer", remoteName, sdp)
            }
        }, mediaConstraints())
    }

    private fun receiveOffer(signal: CallSignal) {
        val peer = createPeer(signal.senderName) ?: return
        val description = SessionDescription(SessionDescription.Type.OFFER, signal.sdp)
        peer.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                peer.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(answer: SessionDescription?) {
                        val sdp = answer ?: return
                        peer.setLocalDescription(SimpleSdpObserver(), sdp)
                        sendSdp("webrtc_answer", signal.senderName, sdp)
                    }
                }, mediaConstraints())
            }
        }, description)
    }

    private fun receiveAnswer(signal: CallSignal) {
        val peer = peers[signal.senderName] ?: return
        peer.setRemoteDescription(
            SimpleSdpObserver(),
            SessionDescription(SessionDescription.Type.ANSWER, signal.sdp)
        )
    }

    private fun receiveIce(signal: CallSignal) {
        val peer = createPeer(signal.senderName) ?: return
        peer.addIceCandidate(
            IceCandidate(signal.sdpMid, signal.sdpMLineIndex, signal.candidate)
        )
    }

    private fun sendSdp(type: String, target: String, description: SessionDescription) {
        val current = state ?: return
        ServerRealtime.sendCallSignal(
            type = type,
            callId = current.callId,
            chatId = current.chatId,
            targets = listOf(target),
            video = current.video,
            sdp = description.description,
            sdpType = description.type.canonicalForm()
        )
    }

    private fun mediaConstraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", state?.video.toString()))
    }

    private fun closeCall(clearSignal: Boolean) {
        peers.values.forEach { it.close() }
        peers.clear()
        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose()
        videoCapturer = null
        videoTrack?.dispose()
        videoTrack = null
        videoSource?.dispose()
        videoSource = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        audioTrack?.dispose()
        audioTrack = null
        audioSource?.dispose()
        audioSource = null
        state = null
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}
