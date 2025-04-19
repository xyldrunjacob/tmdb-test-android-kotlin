package com.xyldrun.core.util

import io.ktor.client.plugins.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed class NetworkError : Exception() {
    data class HttpError(val code: Int, override val message: String? = null) : NetworkError()
    data class NetworkTimeout(override val message: String? = null) : NetworkError()
    data class ServerError(override val message: String? = null) : NetworkError()
    data class ConnectionError(override val message: String? = null) : NetworkError()
    data class UnknownError(override val cause: Throwable? = null) : NetworkError()

    companion object {
        fun fromException(e: Throwable): NetworkError = when (e) {
            is ResponseException -> HttpError(e.response.status.value, e.message)
            is SocketTimeoutException -> NetworkTimeout("Socket timed out")
            is TimeoutCancellationException -> NetworkTimeout("Request timed out")
            is ClientRequestException -> {
                when (e.response.status.value) {
                    in 400..499 -> HttpError(e.response.status.value, e.message)
                    in 500..599 -> ServerError(e.message)
                    else -> UnknownError(e)
                }
            }
            is ServerResponseException -> ServerError(e.message)
            is IOException -> ConnectionError("Network connection error")
            else -> UnknownError(e)
        }
    }
}

object NetworkResilience {
    private val DEFAULT_TIMEOUT = 30.seconds
    private val DEFAULT_INITIAL_DELAY = 1.seconds
    private val DEFAULT_MAX_DELAY = 20.seconds
    private const val DEFAULT_MAX_RETRIES = 3
    private const val DEFAULT_JITTER_FACTOR = 0.1
    private const val MAX_CIRCUIT_BREAKERS = 100
    private val INITIAL_BACKOFF = 1.seconds
    private val MAX_BACKOFF = 15.seconds
    private const val BACKOFF_MULTIPLIER = 1.5

    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()
    private val cleanupMutex = Mutex()
    private val random = Random(System.nanoTime())

    val defaultCircuitBreaker = CircuitBreaker()
    val criticalCircuitBreaker = CircuitBreaker(
        maxFailures = 3,
        resetTimeout = 60.seconds,
        halfOpenMaxAttempts = 2
    )

    suspend fun <T> executeWithRetry(
        maxAttempts: Int = DEFAULT_MAX_RETRIES,
        initialDelay: Duration = DEFAULT_INITIAL_DELAY,
        maxDelay: Duration = DEFAULT_MAX_DELAY,
        shouldRetry: (Exception) -> Boolean = { it !is CancellationException },
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var attempt = 0

        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempt++
                if (attempt >= maxAttempts || !shouldRetry(e)) {
                    throw NetworkError.fromException(e)
                }

                val jitter = (random.nextDouble() * 2 - 1) * DEFAULT_JITTER_FACTOR * currentDelay.inWholeMilliseconds
                val delayWithJitter = (currentDelay.inWholeMilliseconds + jitter)
                    .toLong()
                    .coerceIn(0, maxDelay.inWholeMilliseconds)
                delay(delayWithJitter.milliseconds)
                
                currentDelay = (currentDelay.inWholeMilliseconds * BACKOFF_MULTIPLIER)
                    .toLong()
                    .coerceAtMost(maxDelay.inWholeMilliseconds)
                    .milliseconds
            }
        }
    }

    suspend fun <T> withTimeoutAndFallback(
        timeout: Duration = DEFAULT_TIMEOUT,
        fallback: (suspend () -> T)? = null,
        block: suspend () -> T
    ): T {
        return try {
            withTimeout(timeout) {
                block()
            }
        } catch (e: TimeoutCancellationException) {
            fallback?.invoke() ?: throw NetworkError.NetworkTimeout("Operation timed out after ${timeout.inWholeSeconds} seconds")
        }
    }

    fun getCircuitBreaker(endpoint: String, isCritical: Boolean = false): CircuitBreaker {
        if (circuitBreakers.size >= MAX_CIRCUIT_BREAKERS) {
            // Clean up synchronously if needed
            circuitBreakers.entries.removeIf { (_, breaker) ->
                breaker.getCurrentState() == CircuitBreaker.State.Closed && 
                breaker.getFailureCount() == 0
            }
        }
        return circuitBreakers.computeIfAbsent(endpoint) { _ ->
            if (isCritical) criticalCircuitBreaker else defaultCircuitBreaker
        }
    }
} 