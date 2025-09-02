/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsObserver
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingHistoryEvent
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioDeviceCapabilities
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerDetectorFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy.ActiveSpeakerPolicy
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.LocalVideoConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver
import com.amazonaws.services.chime.sdk.meetings.device.DeviceController
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.ingestion.AppStateMonitor
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeControllerFacade
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.TranscriptEventObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.utils.PermissionError

class DefaultAudioVideoFacade(
    private val context: Context,
    private val audioVideoController: AudioVideoControllerFacade,
    private val realtimeController: RealtimeControllerFacade,
    private val deviceController: DeviceController,
    private val videoTileController: VideoTileController,
    private val activeSpeakerDetector: ActiveSpeakerDetectorFacade,
    private val contentShareController: ContentShareController,
    private val eventAnalyticsController: EventAnalyticsController,
    private val appStateMonitor: AppStateMonitor
) : AudioVideoFacade {

    override fun start() {
        start(AudioVideoConfiguration())
    }

    override fun start(audioVideoConfiguration: AudioVideoConfiguration) {
        appStateMonitor.start()
        checkAudioPermissions(audioVideoConfiguration.audioDeviceCapabilities)
        audioVideoController.start(audioVideoConfiguration)
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
        appStateMonitor.stop()
        audioVideoController.stop()
    }

    override fun startLocalVideo() {
        audioVideoController.startLocalVideo()
    }

    override fun startLocalVideo(config: LocalVideoConfiguration) {
        audioVideoController.startLocalVideo(config)
    }

    override fun startLocalVideo(source: VideoSource) {
        audioVideoController.startLocalVideo(source)
    }

    override fun startLocalVideo(source: VideoSource, config: LocalVideoConfiguration) {
        audioVideoController.startLocalVideo(source, config)
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

    override fun updateVideoSourceSubscriptions(
        addedOrUpdated: Map<RemoteVideoSource, VideoSubscriptionConfiguration>,
        removed: Array<RemoteVideoSource>
    ) {
        audioVideoController.updateVideoSourceSubscriptions(addedOrUpdated, removed)
    }

    override fun promoteToPrimaryMeeting(
        credentials: MeetingSessionCredentials,
        observer: PrimaryMeetingPromotionObserver
    ) {
        audioVideoController.promoteToPrimaryMeeting(credentials, observer)
    }

    override fun demoteFromPrimaryMeeting() {
        contentShareController.stopContentShare()
        audioVideoController.demoteFromPrimaryMeeting()
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

    override fun realtimeSendDataMessage(topic: String, data: Any, lifetimeMs: Int) {
        realtimeController.realtimeSendDataMessage(topic, data, lifetimeMs)
    }

    override fun addRealtimeDataMessageObserver(topic: String, observer: DataMessageObserver) {
        realtimeController.addRealtimeDataMessageObserver(topic, observer)
    }

    override fun removeRealtimeDataMessageObserverFromTopic(topic: String) {
        realtimeController.removeRealtimeDataMessageObserverFromTopic(topic)
    }

    override fun realtimeSetVoiceFocusEnabled(enabled: Boolean): Boolean {
        return realtimeController.realtimeSetVoiceFocusEnabled(enabled)
    }

    override fun realtimeIsVoiceFocusEnabled(): Boolean {
        return realtimeController.realtimeIsVoiceFocusEnabled()
    }

    override fun listAudioDevices(): List<MediaDevice> {
        return deviceController.listAudioDevices()
    }

    override fun chooseAudioDevice(mediaDevice: MediaDevice) {
        deviceController.chooseAudioDevice(mediaDevice)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun getActiveAudioDevice(): MediaDevice? {
        return deviceController.getActiveAudioDevice()
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

    override fun startContentShare(source: ContentShareSource) {
        contentShareController.startContentShare(source)
    }

    override fun startContentShare(source: ContentShareSource, config: LocalVideoConfiguration) {
        contentShareController.startContentShare(source, config)
    }

    override fun stopContentShare() {
        contentShareController.stopContentShare()
    }

    override fun addContentShareObserver(observer: ContentShareObserver) {
        contentShareController.addContentShareObserver(observer)
    }

    override fun removeContentShareObserver(observer: ContentShareObserver) {
        contentShareController.removeContentShareObserver(observer)
    }

    override fun addEventAnalyticsObserver(observer: EventAnalyticsObserver) {
        eventAnalyticsController.addEventAnalyticsObserver(observer)
    }

    override fun removeEventAnalyticsObserver(observer: EventAnalyticsObserver) {
        eventAnalyticsController.removeEventAnalyticsObserver(observer)
    }

    override fun getMeetingHistory(): List<MeetingHistoryEvent> {
        return eventAnalyticsController.getMeetingHistory()
    }

    override fun getCommonEventAttributes(): EventAttributes {
        return eventAnalyticsController.getCommonEventAttributes()
    }

    override fun addRealtimeTranscriptEventObserver(observer: TranscriptEventObserver) {
        realtimeController.addRealtimeTranscriptEventObserver(observer)
    }

    override fun removeRealtimeTranscriptEventObserver(observer: TranscriptEventObserver) {
        realtimeController.removeRealtimeTranscriptEventObserver(observer)
    }

    private fun checkAudioPermissions(audioDeviceCapabilities: AudioDeviceCapabilities) {
        val hasRequiredPermissions: Boolean = audioDeviceCapabilities.requiredPermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!hasRequiredPermissions) {
            val attributes = mutableMapOf<EventAttributeName, Any>(
                EventAttributeName.audioInputErrorMessage to PermissionError.AudioPermissionError
            )
            eventAnalyticsController.publishEvent(EventName.audioInputFailed, attributes)
            throw SecurityException("Missing necessary permissions for WebRTC: ${audioDeviceCapabilities.requiredPermissions().joinToString()}")
        }
    }
}
