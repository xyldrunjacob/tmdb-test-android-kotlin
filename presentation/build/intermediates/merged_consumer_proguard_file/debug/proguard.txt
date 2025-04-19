# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Keep Composables
-keep class * extends androidx.compose.runtime.Composable {
    <init>();
}

# Keep UI models and states
-keep class com.xyldrun.presentation.model.** { *; }
-keep class com.xyldrun.presentation.**.state.** { *; }

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; } 