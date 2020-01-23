package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.session.MeetingSessionConfiguration
import com.amazon.chime.sdk.utils.logger.Logger

class DefaultAudioVideoController(
    private val audioClientController: AudioClientController,
    private val configuration: MeetingSessionConfiguration,
    private val logger: Logger
) : AudioVideoControllerFacade {

    private val TAG = "DefaultAudioVideoController"
    private val DEFAULT_AUDIO_HOST_PORT = 200 // Offset by 200 so that subtraction results in 0

    override fun start() {
        val audioUrlParts: List<String> =
            configuration.urls.audioHostURL.split(":".toRegex()).dropLastWhile { it.isEmpty() }

        val (host: String, portStr: String) = if (audioUrlParts.size == 2) audioUrlParts else listOf(
            audioUrlParts[0],
            "$DEFAULT_AUDIO_HOST_PORT"
        )

        // We subtract 200 here since audio client will add an offset of 200 for the DTLS port
        val port = try {
            Integer.parseInt(portStr) - DEFAULT_AUDIO_HOST_PORT
        } catch (exception: Exception) {
            logger.warn(
                TAG,
                "Error parsing int. Using default value. Exception: ${exception.message}"
            )
            0
        }

        audioClientController.start(
            host,
            port,
            configuration.meetingId,
            configuration.credentials.attendeeId,
            configuration.credentials.joinToken
        )
    }

    override fun stop() {
        audioClientController.stop()
    }

    override fun addObserver(observer: AudioVideoObserver) {
        audioClientController.subscribeAudioClientStateChange(observer)
    }

    override fun removeObserver(observer: AudioVideoObserver) {
        audioClientController.unsubscribeAudioClientStateChange(observer)
    }
}
