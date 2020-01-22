package com.amazon.chime.sdk.session

import android.content.Context
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.DefaultAudioVideoFacade
import com.amazon.chime.sdk.media.mediacontroller.DefaultAudioVideoController
import com.amazon.chime.sdk.media.mediacontroller.DefaultRealtimeController
import com.amazon.chime.sdk.utils.logger.Logger

class DefaultMeetingSession(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger,
    context: Context
) : MeetingSession {
    override val audioVideo: AudioVideoFacade = DefaultAudioVideoFacade(
        DefaultAudioVideoController(context.applicationContext, configuration), DefaultRealtimeController()
    )
}
