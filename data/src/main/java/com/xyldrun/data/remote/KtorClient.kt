package com.xyldrun.data.remote

import android.util.Log
import com.xyldrun.data.BuildConfig
import com.xyldrun.data.remote.error.MovieApiException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorClient {
    private const val TIMEOUT = 60_000L
    private const val HOST = "api.themoviedb.org"
    const val API_VERSION = "3"
    private const val TAG = "KtorClient"

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, message)
                }
            }
            level = LogLevel.ALL
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT
            connectTimeoutMillis = TIMEOUT / 4
            socketTimeoutMillis = TIMEOUT / 4
        }
        
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = HOST
                port = 443
                path(API_VERSION)
            }
            
            Log.d(TAG, "API Key: ${BuildConfig.TMDB_API_KEY}")
            headers {
                append("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkNTU3Njk5OWRmYzQ5NjkyZDkwMjYxNTViNDc1ZTYzOSIsIm5iZiI6MTc0NDEyMDk4Ni41MzEwMDAxLCJzdWIiOiI2N2Y1MmM5YTZjMzU4M2M5NzU5OTdmNjkiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.n-GTA-Zh5J7D8zt3xOcQCmE-p0EVNHso726dJfy6gR4")
                append("Accept", "application/json")
                append("Content-Type", "application/json")
            }
        }
        
        HttpResponseValidator {
            validateResponse { response ->
                Log.d(TAG, "Response status: ${response.status}")
                when (response.status.value) {
                    in 300..399 -> throw RedirectResponseException(response, "Redirect response: ${response.status}")
                    in 400..499 -> {
                        val message = when (response.status.value) {
                            401 -> "Invalid API key. Please check your configuration."
                            404 -> "Resource not found"
                            429 -> "Rate limit exceeded"
                            else -> "Client request error: ${response.status}"
                        }
                        throw ClientRequestException(response, message)
                    }
                    in 500..599 -> throw ServerResponseException(response, "Server error: ${response.status}")
                }
                if (response.status.value >= 600) {
                    throw ResponseException(response, "Unexpected response: ${response.status}")
                }
            }
            handleResponseExceptionWithRequest { cause, request ->
                Log.e(TAG, "Request failed: ${request.url}, error: ${cause.message}")
                throw when (cause) {
                    is ClientRequestException -> MovieApiException.ClientError(cause.message)
                    is ServerResponseException -> MovieApiException.ServerError(cause.message)
                    else -> MovieApiException.UnknownError(cause.message ?: "Unknown error occurred")
                }
            }
        }
    }
} 