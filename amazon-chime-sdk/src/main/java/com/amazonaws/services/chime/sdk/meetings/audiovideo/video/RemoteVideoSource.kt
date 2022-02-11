package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

class RemoteVideoSource(val attendeeId: String) {
    override fun equals(o: Any?): Boolean {
        if (o == this) {
            return true
        }

        if (o !is RemoteVideoSource) {
            return false
        }

        return this.attendeeId == o.attendeeId
    }
}
