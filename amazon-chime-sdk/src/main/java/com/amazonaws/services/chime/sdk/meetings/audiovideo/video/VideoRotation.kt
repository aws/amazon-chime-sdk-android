/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoRotation] describes the rotation of the video frame buffer in degrees clockwise
 * from intended viewing horizon.
 *
 * e.g. If you were recording camera capture upside down relative to
 * the orientation of the sensor, this value would be [VideoRotation.Rotation180].
 */
enum class VideoRotation(val degrees: Int) {
    /**
     * Not rotated
     */
    Rotation0(0),

    /**
     * Rotated 90 degrees clockwise
     */
    Rotation90(90),

    /**
     * Rotated 180 degrees clockwise
     */
    Rotation180(180),

    /**
     * Rotated 270 degrees clockwise
     */
    Rotation270(270);

    companion object {
        fun from(intValue: Int): VideoRotation? = values().find { it.degrees == intValue }
    }
}
