package com.appdist.feature.auth

import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.LoginUseCase
import com.appdist.feature.auth.ui.login.LoginAction
import com.appdist.feature.auth.ui.login.LoginEffect
import com.appdist.feature.auth.ui.login.LoginViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val loginUseCase = mockk<LoginUseCase>()
    private lateinit var vm: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = LoginViewModel(loginUseCase)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `email change updates state`() = runTest {
        vm.onAction(LoginAction.EmailChanged("test@example.com"))
        assertEquals("test@example.com", vm.state.value.email)
    }

    @Test
    fun `password change updates state`() = runTest {
        vm.onAction(LoginAction.PasswordChanged("secret123"))
        assertEquals("secret123", vm.state.value.password)
    }

    @Test
    fun `submit with empty email shows error`() = runTest {
        coEvery { loginUseCase("", any()) } returns Result.Error(AppError.Unknown("Email cannot be blank"))
        vm.onAction(LoginAction.PasswordChanged("password123"))
        vm.onAction(LoginAction.Submit)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `successful submit emits NavigateToHome effect`() = runTest {
        coEvery { loginUseCase("test@example.com", any()) } returns Result.Success(Unit)
        vm.onAction(LoginAction.EmailChanged("test@example.com"))
        vm.onAction(LoginAction.PasswordChanged("password123"))

        vm.effects.test {
            vm.onAction(LoginAction.Submit)
            val effect = awaitItem()
            assertTrue(effect is LoginEffect.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `failed submit sets error in state`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.Error(AppError.NoInternet)
        vm.onAction(LoginAction.EmailChanged("test@example.com"))
        vm.onAction(LoginAction.PasswordChanged("password123"))
        vm.onAction(LoginAction.Submit)
        assertEquals(AppError.NoInternet, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }
}
