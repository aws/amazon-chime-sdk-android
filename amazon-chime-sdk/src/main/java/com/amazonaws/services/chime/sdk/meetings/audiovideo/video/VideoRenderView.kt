/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

interface VideoRenderView {

    /**
     * To initialize any platform specifc resource for the view For eg. EGL context if used.
     * Should be called when the view is created
     *
     * @param initParams: [Any] - Helper object to pass any data required for initialization
     */
    fun initialize(initParams: Any?)

    /**
     * To cleanup any platform specifc resource For eg. EGL context if used.
     * Should be called when the view is no longer used and needs to be destroyed
     */
    fun finalize()

    /**
     * Renders the frame on the view
     *
     * @param frame: [Any] - Video Frame
     */
    fun renderFrame(frame: Any)
}
