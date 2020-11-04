/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

/**
 * [EglCoreFactory] is an factory interface for creating new [EglCore] objects, possible using shared state
 */
interface EglCoreFactory {
    /**
     * Create a new [EglCore] object
     *
     * @return [EglCore] - Newly created and initialized [EglCore] object
     */
    fun createEglCore(): EglCore
}
