/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.util.Timer
import java.util.TimerTask

class DefaultEventReporter constructor(
    private val ingestionConfiguration: IngestionConfiguration,
    private val eventBuffer: EventBuffer,
    private val logger: Logger,
    // isDaemon is false to kill when app gets killed.
    // This is to make sure timer doesn't continuously run after app is killed.
    private val timer: Timer = Timer("DefaultEventReporter", false)
) : EventReporter {
    private val TAG = "DefaultEventReporter"

    private var isStarted = false
    init {
        start()
    }

    override fun report(event: SDKEvent) {
        // Add meeting id and attendee id are meta data that changes
        // from meeting to meeting. Therefore, we would need to store these
        // value just in case it was not be able to be sent.
        when (ingestionConfiguration.clientConfiguration) {
            is MeetingEventClientConfiguration -> {
                event.eventAttributes.putAll(
                    mutableMapOf(
                        EventAttributeName.meetingId to ingestionConfiguration.clientConfiguration.meetingId,
                        EventAttributeName.attendeeId to ingestionConfiguration.clientConfiguration.attendeeId
                    )
                )
            }
        }

        eventBuffer.add(event)
    }

    override fun start() {
        if (isStarted) {
            return
        }

        timer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() = eventBuffer.process()
            },
            ingestionConfiguration.flushIntervalMs,
            ingestionConfiguration.flushIntervalMs
        )
    }

    override fun stop() {
        if (!isStarted) {
            return
        }

        timer.cancel()
    }
}
