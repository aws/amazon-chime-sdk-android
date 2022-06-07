package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * Contains configuration for a local video or content share to be sent
 *
 * @property maxBitRateKbps: UInt - The max bit rate for video encoding, should be greater than 0
 * Actual quality achieved may vary throughout the call depending on what system and network can provide
 */
data class LocalVideoConfiguration(
    val maxBitRateKbps: UInt = 0U
)
