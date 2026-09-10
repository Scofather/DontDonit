# The Anthropic SDK serializes models with Jackson via reflection.
-keep class com.anthropic.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-dontwarn com.anthropic.**

# Jackson
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# kotlinx.serialization keeps generated serializers on our own models.
-keepclassmembers class com.prismgrade.** {
    *** Companion;
}
-keepclasseswithmembers class com.prismgrade.** {
    kotlinx.serialization.KSerializer serializer(...);
}
