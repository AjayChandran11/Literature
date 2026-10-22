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

# Ktor and Koin used to be kept whole (`-keep class io.ktor.** { *; }`, same for org.koin), which
# switched R8 off for the two biggest libraries in the app. Neither needs it: the engine is passed
# explicitly (HttpClient(OkHttp)), the WebSocket frames are decoded by kotlinx.serialization's
# generated serializers (kept above), and Koin resolves its graph through constructor lambdas,
# not reflection. The one reflective seam Ktor has is engine discovery: HttpClient() with no
# engine walks ServiceLoader and instantiates a container by its no-arg constructor, which AGP 9's
# R8 no longer keeps for a bare `-keep class` (the same shape that lost Firebase its registrars).
# Keep that constructor so a future engine-less HttpClient() can't fail the same way.
-keep class * implements io.ktor.client.HttpClientEngineContainer { <init>(); }
-dontwarn io.ktor.**
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

# Firebase discovers ComponentRegistrars reflectively via their no-arg constructor. AGP 9's R8
# (strictFullModeForKeepRules) no longer keeps <init>() for a bare `-keep class`, and the rule
# shipped by firebase-components 18.0.0 is bare — so Crashlytics' registrar lost its constructor
# and the component silently vanished ("FirebaseCrashlytics component is not present", 1.1.12/13).
# Same rule Firebase added upstream in firebase-components 18.0.1.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
