/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

interface EventDao {
    /**
     * List meeting events items which includes all the fields in the table.
     *
     * @param size: [Int] - size of events to list
     * @return list of [MeetingEventItem]
     */
    fun listMeetingEventItems(size: Int): List<MeetingEventItem>

    /**
     *  Delete meeting events by given ids.
     *
     * @param ids: [List<String>] - list of ids to remove
     * @return number of events removed or -1 if delete failed
     */
    fun deleteMeetingEventsByIds(ids: List<String>): Int

    /**
     * Insert a meeting event item.
     *
     * @param event: [MeetingEventItem] - meeting event to insert
     * @return whether insertion is successful or not
     */
    fun insertMeetingEvent(event: MeetingEventItem): Boolean
}
