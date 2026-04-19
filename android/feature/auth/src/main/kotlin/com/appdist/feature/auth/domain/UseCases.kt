package com.appdist.feature.auth.domain

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank()) return Result.Error(AppError.Unknown("Email cannot be blank"))
        if (password.isBlank()) return Result.Error(AppError.Unknown("Password cannot be blank"))
        return repository.login(email.trim(), password)
    }
}
