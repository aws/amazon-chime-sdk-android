/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

enum class EventAttributeName {
    /**
     * Name of device = Manufacturer of Device + Device Model
     */
    deviceName,

    /**
     * Manufacturer of Device
     */
    deviceManufacturer,

    /**
     * Model of Device
     */
    deviceModel,

    /**
     * Version of media SDK
     */
    mediaSdkVersion,

    /**
     * Operating system name
     */
    osName,

    /**
     * Operating system version
     */
    osVersion,

    /**
     * Name of SDK
     */
    sdkName,

    /**
     * Version of SDK
     */
    sdkVersion,

    /**
     * Timestamp of event occurrence
     */
    timestampMs,

    /**
     * AttendeeId
     */
    attendeeId,

    /**
     * External Meeting Id
     */
    externalMeetingId,

    /**
     * External Attendee Id
     */
    externalUserId,

    /**
     * Meeting Id
     */
    meetingId,

    /**
     * History of the meeting events in chronological order
     */
    meetingHistory,

    // Meeting Stats Event Attributes

    /**
     * Maximum number video tile shared during the meeting, including self video tile
     */
    maxVideoTileCount,

    /**
     * Duration of the meeting
     */
    meetingDurationMs,

    /**
     * Error message of the meeting
     */
    meetingErrorMessage,

    /**
     * Meeting Status [MeetingSessionStatus]
     */
    meetingStatus,

    /**
     * The number of poor connection count during the meeting from start to end
     */
    poorConnectionCount,

    /**
     * The number of meeting retry connection count during the meeting from start to end
     */
    retryCount,

    // Device Event Attributes - videoInputFailed

    /**
     * The error of video input selection such as starting camera
     */
    videoInputError;
}
