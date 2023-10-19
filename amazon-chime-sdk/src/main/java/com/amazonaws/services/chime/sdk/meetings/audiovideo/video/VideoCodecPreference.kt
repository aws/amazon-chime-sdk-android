/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

data class VideoCodecPreference(
    val name: String,
    val clockRate: Int,
    val params: Map<String, String>
) {
    override fun toString(): String = name
}

val h264cbp = VideoCodecPreference(
    "H264", 90000, mapOf(
        "level-asymmetry-allowed" to "1",
        "packetization-mode" to "1",
        "profile-level-id" to "42e01f"
    )
)

val h264h = VideoCodecPreference(
    "H264", 90000, mapOf(
        "level-asymmetry-allowed" to "1",
        "packetization-mode" to "1",
        "profile-level-id" to "640c1f"
    )
)

val vp8 = VideoCodecPreference("VP8", 90000, emptyMap())
