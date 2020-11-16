/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer

import android.graphics.Matrix
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import kotlinx.coroutines.Runnable

/**
 * [VideoFrameTextureBuffer] provides an reference counted wrapper of
 * an OpenGLES texture and related metadata
 */
class VideoFrameTextureBuffer(
    override val width: Int,
    override val height: Int,

    /**
     * ID of underlying GL texture
     */
    val textureId: Int,

    /**
     * The transform matrix associated with the frame. This transform matrix maps 2D
     * homogeneous coordinates of the form (s, t, 1) with s and t in the inclusive range `[0, 1]` to
     * the coordinate that should be used to sample that location from the buffer.
     */
    val transformMatrix: Matrix?,

    /**
     * GL type of underlying GL texture
     */
    val type: Type,

    /**
     * Callback to trigger when reference count of this buffer reaches 0 (starts as 1).
     * Use this to trigger capturing or processing of next frame, or to finish a release
     * which was delayed until after all in-flight frames where returned.
     */
    releaseCallback: Runnable
) : VideoFrameBuffer {
    /**
     * Wrapper enum of underlying supported GL texture types
     *
     * @param glTarget: [Int] - Underlying OpenGLES type
     */
    enum class Type(private val glTarget: Int) {
        TEXTURE_OES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES),
        TEXTURE_2D(GLES20.GL_TEXTURE_2D);
    }

    private val refCountDelegate = RefCountDelegate(releaseCallback)
    override fun retain() = refCountDelegate.retain()
    override fun release() = refCountDelegate.release()
}
