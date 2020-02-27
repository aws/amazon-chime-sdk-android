/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.utils.logger.Logger
import com.amazon.chime.webrtc.VideoRenderer
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
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

    private val tileId = 117 // some random prime number
    private val attendeeId = "chimesarang"
    private val pauseType = 0

    @MockK
    private lateinit var videoClientController: VideoClientController

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var tileRenderer: DefaultVideoRenderView

    @MockK
    private lateinit var frame: VideoRenderer.I420Frame

    @InjectMockKs
    private lateinit var videoTileController: DefaultVideoTileController

    // See https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test for more examples
    private val testDispatcher = TestCoroutineDispatcher()

    private var onAddObserverCalled = 0
    private var onRemoveObserverCalled = 0

    private val tileObserver = object : VideoTileObserver {
        override fun onAddVideoTile(tileState: VideoTileState) {
            onAddObserverCalled++
        }

        override fun onRemoveVideoTile(tileState: VideoTileState) {
            onRemoveObserverCalled++
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `unbindVideoView should call release on VideoRenderView`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }
        videoTileController.bindVideoView(tileRenderer, tileId)
        videoTileController.unbindVideoView(tileId)

        verify { tileRenderer.release() }
    }

    @Test
    fun `onReceiveFrame should call renderFrame on VideoRenderView when bound`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }
        videoTileController.bindVideoView(tileRenderer, tileId)
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }

        verify { tileRenderer.renderFrame(frame) }
    }

    @Test
    fun `onReceiveFrame should NOT call onAddVideoTile on Observer when observer has been removed`() {
        videoTileController.addVideoTileObserver(tileObserver)
        videoTileController.removeVideoTileObserver(tileObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }

        Assert.assertEquals(0, onAddObserverCalled)
    }

    @Test
    fun `onReceiveFrame should NOT call onRemoveTile on Observer when observer has been removed`() {
        videoTileController.addVideoTileObserver(tileObserver)
        videoTileController.removeVideoTileObserver(tileObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }

        Assert.assertEquals(0, onAddObserverCalled)
    }

    @Test
    fun `onReceiveFrame should call onAddVideoTile on Observer with non-null frame and new tileId`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }

        Assert.assertEquals(1, onAddObserverCalled)
        verify { mockObserver.onAddVideoTile(VideoTileState(tileId, attendeeId, false)) }
    }

    @Test
    fun `onReceiveFrame should call onRemoveTile on Observer with null frame and old tileId`() {
        val mockObserver = spyk(tileObserver)

        videoTileController.addVideoTileObserver(mockObserver)
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
            videoTileController.onReceiveFrame(null, attendeeId, tileId, pauseType, tileId)
        }

        Assert.assertEquals(1, onRemoveObserverCalled)
        verify { mockObserver.onRemoveVideoTile(VideoTileState(tileId, attendeeId, false)) }
    }

    @Test
    fun `pauseRemoteVideoTile should call VideoClientController's setRemotePaused when tile exists`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }
        videoTileController.pauseRemoteVideoTile(tileId)

        verify { videoClientController.setRemotePaused(true, tileId) }
    }

    @Test
    fun `resumeRemoteVideoTile should call VideoClientController's setRemotePaused when tile exists`() {
        runBlockingTest {
            videoTileController.onReceiveFrame(frame, attendeeId, tileId, pauseType, tileId)
        }
        videoTileController.resumeRemoteVideoTile(tileId)

        verify { videoClientController.setRemotePaused(false, tileId) }
    }

    @Test
    fun `pauseRemoteVideoTile should NOT call VideoClientController's setRemotePaused when tile does not exist`() {
        videoTileController.pauseRemoteVideoTile(tileId)

        verify(exactly = 0) { videoClientController.setRemotePaused(true, tileId) }
    }

    @Test
    fun `resumeRemoteVideoTile should NOT call VideoClientController's setRemotePaused when tile does not exist`() {
        videoTileController.resumeRemoteVideoTile(tileId)

        verify(exactly = 0) { videoClientController.setRemotePaused(false, tileId) }
    }
}
