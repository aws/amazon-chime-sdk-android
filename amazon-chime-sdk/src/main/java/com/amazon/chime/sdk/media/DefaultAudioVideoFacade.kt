package com.amazon.chime.sdk.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.util.Log
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.session.MeetingSessionStatusCode
import com.amazon.chime.sdk.session.SessionStateControllerAction
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.audio.audioclient.AudioClientSignalStrengthChangeListener
import com.xodee.client.audio.audioclient.AudioClientStateChangeListener
import com.xodee.client.audio.audioclient.AudioClientVolumeStateChangeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

// TODO This file is just at a POC stage now and each and very function needs to be refined. However I am going to checkin this file
// so that others can view it to see how audio is working and then each one os us can improve on it by building and polishing new APIs
class DefaultAudioVideoFacade(val context: Context, val meetingSession: String?) :
    AudioVideoFacade {
    var audioclient: AudioClient? = null
    var currentAudioState = SessionStateControllerAction.Init.value
    var currentAudioStatus = MeetingSessionStatusCode.OK.value
    var observerQueue = mutableSetOf<AudioVideoObserver>()

    var audioClientStateChangeListener = object : AudioClientStateChangeListener {
        override fun onAudioClientStateChange(state: Int, status: Int) {
            GlobalScope.launch(Dispatchers.Main) {
                handleAudioClientStateChange(state, status)
            }
        }
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

    var volumeStateChangeListener = object : AudioClientVolumeStateChangeListener {
        override fun onVolumeStateChange(profile_ids: Array<out String>?, volumes: IntArray?) {
        }
    }

    var signalStrengthChangeListener = object : AudioClientSignalStrengthChangeListener {
        override fun onSignalStrengthChange(
            profile_ids: Array<out String>?,
            signal_strengths: IntArray?
        ) {
        }
    }

    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("biba_media_client")
        audioclient = AudioClient(
            AudioClient.L_VERBOSE,
            context.assets,
            audioClientStateChangeListener,
            volumeStateChangeListener,
            signalStrengthChangeListener,
            0
        )
    }

    override fun addObserver(observer: AudioVideoObserver) {
        this.observerQueue.add(observer)
    }

    override fun removeObserver(observer: AudioVideoObserver) {
        this.observerQueue.remove(observer)
    }

    private fun forEachObserver(observerFunction: (observer: AudioVideoObserver) -> Unit) {
        for (observer in observerQueue) {
            observerFunction(observer)
        }
    }

    override fun start() {
        val jsonobj = JSONObject(meetingSession)
        val attendee = jsonobj.getJSONObject("JoinInfo").getJSONObject("Attendee")
        val meetingInfo = jsonobj.getJSONObject("JoinInfo").getJSONObject("Meeting")
        val mediaUrls = meetingInfo.getJSONObject("MediaPlacement")
        val token = attendee.getString("JoinToken")
        val attendeeId = attendee.getString("AttendeeId")
        val meetingId = meetingInfo.getString("MeetingId")
        val audioUrl = mediaUrls.getString("AudioHostUrl")
        val parts = audioUrl.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        // we subtract 200 here since audio client will add an offset of 200 for the DTLS port
        val port = if (parts.size < 2) 0 else Integer.parseInt(parts[1]) - 200
        val host = parts[0]
        val hostsub = host.substringAfter('.').substringAfter('.')
        val audiowsurl = "wss://haxrp." + hostsub + ":443/calls/" + meetingId
        setupRoute()
        setUpAudioConfiguration(context)
        forEachObserver { observer -> observer.onAudioVideoStartConnecting(false) }
        val sessionConnector = object : Thread() {
            override fun run() {
                val res = audioclient?.doStartSession(
                    AudioClient.XTL_DEFAULT_TRANSPORT,
                    host,
                    port,
                    token,
                    meetingId,
                    attendeeId,
                    6,
                    6,
                    false,
                    false,
                    true,
                    audiowsurl,
                    null
                )
                forEachObserver { observer -> observer.onAudioVideoStart(false) }
            }
        }
        sessionConnector.start()
    }

    /* do magic stuff to make call and call routing work */
    private fun setUpAudioConfiguration(context: Context) {
        // there seems to be no call that gives us the native input sample rate, so we just use the output rate
        val nativeSR = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM)
        // The HARDWARE_SAMPLERATE is currently used to construct the proper buffer sizes
        // TODO: Consolidate IO_SAMPLERATE and HARDWARE_SAMPLERATE and update the adhoc buffer size allocation in native layer
        audioclient?.sendMessage(AudioClient.MESS_SET_HARDWARE_SAMPLE_RATE, nativeSR)
        // This IO_SAMPLE_RATE is used to create OpenSLES:
        audioclient?.sendMessage(AudioClient.MESS_SET_IO_SAMPLE_RATE, nativeSR)

        // result is in bytes, so we divide by 2 (16-bit samples)
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
        Log.i(
            "DefaultAudioVideoFacade",
            "spkMinBufSizeInSamples $spkMinBufSizeInSamples micMinBufSizeInSamples $micMinBufSizeInSamples"
        )
        audioclient?.sendMessage(AudioClient.MESS_SET_MIC_FRAMES_PER_BUFFER, micMinBufSizeInSamples)
        audioclient?.sendMessage(AudioClient.MESS_SET_SPK_FRAMES_PER_BUFFER, spkMinBufSizeInSamples)
//        audioclient?.sendMessage(
//            AudioClient.MESS_SET_SPEAKERPHONE_MIC,
//            ConfigureAudio.getOpenSLMicForSpeakerphonePref(context)
//        )

        audioclient?.sendMessage(
            AudioClient.MESS_SET_SPEAKERPHONE_MIC,
            2
        )

        // turn on cvp features as needed.
//        val cvpEnable = XodeePreferences.getInstance()
//            .preferenceIsTrue(context, XodeePreferences.PREFERENCE_CVP_ENABLE)
        val cvpEnable = false
        var cvpFlags =
            if (cvpEnable) AudioClient.CVP_MODULE_DELAY_ESTIMATOR_V2 or AudioClient.CVP_MODULE_SUBBAND_AEC or AudioClient.CVP_MODULE_NOISE_SUPPRESSOR or AudioClient.CVP_MODULE_HALF_DUPLEX else AudioClient.CVP_MODULE_NONE
//        val isDelayEstimatorV2Enabled = Feature.isAudioDelayEstimatorV2EnabledForUser(context)
        val isDelayEstimatorV2Enabled = false
        if (isDelayEstimatorV2Enabled) {
            cvpFlags = cvpFlags or AudioClient.CVP_MODULE_DELAY_ESTIMATOR_V2
        }
//        val isAudioDebugRecordingEnabled = Feature.isAudioDebugRecordingEnabledForUser(context)
        val isAudioDebugRecordingEnabled = false
        if (isAudioDebugRecordingEnabled) {
            cvpFlags = cvpFlags or AudioClient.CVP_MODULE_DEBUG_RECORDING
        }
//        val isAudioCvpAecEnabled = Feature.isAudioCvpAecEnabledForUser(context)
        val isAudioCvpAecEnabled = false
        if (isAudioCvpAecEnabled) {
            cvpFlags = cvpFlags or AudioClient.CVP_MODULE_SUBBAND_AEC
        }
        audioclient?.sendMessage(AudioClient.MESS_SET_CVP_MODULE_FLAG, cvpFlags)

        // turn on cvp preference (e.g., disable gain auto-adjustment) as needed.
//        val cvpPrefEnable = XodeePreferences.getInstance()
//            .preferenceIsTrue(context, XodeePreferences.PREFERENCE_CVP_PREF_ENABLE)
        val cvpPrefEnable = false
        val cvpPrefFlags =
            if (cvpPrefEnable) AudioClient.CVP_PREF_AUTOGAIN_OFF else AudioClient.CVP_PREF_NONE
        audioclient?.sendMessage(AudioClient.MESS_SET_CVP_PREF_FLAG, cvpPrefFlags)
    }

    private fun setupRoute() {
        var audioManager: AudioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var speakerphonePreCall = audioManager.isSpeakerphoneOn()
        var audioModePreCall = audioManager.getMode()

        // initial call settings

        audioManager.setMode(3)/* voice communication */

        audioManager.setSpeakerphoneOn(true)
    }

    /* do magic stuff to make call and call routing work */

    override fun stop() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
