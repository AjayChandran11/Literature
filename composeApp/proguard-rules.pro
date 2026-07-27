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

# Ktor — kept whole: it's the online transport (WebSocket + serialization + OkHttp engine), and
# R8 stripping its reflectively-touched internals has broken client networking in the past.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Koin — kept: the DI graph is resolved at startup; a wrongly-stripped definition crashes on launch.
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# App classes: polymorphic serialization has NO @SerialName anywhere, so the sealed ServerMessage/
# ClientMessage/etc. discriminators default to the fully-qualified class name. Obfuscating these
# would rewrite the wire 'type' values and break the online protocol against the (unminified)
# server — so keep their names. (Shrinking/optimization still applies; only renaming is disabled.)
-keepnames class com.cards.game.literature.** { *; }

# The libraries below ship their own R8 consumer rules, so the previous blanket `-keep ... { *; }`
# for each was removed — it neutralised R8, keeping (notably) all of the largely-unused GMS. Only
# -dontwarn is retained so R8 stays quiet about their optional/absent references.
-dontwarn kotlinx.coroutines.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn co.touchlab.kermit.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
