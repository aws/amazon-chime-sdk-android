/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.Context
import android.util.Log
import com.xodee.client.audio.audioclient.AudioClient

class AudioClientFactory private constructor(
    context: Context,
    audioClientObserver: AudioClientObserver
) {
    private val audioClient: AudioClient

    init {
        try {
            System.loadLibrary("c++_shared")
            System.loadLibrary("amazon_chime_media_client")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("AudioClientFactory", "Unable to load native media libraries: ${e.localizedMessage}")
        }
        audioClient = AudioClient(
            context.assets,
            audioClientObserver,
            audioClientObserver,
            audioClientObserver,
            audioClientObserver,
            audioClientObserver,
            audioClientObserver,
            audioClientObserver,
            audioClientObserver,
            0
        )
    }

    companion object {
        fun getAudioClient(
            context: Context,
            audioClientObserver: AudioClientObserver
        ): AudioClient {
            return AudioClientFactory(
                context,
                audioClientObserver
            ).audioClient
        }
    }
}
