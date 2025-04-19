package com.xyldrun.data.di

import com.xyldrun.data.BuildConfig
import com.xyldrun.data.local.MovieDatabase
import com.xyldrun.data.remote.MovieApi
import com.xyldrun.data.remote.error.MovieApiExceptionMapper
import com.xyldrun.data.repository.MovieRepositoryImpl
import com.xyldrun.domain.repository.MovieRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.xyldrun.data.remote.KtorClient

val dataModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
            encodeDefaults = true
        }
    }

    single { MovieApiExceptionMapper() }

    single { MovieDatabase.create(androidContext()) }

    single { get<MovieDatabase>().movieDao() }

    single { 
        MovieApi(
            client = get(),
            networkMonitor = get(),
            dispatcher = get(),
            exceptionMapper = get()
        )
    }

    single<MovieRepository> {
        MovieRepositoryImpl(
            api = get(),
            dao = get(),
            networkMonitor = get()
        )
    }

    // Provide the existing, fully configured KtorClient.client
    single { KtorClient.client }
} 