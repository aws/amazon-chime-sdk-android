/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

import com.xodee.client.video.VideoClientSignalingDroppedError

enum class SignalingDroppedError {
    /**
     * No error occurred
     */
    none,

    /**
     * The signaling client was disconnected from the server
     */
    signalingClientDisconnected,

    /**
     * The signaling client connection was closed
     */
    signalingClientClosed,

    /**
     * The signaling client encountered an End-Of-File condition
     */
    signalingClientEOF,

    /**
     * A general error occurred in the signaling client
     */
    signalingClientError,

    /**
     * An error occurred with the signaling client's proxy connection
     */
    signalingClientProxyError,

    /**
     * The signaling client failed to establish an initial connection
     */
    signalingClientOpenFailed,

    /**
     * Failed to parse an incoming signaling frame
     */
    signalFrameParseFailed,

    /**
     * Failed to serialize a signaling frame for transmission
     */
    signalFrameSerializeFailed,

    /**
     * Failed to send a signaling frame
     */
    signalFrameSendingFailed,

    /**
     * An internal server error occurred
     */
    internalServerError,

    /**
     * An unspecified or other error occurred
     */
    other;

    companion object {
        /**
         * Converts VideoClientSignalingDroppedError to SignalingDroppedError
         *
         * @param videoClientError The VideoClientSignalingDroppedError to convert
         * @return The corresponding SignalingDroppedError
         */
        fun fromVideoClientSignalingDroppedError(videoClientError: VideoClientSignalingDroppedError): SignalingDroppedError {
            return when (videoClientError) {
                VideoClientSignalingDroppedError.NONE -> none
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_DISCONNECTED -> signalingClientDisconnected
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_CLOSED -> signalingClientClosed
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_EOF -> signalingClientEOF
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_ERROR -> signalingClientError
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_PROXY_ERROR -> signalingClientProxyError
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_OPEN_FAILED -> signalingClientOpenFailed
                VideoClientSignalingDroppedError.SIGNAL_FRAME_PARSE_FAILED -> signalFrameParseFailed
                VideoClientSignalingDroppedError.SIGNAL_FRAME_SERIALIZE_FAILED -> signalFrameSerializeFailed
                VideoClientSignalingDroppedError.VIDEO_SIGNAL_FRAME_SENDING_FAILED -> signalFrameSendingFailed
                VideoClientSignalingDroppedError.INTERNAL_SERVER_ERROR -> internalServerError
                VideoClientSignalingDroppedError.OTHER -> other
            }
        }
    }
}
