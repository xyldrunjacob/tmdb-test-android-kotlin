package com.xyldrun.domain.usecase

import com.xyldrun.domain.repository.MovieRepository

class RefreshMoviesUseCase(private val repository: MovieRepository) {
    suspend operator fun invoke() = repository.refreshPopularMovies()
} 