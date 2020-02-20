package com.amazon.chime.sdk.session

/**
 * [[MeetingSessionStatusCode]] provides additional details for the [[MeetingSessionStatus]]
 * received for a session.
 */
enum class MeetingSessionStatusCode(val value: Int) {
    /**
     * Everything is OK so far.
     */
    OK(0),

    /**
     * The attendee left the meeting normally.
     */
    Left(1),

    /**
     * The attendee joined from another device.
     */
    AudioJoinedFromAnotherDevice(2),

    /**
     * The attendee should explicitly switch itself from joined with audio to
     * checked-in.
     */
    AudioDisconnectAudio(3),

    /**
     * Authentication was rejected. The client is not allowed on this call.
     */
    AudioAuthenticationRejected(4),

    /**
     * The client can not join because the call is at capacity.
     */
    AudioCallAtCapacity(5),

    /**
     * The call was ended.
     */
    AudioCallEnded(6),

    /**
     * There was an internal server error with the audio leg.
     */
    AudioInternalServerError(7),

    /**
     * Could not connect the audio leg due to the service being unavailable.
     */
    AudioServiceUnavailable(8),

    /**
     * The audio leg failed.
     */
    AudioDisconnected(9),

    /**
     * Due to connection health, a reconnect has been triggered.
     */
    ConnectionHealthReconnect(10),

    /**
     * The network has become poor and is no longer good enough for VoIP.
     */
    NetworkBecamePoor(11),

    /**
     * Video Client Failed.
     */
    VideoServiceFailed(12);

    companion object {
        fun from(intValue: Int): MeetingSessionStatusCode? = values().find { it.value == intValue }
    }
}
