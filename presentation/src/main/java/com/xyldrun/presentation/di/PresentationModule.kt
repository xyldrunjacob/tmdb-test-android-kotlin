package com.xyldrun.presentation.di

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.domain.usecase.GetMovieDetailsUseCase
import com.xyldrun.domain.usecase.GetPopularMoviesUseCase
import com.xyldrun.domain.usecase.RefreshMoviesUseCase
import com.xyldrun.presentation.moviedetail.MovieDetailViewModel
import com.xyldrun.presentation.movielist.MovieListViewModel
import com.xyldrun.presentation.ui.common.MovieImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    single { ProcessLifecycleOwner.get().lifecycleScope }
    
    single { MovieImageLoader(androidContext()) }

    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    viewModel {
        MovieListViewModel(
            getPopularMovies = get(),
            refreshMovies = get(),
            networkMonitor = get()
        )
    }

    viewModel { parameters ->
        MovieDetailViewModel(
            getMovieDetailsUseCase = get(),
            movieId = parameters.get()
        )
    }
} 