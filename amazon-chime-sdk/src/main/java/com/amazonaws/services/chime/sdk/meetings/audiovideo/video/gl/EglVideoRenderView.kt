/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer

/**
 * [EglVideoRenderView] is a [VideoRenderView] which requires EGL initialization to render [VideoFrameTextureBuffer] buffers.
 * The [VideoTileController] should automatically manage ([init] and [release]) any bound tiles, but if it
 * is desired to use a view outside of the controller (e.g. in pre-meeting device selection), users will
 * need to call [init] and [release] themselves
 */
interface EglVideoRenderView : VideoRenderView {
    /**
     * Initialize view with factory to create [EglCore] objects to hold/share EGL state
     *
     * @param eglCoreFactory: [EglCoreFactory] - Factory to create [EglCore] objects to hold EGL state
     */
    fun init(eglCoreFactory: EglCoreFactory)

    /**
     * Deallocate any state or resources held by this object
     */
    fun release()
}
