/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.analytics.DefaultEventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.DefaultMeetingStatsCollector
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.DefaultAudioVideoController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.DefaultAudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.DefaultActiveSpeakerDetector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.DefaultContentShareController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoTileFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.device.DefaultDeviceController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.DefaultContentShareVideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.DefaultContentShareVideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.DefaultClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientStateController
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNRequestParams
import com.amazonaws.services.chime.sdk.meetings.realtime.DefaultRealtimeController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultMeetingSession @JvmOverloads constructor(
    override val configuration: MeetingSessionConfiguration,
    override val logger: Logger,
    context: Context,
    eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
) : MeetingSession {

    override val audioVideo: AudioVideoFacade

    override val eventAnalyticsController: EventAnalyticsController

    init {
        val meetingStatsCollector = DefaultMeetingStatsCollector(logger)

        eventAnalyticsController = DefaultEventAnalyticsController(
            logger,
            configuration,
            meetingStatsCollector)

        val metricsCollector =
            DefaultClientMetricsCollector()
        val audioClientObserver =
            DefaultAudioClientObserver(
                logger,
                metricsCollector,
                configuration,
                meetingStatsCollector,
                eventAnalyticsController
            )

        val audioClient =
            AudioClientFactory.getAudioClient(context, audioClientObserver)

        audioClientObserver.audioClient = audioClient

        val audioClientController =
            DefaultAudioClientController(
                context,
                logger,
                audioClientObserver,
                audioClient,
                meetingStatsCollector,
                eventAnalyticsController
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
                context,
                logger,
                turnRequestParams,
                metricsCollector,
                videoClientStateController,
                configuration.urls.urlRewriter
            )

        val videoClientFactory = DefaultVideoClientFactory()

        val videoClientController =
            DefaultVideoClientController(
                context,
                logger,
                videoClientStateController,
                videoClientObserver,
                configuration,
                videoClientFactory,
                eglCoreFactory,
                eventAnalyticsController
            )

        val videoTileFactory = DefaultVideoTileFactory(logger)

        val videoTileController =
            DefaultVideoTileController(
                logger,
                videoClientController,
                videoTileFactory,
                eglCoreFactory,
                meetingStatsCollector
            )
        videoClientObserver.subscribeToVideoTileChange(videoTileController)

        val deviceController =
            DefaultDeviceController(
                context,
                audioClientController,
                videoClientController,
                eventAnalyticsController
            )

        val realtimeController =
            DefaultRealtimeController(
                audioClientController,
                audioClientObserver,
                videoClientController,
                videoClientObserver
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

        val contentShareConfiguration =
            configuration.createContentShareMeetingSessionConfiguration()

        val contentShareTurnRequestParams =
            TURNRequestParams(
                contentShareConfiguration.meetingId,
                contentShareConfiguration.urls.signalingURL,
                contentShareConfiguration.urls.turnControlURL,
                contentShareConfiguration.credentials.joinToken
            )

        val contentShareObserver =
            DefaultContentShareVideoClientObserver(
                context,
                logger,
                contentShareTurnRequestParams,
                metricsCollector,
                contentShareConfiguration.urls.urlRewriter
            )

        val contentShareVideoClientController =
            DefaultContentShareVideoClientController(
                context,
                logger,
                contentShareObserver,
                contentShareConfiguration,
                videoClientFactory,
                eglCoreFactory
            )

        val contentShareController =
            DefaultContentShareController(
                logger,
                contentShareVideoClientController
            )

        audioVideo = DefaultAudioVideoFacade(
            context,
            audioVideoController,
            realtimeController,
            deviceController,
            videoTileController,
            activeSpeakerDetector,
            contentShareController,
            eventAnalyticsController
        )
    }
}
