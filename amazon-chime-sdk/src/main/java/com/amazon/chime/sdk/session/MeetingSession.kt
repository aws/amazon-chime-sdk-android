package com.amazon.chime.sdk.session

import com.amazon.chime.sdk.utils.logger.Logger

interface MeetingSession {
    val configuration: MeetingSessionConfiguration
    val logger: Logger
}
