/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

/**
 * Configuration constants for Bluetooth audio routing operations.
 *
 * Contains timeout and retry parameters used by [BluetoothAudioRouter] implementations.
 */
internal object BluetoothRoutingConfig {
    /**
     * Timeout duration for SCO connection (API 23-30).
     *
     * This timeout is a reasonable estimate and can be tuned further if needed.
     */
    const val SCO_CONNECTION_TIMEOUT_MS = 3000L

    /**
     * Timeout duration for API 31+ Bluetooth device switching.
     *
     * Android documentation recommends up to 30 seconds for Bluetooth device changes.
     * See: https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-manager#set_communication_device
     *
     * Since 30 seconds is too long for a voice call, we use 10 seconds instead.
     */
    const val COMMUNICATION_DEVICE_TIMEOUT_MS = 10000L

    /**
     * Delay between retry attempts for Bluetooth routing mismatch recovery (API 31+).
     *
     * When setCommunicationDevice() returns true but the listener fires with a different device,
     * we retry after this delay to allow the Bluetooth SCO teardown to complete.
     * Car Bluetooth adapters may need longer than headphones for SCO teardown.
     */
    const val BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS = 1000L

    /**
     * Maximum number of retry attempts for Bluetooth routing mismatch recovery (API 31+).
     */
    const val BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES = 5
}
