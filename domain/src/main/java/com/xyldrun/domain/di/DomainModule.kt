package com.xyldrun.domain.di

import com.xyldrun.domain.usecase.GetMovieDetailsUseCase
import com.xyldrun.domain.usecase.GetPopularMoviesUseCase
import com.xyldrun.domain.usecase.RefreshMoviesUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetPopularMoviesUseCase(repository = get()) }
    factory { GetMovieDetailsUseCase(repository = get()) }
    factory { RefreshMoviesUseCase(repository = get()) }
} 