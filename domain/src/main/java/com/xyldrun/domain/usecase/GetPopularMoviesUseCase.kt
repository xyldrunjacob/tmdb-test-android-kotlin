package com.xyldrun.domain.usecase

import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class GetPopularMoviesUseCase(private val repository: MovieRepository) {
    operator fun invoke(): Flow<List<Movie>> = repository.getPopularMovies()
} 