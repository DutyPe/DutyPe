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
# HILT - DEPENDENCY INJECTION (CRITICAL FIX FOR "Multiple entries with same key" CRASH)
# ============================================================================
# Hilt uses generated code and Guava's ImmutableMap internally.
# These rules prevent R8 from breaking Hilt's dependency graph.

# Keep all Hilt generated components and modules
-keep class * extends dagger.hilt.internal.DaggerComponent
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep @dagger.hilt.InstallIn class *

# CRITICAL: Keep Hilt internal classes to prevent "Multiple entries with same key" crash
-keep class dagger.hilt.** { *; }
-keep class dagger.internal.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }

# Keep all Hilt generated code
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_GeneratedInjector { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltComponents$* { *; }
-keep class **_ComponentTreeDeps { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }
-keep class **_Impl { *; }

# Keep Hilt entry points
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.EntryPoint class *

# Keep Hilt modules
-keep @dagger.Module class *
-keep @dagger.hilt.InstallIn class *

# Keep Hilt ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Hilt assisted injection
-keep class * implements dagger.assisted.AssistedFactory { *; }

# ============================================================================
# GUAVA - USED BY HILT INTERNALLY (CRITICAL FIX)
# ============================================================================
# Guava's ImmutableMap is used by Hilt and can cause "Multiple entries with same key"
# crash if obfuscated incorrectly.
-keep class com.google.common.** { *; }
-keep interface com.google.common.** { *; }
-dontwarn com.google.common.**

# Specifically keep ImmutableMap and related classes
-keep class com.google.common.collect.** { *; }
-keep class com.google.common.base.** { *; }

# CRITICAL: Prevent R8 from merging classes that could cause key collisions
-keep,allowobfuscation,allowshrinking class * extends dagger.internal.Factory
-keep,allowobfuscation,allowshrinking class * extends dagger.internal.Binding

# Keep all generated Dagger/Hilt factories
-keep class **_Factory { *; }
-keep class **_Factory$* { *; }
-keep class **_MembersInjector { *; }
-keep class **_Provide*Factory { *; }

# Prevent class merging for Hilt components
-keep,allowobfuscation class * extends dagger.hilt.internal.GeneratedComponent
-keep,allowobfuscation class * extends dagger.hilt.internal.GeneratedComponentManager

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

# Keep all ViewModels to prevent Hilt injection issues
-keep class com.example.dutype.viewmodels.** { *; }
-keep class com.example.dutype.employer.viewmodels.** { *; }
-keep class com.example.dutype.worker.viewmodels.** { *; }

# Keep all services
-keep class com.example.dutype.services.** { *; }

# Keep all repositories
-keep class com.example.dutype.repositories.** { *; }

# Keep DI module
-keep class com.example.dutype.di.** { *; }

# Keep state managers
-keep class com.example.dutype.state.** { *; }

# Keep auth classes
-keep class com.example.dutype.auth.** { *; }

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


# ============================================================================
# JETPACK COMPOSE
# ============================================================================
# Keep Compose runtime classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose UI classes
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.runtime.** { *; }

# ============================================================================
# NAVIGATION COMPOSE
# ============================================================================
-keep class androidx.navigation.** { *; }
-keep class androidx.hilt.navigation.** { *; }

# ============================================================================
# LIFECYCLE & VIEWMODEL
# ============================================================================
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ============================================================================
# ROOM DATABASE
# ============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class com.example.dutype.database.** { *; }

# ============================================================================
# CREDENTIAL MANAGER & GOOGLE SIGN-IN
# ============================================================================
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# ============================================================================
# COIL IMAGE LOADING
# ============================================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================================
# TIMBER LOGGING
# ============================================================================
-keep class timber.log.** { *; }

# ============================================================================
# PLACES SDK
# ============================================================================
-keep class com.google.android.libraries.places.** { *; }
-dontwarn com.google.android.libraries.places.**

# ============================================================================
# PLAY SERVICES
# ============================================================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================================================
# LOTTIE ANIMATIONS
# ============================================================================
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ============================================================================
# ACCOMPANIST
# ============================================================================
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ============================================================================
# DATASTORE
# ============================================================================
-keep class androidx.datastore.** { *; }

# ============================================================================
# PREVENT R8 FROM REMOVING CLASSES USED VIA REFLECTION
# ============================================================================
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

-keepclassmembers class * {
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}

# ============================================================================
# KEEP ENUMS (USED IN MODELS)
# ============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# KEEP PARCELABLE IMPLEMENTATIONS
# ============================================================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================================================
# KEEP SERIALIZABLE IMPLEMENTATIONS
# ============================================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}



# ============================================================================
# EMPLOYER PACKAGE
# ============================================================================
-keep class com.example.dutype.employer.** { *; }
-keep class com.example.dutype.employer.viewmodels.** { *; }
-keep class com.example.dutype.employer.models.** { *; }
-keep class com.example.dutype.employer.screens.** { *; }

# ============================================================================
# WORKER PACKAGE
# ============================================================================
-keep class com.example.dutype.worker.** { *; }
-keep class com.example.dutype.worker.viewmodels.** { *; }
-keep class com.example.dutype.worker.models.** { *; }
-keep class com.example.dutype.worker.screens.** { *; }

# ============================================================================
# COMMON PACKAGE
# ============================================================================
-keep class com.example.dutype.common.** { *; }

# ============================================================================
# UTILS PACKAGE
# ============================================================================
-keep class com.example.dutype.utils.** { *; }

# ============================================================================
# COMPONENTS PACKAGE
# ============================================================================
-keep class com.example.dutype.components.** { *; }

# ============================================================================
# NAVIGATION PACKAGE
# ============================================================================
-keep class com.example.dutype.navigation.** { *; }
