package com.appdist.feature.auth.domain

import com.appdist.core.common.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun requestOtp(email: String): Result<Unit>
    suspend fun verifyOtp(email: String, otp: String): Result<Unit>
    suspend fun logout()
    val isAuthenticated: Flow<Boolean>
}
