/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

import com.amazonaws.services.chime.sdk.BuildConfig

/**
 * [Versioning] provides API to retrieve SDK version
 */
class Versioning {
    companion object {
        /**
         * Return current version of Amazon Chime SDK for Android.
         */
        fun sdkVersion(): String {
            return BuildConfig.VERSION_NAME
        }
    }
}
