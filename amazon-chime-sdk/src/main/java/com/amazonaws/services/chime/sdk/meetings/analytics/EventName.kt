/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

/**
 * [EventName] represent some major event that could help builders to analyze the data.
 */
enum class EventName {
    /**
     * The camera selection failed.
     */
    videoInputFailed,

    /**
     * The meeting will start.
     */
    meetingStartRequested,

    /**
     * The meeting started.
     */
    meetingStartSucceeded,

    /**
     * The meeting failed to start.
     */
    meetingStartFailed,

    /**
     * The meeting ended.
     */
    meetingEnded,

    /**
     * The meeting ended with failure.
     */
    meetingFailed;
}
