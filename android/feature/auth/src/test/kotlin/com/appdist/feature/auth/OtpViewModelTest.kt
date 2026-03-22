package com.appdist.feature.auth

import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.VerifyOtpUseCase
import com.appdist.feature.auth.ui.otp.OtpAction
import com.appdist.feature.auth.ui.otp.OtpEffect
import com.appdist.feature.auth.ui.otp.OtpViewModel
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

class OtpViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val verifyOtp = mockk<VerifyOtpUseCase>()
    private lateinit var vm: OtpViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = OtpViewModel(verifyOtp)
        vm.email = "test@example.com"
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `otp change is capped at 6 chars`() = runTest {
        vm.onAction(OtpAction.OtpChanged("1234567890"))
        assertEquals("123456", vm.state.value.otp)
    }

    @Test
    fun `successful verify emits NavigateToHome effect`() = runTest {
        coEvery { verifyOtp("test@example.com", "123456") } returns Result.Success(Unit)
        vm.onAction(OtpAction.OtpChanged("123456"))

        vm.effects.test {
            vm.onAction(OtpAction.VerifyClicked)
            val effect = awaitItem()
            assertTrue(effect is OtpEffect.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `failed verify sets error and resets isLoading`() = runTest {
        coEvery { verifyOtp(any(), any()) } returns Result.Error(AppError.Network(401, "Invalid OTP"))
        vm.onAction(OtpAction.OtpChanged("000000"))
        vm.onAction(OtpAction.VerifyClicked)
        assertEquals(AppError.Network(401, "Invalid OTP"), vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }
}
