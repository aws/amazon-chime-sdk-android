/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.DefaultAudioVideoController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.DefaultAudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.DefaultActiveSpeakerDetector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoTileFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.device.DefaultDeviceController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.metric.DefaultClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientStateController
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNRequestParams
import com.amazonaws.services.chime.sdk.meetings.realtime.DefaultRealtimeController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient

class DefaultMeetingSession(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger,
    context: Context
) : MeetingSession {

    override val audioVideo: AudioVideoFacade

    init {
        val metricsCollector: ClientMetricsCollector =
            DefaultClientMetricsCollector()
        val audioClientObserver: AudioClientObserver =
            DefaultAudioClientObserver(
                logger,
                metricsCollector
            )

        val audioClient: AudioClient =
            AudioClientFactory.getAudioClient(context, audioClientObserver)

        val audioClientController: AudioClientController =
            DefaultAudioClientController(
                logger,
                audioClientObserver,
                audioClient
            )

        val realtimeController =
            DefaultRealtimeController(
                audioClientController,
                audioClientObserver
            )

        val turnRequestParams =
            TURNRequestParams(
                configuration.meetingId,
                configuration.urls.signalingURL,
                configuration.urls.turnControlURL,
                configuration.credentials.joinToken
            )

        val videoClientStateController =
            DefaultVideoClientStateController(
                logger
            )
        val videoClientObserver =
            DefaultVideoClientObserver(
                logger,
                turnRequestParams,
                metricsCollector,
                videoClientStateController
            )

        val videoClientController =
            DefaultVideoClientController(
                context,
                logger,
                videoClientStateController,
                videoClientObserver
            )

        val videoTileFactory = DefaultVideoTileFactory(logger)

        val videoTileController: VideoTileController =
            DefaultVideoTileController(logger, videoClientController, videoTileFactory)

        videoClientObserver.subscribeToVideoTileChange(videoTileController)
        val deviceController =
            DefaultDeviceController(
                context,
                audioClientController,
                videoClientController
            )

        val activeSpeakerDetector = DefaultActiveSpeakerDetector(audioClientObserver)

        val audioVideoController =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                metricsCollector,
                configuration,
                videoClientController,
                videoClientObserver
            )

        audioVideo = DefaultAudioVideoFacade(
            context,
            audioVideoController,
            realtimeController,
            deviceController,
            videoTileController,
            activeSpeakerDetector
        )
    }
}
