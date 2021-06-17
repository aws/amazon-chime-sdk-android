/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import java.util.UUID

/**
 * [DirtyMeetingEventItem] is a data type of an entry inside DirtyEvents Sqlite table.
 *
 * @property id unique id of the database entry
 * @property data data/meeting event associated with the entry
 * @property ttl lifetime of that entry that will be checked when ingestion service runs
 */
data class DirtyMeetingEventItem(
    val id: String = UUID.randomUUID().toString(),
    val data: SDKEvent,
    val ttl: Long
)
