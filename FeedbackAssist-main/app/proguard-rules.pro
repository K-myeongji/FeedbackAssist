# Add project specific ProGuard rules here.
# Optimization and obfuscation rules

# ========== VolumeAssist Critical Classes ==========

# Keep MainActivity for proper lifecycle
-keep public class com.feedbackassist.MainActivity {
    public protected *;
}

# Keep OverlayService - critical for foreground service
-keep class com.feedbackassist.OverlayService { *; }

# Keep OverlayView - reflection and WindowManager interaction
-keep public class com.feedbackassist.OverlayView {
    public protected *;
}

# Keep VolumeController utility
-keep public class com.feedbackassist.VolumeController {
    public *;
}

# Keep BootReceiver for BOOT_COMPLETED broadcast
-keep public class com.feedbackassist.BootReceiver {
    public protected *;
}

# Keep all public methods in our package
-keepclassmembers class com.feedbackassist.** {
    public *;
}

# ========== AndroidX & Material Design ==========

# AndroidX lifecycle
-keep class androidx.lifecycle.** { *; }

# Material Design components
-keep class com.google.android.material.** { *; }

# ========== Optimization Settings ==========

# Optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Preserve annotations
-keepattributes *Annotation*

# Keep crash reporting info
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ========== Suppress Warnings ==========

# Don't warn about missing classes
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn kotlinx.serialization.**

