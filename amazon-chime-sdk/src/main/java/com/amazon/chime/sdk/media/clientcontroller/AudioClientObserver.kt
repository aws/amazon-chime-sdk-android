/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.xodee.client.audio.audioclient.AudioClientLogListener
import com.xodee.client.audio.audioclient.AudioClientSignalStrengthChangeListener
import com.xodee.client.audio.audioclient.AudioClientStateChangeListener
import com.xodee.client.audio.audioclient.AudioClientVolumeStateChangeListener

/**
 * [AudioClientObserver]'s responsibility is to handle AudioClient callbacks and maintain all
 * the observers ([AudioVideoObserver], [RealtimeObserver]) that need to be notified
 */
interface AudioClientObserver : AudioClientStateChangeListener,
    AudioClientVolumeStateChangeListener,
    AudioClientSignalStrengthChangeListener, AudioClientLogListener {

    fun subscribeToAudioClientStateChange(observer: AudioVideoObserver)
    fun unsubscribeFromAudioClientStateChange(observer: AudioVideoObserver)
    fun notifyAudioClientObserver(observerFunction: (observer: AudioVideoObserver) -> Unit)
    fun subscribeToRealTimeEvents(observer: RealtimeObserver)
    fun unsubscribeFromRealTimeEvents(observer: RealtimeObserver)
}
