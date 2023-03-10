/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.DirtyEventDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.DirtyMeetingEventItem
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.EventDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.IngestionEventConverter
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.MeetingEventItem
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DefaultMeetingEventBuffer @JvmOverloads constructor(
    private val ingestionConfiguration: IngestionConfiguration,
    private val eventDao: EventDao,
    private val dirtyEventDao: DirtyEventDao,
    private val eventSender: EventSender,
    private val logger: Logger,
    private val eventScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : EventBuffer {
    private val TAG = "DefaultMeetingEventBuffer"
    private val TWO_DAYS_IN_MILLISECONDS = 172_800_000

    private var isRunning = false

    init {
        // process dirty events when init
        processDirtyEvents()
    }

    override fun add(item: SDKEvent) {
        val meetingEventItem = MeetingEventItem(data = item)
        // Meeting event is also added in case of immediate send
        // due to the fact that people will likely close the app,
        // leading to possible loss of data. To make sure data is not lost,
        // we first add it to the database.
        eventDao.insertMeetingEvent(meetingEventItem)

        if (shouldProcessImmediately(item)) {
            eventScope.launch {
                processEvents(listOf(meetingEventItem))
            }
        }
    }

    override fun process() {
        eventScope.launch {
            if (isRunning) {
                return@launch
            }
            isRunning = true

            val meetingEventItems = eventDao.listMeetingEventItems(ingestionConfiguration.flushSize)
            processEvents(meetingEventItems)
            isRunning = false
        }
    }

    private suspend fun processEvents(eventItems: List<MeetingEventItem>) {
        /*
         * This will do few things
         * 1. Send data if not empty
         * 2. Retry if it is failure
         * 3. If success, remove from event
         * 4. If failure, add it to dirty event
         * 5. Remove ids from event table
         */
        try {
            if (eventItems.isEmpty()) {
                return
            }

            val eventRecord =
                IngestionEventConverter.fromMeetingEventItems(eventItems, ingestionConfiguration)

            val idsToRemove = eventItems.map { it.id }.toList()

            val isSuccess = eventSender.sendRecord(eventRecord)

            if (!isSuccess) {
                logger.warn(
                    TAG,
                    "Unable to publish data to ingestion service. Putting it in the dirtyMeetingEvent"
                )

                dirtyEventDao.insertDirtyMeetingEventItems(
                    toDirtyMeetingEventItems(
                        eventItems.map { it.data }.toList()
                    )
                )
            }
            eventDao.deleteMeetingEventsByIds(idsToRemove)
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Unable to process event ${exception.localizedMessage}"
            )
        }
    }

    private fun toDirtyMeetingEventItems(events: List<SDKEvent>): List<DirtyMeetingEventItem> {
        val currentTime = Calendar.getInstance().timeInMillis
        return events.map {
            DirtyMeetingEventItem(
                data = it,
                ttl = currentTime + TWO_DAYS_IN_MILLISECONDS
            )
        }.toList()
    }

    private fun shouldProcessImmediately(
        event: SDKEvent
    ): Boolean {
        val eventName = event.name
        val eventAttributes = event.eventAttributes

        return (eventName == EventName.meetingFailed.name &&
                eventAttributes[EventAttributeName.meetingStatus.name] == MeetingSessionStatusCode.AudioAuthenticationRejected ||
                eventAttributes[EventAttributeName.meetingStatus.name] == MeetingSessionStatusCode.AudioInternalServerError ||
                eventAttributes[EventAttributeName.meetingStatus.name] == MeetingSessionStatusCode.AudioServiceUnavailable ||
                eventAttributes[EventAttributeName.meetingStatus.name] == MeetingSessionStatusCode.AudioDisconnected)
    }

    private fun processDirtyEvents() {
        eventScope.launch {
            try {
                val validEvents = getValidDirtyEvents().toMutableList()
                // Send valid events
                var isSentSuccessful = true
                // There is an known issue with .isNotEmpty() in Mockk in 1.10.0,
                // it has been fixed in new version, however, SDK unit tests fails with new version
                // Will use dirtyEvents.size instead for now
                while (validEvents.size > 0 && isSentSuccessful) {
                    val ingestionRecord =
                        IngestionEventConverter.fromDirtyMeetingEventItems(validEvents, ingestionConfiguration)
                    isSentSuccessful = eventSender.sendRecord(ingestionRecord)

                    // Find the ids that will be removed based on success status
                    if (isSentSuccessful) {
                        dirtyEventDao.deleteDirtyEventsByIds(validEvents.map { it.id })
                    }
                    validEvents.clear()
                    validEvents.addAll(getValidDirtyEvents())
                }
            } catch (exception: Exception) {
                logger.error(TAG, "Unable to process dirty events $exception")
            }
        }
    }

    private fun getValidDirtyEvents(): List<DirtyMeetingEventItem> {
        // Get next batch of dirty events
        val dirtyEvents =
            dirtyEventDao.listDirtyMeetingEventItems(ingestionConfiguration.flushSize)
        val validEvents = mutableListOf<DirtyMeetingEventItem>()
        val invalidEventIds = mutableListOf<String>()

        // Filter valid events
        val currentTime = Calendar.getInstance().timeInMillis
        for (event in dirtyEvents) {
            if (event.ttl > currentTime &&
                event.data.eventAttributes.isNotEmpty() &&
                event.data.eventAttributes[EventAttributeName.timestampMs.name] != null) {
                validEvents.add(event)
            } else {
                invalidEventIds.add(event.id)
            }
        }
        // Delete invalid / expired events
        dirtyEventDao.deleteDirtyEventsByIds(invalidEventIds)

        return validEvents
    }
}
