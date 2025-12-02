# ============================================================================
# APPLICATION CLASS
# ============================================================================
# This rule is critical. It prevents R8 from removing or renaming your main
# Application class, which would cause a ClassNotFoundException at startup.
-keep class com.example.dutype.DutyPeApplication { *; }

# ============================================================================
# ANDROID & KOTLIN DEFAULTS
# ============================================================================
# This file is included in the default ProGuard rules for Android. It's good
# practice to keep these basic rules for compatibility.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*

# Keep native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Preserve Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes InnerClasses,EnclosingMethod,Signature
-keep class kotlin.reflect.jvm.internal.** { *; }

# ============================================================================
# COROUTINES
# ============================================================================
# This is a common requirement for projects using Kotlin Coroutines.
-keep class kotlinx.coroutines.debug.** { *; }

# ============================================================================
# HILT - DEPENDENCY INJECTION
# ============================================================================
# Hilt uses generated code, so these rules are necessary to ensure that it
# works correctly after obfuscation.
-keep class * extends dagger.hilt.internal.DaggerComponent
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep @dagger.hilt.InstallIn class *

# ============================================================================
# GOOGLE MOBILE ADS (GMS ADS) - CRITICAL FIX FOR Multiple entries with same key
# ============================================================================
# This rule is essential to prevent R8 from breaking the internal AdMob service
# initialization, which causes the "Multiple entries with same key" crash.
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable
# Google Mobile Ads SDK uses reflection and dynamic class loading.
# These rules prevent R8/ProGuard from obfuscating or removing ad-related classes.
# This is critical to prevent crashes like "Multiple entries with same key" in ImmutableMap
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class * extends java.util.List

# ============================================================================
# FIREBASE & FIRESTORE
# ============================================================================
# Firebase libraries often use reflection, so these rules are important.
-keep class com.google.firebase.provider.FirebaseInitProvider
-keepnames class com.google.firebase.auth.** { *; }
-keepnames class com.google.firebase.firestore.** { *; }

# Keep your data model classes that are used by Firestore.
# This is critical to prevent crashes from data serialization/deserialization.
-keep class com.example.dutype.models.** { *; }

# ============================================================================
# KOTLINX SERIALIZATION
# ============================================================================
# If you are using kotlinx.serialization, you need to keep the @Serializable
# annotations and the generated .Companion classes.
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class **.Companion { *; }

# ============================================================================
# LOGGING - Remove in Release Builds
# ============================================================================
# This rule removes logging calls from your release build, which is a good
# practice for reducing APK size and improving performance.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
