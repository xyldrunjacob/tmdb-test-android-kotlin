package com.xyldrun.data.remote.error

sealed class MovieApiException : Exception() {
    object Unauthorized : MovieApiException()
    object NotFound : MovieApiException()
    object RateLimitExceeded : MovieApiException()
    data class ServerError(override val message: String) : MovieApiException()
    data class ClientError(override val message: String) : MovieApiException()
    data class UnknownError(override val message: String) : MovieApiException()
    data class NetworkError(override val message: String) : MovieApiException()
    data class NoInternetConnection(override val message: String) : MovieApiException()
    data class CacheError(override val message: String) : MovieApiException()
}