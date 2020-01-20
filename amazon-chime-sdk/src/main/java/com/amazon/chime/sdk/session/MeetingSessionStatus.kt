package com.amazon.chime.sdk.session

/**
 * [[MeetingSessionStatus]] indicates a status received regarding the session.
 */
data class MeetingSessionStatus(val statusCode: MeetingSessionStatusCode?) {
    constructor(statusCodeValue: Int) : this(MeetingSessionStatusCode.fromValue(statusCodeValue))
}
