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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("Skipping all tests due to issue with mocking System::class")
class ObserverUtilsTest {
    @MockK
    private lateinit var mockObserver: MockObserver

    class MockObserver {
        fun mockFn() {
        }
    }

    private val testDispatcher = TestCoroutineDispatcher()
    private val mockObservers = ConcurrentSet.createConcurrentSet<MockObserver>()

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

    @Test
    fun `forEachObserver should call for each observer that is added concurrently`() = runBlockingTest {
        launch {
            for (index in 0..49) {
                val mockObserver: MockObserver = mockk(relaxed = true)
                mockObservers.add(mockObserver)
            }
        }
        val deferred = async {
            for (index in 0..48) {
                val mockObserver: MockObserver = mockk(relaxed = true)
                mockObservers.add(mockObserver)
            }
        }
        deferred.await()
        ObserverUtils.notifyObserverOnMainThread(mockObservers) { it.mockFn() }

        assertEquals(mockObservers.size, 100)
        verify {
            for (observer in mockObservers) {
                observer.mockFn()
            }
        }
    }
}
