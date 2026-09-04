# Keep native JNI interfaces for UVC streaming
-keep class com.jiangdg.ausbc.** { *; }
-keepclassmembers class * {
    native <methods>;
}
