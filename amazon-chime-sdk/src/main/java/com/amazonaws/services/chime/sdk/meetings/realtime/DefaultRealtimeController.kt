/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime

import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver

class DefaultRealtimeController(
    private val audioClientController: AudioClientController,
    private val audioClientObserver: AudioClientObserver,
    private val videoClientController: VideoClientController,
    private val videoClientObserver: VideoClientObserver
) :
    RealtimeControllerFacade {

    override fun realtimeLocalMute(): Boolean {
        return audioClientController.setMute(true)
    }

    override fun realtimeLocalUnmute(): Boolean {
        return audioClientController.setMute(false)
    }

    override fun addRealtimeObserver(observer: RealtimeObserver) {
        audioClientObserver.subscribeToRealTimeEvents(observer)
    }

    override fun removeRealtimeObserver(observer: RealtimeObserver) {
        audioClientObserver.unsubscribeFromRealTimeEvents(observer)
    }

    override fun realtimeSendDataMessage(topic: String, data: Any, lifetimeMs: Int) {
        videoClientController.sendDataMessage(topic, data, lifetimeMs)
    }

    override fun addRealtimeDataMessageObserver(topic: String, observer: DataMessageObserver) {
        videoClientObserver.subscribeToReceiveDataMessage(topic, observer)
    }

    override fun removeRealtimeDataMessageObserverFromTopic(topic: String) {
        videoClientObserver.unsubscribeFromReceiveDataMessage(topic)
    }

    override fun realtimeSetVoiceFocusEnabled(enabled: Boolean): Boolean {
        return audioClientController.setVoiceFocusEnabled(enabled)
    }

    override fun realtimeIsVoiceFocusEnabled(): Boolean {
        return audioClientController.isVoiceFocusEnabled()
    }
}
