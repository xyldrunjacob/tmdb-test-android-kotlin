package com.xyldrun.presentation.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xyldrun.data.api.MovieApi
import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.model.MovieDetails
import com.xyldrun.domain.model.Video
import com.xyldrun.presentation.MainActivity
import com.xyldrun.presentation.moviedetails.MovieDetailsViewModel
import com.xyldrun.presentation.ui.common.MovieImageLoader
import com.xyldrun.presentation.util.BaseIntegrationTest
import com.xyldrun.presentation.util.ComposeTestExtensions.verifyErrorState
import com.xyldrun.presentation.util.TestDataFactory
import com.xyldrun.presentation.util.TestTags
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
class MovieDetailsIntegrationTest : BaseIntegrationTest() {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val movieApi = mockk<MovieApi>()
    private val movieImageLoader = mockk<MovieImageLoader>()
    private lateinit var movieDetailsViewModel: MovieDetailsViewModel
    private val testMovie = TestDataFactory.createTestMovie()
    private val testMovieDetails = TestDataFactory.createTestMovieDetails()

    @Before
    fun setup() {
        stopKoin()

        movieDetailsViewModel = MovieDetailsViewModel(movieApi)

        val testModule = module {
            single { movieApi }
            single { movieImageLoader }
            viewModel { movieDetailsViewModel }
        }

        startKoin {
            modules(testModule)
        }
    }

    @Test
    fun testDetailsLoadingAndDisplay() {
        coEvery { movieApi.getMovieDetails(1) } returns flowOf(testMovieDetails)

        movieDetailsViewModel.loadMovieDetails(1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.DETAILS_LOADING).assertExists()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(testMovieDetails.title).assertExists()
        composeTestRule.onNodeWithText(testMovieDetails.overview).assertExists()
        composeTestRule.onNodeWithText("${testMovieDetails.runtime} min").assertExists()

        testMovieDetails.genres.forEach { genre ->
            composeTestRule.onNodeWithText(genre).assertExists()
        }
    }

    @Test
    fun testVideoPlayback() {
        coEvery { movieApi.getMovieDetails(1) } returns flowOf(testMovieDetails)

        movieDetailsViewModel.loadMovieDetails(1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.VIDEOS_SECTION).assertExists()
        composeTestRule.onNodeWithText("Trailer 1").performClick()

        composeTestRule.onNodeWithTag(TestTags.VIDEO_PLAYER).assertExists()
        composeTestRule.onNodeWithTag(TestTags.VIDEO_PLAYER_CONTROLS).assertExists()
    }

    @Test
    fun testSimilarMoviesNavigation() {
        coEvery { movieApi.getMovieDetails(1) } returns flowOf(testMovieDetails)

        movieDetailsViewModel.loadMovieDetails(1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SIMILAR_MOVIES_SECTION).assertExists()
        composeTestRule.onNodeWithText(testMovie.copy(id = 2).title).performClick()

        verify { movieDetailsViewModel.loadMovieDetails(2) }
    }

    @Test
    fun testErrorHandling() {
        coEvery { movieApi.getMovieDetails(1) } throws Exception("Network error")

        movieDetailsViewModel.loadMovieDetails(1)
        composeTestRule.waitForIdle()

        composeTestRule.verifyErrorState()

        composeTestRule.onNodeWithTag(TestTags.RETRY_BUTTON).performClick()
        verify { movieDetailsViewModel.loadMovieDetails(1) }
    }

    @Test
    fun testShareFunctionality() {
        coEvery { movieApi.getMovieDetails(1) } returns flowOf(testMovieDetails)

        movieDetailsViewModel.loadMovieDetails(1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SHARE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.SHARE_SHEET).assertExists()
    }

    @Test
    fun testBookmarkFunctionality() {
        coEvery { movieApi.getMovieDetails(1) } returns flowOf(testMovieDetails)

        movieDetailsViewModel.loadMovieDetails(1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.BOOKMARK_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.BOOKMARK_BUTTON_SELECTED).assertExists()
    }
} 