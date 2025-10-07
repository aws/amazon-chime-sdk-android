/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import org.amazon.chime.webrtc.NetworkChangeDetector.ConnectionType

/**
 * Represents the network connection type
 */
enum class NetworkConnectionType(val description: String) {
    /**
     * Unknown network type
     */
    UNKNOWN("Unknown"),

    /**
     * Ethernet network type
     */
    ETHERNET("Ethernet"),

    /**
     * Wi-Fi network type
     */
    WIFI("Wifi"),

    /**
     * 5G mobile network type
     */
    FIVE_G("5G"),

    /**
     * 4G mobile network type
     */
    FOUR_G("4G"),

    /**
     * 3G mobile network type
     */
    THREE_G("3G"),

    /**
     * 2G mobile network type
     */
    TWO_G("2G"),

    /**
     * Generic/unknown cellular network (used when type cannot be determined)
     */
    CELLULAR("Cellular"),

    /**
     * Bluetooth network type
     */
    BLUETOOTH("Bluetooth"),

    /**
     * VPN network type
     */
    VPN("VPN"),

    /**
     * No network connection
     */
    NONE("None");

    companion object {
        /**
         * Maps a WebRTC-specific ConnectionType enum to this SDK-friendly NetworkConnectionType.
         *
         * @param connectionType The WebRTC ConnectionType reported by NetworkMonitor.
         * @return Corresponding NetworkConnectionType.
         */
        fun fromWebRTCConnectionType(connectionType: ConnectionType): NetworkConnectionType {
            return when (connectionType) {
                ConnectionType.CONNECTION_UNKNOWN -> UNKNOWN
                ConnectionType.CONNECTION_ETHERNET -> ETHERNET
                ConnectionType.CONNECTION_WIFI -> WIFI
                ConnectionType.CONNECTION_5G -> FIVE_G
                ConnectionType.CONNECTION_4G -> FOUR_G
                ConnectionType.CONNECTION_3G -> THREE_G
                ConnectionType.CONNECTION_2G -> TWO_G
                ConnectionType.CONNECTION_UNKNOWN_CELLULAR -> CELLULAR
                ConnectionType.CONNECTION_BLUETOOTH -> BLUETOOTH
                ConnectionType.CONNECTION_VPN -> VPN
                ConnectionType.CONNECTION_NONE -> NONE
            }
        }
    }
}
