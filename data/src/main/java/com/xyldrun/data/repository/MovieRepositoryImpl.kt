package com.xyldrun.data.repository

import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.data.local.MovieDao
import com.xyldrun.data.local.MovieEntity
import com.xyldrun.data.remote.MovieApi
import com.xyldrun.data.remote.dto.MovieDetailsDto
import com.xyldrun.data.remote.dto.MovieDto
import com.xyldrun.data.remote.dto.MovieListResponse
import com.xyldrun.data.remote.error.MovieApiException
import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MovieRepositoryImpl(
    private val api: MovieApi,
    private val dao: MovieDao,
    private val networkMonitor: NetworkMonitor
) : MovieRepository {

    // Cache transformation functions
    private val entityToDomainMapper: (MovieEntity) -> Movie = { entity ->
        Movie(
            id = entity.id,
            title = entity.title,
            overview = entity.overview,
            posterPath = entity.posterPath ?: "",
            backdropPath = entity.backdropPath ?: "",
            releaseDate = entity.releaseDate,
            voteAverage = entity.voteAverage,
            voteCount = entity.voteCount
        )
    }

    private val dtoToEntityMapper: (MovieDto) -> MovieEntity = { dto ->
        MovieEntity(
            id = dto.id,
            title = dto.title,
            overview = dto.overview,
            posterPath = dto.posterPath,
            backdropPath = dto.backdropPath,
            releaseDate = dto.releaseDate,
            voteAverage = dto.voteAverage,
            voteCount = dto.voteCount
        )
    }

    private val detailsDtoToEntityMapper: (MovieDetailsDto) -> MovieEntity = { dto ->
        MovieEntity(
            id = dto.id,
            title = dto.title,
            overview = dto.overview,
            posterPath = dto.posterPath,
            backdropPath = dto.backdropPath,
            releaseDate = dto.releaseDate,
            voteAverage = dto.voteAverage,
            voteCount = dto.voteCount
        )
    }

    override fun getPopularMovies(): Flow<List<Movie>> {
        return dao.getAllMovies()
            .map { entities -> entities.map(entityToDomainMapper) }
    }

    override suspend fun refreshPopularMovies(page: Int) {
        try {
            if (!networkMonitor.isNetworkAvailable()) {
                handleOfflineState()
                return
            }

            val response = api.getPopularMovies(page).first()
            
            if (page == 1) {
                handleFirstPageResponse(response)
            } else {
                handlePaginationResponse(response)
            }
        } catch (e: Exception) {
            when (e) {
                is MovieApiException.ServerError -> handleServerError(page)
                is MovieApiException.NetworkError -> handleOfflineState()
                else -> handleNetworkError(e, page)
            }
        }
    }

    private suspend fun handleServerError(page: Int) {
        val movieCount = dao.getMovieCount()
        if (page == 1 && movieCount > 0) {
            throw MovieApiException.ServerError("Service is temporarily unavailable. Showing cached data.")
        } else {
            throw MovieApiException.ServerError("Service is temporarily unavailable. Please try again later.")
        }
    }

    private suspend fun handleFirstPageResponse(response: MovieListResponse) {
        val existingMovies = dao.getAllMoviesAsList()
        val newMovies = response.results.map(dtoToEntityMapper)
        val updatedMovies = mergeMovieLists(existingMovies, newMovies)
        dao.updateMovies(updatedMovies)
    }

    private suspend fun handlePaginationResponse(response: MovieListResponse) {
        dao.insertMovies(response.results.map(dtoToEntityMapper))
    }

    private suspend fun handleOfflineState() {
        val movieCount = dao.getMovieCount()
        if (movieCount > 0) {
            throw MovieApiException.NetworkError("Network is unavailable")
        } else {
            throw MovieApiException.NoInternetConnection("No internet connection available")
        }
    }

    private suspend fun handleNetworkError(error: Exception, page: Int) {
        val movieCount = dao.getMovieCount()
        if (page == 1 && movieCount > 0) {
            throw MovieApiException.CacheError("Using cached data: ${error.message ?: "Network error"}")
        } else {
            when (error) {
                is MovieApiException -> throw error
                else -> throw MovieApiException.UnknownError("Unknown error occurred: ${error.message ?: "No error message"}")
            }
        }
    }

    private suspend fun mergeMovieLists(
        existing: List<MovieEntity>,
        new: List<MovieEntity>
    ): List<MovieEntity> {
        val existingIds = existing.asSequence().map { it.id }.toSet()
        val updatedExisting = existing.asSequence().map { existingMovie ->
            new.find { it.id == existingMovie.id }?.let { newMovie ->
                existingMovie.copy(
                    title = newMovie.title,
                    overview = newMovie.overview,
                    posterPath = newMovie.posterPath,
                    backdropPath = newMovie.backdropPath,
                    releaseDate = newMovie.releaseDate,
                    voteAverage = newMovie.voteAverage,
                    voteCount = newMovie.voteCount
                )
            } ?: existingMovie
        }
        val newMovies = new.asSequence().filter { it.id !in existingIds }
        return (updatedExisting + newMovies).toList()
    }

    override suspend fun getMovieById(id: Int): Movie? {
        return try {
            api.getMovieDetails(id).first().let(detailsDtoToEntityMapper).let(entityToDomainMapper)
        } catch (e: Exception) {
            dao.getMovieById(id)?.let(entityToDomainMapper)
        }
    }

    override suspend fun clearMovies() {
        try {
            dao.clearMovies()
        } catch (e: Exception) {
            throw MovieApiException.CacheError("Failed to clear movies: ${e.message}")
        }
    }
} 