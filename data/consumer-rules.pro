# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase

# Keep Ktor related classes
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }

# Keep data models
-keep class com.xyldrun.data.model.** { *; }
-keep class com.xyldrun.data.local.** { *; }
-keep class com.xyldrun.data.remote.** { *; } 