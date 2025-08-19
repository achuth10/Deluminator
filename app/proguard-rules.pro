# Budget Deluminator ProGuard Rules

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep data classes and entities
-keep class com.example.budgetdeluminator.data.entity.** { *; }
-keep class com.example.budgetdeluminator.data.model.** { *; }

# Keep ViewBinding classes
-keep class * extends androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(...);
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep custom exceptions for better crash reports
-keep public class * extends java.lang.Exception

# Keep currency and validation utility methods
-keep class com.example.budgetdeluminator.utils.** { public *; }

# Keep biometric authentication helper
-keep class com.example.budgetdeluminator.utils.BiometricAuthHelper { *; }

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep crash reporting information
-keepattributes *Annotation*
-keepattributes Exceptions

# Optimize and obfuscate
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify