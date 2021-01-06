/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

/**
 * [ContentShareStatus] indicates a status received regarding the content share.
 *
 * @param statusCode: [ContentShareStatusCode] - Additional details for the status
 */
data class ContentShareStatus(val statusCode: ContentShareStatusCode)
