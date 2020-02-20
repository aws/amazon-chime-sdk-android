package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.session.MeetingSessionConfiguration
import com.amazon.chime.sdk.session.MeetingSessionCredentials
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.session.MeetingSessionURLs
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DefaultAudioVideoControllerTest {
    private val observer = object : AudioVideoObserver {

        override fun onAudioClientConnecting(reconnecting: Boolean) {
        }

        override fun onAudioClientStart(reconnecting: Boolean) {
        }

        override fun onAudioClientStop(sessionStatus: MeetingSessionStatus) {
        }

        override fun onAudioClientReconnectionCancel() {
        }

        override fun onConnectionRecover() {
        }

        override fun onConnectionBecomePoor() {
        }

        override fun onVideoClientStart() {
        }

        override fun onVideoClientStop(sessionStatus: MeetingSessionStatus) {
        }

        override fun onVideoClientConnecting() {
        }
    }
    private val meetingSessionConfiguration = MeetingSessionConfiguration(
        "meetingId",
        MeetingSessionCredentials("attendeeId", "joinToken"),
        MeetingSessionURLs("audioHostURL", "turnControlURL", "signalingURL")
    )

    @MockK
    private lateinit var audioClientObserver: AudioClientObserver

    @MockK
    private lateinit var audioClientController: AudioClientController

    @MockK
    private lateinit var videoClientController: VideoClientController

    private lateinit var audioVideoController: DefaultAudioVideoController

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        audioVideoController =
            DefaultAudioVideoController(
                meetingSessionConfiguration,
                audioClientController,
                audioClientObserver,
                videoClientController

            )
    }

    @Test
    fun `start should call audioClientController start with the parameters in configuration`() {
        audioVideoController.start()
        verify {
            audioClientController.start(
                "audioHostURL",
                "meetingId",
                "attendeeId",
                "joinToken"
            )
        }
    }

    @Test
    fun `stop should call audioClientController stop`() {
        audioVideoController.stop()
        verify { audioClientController.stop() }
    }

    @Test
    fun `startLocalVideo should call videoClientController enableSelfVideo`() {
        audioVideoController.startLocalVideo()

        verify { videoClientController.enableSelfVideo(true) }
    }

    @Test
    fun `stopLocalVideo should call videoClientController enableSelfVideo`() {
        audioVideoController.stopLocalVideo()

        verify { videoClientController.enableSelfVideo(false) }
    }

    @Test
    fun `addObserver should call audioClientObserver subscribeToAudioClientStateChange with given observer`() {
        audioVideoController.addObserver(observer)
        verify { audioClientObserver.subscribeToAudioClientStateChange(observer) }
    }

    @Test
    fun `removeObserver should call audioClientObserver unsubscribeFromAudioClientStateChange with given observer`() {
        audioVideoController.removeObserver(observer)
        verify { audioClientObserver.unsubscribeFromAudioClientStateChange(observer) }
    }
}
