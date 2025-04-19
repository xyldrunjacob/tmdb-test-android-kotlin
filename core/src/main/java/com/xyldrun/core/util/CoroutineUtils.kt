package com.xyldrun.core.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

object CoroutineUtils {
    private val DEFAULT_TIMEOUT = 10000.milliseconds

    inline fun CoroutineScope.safeLaunch(
        context: CoroutineContext = EmptyCoroutineContext,
        crossinline onError: (Throwable) -> Unit = { },
        crossinline block: suspend CoroutineScope.() -> Unit
    ): Job {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            onError(throwable)
        }
        return this.launch(context + exceptionHandler) {
            block()
        }
    }

    fun <T> Flow<T>.handleErrors(onError: (Throwable) -> Unit): Flow<T> =
        catch { throwable ->
            onError(throwable)
            throw throwable
        }

    suspend fun <T> safeApiCall(
        timeout: Duration = DEFAULT_TIMEOUT,
        retryPolicy: RetryPolicy = RetryPolicy(),
        block: suspend () -> T,
        onError: (Throwable) -> NetworkError = { 
            when (it) {
                is java.net.UnknownHostException -> NetworkError.ConnectionError("No internet connection")
                is kotlinx.coroutines.TimeoutCancellationException -> NetworkError.NetworkTimeout("Request timed out")
                else -> NetworkError.UnknownError(it)
            }
        }
    ): Result<T> = try {
        val result = NetworkResilience.withTimeoutAndFallback(timeout) {
            if (retryPolicy.enabled) {
                NetworkResilience.executeWithRetry(
                    maxAttempts = retryPolicy.maxRetries,
                    shouldRetry = { e -> retryPolicy.shouldRetry(e) },
                    block = block
                )
            } else {
                block()
            }
        }
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(onError(e))
    }
}

data class RetryPolicy(
    val enabled: Boolean = true,
    val maxRetries: Int = 3,
    val shouldRetry: (Throwable) -> Boolean = { e -> 
        e is java.io.IOException || e is NetworkError.ServerError
    }
)

class LifecycleAwareScope(
    private val lifecycleOwner: LifecycleOwner,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private var scope: CoroutineScope? = null

    init {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                scope = CoroutineScope(SupervisorJob() + dispatcher)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                scope?.coroutineContext?.get(Job)?.cancel()
                scope = null
            }
        })
    }

    fun getScope(): CoroutineScope = scope ?: throw IllegalStateException(
        "Scope accessed before onCreate or after onDestroy"
    )
} 