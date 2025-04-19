package com.xyldrun.core.util

sealed class MovieError {
    data class NetworkError(val message: String) : MovieError()
    data class DatabaseError(val message: String) : MovieError()
    data class UnknownError(val message: String) : MovieError()
    
    val errorMessage: String
        get() = when (this) {
            is NetworkError -> message
            is DatabaseError -> message
            is UnknownError -> message
        }
} 