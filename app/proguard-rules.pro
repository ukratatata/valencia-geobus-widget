# Proguard rules for València GeoBus Widget
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.valencia.geobus.data.local.entity.** { *; }
-keep class com.valencia.geobus.data.model.** { *; }
-keep class com.google.gson.** { *; }
