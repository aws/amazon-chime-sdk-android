/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
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
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.InvalidParameterException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DefaultVideoClientObserver(
    private val logger: Logger,
    private val turnRequestParams: TURNRequestParams,
    private val clientMetricsCollector: ClientMetricsCollector,
    private val videoClientStateController: VideoClientStateController,
    private val urlRewriter: URLRewriter
) : VideoClientObserver {
    private val TAG = "DefaultVideoClientObserver"
    private val TOKEN_HEADER = "X-Chime-Auth-Token"
    private val SYSPROP_USER_AGENT = "http.agent"
    private val USER_AGENT_HEADER = "User-Agent"
    private val CONTENT_TYPE_HEADER = "Content-Type"
    private val CONTENT_TYPE = "application/json"
    private val MEETING_ID_KEY = "meetingId"
    private val TOKEN_KEY = "_aws_wt_session"

    private var videoClientStateObservers = mutableSetOf<AudioVideoObserver>()
    private var videoClientTileObservers = mutableSetOf<VideoTileController>()
    private var dataMessageObserversByTopic = mutableMapOf<String, MutableSet<DataMessageObserver>>()

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

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
            val turnResponse: TURNCredentials? = doTurnRequest()
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
        return emptyArray()
    }

    private suspend fun doTurnRequest(): TURNCredentials? {
        return withContext(ioDispatcher) {
            try {
                val response = StringBuffer()
                logger.info(TAG, "Making TURN Request")
                with(URL(turnRequestParams.turnControlUrl).openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    addRequestProperty(TOKEN_HEADER, "$TOKEN_KEY=${turnRequestParams.joinToken}")
                    setRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE)
                    val user_agent = System.getProperty(SYSPROP_USER_AGENT)
                    logger.info(TAG, "User Agent while doing TURN request is $user_agent")
                    setRequestProperty(USER_AGENT_HEADER, user_agent)
                    val out = BufferedWriter(OutputStreamWriter(outputStream))
                    out.write(
                        JSONObject().put(
                            MEETING_ID_KEY,
                            turnRequestParams.meetingId
                        ).toString()
                    )
                    out.flush()
                    out.close()
                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }
                    if (responseCode == 200) {
                        logger.info(TAG, "TURN Request Success")
                        var responseObject = JSONObject(response.toString())
                        val jsonArray =
                            responseObject.getJSONArray(TURNCredentials.TURN_CREDENTIALS_RESULT_URIS)
                        val uris = arrayOfNulls<String>(jsonArray.length())
                        for (i in 0 until jsonArray.length()) {
                            uris[i] = jsonArray.getString(i)
                        }
                        TURNCredentials(
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_USERNAME),
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_PASSWORD),
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_TTL),
                            uris
                        )
                    } else {
                        logger.error(
                            TAG,
                            "TURN Request got error with Response code: $responseCode"
                        )
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error(TAG, "Exception while doing TURN Request: $exception")
                null
            }
        }
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
