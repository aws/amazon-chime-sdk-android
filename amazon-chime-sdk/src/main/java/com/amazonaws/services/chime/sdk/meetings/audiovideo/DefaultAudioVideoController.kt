/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import java.util.Timer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.timerTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultAudioVideoController(
    private val audioClientController: AudioClientController,
    private val audioClientObserver: AudioClientObserver,
    private val clientMetricsCollector: ClientMetricsCollector,
    private val configuration: MeetingSessionConfiguration,
    private val videoClientController: VideoClientController,
    private val videoClientObserver: VideoClientObserver
) : AudioVideoControllerFacade {

    companion object {
        private const val PRIMARY_MEETING_PROMOTION_TIMEOUT = 5000L
    }
    private var primaryMeetingPromotionObserver: PrimaryMeetingPromotionObserver? = null

    override fun start() {
        start(AudioVideoConfiguration())
    }

    override fun start(audioVideoConfiguration: AudioVideoConfiguration) {
        audioClientController.start(
            audioFallbackUrl = configuration.urls.audioFallbackURL,
            audioHostUrl = configuration.urls.audioHostURL,
            meetingId = configuration.meetingId,
            attendeeId = configuration.credentials.attendeeId,
            joinToken = configuration.credentials.joinToken,
            audioMode = audioVideoConfiguration.audioMode
        )
        videoClientController.start()
    }

    override fun stop() {
        audioClientController.stop()
        videoClientController.stopAndDestroy()
    }

    override fun startLocalVideo() {
        videoClientController.startLocalVideo()
    }

    override fun startLocalVideo(source: VideoSource) {
        videoClientController.startLocalVideo(source)
    }

    override fun stopLocalVideo() {
        videoClientController.stopLocalVideo()
    }

    override fun startRemoteVideo() {
        videoClientController.startRemoteVideo()
    }

    override fun stopRemoteVideo() {
        videoClientController.stopRemoteVideo()
    }

    override fun addAudioVideoObserver(observer: AudioVideoObserver) {
        audioClientObserver.subscribeToAudioClientStateChange(observer)
        videoClientObserver.subscribeToVideoClientStateChange(observer)
    }

    override fun removeAudioVideoObserver(observer: AudioVideoObserver) {
        audioClientObserver.unsubscribeFromAudioClientStateChange(observer)
        videoClientObserver.unsubscribeFromVideoClientStateChange(observer)
    }

    override fun addMetricsObserver(observer: MetricsObserver) {
        clientMetricsCollector.subscribeToMetrics(observer)
    }

    override fun removeMetricsObserver(observer: MetricsObserver) {
        clientMetricsCollector.unsubscribeFromMetrics(observer)
    }

    override fun updateVideoSourceSubscriptions(
        addedOrUpdated: Map<RemoteVideoSource, VideoSubscriptionConfiguration>,
        removed: Array<RemoteVideoSource>
    ) {
        videoClientController.updateVideoSourceSubscriptions(addedOrUpdated, removed)
    }

    override fun promoteToPrimaryMeeting(
        credentials: MeetingSessionCredentials,
        observer: PrimaryMeetingPromotionObserver
    ) {
        // We create a pseudo-'Wait Group' to wait for both audio and video to complete,
        // and then merge their statuses
        val audioVideoPromotionsToComplete = AtomicInteger()
        audioVideoPromotionsToComplete.addAndGet(2)

        var audioClientStatus = MeetingSessionStatus(MeetingSessionStatusCode.OK)
        var videoClientStatus = MeetingSessionStatus(MeetingSessionStatusCode.OK)
        val mergeAndReturnPromotionStatuses = fun (
            firstStatus: MeetingSessionStatus,
            secondStatus: MeetingSessionStatus
        ) {
            // We intentionally don't differentiate status sources here
            // as the preferences towards audio vs. video really doesn't matter
            val mergedStatus = when {
                firstStatus.statusCode != MeetingSessionStatusCode.OK -> firstStatus
                secondStatus.statusCode != MeetingSessionStatusCode.OK -> secondStatus
                else -> firstStatus
            }

            CoroutineScope(Dispatchers.Main).launch {
                primaryMeetingPromotionObserver?.onPrimaryMeetingPromotion(mergedStatus)
            }
        }

        primaryMeetingPromotionObserver = observer
        // In these observers we try demoting the other client. Note that the individual controllers
        // do not follow the exact same pattern of calling back on observer (with `MeetingSessionStatusCode.OK` in
        // the case of explicit demotion request so we don't need to worry about any infinite loops
        val audioPrimaryMeetingPromotionObserverAdapter = object : PrimaryMeetingPromotionObserver {
            override fun onPrimaryMeetingPromotion(status: MeetingSessionStatus) {
                audioClientStatus = status
                if (audioVideoPromotionsToComplete.decrementAndGet() == 0) {
                    mergeAndReturnPromotionStatuses(audioClientStatus, videoClientStatus)
                }
            }

            override fun onPrimaryMeetingDemotion(status: MeetingSessionStatus) {
                videoClientController.demoteFromPrimaryMeeting()
                CoroutineScope(Dispatchers.Main).launch {
                    primaryMeetingPromotionObserver?.onPrimaryMeetingDemotion(status)
                }
            }
        }
        val videoPrimaryMeetingPromotionObserverAdapter = object : PrimaryMeetingPromotionObserver {
            override fun onPrimaryMeetingPromotion(status: MeetingSessionStatus) {
                videoClientStatus = status
                if (audioVideoPromotionsToComplete.decrementAndGet() == 0) {
                    mergeAndReturnPromotionStatuses(audioClientStatus, videoClientStatus)
                }
            }

            override fun onPrimaryMeetingDemotion(status: MeetingSessionStatus) {
                audioClientController.demoteFromPrimaryMeeting()
                CoroutineScope(Dispatchers.Main).launch {
                    primaryMeetingPromotionObserver?.onPrimaryMeetingDemotion(status)
                }
            }
        }

        audioClientController.promoteToPrimaryMeeting(credentials, audioPrimaryMeetingPromotionObserverAdapter)
        videoClientController.promoteToPrimaryMeeting(credentials, videoPrimaryMeetingPromotionObserverAdapter)

        // Setup timeout in case those callbacks are never hit for extremely spurious network or server issues
        Timer().schedule(timerTask {
            // Setting to -1 will invalidate any late callbacks
            if (audioVideoPromotionsToComplete.getAndSet(-1) != 0) {
                // Try demoting to clean up separate state but remove existing cached observer to avoid repeated callbacks
                primaryMeetingPromotionObserver = null
                videoClientController.demoteFromPrimaryMeeting()
                audioClientController.demoteFromPrimaryMeeting()
                // Using observer from scope
                CoroutineScope(Dispatchers.Main).launch {
                    observer.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioInternalServerError))
                }
            }
        }, PRIMARY_MEETING_PROMOTION_TIMEOUT)
    }

    override fun demoteFromPrimaryMeeting() {
        audioClientController.demoteFromPrimaryMeeting()
        videoClientController.demoteFromPrimaryMeeting()
        CoroutineScope(Dispatchers.Main).launch {
            primaryMeetingPromotionObserver?.onPrimaryMeetingDemotion(MeetingSessionStatus(MeetingSessionStatusCode.OK))
        }
    }
}
