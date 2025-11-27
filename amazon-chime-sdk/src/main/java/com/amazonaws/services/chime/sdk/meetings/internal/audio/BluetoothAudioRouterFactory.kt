/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

/**
 * Factory for creating the appropriate [BluetoothAudioRouter] implementation based on API level.
 *
 * - API 23-30: Returns [ScoBluetoothAudioRouter] (SCO-based routing)
 * - API 31+: Returns [CommunicationDeviceBluetoothAudioRouter] (setCommunicationDevice-based routing)
 */
internal object BluetoothAudioRouterFactory {

    /**
     * Creates the appropriate BluetoothAudioRouter implementation for the current API level.
     *
     * @param context The application context
     * @param audioManager The AudioManager for controlling audio routing
     * @param logger The logger for debug and error messages
     * @param buildVersion The Android SDK version (defaults to Build.VERSION.SDK_INT)
     * @return The appropriate BluetoothAudioRouter implementation
     */
    @SuppressLint("NewApi")
    fun create(
        context: Context,
        audioManager: AudioManager,
        logger: Logger,
        buildVersion: Int = Build.VERSION.SDK_INT
    ): BluetoothAudioRouter {
        return if (buildVersion >= Build.VERSION_CODES.S) {
            CommunicationDeviceBluetoothAudioRouter(context, audioManager, logger)
        } else {
            ScoBluetoothAudioRouter(context, audioManager, logger)
        }
    }
}
