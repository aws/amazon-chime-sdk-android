/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.utils.singleton

/**
 * If the singleton needed no parameters we can just use companion object instead of this.
 * Referencing [[SynchronizedLazyImpl]] for double checked locking algorithm.
 */
open class SingletonWithParams<out T : Any, in A>(instanceCreator: (A) -> T) {
    private var instanceCreator: ((A) -> T)? = instanceCreator

    @Volatile
    private var instance: T? = null

    fun getInstance(args: A): T {
        val instance1 = instance
        if (instance1 != null) {
            return instance1
        }

        return synchronized(this) {
            val instance2 = instance
            if (instance2 != null) {
                instance2
            } else {
                val createdInstance = instanceCreator!!(args)
                instance = createdInstance
                instanceCreator = null
                createdInstance
            }
        }
    }
}
