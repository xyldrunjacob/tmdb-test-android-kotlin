package com.xyldrun.domain.usecase

import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.repository.MovieRepository

class GetMovieDetailsUseCase(private val repository: MovieRepository) {
    suspend operator fun invoke(id: Int): Movie? = repository.getMovieById(id)
} 