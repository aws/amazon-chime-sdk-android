/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource

/**
 * [ContentShareSource] contains the media sources to attach to the content share
 */
open class ContentShareSource {
    open var videoSource: VideoSource? = null
}
