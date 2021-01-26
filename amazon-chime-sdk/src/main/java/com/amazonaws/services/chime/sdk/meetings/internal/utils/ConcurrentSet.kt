/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class ConcurrentSet {
    companion object {
        fun <T> createConcurrentSet(): MutableSet<T> {
            return Collections.newSetFromMap(ConcurrentHashMap<T, Boolean>())
        }
    }
}
