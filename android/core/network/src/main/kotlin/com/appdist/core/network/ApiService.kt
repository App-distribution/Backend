package com.appdist.core.network

import com.appdist.core.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/v1/auth/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): Response<Unit>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<TokenResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    // Projects
    @GET("api/v1/workspaces/{workspaceId}/projects")
    suspend fun getProjects(@Path("workspaceId") workspaceId: String): Response<List<ProjectResponse>>

    @GET("api/v1/projects/{id}")
    suspend fun getProject(@Path("id") id: String): Response<ProjectResponse>

    // Builds
    @GET("api/v1/projects/{projectId}/builds")
    suspend fun getBuilds(
        @Path("projectId") projectId: String,
        @Query("channel") channel: String? = null,
        @Query("env") env: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<List<BuildResponse>>

    @GET("api/v1/builds/{id}")
    suspend fun getBuild(@Path("id") id: String): Response<BuildResponse>

    @GET("api/v1/builds/{id}/download-url")
    suspend fun getDownloadUrl(@Path("id") id: String): Response<DownloadUrlResponse>

    @GET("api/v1/builds/recent")
    suspend fun getRecentBuilds(@Query("limit") limit: Int = 20): Response<List<BuildResponse>>

    // Events
    @POST("api/v1/builds/{id}/install-event")
    suspend fun reportInstall(
        @Path("id") id: String,
        @Body request: InstallEventRequest
    ): Response<Unit>

    @POST("api/v1/builds/{id}/download-event")
    suspend fun reportDownload(@Path("id") id: String): Response<Unit>

    // User
    @GET("api/v1/users/me")
    suspend fun getMe(): Response<UserResponse>

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): Response<UserResponse>
}
