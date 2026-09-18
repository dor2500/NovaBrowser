# NovaBrowser ProGuard Rules
-keepattributes *Annotation*
-dontwarn org.codehaus.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
