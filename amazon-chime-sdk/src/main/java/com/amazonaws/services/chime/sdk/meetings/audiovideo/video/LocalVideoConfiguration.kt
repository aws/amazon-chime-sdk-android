package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * Contains configuration for a local video or content share to be sent
 *
 * @property maxBitRateKbps: Int - The max bit rate for video encoding, should be greater than 0
 * Actual quality achieved may vary throughout the call depending on what system and network can provide
 */
data class LocalVideoConfiguration @JvmOverloads constructor(
    private val maxBitRateKbps: Int = 0
) {
    val safeMaxBitRateKbps: Int
    get() = if (maxBitRateKbps < 0) 0 else maxBitRateKbps
}
