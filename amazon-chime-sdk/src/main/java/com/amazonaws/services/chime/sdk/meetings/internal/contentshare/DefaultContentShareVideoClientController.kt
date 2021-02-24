/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatusCode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.utils.AppInfoUtil
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoSourceAdapter
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClientConfig
import com.xodee.client.video.VideoClientConfigBuilder

class DefaultContentShareVideoClientController(
    private val context: Context,
    private val logger: Logger,
    private val contentShareVideoClientObserver: ContentShareVideoClientObserver,
    private val configuration: MeetingSessionConfiguration,
    private val videoClientFactory: VideoClientFactory,
    private val eglCoreFactory: EglCoreFactory
) : ContentShareVideoClientController {
    private val observers = mutableSetOf<ContentShareObserver>()
    private val videoSourceAdapter = VideoSourceAdapter()
    private var videoClient: VideoClient? = null
    private var eglCore: EglCore? = null
    private var isSharing = false

    private val TAG = "DefaultContentShareVideoClientController"

    private val VIDEO_CLIENT_FLAG_ENABLE_SEND_SIDE_BWE = 1 shl 5
    private val VIDEO_CLIENT_FLAG_ENABLE_USE_HW_DECODE_AND_RENDER = 1 shl 6
    private val VIDEO_CLIENT_FLAG_ENABLE_TWO_SIMULCAST_STREAMS = 1 shl 12
    private val VIDEO_CLIENT_FLAG_DISABLE_SIMULCAST_P2P = 1 shl 14
    private val VIDEO_CLIENT_FLAG_DISABLE_CAPTURER = 1 shl 20
    private val VIDEO_CLIENT_FLAG_IS_CONTENT = 1 shl 23
    private val VIDEO_CLIENT_FLAG_ENABLE_INBAND_TURN_CREDS = 1 shl 26

    override fun startVideoShare(videoSource: VideoSource) {
        // Start the given content share source
        if (eglCore == null) {
            logger.debug(TAG, "Creating EGL core")
            eglCore = eglCoreFactory.createEglCore()
        }

        // Start video client only when not currently sharing
        if (!isSharing) {
            if (videoClient == null) {
                initializeVideoClient()
            }

            if (!startVideoClient()) {
                ObserverUtils.notifyObserverOnMainThread(observers) {
                    it.onContentShareStopped(
                        ContentShareStatus(
                            ContentShareStatusCode.VideoServiceFailed
                        )
                    )
                }
                return
            }
        }

        logger.debug(TAG, "Setting external video source to content share source")
        videoSourceAdapter.source = videoSource
        videoClient?.setExternalVideoSource(videoSourceAdapter, eglCore?.eglContext)
        logger.debug(TAG, "Setting sending to true")
        videoClient?.setSending(true)
        isSharing = true
    }

    private fun initializeVideoClient() {
        logger.info(TAG, "Initializing content share video client")
        AppInfoUtil.initializeVideoClientAppDetailedInfo(context)
        // Thread safe operation, can be called multiple times
        VideoClient.javaInitializeGlobals(context)
        videoClient = videoClientFactory.getVideoClient(contentShareVideoClientObserver)
        // Content share is send only
        videoClient?.setReceiving(false)
    }

    private fun startVideoClient(): Boolean {
        logger.info(TAG, "Starting content share video client for content share")
        var flag = 0
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_SEND_SIDE_BWE
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_USE_HW_DECODE_AND_RENDER
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_TWO_SIMULCAST_STREAMS
        flag = flag or VIDEO_CLIENT_FLAG_DISABLE_SIMULCAST_P2P
        flag = flag or VIDEO_CLIENT_FLAG_DISABLE_CAPTURER
        flag = flag or VIDEO_CLIENT_FLAG_IS_CONTENT
        flag = flag or VIDEO_CLIENT_FLAG_ENABLE_INBAND_TURN_CREDS

        val videoClientConfig: VideoClientConfig = VideoClientConfigBuilder()
            .setMeetingId(configuration.meetingId)
            .setToken(configuration.credentials.joinToken)
            .setAudioHostUrl(configuration.urls.audioHostURL)
            .setFlags(flag)
            .setSharedEglContext(eglCore?.eglContext)
            .setSignalingUrl(configuration.urls.signalingURL)
            .createVideoClientConfig()
        val result = videoClient?.start(videoClientConfig) ?: false
        logger.info(TAG, "Content share video client start result: $result")
        return result
    }

    override fun stopVideoShare() {
        logger.info(TAG, "Stopping content share video client")
        videoClient?.javaStopService()
        videoClient?.destroy()
        videoClient = null

        isSharing = false
        eglCore?.release()
        eglCore = null
    }

    override fun subscribeToVideoClientStateChange(observer: ContentShareObserver) {
        observers.add(observer)
        contentShareVideoClientObserver.subscribeToVideoClientStateChange(observer)
    }

    override fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver) {
        observers.remove(observer)
        contentShareVideoClientObserver.unsubscribeFromVideoClientStateChange(observer)
    }
}
