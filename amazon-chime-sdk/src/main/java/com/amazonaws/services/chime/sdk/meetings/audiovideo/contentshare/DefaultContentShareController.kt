/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.ContentShareVideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultContentShareController(
    private val logger: Logger,
    private val contentShareVideoClientController: ContentShareVideoClientController
) : ContentShareController {

    private val TAG = "DefaultContentShareController"

    override fun startContentShare(source: ContentShareSource) {
        source.videoSource?.let {
            contentShareVideoClientController.startVideoShare(it)
        }
    }

    override fun stopContentShare() {
        contentShareVideoClientController.stopVideoShare()
    }

    override fun addContentShareObserver(observer: ContentShareObserver) {
        contentShareVideoClientController.subscribeToVideoClientStateChange(observer)
    }

    override fun removeContentShareObserver(observer: ContentShareObserver) {
        contentShareVideoClientController.unsubscribeFromVideoClientStateChange(observer)
    }
}
