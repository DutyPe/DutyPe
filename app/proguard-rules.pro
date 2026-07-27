# =============================================================================
# DutyPe R8 / ProGuard rules
# =============================================================================
# Philosophy: trust the consumer-rules.pro that each library ships. Adding
# `-keep class library.** { *; }` here on top of that is the #1 reason apps
# ship multi-MB dex bloat — it tells R8 to retain every class/method even if
# unreachable, which on libraries like material-icons-extended (~30k classes)
# costs ~15 MB of dex per release.
#
# Only add rules here when the app uses reflection, JNI, or serialization in
# a way that R8 cannot statically prove is reachable.
# =============================================================================

# -----------------------------------------------------------------------------
# General Android / Kotlin
# -----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations,AnnotationDefault

# Native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Kotlin metadata (needed for reflection on Kotlin types)
-keep class kotlin.Metadata { *; }

# Application class — referenced from the manifest, R8 normally keeps this,
# but the explicit rule documents the dependency.
-keep class com.dutype.app.DutyPeApplication
-keep class com.example.dutype.DutyPeApplication

# -----------------------------------------------------------------------------
# App data classes that Firestore + Gson + kotlinx.serialization deserialize
# into. R8 cannot see field accesses through reflection so we must keep these.
# -----------------------------------------------------------------------------
-keep class com.example.dutype.models.** { *; }
-keepclassmembers class com.example.dutype.models.** { *; }

# kotlinx.serialization @Serializable types and their generated $$serializer
# companions (used by Nav 3 typed routes in navigation/destinations/).
-keep,includedescriptorclasses class com.example.dutype.**$$serializer { *; }
-keepclassmembers class com.example.dutype.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.dutype.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# -----------------------------------------------------------------------------
# Hilt / Dagger
# Hilt generates code that R8 must not strip or rename incorrectly. The
# library ships consumer rules but a few app-side rules are still required
# for assisted injection + ViewModel discovery.
# -----------------------------------------------------------------------------
-keep,allowobfuscation,allowshrinking class dagger.hilt.android.internal.** { *; }
-keep class * extends androidx.lifecycle.ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class * implements dagger.assisted.AssistedFactory

# Standard reflection-driven injection points
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}

# -----------------------------------------------------------------------------
# Room — DAOs and Entities are accessed reflectively at first DB open.
# -----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# -----------------------------------------------------------------------------
# Parcelable + Serializable + Enum boilerplate (Android requires CREATOR
# fields to be discoverable via reflection at runtime).
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

# Production v63 crash guard: Android framework dispatches these callbacks via
# the OnAttachStateChangeListener interface. R8 must not remove or merge the
# concrete implementations from AppCompat/Material/Compose listener classes.
-keep class * implements android.view.View$OnAttachStateChangeListener {
    public void onViewAttachedToWindow(android.view.View);
    public void onViewDetachedFromWindow(android.view.View);
}
-keepclassmembers class * {
    public void onViewAttachedToWindow(android.view.View);
    public void onViewDetachedFromWindow(android.view.View);
}

# Production v65 crash guard: Compose draw callbacks are interface-dispatched
# through DrawModifierNode. Reused release mappings from older Compose internals
# must not rename the interface method without the concrete implementation.
-keepclassmembers interface androidx.compose.ui.node.DrawModifierNode {
    public void draw(androidx.compose.ui.graphics.drawscope.ContentDrawScope);
}
-keepclassmembers class * implements androidx.compose.ui.node.DrawModifierNode {
    public void draw(androidx.compose.ui.graphics.drawscope.ContentDrawScope);
}
-keepclassmembers class * {
    public void draw(androidx.compose.ui.graphics.drawscope.ContentDrawScope);
}

# Strip verbose logging in release. R8 will inline + dead-code-eliminate the
# call sites once it knows these methods have no side effects.
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
# Suppress noisy warnings from libraries that ship with optional / unused
# transitive references R8 can't resolve.
# -----------------------------------------------------------------------------
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.beans.**

# -----------------------------------------------------------------------------
# P2: Reflection & Native keep rules for SQLCipher, Play Billing, WorkManager
# Note: Firebase ships its own consumer rules. Do not use blanket keep on firebase.**
# -----------------------------------------------------------------------------
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.database.** { *; }
-keep class com.android.billingclient.api.** { *; }
-keep class androidx.work.** { *; }

# Exclude legacy firebase-iid registrars from component discovery
-dontwarn com.google.firebase.iid.**

