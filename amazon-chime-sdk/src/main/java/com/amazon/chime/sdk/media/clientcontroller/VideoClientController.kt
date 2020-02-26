/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileController
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.session.MeetingSessionStatusCode
import com.amazon.chime.sdk.session.MeetingSessionTURNCredentials
import com.amazon.chime.sdk.utils.logger.Logger
import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClientCapturer
import com.xodee.client.video.VideoClientDelegate
import com.xodee.client.video.VideoDevice
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VideoClientController constructor(
    val context: Context,
    val logger: Logger
) :
    VideoClientDelegate {

    private val TAG = "VideoClientController"
    private val TOKEN_HEADER = "X-Chime-Auth-Token"
    private val CONTENT_TYPE_HEADER = "Content-Type"
    private val CONTENT_TYPE = "application/json"
    private val MEETING_ID_KEY = "meetingId"
    private val TOKEN_KEY = "_aws_wt_session"
    private val permissions = arrayOf(
        Manifest.permission.CAMERA
    )
    private var videoClient: VideoClient? = null
    private var videoClientStateObservers = mutableSetOf<AudioVideoObserver>()
    private var videoClientTileObservers = mutableSetOf<VideoTileController>()
    private var videoClientState: VideoClientState = VideoClientState.UNINITIALIZED
    private var isSelfVideoSending: Boolean = false
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private var signalingUrl: String? = null
    private var turnControlUrl: String? = null
    private var meetingId: String? = null
    private var joinToken: String? = null

    private enum class VideoClientState(val value: Int) {
        UNINITIALIZED(-1),
        INITIALIZED(0),
        STARTED(1),
        STOPPED(2),
    }

    internal fun start(
        controlUrl: String,
        signalingUrl: String,
        meetingId: String,
        token: String,
        sending: Boolean
    ) {
        val hasPermission: Boolean = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (sending && !hasPermission) {
            throw SecurityException(
                "Missing necessary permissions for WebRTC: ${permissions.joinToString(
                    separator = ", ",
                    prefix = "",
                    postfix = ""
                )}"
            )
        }
        this.signalingUrl = signalingUrl
        this.turnControlUrl = controlUrl
        this.meetingId = meetingId
        this.joinToken = token
        if (videoClientState == VideoClientState.UNINITIALIZED) {
            initialize()
        }
        if (videoClientState == VideoClientState.STARTED) {
            logger.info(TAG, "VideoClientState is already started ")
            return
        }
        if (videoClientState == VideoClientState.INITIALIZED || videoClientState == VideoClientState.STOPPED) {
            logger.info(TAG, "Starting VideoClient with sending = $sending")
            enableSelfVideo(sending)
            videoClient?.startServiceV2(
                "",
                "",
                meetingId,
                token,
                sending,
                0,
                0
            )
            videoClientState = VideoClientState.STARTED
        }
    }

    private fun initialize() {
        if (videoClientState == VideoClientState.UNINITIALIZED) {
            logger.info(TAG, "VideoClientState is UNINITIALIZED. Therefore creating a new one")
            // TODO check with video team if this function is required
            // initializeAppDetailedInfo()
            VideoClient.initializeGlobals(context)
            VideoClientCapturer.getInstance(context)
            videoClient = VideoClient(this)
            forEachVideoTileObserver { observer -> observer.initialize() }
            videoClientState = VideoClientState.INITIALIZED
        }
    }

    private fun stop() {
        if (videoClientState == VideoClientState.STARTED) {
            logger.info(
                TAG,
                "Stopping Video Client"
            )
            videoClient?.stopService()
            isSelfVideoSending = false
            videoClientState = VideoClientState.STOPPED
        }
    }

    internal fun stopAndDestroy() {
        if (VideoClientState.UNINITIALIZED == videoClientState) return
        if (videoClientState == VideoClientState.STARTED) {
            stop()
        }
        logger.info(TAG, "VideoClient is being destroyed")
        videoClient?.clearCurrentDevice()
        forEachVideoTileObserver { observer -> observer.destroy() }
        videoClient?.destroy()
        videoClient = null
        VideoClient.finalizeGlobals()
        videoClientState = VideoClientState.UNINITIALIZED
    }

    internal fun enableSelfVideo(isEnable: Boolean) {
        logger.info(TAG, "Enable Self Video with isEnable = $isEnable")
        if (videoClientState == VideoClientState.UNINITIALIZED) {
            logger.info(TAG, "Video Client is not initialized so returning without doing anything")
            return
        }
        isSelfVideoSending = isEnable
        if (isSelfVideoSending) {
            val hasPermission: Boolean = permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (!hasPermission) {
                throw SecurityException(
                    "Missing necessary permissions for WebRTC: ${permissions.joinToString(
                        separator = ", ",
                        prefix = "",
                        postfix = ""
                    )}"
                )
            }
            val currentDevice = getActiveCamera()
            if (currentDevice == null) {
                setFrontCameraAsCurrentDevice()
            }
        }
        videoClient?.setSending(isSelfVideoSending)
    }

    internal fun getActiveCamera(): VideoDevice? {
        return videoClient?.currentDevice
    }

    internal fun switchCamera() {
        if (videoClientState >= VideoClientState.INITIALIZED) {
            logger.info(TAG, "Switching Camera")
            val nextDevice = videoClient?.devices
                ?.filter { it.identifier != (getActiveCamera()?.identifier) }
                ?.elementAtOrNull(0)
            if (nextDevice != null) {
                videoClient?.currentDevice = nextDevice
            }
        }
    }

    private fun setFrontCameraAsCurrentDevice() {
        if (videoClientState >= VideoClientState.INITIALIZED) {
            logger.info(TAG, "Setting setFrontCameraAsCurrentDevice")
            val currentDevice = getActiveCamera()
            if (currentDevice == null || !currentDevice.isFrontFacing) {
                val frontDevice =
                    videoClient?.devices?.filter { it.isFrontFacing }?.elementAtOrNull(0)
                if (frontDevice != null) {
                    videoClient?.currentDevice = frontDevice
                }
            }
        }
    }

    private fun forEachVideoClientStateObserver(observerFunction: (observer: AudioVideoObserver) -> Unit) {
        for (observer in videoClientStateObservers) {
            observerFunction(observer)
        }
    }

    private fun forEachVideoTileObserver(observerFunction: (observer: VideoTileController) -> Unit) {
        for (observer in videoClientTileObservers) {
            observerFunction(observer)
        }
    }

    fun subscribeToVideoClientStateChange(observer: AudioVideoObserver) {
        videoClientStateObservers.add(observer)
    }

    fun unsubscribeFromVideoClientStateChange(observer: AudioVideoObserver) {
        videoClientStateObservers.remove(observer)
    }

    internal fun subscribeToVideoTile(observer: VideoTileController) {
        videoClientTileObservers.add(observer)
    }

    internal fun unsubscribeFromVideoTile(observer: VideoTileController) {
        videoClientTileObservers.remove(observer)
    }

    override fun isConnecting(client: VideoClient?) {
        logger.info(TAG, "isConnecting")
        forEachVideoClientStateObserver { observer -> observer.onVideoClientConnecting() }
    }

    override fun didConnect(client: VideoClient?, controlStatus: Int) {
        logger.info(TAG, "didConnect")
        forEachVideoClientStateObserver { observer -> observer.onVideoClientStart() }
    }

    override fun didFail(client: VideoClient?, status: Int, controlStatus: Int) {
        logger.info(TAG, "didFail with controlStatus = $controlStatus")
        forEachVideoClientStateObserver { observer ->
            observer.onVideoClientStop(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.VideoServiceFailed
                )
            )
        }
    }

    override fun didStop(client: VideoClient?) {
        logger.info(TAG, "didStop")
        videoClientState = VideoClientState.STOPPED
        forEachVideoClientStateObserver { observer ->
            observer.onVideoClientStop(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.OK
                )
            )
        }
    }

    override fun cameraSendIsAvailable(client: VideoClient?, available: Boolean) {
        logger.info(TAG, "cameraSendIsAvailable")
    }

    override fun requestTurnCreds(client: VideoClient?) {
        logger.info(TAG, "requestTurnCreds")
        uiScope.launch {
            val turnResponse: MeetingSessionTURNCredentials? = doTurnRequest()
            with(turnResponse) {
                val isActive = videoClient?.isActive ?: false
                if (this != null && isActive) {
                    videoClient?.updateTurnCredentials(
                        username,
                        password,
                        ttl,
                        uris,
                        signalingUrl,
                        VideoClient.VideoClientTurnStatus.VIDEO_CLIENT_TURN_FEATURE_ON
                    )
                } else {
                    videoClient?.updateTurnCredentials(
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
        forEachVideoTileObserver { observer ->
            observer.onReceiveFrame(
                frame,
                profileId,
                displayId,
                pauseType,
                videoId
            )
        }
    }

    private suspend fun doTurnRequest(): MeetingSessionTURNCredentials? {
        return withContext(ioDispatcher) {
            try {
                val response = StringBuffer()
                logger.info(TAG, "Making TURN Request")
                with(URL(turnControlUrl).openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    addRequestProperty(TOKEN_HEADER, "$TOKEN_KEY=$joinToken")
                    setRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE)
                    val out = BufferedWriter(OutputStreamWriter(outputStream))
                    out.write(JSONObject().put(MEETING_ID_KEY, meetingId).toString())
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
                            responseObject.getJSONArray(MeetingSessionTURNCredentials.TURN_CREDENTIALS_RESULT_URIS)
                        val uris = arrayOfNulls<String>(jsonArray.length())
                        for (i in 0 until jsonArray.length()) {
                            uris[i] = jsonArray.getString(i)
                        }
                        MeetingSessionTURNCredentials(
                            responseObject.getString(MeetingSessionTURNCredentials.TURN_CREDENTIALS_RESULT_USERNAME),
                            responseObject.getString(MeetingSessionTURNCredentials.TURN_CREDENTIALS_RESULT_PASSWORD),
                            responseObject.getString(MeetingSessionTURNCredentials.TURN_CREDENTIALS_RESULT_TTL),
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
}
