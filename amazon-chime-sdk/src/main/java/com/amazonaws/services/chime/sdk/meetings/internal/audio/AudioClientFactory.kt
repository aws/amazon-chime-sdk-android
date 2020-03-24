/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.Context
import com.xodee.client.audio.audioclient.AudioClient

class AudioClientFactory private constructor(
    context: Context,
    audioClientObserver: AudioClientObserver
) {
    private val audioClient: AudioClient

    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("amazon_chime_media_client")
        audioClient = AudioClient(
            context.assets,
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
