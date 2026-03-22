package com.appdist.feature.auth

import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.RequestOtpUseCase
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val requestOtp = mockk<RequestOtpUseCase>()
    private lateinit var vm: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = LoginViewModel(requestOtp)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `email change updates state`() = runTest {
        vm.onAction(LoginAction.EmailChanged("test@example.com"))
        assertEquals("test@example.com", vm.state.value.email)
    }

    @Test
    fun `successful submit emits NavigateToOtp effect`() = runTest {
        coEvery { requestOtp("test@example.com") } returns Result.Success(Unit)
        vm.onAction(LoginAction.EmailChanged("test@example.com"))

        vm.effects.test {
            vm.onAction(LoginAction.SubmitClicked)
            val effect = awaitItem()
            assertTrue(effect is LoginEffect.NavigateToOtp)
            assertEquals("test@example.com", (effect as LoginEffect.NavigateToOtp).email)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `failed submit sets error in state`() = runTest {
        coEvery { requestOtp(any()) } returns Result.Error(AppError.NoInternet)
        vm.onAction(LoginAction.EmailChanged("test@example.com"))
        vm.onAction(LoginAction.SubmitClicked)
        assertEquals(AppError.NoInternet, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }
}
