# Room: keep entities and DAOs (Room uses reflection on field names)
-keep class com.fastingtracker.data.local.entity.** { *; }
-keep class com.fastingtracker.data.local.dao.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Domain models, Backup DTOs, and Type-Safe Navigation routes (kotlinx.serialization)
-keep class com.fastingtracker.domain.model.** { *; }
-keep class com.fastingtracker.data.backup.** { *; }
-keep class com.fastingtracker.navigation.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.fastingtracker.**$$serializer { *; }
-keepclassmembers class com.fastingtracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.fastingtracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt, WorkManager & Glance Widget EntryPoints
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep class com.fastingtracker.widget.** { *; }
-keep class com.fastingtracker.notification.LiveUpdateRefreshWorker { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Billing client (Play Store ProGuard rules)
-keep class com.android.billingclient.api.** { *; }

# Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Crash reports: keep line numbers
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
