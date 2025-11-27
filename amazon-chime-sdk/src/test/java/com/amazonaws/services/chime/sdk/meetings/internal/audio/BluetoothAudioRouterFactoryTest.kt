/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import java.util.concurrent.Executor
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BluetoothAudioRouterFactory].
 *
 * **Validates: Invariant 1** from the design document:
 * - On API 23-30, the ScoBluetoothAudioRouter is used
 * - On API 31+, the CommunicationDeviceBluetoothAudioRouter is used
 *
 * _Requirements: 1.2, 1.3_
 */
class BluetoothAudioRouterFactoryTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioManager: AudioManager

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var mainExecutor: Executor

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock context to return itself for registerReceiver (needed by ScoBluetoothAudioRouter)
        every { context.registerReceiver(any(), any()) } returns mockk()

        // Mock context.mainExecutor (needed by CommunicationDeviceBluetoothAudioRouter)
        every { context.mainExecutor } returns mainExecutor
    }

    // ========== Test: API < 31 returns ScoBluetoothAudioRouter ==========
    // **Validates: Invariant 1 - API-level routing delegation**
    // _Requirements: 1.2_

    @Test
    fun `create should return ScoBluetoothAudioRouter for API 30`() {
        // When: Creating router for API 30
        val router = BluetoothAudioRouterFactory.create(
            context = context,
            audioManager = audioManager,
            logger = logger,
            buildVersion = Build.VERSION_CODES.R // API 30
        )

        // Then: Should return ScoBluetoothAudioRouter
        assertTrue(
            "Factory should return ScoBluetoothAudioRouter for API 30",
            router is ScoBluetoothAudioRouter
        )
    }

    @Test
    fun `create should return ScoBluetoothAudioRouter for API 29`() {
        // When: Creating router for API 29
        val router = BluetoothAudioRouterFactory.create(
            context = context,
            audioManager = audioManager,
            logger = logger,
            buildVersion = Build.VERSION_CODES.Q // API 29
        )

        // Then: Should return ScoBluetoothAudioRouter
        assertTrue(
            "Factory should return ScoBluetoothAudioRouter for API 29",
            router is ScoBluetoothAudioRouter
        )
    }

    @Test
    fun `create should return ScoBluetoothAudioRouter for API 23`() {
        // When: Creating router for API 23 (minimum supported)
        val router = BluetoothAudioRouterFactory.create(
            context = context,
            audioManager = audioManager,
            logger = logger,
            buildVersion = Build.VERSION_CODES.M // API 23
        )

        // Then: Should return ScoBluetoothAudioRouter
        assertTrue(
            "Factory should return ScoBluetoothAudioRouter for API 23",
            router is ScoBluetoothAudioRouter
        )
    }

    // ========== Test: API >= 31 returns CommunicationDeviceBluetoothAudioRouter ==========
    // **Validates: Invariant 1 - API-level routing delegation**
    // _Requirements: 1.3_

    @Test
    fun `create should return CommunicationDeviceBluetoothAudioRouter for API 31`() {
        // When: Creating router for API 31
        val router = BluetoothAudioRouterFactory.create(
            context = context,
            audioManager = audioManager,
            logger = logger,
            buildVersion = Build.VERSION_CODES.S // API 31
        )

        // Then: Should return CommunicationDeviceBluetoothAudioRouter
        assertTrue(
            "Factory should return CommunicationDeviceBluetoothAudioRouter for API 31",
            router is CommunicationDeviceBluetoothAudioRouter
        )
    }

    @Test
    fun `create should return CommunicationDeviceBluetoothAudioRouter for API 33`() {
        // When: Creating router for API 33
        val router = BluetoothAudioRouterFactory.create(
            context = context,
            audioManager = audioManager,
            logger = logger,
            buildVersion = Build.VERSION_CODES.TIRAMISU // API 33
        )

        // Then: Should return CommunicationDeviceBluetoothAudioRouter
        assertTrue(
            "Factory should return CommunicationDeviceBluetoothAudioRouter for API 33",
            router is CommunicationDeviceBluetoothAudioRouter
        )
    }

    @Test
    fun `create should return CommunicationDeviceBluetoothAudioRouter for API 34`() {
        // When: Creating router for API 34
        val router = BluetoothAudioRouterFactory.create(
            context = context,
            audioManager = audioManager,
            logger = logger,
            buildVersion = 34 // API 34
        )

        // Then: Should return CommunicationDeviceBluetoothAudioRouter
        assertTrue(
            "Factory should return CommunicationDeviceBluetoothAudioRouter for API 34",
            router is CommunicationDeviceBluetoothAudioRouter
        )
    }
}
