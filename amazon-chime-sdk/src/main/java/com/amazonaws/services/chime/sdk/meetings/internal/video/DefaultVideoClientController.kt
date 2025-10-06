/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.PrimaryMeetingPromotionObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.LocalVideoConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultCameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.internal.utils.AppInfoUtil
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.google.gson.Gson
import com.xodee.client.video.RemoteVideoSourceInternal
import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClientConfig
import com.xodee.client.video.VideoClientConfigBuilder
import com.xodee.client.video.VideoPriorityInternal
import com.xodee.client.video.VideoResolutionInternal
import com.xodee.client.video.VideoSubscriptionConfigurationInternal
import java.security.InvalidParameterException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultVideoClientController(
    private val context: Context,
    private val logger: Logger,
    private val videoClientStateController: VideoClientStateController,
    private val videoClientObserver: VideoClientObserver,
    private val configuration: MeetingSessionConfiguration,
    private val videoClientFactory: VideoClientFactory,
    private val eglCoreFactory: EglCoreFactory,
    private val eventAnalyticsController: EventAnalyticsController
) : VideoClientController,
    VideoClientLifecycleHandler {

    private val DATA_MAX_SIZE = 2048
    private val TOPIC_REGEX = "^[a-zA-Z0-9_-]{1,36}$".toRegex()
    private val TAG = "DefaultVideoClientController"

    private val VIDEO_CLIENT_FLAG_ENABLE_SEND_SIDE_BWE = 1 shl 5
    private val VIDEO_CLIENT_FLAG_ENABLE_USE_HW_DECODE_AND_RENDER = 1 shl 6
    private val VIDEO_CLIENT_FLAG_ENABLE_TWO_SIMULCAST_STREAMS = 1 shl 12
    private val VIDEO_CLIENT_FLAG_DISABLE_SIMULCAST_P2P = 1 shl 14
    private val VIDEO_CLIENT_FLAG_DISABLE_CAPTURER = 1 shl 20
    private val VIDEO_CLIENT_FLAG_EXCLUDE_SELF_CONTENT_IN_INDEX = 1 shl 24
    private val VIDEO_CLIENT_FLAG_ENABLE_INBAND_TURN_CREDS = 1 shl 26

    private val gson = Gson()

    private val cameraCaptureSource: DefaultCameraCaptureSource
    private var videoSourceAdapter = VideoSourceAdapter()
    private var isUsingInternalCaptureSource = false
    private val videoClientStopMutex = Mutex()
    init {
        videoClientStateController.bindLifecycleHandler(this)

        val surfaceTextureCaptureSourceFactory =
            DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
        cameraCaptureSource =
            DefaultCameraCaptureSource(
                context,
                logger,
                surfaceTextureCaptureSourceFactory,
                eventAnalyticsController
            )
    }

    private var videoClient: VideoClient? = null

    private var eglCore: EglCore? = null

    override fun setMaxBitRateKbps(maxBitRate: Int) {
        videoClient?.setMaxBitRateKbps(maxBitRate)
    }

    override fun start() {
        if (eglCore == null) {
            eglCore = eglCoreFactory.createEglCore()
        }

        videoClientStateController.start()
    }

    override fun stopAndDestroy() {
        GlobalScope.launch {
            // So it doesn't call it spuriously
            videoClientObserver.primaryMeetingPromotionObserver = null

            videoClientStopMutex.withLock {
                // Race conditions in video client stop functions may lead to crashes at pointer deallocation
                // if multiple such threads are called at the same time.
                videoClientStateController.stop()

                // We put release eglCore under mutex as a race condition may decrease its reference counter
                // below 0 and cause crashes if multiple such calls happen together.
                eglCore?.release()
                eglCore = null
            }
        }
    }

    override fun startLocalVideo() {
        val config = LocalVideoConfiguration()
        startLocalVideo(config)
    }

    override fun startLocalVideo(config: LocalVideoConfiguration) {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return
        videoSourceAdapter.source = cameraCaptureSource
        logger.info(TAG, "Setting external video source in media client to internal camera source")
        videoClient?.setExternalVideoSource(videoSourceAdapter, eglCore?.eglContext)
        videoClient?.setSending(true)

        cameraCaptureSource.start()
        isUsingInternalCaptureSource = true
        config.safeMaxBitRateKbps.let {
            if (it > 0) videoClient?.setMaxBitRateKbps(it)
        }
    }

    override fun startLocalVideo(source: VideoSource) {
        val config = LocalVideoConfiguration()
        startLocalVideo(source, config)
    }

    override fun startLocalVideo(source: VideoSource, config: LocalVideoConfiguration) {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return
        stopInternalCaptureSourceIfRunning()

        videoSourceAdapter.source = source
        logger.info(TAG, "Setting external video source in media client to custom source")
        videoClient?.setExternalVideoSource(videoSourceAdapter, eglCore?.eglContext)
        videoClient?.setSending(true)
        config.safeMaxBitRateKbps.let {
            if (it > 0) videoClient?.setMaxBitRateKbps(it)
        }
    }

    override fun stopLocalVideo() {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return

        logger.info(TAG, "Stopping local video")
        videoClient?.setSending(false)
        stopInternalCaptureSourceIfRunning()
    }

    override fun startRemoteVideo() {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return

        logger.info(TAG, "Starting remote video")
        videoClient?.setReceiving(true)
    }

    override fun stopRemoteVideo() {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return

        logger.info(TAG, "Stopping remote video")
        videoClient?.setReceiving(false)
    }

    override fun getActiveCamera(): MediaDevice? {
        if (isUsingInternalCaptureSource) {
            return cameraCaptureSource.device
        }
        return null
    }

    override fun switchCamera() {
        cameraCaptureSource.switchCamera()
    }

    override fun setRemotePaused(isPaused: Boolean, videoId: Int) {
        if (!videoClientStateController.canAct(VideoClientState.STARTED)) return

        logger.info(TAG, "Set pause for videoId: $videoId, isPaused: $isPaused")
        videoClient?.setRemotePause(videoId, isPaused)
    }

    override fun getConfiguration(): MeetingSessionConfiguration {
        return configuration
    }

    override fun sendDataMessage(topic: String, data: Any, lifetimeMs: Int) {
        if (!videoClientStateController.canAct(VideoClientState.STARTED)) return

        val byteArray = data as? ByteArray
            ?: if (data is String) {
                data.toByteArray()
            } else {
                gson.toJson(data).toByteArray()
            }

        if (!TOPIC_REGEX.matches(topic)) {
            throw InvalidParameterException("Invalid topic")
        }
        if (byteArray.size > DATA_MAX_SIZE) {
            throw InvalidParameterException("Data size has to be less than or equal to 2048 bytes")
        }
        if (lifetimeMs < 0) {
            throw InvalidParameterException("The life time of the message has to be non negative")
        }

        videoClient?.sendDataMessage(topic, byteArray, lifetimeMs)
    }

    override fun updateVideoSourceSubscriptions(
        addedOrUpdated: Map<RemoteVideoSource, VideoSubscriptionConfiguration>,
        removed: Array<RemoteVideoSource>
    ) {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return
        for (key in addedOrUpdated.keys) {
            logger.info(
                TAG,
                "Adding/updating video sources subscription: ${key.attendeeId} with priority: ${addedOrUpdated[key]?.priority} " +
                        "targetResolution: ${addedOrUpdated[key]?.targetResolution}"
            )
        }
        for (key in removed) {
            logger.info(
                TAG,
                "Removing video source subscription: ${key.attendeeId}"
            )
        }

        val addedOrUpdatedInternal: MutableMap<RemoteVideoSourceInternal, VideoSubscriptionConfigurationInternal> =
            mutableMapOf()
        for ((source, config) in addedOrUpdated) {
            addedOrUpdatedInternal[RemoteVideoSourceInternal(source.attendeeId)] =
                VideoSubscriptionConfigurationInternal(
                    VideoPriorityInternal(config.priority.value),
                    VideoResolutionInternal(config.targetResolution.width, config.targetResolution.height)
                )
        }
        val removedInternal = removed.map { source -> RemoteVideoSourceInternal(source.attendeeId) }

        videoClient?.updateVideoSourceSubscriptions(addedOrUpdatedInternal, removedInternal)
    }

    override fun promoteToPrimaryMeeting(
        credentials: MeetingSessionCredentials,
        observer: PrimaryMeetingPromotionObserver
    ) {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) {
            observer.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioServiceUnavailable))
            return
        }
        videoClientObserver.primaryMeetingPromotionObserver = observer
        videoClient?.promoteToPrimaryMeeting(credentials.attendeeId, credentials.externalUserId, credentials.joinToken)
    }

    override fun demoteFromPrimaryMeeting() {
        if (videoClientStateController.canAct(VideoClientState.INITIALIZED)) videoClient?.demoteFromPrimaryMeeting()
    }

    override fun initializeVideoClient() {
        logger.info(TAG, "Initializing video client")
        AppInfoUtil.initializeVideoClientAppDetailedInfo(context)
        VideoClient.javaInitializeGlobals(context)
        videoClient = videoClientFactory.getVideoClient(videoClientObserver)
    }

    override fun startVideoClient() {
        logger.info(TAG, "Starting video client")
        videoClient?.setReceiving(false)
        var flag = 0
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_SEND_SIDE_BWE
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_USE_HW_DECODE_AND_RENDER
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_TWO_SIMULCAST_STREAMS
        flag = flag or VIDEO_CLIENT_FLAG_DISABLE_SIMULCAST_P2P
        flag = flag or VIDEO_CLIENT_FLAG_DISABLE_CAPTURER
        flag = flag or VIDEO_CLIENT_FLAG_EXCLUDE_SELF_CONTENT_IN_INDEX
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_INBAND_TURN_CREDS
        val videoClientConfig: VideoClientConfig = VideoClientConfigBuilder()
            .setMeetingId(configuration.meetingId)
            .setToken(configuration.credentials.joinToken)
            .setFlags(flag)
            .setSharedEglContext(eglCore?.eglContext)
            .setSignalingUrl(configuration.urls.signalingURL)
            .createVideoClientConfig()
        videoClient?.start(videoClientConfig)

        videoSourceAdapter.let { videoClient?.setExternalVideoSource(it, eglCore?.eglContext) }
    }

    override fun stopVideoClient() {
        logger.info(TAG, "Stopping video client")
        videoClient?.javaStopService()
        // SDK owns the lifecycle of the internal capture source
        stopInternalCaptureSourceIfRunning()
    }

    override fun destroyVideoClient() {
        logger.info(TAG, "Destroying video client")
        videoClient?.destroy()
        videoClient = null
        VideoClient.finalizeGlobals()
    }

    private fun stopInternalCaptureSourceIfRunning() {
        if (isUsingInternalCaptureSource) {
            cameraCaptureSource.stop()
            isUsingInternalCaptureSource = false
        }
    }
}
