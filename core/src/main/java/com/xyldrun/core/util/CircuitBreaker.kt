package com.xyldrun.core.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class CircuitBreaker(
    private val maxFailures: Int = 5,
    private val resetTimeout: Duration = 30.seconds,
    private val halfOpenMaxAttempts: Int = 3
) {
    private val mutex = Mutex()
    private val stateRef = AtomicReference<State>(State.Closed)
    private val failureCount = AtomicInteger(0)
    private val halfOpenAttempts = AtomicInteger(0)
    @Volatile private var lastAttemptTime: Instant = Instant.now()

    sealed class State {
        object Closed : State()
        object Open : State()
        object HalfOpen : State()
    }

    suspend fun <T> execute(block: suspend () -> T): T {
        // Fast path check without lock
        val currentState = stateRef.get()
        if (currentState is State.Closed && failureCount.get() == 0) {
            return try {
                block()
            } catch (e: Exception) {
                handleFailure(e)
            }
        }

        return mutex.withLock {
            when (stateRef.get()) {
                is State.Open -> {
                    if (shouldAttemptReset()) {
                        moveToHalfOpen()
                    } else {
                        throw CircuitBreakerOpenException("Circuit breaker is open")
                    }
                }
                is State.HalfOpen -> {
                    if (halfOpenAttempts.get() >= halfOpenMaxAttempts) {
                        moveToOpen()
                        throw CircuitBreakerOpenException("Circuit breaker is open")
                    }
                    halfOpenAttempts.incrementAndGet()
                }
                is State.Closed -> { /* proceed with execution */ }
            }

            try {
                val result = block()
                onSuccess()
                result
            } catch (e: Exception) {
                onFailure(e)
                throw e
            }
        }
    }

    private fun shouldAttemptReset(): Boolean {
        return Instant.now().isAfter(lastAttemptTime.plusMillis(resetTimeout.inWholeMilliseconds))
    }

    private fun moveToHalfOpen() {
        stateRef.set(State.HalfOpen)
        halfOpenAttempts.set(0)
        lastAttemptTime = Instant.now()
    }

    private fun moveToOpen() {
        stateRef.set(State.Open)
        lastAttemptTime = Instant.now()
    }

    private fun moveToClosed() {
        stateRef.set(State.Closed)
        failureCount.set(0)
        halfOpenAttempts.set(0)
    }

    private fun onSuccess() {
        when (stateRef.get()) {
            is State.HalfOpen -> moveToClosed()
            is State.Closed -> failureCount.set(0)
            is State.Open -> { /* shouldn't happen */ }
        }
    }

    private suspend fun handleFailure(e: Exception): Nothing {
        mutex.withLock {
            onFailure(e)
        }
        throw e
    }

    private fun onFailure(exception: Exception) {
        when (stateRef.get()) {
            is State.Closed -> {
                if (failureCount.incrementAndGet() >= maxFailures) {
                    moveToOpen()
                }
            }
            is State.HalfOpen -> moveToOpen()
            is State.Open -> { /* already open */ }
        }
        throw exception
    }

    // Expose state for monitoring
    fun getCurrentState(): State = stateRef.get()
    fun getFailureCount(): Int = failureCount.get()
    fun getHalfOpenAttempts(): Int = halfOpenAttempts.get()
}

class CircuitBreakerOpenException(message: String) : Exception(message)

// Extension function to create a circuit breaker with default settings
fun circuitBreaker(
    maxFailures: Int = 5,
    resetTimeout: Duration = 30.seconds,
    halfOpenMaxAttempts: Int = 3
) = CircuitBreaker(maxFailures, resetTimeout, halfOpenMaxAttempts) 