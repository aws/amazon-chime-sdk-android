/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.ContentShareVideoClientObserver
import com.xodee.client.video.VideoClient

interface VideoClientFactory {
    /**
     * Get a [VideoClient]
     *
     * @param videoClientObserver: [VideoClientObserver] - observer for video client
     */
    fun getVideoClient(videoClientObserver: VideoClientObserver): VideoClient

    /**
     * Get a [VideoClient]
     *
     * @param contentShareVideoClientObserver: [ContentShareVideoClientObserver] - observer for video client
     */
    fun getVideoClient(contentShareVideoClientObserver: ContentShareVideoClientObserver): VideoClient
}
