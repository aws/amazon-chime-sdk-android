/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video.gl

/**
 * [ShareEglLock] is to synchronizes the EGL operations. The main motivation is to avoid crashes
 * due to missing frames during rendering from race condition.
 */
class ShareEglLock {
    companion object {
        val Lock = object {}
    }
}
