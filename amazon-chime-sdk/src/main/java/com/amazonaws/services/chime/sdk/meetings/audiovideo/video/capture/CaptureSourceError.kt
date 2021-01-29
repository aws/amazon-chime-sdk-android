/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

/**
 * [CaptureSourceError] describes an error resulting from a capture source failure
 * These can be used to trigger UI, or attempt to restart the capture source.
 */
enum class CaptureSourceError {
    /**
     * Unknown error, and catch-all for errors not otherwise covered
     */
    Unknown,

    /**
     * A failure to obtain necessary permission to start video
     */
    PermissionError,

    /**
     * A failure observed from a system API used for capturing
     * e.g. In response to a `CameraDevice.StateCallback().onError` call
     */
    SystemFailure,

    /**
     * A failure observer during configuration
     */
    ConfigurationFailure;
}
