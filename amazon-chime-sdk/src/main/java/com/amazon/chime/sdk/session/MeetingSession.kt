/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.utils.logger.Logger

/**
 * [[MeetingSession]] contains everything needed for the attendee to authenticate,
 * reach the meeting service, start audio, and start video
 */
interface MeetingSession {
    val configuration: MeetingSessionConfiguration
    val logger: Logger
    val audioVideo: AudioVideoFacade
}
