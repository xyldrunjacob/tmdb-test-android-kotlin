# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep the application class
-keep class com.xyldrun.myapplication.MovieApp { *; }

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-keep class androidx.compose.ui.text.font.FontFamily { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Koin
-keepnames class org.koin.** { *; }
-keep class org.koin.** { *; }
-keep class org.koin.android.** { *; }
-keep class org.koin.androidx.** { *; }

# Keep data classes (DTOs and Entities)
-keep class com.xyldrun.data.remote.** { *; }
-keep class com.xyldrun.data.local.** { *; }
-keep class com.xyldrun.domain.model.** { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontwarn sun.misc.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Keep Accompanist
-keep class com.google.accompanist.** { *; }
-keep class androidx.navigation.** { *; }

# Keep Shimmer
-keep class com.valentinilk.shimmer.** { *; }

# Keep JUnit and Testing related classes for debug builds
-keepclassmembers class * {
    @org.junit.Test *;
    @org.junit.runner.RunWith *;
}

# Keep Lifecycle components
-keep class androidx.lifecycle.** { *; }
-keep class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keepclassmembers class androidx.lifecycle.Lifecycle$State { *; }
-keepclassmembers class androidx.lifecycle.Lifecycle$Event { *; }
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent *;
}

# General rules
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers
-keepattributes Signature                         # Keep generic signatures
-keepattributes *Annotation*                      # Keep Annotations
-keepattributes Exception                         # Keep Exception names
-keepattributes InnerClasses                      # Keep InnerClasses
-keepattributes Deprecated                        # Keep Deprecated annotations
-keepattributes EnclosingMethod                   # Keep Enclosing Methods
-keepattributes Exceptions                        # Keep Exceptions

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep custom application classes
-keep public class * extends android.app.Application

# Keep activities, services, etc.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    *** get*();
}

# Keep Ktor classes
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**