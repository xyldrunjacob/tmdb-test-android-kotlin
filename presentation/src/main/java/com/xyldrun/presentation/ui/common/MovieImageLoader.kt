package com.xyldrun.presentation.ui.common

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.util.DebugLogger
import com.xyldrun.domain.model.Movie

class MovieImageLoader(
    private val context: Context
) {
    private val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25) // Use 25% of app memory for image cache
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02) // Use 2% of disk space
                .build()
        }
        .networkCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .logger(DebugLogger())
        .respectCacheHeaders(true)
        .crossfade(CROSSFADE_DURATION)
        .build()

    fun preloadImages(movies: List<Movie>) {
        movies.forEach { movie ->
            movie.posterPath.takeIf { it.isNotBlank() }?.let { path ->
                preloadImage(path, ImageType.POSTER_SMALL)
                preloadImage(path, ImageType.POSTER_LARGE)
            }
            movie.backdropPath.takeIf { it.isNotBlank() }?.let { path ->
                preloadImage(path, ImageType.BACKDROP_SMALL)
                preloadImage(path, ImageType.BACKDROP_LARGE)
            }
        }
    }

    private fun preloadImage(path: String, type: ImageType) {
        val request = buildImageRequest(path, type)
        imageLoader.enqueue(request)
    }

    fun buildImageRequest(path: String?, type: ImageType): ImageRequest {
        return ImageRequest.Builder(context)
            .data(buildImageUrl(path, type))
            .size(type.width, type.height)
            .memoryCacheKey(path)
            .diskCacheKey(path)
            .crossfade(true)
            .build()
    }

    private fun buildImageUrl(path: String?, type: ImageType): String? {
        if (path.isNullOrBlank()) return null
        return "https://image.tmdb.org/t/p/${type.size}$path"
    }

    fun loadPosterImage(posterPath: String): String {
        return "$TMDB_IMAGE_URL$posterPath"
    }

    fun loadBackdropImage(backdropPath: String): String {
        return "$TMDB_IMAGE_URL$backdropPath"
    }

    enum class ImageType(val size: String, val width: Int, val height: Int) {
        POSTER_SMALL("w342", 342, 513),
        POSTER_LARGE("w780", 780, 1170),
        BACKDROP_SMALL("w780", 780, 439),
        BACKDROP_LARGE("w1280", 1280, 720)
    }

    companion object {
        private const val CROSSFADE_DURATION = 300
        private const val TMDB_IMAGE_URL = "https://image.tmdb.org/t/p/original"

        fun getImageUrl(path: String, type: ImageType): String {
            return "https://image.tmdb.org/t/p/${type.size}$path"
        }
    }
} 