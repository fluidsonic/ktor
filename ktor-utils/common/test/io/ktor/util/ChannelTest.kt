/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import io.ktor.test.dispatcher.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.test.*

class ChannelTest {

    @Test
    fun testCopyToBoth() = testSuspend {
        val data = ByteArray(16 * 1024) { it.toByte() }
        val source = ByteChannel()
        val first = ByteChannel()
        val second = ByteChannel()

        source.copyToBoth(first, second)

        launch(Dispatchers.Unconfined) {
            source.writeFully(data)
            source.close()
        }

        val firstResult = async(Dispatchers.Unconfined) {
            first.readRemaining().readBytes()
        }
        val secondResult = async(Dispatchers.Unconfined) {
            second.readRemaining().readBytes()
        }

        val results = listOf(firstResult, secondResult).awaitAll()
        assertArrayEquals(data, results[0])
        assertArrayEquals(data, results[1])
    }

    @Test
    fun testCopyToBothCancelSource() = testSuspend {
        val source = ByteChannel()
        val first = ByteChannel()
        val second = ByteChannel()

        source.copyToBoth(first, second)

        val message = "Expected reason"

        launch(Dispatchers.Unconfined) {
            source.cancel(IllegalStateException(message))
        }

        assertFailsWithMessage(message) {
            val firstResult = GlobalScope.async(Dispatchers.Unconfined) {
                first.readRemaining().readBytes()
            }
            firstResult.await()

        }

        assertFailsWithMessage(message) {
            val secondResult = GlobalScope.async(Dispatchers.Unconfined) {
                second.readRemaining().readBytes()
            }
            secondResult.await()
        }
    }

    @Test
    fun testCopyToBothCancelFirstReader() = testSuspend {
        val data = ByteArray(16 * 1024) { it.toByte() }
        val source = ByteChannel()
        val first = ByteChannel()
        val second = ByteChannel()

        source.copyToBoth(first, second)

        val message = "Expected reason"

        val sourceResult = GlobalScope.async(Dispatchers.Unconfined) {
            source.writeFully(data)
            source.close()
        }

        first.cancel(IllegalStateException(message))

        assertFailsWithMessage(message) {
            sourceResult.await()
        }

        assertFailsWithMessage(message) {
            val secondResult = GlobalScope.async(Dispatchers.Unconfined) {
                second.readRemaining().readBytes()
            }
            secondResult.await()
        }
    }

    @Test
    fun testCopyToBothCancelSecondReader() = testSuspend {
        val data = ByteArray(16 * 1024) { it.toByte() }
        val source = ByteChannel()
        val first = ByteChannel()
        val second = ByteChannel()

        source.copyToBoth(second, first)

        val message = "Expected reason"

        val sourceResult = GlobalScope.async(Dispatchers.Unconfined) {
            source.writeFully(data)
            source.close()
        }

        first.cancel(IllegalStateException(message))

        assertFailsWithMessage(message) {
            val secondResult = GlobalScope.async(Dispatchers.Unconfined) {
                second.readRemaining().readBytes()
            }
            secondResult.await()
        }

        assertFailsWithMessage(message) {
            sourceResult.await()
        }
    }

    @Test
    fun testSplit() = testSuspend {
        val data = ByteArray(16 * 1024) { it.toByte() }
        val source = ByteChannel()

        val (first, second) = source.split()

        launch(Dispatchers.Unconfined) {
            source.writeFully(data)
            source.close()
        }

        val firstResult = async(Dispatchers.Unconfined) {
            first.readRemaining().readBytes()
        }
        val secondResult = async(Dispatchers.Unconfined) {
            second.readRemaining().readBytes()
        }

        val result1 = firstResult.await()
        val result2 = secondResult.await()
        assertArrayEquals(data, result1)
        assertArrayEquals(data, result2)
    }

    @Test
    fun testSplitCancelSource() = testSuspend {
        val source = ByteChannel()

        val (first, second) = source.split()

        val message = "Expected reason"
        launch {
            source.cancel(IllegalStateException(message))
        }

        assertFailsWithMessage("Expected reason") {
            val firstResult = GlobalScope.async(Dispatchers.Unconfined) {
                first.readRemaining().readBytes()
            }
            firstResult.await()
        }

        assertFailsWithMessage("Expected reason") {
            val secondResult = GlobalScope.async(Dispatchers.Unconfined) {
                second.readRemaining().readBytes()
            }
            secondResult.await()
        }
    }

    @Test
    fun testSplitCancelFirstReader() = testSuspend {
        val data = ByteArray(16 * 1024) { it.toByte() }
        val source = ByteChannel()

        val (first, second) = source.split()

        val message = "Expected reason"

        val sourceResult = GlobalScope.async(Dispatchers.Unconfined) {
            source.writeFully(data)
            source.close()
        }

        launch {
            first.cancel(IllegalStateException(message))
        }

        assertFailsWithMessage("Expected reason") {
            sourceResult.await()
        }

        assertFailsWithMessage("Expected reason") {
            val secondResult = GlobalScope.async(Dispatchers.Unconfined) {
                second.readRemaining().readBytes()
            }
            secondResult.await()
        }
    }

    @Test
    fun testSplitCancelSecondReader() = testSuspend {
        val data = ByteArray(16 * 1024) { it.toByte() }
        val source = ByteChannel()

        val (first, second) = source.split()

        val message = "Expected reason"

        val sourceResult = GlobalScope.async(Dispatchers.Unconfined) {
            source.writeFully(data)
            source.close()
        }

        launch {
            second.cancel(IllegalStateException(message))
        }

        assertFailsWithMessage("Expected reason") {
            val firstResult = GlobalScope.async(Dispatchers.Unconfined) {
                first.readRemaining().readBytes()
            }
            firstResult.await()
        }

        assertFailsWithMessage("Expected reason") {
            sourceResult.await()
        }
    }
}

private inline fun assertFailsWithMessage(message: String, block: () -> Unit) {
    var fail = false
    try {
        block()
    } catch (cause: Throwable) {
        assertEquals(message, cause.message)
        fail = true
    }

    assertTrue(fail)
}

private fun assertArrayEquals(expected: ByteArray, actual: ByteArray) {
    assertTrue { expected.contentEquals(actual) }
}
