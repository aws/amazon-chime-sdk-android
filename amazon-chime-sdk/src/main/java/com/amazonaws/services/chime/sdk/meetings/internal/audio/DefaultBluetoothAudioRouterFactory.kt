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
 * Default implementation of [BluetoothAudioRouterFactory] that creates the appropriate
 * [BluetoothAudioRouter] implementation based on API level.
 *
 * - API 23-30: Returns [ScoBluetoothAudioRouter] (SCO-based routing)
 * - API 31+: Returns [CommunicationDeviceBluetoothAudioRouter] (setCommunicationDevice-based routing)
 *
 * @param buildVersion The Android SDK version (defaults to Build.VERSION.SDK_INT)
 */
internal class DefaultBluetoothAudioRouterFactory(
    private val buildVersion: Int = Build.VERSION.SDK_INT
) : BluetoothAudioRouterFactory {

    @SuppressLint("NewApi")
    override fun create(
        context: Context,
        audioManager: AudioManager,
        logger: Logger
    ): BluetoothAudioRouter {
        return if (buildVersion >= Build.VERSION_CODES.S) {
            CommunicationDeviceBluetoothAudioRouter(context, audioManager, logger)
        } else {
            ScoBluetoothAudioRouter(context, audioManager, logger)
        }
    }
}
