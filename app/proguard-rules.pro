# =============================================================================
# DutyPe R8 / ProGuard rules
# =============================================================================

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations,AnnotationDefault

# Native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Application & Activity classes
-keep class com.dutype.app.DutyPeApplication { *; }
-keep class com.example.dutype.DutyPeApplication { *; }
-keep class com.example.dutype.MainActivity { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }

# App models & @Keep annotated classes
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers @androidx.annotation.Keep class * { *; }
-keep class com.example.dutype.models.** { *; }
-keepclassmembers class com.example.dutype.models.** { *; }
-keep class com.example.dutype.homeservices.** { *; }
-keepclassmembers class com.example.dutype.homeservices.** { *; }
-keep class com.example.dutype.employer.models.** { *; }
-keepclassmembers class com.example.dutype.employer.models.** { *; }
-keep class com.example.dutype.employer.ai.** { *; }
-keepclassmembers class com.example.dutype.employer.ai.** { *; }
-keep class com.example.dutype.employer.voice.** { *; }
-keepclassmembers class com.example.dutype.employer.voice.** { *; }
-keep class com.example.dutype.worker.models.** { *; }
-keepclassmembers class com.example.dutype.worker.models.** { *; }
-keep class com.example.dutype.urgent.** { *; }
-keepclassmembers class com.example.dutype.urgent.** { *; }
-keep class com.example.dutype.profile.** { *; }
-keepclassmembers class com.example.dutype.profile.** { *; }
-keep class com.example.dutype.data.** { *; }
-keepclassmembers class com.example.dutype.data.** { *; }
-keep class com.example.dutype.auth.** { *; }
-keepclassmembers class com.example.dutype.auth.** { *; }
-keep class com.example.dutype.components.** { *; }
-keepclassmembers class com.example.dutype.components.** { *; }
-keep,includedescriptorclasses class com.example.dutype.**$$serializer { *; }

# -----------------------------------------------------------------------------
# Coil (Image Loading)
# -----------------------------------------------------------------------------
-keep class coil.** { *; }
-keep interface coil.** { *; }
-keepclassmembers class coil.** { *; }
-keepclassmembers interface coil.** { *; }
-keep class coil.intercept.** { *; }
-keepclassmembers class coil.intercept.** { *; }
-keep class coil.request.ImageResult { *; }
-keep class coil.request.SuccessResult { *; }
-keep class coil.request.ErrorResult { *; }
-keep class coil.request.ImageRequest { *; }
-keep class coil.request.Options { *; }
-keep class coil.compose.** { *; }
-keepclassmembers class coil.compose.** { *; }
-dontwarn coil.**

# -----------------------------------------------------------------------------
# Gson & Serialization
# -----------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# -----------------------------------------------------------------------------
# Kotlin Coroutines & Compose Stability
# -----------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
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
-keeppackagenames com.example.dutype.**

# -----------------------------------------------------------------------------
# AndroidX Lifecycle, ViewModel, & Hilt
# -----------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class * implements dagger.assisted.AssistedFactory

-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }
-keepclassmembers interface androidx.lifecycle.** { *; }
-keep class * implements androidx.lifecycle.HasDefaultViewModelProviderFactory { *; }
-keepclassmembers class * implements androidx.lifecycle.HasDefaultViewModelProviderFactory { *; }

-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keepclassmembers class dagger.hilt.** { *; }
-keepclassmembers interface dagger.hilt.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}

# -----------------------------------------------------------------------------
# Room & SQLCipher
# -----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomOpenHelper$Delegate { *; }
-keepclassmembers class * extends androidx.room.RoomOpenHelper$Delegate { *; }
-keep class androidx.room.RoomOpenHelper { *; }
-keep class androidx.room.RoomOpenHelper$Delegate { *; }
-keepclassmembers class androidx.room.RoomOpenHelper$Delegate { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers @androidx.room.Dao class * { *; }

-keep class androidx.sqlite.db.** { *; }
-keepclassmembers class androidx.sqlite.db.** { *; }

-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.security.** { *; }
-keepclassmembers class net.zetetic.database.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { *; }

# -----------------------------------------------------------------------------
# Parcelable + Serializable + Enum
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

# Optimization safety
-allowaccessmodification
# Note: -repackageclasses '' is disabled because it collapses Compose runtime lambdas,
# singletons, and Coil interceptors into the default package, causing R8 class/interface
# merging and runtime ClassCastExceptions.
#-repackageclasses ''

-keep class * implements android.view.View$OnAttachStateChangeListener {
    public void onViewAttachedToWindow(android.view.View);
    public void onViewDetachedFromWindow(android.view.View);
}
-keepclassmembers class * {
    public void onViewAttachedToWindow(android.view.View);
    public void onViewDetachedFromWindow(android.view.View);
}

-keepclassmembers interface androidx.compose.ui.node.DrawModifierNode {
    public void draw(androidx.compose.ui.graphics.drawscope.ContentDrawScope);
}
-keepclassmembers class * implements androidx.compose.ui.node.DrawModifierNode {
    public void draw(androidx.compose.ui.graphics.drawscope.ContentDrawScope);
}

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

-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.beans.**

# WorkManager
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
# Firebase Component Discovery — required for Firebase to find its own modules via reflection
# Without these, R8 strips ComponentRegistrar implementations and FirebaseInitProvider crashes with MissingDependencyException.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
    public java.util.List getComponents();
}
-keep class com.google.firebase.components.** { *; }
-keep interface com.google.firebase.components.** { *; }
-keepclassmembers class com.google.firebase.components.** { *; }
-keep class com.google.firebase.inject.** { *; }
-keep interface com.google.firebase.inject.** { *; }
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-keep class com.google.firebase.components.ComponentDiscoveryService { *; }
-keep class com.example.dutype.di.FirebaseIidRegistrar { *; }
-keep class com.google.firebase.iid.internal.** { *; }
-keep interface com.google.firebase.iid.internal.** { *; }

# Keep firebase-common, firebase-installations, and analytics which ComponentRuntime depends on
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# -----------------------------------------------------------------------------
# Google Play Core In-App Review
# -----------------------------------------------------------------------------
-keep class com.google.android.play.core.review.** { *; }
-keep interface com.google.android.play.core.review.** { *; }
-keepclassmembers class com.google.android.play.core.review.** { *; }
-keepclassmembers interface com.google.android.play.core.review.** { *; }
-keep class com.google.android.play.core.common.** { *; }
-keep class com.google.android.play.core.tasks.** { *; }
-keepclassmembers class com.google.android.play.core.tasks.** { *; }
-dontwarn com.google.android.play.core.**

# -----------------------------------------------------------------------------
# Jetpack Compose Animation, Transitions & Vector Converters
# -----------------------------------------------------------------------------
-keep class androidx.compose.animation.** { *; }
-keep interface androidx.compose.animation.** { *; }
-keepclassmembers class androidx.compose.animation.** { *; }
-keepclassmembers interface androidx.compose.animation.** { *; }
-keep class androidx.compose.animation.core.** { *; }
-keep interface androidx.compose.animation.core.** { *; }
-keepclassmembers class androidx.compose.animation.core.** { *; }
-keepclassmembers interface androidx.compose.animation.core.** { *; }
-keep class androidx.compose.runtime.saveable.** { *; }
-keepclassmembers class androidx.compose.runtime.saveable.** { *; }

# DutyPe Review & Rating Components
-keep class com.example.dutype.utils.InAppReviewManager { *; }
-keepclassmembers class com.example.dutype.utils.InAppReviewManager { *; }
-keep class com.example.dutype.utils.ReviewStats { *; }
-keepclassmembers class com.example.dutype.utils.ReviewStats { *; }
-keep class com.example.dutype.services.InAppReviewTriggerService { *; }
-keepclassmembers class com.example.dutype.services.InAppReviewTriggerService { *; }
-keep class com.example.dutype.components.PlayStoreRatingPromptKt { *; }
-keepclassmembers class com.example.dutype.components.PlayStoreRatingPromptKt { *; }


