package com.xyldrun.presentation.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xyldrun.data.api.MovieApi
import com.xyldrun.domain.model.Movie
import com.xyldrun.presentation.MainActivity
import com.xyldrun.presentation.search.SearchViewModel
import com.xyldrun.presentation.ui.common.MovieImageLoader
import com.xyldrun.presentation.util.BaseIntegrationTest
import com.xyldrun.presentation.util.ComposeTestExtensions.performSearch
import com.xyldrun.presentation.util.ComposeTestExtensions.verifyEmptyState
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

@RunWith(AndroidJUnit4::class)
class SearchIntegrationTest : BaseIntegrationTest() {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val movieApi = mockk<MovieApi>()
    private val movieImageLoader = mockk<MovieImageLoader>()
    private lateinit var searchViewModel: SearchViewModel
    private val testMovies = TestDataFactory.createTestMovieList(5)

    @Before
    fun setup() {
        stopKoin()

        searchViewModel = SearchViewModel(movieApi)

        val testModule = module {
            single { movieApi }
            single { movieImageLoader }
            viewModel { searchViewModel }
        }

        startKoin {
            modules(testModule)
        }
    }

    @Test
    fun testSearchFlow() {
        val searchQuery = "Test"
        coEvery { movieApi.searchMovies(searchQuery, any()) } returns flowOf(testMovies)

        composeTestRule.performSearch(searchQuery)
        composeTestRule.onNodeWithTag(TestTags.SEARCH_LOADING).assertExists()
        composeTestRule.waitForIdle()

        testMovies.forEach { movie ->
            composeTestRule.onNodeWithText(movie.title).assertExists()
        }
    }

    @Test
    fun testEmptySearchResults() {
        val searchQuery = "NonExistentMovie"
        coEvery { movieApi.searchMovies(searchQuery, any()) } returns flowOf(emptyList())

        composeTestRule.performSearch(searchQuery)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SEARCH_EMPTY_STATE).assertExists()
        composeTestRule.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun testSearchDebounce() {
        val searchQuery = "Test"
        coEvery { movieApi.searchMovies(any(), any()) } returns flowOf(testMovies)

        composeTestRule.onNodeWithTag(TestTags.SEARCH_BUTTON).performClick()

        searchQuery.forEach { char ->
            composeTestRule.onNodeWithTag(TestTags.SEARCH_FIELD)
                .performTextInput(char.toString())
        }

        composeTestRule.waitForIdle()
        verify(exactly = 1) { movieApi.searchMovies(any(), any()) }
    }

    @Test
    fun testSearchHistory() {
        val searchQuery = "Test"
        coEvery { movieApi.searchMovies(searchQuery, any()) } returns flowOf(testMovies)

        composeTestRule.performSearch(searchQuery)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SEARCH_CLEAR_BUTTON).performClick()

        composeTestRule.onNodeWithTag(TestTags.SEARCH_HISTORY).assertExists()
        composeTestRule.onNodeWithText(searchQuery).assertExists()

        composeTestRule.onNodeWithText(searchQuery).performClick()

        verify(exactly = 2) { movieApi.searchMovies(searchQuery, any()) }
    }

    @Test
    fun testSearchFilters() {
        composeTestRule.onNodeWithTag(TestTags.SEARCH_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.SEARCH_FILTERS_BUTTON).performClick()
        composeTestRule.onNodeWithTag("genre_filter_action").performClick()
        composeTestRule.onNodeWithTag(TestTags.APPLY_FILTERS_BUTTON).performClick()

        verify { movieApi.searchMovies(any(), any(), genre = "Action") }
    }

    @Test
    fun testSearchErrorHandling() {
        val searchQuery = "Test"
        coEvery { movieApi.searchMovies(searchQuery, any()) } throws Exception("Network error")

        composeTestRule.performSearch(searchQuery)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SEARCH_ERROR_STATE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.RETRY_BUTTON).assertExists()

        composeTestRule.onNodeWithTag(TestTags.RETRY_BUTTON).performClick()
        verify(exactly = 2) { movieApi.searchMovies(searchQuery, any()) }
    }

    @Test
    fun testSearchSuggestions() {
        composeTestRule.onNodeWithTag(TestTags.SEARCH_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.SEARCH_FIELD)
            .performTextInput("Av")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SEARCH_SUGGESTIONS).assertExists()
        composeTestRule.onNodeWithText("Avengers").assertExists()

        composeTestRule.onNodeWithText("Avengers").performClick()

        verify { movieApi.searchMovies("Avengers", any()) }
    }
} 