package com.appdist.feature.auth.data

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.datastore.TokenManager
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.RequestOtpRequest
import com.appdist.core.network.dto.VerifyOtpRequest
import com.appdist.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = tokenManager.isAuthenticated

    override suspend fun requestOtp(email: String): Result<Unit> = try {
        val response = api.requestOtp(RequestOtpRequest(email))
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(e.toNetworkError())
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<Unit> = try {
        val response = api.verifyOtp(VerifyOtpRequest(email, otp))
        if (response.isSuccessful) {
            val body = response.body()!!
            tokenManager.saveTokens(body.accessToken, body.refreshToken)
            Result.Success(Unit)
        } else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(e.toNetworkError())
    }

    override suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenManager.clear()
    }

    private fun Exception.toNetworkError(): AppError =
        if (this is java.io.IOException) AppError.NoInternet
        else AppError.Unknown(message ?: "Unknown error")
}
