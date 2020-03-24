/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal

/**
 * [SessionStateControllerAction] is a state-changing action to perform.
 */
enum class SessionStateControllerAction(val value: Int) {
    Unknown(-1),
    Init(0),
    Connecting(1),
    FinishConnecting(2),
    Updating(3),
    FinishUpdating(4),
    Reconnecting(5),
    Disconnecting(6),
    FinishDisconnecting(7),
    Fail(8);
}
