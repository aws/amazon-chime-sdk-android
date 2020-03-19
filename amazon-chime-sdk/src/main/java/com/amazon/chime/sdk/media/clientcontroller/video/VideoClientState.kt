/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller.video

/**
 * The current state of the Video Client. Used by [VideoClientStateController] for lifecycle methods
 * and to determine if [VideoClientController] actions are allowed based on current state.
 */
enum class VideoClientState(val value: Int) {
    UNINITIALIZED(-1),
    INITIALIZED(0),
    STARTED(1),
    STOPPED(2),
}
