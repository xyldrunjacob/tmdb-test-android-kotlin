package com.xyldrun.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xyldrun.presentation.moviedetail.MovieDetailScreen
import com.xyldrun.presentation.movielist.MovieListRoute
import com.xyldrun.presentation.theme.MovieAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovieAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MovieAppNavigation()
                }
            }
        }
    }
}

@Composable
fun MovieAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "movieList"
    ) {
        composable("movieList") {
            MovieListRoute(
                onMovieClick = { movieId ->
                    navController.navigate("movieDetail/$movieId")
                }
            )
        }
        composable(
            route = "movieDetail/{movieId}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            MovieDetailScreen(
                movieId = backStackEntry.arguments?.getInt("movieId") ?: 0,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}