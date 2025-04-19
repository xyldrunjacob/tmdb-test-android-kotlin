package com.xyldrun.presentation.movielist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.usecase.GetPopularMoviesUseCase
import com.xyldrun.domain.usecase.RefreshMoviesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class MovieListViewModel(
    private val getPopularMovies: GetPopularMoviesUseCase,
    private val refreshMovies: RefreshMoviesUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieListUiState>(MovieListUiState.Loading)
    val uiState: StateFlow<MovieListUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false
    private var loadJob: Job? = null
    private var isLoadingMore = false
    private var currentMovies: List<Movie> = emptyList()
    private var isRefreshing = false

    init {
        observeNetworkChanges()
        loadMovies(forceRefresh = true)
    }

    private fun observeNetworkChanges() {
        networkMonitor.observeNetworkState()
            .onEach { isConnected ->
                Log.d("MovieListViewModel", "Network state changed: isConnected=$isConnected")
                if (isConnected && _uiState.value is MovieListUiState.Error) {
                    loadMovies(forceRefresh = true)
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadMovies(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                Timber.d("Loading movies: forceRefresh=$forceRefresh, currentPage=$currentPage")
                
                if (forceRefresh) {
                    currentPage = 1
                    isLastPage = false
                    _uiState.update { MovieListUiState.Loading }
                }

                val currentState = _uiState.value
                if (currentState is MovieListUiState.Success) {
                    _uiState.update { 
                        currentState.copy(
                            isLoadingMore = true,
                            isRefreshing = forceRefresh,
                            loadMoreError = null
                        )
                    }
                }

                if (forceRefresh) {
                    Timber.d("Refreshing movies")
                    refreshMovies()
                }

                val movies = getPopularMovies()
                movies.collect { movieList ->
                    Timber.d("Received ${movieList.size} movies")
                    val currentMovies = if (currentState is MovieListUiState.Success && !forceRefresh) {
                        currentState.movies + movieList
                    } else {
                        movieList
                    }

                    isLastPage = movieList.isEmpty()
                    _uiState.update {
                        MovieListUiState.Success(
                            movies = currentMovies,
                            isLoadingMore = false,
                            canLoadMore = !isLastPage,
                            isShowingCached = !networkMonitor.isNetworkAvailable(),
                            isRefreshing = false,
                            loadMoreError = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading movies")
                val currentState = _uiState.value
                if (currentState is MovieListUiState.Success) {
                    _uiState.update {
                        currentState.copy(
                            isLoadingMore = false,
                            isRefreshing = false,
                            loadMoreError = e.message ?: "Unknown error"
                        )
                    }
                } else {
                    val isOffline = !networkMonitor.isNetworkAvailable()
                    val showingCached = currentState is MovieListUiState.Success && currentState.movies.isNotEmpty()
                    _uiState.update { 
                        MovieListUiState.Error(
                            message = e.message ?: "Unknown error",
                            isOffline = isOffline,
                            showingCached = showingCached
                        )
                    }
                }
            }
        }
    }

    fun retryLoadMore() {
        loadMovies(forceRefresh = false)
    }

    fun refresh() {
        loadMovies(forceRefresh = true)
    }
} 