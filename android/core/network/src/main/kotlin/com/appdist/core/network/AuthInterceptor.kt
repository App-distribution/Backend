package com.appdist.core.network

import com.appdist.core.datastore.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenManager.getAccessToken() }

        val request = chain.request().newBuilder()
            .apply { if (accessToken != null) header("Authorization", "Bearer $accessToken") }
            .build()

        val response = chain.proceed(request)

        if (response.code != 401) return response

        response.close()

        // Attempt token refresh
        val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return response

        val refreshRequest = Request.Builder()
            .url(buildRefreshUrl(chain.request().url.toString()))
            .post("""{"refresh_token":"$refreshToken"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val refreshResponse = try {
            chain.proceed(refreshRequest)
        } catch (e: Exception) {
            return response
        }

        if (refreshResponse.code == 200) {
            val body = refreshResponse.body?.string()
            refreshResponse.close()
            val newAccessToken = body?.let { parseAccessToken(it) }
            if (newAccessToken != null) {
                runBlocking { tokenManager.updateAccessToken(newAccessToken) }
                return chain.proceed(
                    request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                )
            }
        }

        refreshResponse.close()
        return response
    }

    private fun buildRefreshUrl(originalUrl: String): String {
        val base = originalUrl.substringBefore("api/v1")
        return "${base}api/v1/auth/refresh"
    }

    private fun parseAccessToken(json: String): String? = try {
        org.json.JSONObject(json).getString("access_token")
    } catch (e: Exception) {
        null
    }
}
