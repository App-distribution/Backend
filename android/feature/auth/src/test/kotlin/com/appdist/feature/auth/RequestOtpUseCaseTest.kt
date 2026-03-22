package com.appdist.feature.auth

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.AuthRepository
import com.appdist.feature.auth.domain.RequestOtpUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RequestOtpUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = RequestOtpUseCase(repository)

    @Test
    fun `returns Success when repository succeeds`() = runTest {
        coEvery { repository.requestOtp("test@example.com") } returns Result.Success(Unit)
        val result = useCase("test@example.com")
        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `returns Error when email is blank`() = runTest {
        val result = useCase("  ")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Unknown)
    }

    @Test
    fun `propagates repository error`() = runTest {
        val error = AppError.NoInternet
        coEvery { repository.requestOtp(any()) } returns Result.Error(error)
        val result = useCase("test@example.com")
        assertEquals(Result.Error<Unit>(error), result)
    }
}
