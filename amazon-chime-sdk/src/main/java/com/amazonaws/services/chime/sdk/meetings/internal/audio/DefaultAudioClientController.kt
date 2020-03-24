/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultAudioClientController(
    private val logger: Logger,
    private val audioClientObserver: AudioClientObserver,
    private val audioClient: AudioClient
) : AudioClientController {
    private val TAG = "DefaultAudioClientController"
    private val DEFAULT_PORT = 0 // In case the URL does not have port
    private val AUDIO_PORT_OFFSET = 200 // Offset by 200 so that subtraction results in 0
    private val DEFAULT_MIC_AND_SPEAKER = false
    private val DEFAULT_PRESENTER = true
    private val AUDIO_CLIENT_RESULT_SUCCESS = 0

    private val uiScope = CoroutineScope(Dispatchers.Main)

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

    override fun getRoute(): Int {
        return audioClient.route
    }

    override fun setRoute(route: Int): Boolean {
        if (getRoute() == route) return true
        logger.info(TAG, "Setting route to $route")

        return audioClient.setRoute(route) == AUDIO_CLIENT_RESULT_SUCCESS
    }

    override fun start(
        audioFallbackUrl: String,
        audioHostUrl: String,
        meetingId: String,
        attendeeId: String,
        joinToken: String
    ) {
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
            DEFAULT_PORT
        }

        setUpAudioConfiguration()
        audioClientObserver.notifyAudioClientObserver { observer ->
            observer.onAudioSessionStartedConnecting(
                false
            )
        }

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
                audioFallbackUrl,
                null
            )

            if (res != AUDIO_CLIENT_RESULT_SUCCESS) {
                logger.error(TAG, "Failed to start audio session. Response code: $res")
            } else {
                logger.info(TAG, "Started audio session.")
            }
            audioClientObserver.notifyAudioClientObserver { observer ->
                observer.onAudioSessionStarted(
                    false
                )
            }
        }
    }

    override fun stop() {
        audioClient.stopSession()
    }

    override fun setMute(isMuted: Boolean): Boolean {
        return AudioClient.AUDIO_CLIENT_OK == audioClient.setMicMute(isMuted)
    }
}
