package com.appdist.domain.repository

import com.appdist.domain.model.OtpCode

interface OtpRepository {
    suspend fun create(email: String, code: String, ttlMinutes: Long): OtpCode
    suspend fun findValid(email: String, code: String): OtpCode?
    suspend fun markUsed(email: String, code: String)
    /** Считает неверную попытку и сжигает активный код, когда их набралось [maxAttempts]. */
    suspend fun registerFailedAttempt(email: String, maxAttempts: Int)
    suspend fun deleteExpired()
}
