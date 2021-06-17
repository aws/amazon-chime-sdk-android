/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

interface DirtyEventDao {
    /**
     * List dirty meeting events items which includes all the fields in the table.
     *
     * @param size: [Int] - size to query
     * @return list of dirty meeting event items from database
     */
    fun listDirtyMeetingEventItems(size: Int): List<DirtyMeetingEventItem>

    /**
     * Delete dirty events by given ids.
     *
     * @param ids: [List<String>] - ids of dirty events
     * @return number of events removed or -1 if delete failed
     */
    fun deleteDirtyEventsByIds(ids: List<String>): Int

    /**
     * Insert multiple dirty meeting events.
     *
     * @param dirtyEvents: [List<DirtyMeetingEventItem>] - List of DirtyMeetingEventItem
     * @return whether insertion was successful or not
     */
    fun insertDirtyMeetingEventItems(dirtyEvents: List<DirtyMeetingEventItem>): Boolean
}
