/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.amazonaws.services.chime.sdk.meetings.analytics.toStringKeyMap
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionConfiguration
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionMetadata
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration

object EventAttributesUtils {
    fun getIngestionMetadata(ingestionConfiguration: IngestionConfiguration): IngestionMetadata {
        val metadata = ingestionConfiguration.clientConfiguration.metadataAttributes.toMutableMap()
        metadata.putAll(getCommonAttributes().toStringKeyMap())

        return metadata
    }

    fun getCommonAttributes(meetingSessionConfiguration: MeetingSessionConfiguration): EventAttributes {
        val attributes = getCommonAttributes()

        attributes.putAll(mapOf(
            EventAttributeName.meetingId to meetingSessionConfiguration.meetingId,
            EventAttributeName.attendeeId to
                    meetingSessionConfiguration.credentials.attendeeId,
            EventAttributeName.externalUserId to
                    meetingSessionConfiguration.credentials.externalUserId
        ))

        meetingSessionConfiguration.externalMeetingId?.let {
            attributes[EventAttributeName.externalMeetingId] = it
        }

        return attributes
    }

    fun getCommonAttributes(): EventAttributes {
        val attributes = mutableMapOf(
            EventAttributeName.deviceName to DeviceUtils.deviceName,
            EventAttributeName.deviceManufacturer to DeviceUtils.deviceManufacturer,
            EventAttributeName.deviceModel to DeviceUtils.deviceModel,
            EventAttributeName.mediaSdkVersion to DeviceUtils.mediaSDKVersion,
            EventAttributeName.osName to DeviceUtils.osName,
            EventAttributeName.osVersion to DeviceUtils.osVersion,
            EventAttributeName.sdkName to DeviceUtils.sdkName,
            EventAttributeName.sdkVersion to DeviceUtils.sdkVersion
        )
        return attributes as EventAttributes
    }
}
