package com.amazon.chime.sdk.session

import android.content.Context
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.DefaultAudioVideoFacade
import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.DefaultAudioClientController
import com.amazon.chime.sdk.media.clientcontroller.DefaultAudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.media.clientcontroller.VideoClientControllerParams
import com.amazon.chime.sdk.media.devicecontroller.DefaultDeviceController
import com.amazon.chime.sdk.media.mediacontroller.DefaultAudioVideoController
import com.amazon.chime.sdk.media.mediacontroller.DefaultRealtimeController
import com.amazon.chime.sdk.media.mediacontroller.video.DefaultVideoTileController
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileController
import com.amazon.chime.sdk.utils.logger.Logger

class DefaultMeetingSession(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger,
    context: Context
) : MeetingSession {

    override val audioVideo: AudioVideoFacade

    init {
        val audioClientObserver: AudioClientObserver = DefaultAudioClientObserver(logger)
        val audioClientController: AudioClientController = DefaultAudioClientController(
            context,
            logger,
            audioClientObserver
        )
        val videoTileController: VideoTileController = DefaultVideoTileController(logger)
        val videoClientController =
            VideoClientController.getInstance(
                VideoClientControllerParams(
                    context,
                    logger
                )
            )
        videoClientController.subscribeToVideoTile(videoTileController)
        val audioVideoController = DefaultAudioVideoController(configuration, audioClientController, audioClientObserver, videoClientController)
        val realtimeController =
            DefaultRealtimeController(audioClientController, audioClientObserver)
        val deviceController = DefaultDeviceController(context, audioClientController, videoClientController)
        audioVideo = DefaultAudioVideoFacade(
            context,
            audioVideoController,
            realtimeController,
            deviceController,
            videoTileController
        )
    }
}
