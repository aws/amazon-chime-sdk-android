package com.amazon.chime.sdk.media.clientcontroller

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import com.amazon.chime.sdk.R
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.session.MeetingSessionStatusCode
import com.amazon.chime.sdk.session.SessionStateControllerAction
import com.amazon.chime.sdk.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.audio.audioclient.AudioClientSignalStrengthChangeListener
import com.xodee.client.audio.audioclient.AudioClientStateChangeListener
import com.xodee.client.audio.audioclient.AudioClientVolumeStateChangeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Make singleton so that only one audio client can be created / used
class AudioClientController(private val context: Context, private val logger: Logger) {
    private val TAG = "AudioClientController"

    private val audioClient: AudioClient
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private var currentAudioState = SessionStateControllerAction.Init.value
    private var currentAudioStatus = MeetingSessionStatusCode.OK.value
    private var audioClientStateObservers = mutableSetOf<AudioVideoObserver>()
    private var volumeIndicatorCallbacks = mutableSetOf<(Map<String, Int>) -> Unit>()
    private var signalStrengthChangeCallbacks = mutableSetOf<(Map<String, Int>) -> Unit>()

    private var audioClientStateChangeListener = object : AudioClientStateChangeListener {
        override fun onAudioClientStateChange(state: Int, status: Int) {
            uiScope.launch {
                handleAudioClientStateChange(state, status)
            }
        }
    }

    private var volumeStateChangeListener = object : AudioClientVolumeStateChangeListener {
        override fun onVolumeStateChange(profile_ids: Array<out String>?, volumes: IntArray?) {
            if (profile_ids == null || volumes == null) return

            val attendeeVolumeMap = mutableMapOf<String, Int>()
            (0 until profile_ids.size).map { i -> attendeeVolumeMap[profile_ids[i]] = volumes[i] }

            volumeIndicatorCallbacks.forEach { fn -> fn(attendeeVolumeMap) }
        }
    }

    private var signalStrengthChangeListener = object : AudioClientSignalStrengthChangeListener {
        override fun onSignalStrengthChange(
            profile_ids: Array<out String>?,
            signal_strengths: IntArray?
        ) {
            if (profile_ids == null || signal_strengths == null) return

            val attendeeSignalStrengthMap = mutableMapOf<String, Int>()
            (0 until profile_ids.size).map { i ->
                attendeeSignalStrengthMap[profile_ids[i]] = signal_strengths[i]
            }

            signalStrengthChangeCallbacks.forEach { fn -> fn(attendeeSignalStrengthMap) }
        }
    }

    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("biba_media_client")

        audioClient = AudioClient(
            AudioClient.L_VERBOSE,
            context.assets,
            audioClientStateChangeListener,
            volumeStateChangeListener,
            signalStrengthChangeListener,
            0
        )
    }

    private fun handleAudioClientStateChange(newAudioState: Int, newAudioStatus: Int) {
        if (newAudioState == SessionStateControllerAction.Unknown.value) return
        if (newAudioState == currentAudioState && newAudioStatus == currentAudioStatus) return

        when (newAudioState) {
            SessionStateControllerAction.Connected.value -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting.value
                    -> forEachObserver { observer -> observer.onAudioVideoStart(false) }

                    SessionStateControllerAction.Reconnecting.value
                    -> forEachObserver { observer -> observer.onAudioVideoStart(true) }

                    SessionStateControllerAction.Connected.value -> {
                        when (newAudioStatus) {
                            MeetingSessionStatusCode.OK.value -> {
                                if (currentAudioStatus == MeetingSessionStatusCode.NetworkIsNotGoodEnoughForVoIP.value) {
                                    forEachObserver { observer -> observer.onConnectionRecovered() }
                                }
                            }
                            MeetingSessionStatusCode.NetworkIsNotGoodEnoughForVoIP.value -> {
                                if (currentAudioStatus == MeetingSessionStatusCode.OK.value) {
                                    forEachObserver { observer -> observer.onConnectionBecamePoor() }
                                }
                            }
                        }
                    }
                }
            }
            SessionStateControllerAction.DisconnectedNormal.value -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting.value,
                    SessionStateControllerAction.Connected.value
                    -> forEachObserver { observer ->
                        observer.onAudioVideoStop(
                            MeetingSessionStatus(MeetingSessionStatusCode.OK)
                        )
                    }
                    SessionStateControllerAction.Reconnecting.value ->
                        forEachObserver { observer -> observer.onAudioReconnectionCancel() }
                }
            }
            SessionStateControllerAction.DisconnectedAbnormal.value,
            SessionStateControllerAction.FailedToConnect.value -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting.value,
                    SessionStateControllerAction.Reconnecting.value,
                    SessionStateControllerAction.Connected.value
                    -> forEachObserver { observer ->
                        observer.onAudioVideoStop(
                            MeetingSessionStatus(newAudioStatus)
                        )
                    }
                }
            }
            SessionStateControllerAction.Reconnecting.value -> {
                if (currentAudioState == SessionStateControllerAction.Connected.value) {
                    forEachObserver { observer -> observer.onAudioVideoStart(true) }
                }
            }
            SessionStateControllerAction.ServerHungup.value -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting.value,
                    SessionStateControllerAction.Connected.value -> forEachObserver { observer ->
                        observer.onAudioVideoStop(MeetingSessionStatus(newAudioStatus))
                    }
                    SessionStateControllerAction.Reconnecting.value -> {
                        forEachObserver { observer ->
                            observer.onAudioVideoStop(MeetingSessionStatus(newAudioStatus))
                            observer.onAudioReconnectionCancel()
                        }
                    }
                }
            }
        }
        currentAudioState = newAudioState
        currentAudioStatus = newAudioStatus
    }

    private fun forEachObserver(observerFunction: (observer: AudioVideoObserver) -> Unit) {
        for (observer in audioClientStateObservers) {
            observerFunction(observer)
        }
    }

    private fun setUpAudioConfiguration() {
        // There seems to be no call that gives us the native input sample rate, so we just use the output rate
        val nativeSR = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM)

        // The HARDWARE_SAMPLERATE is currently used to construct the proper buffer sizes
        audioClient.sendMessage(AudioClient.MESS_SET_HARDWARE_SAMPLE_RATE, nativeSR)

        // This IO_SAMPLE_RATE is used to create OpenSLES:
        audioClient.sendMessage(AudioClient.MESS_SET_IO_SAMPLE_RATE, nativeSR)

        // Result is in bytes, so we divide by 2 (16-bit samples)
        val spkMinBufSizeInSamples = AudioTrack.getMinBufferSize(
            nativeSR,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) / 2

        val micMinBufSizeInSamples = AudioRecord.getMinBufferSize(
            nativeSR,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) / 2

        logger.info(
            TAG,
            "spkMinBufSizeInSamples $spkMinBufSizeInSamples micMinBufSizeInSamples $micMinBufSizeInSamples"
        )

        audioClient.sendMessage(AudioClient.MESS_SET_MIC_FRAMES_PER_BUFFER, micMinBufSizeInSamples)
        audioClient.sendMessage(AudioClient.MESS_SET_SPK_FRAMES_PER_BUFFER, spkMinBufSizeInSamples)
        audioClient.sendMessage(AudioClient.MESS_SET_SPEAKERPHONE_MIC, 2)
        audioClient.sendMessage(AudioClient.MESS_SET_CVP_MODULE_FLAG, AudioClient.CVP_MODULE_NONE)
        audioClient.sendMessage(AudioClient.MESS_SET_CVP_PREF_FLAG, AudioClient.CVP_PREF_NONE)
    }

    private fun setupRoute() {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
            mode = 3 // Voice communication
            isSpeakerphoneOn = true
        }
    }

    fun start(host: String, port: Int, meetingId: String, attendeeId: String, joinToken: String) {
        val hostSubStr = host.substringAfter('.').substringAfter('.')
        val audioWSUrl =
            context.getString(R.string.audio_ws_url, hostSubStr, meetingId)

        setupRoute()
        setUpAudioConfiguration()
        forEachObserver { observer -> observer.onAudioVideoStartConnecting(false) }

        // TODO: Cleanup the rest of the hard coded fields
        uiScope.launch {
            val res = audioClient.doStartSession(
                AudioClient.XTL_DEFAULT_TRANSPORT,
                host,
                port,
                joinToken,
                meetingId,
                attendeeId,
                6,
                6,
                false,
                false,
                true,
                audioWSUrl,
                null
            )

            logger.info(TAG, "Started audio session. Result code: $res")
            forEachObserver { observer -> observer.onAudioVideoStart(false) }
        }
    }

    fun stop() {
        audioClient.doStopSession()
    }

    fun subscribeToAudioClientStateChange(observer: AudioVideoObserver) {
        this.audioClientStateObservers.add(observer)
    }

    fun unsubscribeFromAudioClientStateChange(observer: AudioVideoObserver) {
        this.audioClientStateObservers.remove(observer)
    }

    fun subscribeToVolumeIndicator(callback: (Map<String, Int>) -> Unit) {
        volumeIndicatorCallbacks.add(callback)
    }

    fun unsubscribeFromVolumeIndicator(callback: (Map<String, Int>) -> Unit) {
        volumeIndicatorCallbacks.remove(callback)
    }

    fun subscribeToSignalStrengthChange(callback: (Map<String, Int>) -> Unit) {
        signalStrengthChangeCallbacks.add(callback)
    }

    fun unsubscribeFromSignalStrengthChange(callback: (Map<String, Int>) -> Unit) {
        signalStrengthChangeCallbacks.remove(callback)
    }

    fun setMute(isMuted: Boolean): Boolean {
        return AudioClient.AUDIO_CLIENT_OK == audioClient.setMicMute(isMuted)
    }
}
