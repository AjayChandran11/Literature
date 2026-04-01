# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.cards.game.literature.**$$serializer { *; }
-keepclassmembers class com.cards.game.literature.** {
    *** Companion;
}
-keepclasseswithmembers class com.cards.game.literature.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keepnames class com.cards.game.literature.** { *; }

# OkHttp (used by Ktor on Android)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Kermit logging
-keep class co.touchlab.kermit.** { *; }
-dontwarn co.touchlab.kermit.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
