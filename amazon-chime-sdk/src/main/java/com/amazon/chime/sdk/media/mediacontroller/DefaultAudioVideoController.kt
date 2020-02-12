package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.session.MeetingSessionConfiguration

class DefaultAudioVideoController(
    private val audioClientController: AudioClientController,
    private val videoClientController: VideoClientController,
    private val configuration: MeetingSessionConfiguration
) : AudioVideoControllerFacade {

    override fun start() {
        audioClientController.start(
            configuration.urls.audioHostURL,
            configuration.meetingId,
            configuration.credentials.attendeeId,
            configuration.credentials.joinToken
        )
        videoClientController.start(
            configuration.urls.turnControlURL,
            configuration.urls.signalingURL,
            configuration.meetingId,
            configuration.credentials.joinToken,
            false
        )
    }

    override fun stop() {
        audioClientController.stop()
        videoClientController.stopAndDestroy()
    }

    override fun addObserver(observer: AudioVideoObserver) {
        audioClientController.subscribeToAudioClientStateChange(observer)
    }

    override fun removeObserver(observer: AudioVideoObserver) {
        audioClientController.unsubscribeFromAudioClientStateChange(observer)
    }
}
