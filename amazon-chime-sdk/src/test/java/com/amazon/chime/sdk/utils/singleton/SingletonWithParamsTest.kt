package com.amazon.chime.sdk.utils.singleton

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class TestClass private constructor(val hasProp: Boolean) {
    companion object : SingletonWithParams<TestClass, Boolean>(::TestClass)
}

class SingletonWithParamsTest {

    @Test
    fun `getInstance should return instance with given parameters`() {
        val instance = TestClass.getInstance(true)
        assertTrue(instance.hasProp)
    }

    @Test
    fun `getInstance should return singleton when called multiple times`() {
        val instance = TestClass.getInstance(true)
        val anotherInstance = TestClass.getInstance(true)
        assertTrue(instance === anotherInstance)
    }

    @Test
    fun `getInstance should return same instance when called from multiple threads`() = runBlocking {
        val instance = TestClass.getInstance(true)
        val assertions = launch {
            repeat(3) {
                launch {
                    val otherInstance = TestClass.getInstance(true)
                    assertTrue(otherInstance === instance)
                }
            }
        }
        assertions.join()
    }
}
