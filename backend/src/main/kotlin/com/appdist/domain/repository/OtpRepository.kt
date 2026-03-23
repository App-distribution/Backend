package com.appdist.domain.repository

import com.appdist.domain.model.OtpCode

interface OtpRepository {
    suspend fun create(email: String, code: String, ttlMinutes: Long): OtpCode
    suspend fun findValid(email: String, code: String): OtpCode?
    suspend fun markUsed(email: String, code: String)
    suspend fun deleteExpired()
}
