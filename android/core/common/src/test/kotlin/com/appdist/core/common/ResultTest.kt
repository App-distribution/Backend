package com.appdist.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {
    @Test
    fun `Success holds value`() {
        val result: Result<Int> = Result.Success(42)
        assertTrue(result is Result.Success)
        assertEquals(42, (result as Result.Success).data)
    }

    @Test
    fun `Error holds AppError`() {
        val error = AppError.NoInternet
        val result: Result<Int> = Result.Error(error)
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }

    @Test
    fun `getOrNull returns value on Success`() {
        assertEquals(42, Result.Success(42).getOrNull())
    }

    @Test
    fun `getOrNull returns null on Error`() {
        assertNull(Result.Error<Int>(AppError.NoInternet).getOrNull())
    }

    @Test
    fun `map transforms Success value`() {
        val result = Result.Success(2).map { it * 3 }
        assertEquals(Result.Success(6), result)
    }

    @Test
    fun `map preserves Error`() {
        val error = AppError.NoInternet
        val result = Result.Error<Int>(error).map { it * 3 }
        assertEquals(Result.Error<Int>(error), result)
    }

    private fun <T> assertNull(value: T?) = assertTrue(value == null)
}
