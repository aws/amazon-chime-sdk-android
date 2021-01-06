/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultModalityTest {
    private val id = "abc"
    private val modalityType = ModalityType.Content
    private val idWithModality = "abc#" + modalityType.value
    private val idWithOnlySeparator = DefaultModality.MODALITY_SEPARATOR

    @Test
    fun `id should return expected value`() {
        assertEquals(id, DefaultModality(id).id())
        assertEquals(idWithModality, DefaultModality(idWithModality).id())
        assertEquals(idWithOnlySeparator, DefaultModality(idWithOnlySeparator).id())
        assertEquals("", DefaultModality("").id())
    }

    @Test
    fun `base should return expected value`() {
        assertEquals(id, DefaultModality(id).base())
        assertEquals(id, DefaultModality(idWithModality).base())
        assertEquals("", DefaultModality(idWithOnlySeparator).base())
        assertEquals("", DefaultModality("").base())
    }

    @Test
    fun `modality should return expected value`() {
        assertNull(DefaultModality(id).modality())
        assertEquals(modalityType, DefaultModality(idWithModality).modality())
        assertNull(DefaultModality(idWithOnlySeparator).modality())
        assertNull(DefaultModality("").modality())
    }

    @Test
    fun `hasModality should return expected value`() {
        assertFalse(DefaultModality(id).hasModality(modalityType))
        assertTrue(DefaultModality(idWithModality).hasModality(modalityType))
        assertFalse(DefaultModality(idWithOnlySeparator).hasModality(modalityType))
        assertFalse(DefaultModality("").hasModality(modalityType))
    }
}
