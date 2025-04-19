package com.xyldrun.data.remote.error

import io.ktor.client.plugins.*
import io.ktor.http.*

class MovieApiExceptionMapper {
    fun map(error: ResponseException): MovieApiException {
        return when (error.response.status) {
            HttpStatusCode.Unauthorized -> MovieApiException.Unauthorized
            HttpStatusCode.NotFound -> MovieApiException.NotFound
            HttpStatusCode.TooManyRequests -> MovieApiException.RateLimitExceeded
            in HttpStatusCode.InternalServerError..HttpStatusCode.GatewayTimeout -> 
                MovieApiException.ServerError(error.message ?: "Server error")
            else -> MovieApiException.UnknownError(error.message ?: "Unknown error")
        }
    }
}