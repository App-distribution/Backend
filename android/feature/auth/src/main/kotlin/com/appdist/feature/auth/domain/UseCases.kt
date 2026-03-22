package com.appdist.feature.auth.domain

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.Error(AppError.Unknown("Email cannot be blank"))
        return repository.requestOtp(email.trim())
    }
}

class VerifyOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Result<Unit> {
        if (otp.length != 6) return Result.Error(AppError.Unknown("OTP must be 6 digits"))
        return repository.verifyOtp(email, otp)
    }
}
