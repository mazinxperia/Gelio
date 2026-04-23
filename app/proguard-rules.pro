# Release hardening rules.
# Keep these narrow: do not add broad app-package keep rules unless a release-only
# crash proves that a specific class/member is accessed reflectively.

# Keep metadata commonly needed by AndroidX/Room/Kotlin-generated code and library
# consumer rules, without preserving source file names or line numbers.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Strip low-value debug logging from release output. Warnings/errors are kept.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static boolean isLoggable(java.lang.String, int);
}

# LinkedGalleryScrubber reflects into Material3 CarouselState for its internal
# pager bridge. Keep only that method name so release obfuscation does not break
# the gallery scrubber.
-keepclassmembers class androidx.compose.material3.carousel.CarouselState {
    public *** getPagerState$material3(...);
}

# Optional extra packaging obfuscation, intentionally disabled until a signed
# release APK passes a real-tablet smoke test. If enabled later, test every
# manifest service/receiver, Room path, WebView viewer, and gallery scrubber.
#
# -repackageclasses ''
#
# If a release-only regression appears, add the narrowest keep rule for the
# failing class/member. Do not disable R8 globally and do not keep the whole app.
