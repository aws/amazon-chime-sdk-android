package com.amazonaws.services.chime.sdk.meetings.utils

import com.xodee.client.video.VideoClientSignalingDroppedError

enum class SignalingDroppedError {
    /**
     * No error occurred
     */
    None,

    /**
     * The signaling client was disconnected from the server
     */
    SignalingClientDisconnected,

    /**
     * The signaling client connection was closed
     */
    SignalingClientClosed,

    /**
     * The signaling client encountered an End-Of-File condition
     */
    SignalingClientEOF,

    /**
     * A general error occurred in the signaling client
     */
    SignalingClientError,

    /**
     * An error occurred with the signaling client's proxy connection
     */
    SignalingClientProxyError,

    /**
     * The signaling client failed to establish an initial connection
     */
    SignalingClientOpenFailed,

    /**
     * Failed to parse an incoming signaling frame
     */
    SignalFrameParseFailed,

    /**
     * Failed to serialize a signaling frame for transmission
     */
    SignalFrameSerializeFailed,

    /**
     * Failed to send a signaling frame
     */
    SignalFrameSendingFailed,

    /**
     * An internal server error occurred
     */
    InternalServerError,

    /**
     * An unspecified or other error occurred
     */
    Other;

    companion object {
        /**
         * Converts VideoClientSignalingDroppedError to SignalingDroppedError
         *
         * @param videoClientError The VideoClientSignalingDroppedError to convert
         * @return The corresponding SignalingDroppedError
         */
        fun fromVideoClientSignalingDroppedError(videoClientError: VideoClientSignalingDroppedError): SignalingDroppedError {
            return when (videoClientError) {
                VideoClientSignalingDroppedError.NONE -> None
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_DISCONNECTED -> SignalingClientDisconnected
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_CLOSED -> SignalingClientClosed
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_EOF -> SignalingClientEOF
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_ERROR -> SignalingClientError
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_PROXY_ERROR -> SignalingClientProxyError
                VideoClientSignalingDroppedError.SIGNALING_CLIENT_OPEN_FAILED -> SignalingClientOpenFailed
                VideoClientSignalingDroppedError.SIGNAL_FRAME_PARSE_FAILED -> SignalFrameParseFailed
                VideoClientSignalingDroppedError.SIGNAL_FRAME_SERIALIZE_FAILED -> SignalFrameSerializeFailed
                VideoClientSignalingDroppedError.VIDEO_SIGNAL_FRAME_SENDING_FAILED -> SignalFrameSendingFailed
                VideoClientSignalingDroppedError.INTERNAL_SERVER_ERROR -> InternalServerError
                VideoClientSignalingDroppedError.OTHER -> Other
            }
        }
    }
}
