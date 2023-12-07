package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * Customizable video resolution parameters for a remote video source.
 */
enum class VideoResolution(val width: Int, val height: Int) {
    Low(360, 240),
    Medium(640, 480),
    High(960, 720),
    Disabled(0, 0),
    VideoResolutionHD(1280, 720),
    VideoResolutionFHD(1920, 1080),
    VideoResolutionUHD(3840, 2160),
}
