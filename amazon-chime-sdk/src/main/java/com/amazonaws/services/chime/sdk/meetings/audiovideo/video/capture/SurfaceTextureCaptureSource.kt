/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.view.Surface
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource

/**
 * [SurfaceTextureCaptureSource] provides a [Surface] which can be passed to system sources like the camera.
 * Upon [start] call, the source will listen to the surface and emit any new images as [VideoFrame] objects to any
 * downstream [VideoSink] interfaces. This class is mostly intended for composition within [VideoSource] implementations which will
 * pass the created [Surface] to a system source, then call [addVideoSink] to receive the frames before transforming and
 * passing downstream.
 */
interface SurfaceTextureCaptureSource : VideoCaptureSource {
    /**
     * [Surface] from which any buffers submitted to will be emitted as a [VideoFrame].
     * User must call [start] to start listening, and [stop] will likewise stop listening.
     */
    val surface: Surface

    /**
     * Deallocate any state or resources held by this object. Not possible to reuse after call. Surface
     * will have been released.
     */
    fun release()
}
