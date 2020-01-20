package com.amazon.chime.sdk.session

enum class MeetingSessionStatusCode(val value: Int) {
    /**
     * Everything is OK so far.
     */
    OK(0),

    /**
     * Network is not good enough for VoIP.
     */
    NetworkIsNotGoodEnoughForVoIP(59),

    /**
     * Server hung up.
     */
    AudioServerHungup(60),

    /**
     * The attendee joined from another device.
     */
    AudioJoinedFromAnotherDevice(61),

    /**
     * There was an internal server error with the audio leg.
     */
    AudioInternalServerError(62),

    /**
     * Authentication was rejected. The client is not allowed on this call.
     */
    AudioAuthenticationRejected(63),

    /**
     * The client can not join because the call is at capacity.
     */
    AudioCallAtCapacity(64),

    /**
     * The attendee should explicitly switch itself from joined with audio to
     * checked-in.
     */
    AudioDisconnectAudio(69);

    companion object {
        fun fromValue(value: Int): MeetingSessionStatusCode? {
            when (value) {
                0 -> return OK
                59 -> return NetworkIsNotGoodEnoughForVoIP
                60 -> return AudioServerHungup
                61 -> return AudioJoinedFromAnotherDevice
                62 -> return AudioInternalServerError
                63 -> return AudioAuthenticationRejected
                64 -> return AudioCallAtCapacity
                69 -> return AudioDisconnectAudio
                else -> return null
            }
        }
    }
}
