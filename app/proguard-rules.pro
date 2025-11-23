# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================================
# CRITICAL: Enable line number mapping for crash debugging
# ============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# APP-SPECIFIC RULES
# ============================================================================
# Keep all classes in your app package
-keep class com.example.dutype.** { *; }
-keep class com.dutype.app.** { *; }

# Keep all enums
-keepclassmembers enum com.example.dutype.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# FIREBASE RULES
# ============================================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.storage.** { *; }
-keep interface com.google.firebase.** { *; }

# Preserve firebase internal classes
-keepclasseswithmembernames class com.google.firebase.database.collection.** { *; }
-keepclasseswithmembernames class com.google.firebase.** { *; }

# ============================================================================
# HILT DEPENDENCY INJECTION RULES
# ============================================================================
-keep class * extends dagger.hilt.internal.DaggerComponent
-keep @dagger.hilt.android.HiltAndroidApp class *
-keepclasseswithmembers class **_MembersInjector { *; }
-keepclasseswithmembers class **_Factory { *; }
-keepclasseswithmembers class **_Provide* { *; }
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembernames class * {
    @dagger.hilt.* <fields>;
}

# ============================================================================
# COMPOSE UI RULES
# ============================================================================
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keepclassmembers class androidx.compose.runtime.** {
    *** emit(...);
}
-keepclasseswithmembernames class androidx.compose.** {
    *** invoke(...);
}
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep interface androidx.compose.runtime.Composable { *; }

# ============================================================================
# GOOGLE PLAY SERVICES RULES
# ============================================================================
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-keep class com.google.android.libraries.places.** { *; }
-keep interface com.google.android.gms.** { *; }
-keepclasseswithmembernames class com.google.android.gms.** { *; }
-keepclasseswithmembernames class com.google.android.libraries.** { *; }

# Preserve Google Play Services internal classes
-keepclassmembers class com.google.android.gms.** { *; }
-keepclassmembers class com.google.android.libraries.** { *; }

# ============================================================================
# CREDENTIALS & AUTHENTICATION
# ============================================================================
-keep class androidx.credentials.** { *; }
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keepclasseswithmembernames class androidx.credentials.** { *; }

# Preserve annotation classes
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes *Annotation*

# ============================================================================
# GSON SERIALIZATION RULES
# ============================================================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Preserve model/data classes
-keep class com.example.dutype.models.** { *; }
-keep class com.example.dutype.worker.models.** { *; }
-keep class com.example.dutype.employer.models.** { *; }
-keepclassmembers class com.example.dutype.models.** { *; }
-keepclassmembers class com.example.dutype.worker.models.** { *; }
-keepclassmembers class com.example.dutype.employer.models.** { *; }

# ============================================================================
# KOTLIN SERIALIZATION RULES
# ============================================================================
-keepattributes Signature
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.SerializableKt <methods>;
}

# ============================================================================
# COROUTINES RULES
# ============================================================================
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# ============================================================================
# COIL IMAGE LOADING
# ============================================================================
-keep class coil.** { *; }
-keep class coil.compose.** { *; }
-keep interface coil.** { *; }
-keepclassmembers class coil.** { *; }

# ============================================================================
# LOTTIE ANIMATIONS
# ============================================================================
-keep class com.airbnb.lottie.** { *; }
-keep interface com.airbnb.lottie.** { *; }

# ============================================================================
# TIMBER LOGGING
# ============================================================================
-keep class timber.log.** { *; }
-keepclassmembers class timber.log.** { *; }

# ============================================================================
# ROOM DATABASE
# ============================================================================
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }

# ============================================================================
# DATASTORE RULES
# ============================================================================
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.** { *; }
-keep interface androidx.datastore.** { *; }

# ============================================================================
# ANDROIDX RULES
# ============================================================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }

# Preserve lifecycle classes
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }

# ============================================================================
# NATIVE METHODS
# ============================================================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# SUPPRESS WARNINGS
# ============================================================================
-ignorewarnings
-dontoptimize
-dontobfuscate

# ============================================================================
# OPTIMIZATION SETTINGS
# ============================================================================
# Optimize for speed (if you use optimization)
# -optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ============================================================================
# DEBUGGING SUPPORT
# ============================================================================
# Keep exceptions and their stack traces
-keepclasseswithmembernames class * {
    *** *(...);
}

# Preserve enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# SIZE OPTIMIZATION - AGGRESSIVE
# ============================================================================
# Remove unused classes, methods, and fields more aggressively
-dontshrink
-dontoptimize
-verbose

# Allow aggressive inlining and optimization
-allowaccessmodification
-mergeinterfacesaggressively

# Remove logging statements
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# For Timber (debug logs)
-assumenosideeffects class com.jakewharton.timber.log.Timber$DebugTree {
    *** debug(...);
    *** v(...);
}

# Remove unused code from Kotlin standard library
-dontwarn kotlin.reflect.**
-dontwarn kotlin.jvm.internal.Reflection

# Compress constants
-repackageclasses
-keepattributes InnerClasses,EnclosingMethod