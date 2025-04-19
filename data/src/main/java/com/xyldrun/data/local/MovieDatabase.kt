package com.xyldrun.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(entities = [MovieEntity::class], version = 1)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        private const val DATABASE_NAME = "movie_database"

        fun create(context: Context): MovieDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MovieDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY last_updated DESC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies ORDER BY last_updated DESC")
    suspend fun getAllMoviesAsList(): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Int): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Transaction
    suspend fun updateMovies(movies: List<MovieEntity>) {
        clearMovies()
        insertMovies(movies)
    }

    @Query("DELETE FROM movies")
    suspend fun clearMovies()

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun getMovieCount(): Int

    @Query("SELECT * FROM movies WHERE last_updated < :timestamp")
    suspend fun getStaleMovies(timestamp: Long): List<MovieEntity>

    @Delete
    suspend fun deleteMovies(movies: List<MovieEntity>)

    @Transaction
    suspend fun cleanupStaleCache(maxAgeMs: Long) {
        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        val staleMovies = getStaleMovies(cutoffTime)
        if (staleMovies.isNotEmpty()) {
            deleteMovies(staleMovies)
        }
    }
} 