/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource

/**
 * [ContentShareController] exposes methods for starting and stopping content share with a [ContentShareSource].
 * The content represents a media steam to be shared in the meeting, such as screen capture or
 * media files.
 * Read [content share guide](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/content_share.md) for details.
 */
interface ContentShareController {
    /**
     * Start sharing the content of a given [ContentShareSource].
     *
     * Once sharing has started successfully, [ContentShareObserver.onContentShareStarted] will
     * be invoked. If sharing fails or stops, [ContentShareObserver.onContentShareStopped]
     * will be invoked with [ContentShareStatus] as the cause.
     *
     * This will call [VideoSource.addVideoSink] on the provided source
     * and [VideoSource.removeVideoSink] on the previously provided source.
     *
     * Calling this function repeatedly will replace the previous [ContentShareSource] as the one being
     * transmitted.
     *
     * @param source: [ContentShareSource] - The source of content to be shared.
     */
    fun startContentShare(source: ContentShareSource)

    /**
     * Stop sharing the content of a [ContentShareSource] that previously started.
     *
     * Once the sharing stops successfully, [ContentShareObserver.onContentShareStopped]
     * will be invoked with status code [ContentShareStatusCode.OK].
     *
     */
    fun stopContentShare()

    /**
     * Subscribe the given observer to content share events (sharing started and stopped).
     *
     * @param observer: [ContentShareObserver] - The observer to be notified for events.
     */
    fun addContentShareObserver(observer: ContentShareObserver)

    /**
     * Unsubscribe the given observer from content share events.
     *
     * @param observer: [ContentShareObserver] - The observer to be removed for events.
     */
    fun removeContentShareObserver(observer: ContentShareObserver)
}
