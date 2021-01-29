/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

data class MeetingHistoryEvent(val meetingHistoryEventName: MeetingHistoryEventName, val timestamp: Long)
