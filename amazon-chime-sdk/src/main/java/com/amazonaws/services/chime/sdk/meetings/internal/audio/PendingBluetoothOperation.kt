/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType

/**
 * Encapsulates state for a pending Bluetooth audio route operation.
 *
 * This is used to defer setRoute() calls until Bluetooth connection is confirmed.
 * The operation is considered complete when either [onSuccess] or [onFailure] is invoked.
 *
 * @property route The AudioClient route constant for Bluetooth
 * @property deviceType The type of the target Bluetooth device
 * @property deviceId The AudioDeviceInfo ID (only used for API 31+ retry-on-mismatch)
 * @property onSuccess Callback invoked when routing succeeds, with route and device type
 * @property onFailure Callback invoked when routing fails or times out
 * @property timestamp The time when this operation was created, used for timeout tracking
 */
internal data class PendingBluetoothOperation(
    val route: Int,
    val deviceType: MediaDeviceType,
    val deviceId: Int?,
    val onSuccess: (route: Int, deviceType: MediaDeviceType) -> Unit,
    val onFailure: () -> Unit,
    val timestamp: Long = System.currentTimeMillis()
)
