/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime.datamessage

/**
 * [DataMessageObserver] lets one listen to data message receiving event.
 * One can subscribe this observer to multiple data message topic in order
 * to receive and process the message that sent to the topics.
 *
 *  Note: callback will be called on main thread.
 */
interface DataMessageObserver {
    /**
     * Handles data message being received.
     *
     * @param dataMessage: [DataMessage] - data message being received
     */
    fun onDataMessageReceived(dataMessage: DataMessage)
}
