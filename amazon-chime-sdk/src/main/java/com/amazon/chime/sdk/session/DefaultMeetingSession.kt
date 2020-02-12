package com.amazon.chime.sdk.session

import android.content.Context
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.DefaultAudioVideoFacade
import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientControllerParams
import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.media.clientcontroller.VideoClientControllerParams
import com.amazon.chime.sdk.media.devicecontroller.DefaultDeviceController
import com.amazon.chime.sdk.media.mediacontroller.DefaultAudioVideoController
import com.amazon.chime.sdk.media.mediacontroller.DefaultRealtimeController
import com.amazon.chime.sdk.utils.logger.Logger

class DefaultMeetingSession(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger,
    context: Context
) : MeetingSession {

    override val audioVideo: AudioVideoFacade

    init {
        val audioClientController =
            AudioClientController.getInstance(AudioClientControllerParams(context, logger))
        val videoClientController =
            VideoClientController.getInstance(VideoClientControllerParams(context, logger))

        val audioVideoController = DefaultAudioVideoController(audioClientController, videoClientController, configuration)
        val realtimeController = DefaultRealtimeController(audioClientController)
        val deviceController = DefaultDeviceController(context, audioClientController)
        audioVideo = DefaultAudioVideoFacade(context, audioVideoController, realtimeController, deviceController)
    }
}
