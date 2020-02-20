/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.utils.logger

import android.util.Log

class ConsoleLogger(private var level: LogLevel = LogLevel.WARN) : Logger {

    override fun verbose(tag: String, msg: String) {
        this.log(LogLevel.VERBOSE, tag, msg)
    }

    override fun debug(tag: String, msg: String) {
        this.log(LogLevel.DEBUG, tag, msg)
    }

    override fun info(tag: String, msg: String) {
        this.log(LogLevel.INFO, tag, msg)
    }

    override fun warn(tag: String, msg: String) {
        this.log(LogLevel.WARN, tag, msg)
    }

    override fun error(tag: String, msg: String) {
        this.log(LogLevel.ERROR, tag, msg)
    }

    override fun setLogLevel(level: LogLevel) {
        this.level = level
    }

    override fun getLogLevel(): LogLevel {
        return this.level
    }

    private fun log(type: LogLevel, tag: String, msg: String) {
        if (type.priority < this.level.priority) return

        when (type) {
            LogLevel.VERBOSE -> Log.v(tag, msg)
            LogLevel.DEBUG -> Log.d(tag, msg)
            LogLevel.INFO -> Log.i(tag, msg)
            LogLevel.WARN -> Log.w(tag, msg)
            LogLevel.ERROR -> Log.e(tag, msg)
        }
    }
}
