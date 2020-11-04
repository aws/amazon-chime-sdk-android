/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

/**
 * [CaptureSourceObserver] observes events resulting from different types of capture devices. Builders
 * may desire this input to decide when to show certain UI elements, or to notify users of failure.
 */
interface CaptureSourceObserver {
    /**
     * Called when the capture source has started successfully and has started emitting frames
     */
    fun onCaptureStarted()

    /**
     * Called when the capture source has stopped when expected. The capture may be in the processed of restarting,
     * e.g. this may occur when switching cameras.
     */
    fun onCaptureStopped()

    /**
     * Called when the capture source failed unexpectedly. This may be due to misconfiguration
     * or Android system error, and the capture source may be in an unknown state.
     *
     * This does not necessarily indicate that calling [VideoCaptureSource.start] again will fail.
     *
     * @param error: [CaptureSourceError] - The reason why the source has stopped.
     */
    fun onCaptureFailed(error: CaptureSourceError)
}
