/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint

/**
 * [SurfaceTextureCaptureSourceFactory] is an factory interface for creating new [SurfaceTextureCaptureSource] objects,
 * possible using shared state. This provides flexibility over use of [SurfaceTextureCaptureSource] objects since
 * they may not allow reuse, or may have a delay before possible reuse.
 */
interface SurfaceTextureCaptureSourceFactory {
    /**
     * Create a new [SurfaceTextureCaptureSource] object
     *
     * @return [SurfaceTextureCaptureSource] - Newly created and initialized [SurfaceTextureCaptureSource] object
     */
    fun createSurfaceTextureCaptureSource(
        width: Int,
        height: Int,
        contentHint: VideoContentHint
    ): SurfaceTextureCaptureSource
}
