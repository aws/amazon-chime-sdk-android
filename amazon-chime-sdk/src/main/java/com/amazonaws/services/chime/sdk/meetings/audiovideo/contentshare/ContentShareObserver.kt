/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

/**
 * [ContentShareObserver] handles all callbacks related to the content share.
 * By implementing the callback functions and registering with [ContentShareController.addContentShareObserver],
 * one can get notified with content share status events.
 */
interface ContentShareObserver {
    /**
     * Called when the content share has started.
     * This callback will be on the main thread.
     */
    fun onContentShareStarted()

    /**
     * Called when the content is no longer shared with other attendees
     * with the reason provided in the status.
     *
     * If you no longer need the source producing frames,
     * most builders can stop the source after this callback is invoked.
     *
     * @param status: [ContentShareStatus] - the reason why the content share has stopped
     */
    fun onContentShareStopped(status: ContentShareStatus)
}
