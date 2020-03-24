/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

/**
 * [MeetingSessionStatus] indicates a status received regarding the session.
 *
 * @param statusCode: [MeetingSessionStatusCode] - Additional details for the status
 */
data class MeetingSessionStatus(val statusCode: MeetingSessionStatusCode?) {
    constructor(statusCodeValue: Int) : this(MeetingSessionStatusCode.from(statusCodeValue))
}
