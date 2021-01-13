/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DNSServerUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.TURNRequestUtils
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessage
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.session.URLRewriter
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.DataMessage as mediaDataMessage
import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClient.VIDEO_CLIENT_NO_PAUSE
import com.xodee.client.video.VideoClient.VIDEO_CLIENT_REMOTE_PAUSED_BY_LOCAL_BAD_NETWORK
import com.xodee.client.video.VideoClient.VIDEO_CLIENT_REMOTE_PAUSED_BY_USER
import java.security.InvalidParameterException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultVideoClientObserver(
    private val context: Context,
    private val logger: Logger,
    private val turnRequestParams: TURNRequestParams,
    private val clientMetricsCollector: ClientMetricsCollector,
    private val videoClientStateController: VideoClientStateController,
    private val urlRewriter: URLRewriter
) : VideoClientObserver {
    private val TAG = "DefaultVideoClientObserver"

    private var videoClientStateObservers = mutableSetOf<AudioVideoObserver>()
    private var videoClientTileObservers = mutableSetOf<VideoTileController>()
    private var dataMessageObserversByTopic = mutableMapOf<String, MutableSet<DataMessageObserver>>()

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun isConnecting(client: VideoClient?) {
        logger.info(TAG, "isConnecting")
        forEachVideoClientStateObserver { observer -> observer.onVideoSessionStartedConnecting() }
    }

    override fun didConnect(client: VideoClient?, controlStatus: Int) {
        logger.info(TAG, "didConnect with controlStatus: $controlStatus")

        videoClientStateController.updateState(VideoClientState.STARTED)
        if (controlStatus == VideoClient.VIDEO_CLIENT_STATUS_CALL_AT_CAPACITY_VIEW_ONLY) {
            forEachVideoClientStateObserver {
                it.onVideoSessionStarted(
                    MeetingSessionStatus(
                        MeetingSessionStatusCode.VideoAtCapacityViewOnly
                    )
                )
            }
        } else {
            forEachVideoClientStateObserver { observer ->
                observer.onVideoSessionStarted(
                    MeetingSessionStatus(MeetingSessionStatusCode.OK)
                )
            }
        }
    }

    override fun didFail(client: VideoClient?, status: Int, controlStatus: Int) {
        logger.info(TAG, "didFail with controlStatus: $controlStatus")

        forEachVideoClientStateObserver { observer ->
            observer.onVideoSessionStopped(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.VideoServiceFailed
                )
            )
        }
    }

    override fun didStop(client: VideoClient?) {
        logger.info(TAG, "didStop")

        videoClientStateController.updateState(VideoClientState.STOPPED)
        forEachVideoClientStateObserver { observer ->
            observer.onVideoSessionStopped(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.OK
                )
            )
        }
    }

    override fun cameraSendIsAvailable(client: VideoClient?, available: Boolean) {
        logger.debug(TAG, "cameraSendIsAvailable: $available")
    }

    override fun pauseRemoteVideo(client: VideoClient?, display_id: Int, pause: Boolean) {
        logger.info(TAG, "pauseRemoteVideo")
    }

    override fun onCameraChanged() {
        logger.info(TAG, "onCameraChanged")
    }

    override fun didReceiveFrame(
        client: VideoClient?,
        frame: Any?,
        profileId: String?,
        displayId: Int,
        pauseType: Int,
        videoId: Int
    ) {
        val pauseState: VideoPauseState = when (pauseType) {
            VIDEO_CLIENT_NO_PAUSE -> VideoPauseState.Unpaused
            VIDEO_CLIENT_REMOTE_PAUSED_BY_USER -> VideoPauseState.PausedByUserRequest
            VIDEO_CLIENT_REMOTE_PAUSED_BY_LOCAL_BAD_NETWORK -> VideoPauseState.PausedForPoorConnection
            else -> VideoPauseState.Unpaused
        }

        val sdkFrame = (frame as? com.xodee.client.video.VideoFrame)?.let {
            val bufferAdapter: VideoFrameBuffer = when (frame.buffer) {
                is com.xodee.client.video.VideoFrameTextureBuffer -> {
                    val buffer = frame.buffer as com.xodee.client.video.VideoFrameTextureBuffer
                    val type = when (buffer.type) {
                        com.xodee.client.video.VideoFrameTextureBuffer.Type.OES -> VideoFrameTextureBuffer.Type.TEXTURE_OES
                        com.xodee.client.video.VideoFrameTextureBuffer.Type.RGB -> VideoFrameTextureBuffer.Type.TEXTURE_2D
                        else -> throw InvalidParameterException("Unsupported texture buffer type")
                    }
                    // Retain this buffer and create a new buffer with same internals that releases the original buffer when released itself
                    buffer.retain()
                    VideoFrameTextureBuffer(buffer.width, buffer.height, buffer.textureId, buffer.transformMatrix, type, Runnable { buffer.release() })
                }
                is com.xodee.client.video.VideoFrameI420Buffer -> {
                    val buffer = frame.buffer as com.xodee.client.video.VideoFrameI420Buffer
                    // Retain this buffer and create a new buffer with same internals that releases the original buffer when released itself
                    buffer.retain()
                    VideoFrameI420Buffer(buffer.width, buffer.height, buffer.dataY, buffer.dataU, buffer.dataV, buffer.strideY, buffer.strideU, buffer.strideV, Runnable { buffer.release() })
                }
                else -> throw InvalidParameterException("Video frame must have non null I420 or texture buffer")
            }
            VideoFrame(frame.timestampNs, bufferAdapter, VideoRotation.from(frame.rotation) ?: VideoRotation.Rotation0)
        }

        notifyVideoTileObserver { observer ->
            observer.onReceiveFrame(
                sdkFrame,
                videoId,
                profileId,
                pauseState
            )
        }

        // Frames passed up have additional ref count added so we need to release when finished
        // to not leak the frame/buffer
        sdkFrame?.release()
    }

    override fun onMetrics(metrics: IntArray?, values: DoubleArray?) {
        if (metrics == null || values == null) return

        val metricMap = mutableMapOf<Int, Double>()
        (metrics.indices).map { i -> metricMap[metrics[i]] = values[i] }
        clientMetricsCollector.processVideoClientMetrics(metricMap)
    }

    override fun onLogMessage(logLevel: Int, message: String?) {
        if (message == null) return
        // Only print error and fatal as the Media team's request to avoid noise for application
        // that has log level set to INFO or higher. All other cases, print as verbose
        if (logLevel == AudioClient.L_ERROR || logLevel == AudioClient.L_FATAL) {
            logger.error(TAG, message)
        } else {
            logger.verbose(TAG, message)
        }
    }

    override fun requestTurnCreds(client: VideoClient?) {
        logger.info(TAG, "requestTurnCreds")
        uiScope.launch {
            val turnResponse: TURNCredentials? = TURNRequestUtils.doTurnRequest(turnRequestParams, logger)
            with(turnResponse) {
                val isActive = client?.isActive ?: false
                if (this != null && isActive) {
                    val newUris = uris.map { url -> url?.let {
                        urlRewriter(it)
                    } }.toTypedArray()
                    client?.updateTurnCredentials(
                        username,
                        password,
                        ttl,
                        newUris,
                        turnRequestParams.signalingUrl,
                        VideoClient.VideoClientTurnStatus.VIDEO_CLIENT_TURN_FEATURE_ON
                    )
                } else {
                    client?.updateTurnCredentials(
                        null,
                        null,
                        null,
                        null,
                        null,
                        VideoClient.VideoClientTurnStatus.VIDEO_CLIENT_TURN_STATUS_CCP_FAILURE
                    )
                }
            }
        }
    }

    override fun getAvailableDnsServers(): Array<String> {
        return DNSServerUtils.getAvailableDnsServers(context, logger)
    }

    override fun onDataMessageReceived(dataMessages: Array<mediaDataMessage>?) {
        if (dataMessages == null) return

        logger.debug(TAG, "onDataMessageReceived with size: ${dataMessages.size}")
        for (dataMessage in dataMessages) {
            if (!dataMessageObserversByTopic.containsKey(dataMessage.topic)) continue
            val sdkDataMessage = DataMessage(
                dataMessage.timestampMs,
                dataMessage.topic,
                dataMessage.data,
                dataMessage.senderAttendeeId,
                dataMessage.senderExternalUserId,
                dataMessage.throttled
            )
            dataMessageObserversByTopic[dataMessage.topic]?.let { observers ->
                ObserverUtils.notifyObserverOnMainThread(observers) {
                    it.onDataMessageReceived(sdkDataMessage)
                }
            }
        }
    }

    override fun onTurnURIsReceived(uris: List<String>): List<String> {
        return uris.map(urlRewriter)
    }

    override fun subscribeToVideoClientStateChange(observer: AudioVideoObserver) {
        videoClientStateObservers.add(observer)
    }

    override fun unsubscribeFromVideoClientStateChange(observer: AudioVideoObserver) {
        videoClientStateObservers.remove(observer)
    }

    override fun subscribeToVideoTileChange(observer: VideoTileController) {
        videoClientTileObservers.add(observer)
    }

    override fun unsubscribeFromVideoTileChange(observer: VideoTileController) {
        videoClientTileObservers.remove(observer)
    }

    override fun subscribeToReceiveDataMessage(topic: String, observer: DataMessageObserver) {
        dataMessageObserversByTopic.getOrPut(topic, { mutableSetOf() }).add(observer)
    }

    override fun unsubscribeFromReceiveDataMessage(topic: String) {
        dataMessageObserversByTopic.remove(topic)
    }

    override fun notifyVideoTileObserver(observerFunction: (observer: VideoTileController) -> Unit) {
        for (observer in videoClientTileObservers) {
            observerFunction(observer)
        }
    }

    private fun forEachVideoClientStateObserver(observerFunction: (observer: AudioVideoObserver) -> Unit) {
        ObserverUtils.notifyObserverOnMainThread(videoClientStateObservers, observerFunction)
    }
}
