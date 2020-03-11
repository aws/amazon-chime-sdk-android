/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

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
