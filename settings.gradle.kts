pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            version("compose", "1.5.4")
            version("compose-compiler", "1.5.11")
            version("coroutines", "1.7.3")
            version("lifecycle", "2.7.0")
            version("koin", "3.5.3")
            version("ktor", "2.3.8")
            version("room", "2.6.1")
            version("navigation", "2.7.7")
            version("accompanist", "0.32.0")
            version("coil", "2.5.0")
            version("lottie", "6.3.0")
            version("shimmer", "1.2.0")
            version("compose-bom", "2024.02.02")
            version("android-gradle-plugin", "8.2.0")
            version("kotlin", "1.9.23")
            
            // Plugins
            plugin("android-application", "com.android.application").versionRef("android-gradle-plugin")
            plugin("android-library", "com.android.library").versionRef("android-gradle-plugin")
            plugin("kotlin-android", "org.jetbrains.kotlin.android").versionRef("kotlin")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            
            // Core Android
            library("androidx-core-ktx", "androidx.core", "core-ktx").version("1.12.0")
            library("activity-compose", "androidx.activity", "activity-compose").version("1.8.2")
            
            // Compose BOM and UI components
            library("compose-bom", "androidx.compose", "compose-bom").versionRef("compose-bom")
            library("ui", "androidx.compose.ui", "ui")
            library("ui-graphics", "androidx.compose.ui", "ui-graphics")
            library("ui-tooling", "androidx.compose.ui", "ui-tooling")
            library("ui-tooling-preview", "androidx.compose.ui", "ui-tooling-preview")
            library("material3", "androidx.compose.material3", "material3")
            library("ui-test-junit4", "androidx.compose.ui", "ui-test-junit4")
            library("ui-test-manifest", "androidx.compose.ui", "ui-test-manifest")
            
            // Ktor
            library("ktor-core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor-android", "io.ktor", "ktor-client-android").versionRef("ktor")
            library("ktor-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            library("ktor-logging", "io.ktor", "ktor-client-logging").versionRef("ktor")
            library("ktor-auth", "io.ktor", "ktor-client-auth").versionRef("ktor")
            
            // Coroutines
            library("coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")
            library("coroutines-android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef("coroutines")
            
            // Lifecycle
            library("lifecycle-runtime-ktx", "androidx.lifecycle", "lifecycle-runtime-ktx").versionRef("lifecycle")
            library("lifecycle-viewmodel-ktx", "androidx.lifecycle", "lifecycle-viewmodel-ktx").versionRef("lifecycle")
            library("lifecycle-viewmodel-compose", "androidx.lifecycle", "lifecycle-viewmodel-compose").versionRef("lifecycle")
            
            // Koin
            library("koin-android", "io.insert-koin", "koin-android").versionRef("koin")
            library("koin-compose", "io.insert-koin", "koin-androidx-compose").versionRef("koin")
            
            // Room
            library("room-runtime", "androidx.room", "room-runtime").versionRef("room")
            library("room-ktx", "androidx.room", "room-ktx").versionRef("room")
            library("room-compiler", "androidx.room", "room-compiler").versionRef("room")
            
            // Navigation
            library("navigation-compose", "androidx.navigation", "navigation-compose").versionRef("navigation")
            library("androidx-navigation-fragment-ktx", "androidx.navigation", "navigation-fragment-ktx").versionRef("navigation")
            library("androidx-navigation-ui-ktx", "androidx.navigation", "navigation-ui-ktx").versionRef("navigation")
            
            // Accompanist
            library("accompanist-swiperefresh", "com.google.accompanist", "accompanist-swiperefresh").versionRef("accompanist")
            library("accompanist-placeholder", "com.google.accompanist", "accompanist-placeholder").versionRef("accompanist")
            
            // Image Loading
            library("coil-compose", "io.coil-kt", "coil-compose").versionRef("coil")
            
            // Testing
            library("junit", "junit", "junit").version("4.13.2")
            library("androidx-junit", "androidx.test.ext", "junit").version("1.1.5")
            library("androidx-espresso-core", "androidx.test.espresso", "espresso-core").version("3.5.1")
            library("ktor-client-mock", "io.ktor", "ktor-client-mock").versionRef("ktor")

            // Bundles
            bundle("ktor", listOf(
                "ktor-core",
                "ktor-android",
                "ktor-content-negotiation",
                "ktor-json",
                "ktor-logging"
            ))
        }
    }
}

rootProject.name = "MyApplication"
include(":app")
include(":core")
include(":data")
include(":domain")
include(":presentation")
 