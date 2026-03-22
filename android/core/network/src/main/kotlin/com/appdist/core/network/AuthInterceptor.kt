package com.appdist.core.network

import com.appdist.core.datastore.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val json = Json { ignoreUnknownKeys = true }

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenManager.getAccessToken() }

        val request = chain.request().newBuilder()
            .apply { if (accessToken != null) header("Authorization", "Bearer $accessToken") }
            .build()

        val response = chain.proceed(request)

        if (response.code != 401) return response

        response.close()

        return synchronized(this) {
            // Check if another thread already refreshed the token while we were waiting
            val currentToken = runBlocking { tokenManager.getAccessToken() }
            if (currentToken != null && currentToken != accessToken) {
                // Token was already refreshed — retry with new token
                return@synchronized chain.proceed(
                    request.newBuilder().header("Authorization", "Bearer $currentToken").build()
                )
            }

            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
                ?: return@synchronized buildUnauthorizedResponse(request)

            val refreshUrl = buildRefreshUrl(chain.request().url.toString())
            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post("""{"refresh_token":"$refreshToken"}""".toRequestBody("application/json".toMediaType()))
                .build()

            val refreshResponse = try {
                chain.proceed(refreshRequest)
            } catch (e: Exception) {
                return@synchronized buildUnauthorizedResponse(request)
            }

            if (refreshResponse.code == 200) {
                val body = refreshResponse.body?.string()
                refreshResponse.close()
                val newAccessToken = body?.let { parseTokenResponse(it) }
                if (newAccessToken != null) {
                    runBlocking { tokenManager.updateAccessToken(newAccessToken) }
                    return@synchronized chain.proceed(
                        request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    )
                }
            }

            refreshResponse.close()
            buildUnauthorizedResponse(request)
        }
    }

    private fun buildRefreshUrl(originalUrl: String): String {
        val url = originalUrl.toHttpUrl()
        return "${url.scheme}://${url.host}${if (url.port != -1) ":${url.port}" else ""}/api/v1/auth/refresh"
    }

    private fun parseTokenResponse(jsonString: String): String? = try {
        json.decodeFromString<com.appdist.core.network.dto.TokenResponse>(jsonString).accessToken
    } catch (e: Exception) {
        null
    }

    private fun buildUnauthorizedResponse(request: Request): Response =
        Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()
}
