package com.amazon.chime.sdk.utils.logger

enum class LogLevel(val priority: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    OFF(5)
}
