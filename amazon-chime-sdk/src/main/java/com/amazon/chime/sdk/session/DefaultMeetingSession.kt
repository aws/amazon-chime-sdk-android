/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

import android.content.Context
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.DefaultAudioVideoFacade
import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientFactory
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.ClientMetricsCollector
import com.amazon.chime.sdk.media.clientcontroller.DefaultAudioClientController
import com.amazon.chime.sdk.media.clientcontroller.DefaultAudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.DefaultClientMetricsCollector
import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.media.devicecontroller.DefaultDeviceController
import com.amazon.chime.sdk.media.mediacontroller.DefaultAudioVideoController
import com.amazon.chime.sdk.media.mediacontroller.DefaultRealtimeController
import com.amazon.chime.sdk.media.mediacontroller.video.DefaultVideoTileController
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileController
import com.amazon.chime.sdk.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient

class DefaultMeetingSession(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger,
    context: Context
) : MeetingSession {

    override val audioVideo: AudioVideoFacade

    init {
        val metricsCollector: ClientMetricsCollector = DefaultClientMetricsCollector()
        val audioClientObserver: AudioClientObserver =
            DefaultAudioClientObserver(logger, metricsCollector)

        val audioClient: AudioClient =
            AudioClientFactory.getAudioClient(context, audioClientObserver)

        val audioClientController: AudioClientController = DefaultAudioClientController(
            logger,
            audioClientObserver,
            audioClient
        )

        val realtimeController =
            DefaultRealtimeController(audioClientController, audioClientObserver)

        val videoTileController: VideoTileController = DefaultVideoTileController(logger)
        val videoClientController = VideoClientController(context, logger)

        videoClientController.subscribeToVideoTile(videoTileController)
        val deviceController =
            DefaultDeviceController(context, audioClientController, videoClientController)

        val audioVideoController =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                metricsCollector,
                configuration,
                videoClientController
            )

        audioVideo = DefaultAudioVideoFacade(
            context,
            audioVideoController,
            realtimeController,
            deviceController,
            videoTileController
        )
    }
}
