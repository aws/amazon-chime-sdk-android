package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.session.MeetingSessionConfiguration

class DefaultAudioVideoController(
    private val configuration: MeetingSessionConfiguration,
    private val audioClientController: AudioClientController,
    private val audioClientObserver: AudioClientObserver,
    private val videoClientController: VideoClientController
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

    override fun startLocalVideo() {
        videoClientController.enableSelfVideo(true)
    }

    override fun stopLocalVideo() {
        videoClientController.enableSelfVideo(false)
    }

    override fun addObserver(observer: AudioVideoObserver) {
        audioClientObserver.subscribeToAudioClientStateChange(observer)
        videoClientController.subscribeToVideoClientStateChange(observer)
    }

    override fun removeObserver(observer: AudioVideoObserver) {
        audioClientObserver.unsubscribeFromAudioClientStateChange(observer)
        videoClientController.unsubscribeFromVideoClientStateChange(observer)
    }
}
