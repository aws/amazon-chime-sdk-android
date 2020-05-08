/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

/**
 * The current state of the Audio Client. Used by [AudioClientController]
 * to determine if actions are allowed based on current state.
 */
enum class AudioClientState(val value: Int) {
    INITIALIZED(0),
    STARTED(1),
    STOPPED(2),
}
