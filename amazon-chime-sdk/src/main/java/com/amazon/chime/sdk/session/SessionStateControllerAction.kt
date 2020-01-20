package com.amazon.chime.sdk.session

enum class SessionStateControllerAction(val value: Int) {
    Unknown(-1),
    Init(0),
    Connecting(1),
    Connected(2),
    Reconnecting(3),
    FailedToConnect(4),
    Disconnecting(5),
    DisconnectedNormal(6),
    DisconnectedAbnormal(7),
    ServerHungup(8)
}
