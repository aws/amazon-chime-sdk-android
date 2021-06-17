/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * [MeetingEventClientConfiguration] defines one type of [EventClientConfiguration]
 * that is needed for [DefaultEventReporter]
 *
 * @property eventClientJoinToken: [String] - an authorization token to send
 * @property meetingId: [String] - meeting id
 * @property attendeeId: [String] - attendee id
 */
data class MeetingEventClientConfiguration(
    override val eventClientJoinToken: String,
    val meetingId: String,
    val attendeeId: String
) : EventClientConfiguration {
    override val type: EventClientType = EventClientType.Meet
}
