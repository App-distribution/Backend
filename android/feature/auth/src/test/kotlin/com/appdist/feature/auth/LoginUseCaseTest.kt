package com.appdist.feature.auth

import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.AuthRepository
import com.appdist.feature.auth.domain.LoginUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginUseCaseTest {
    private val repository: AuthRepository = mockk()
    private val useCase = LoginUseCase(repository)

    @Test
    fun `blank email returns error without calling repository`() = runTest {
        val result = useCase("", "password123")
        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `blank password returns error without calling repository`() = runTest {
        val result = useCase("user@example.com", "")
        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `valid inputs delegate to repository`() = runTest {
        coEvery { repository.login("user@example.com", "secret") } returns Result.Success(Unit)
        val result = useCase("user@example.com", "secret")
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.login("user@example.com", "secret") }
    }
}
