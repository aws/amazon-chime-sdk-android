/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.amazonaws.services.chime.sdk.meetings.ingestion.EventClientType
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionConfiguration
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionEvent
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionMetadata
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionPayload
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionRecord
import com.amazonaws.services.chime.sdk.meetings.internal.utils.EventAttributesUtils

object IngestionEventConverter {
    private const val NAME_KEY = "name"
    private const val TIMESTAMP_KEY = "ts"
    private const val ID_KEY = "id"
    private const val TTL_KEY = "ttl"

    private val attributesToFilter =
        setOf(EventAttributeName.meetingId, EventAttributeName.attendeeId)
    private val eventMetadataAttributeNames =
        listOf(EventAttributeName.meetingId, EventAttributeName.attendeeId)

    fun fromDirtyMeetingEventItems(items: List<DirtyMeetingEventItem>, ingestionConfiguration: IngestionConfiguration): IngestionRecord {
        if (items.isEmpty()) {
            return IngestionRecord(mutableMapOf(), listOf())
        }

        // When sending a batch, server accepts the data as
        // "metadata": { "meetingId": "meetingId0" },
        // "events": [
        //   {
        //      "metadata": { "meetingId": "meetingId2" } // This overrides record-level metadata
        //      "type": "Meet",
        //      "v": 1,
        //      "payloads": []
        //   },
        //   {
        //      "metadata": { "meetingId": "meetingId1" } // This overrides record-level metadata
        //      "type": "Meet",
        //      "v": 1,
        //      "payloads": []
        //   },
        // ]
        // This is to group events so that each event contains metadata to override the record-level metadata
        // in case they are different.
        val dirtyMeetingEventsByMeetingId: Map<String, List<DirtyMeetingEventItem>> =
            items.groupBy {
                it.data.eventAttributes[EventAttributeName.meetingId] as? String ?: ""
            }

        val ingestionEvents: List<IngestionEvent> = dirtyMeetingEventsByMeetingId.map {
            IngestionEvent(
                EventClientType.Meet,
                toIngestionMetadata(it.value.first()),
                it.value.map { meetingEventItem ->
                    toIngestionPayload(
                        meetingEventItem
                    )
                })
        }

        val rootMetadata: IngestionMetadata = EventAttributesUtils.getCommonAttributes(ingestionConfiguration)

        return IngestionRecord(rootMetadata, ingestionEvents)
    }

    // Need to have different name due to Kotlin name collision on List
    fun fromMeetingEventItems(items: List<MeetingEventItem>, ingestionConfiguration: IngestionConfiguration): IngestionRecord {
        if (items.isEmpty()) {
            return IngestionRecord(mutableMapOf(), listOf())
        }

        // When sending a batch, server accepts the data as
        // "events": [
        //   {
        //      "metadata": { "meetingId": "meetingId2" } // This overrides record-level metadata
        //      "type": "Meet",
        //      "v": 1,
        //      "payloads": []
        //   },
        //   {
        //      "metadata": { "meetingId": "meetingId1" } // This overrides record-level metadata
        //      "type": "Meet",
        //      "v": 1,
        //      "payloads": []
        //   },
        // ]
        // This is to group events so that each event contains metadata to override the record-level metadata
        // in case they are different.
        val meetingEventsByMeetingId: Map<String, List<MeetingEventItem>> =
            items.groupBy {
                it.data.eventAttributes[EventAttributeName.meetingId] as? String ?: ""
            }

        val ingestionEvents = meetingEventsByMeetingId.map {
            IngestionEvent(
                EventClientType.Meet,
                toIngestionMetadata(it.value.first()),
                it.value.map { meetingEventItem ->
                    toIngestionPayload(
                        meetingEventItem
                    )
                })
        }

        val rootMetadata: IngestionMetadata = EventAttributesUtils.getCommonAttributes(ingestionConfiguration)
        return IngestionRecord(rootMetadata, ingestionEvents)
    }

    private fun toIngestionMetadata(eventItem: MeetingEventItem): IngestionMetadata {
        return toIngestionMetadata(eventItem.data.eventAttributes)
    }

    private fun toIngestionMetadata(dirtyEventItem: DirtyMeetingEventItem): IngestionMetadata {
        return toIngestionMetadata(dirtyEventItem.data.eventAttributes)
    }

    private fun toIngestionMetadata(eventAttributes: EventAttributes): IngestionMetadata {
        val ingestionMetadata = mutableMapOf<EventAttributeName, Any>()

        // These converts to some of overridden ingestion meta data
        for (attributeName in eventMetadataAttributeNames) {
            val value = eventAttributes[attributeName]
            value?.let {
                ingestionMetadata[attributeName] = it
            }
        }

        return ingestionMetadata
    }

    private fun toIngestionPayload(event: MeetingEventItem): IngestionPayload {
        val payload = mutableMapOf(
            NAME_KEY to event.data.name,
            TIMESTAMP_KEY to (event.data.eventAttributes[EventAttributeName.timestampMs] as Double).toLong(),
            ID_KEY to event.id
        )
        // Filter out meeting id and attendee id since this is not needed for payload
        event.data.eventAttributes.filterNot { attributesToFilter.contains(it.key) }
            .forEach {
                payload[it.key.name] = it.value
            }

        return payload
    }

    private fun toIngestionPayload(dirtyEvent: DirtyMeetingEventItem): IngestionPayload {
        val payload = mutableMapOf(
            NAME_KEY to dirtyEvent.data.name,
            TTL_KEY to dirtyEvent.ttl,
            TIMESTAMP_KEY to (dirtyEvent.data.eventAttributes[EventAttributeName.timestampMs] as Double).toLong(),
            ID_KEY to dirtyEvent.id
        )
        dirtyEvent.data.eventAttributes.filterNot { attributesToFilter.contains(it.key) }
            .forEach {
                payload[it.key.name] = it.value
            }

        return payload
    }
}
