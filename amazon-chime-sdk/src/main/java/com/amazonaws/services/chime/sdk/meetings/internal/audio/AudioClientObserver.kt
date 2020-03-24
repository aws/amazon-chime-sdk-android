/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.xodee.client.audio.audioclient.AudioClientLogListener
import com.xodee.client.audio.audioclient.AudioClientMetricsListener
import com.xodee.client.audio.audioclient.AudioClientSignalStrengthChangeListener
import com.xodee.client.audio.audioclient.AudioClientStateChangeListener
import com.xodee.client.audio.audioclient.AudioClientVolumeStateChangeListener

/**
 * [AudioClientObserver]'s responsibility is to handle AudioClient callbacks and maintain all
 * the observers ([AudioVideoObserver], [RealtimeObserver]) that need to be notified
 */
interface AudioClientObserver : AudioClientStateChangeListener,
    AudioClientVolumeStateChangeListener,
    AudioClientSignalStrengthChangeListener, AudioClientLogListener,
    AudioClientMetricsListener {

    fun subscribeToAudioClientStateChange(observer: AudioVideoObserver)
    fun unsubscribeFromAudioClientStateChange(observer: AudioVideoObserver)
    fun notifyAudioClientObserver(observerFunction: (observer: AudioVideoObserver) -> Unit)
    fun subscribeToRealTimeEvents(observer: RealtimeObserver)
    fun unsubscribeFromRealTimeEvents(observer: RealtimeObserver)
}
