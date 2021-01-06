/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.xodee.client.video.VideoClientDelegate
import com.xodee.client.video.VideoClientLogListener

/**
 * [ContentShareVideoClientObserver] handles all callbacks related to the separate video client
 * for content share and allows higher level components to observe the lower level Video Client events.
 */
interface ContentShareVideoClientObserver : VideoClientDelegate, VideoClientLogListener {

    /**
     * Subscribe to video client state and connection events with an [ContentShareObserver]
     *
     * @param observer: [ContentShareObserver] - The observer to subscribe to events with.
     */
    fun subscribeToVideoClientStateChange(observer: ContentShareObserver)

    /**
     * Unsubscribe from video client state and connection events by removing the specified [ContentShareObserver]
     *
     * @param observer: [ContentShareObserver] - The observer to unsubscribe from events with.
     */
    fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver)
}
