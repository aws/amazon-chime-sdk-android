package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

class VideoSubscriptionConfiguration(var priority: VideoPriority, var resolution: VideoResolution) {
    override fun equals(o: Any?): Boolean {
        if (o == this) {
            return true
        }

        if (o !is VideoSubscriptionConfiguration) {
            return false
        }

        return this.priority == o.priority && this.resolution == o.resolution
    }
}
