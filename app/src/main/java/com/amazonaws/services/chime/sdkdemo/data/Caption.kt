/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.data

data class Caption(
    val speakerName: String?,
    val isPartial: Boolean,
    val content: String
)
