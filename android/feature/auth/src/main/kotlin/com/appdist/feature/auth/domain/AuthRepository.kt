package com.appdist.feature.auth.domain

import com.appdist.core.common.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    val isAuthenticated: Flow<Boolean>
}
