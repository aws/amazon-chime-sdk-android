/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.google.gson.annotations.SerializedName

typealias IngestionPayload = Map<String, Any>

/**
 * [IngestionEvent] defines the event format ingestion server will accept
 *
 * @property type: [EventClientType] - type of Event.
 * @property payloads: [List<Map<String, Any>] - list of map that contains details of event
 * @property version: [Int] - version of this event. If the format changes, it will have different version.
 */
data class IngestionEvent(
    val type: EventClientType,
    val metadata: IngestionMetadata,
    val payloads: List<IngestionPayload>,
    @SerializedName("v")
    val version: Int = 1
)
