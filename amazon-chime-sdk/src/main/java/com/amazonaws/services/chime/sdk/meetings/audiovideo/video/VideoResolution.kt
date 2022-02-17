package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/*
 * Customizable video resolution parameters for a remote video source.
 */
enum class VideoResolution(val width: Int, val height: Int) {
    low(120, 140),
    medium(640, 480),
    high(960, 720);
}
