/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultVideoClientStateController(
    private val logger: Logger
) :
    VideoClientStateController {
    private val TAG = "DefaultVideoClientStateController"

    private var videoClientState =
        VideoClientState.UNINITIALIZED
    private var lifecycleHandler: VideoClientLifecycleHandler? = null

    override fun bindLifecycleHandler(lifecycleHandler: VideoClientLifecycleHandler) {
        this.lifecycleHandler = lifecycleHandler
    }

    override fun updateState(newState: VideoClientState) {
        videoClientState = newState
    }

    override fun start() {
        when (videoClientState) {
            VideoClientState.UNINITIALIZED -> {
                lifecycleHandler?.initializeVideoClient()
                lifecycleHandler?.startVideoClient()
                videoClientState =
                    VideoClientState.STARTED
            }
            VideoClientState.INITIALIZED,
            VideoClientState.STOPPED -> {
                lifecycleHandler?.startVideoClient()
                videoClientState =
                    VideoClientState.STARTED
            }
            else -> logger.warn(TAG, "VideoClient is already in a start state, ignoring")
        }
    }

    override fun stop() {
        when (videoClientState) {
            VideoClientState.STARTED -> {
                lifecycleHandler?.stopVideoClient()
                lifecycleHandler?.destroyVideoClient()
                videoClientState =
                    VideoClientState.UNINITIALIZED
            }
            VideoClientState.INITIALIZED,
            VideoClientState.STOPPED -> {
                lifecycleHandler?.destroyVideoClient()
                videoClientState =
                    VideoClientState.UNINITIALIZED
            }
            else -> logger.warn(TAG, "VideoClient is already in an uninitialized state, ignoring")
        }
    }

    override fun canAct(minimalRequiredState: VideoClientState): Boolean {
        if (videoClientState >= minimalRequiredState) return true

        logger.warn(
            TAG,
            "Video client is not at $minimalRequiredState state or higher, ignoring action"
        )
        return false
    }
}
