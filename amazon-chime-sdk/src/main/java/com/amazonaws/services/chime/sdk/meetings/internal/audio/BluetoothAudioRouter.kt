/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType

/**
 * [BluetoothAudioRouter] abstracts Bluetooth audio routing operations across different Android API levels.
 *
 * This interface provides a unified contract for Bluetooth audio routing, with implementations
 * handling the API-specific details:
 * - API 23-30: Uses SCO-based routing via [android.media.AudioManager.startBluetoothSco]
 * - API 31+: Uses Communication Device API via [android.media.AudioManager.setCommunicationDevice]
 *
 * The routing operation is asynchronous - success or failure is reported via callbacks.
 */
internal interface BluetoothAudioRouter {
    /**
     * Routes audio to the specified Bluetooth device.
     *
     * The operation is asynchronous - success/failure is reported via callbacks.
     * Only one routing operation can be pending at a time; calling this method
     * while an operation is pending will cancel the previous operation.
     *
     * @param mediaDevice The target Bluetooth [MediaDevice]
     * @param route The AudioClient route constant for Bluetooth
     * @param onSuccess Callback invoked when routing succeeds, with route and device type
     * @param onFailure Callback invoked when routing fails or times out
     */
    fun routeToBluetoothDevice(
        mediaDevice: MediaDevice,
        route: Int,
        onSuccess: (route: Int, deviceType: MediaDeviceType) -> Unit,
        onFailure: () -> Unit
    )

    /**
     * Cancels any pending Bluetooth routing operation.
     *
     * Called when the user selects a different device or the audio session ends.
     * After cancellation, neither the success nor failure callback will be invoked.
     */
    fun cancelPendingOperation()
}
