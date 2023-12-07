/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution

// https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateMeeting.html
data class CreateMeetingResponse(val Meeting: Meeting)

data class Meeting(
    val ExternalMeetingId: String?,
    val MediaPlacement: MediaPlacement,
    val MediaRegion: String,
    val MeetingId: String,
    val MeetingFeatures: MeetingFeatures
) {
    @JvmOverloads
    constructor(
        ExternalMeetingId: String?,
        MediaPlacement: MediaPlacement,
        MediaRegion: String,
        MeetingId: String
    ) : this (ExternalMeetingId,
        MediaPlacement,
        MediaRegion,
        MeetingId,
        MeetingFeatures()
    )
}

data class MediaPlacement @JvmOverloads constructor(
    val AudioFallbackUrl: String,
    val AudioHostUrl: String,
    val SignalingUrl: String,
    val TurnControlUrl: String,
    val EventIngestionUrl: String? = null
)

data class MeetingFeatures(
    val videoMaxResolution: VideoResolution = VideoResolution.VideoResolutionHD,
    val contentMaxResolution: VideoResolution = VideoResolution.VideoResolutionFHD
) {
    @JvmOverloads
    constructor(
        createMeetingResponse: CreateMeetingResponse
    ) : this (createMeetingResponse.Meeting.MeetingFeatures.videoMaxResolution,
        createMeetingResponse.Meeting.MeetingFeatures.contentMaxResolution
    )

    companion object {
        fun parseMaxResolution(resolution: String): VideoResolution {
            val maxResolution: VideoResolution
            val resolutionString = resolution.toLowerCase()
            if (resolutionString == "none") {
                maxResolution = VideoResolution.Disabled
            } else if (resolutionString == "hd") {
                maxResolution = VideoResolution.VideoResolutionHD
            } else if (resolutionString == "fhd") {
                maxResolution = VideoResolution.VideoResolutionFHD
            } else {
                maxResolution = VideoResolution.VideoResolutionUHD
            }
            return maxResolution
        }
        operator fun invoke(video: String?, content: String?): MeetingFeatures {
            return MeetingFeatures(parseMaxResolution(video ?: "HD"), parseMaxResolution(content ?: "FHD"))
        }
    }
}
