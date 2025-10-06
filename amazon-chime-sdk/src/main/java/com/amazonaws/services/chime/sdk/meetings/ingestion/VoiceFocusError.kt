/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

enum class VoiceFocusError {

    /** Audio client has not been started yet */
    audioClientNotStarted,

    /** General audio client error occurred */
    audioClientError,

    /** Invalid parameters */
    invalidParameters,

    /** Voice Focus has not been initialized */
    notInitialized,

    /** Other or unknown error */
    other;

    companion object {

        /**
         * Convert an XAL error code into a VoiceFocusError.
         *
         * @param xalError Error code from the native XAL layer.
         * @return Corresponding VoiceFocusError enum value.
         */
        fun fromXalError(xalError: Int): VoiceFocusError {
            return when (xalError) {
                1 -> audioClientError // AUDIO_CLIENT_ERR
                5 -> invalidParameters // XAL_ERR_INVALID_PARAMETERS
                6 -> notInitialized // XAL_ERR_NOT_INITIALIZED
                else -> other
            }
        }
    }
}
