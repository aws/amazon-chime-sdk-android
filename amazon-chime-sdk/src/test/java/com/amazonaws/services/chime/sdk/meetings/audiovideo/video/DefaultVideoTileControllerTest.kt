/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingStatsCollector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
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
    private lateinit var mockMeetingSessionConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var mockVideoTileFactory: VideoTileFactory

    @MockK
    private lateinit var mockVideoTileController: VideoClientController

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockEglCoreFactory: EglCoreFactory

    @MockK
    private lateinit var mockVideoTile: VideoTile

    @MockK
    private lateinit var mockVideoRenderView: VideoRenderView

    @MockK
    private lateinit var mockVideoFrame: VideoFrame

    @MockK
    private lateinit var mockVideoFrameBuffer: VideoFrameBuffer

    @MockK
    private lateinit var meetingStatsCollector: MeetingStatsCollector

    @InjectMockKs
    private lateinit var videoTileController: DefaultVideoTileController

    // See https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test for more examples
    private val testDispatcher = TestCoroutineDispatcher()

    private val tileId = 7 // some random prime number
    private val attendeeId = "chimesarang"
    private val testWidth = 0
    private val testHeight = 0
    private val testTimestamp: Long = 1000
    private val testRotation = VideoRotation.Rotation90
    private val localTile = true
    private val remoteTile = false

    private var onAddObserverCalled = 0
    private var onRemoveObserverCalled = 0
    private var onPauseObserverCalled = 0
    private var onResumeObserverCalled = 0
    private var onSizeChangedObserverCalled = 0

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

        override fun onVideoTileSizeChanged(tileState: VideoTileState) {
            onSizeChangedObserverCalled++
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockMeetingSessionConfiguration.credentials.attendeeId } returns "attendeeId"
        every { mockVideoTileController.getConfiguration() } returns mockMeetingSessionConfiguration
        every { mockVideoTileFactory.makeTile(tileId, any(), testWidth, testHeight, any()) } returns mockVideoTile
        every { mockVideoTile.state } returns VideoTileState(
            tileId,
            attendeeId,
            testWidth,
            testHeight,
            VideoPauseState.Unpaused,
            remoteTile
        )
        every { mockVideoTile.videoRenderView } returns mockVideoRenderView
        every { mockVideoFrame.width } returns testWidth
        every { mockVideoFrame.height } returns testHeight

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
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
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
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }
        videoTileController.bindVideoView(mockVideoRenderView, tileId)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        verify { mockVideoTile.onVideoFrameReceived(any()) }
    }

    @Test
    fun `onReceiveFrame should NOT notify observer about video tile add when observer has been removed`() {
        videoTileController.addVideoTileObserver(tileObserver)
        videoTileController.removeVideoTileObserver(tileObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
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
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
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
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        Assert.assertEquals(1, onAddObserverCalled)
        verify {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
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
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            videoTileController.onReceiveFrame(null, tileId, attendeeId, VideoPauseState.Unpaused)
        }

        Assert.assertEquals(1, onRemoveObserverCalled)
        verify {
            mockObserver.onVideoTileRemoved(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
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
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.PausedForPoorConnection
            )
        }

        verify { mockVideoTile.setPauseState(VideoPauseState.PausedForPoorConnection) }
        // Mock video tile state will be equal to Unpaused
        verify(exactly = 1) {
            mockObserver.onVideoTilePaused(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
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
            testWidth,
            testHeight,
            VideoPauseState.PausedForPoorConnection,
            remoteTile
        )

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.PausedForPoorConnection
            )
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        verify { mockVideoTile.setPauseState(VideoPauseState.Unpaused) }
        // Mock video tile state will be equal to PausedForPoorConnection
        verify(exactly = 1) {
            mockObserver.onVideoTileResumed(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.PausedForPoorConnection,
                    remoteTile
                )
            )
        }
    }

    @Test
    fun `pauseRemoteVideoTile should call VideoClientController's setRemotePaused when tile exists`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
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
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            videoTileController.pauseRemoteVideoTile(tileId)
        }

        // Mock video tile state will be equal to Unpaused
        verify(exactly = 1) {
            mockObserver.onVideoTilePaused(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
    }

    @Test
    fun `resumeRemoteVideoTile should call VideoClientController's setRemotePaused when tile exists`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
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
            testWidth,
            testHeight,
            VideoPauseState.PausedByUserRequest,
            remoteTile
        )
        videoTileController.addVideoTileObserver(mockObserver)

        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.PausedByUserRequest
            )
            videoTileController.resumeRemoteVideoTile(tileId)
        }

        // Mock video tile state will be equal to PausedByUserRequest
        verify(exactly = 1) {
            mockObserver.onVideoTileResumed(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.PausedByUserRequest,
                    remoteTile
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
        every { mockVideoTile.state } returns VideoTileState(tileId, attendeeId, testWidth, testHeight, VideoPauseState.Unpaused, localTile)
        runBlockingTest {
            videoTileController.onReceiveFrame(mockVideoFrame, tileId, null, VideoPauseState.Unpaused)
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
        every { mockVideoTile.state } returns VideoTileState(tileId, attendeeId, testWidth, testHeight, VideoPauseState.Unpaused, localTile)
        runBlockingTest {
            videoTileController.onReceiveFrame(mockVideoFrame, tileId, null, VideoPauseState.Unpaused)
        }

        videoTileController.resumeRemoteVideoTile(tileId)

        verify(exactly = 0) { mockVideoTileController.setRemotePaused(any(), any()) }
    }

    @Test
    fun `bind should unbind the view first when already bound`() {
        val mockVideoTile2: VideoTile = mockk(relaxUnitFun = true)
        val tileId2 = 127

        every { mockVideoTileFactory.makeTile(tileId2, any(), testWidth, testHeight, remoteTile) } returns mockVideoTile2
        every { mockVideoTile2.state } returns VideoTileState(
            tileId2,
            attendeeId,
            testWidth,
            testHeight,
            VideoPauseState.Unpaused,
            remoteTile
        )
        every { mockVideoTile2.videoRenderView } returns null
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId2,
                attendeeId,
                VideoPauseState.Unpaused
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
        every { mockVideoTileFactory.makeTile(tileId2, any(), testWidth, testHeight, remoteTile) } returns mockVideoTile2
        every { mockVideoTile2.state } returns VideoTileState(
            tileId2,
            attendeeId,
            testWidth,
            testHeight,
            VideoPauseState.Unpaused,
            remoteTile
        )
        every { mockVideoTile2.videoRenderView } returns null
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId2,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        videoTileController.bindVideoView(mockVideoRenderView, tileId)
        videoTileController.bindVideoView(mockVideoRenderView2, tileId2)

        verify(exactly = 0) { mockVideoTile.unbind() }
        verify(exactly = 0) { mockVideoTile2.unbind() }
        verify(exactly = 1) { mockVideoTile.bind(any()) }
        verify(exactly = 1) { mockVideoTile2.bind(any()) }
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile size change with new frame size`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        every { mockVideoFrameBuffer.width } returns testWidth + 1
        every { mockVideoFrameBuffer.height } returns testHeight + 1

        runBlockingTest {
            videoTileController.onReceiveFrame(
                VideoFrame(
                        testTimestamp,
                        mockVideoFrameBuffer,
                        testRotation
                ),
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        Assert.assertEquals(1, onSizeChangedObserverCalled)
        verify {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth + 1,
                    testHeight + 1,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
    }

    @Test
    fun `onReceiveFrame should NOT notify observer about video tile size change with the same frame size`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        every { mockVideoFrameBuffer.width } returns testWidth
        every { mockVideoFrameBuffer.height } returns testHeight

        runBlockingTest {
            videoTileController.onReceiveFrame(
                VideoFrame(
                        testTimestamp,
                        mockVideoFrameBuffer,
                        testRotation
                ),
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        Assert.assertEquals(0, onSizeChangedObserverCalled)
        verify {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
    }

    @Test
    fun `Local video tile should be added with attendeeId`() {
        val mockObserver = spyk(tileObserver)
        every { mockVideoTile.state } returns VideoTileState(
            tileId,
            attendeeId,
            testWidth,
            testHeight,
            VideoPauseState.Unpaused,
            localTile
        )

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                null,
                VideoPauseState.Unpaused
            )
        }

        verify {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    localTile
                )
            )
        }
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile add when the video is unbound and previously removed`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            // Add video but do not bind it
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            // Remove video
            videoTileController.onReceiveFrame(null, tileId, attendeeId, VideoPauseState.Unpaused)
            // Add video again
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        verify(exactly = 1) {
            mockObserver.onVideoTileRemoved(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
        verify(exactly = 2) {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile add when the video is bound and previously removed`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            // Add video and bind it
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            videoTileController.bindVideoView(mockVideoRenderView, tileId)
            // Remove video
            videoTileController.onReceiveFrame(null, tileId, attendeeId, VideoPauseState.Unpaused)
            // Add video again
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        verify(exactly = 1) {
            mockObserver.onVideoTileRemoved(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
        verify(exactly = 2) {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
    }

    @Test
    fun `onReceiveFrame should notify observer about video tile add when the paused video was previously removed then added and resumed`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            // Add video but do not bind it
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
            // Pause video
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.PausedByUserRequest
            )
            // Remove video
            videoTileController.onReceiveFrame(null, tileId, attendeeId, VideoPauseState.Unpaused)
            // Add video again
            videoTileController.onReceiveFrame(
                null,
                tileId,
                attendeeId,
                VideoPauseState.PausedByUserRequest
            )
            // Resume video
            videoTileController.onReceiveFrame(
                mockVideoFrame,
                tileId,
                attendeeId,
                VideoPauseState.Unpaused
            )
        }

        verify(exactly = 1) {
            mockObserver.onVideoTileRemoved(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
        verify(exactly = 2) {
            mockObserver.onVideoTileAdded(
                VideoTileState(
                    tileId,
                    attendeeId,
                    testWidth,
                    testHeight,
                    VideoPauseState.Unpaused,
                    remoteTile
                )
            )
        }
    }
}
