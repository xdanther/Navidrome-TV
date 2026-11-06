# Kotlinx serialization keeps generated serializers referenced via reflection-free lookups.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.navidrometv.**$$serializer { *; }
-keep class com.navidrometv.data.remote.dto.** { *; }

# NanoHTTPD is used by the optional Party Mode guest server.
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**
