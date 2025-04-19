package com.xyldrun.domain.repository

import com.xyldrun.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getPopularMovies(): Flow<List<Movie>>
    suspend fun refreshPopularMovies(page: Int = 1)
    suspend fun getMovieById(id: Int): Movie?
    suspend fun clearMovies()
} 