/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

import android.content.Context
import com.amazon.chime.sdk.utils.singleton.SingletonWithParams
import com.xodee.client.audio.audioclient.AudioClient

/**
 * This is so that we only need one version of [SingletonWithParams].
 */
data class AudioClientSingletonParams(
    val context: Context,
    val audioClientObserver: AudioClientObserver
)

/**
 * Singleton to prevent more than one [AudioClient] from being created.
 *
 * We are using this pattern of [SingletonWithParams] and [AudioClientSingletonParams]
 * because object currently does not support parameters and we need them for initialization.
 */
class AudioClientSingleton private constructor(params: AudioClientSingletonParams) {
    var audioClient: AudioClient

    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("biba_media_client")

        audioClient = AudioClient(
            params.context.assets,
            params.audioClientObserver,
            params.audioClientObserver,
            params.audioClientObserver,
            params.audioClientObserver,
            0
        )
    }

    companion object :
        SingletonWithParams<AudioClientSingleton, AudioClientSingletonParams>(::AudioClientSingleton)
}
