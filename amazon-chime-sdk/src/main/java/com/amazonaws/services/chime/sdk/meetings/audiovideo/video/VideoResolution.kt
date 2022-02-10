package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

enum class VideoResolution(val width: Int, val height: Int) {
    LOW(120, 140),
    MEDIUM(640, 480),
    HIGH(960, 720);
}