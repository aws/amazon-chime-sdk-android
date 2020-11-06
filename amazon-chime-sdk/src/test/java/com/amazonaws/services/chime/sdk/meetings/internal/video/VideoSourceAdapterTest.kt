/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameRGBABuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import java.lang.Exception
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VideoSourceAdapterTest {
    @MockK
    private lateinit var mockSdkVideoSource: VideoSource

    @MockK
    private lateinit var mockVideoFrame: VideoFrame

    @MockK
    private lateinit var mockSdkVideoFrameBuffer: VideoFrameBuffer

    @MockK
    private lateinit var mockSdkVideoFrameI420Buffer: VideoFrameI420Buffer

    @MockK
    private lateinit var mockSdkVideoFrameRGBABuffer: VideoFrameRGBABuffer

    @MockK
    private lateinit var mockSdkVideoFrameTextureBuffer: VideoFrameTextureBuffer

    @MockK
    private lateinit var mockMediaVideoSink: com.xodee.client.video.VideoSink

    private val testSDKContentHint = VideoContentHint.Motion
    private val testMediaContentHint = com.xodee.client.video.ContentHint.MOTION

    private val testTimestamp: Long = 1
    private val testRotation = VideoRotation.Rotation90

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        every { mockSdkVideoSource.contentHint } returns testSDKContentHint
        every { mockVideoFrame.timestampNs } returns testTimestamp
        every { mockVideoFrame.rotation } returns testRotation
    }

    @Test
    fun `Content hint is passed through`() {
        val adapter = VideoSourceAdapter()
        adapter.source = mockSdkVideoSource

        assertEquals(adapter.contentHint, testMediaContentHint)
    }

    @Test
    fun `Timestamp and rotation is passed through`() {
        val adapter = VideoSourceAdapter()
        adapter.source = mockSdkVideoSource
        adapter.addSink(mockMediaVideoSink)

        adapter.onVideoFrameReceived(VideoFrame(testTimestamp, mockSdkVideoFrameTextureBuffer, testRotation))

        val slot = slot<com.xodee.client.video.VideoFrame>()
        verify(exactly = 1) { mockMediaVideoSink.onFrameCaptured(capture(slot)) }
        assertEquals(slot.captured.rotation, testRotation.degrees)
        assertEquals(slot.captured.timestampNs, testTimestamp)
    }

    @Test
    fun `Passing a generic SDK buffer results in an exception`() {
        val adapter = VideoSourceAdapter()
        adapter.source = mockSdkVideoSource
        adapter.addSink(mockMediaVideoSink)

        var exceptionThrown = false
        try {
            adapter.onVideoFrameReceived(VideoFrame(testTimestamp, mockSdkVideoFrameBuffer, testRotation))
        } catch (e: Exception) {
            exceptionThrown = true
        }
        assert(exceptionThrown)
    }

    @Test
    fun `Passing an I420 SDK buffer results in an I420 Media buffer`() {
        val adapter = VideoSourceAdapter()
        adapter.source = mockSdkVideoSource
        adapter.addSink(mockMediaVideoSink)

        adapter.onVideoFrameReceived(VideoFrame(testTimestamp, mockSdkVideoFrameI420Buffer, testRotation))

        val slot = slot<com.xodee.client.video.VideoFrame>()
        verify(exactly = 1) { mockMediaVideoSink.onFrameCaptured(capture(slot)) }
        assert(slot.captured.buffer is com.xodee.client.video.VideoFrameI420Buffer)
    }

    @Test
    fun `Passing an RGBA SDK buffer results in an RGBA Media buffer`() {
        val adapter = VideoSourceAdapter()
        adapter.source = mockSdkVideoSource
        adapter.addSink(mockMediaVideoSink)

        adapter.onVideoFrameReceived(VideoFrame(testTimestamp, mockSdkVideoFrameRGBABuffer, testRotation))

        val slot = slot<com.xodee.client.video.VideoFrame>()
        verify(exactly = 1) { mockMediaVideoSink.onFrameCaptured(capture(slot)) }
        assert(slot.captured.buffer is com.xodee.client.video.VideoFrameRGBABuffer)
    }

    @Test
    fun `Passing a texture SDK buffer results in a texture Media buffer`() {
        val adapter = VideoSourceAdapter()
        adapter.source = mockSdkVideoSource
        adapter.addSink(mockMediaVideoSink)

        adapter.onVideoFrameReceived(VideoFrame(testTimestamp, mockSdkVideoFrameTextureBuffer, testRotation))

        val slot = slot<com.xodee.client.video.VideoFrame>()
        verify(exactly = 1) { mockMediaVideoSink.onFrameCaptured(capture(slot)) }
        assert(slot.captured.buffer is com.xodee.client.video.VideoFrameTextureBuffer)
    }
}
