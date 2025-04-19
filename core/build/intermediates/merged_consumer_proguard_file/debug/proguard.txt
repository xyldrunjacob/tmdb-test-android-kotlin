# Add any ProGuard rules required for the core module
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep any classes that are referenced by AndroidManifest.xml
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider 