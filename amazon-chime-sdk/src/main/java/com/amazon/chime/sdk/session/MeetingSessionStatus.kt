package com.amazon.chime.sdk.session

/**
 * [[MeetingSessionStatus]] indicates a status received regarding the session.
 *
 * @param statusCode: [MeetingSessionStatusCode] - Additional details for the status
 */
data class MeetingSessionStatus(val statusCode: MeetingSessionStatusCode?) {
    constructor(statusCodeValue: Int) : this(MeetingSessionStatusCode.from(statusCodeValue))
}
