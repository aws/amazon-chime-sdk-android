/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.PrimaryMeetingPromotionObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.TranscriptEventObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.xodee.client.audio.audioclient.AudioClientLogListener
import com.xodee.client.audio.audioclient.AudioClientMetricsListener
import com.xodee.client.audio.audioclient.AudioClientPresenceListener
import com.xodee.client.audio.audioclient.AudioClientPrimaryMeetingEventListener
import com.xodee.client.audio.audioclient.AudioClientSignalStrengthChangeListener
import com.xodee.client.audio.audioclient.AudioClientStateChangeListener
import com.xodee.client.audio.audioclient.AudioClientTranscriptEventsListener
import com.xodee.client.audio.audioclient.AudioClientVolumeStateChangeListener

/**
 * [AudioClientObserver]'s responsibility is to handle AudioClient callbacks and maintain all
 * the observers ([AudioVideoObserver], [RealtimeObserver]) that need to be notified
 */
interface AudioClientObserver : AudioClientStateChangeListener,
    AudioClientVolumeStateChangeListener,
    AudioClientSignalStrengthChangeListener, AudioClientLogListener,
    AudioClientMetricsListener, AudioClientPresenceListener,
    AudioClientTranscriptEventsListener, AudioClientPrimaryMeetingEventListener {

    fun subscribeToAudioClientStateChange(observer: AudioVideoObserver)
    fun unsubscribeFromAudioClientStateChange(observer: AudioVideoObserver)
    fun notifyAudioClientObserver(observerFunction: (observer: AudioVideoObserver) -> Unit)
    fun subscribeToRealTimeEvents(observer: RealtimeObserver)
    fun unsubscribeFromRealTimeEvents(observer: RealtimeObserver)
    fun subscribeToTranscriptEvent(observer: TranscriptEventObserver)
    fun unsubscribeFromTranscriptEvent(observer: TranscriptEventObserver)
    // This only supports one observer at a time
    var primaryMeetingPromotionObserver: PrimaryMeetingPromotionObserver?
    var currentAudioStatus: MeetingSessionStatusCode?
}
