/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import java.util.UUID

/**
 * [MeetingEventItem] is a data type of an entry inside Events SQLite table.
 *
 * @property id unique id of the database entry
 * @property data data/meeting event associated with the entry
 */
data class MeetingEventItem(
    val id: String = UUID.randomUUID().toString(),
    val data: SDKEvent
)
