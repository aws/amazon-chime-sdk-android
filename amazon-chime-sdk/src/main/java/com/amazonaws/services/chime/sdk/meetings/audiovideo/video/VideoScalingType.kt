package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoScalingType] describes the scaling type of how video is rendered.  Certain types
 * may effect how much of a video is cropped.
 */
enum class VideoScalingType {
    /**
     * Fit the frame to the surrounding view to avoid any cropping.
     */
    AspectFit,

    /**
     * Fill the surrounding view; this may crop if the aspect ratios do not match.
     */
    AspectFill
}
