package com.xyldrun.presentation.movielist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xyldrun.domain.model.Movie
import com.xyldrun.presentation.R
import com.xyldrun.presentation.ui.common.EmptyStateView
import com.xyldrun.presentation.ui.common.LoadingAnimation
import com.xyldrun.presentation.ui.common.LoadingScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun MovieListRoute(
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovieListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MovieListScreen(
        uiState = uiState,
        onMovieClick = onMovieClick,
        onRetry = viewModel::refresh,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    uiState: MovieListUiState,
    onMovieClick: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRefreshing = when (uiState) {
        is MovieListUiState.Success -> uiState.isRefreshing
        else -> false
    }
    
    val state = rememberPullToRefreshState()
    
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRetry()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (uiState) {
            is MovieListUiState.Loading -> {
                LoadingScreen()
            }
            is MovieListUiState.Success -> {
                if (uiState.movies.isEmpty()) {
                    EmptyStateView(
                        message = stringResource(R.string.no_movies_found),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MovieList(
                        movies = uiState.movies,
                        isLoadingMore = uiState.isLoadingMore,
                        onLoadMore = onRetry,
                        onMovieClick = onMovieClick
                    )
                }
            }
            is MovieListUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    isOffline = uiState.isOffline
                )
            }
        }

        PullToRefreshContainer(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("refresh_container")
        )
    }
}

@Composable
private fun MovieList(
    movies: List<Movie>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(listState) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - 2)
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }

    if (movies.isEmpty()) {
        EmptyStateView(
            message = "No movies found",
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = movies,
                key = { it.id }
            ) { movie ->
                MovieItem(
                    movie = movie,
                    onClick = { onMovieClick(movie.id) }
                )
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingAnimation()
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieItem(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.overview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isOffline) "No internet connection" else message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}