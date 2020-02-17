package com.amazon.chime.sdk.media.clientcontroller

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import com.amazon.chime.sdk.R
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.session.MeetingSessionStatusCode
import com.amazon.chime.sdk.session.SessionStateControllerAction
import com.amazon.chime.sdk.utils.logger.Logger
import com.amazon.chime.sdk.utils.singleton.SingletonWithParams
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.audio.audioclient.AudioClientLogListener
import com.xodee.client.audio.audioclient.AudioClientSignalStrengthChangeListener
import com.xodee.client.audio.audioclient.AudioClientStateChangeListener
import com.xodee.client.audio.audioclient.AudioClientVolumeStateChangeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This is so that we only need one version of [[SingletonWithParams]].
 */
data class AudioClientControllerParams(val context: Context, val logger: Logger)

/**
 * Singleton to prevent more than one [[AudioClient]] from being created. Normally we'd use object
 * but this class requires parameters for initialization and object currently does not support that.
 *
 * Instead, we use a companion object extending [[SingletonWithParams]] and taking
 * [[AudioClientControllerParams]] to create and retrieve an instance of [[AudioClientController]]
 */
class AudioClientController private constructor(params: AudioClientControllerParams) {
    private val context: Context = params.context
    private val logger: Logger = params.logger

    private val TAG = "AudioClientController"
    private val AUDIO_PORT_OFFSET = 200 // Offset by 200 so that subtraction results in 0
    private val DEFAULT_MIC_AND_SPEAKER = false
    private val DEFAULT_PRESENTER = true
    private val AUDIO_CLIENT_RESULT_SUCCESS = 0

    /**
     * These are not present in [AudioClient], so defining it here
     * TODO: Add it to AudioClient
     */
    private val AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE = 65
    private val AUDIO_CLIENT_ERR_CALL_ENDED = 75

    private val audioClient: AudioClient
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private var currentAudioState = SessionStateControllerAction.Init
    private var currentAudioStatus: MeetingSessionStatusCode? = MeetingSessionStatusCode.OK
    private var audioClientStateObservers = mutableSetOf<AudioVideoObserver>()
    private var realtimeEventObservers = mutableSetOf<RealtimeObserver>()

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
            (profile_ids.indices).map { i -> attendeeVolumeMap[profile_ids[i]] = volumes[i] }
            realtimeEventObservers.forEach { it.onVolumeChange(attendeeVolumeMap) }
        }
    }

    private var signalStrengthChangeListener = object : AudioClientSignalStrengthChangeListener {
        override fun onSignalStrengthChange(
            profile_ids: Array<out String>?,
            signal_strengths: IntArray?
        ) {
            if (profile_ids == null || signal_strengths == null) return

            val attendeeSignalStrengthMap = mutableMapOf<String, Int>()
            (profile_ids.indices).map { i ->
                attendeeSignalStrengthMap[profile_ids[i]] = signal_strengths[i]
            }

            realtimeEventObservers.forEach { it.onSignalStrengthChange(attendeeSignalStrengthMap) }
        }
    }

    private var logListener = object : AudioClientLogListener {
        override fun onLogMessage(logLevel: Int, message: String?) {
            if (message == null) return

            // Only print error and fatal as the median team's request to avoid noise
            // Will be changed back to respect logger settings once sanitize the logs
            if (logLevel == AudioClient.L_ERROR || logLevel == AudioClient.L_FATAL) {
                logger.error(TAG, message)
            }
        }
    }

    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("biba_media_client")

        audioClient = AudioClient(
            context.assets,
            audioClientStateChangeListener,
            volumeStateChangeListener,
            signalStrengthChangeListener,
            logListener,
            0
        )
    }

    companion object :
        SingletonWithParams<AudioClientController, AudioClientControllerParams>(::AudioClientController)

    // Tincan's integer values do not map 1:1 to our integer values
    private fun toAudioClientState(internalAudioClientState: Int): SessionStateControllerAction {
        return when (internalAudioClientState) {
            AudioClient.AUDIO_CLIENT_STATE_UNKNOWN -> SessionStateControllerAction.Unknown
            AudioClient.AUDIO_CLIENT_STATE_INIT -> SessionStateControllerAction.Init
            AudioClient.AUDIO_CLIENT_STATE_CONNECTING -> SessionStateControllerAction.Connecting
            AudioClient.AUDIO_CLIENT_STATE_CONNECTED -> SessionStateControllerAction.FinishConnecting
            AudioClient.AUDIO_CLIENT_STATE_RECONNECTING -> SessionStateControllerAction.Reconnecting
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING -> SessionStateControllerAction.Disconnecting
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL -> SessionStateControllerAction.FinishDisconnecting
            AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
            AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP -> SessionStateControllerAction.Fail
            else -> SessionStateControllerAction.Unknown
        }
    }

    private fun toAudioStatus(internalAudioStatus: Int): MeetingSessionStatusCode? {
        return when (internalAudioStatus) {
            AudioClient.AUDIO_CLIENT_OK -> MeetingSessionStatusCode.OK
            AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP -> MeetingSessionStatusCode.NetworkBecamePoor
            AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP -> MeetingSessionStatusCode.AudioDisconnected
            AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE -> MeetingSessionStatusCode.AudioJoinedFromAnotherDevice
            AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR -> MeetingSessionStatusCode.AudioInternalServerError
            AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED -> MeetingSessionStatusCode.AudioAuthenticationRejected
            AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY -> MeetingSessionStatusCode.AudioCallAtCapacity
            AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE -> MeetingSessionStatusCode.AudioServiceUnavailable
            AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO -> MeetingSessionStatusCode.AudioDisconnectAudio
            AUDIO_CLIENT_ERR_CALL_ENDED -> MeetingSessionStatusCode.AudioCallEnded
            else -> null
        }
    }

    private fun handleAudioClientStateChange(
        newInternalAudioState: Int,
        newInternalAudioStatus: Int
    ) {
        val newAudioState: SessionStateControllerAction = toAudioClientState(newInternalAudioState)
        val newAudioStatus: MeetingSessionStatusCode? = toAudioStatus(newInternalAudioStatus)

        if (newAudioState == SessionStateControllerAction.Unknown) return
        if (newAudioState == currentAudioState && newAudioStatus == currentAudioStatus) return

        when (newAudioState) {
            SessionStateControllerAction.FinishConnecting -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting ->
                        forEachObserver { observer -> observer.onAudioVideoStart(false) }
                    SessionStateControllerAction.Reconnecting ->
                        forEachObserver { observer -> observer.onAudioVideoStart(true) }
                    SessionStateControllerAction.FinishConnecting ->
                        when (newAudioStatus) {
                            MeetingSessionStatusCode.OK ->
                                if (currentAudioStatus == MeetingSessionStatusCode.NetworkBecamePoor) {
                                    forEachObserver { observer -> observer.onConnectionRecovered() }
                                }
                            MeetingSessionStatusCode.NetworkBecamePoor ->
                                if (currentAudioStatus == MeetingSessionStatusCode.OK) {
                                    forEachObserver { observer -> observer.onConnectionBecamePoor() }
                                }
                        }
                }
            }
            SessionStateControllerAction.Reconnecting -> {
                if (currentAudioState == SessionStateControllerAction.FinishConnecting) {
                    forEachObserver { observer -> observer.onAudioVideoStart(true) }
                }
            }
            SessionStateControllerAction.FinishDisconnecting -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting,
                    SessionStateControllerAction.FinishConnecting ->
                        forEachObserver { observer ->
                            observer.onAudioVideoStop(
                                MeetingSessionStatus(MeetingSessionStatusCode.OK)
                            )
                        }
                    SessionStateControllerAction.Reconnecting ->
                        forEachObserver { observer -> observer.onAudioReconnectionCancel() }
                }
            }
            SessionStateControllerAction.Fail -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting,
                    SessionStateControllerAction.FinishConnecting ->
                        forEachObserver { observer ->
                            observer.onAudioVideoStop(MeetingSessionStatus(newAudioStatus))
                        }
                    SessionStateControllerAction.Reconnecting ->
                        forEachObserver { observer ->
                            observer.onAudioReconnectionCancel()
                            observer.onAudioVideoStop(MeetingSessionStatus(newAudioStatus))
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

    fun getRoute(): Int {
        return audioClient.route
    }

    fun setRoute(route: Int): Boolean {
        if (getRoute() == route) return true
        logger.info(TAG, "Setting route to $route")

        return audioClient.setRoute(route) == AUDIO_CLIENT_RESULT_SUCCESS
    }

    fun start(audioHostUrl: String, meetingId: String, attendeeId: String, joinToken: String) {
        val audioUrlParts: List<String> =
            audioHostUrl.split(":".toRegex()).dropLastWhile { it.isEmpty() }

        val (host: String, portStr: String) = if (audioUrlParts.size == 2) audioUrlParts else listOf(
            audioUrlParts[0],
            "$AUDIO_PORT_OFFSET"
        )

        // We subtract 200 here since audio client will add an offset of 200 for the DTLS port
        val port = try {
            Integer.parseInt(portStr) - AUDIO_PORT_OFFSET
        } catch (exception: Exception) {
            logger.warn(
                TAG,
                "Error parsing int. Using default value. Exception: ${exception.message}"
            )
            0
        }

        val hostSubStr = host.substringAfter('.').substringAfter('.')
        val audioWSUrl =
            context.getString(R.string.audio_ws_url, hostSubStr, meetingId)

        setUpAudioConfiguration()
        forEachObserver { observer -> observer.onAudioVideoStartConnecting(false) }

        uiScope.launch {
            val res = audioClient.startSession(
                AudioClient.XTL_DEFAULT_TRANSPORT,
                host,
                port,
                joinToken,
                meetingId,
                attendeeId,
                AudioClient.kCodecOpusLow,
                AudioClient.kCodecOpusLow,
                DEFAULT_MIC_AND_SPEAKER,
                DEFAULT_MIC_AND_SPEAKER,
                DEFAULT_PRESENTER,
                audioWSUrl,
                null
            )

            if (res != AUDIO_CLIENT_RESULT_SUCCESS) {
                logger.error(TAG, "Failed to start audio session. Response code: $res")
            } else {
                logger.info(TAG, "Started audio session.")
            }
            forEachObserver { observer -> observer.onAudioVideoStart(false) }
        }
    }

    fun stop() {
        audioClient.stopSession()
    }

    fun subscribeToAudioClientStateChange(observer: AudioVideoObserver) {
        this.audioClientStateObservers.add(observer)
    }

    fun unsubscribeFromAudioClientStateChange(observer: AudioVideoObserver) {
        this.audioClientStateObservers.remove(observer)
    }

    fun subscribeToRealTimeEvents(observer: RealtimeObserver) {
        realtimeEventObservers.add(observer)
    }

    fun unsubscribeFromRealTimeEvents(observer: RealtimeObserver) {
        realtimeEventObservers.remove(observer)
    }

    fun setMute(isMuted: Boolean): Boolean {
        return AudioClient.AUDIO_CLIENT_OK == audioClient.setMicMute(isMuted)
    }
}
