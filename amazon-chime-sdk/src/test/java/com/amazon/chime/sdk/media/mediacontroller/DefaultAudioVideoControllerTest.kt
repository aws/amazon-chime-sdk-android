package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.ClientMetricsCollector
import com.amazon.chime.sdk.media.clientcontroller.ObservableMetric
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
        override fun onAudioVideoStartConnecting(reconnecting: Boolean) {
        }

        override fun onAudioVideoStart(reconnecting: Boolean) {
        }

        override fun onAudioVideoStop(sessionStatus: MeetingSessionStatus) {
        }

        override fun onAudioReconnectionCancel() {
        }

        override fun onConnectionRecovered() {
        }

        override fun onConnectionBecamePoor() {
        }

        override fun onReceiveMetric(metrics: Map<ObservableMetric, Any>) {
        }
    }
    private val meetingSessionConfiguration = MeetingSessionConfiguration(
        "meetingId",
        MeetingSessionCredentials("attendeeId", "joinToken"),
        MeetingSessionURLs("audioHostURL")
    )

    @MockK
    private lateinit var audioClientObserver: AudioClientObserver

    @MockK
    private lateinit var audioClientController: AudioClientController

    @MockK
    private lateinit var clientMetricsCollector: ClientMetricsCollector

    private lateinit var audioVideoController: DefaultAudioVideoController

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        audioVideoController =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                clientMetricsCollector,
                meetingSessionConfiguration
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
    fun `addObserver should call audioClientObserver subscribeToAudioClientStateChange with given observer`() {
        audioVideoController.addObserver(observer)
        verify { audioClientObserver.subscribeToAudioClientStateChange(observer) }
    }

    @Test
    fun `removeObserver should call audioClientObserver unsubscribeFromAudioClientStateChange with given observer`() {
        audioVideoController.removeObserver(observer)
        verify { audioClientObserver.unsubscribeFromAudioClientStateChange(observer) }
    }

    @Test
    fun `addObserver should call clientMetricsCollector addObserver with given observer`() {
        audioVideoController.addObserver(observer)
        verify { clientMetricsCollector.addObserver(observer) }
    }

    @Test
    fun `removeObserver should call clientMetricsCollector removeObserver with given observer`() {
        audioVideoController.removeObserver(observer)
        verify { clientMetricsCollector.removeObserver(observer) }
    }
}
