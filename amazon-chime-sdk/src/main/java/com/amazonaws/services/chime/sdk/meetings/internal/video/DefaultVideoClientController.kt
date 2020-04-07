/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.BuildConfig
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClientCapturer
import com.xodee.client.video.VideoDevice

class DefaultVideoClientController constructor(
    private val context: Context,
    private val logger: Logger,
    private val videoClientStateController: VideoClientStateController,
    private val videoClientObserver: VideoClientObserver
) : VideoClientController,
    VideoClientLifecycleHandler {
    private val TAG = "DefaultVideoClientController"

    private val permissions = arrayOf(
        Manifest.permission.CAMERA
    )

    init {
        videoClientStateController.bindLifecycleHandler(this)
    }

    private var videoClient: VideoClient? = null
    private var meetingId: String? = null
    private var joinToken: String? = null

    override fun start(meetingId: String, joinToken: String) {
        this.meetingId = meetingId
        this.joinToken = joinToken

        videoClientStateController.start()
    }

    override fun stopAndDestroy() {
        videoClientStateController.stop()
    }

    override fun startLocalVideo() {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return

        logger.info(TAG, "Starting local video")
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

        getActiveCamera() ?: setFrontCameraAsCurrentDevice()
        videoClient?.setSending(true)
    }

    override fun stopLocalVideo() {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return

        logger.info(TAG, "Stopping local video")
        videoClient?.setSending(false)
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

    override fun getActiveCamera(): VideoDevice? {
        return videoClient?.currentDevice
    }

    override fun switchCamera() {
        if (!videoClientStateController.canAct(VideoClientState.INITIALIZED)) return

        logger.info(TAG, "Switching camera")
        val nextDevice: VideoDevice? = videoClient?.devices
            ?.filter { it.identifier != (getActiveCamera()?.identifier) }
            ?.elementAtOrNull(0)
        nextDevice?.let { videoClient?.currentDevice = it }
    }

    private fun setFrontCameraAsCurrentDevice() {
        logger.info(TAG, "Setting front camera as current device")
        val currentDevice: VideoDevice? = getActiveCamera()
        if (currentDevice == null || !currentDevice.isFrontFacing) {
            val frontDevice: VideoDevice? =
                videoClient?.devices?.filter { it.isFrontFacing }?.elementAtOrNull(0)
            frontDevice?.let { videoClient?.currentDevice = it }
        }
    }

    override fun setRemotePaused(isPaused: Boolean, videoId: Int) {
        if (!videoClientStateController.canAct(VideoClientState.STARTED)) return

        logger.info(TAG, "Set pause for videoId: $videoId, isPaused: $isPaused")
        videoClient?.setRemotePause(videoId, isPaused)
    }

    override fun initializeVideoClient() {
        logger.info(TAG, "Initializing video client")
        initializeAppDetailedInfo()
        VideoClient.initializeGlobals(context)
        VideoClientCapturer.getInstance(context)
        videoClient = VideoClient(videoClientObserver, videoClientObserver)
        videoClientObserver.notifyVideoTileObserver { observer -> observer.initialize() }
    }

    override fun startVideoClient() {
        logger.info(TAG, "Starting video client")
        videoClient?.setReceiving(false)
        videoClient?.startServiceV2(
            "",
            "",
            meetingId,
            joinToken,
            false,
            0,
            0
        )
    }

    override fun stopVideoClient() {
        logger.info(TAG, "Stopping video client")
        videoClient?.stopService()
    }

    override fun destroyVideoClient() {
        logger.info(TAG, "Destroying video client")
        videoClient?.clearCurrentDevice()
        videoClientObserver.notifyVideoTileObserver { observer -> observer.destroy() }
        videoClient?.destroy()
        videoClient = null
        VideoClient.finalizeGlobals()
    }

    private fun initializeAppDetailedInfo() {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        val osVersion = android.os.Build.VERSION.RELEASE
        val packageName = context.packageName
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        val appVer = packageInfo.versionName
        val appCode = packageInfo.versionCode.toString()
        val clientSource = "amazon-chime-sdk"
        val sdkVersion = BuildConfig.VERSION_NAME

        VideoClient.AppDetailedInfo.initialize(
            String.format("Android %s", appVer),
            appCode,
            model,
            manufacturer,
            osVersion,
            clientSource,
            sdkVersion
        )
    }
}
