/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.lang.Exception
import org.junit.Before
import org.junit.Test

// For some reason, this test won't be recognized as a test if it is called `RefCountDelegateTest`
class RefCountTest {

    @MockK
    private lateinit var mockCallback: Runnable

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `release should trigger release callback on newly created delegate`() {
        val refCountDelegate = RefCountDelegate(mockCallback)
        refCountDelegate.release()

        verify(exactly = 1) { mockCallback.run() }
    }

    @Test
    fun `release should not trigger release callback on delegate after retain is called`() {
        val refCountDelegate = RefCountDelegate(mockCallback)
        refCountDelegate.retain()
        refCountDelegate.release()

        verify(exactly = 0) { mockCallback.run() }
    }

    @Test
    fun `release should trigger release callback if release is called the same amount of times as retain + 1`() {
        val refCountDelegate = RefCountDelegate(mockCallback)
        refCountDelegate.retain()
        refCountDelegate.retain()
        refCountDelegate.retain()
        refCountDelegate.release()
        refCountDelegate.release()
        refCountDelegate.release()

        refCountDelegate.retain()
        refCountDelegate.release()
        refCountDelegate.retain()
        refCountDelegate.release()

        refCountDelegate.release()

        verify(exactly = 1) { mockCallback.run() }
    }

    @Test
    fun `additional releases should throw exception and not retrigger callback`() {
        val refCountDelegate = RefCountDelegate(mockCallback)

        refCountDelegate.release()

        var exceptionThrown = false
        try {
            refCountDelegate.release()
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assert(exceptionThrown)
        verify(exactly = 1) { mockCallback.run() }
    }

    @Test
    fun `additional retains should throw exception`() {
        val refCountDelegate = RefCountDelegate(mockCallback)

        refCountDelegate.release()

        var exceptionThrown = false
        try {
            refCountDelegate.retain()
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assert(exceptionThrown)
        verify(exactly = 1) { mockCallback.run() }
    }
}
