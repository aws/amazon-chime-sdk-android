/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer

import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import java.nio.ByteBuffer

/**
 * [VideoFrameRGBABuffer] provides an reference counted wrapper of
 * an RGBA natively (i.e. in JNI) allocated direct byte buffer.
 */
class VideoFrameRGBABuffer(
    override val width: Int,
    override val height: Int,

    /**
     * RGBA plane data of video frame in memory. This must be a natively allocated direct byte
     * buffer so that it can be passed to native code
     */
    val data: ByteBuffer,

    /**
     * Stride of RGBA plane of video frame
     */
    val stride: Int,

    /**
     * Callback to trigger when reference count of this buffer reaches 0 (starts as 1).
     * Use this to release underlying natively allocated direct byte buffer
     */
    releaseCallback: Runnable
) : VideoFrameBuffer {
    private val refCountDelegate = RefCountDelegate(releaseCallback)

    init {
        check(data.isDirect) { "Only direct byte buffers are supported" }
    }

    override fun retain() = refCountDelegate.retain()
    override fun release() = refCountDelegate.release()
}
