/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoPauseState] describes the pause status of a video tile.
 */
enum class VideoPauseState(val value: Int) {
    /**
     * The video tile is not paused
     */
    Unpaused(0),

    /**
     * The video tile has been paused by the user, and will only be unpaused if the user requests it to resume.
     */
    PausedByUserRequest(1),

    /**
     * The video tile has been paused to save on local downlink bandwidth.  When the connection improves,
     * it will be automatically unpaused by the client.  User requested pauses will shadow this pause,
     * but if the connection has not recovered on resume the tile will still be paused with this state.
     */
    PausedForPoorConnection(2);

    companion object {
        fun from(intValue: Int): VideoPauseState? = values().find { it.value == intValue }
    }
}
