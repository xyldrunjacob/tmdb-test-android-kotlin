package com.xyldrun.domain.model

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String,
    val backdropPath: String,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<Genre> = emptyList(),
    val runtime: Int? = null,
    val tagline: String? = null,
    val status: String = "",
    val videos: List<Video> = emptyList(),
    val similarMovies: List<Movie> = emptyList(),
    val recommendations: List<Movie> = emptyList()
)

data class Genre(
    val id: Int,
    val name: String
)

data class Video(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String
) {
    val thumbnailUrl: String
        get() = when (site.lowercase()) {
            "youtube" -> "https://img.youtube.com/vi/$key/hqdefault.jpg"
            else -> ""
        }
        
    val videoUrl: String
        get() = when (site.lowercase()) {
            "youtube" -> "https://www.youtube.com/watch?v=$key"
            "vimeo" -> "https://vimeo.com/$key"
            else -> ""
        }
} 