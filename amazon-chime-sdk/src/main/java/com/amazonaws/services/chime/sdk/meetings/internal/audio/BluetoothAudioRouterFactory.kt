/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.Context
import android.media.AudioManager
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

/**
 * Factory interface for creating [BluetoothAudioRouter] implementations.
 */
internal interface BluetoothAudioRouterFactory {
    /**
     * Creates a BluetoothAudioRouter implementation.
     *
     * @param context The application context
     * @param audioManager The AudioManager for controlling audio routing
     * @param logger The logger for debug and error messages
     * @return The appropriate BluetoothAudioRouter implementation
     */
    fun create(
        context: Context,
        audioManager: AudioManager,
        logger: Logger
    ): BluetoothAudioRouter
}
