/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video.gl

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory

/**
 * [EglRenderer] is a helper interface to draw [VideoFrame] objects to a provided OpenGLES textures
 * with additional display relation options.
 *
 * It is currently used by [SurfaceRenderView] and [TextureRenderView]
 */
interface EglRenderer : VideoSink {
    /**
     * Aspect ratio of displayed surface, used internally to set viewport and scaling
     */
    var aspectRatio: Float

    /**
     * Desired mirror across vertical axis (e.g. for self video)
     */
    var mirror: Boolean

    /**
     * Initialize with factory to create [EglCore] objects to hold/share EGL state
     *
     * @param eglCoreFactory: [EglCoreFactory] - Factory to create [EglCore] objects to hold EGL state
     */
    fun init(eglCoreFactory: EglCoreFactory)

    /**
     * Deallocate any state or resources held by this object. Not calling this function will leak resources.
     */
    fun release()

    /**
     * Initialize internal EGL target rendering surface from [Surface] or [SurfaceTexture]
     *
     * @param inputSurface: [Any] - Target [Surface] or [SurfaceTexture] (will assert on type)
     */
    fun createEglSurface(inputSurface: Any)

    /**
     * Release internal EGL target rendering surface. Not calling this function will leak resources.
     */
    fun releaseEglSurface()
}
