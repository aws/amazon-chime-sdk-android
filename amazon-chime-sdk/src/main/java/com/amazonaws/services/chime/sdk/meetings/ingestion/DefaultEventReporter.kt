/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

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
        val updatedEvent = event.putAttributes(ingestionConfiguration.clientConfiguration.metadataAttributes)

        eventBuffer.add(updatedEvent)
    }

    override fun start() {
        if (isStarted || ingestionConfiguration.disabled) {
            return
        }

        isStarted = true

        try {
            timer.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() = eventBuffer.process()
                },
                ingestionConfiguration.flushIntervalMs,
                ingestionConfiguration.flushIntervalMs
            )
        } catch (error: Error) {
            logger.error(TAG, "Failed to start timer for event buffer to process")
            isStarted = false
        }
    }

    override fun stop() {
        if (!isStarted) {
            return
        }

        timer.cancel()

        isStarted = false
    }
}
