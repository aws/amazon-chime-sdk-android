/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class ObserverUtilsTest {
    @MockK
    private lateinit var mockObserver: MockObserver

    class MockObserver {
        fun mockFn() {
        }
    }

    private val testDispatcher = TestCoroutineDispatcher()
    private val mockObservers = mutableSetOf<MockObserver>()

    @Before
    fun setUp() {
        mockkStatic(System::class)
        Dispatchers.setMain(testDispatcher)
        mockObserver = mockk(relaxed = true)
        mockObservers.add(mockObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `forEachObserver should call function that is passed in`() {
        ObserverUtils.notifyObserverOnMainThread(mockObservers) { it.mockFn() }

        verify { mockObserver.mockFn() }
    }
}
