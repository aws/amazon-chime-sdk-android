package com.amazon.chime.sdk.session

import com.amazon.chime.sdk.utils.logger.Logger

class DefaultMeetingSession(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger
) : MeetingSession
