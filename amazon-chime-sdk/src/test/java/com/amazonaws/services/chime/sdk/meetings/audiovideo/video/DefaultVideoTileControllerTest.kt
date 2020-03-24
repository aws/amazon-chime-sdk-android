/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazon.chime.webrtc.VideoRenderer
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DefaultVideoTileControllerTest {
    @MockK
    private lateinit var mockVideoTileFactory: VideoTileFactory

    @MockK
    private lateinit var mockVideoTileController: VideoClientController

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockVideoTile: VideoTile

    @MockK
    private lateinit var mockVideoRenderView: VideoRenderView

    @MockK
    private lateinit var mockFrame: VideoRenderer.I420Frame

    @InjectMockKs
    private lateinit var videoTileController: DefaultVideoTileController

    // See https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test for more examples
    private val testDispatcher = TestCoroutineDispatcher()

    private val tileId = 7 // some random prime number
    private val attendeeId = "chimesarang"

    private var onAddObserverCalled = 0
    private var onRemoveObserverCalled = 0
    private var onPauseObserverCalled = 0
    private var onResumeObserverCalled = 0

    private val tileObserver = object : VideoTileObserver {
        override fun onVideoTileAdded(tileState: VideoTileState) {
            onAddObserverCalled++
        }

        override fun onVideoTileRemoved(tileState: VideoTileState) {
            onRemoveObserverCalled++
        }

        override fun onVideoTilePaused(tileState: VideoTileState) {
            onPauseObserverCalled++
        }

        override fun onVideoTileResumed(tileState: VideoTileState) {
            onResumeObserverCalled++
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockVideoTileFactory.makeTile(tileId, any()) } returns mockVideoTile
        every { mockVideoTile.state } returns VideoTileState(
            tileId,
            attendeeId,
            VideoPauseState.Unpaused
        )
        every { mockVideoTile.videoRenderView } returns mockVideoRenderView

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `unbindVideoView should call finalize on VideoRenderView`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }
        videoTileController.bindVideoView(mockVideoRenderView, tileId)
        videoTileController.unbindVideoView(tileId)

        verify { mockVideoTile.unbind() }
    }

    @Test
    fun `onReceiveFrame should call renderFrame on VideoRenderView when bound`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }
        videoTileController.bindVideoView(mockVideoRenderView, tileId)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }

        verify { mockVideoTile.renderFrame(any()) }
    }

    @Test
    fun `onReceiveFrame should NOT notify observer about video tile add when observer has been removed`() {
        videoTileController.addVideoTileObserver(tileObserver)
        videoTileController.removeVideoTileObserver(tileObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }

        Assert.assertEquals(0, onAddObserverCalled)
    }

    @Test
    fun `onReceiveFrame should NOT notify observer about video tile remove when observer has been removed`() {
        videoTileController.addVideoTileObserver(tileObserver)
        videoTileController.removeVideoTileObserver(tileObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }

        Assert.assertEquals(0, onAddObserverCalled)
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile add with non-null frame and new tileId`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }

        Assert.assertEquals(1, onAddObserverCalled)
        verify {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    VideoPauseState.Unpaused
                )
            )
        }
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile remove with null frame and old tileId`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
            videoTileController.onReceiveFrame(null, attendeeId, VideoPauseState.Unpaused, tileId)
        }

        Assert.assertEquals(1, onRemoveObserverCalled)
        verify {
            mockObserver.onVideoTileRemoved(
                VideoTileState(
                    tileId,
                    attendeeId,
                    VideoPauseState.Unpaused
                )
            )
        }
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile pause when pause state is changed to paused`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.PausedForPoorConnection,
                tileId
            )
        }

        verify { mockVideoTile.setPauseState(VideoPauseState.PausedForPoorConnection) }
        // Mock video tile state will be equal to Unpaused
        verify(exactly = 1) {
            mockObserver.onVideoTilePaused(
                VideoTileState(
                    tileId,
                    attendeeId,
                    VideoPauseState.Unpaused
                )
            )
        }
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile resume when pause state is changed from paused to unpaused`() {
        val mockObserver = spyk(tileObserver)
        every { mockVideoTile.state } returns VideoTileState(
            tileId,
            attendeeId,
            VideoPauseState.PausedForPoorConnection
        )

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.PausedForPoorConnection,
                tileId
            )
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }

        verify { mockVideoTile.setPauseState(VideoPauseState.Unpaused) }
        // Mock video tile state will be equal to PausedForPoorConnection
        verify(exactly = 1) {
            mockObserver.onVideoTileResumed(
                VideoTileState(
                    tileId,
                    attendeeId,
                    VideoPauseState.PausedForPoorConnection
                )
            )
        }
    }

    @Test
    fun `pauseRemoteVideoTile should call VideoClientController's setRemotePaused when tile exists`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }
        videoTileController.pauseRemoteVideoTile(tileId)

        verify { mockVideoTileController.setRemotePaused(true, tileId) }
    }

    @Test
    fun `pauseRemoteVideoTile should notify observer about video tile pause when tile exists`() {
        val mockObserver = spyk(tileObserver)
        videoTileController.addVideoTileObserver(mockObserver)

        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
            videoTileController.pauseRemoteVideoTile(tileId)
        }

        // Mock video tile state will be equal to Unpaused
        verify(exactly = 1) {
            mockObserver.onVideoTilePaused(
                VideoTileState(
                    tileId,
                    attendeeId,
                    VideoPauseState.Unpaused
                )
            )
        }
    }

    @Test
    fun `resumeRemoteVideoTile should call VideoClientController's setRemotePaused when tile exists`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
        }
        videoTileController.resumeRemoteVideoTile(tileId)

        verify { mockVideoTileController.setRemotePaused(false, tileId) }
    }

    @Test
    fun `resumeRemoteVideoTile should notify observer about video tile resume when tile exists`() {
        val mockObserver = spyk(tileObserver)
        every { mockVideoTile.state } returns VideoTileState(
            tileId,
            attendeeId,
            VideoPauseState.PausedByUserRequest
        )
        videoTileController.addVideoTileObserver(mockObserver)

        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.PausedByUserRequest,
                tileId
            )
            videoTileController.resumeRemoteVideoTile(tileId)
        }

        // Mock video tile state will be equal to PausedByUserRequest
        verify(exactly = 1) {
            mockObserver.onVideoTileResumed(
                VideoTileState(
                    tileId,
                    attendeeId,
                    VideoPauseState.PausedByUserRequest
                )
            )
        }
    }

    @Test
    fun `pauseRemoteVideoTile should NOT call VideoClientController's setRemotePaused when tile does not exist`() {
        videoTileController.pauseRemoteVideoTile(tileId)

        verify(exactly = 0) { mockVideoTileController.setRemotePaused(true, tileId) }
    }

    @Test
    fun `pauseRemoteVideoTile should NOT notify observer about video tile pause when tile does not exist`() {
        videoTileController.addVideoTileObserver(tileObserver)
        videoTileController.pauseRemoteVideoTile(tileId)

        Assert.assertEquals(0, onPauseObserverCalled)
    }

    @Test
    fun `pauseRemoteVideoTile should NOT call VideoClientController's setRemotePaused when local tile`() {
        every { mockVideoTile.state } returns VideoTileState(tileId, null, VideoPauseState.Unpaused)
        runBlockingTest {
            videoTileController.onReceiveFrame(mockFrame, null, VideoPauseState.Unpaused, tileId)
        }

        videoTileController.pauseRemoteVideoTile(tileId)

        verify(exactly = 0) { mockVideoTileController.setRemotePaused(any(), any()) }
    }

    @Test
    fun `resumeRemoteVideoTile should NOT call VideoClientController's setRemotePaused when tile does not exist`() {
        videoTileController.resumeRemoteVideoTile(tileId)

        verify(exactly = 0) { mockVideoTileController.setRemotePaused(false, tileId) }
    }

    @Test
    fun `resumeRemoteVideoTile should NOT notify observer about video tile resume when tile does not exists`() {
        videoTileController.addVideoTileObserver(tileObserver)
        videoTileController.resumeRemoteVideoTile(tileId)

        Assert.assertEquals(0, onResumeObserverCalled)
    }

    @Test
    fun `resumeRemoteVideoTile should NOT call VideoClientController's setRemotePaused when local tile`() {
        every { mockVideoTile.state } returns VideoTileState(tileId, null, VideoPauseState.Unpaused)
        runBlockingTest {
            videoTileController.onReceiveFrame(mockFrame, null, VideoPauseState.Unpaused, tileId)
        }

        videoTileController.resumeRemoteVideoTile(tileId)

        verify(exactly = 0) { mockVideoTileController.setRemotePaused(any(), any()) }
    }

    @Test
    fun `bind should unbind the view first when already bound`() {
        val mockVideoTile2: VideoTile = mockk(relaxUnitFun = true)
        val tileId2 = 127

        every { mockVideoTileFactory.makeTile(tileId2, any()) } returns mockVideoTile2
        every { mockVideoTile2.state } returns VideoTileState(
            tileId2,
            attendeeId,
            VideoPauseState.Unpaused
        )
        every { mockVideoTile2.videoRenderView } returns mockVideoRenderView
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId2
            )
        }

        videoTileController.bindVideoView(mockVideoRenderView, tileId)
        videoTileController.bindVideoView(mockVideoRenderView, tileId2)

        verify { mockVideoTile.unbind() }
        verify(exactly = 0) { mockVideoTile2.unbind() }
    }

    @Test
    fun `bind should NOT call unbind on the first view when not bound`() {
        val mockVideoTile2: VideoTile = mockk(relaxUnitFun = true)
        val mockVideoRenderView2: VideoRenderView = mockk(relaxUnitFun = true)
        val tileId2 = 127

        every { mockVideoTileFactory.makeTile(tileId2, any()) } returns mockVideoTile2
        every { mockVideoTile2.state } returns VideoTileState(
            tileId2,
            attendeeId,
            VideoPauseState.Unpaused
        )
        every { mockVideoTile2.videoRenderView } returns mockVideoRenderView2
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId
            )
            videoTileController.onReceiveFrame(
                mockFrame,
                attendeeId,
                VideoPauseState.Unpaused,
                tileId2
            )
        }

        videoTileController.bindVideoView(mockVideoRenderView, tileId)
        videoTileController.bindVideoView(mockVideoRenderView2, tileId2)

        verify(exactly = 0) { mockVideoTile.unbind() }
        verify(exactly = 0) { mockVideoTile2.unbind() }
        verify(exactly = 1) { mockVideoTile.bind(any(), any()) }
        verify(exactly = 1) { mockVideoTile2.bind(any(), any()) }
    }
}
