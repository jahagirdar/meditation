# Serenity proguard rules

# Keep Room entities
-keep class com.serenity.data.db.entities.** { *; }

# Keep domain models
-keep class com.serenity.domain.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Wearable listener service
-keep class com.serenity.watch.PhoneWearableListenerService { *; }

# Keep enums (used in DB as strings)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# JSON (org.json is built-in, no rules needed)

# Suppress notes about missing R8 rules
-dontnote **
