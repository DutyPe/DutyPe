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

# App models
-keep class com.example.dutype.models.** { *; }
-keepclassmembers class com.example.dutype.models.** { *; }
-keep,includedescriptorclasses class com.example.dutype.**$$serializer { *; }

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

# -----------------------------------------------------------------------------
# Optimization safety
# -----------------------------------------------------------------------------
-allowaccessmodification
-repackageclasses ''

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
