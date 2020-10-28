/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer

import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import java.nio.ByteBuffer

/**
 * [VideoFrameI420Buffer] provides an reference counted wrapper of
 * a YUV where planes are natively (i.e. in JNI) allocated direct byte buffers.
 */
class VideoFrameI420Buffer(
    override val width: Int,
    override val height: Int,

    /**
     * Y plane data of video frame in memory. This must be a natively allocated direct byte
     * buffer that it can be passed to native code
     */
    val dataY: ByteBuffer,

    /**
     * U plane data of video frame in memory. This must be a natively allocated direct byte
     * buffer so that it can be passed to native code
     */
    val dataU: ByteBuffer,

    /**
     * V plane data of video frame in memory. This must be a natively allocated direct byte
     * buffer so that it can be passed to native code
     */
    val dataV: ByteBuffer,

    /**
     * Stride of Y plane of video frame
     */
    val strideY: Int,

    /**
     * Stride of U plane of video frame
     */
    val strideU: Int,

    /**
     * Stride of V plane of video frame
     */
    val strideV: Int,

    /**
     * Callback to trigger when reference count of this buffer reaches 0 (starts as 1).
     * Use this to release underlying natively allocated direct byte buffer(s)
     */
    releaseCallback: Runnable
) : VideoFrameBuffer {
    private val refCountDelegate = RefCountDelegate(releaseCallback)

    init {
        check(dataY.isDirect && dataU.isDirect && dataV.isDirect) { "Only direct byte buffers are supported" }
    }

    override fun retain() = refCountDelegate.retain()
    override fun release() = refCountDelegate.release()
}
