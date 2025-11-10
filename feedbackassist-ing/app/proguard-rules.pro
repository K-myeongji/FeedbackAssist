# Add project specific ProGuard rules here.
# Optimization and obfuscation rules

# ========== VolumeAssist Critical Classes ==========

# Keep MainActivity for proper lifecycle
-keep public class com.volumeassist.MainActivity {
    public protected *;
}

# Keep OverlayService - critical for foreground service
-keep class com.volumeassist.OverlayService { *; }

# Keep OverlayView - reflection and WindowManager interaction
-keep public class com.volumeassist.OverlayView {
    public protected *;
}

# Keep VolumeController utility
-keep public class com.volumeassist.VolumeController {
    public *;
}

# Keep BootReceiver for BOOT_COMPLETED broadcast
-keep public class com.volumeassist.BootReceiver {
    public protected *;
}

# Keep all public methods in our package
-keepclassmembers class com.volumeassist.** {
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

