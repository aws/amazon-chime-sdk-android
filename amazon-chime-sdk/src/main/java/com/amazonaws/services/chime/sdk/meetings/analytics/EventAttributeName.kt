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
     * Duration of the meeting starting process
     */
    meetingStartDurationMs,

    /**
     * Duration of the meeting reconnect process
     */
    meetingReconnectDurationMs,

    /**
     * Duration of the meeting
     */
    meetingDurationMs,

    /**
     * Error message of the meeting
     */
    meetingErrorMessage,

    /**
     * The application state
     */
    appState,

    /**
     * The current battery level
     */
    batteryLevel,

    /**
     * The current battery state
     */
    batteryState,

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
     * The error of audio input selection or access such as starting microphone
     */
    audioInputErrorMessage,

    /**
     * The error of video input selection or access such as starting camera
     */
    videoInputErrorMessage,

    /**
     * The error message that explains why the signaling websocket connection dropped
     */
    signalingDroppedErrorMessage,

    /**
     * The error message that explains why content share failed
     */
    contentShareErrorMessage,

    /**
     * The error message explaining why enabling or disabling Voice Focus failed
     */
    voiceFocusErrorMessage,

    /**
     * The selected audio device type
     */
    audioDeviceType,

    /**
     * The selected video device type
     */
    videoDeviceType,

    /**
     * Whether low power mode is currently enabled
     */
    lowPowerModeEnabled,

    /**
     * The network connection type
     */
    networkConnectionType,

    /**
     * The time taken for connection's ICE gathering state to complete
     */
    iceGatheringDurationMs,

    /**
     * The time taken for opening a WebSocket connection.
     */
    signalingOpenDurationMs;
}
