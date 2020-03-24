/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerDetectorFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy.ActiveSpeakerPolicy
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver
import com.amazonaws.services.chime.sdk.meetings.device.DeviceController
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeControllerFacade
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver

class DefaultAudioVideoFacade(
    private val context: Context,
    private val audioVideoController: AudioVideoControllerFacade,
    private val realtimeController: RealtimeControllerFacade,
    private val deviceController: DeviceController,
    private val videoTileController: VideoTileController,
    private val activeSpeakerDetector: ActiveSpeakerDetectorFacade
) : AudioVideoFacade {

    private val permissions = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.RECORD_AUDIO
    )

    override fun start() {
        val hasPermission: Boolean = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            audioVideoController.start()
        } else {
            throw SecurityException(
                "Missing necessary permissions for WebRTC: ${permissions.joinToString(
                    separator = ", ",
                    prefix = "",
                    postfix = ""
                )}"
            )
        }
    }

    override fun addAudioVideoObserver(observer: AudioVideoObserver) {
        audioVideoController.addAudioVideoObserver(observer)
    }

    override fun removeAudioVideoObserver(observer: AudioVideoObserver) {
        audioVideoController.removeAudioVideoObserver(observer)
    }

    override fun addMetricsObserver(observer: MetricsObserver) {
        audioVideoController.addMetricsObserver(observer)
    }

    override fun removeMetricsObserver(observer: MetricsObserver) {
        audioVideoController.removeMetricsObserver(observer)
    }

    override fun stop() {
        audioVideoController.stop()
    }

    override fun startLocalVideo() {
        audioVideoController.startLocalVideo()
    }

    override fun stopLocalVideo() {
        audioVideoController.stopLocalVideo()
    }

    override fun startRemoteVideo() {
        audioVideoController.startRemoteVideo()
    }

    override fun stopRemoteVideo() {
        audioVideoController.stopRemoteVideo()
    }

    override fun realtimeLocalMute(): Boolean {
        return realtimeController.realtimeLocalMute()
    }

    override fun realtimeLocalUnmute(): Boolean {
        return realtimeController.realtimeLocalUnmute()
    }

    override fun addRealtimeObserver(observer: RealtimeObserver) {
        realtimeController.addRealtimeObserver(observer)
    }

    override fun removeRealtimeObserver(observer: RealtimeObserver) {
        realtimeController.removeRealtimeObserver(observer)
    }

    override fun listAudioDevices(): List<MediaDevice> {
        return deviceController.listAudioDevices()
    }

    override fun chooseAudioDevice(mediaDevice: MediaDevice) {
        deviceController.chooseAudioDevice(mediaDevice)
    }

    override fun getActiveCamera(): MediaDevice? {
        return deviceController.getActiveCamera()
    }

    override fun switchCamera() {
        deviceController.switchCamera()
    }

    override fun addDeviceChangeObserver(observer: DeviceChangeObserver) {
        deviceController.addDeviceChangeObserver(observer)
    }

    override fun removeDeviceChangeObserver(observer: DeviceChangeObserver) {
        deviceController.removeDeviceChangeObserver(observer)
    }

    override fun bindVideoView(videoView: VideoRenderView, tileId: Int) {
        videoTileController.bindVideoView(videoView, tileId)
    }

    override fun unbindVideoView(tileId: Int) {
        videoTileController.unbindVideoView(tileId)
    }

    override fun addVideoTileObserver(observer: VideoTileObserver) {
        videoTileController.addVideoTileObserver(observer)
    }

    override fun removeVideoTileObserver(observer: VideoTileObserver) {
        videoTileController.removeVideoTileObserver(observer)
    }

    override fun pauseRemoteVideoTile(tileId: Int) {
        videoTileController.pauseRemoteVideoTile(tileId)
    }

    override fun resumeRemoteVideoTile(tileId: Int) {
        videoTileController.resumeRemoteVideoTile(tileId)
    }

    override fun addActiveSpeakerObserver(
        policy: ActiveSpeakerPolicy,
        observer: ActiveSpeakerObserver
    ) {
        activeSpeakerDetector.addActiveSpeakerObserver(policy, observer)
    }

    override fun removeActiveSpeakerObserver(observer: ActiveSpeakerObserver) {
        activeSpeakerDetector.removeActiveSpeakerObserver(observer)
    }
}
