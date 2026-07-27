# =============================================================================
# Phuzle Messages — ProGuard / R8 rules
# =============================================================================
# R8 is enabled for release builds (isMinifyEnabled = true in build.gradle.kts).
# Rules below protect classes that are accessed via reflection, serialisation, or
# JNI so R8 does not rename or remove them.  Third-party libraries that ship
# their own consumer rules (e.g. Retrofit, OkHttp, Coroutines) are already
# handled automatically; the rules here cover the remainder.
# =============================================================================

# ---------------------------------------------------------------------------
# Kotlin
# ---------------------------------------------------------------------------
# Preserve Kotlin metadata so reflection-based code (serialisation, etc.) works.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable   # readable crash stack traces

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlin.coroutines.** { *; }

# ---------------------------------------------------------------------------
# Room — entities and DAOs are accessed by generated code & reflection
# ---------------------------------------------------------------------------
-keep class com.phuzle.labs.messages.data.db.entity.** { *; }
-keep interface com.phuzle.labs.messages.data.db.dao.** { *; }
# Room-generated _Impl classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# ---------------------------------------------------------------------------
# Retrofit + Gson — API response models must not be renamed/removed
# ---------------------------------------------------------------------------
# Retrofit internals
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson — preserve generic type tokens
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# App network/data models used with Gson (add sub-packages as needed)
-keep class com.phuzle.labs.messages.data.** { *; }
-keep class com.phuzle.labs.messages.domain.model.** { *; }

# ---------------------------------------------------------------------------
# Firebase
# ---------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
# Crashlytics — do not obfuscate NDK crash reporting internals
-keepattributes *Annotation*
-keep public class * extends java.lang.Exception
# Remote Config — field names used as config keys
-keep class com.google.firebase.remoteconfig.** { *; }

# ---------------------------------------------------------------------------
# WorkManager — Worker subclasses instantiated by class name at runtime
# ---------------------------------------------------------------------------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---------------------------------------------------------------------------
# DataStore — Protobuf / Preferences serialiser internals
# ---------------------------------------------------------------------------
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ---------------------------------------------------------------------------
# Coil — image loader registered via ServiceLoader
# ---------------------------------------------------------------------------
-keep class coil.** { *; }

# ---------------------------------------------------------------------------
# OkHttp / Okio (complement the consumer rules shipped by the library)
# ---------------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------------------------------------------------------------------------
# Biometric
# ---------------------------------------------------------------------------
-keep class androidx.biometric.** { *; }

# ---------------------------------------------------------------------------
# Suppress warnings for optional/platform classes not present on Android
# ---------------------------------------------------------------------------
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
