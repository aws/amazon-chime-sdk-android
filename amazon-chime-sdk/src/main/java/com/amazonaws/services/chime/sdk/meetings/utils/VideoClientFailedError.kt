/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

enum class VideoClientFailedError {

    /**
     * Failed to authenticate the client.
     */
    authenticationFailed,

    /**
     * Failed to create a peer-to-peer connection.
     */
    peerConnectionCreateFailed,

    /**
     * The maximum allowed retry period for reconnecting or attempting an operation was exceeded.
     */
    maxRetryPeriodExceeded,

    /**
     * An unspecified or other error occurred
     */
    other;

    companion object {
        /**
         * Maps a video client status code to a [VideoClientFailedError] enum.
         *
         * @param videoClientStatus The integer status code returned by the video client.
         * @return The corresponding [VideoClientFailedError] value.
         *
         * Status code mappings:
         * - 6  -> [VideoClientFailedError.peerConnectionCreateFailed]
         * - 41 -> [VideoClientFailedError.maxRetryPeriodExceeded]
         * - 64 -> [VideoClientFailedError.authenticationFailed]
         * - Any other value -> [VideoClientFailedError.other]
         */
        fun fromVideoClientStatus(videoClientStatus: Int): VideoClientFailedError {
            return when (videoClientStatus) {
                6 -> peerConnectionCreateFailed
                41 -> maxRetryPeriodExceeded
                64 -> authenticationFailed
                else -> other
            }
        }
    }
}
