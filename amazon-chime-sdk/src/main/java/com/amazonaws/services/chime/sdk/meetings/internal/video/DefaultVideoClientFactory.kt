/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.ContentShareVideoClientObserver
import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClientDataMessageListener

class DefaultVideoClientFactory : VideoClientFactory {
    override fun getVideoClient(videoClientObserver: VideoClientObserver): VideoClient {
        return VideoClient(videoClientObserver, videoClientObserver, videoClientObserver)
    }

    override fun getVideoClient(contentShareVideoClientObserver: ContentShareVideoClientObserver): VideoClient {
        return VideoClient(
            contentShareVideoClientObserver,
            contentShareVideoClientObserver,
            VideoClientDataMessageListener { Unit }
        )
    }
}
