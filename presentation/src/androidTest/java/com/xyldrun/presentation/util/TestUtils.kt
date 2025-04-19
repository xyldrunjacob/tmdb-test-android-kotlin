package com.xyldrun.presentation.util

import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.model.MovieDetails
import com.xyldrun.domain.model.Video

/**
 * Test data factory for creating test models
 */
object TestDataFactory {
    fun createTestMovie(
        id: Int = 1,
        title: String = "Test Movie $id",
        overview: String = "Overview $id",
        posterPath: String = "/poster$id.jpg",
        backdropPath: String = "/backdrop$id.jpg",
        releaseDate: String = "2024-01-0$id",
        voteAverage: Float = 4.5f + id
    ) = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage
    )

    fun createTestMovieList(count: Int) = List(count) { index ->
        createTestMovie(id = index)
    }

    fun createTestMovieDetails(
        id: Int = 1,
        movie: Movie = createTestMovie(id),
        runtime: Int = 120,
        genres: List<String> = listOf("Action", "Drama"),
        videos: List<Video> = listOf(createTestVideo()),
        similarMovies: List<Movie> = listOf(createTestMovie(id + 1)),
        recommendations: List<Movie> = listOf(createTestMovie(id + 2))
    ) = MovieDetails(
        id = movie.id,
        title = movie.title,
        overview = movie.overview,
        posterPath = movie.posterPath,
        backdropPath = movie.backdropPath,
        releaseDate = movie.releaseDate,
        voteAverage = movie.voteAverage,
        runtime = runtime,
        genres = genres,
        videos = videos,
        similarMovies = similarMovies,
        recommendations = recommendations
    )

    private fun createTestVideo(
        id: String = "1",
        key: String = "videoKey1",
        name: String = "Trailer 1",
        type: String = "Trailer"
    ) = Video(
        id = id,
        key = key,
        name = name,
        type = type
    )
}

/**
 * Common UI test tags
 */
object TestTags {
    // Loading states
    const val LOADING = "loading"
    const val SHIMMER_LOADING = "shimmer_loading"
    const val LOADING_MORE = "loading_more_indicator"
    const val DETAILS_LOADING = "details_loading"
    const val SEARCH_LOADING = "search_loading"

    // Content sections
    const val MOVIE_GRID = "movie_grid"
    const val VIDEOS_SECTION = "videos_section"
    const val SIMILAR_MOVIES_SECTION = "similar_movies_section"
    const val VIDEO_PLAYER = "video_player"
    const val VIDEO_PLAYER_CONTROLS = "video_player_controls"

    // Error states
    const val ERROR_STATE = "error_state"
    const val RETRY_BUTTON = "retry_button"
    const val SEARCH_ERROR_STATE = "search_error_state"

    // Empty states
    const val EMPTY_STATE = "empty_state"
    const val EMPTY_STATE_ANIMATION = "empty_state_animation"
    const val SEARCH_EMPTY_STATE = "search_empty_state"

    // Search
    const val SEARCH_BUTTON = "search_button"
    const val SEARCH_FIELD = "search_field"
    const val SEARCH_CLEAR_BUTTON = "search_clear_button"
    const val SEARCH_HISTORY = "search_history"
    const val SEARCH_FILTERS_BUTTON = "search_filters_button"
    const val SEARCH_SUGGESTIONS = "search_suggestions"
    const val APPLY_FILTERS_BUTTON = "apply_filters_button"

    // Actions
    const val SHARE_BUTTON = "share_button"
    const val SHARE_SHEET = "share_sheet"
    const val BOOKMARK_BUTTON = "bookmark_button"
    const val BOOKMARK_BUTTON_SELECTED = "bookmark_button_selected"
}

/**
 * Base class for integration tests with common setup
 */
abstract class BaseIntegrationTest {
    protected val movieApi = mockk<MovieApi>()
    protected val movieImageLoader = mockk<MovieImageLoader>()

    @Before
    fun baseSetup() {
        stopKoin()
        setupDependencyInjection()
    }

    abstract fun setupDependencyInjection()

    protected fun setupTestModule(vararg additionalComponents: Pair<KClass<*>, Any>) {
        val baseModule = module {
            single { movieApi }
            single { movieImageLoader }
        }

        val testModule = module {
            includes(baseModule)
            additionalComponents.forEach { (kClass, instance) ->
                single(kClass) { instance }
            }
        }

        startKoin {
            modules(testModule)
        }
    }
}

/**
 * Extensions for compose UI testing
 */
object ComposeTestExtensions {
    fun ComposeTestRule.waitForLoading() {
        waitForIdle()
        onNodeWithTag(TestTags.LOADING).assertExists()
        waitForIdle()
    }

    fun ComposeTestRule.performSearch(query: String) {
        onNodeWithTag(TestTags.SEARCH_BUTTON).performClick()
        onNodeWithTag(TestTags.SEARCH_FIELD).performTextInput(query)
        waitForIdle()
    }

    fun ComposeTestRule.verifyErrorState() {
        onNodeWithTag(TestTags.ERROR_STATE).assertExists()
        onNodeWithTag(TestTags.RETRY_BUTTON).assertExists()
    }

    fun ComposeTestRule.verifyEmptyState() {
        onNodeWithTag(TestTags.EMPTY_STATE).assertExists()
        onNodeWithTag(TestTags.EMPTY_STATE_ANIMATION).assertExists()
    }
}

object SemanticMatchers {
    fun hasLoadingIndicator() = hasTestTag("loading_indicator")
    fun hasErrorState() = hasTestTag("error_state")
    fun hasEmptyState() = hasTestTag("empty_state")
    fun hasMovieGrid() = hasTestTag("movie_grid")
    fun hasShimmerEffect() = hasTestTag("shimmer_loading")
}

fun ComposeTestRule.assertLoadingState() {
    onNode(SemanticMatchers.hasLoadingIndicator()).assertExists()
    onNode(SemanticMatchers.hasMovieGrid()).assertDoesNotExist()
}

fun ComposeTestRule.assertErrorState(errorMessage: String) {
    onNode(SemanticMatchers.hasErrorState()).assertExists()
    onNodeWithText(errorMessage).assertExists()
    onNodeWithText("Retry").assertExists()
}

fun ComposeTestRule.assertEmptyState() {
    onNode(SemanticMatchers.hasEmptyState()).assertExists()
    onNodeWithText("No movies found").assertExists()
}

fun ComposeTestRule.assertMovieList(movies: List<Movie>) {
    onNode(SemanticMatchers.hasMovieGrid()).assertExists()
    movies.forEach { movie ->
        onNodeWithText(movie.title).assertExists()
    }
} 