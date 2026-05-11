# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Keep Kotlin serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.packly.app.**$$serializer { *; }
-keepclassmembers class com.packly.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.packly.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
