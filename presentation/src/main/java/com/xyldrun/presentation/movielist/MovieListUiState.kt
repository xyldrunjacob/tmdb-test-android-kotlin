package com.xyldrun.presentation.movielist

import com.xyldrun.domain.model.Movie

sealed class MovieListUiState {
    data object Loading : MovieListUiState()
    
    data class Success(
        val movies: List<Movie>,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = true,
        val loadMoreError: String? = null,
        val isShowingCached: Boolean = false,
        val isRefreshing: Boolean = false
    ) : MovieListUiState()
    
    data class Error(
        val message: String,
        val isOffline: Boolean = false,
        val showingCached: Boolean = false
    ) : MovieListUiState()
} 