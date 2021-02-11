/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.graphics.Matrix
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameRGBABuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import java.nio.ByteBuffer
import java.security.InvalidParameterException

/**
 * [VideoSourceAdapter] provides two classes to adapt [VideoSource] to [com.xodee.client.video.VideoSource].
 */
class VideoSourceAdapter : VideoSink,
    com.xodee.client.video.VideoSource {

    class VideoFrameTextureBufferAdapter(
        private val textureBuffer: VideoFrameTextureBuffer
    ) : com.xodee.client.video.VideoFrameTextureBuffer {
        override fun getWidth(): Int = textureBuffer.width
        override fun getHeight(): Int = textureBuffer.height
        override fun getType(): com.xodee.client.video.VideoFrameTextureBuffer.Type {
            return when (textureBuffer.type) {
                VideoFrameTextureBuffer.Type.TEXTURE_OES -> com.xodee.client.video.VideoFrameTextureBuffer.Type.OES
                VideoFrameTextureBuffer.Type.TEXTURE_2D -> com.xodee.client.video.VideoFrameTextureBuffer.Type.RGB
            }
        }

        override fun getTransformMatrix(): Matrix? = textureBuffer.transformMatrix
        override fun getTextureId(): Int = textureBuffer.textureId
        override fun release() = textureBuffer.release()
        override fun retain() = textureBuffer.retain()
    }

    class VideoFrameRGBABufferAdapter(private val rgbaBuffer: VideoFrameRGBABuffer) :
        com.xodee.client.video.VideoFrameRGBABuffer {
        override fun getWidth(): Int = rgbaBuffer.width
        override fun getHeight(): Int = rgbaBuffer.height
        override fun getData(): ByteBuffer? = rgbaBuffer.data
        override fun getStride(): Int = rgbaBuffer.stride
        override fun retain() = rgbaBuffer.retain()
        override fun release() = rgbaBuffer.release()
    }

    class VideoFrameI420BufferAdapter(
        private val i420Buffer: VideoFrameI420Buffer
    ) : com.xodee.client.video.VideoFrameI420Buffer {
        override fun getWidth(): Int = i420Buffer.width
        override fun getHeight(): Int = i420Buffer.height
        override fun getDataY(): ByteBuffer? = i420Buffer.dataY
        override fun getDataU(): ByteBuffer? = i420Buffer.dataU
        override fun getDataV(): ByteBuffer? = i420Buffer.dataV
        override fun getStrideY(): Int = i420Buffer.strideY
        override fun getStrideU(): Int = i420Buffer.strideU
        override fun getStrideV(): Int = i420Buffer.strideV
        override fun retain() = i420Buffer.retain()
        override fun release() = i420Buffer.release()
    }

    var source: VideoSource? = null
        set(value) {
            source?.removeVideoSink(this)
            field = value
            source?.addVideoSink(this)
        }

    // Concurrency modification could happen when source gets
    // removed from media server (media thread) while sending frames (app thread).
    private var sinks = ConcurrentSet.createConcurrentSet<com.xodee.client.video.VideoSink>()

    override fun addSink(sink: com.xodee.client.video.VideoSink) {
        sinks.add(sink)
    }

    override fun removeSink(sink: com.xodee.client.video.VideoSink) {
        sinks.remove(sink)
    }

    override fun getContentHint(): com.xodee.client.video.ContentHint {
        return when (source?.contentHint) {
            VideoContentHint.None -> com.xodee.client.video.ContentHint.NONE
            VideoContentHint.Motion -> com.xodee.client.video.ContentHint.MOTION
            VideoContentHint.Detail -> com.xodee.client.video.ContentHint.DETAIL
            VideoContentHint.Text -> com.xodee.client.video.ContentHint.TEXT
            else -> com.xodee.client.video.ContentHint.NONE
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        val buffer = when (frame.buffer) {
            is VideoFrameTextureBuffer -> VideoFrameTextureBufferAdapter(frame.buffer)
            is VideoFrameI420Buffer -> VideoFrameI420BufferAdapter(frame.buffer)
            is VideoFrameRGBABuffer -> VideoFrameRGBABufferAdapter(frame.buffer)
            else -> throw InvalidParameterException("Media SDK only supports texture, I420, and RGBA video frame buffers")
        }

        val videoClientFrame = com.xodee.client.video.VideoFrame(
            frame.width, frame.height, frame.timestampNs, frame.rotation.degrees, buffer
        )
        sinks.forEach { it.onFrameCaptured(videoClientFrame) }
    }
}
