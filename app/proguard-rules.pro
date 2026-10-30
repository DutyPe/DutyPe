# =============================================================================
# DutyPe Production R8 / ProGuard Configuration
# =============================================================================
# Follows Google Android Developer Best Practices for Code Shrinking:
# 1. Trust consumer-rules.pro bundled in official AndroidX / Firebase / Play SDKs.
# 2. Avoid blanket wildcards on third-party SDKs to prevent multi-MB DEX bloat.
# 3. Keep reflective models, JNI entry points, Hilt injection, and Room DAOs.
# =============================================================================

# -----------------------------------------------------------------------------
# General Android & Kotlin Defaults
# -----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations,AnnotationDefault

# Native JNI methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Kotlin metadata (needed for reflection on Kotlin types)
-keep class kotlin.Metadata { *; }

# Application & Activity classes
-keep class com.dutype.app.DutyPeApplication
-keep class com.example.dutype.DutyPeApplication
-keep class com.example.dutype.MainActivity
-keep class * extends androidx.activity.ComponentActivity
-keep class * extends androidx.fragment.app.Fragment

# -----------------------------------------------------------------------------
# App Data Models & Serialization
# Preserves data classes accessed via Firestore / Gson reflection
# -----------------------------------------------------------------------------
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers @androidx.annotation.Keep class * { *; }

-keep class com.example.dutype.models.** { *; }
-keepclassmembers class com.example.dutype.models.** { *; }
-keep class com.example.dutype.employer.models.** { *; }
-keepclassmembers class com.example.dutype.employer.models.** { *; }
-keep class com.example.dutype.worker.models.** { *; }
-keepclassmembers class com.example.dutype.worker.models.** { *; }
-keep class com.example.dutype.homeservices.models.** { *; }
-keepclassmembers class com.example.dutype.homeservices.models.** { *; }
-keep class com.example.dutype.profile.models.** { *; }
-keepclassmembers class com.example.dutype.profile.models.** { *; }

# kotlinx.serialization @Serializable types and generated serializers
-keep,includedescriptorclasses class com.example.dutype.**$$serializer { *; }
-keepclassmembers class com.example.dutype.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.dutype.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# -----------------------------------------------------------------------------
# Gson & Reflection
# -----------------------------------------------------------------------------
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# -----------------------------------------------------------------------------
# Coil (Image Loading)
# Coil 2.x ships its own consumer rules; keep main image loader entry points
# -----------------------------------------------------------------------------
-keep class coil.ImageLoader { *; }
-keep class coil.ImageLoaderFactory { *; }
-dontwarn coil.**

# -----------------------------------------------------------------------------
# Kotlin Coroutines & Jetpack Compose Stability
# -----------------------------------------------------------------------------
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.SuspendLambda {
    *** invokeSuspend(...);
}
-keep class kotlin.coroutines.Continuation { *; }
-keep interface kotlin.coroutines.Continuation { *; }
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }
-keepclassmembers class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }
-keep interface androidx.compose.runtime.internal.ComposableLambda { *; }
-dontwarn androidx.compose.**

# -----------------------------------------------------------------------------
# Hilt / Dagger Dependency Injection
# -----------------------------------------------------------------------------
-keep,allowobfuscation,allowshrinking class dagger.hilt.android.internal.** { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class * implements dagger.assisted.AssistedFactory

-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}

# -----------------------------------------------------------------------------
# Room & SQLCipher Encrypted Database
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
# Parcelable + Serializable + Enum Boilerplate
# -----------------------------------------------------------------------------
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -----------------------------------------------------------------------------
# Firebase Components & Discovery
# Firebase SDKs ship their own consumer-rules.pro. Keep only component
# registrars and service entry points to allow reflection-based discovery.
# -----------------------------------------------------------------------------
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
    public java.util.List getComponents();
}
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-keep class com.google.firebase.components.ComponentDiscoveryService { *; }
-keep class com.example.dutype.di.FirebaseIidRegistrar {
    public <init>();
    public java.util.List getComponents();
}
-keep class com.example.dutype.services.DutyPeMessagingService { *; }
-dontwarn com.google.firebase.**

# -----------------------------------------------------------------------------
# Google Mobile Ads (AdMob)
# play-services-ads ships its own consumer rules; keep provider entry points
# -----------------------------------------------------------------------------
-keep public class com.google.android.gms.ads.MobileAdsInitProvider
-keep public class com.google.android.gms.ads.identifier.AdvertisingIdClient { *; }
-keep public class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info { *; }
-keep class com.google.android.gms.common.internal.safeparcel.SafeParcelable
-dontwarn com.google.android.gms.ads.**

# -----------------------------------------------------------------------------
# Google Play In-App Review & Truecaller SDK
# -----------------------------------------------------------------------------
-keep class com.google.android.play.core.review.** { *; }
-keep class com.google.android.play.core.common.** { *; }
-keep class com.truecaller.android.sdk.** { *; }
-dontwarn com.google.android.play.core.**
-dontwarn com.truecaller.android.sdk.**

# -----------------------------------------------------------------------------
# WorkManager Background Workers & Internal Room DAOs
# -----------------------------------------------------------------------------
-keep class com.example.dutype.workers.** { <init>(...); }
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }
-keepclassmembers interface androidx.work.** { *; }
-keep class androidx.work.impl.model.** { *; }
-keep interface androidx.work.impl.model.** { *; }
-keepclassmembers class androidx.work.impl.model.** { *; }
-keepclassmembers interface androidx.work.impl.model.** { *; }
-keep class * extends androidx.work.impl.model.** { *; }
-keep class * implements androidx.work.impl.model.** { *; }
-keepclassmembers class * extends androidx.work.impl.model.** { *; }
-keepclassmembers class * implements androidx.work.impl.model.** { *; }
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class androidx.work.impl.background.systemalarm.SystemAlarmService { *; }
-keep class androidx.work.impl.foreground.SystemForegroundService { *; }
-dontwarn androidx.work.impl.**

# -----------------------------------------------------------------------------
# Optimization & Code Stripping
# -----------------------------------------------------------------------------
-allowaccessmodification

# Strip verbose logging in release builds (inlined and dead-code eliminated)
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static *** tag(...);
}

# -----------------------------------------------------------------------------
# Suppress noisy warnings from optional / unused transitive references
# -----------------------------------------------------------------------------
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.beans.**
