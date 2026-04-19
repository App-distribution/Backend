package com.appdist.feature.auth.data

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.datastore.TokenManager
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.LoginRequest
import com.appdist.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = tokenManager.isAuthenticated

    override suspend fun login(email: String, password: String): Result<Unit> = try {
        val response = api.login(LoginRequest(email, password))
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Result.Error(AppError.Network(response.code(), "Empty response body"))
            tokenManager.saveTokens(body.accessToken, body.refreshToken)
            Result.Success(Unit)
        } else {
            Result.Error(AppError.Network(response.code(), response.message()))
        }
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
