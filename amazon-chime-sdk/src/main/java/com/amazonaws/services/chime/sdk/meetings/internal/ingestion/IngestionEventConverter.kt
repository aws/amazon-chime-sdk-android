/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionConfiguration
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionEvent
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionMetadata
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionPayload
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionRecord
import com.amazonaws.services.chime.sdk.meetings.internal.utils.EventAttributesUtils

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
object IngestionEventConverter {
    private const val NAME_KEY = "name"
    private const val TIMESTAMP_KEY = "ts"
    private const val ID_KEY = "id"
    private const val TTL_KEY = "ttl"

    fun fromDirtyMeetingEventItems(items: List<DirtyMeetingEventItem>, ingestionConfiguration: IngestionConfiguration): IngestionRecord {
        if (items.isEmpty()) {
            return IngestionRecord(mutableMapOf(), listOf())
        }

        val dirtyMeetingEventsByMeetingId: Map<String, List<DirtyMeetingEventItem>> =
            items.groupBy {
                it.data.eventAttributes[EventAttributeName.meetingId.name] as? String ?: ""
            }

        val eventMetadataKeys = ingestionConfiguration.clientConfiguration.metadataAttributes.keys
        val ingestionEvents: List<IngestionEvent> = dirtyMeetingEventsByMeetingId.map {
            IngestionEvent(
                type = ingestionConfiguration.clientConfiguration.tag,
                metadata = toIngestionMetadata(it.value.first(), eventMetadataKeys),
                payloads = it.value.map { meetingEventItem ->
                    toIngestionPayload(
                        meetingEventItem, eventMetadataKeys
                    )
                })
        }

        val rootMetadata: IngestionMetadata = EventAttributesUtils.getIngestionMetadata(ingestionConfiguration)

        return IngestionRecord(rootMetadata, ingestionEvents)
    }

    // Need to have different name due to Kotlin name collision on List
    fun fromMeetingEventItems(items: List<MeetingEventItem>, ingestionConfiguration: IngestionConfiguration): IngestionRecord {
        if (items.isEmpty()) {
            return IngestionRecord(mutableMapOf(), listOf())
        }

        val meetingEventsByMeetingId: Map<String, List<MeetingEventItem>> =
            items.groupBy {
                it.data.eventAttributes[EventAttributeName.meetingId.name] as? String ?: ""
            }

        val eventMetadataKeys = ingestionConfiguration.clientConfiguration.metadataAttributes.keys

        val ingestionEvents = meetingEventsByMeetingId.map {
            IngestionEvent(
                type = ingestionConfiguration.clientConfiguration.tag,
                metadata = toIngestionMetadata(it.value.first(), eventMetadataKeys),
                payloads = it.value.map { meetingEventItem ->
                    toIngestionPayload(
                        meetingEventItem, eventMetadataKeys
                    )
                })
        }

        val rootMetadata: IngestionMetadata = EventAttributesUtils.getIngestionMetadata(ingestionConfiguration)
        return IngestionRecord(rootMetadata, ingestionEvents)
    }

    private fun toIngestionMetadata(eventItem: MeetingEventItem, metadataAttributeKeys: Set<String>): IngestionMetadata {
        return toIngestionMetadata(eventItem.data.eventAttributes, metadataAttributeKeys)
    }

    private fun toIngestionMetadata(dirtyEventItem: DirtyMeetingEventItem, metadataAttributeKeys: Set<String>): IngestionMetadata {
        return toIngestionMetadata(dirtyEventItem.data.eventAttributes, metadataAttributeKeys)
    }

    private fun toIngestionMetadata(eventAttributes: Map<String, Any>, metadataAttributeKeys: Set<String>): IngestionMetadata {
        val ingestionMetadata = mutableMapOf<String, Any>()

        // These converts to some of overridden ingestion meta data
        for (attributeName in metadataAttributeKeys) {
            val value = eventAttributes[attributeName]
            value?.let {
                ingestionMetadata[attributeName] = it
            }
        }

        return ingestionMetadata
    }

    private fun toIngestionPayload(event: MeetingEventItem, metadataAttributeKeys: Set<String>): IngestionPayload {
        val payload: MutableMap<String, Any> = mutableMapOf(
            NAME_KEY to event.data.name,
            TIMESTAMP_KEY to (event.data.eventAttributes[EventAttributeName.timestampMs.name] as Double).toLong(),
            ID_KEY to event.id
        )
        // Filter out metadata attributes since this is not needed for payload
        event.data.eventAttributes.filterNot { metadataAttributeKeys.contains(it.key) }
            .forEach {
                payload[it.key] = it.value
            }

        return payload
    }

    private fun toIngestionPayload(dirtyEvent: DirtyMeetingEventItem, metadataAttributeKeys: Set<String>): IngestionPayload {
        val payload: MutableMap<String, Any> = mutableMapOf(
            NAME_KEY to dirtyEvent.data.name,
            TTL_KEY to dirtyEvent.ttl,
            TIMESTAMP_KEY to (dirtyEvent.data.eventAttributes[EventAttributeName.timestampMs.name] as Double).toLong(),
            ID_KEY to dirtyEvent.id
        )
        // Filter out metadata attributes since this is not needed for payload
        dirtyEvent.data.eventAttributes.filterNot { metadataAttributeKeys.contains(it.key) }
            .forEach {
                payload[it.key] = it.value
            }

        return payload
    }
}
