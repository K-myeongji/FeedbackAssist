# VolumeAssist

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen" alt="Platform">
  <img src="https://img.shields.io/badge/API-33%2B-blue" alt="API">
  <img src="https://img.shields.io/badge/License-Personal-red" alt="License">
</p>

## 📥 **Download APK**

**🎉 Ready to use! No build required!**

👉 **[Download VolumeAssist v1.0 APK](releases/VolumeAssist-v1.0.apk)** 👈

*Just download, install, and you're good to go!*

---

A lightweight Android overlay app to control volume and power functions when physical buttons are not working. Perfect for devices with broken hardware buttons.

## 🎯 Features

- **Collapsed Overlay**: Minimal floating icon that stays out of your way
- **Expandable Controls**: Tap to expand full control panel
- **Volume Control**: Increase/decrease volume with hold-to-increment
- **Visual Feedback**: Real-time volume percentage display
- **Lock Screen**: Quick screen lock button
- **Flashlight Toggle**: Built-in flashlight control
- **Auto-Hide**: Automatically hides in fullscreen apps (YouTube, Netflix, Games, Camera)
- **Auto-Collapse**: Collapses after 3 seconds of inactivity
- **Draggable**: Position the collapsed icon anywhere you want
- **Haptic Feedback**: Gentle vibration on interactions
- **Auto-Start**: Option to start on boot
- **Customizable**: Choose colors, transparency, and size
- **Intro Slides**: First-time user guide
- **Battery Efficient**: Minimal resource usage
- **Safe & Sandboxed**: Won't harm your device

## 📱 Requirements

- **OS**: Android 13 (API 33) or higher
- **Permissions**: Overlay, Notification, Flashlight

## 🚀 How to Build

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK API 34

### Build Steps

1. **Open Project in Android Studio**
   ```
   File → Open → Select VolumeAssist folder
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync Gradle
   - Wait for dependencies to download

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **Install on Phone**
   - Connect phone via USB with USB Debugging enabled
   - Click "Run" or use `adb install app/build/outputs/apk/debug/app-debug.apk`

## 🎨 How to Use

1. **First Launch**
   - View intro slides explaining features
   - Grant overlay permission when prompted
   - Grant notification permission (Android 13+)
   - Enable auto-start (optional)
   - Start the service from main screen

2. **Daily Use**
   - Small floating icon appears on screen edge
   - **Tap icon** → Expands full control panel
   - **Volume Up/Down** → Press and hold for continuous adjustment
   - **Lock screen** → Quick lock button
   - **Toggle flashlight** → Instant flashlight control
   - **Panel auto-collapses** after 3 seconds
   - **Drag icon** to reposition when collapsed

3. **Customization**
   - Open settings from main screen
   - Choose from 5 color themes
   - Adjust transparency (0-100%)
   - Change overlay size (50-150%)
   - Changes apply instantly without restart

4. **Auto-Hide**
   - Overlay automatically hides during:
     - Video playback (YouTube, Netflix, Prime, etc.)
     - Gaming
     - Camera use
     - Full-screen apps

## 🔧 Customization

To add more apps to auto-hide list, edit `OverlayView.kt`:

```kotlin
private val fullscreenApps = setOf(
    "com.yourapp.package", // Add your app package name
    // ... existing apps
)
```

## 📂 Project Structure

```
VolumeAssist/
├── app/
│   ├── src/main/
│   │   ├── java/com/volumeassist/
│   │   │   ├── MainActivity.kt          # Main activity with permissions
│   │   │   ├── OverlayService.kt        # Foreground service
│   │   │   ├── OverlayView.kt           # Overlay UI & logic
│   │   │   ├── BootReceiver.kt          # Auto-start on boot
│   │   │   └── utils/
│   │   │       └── VolumeController.kt  # Volume control logic
│   │   ├── res/
│   │   │   ├── layout/                  # XML layouts
│   │   │   ├── drawable/                # Icons & shapes
│   │   │   └── values/                  # Colors, strings, themes
│   │   └── AndroidManifest.xml          # App configuration
│   └── build.gradle                     # App dependencies
├── build.gradle                         # Project configuration
└── settings.gradle                      # Project settings
```

## 🛡️ Permissions

- **SYSTEM_ALERT_WINDOW**: Draw overlay on screen
- **FOREGROUND_SERVICE**: Keep service running
- **RECEIVE_BOOT_COMPLETED**: Auto-start on boot
- **VIBRATE**: Haptic feedback
- **FLASHLIGHT/CAMERA**: Flashlight control
- **POST_NOTIFICATIONS**: Foreground service notification (Android 13+)

## ⚡ Performance

- **APK Size**: ~12 MB
- **RAM Usage**: ~25-30 MB
- **Battery Impact**: Minimal with health check monitoring
- **CPU Usage**: Near zero when idle

## 🐛 Troubleshooting

**Overlay not showing?**
- Check overlay permission in Settings → Apps → VolumeAssist → Display over other apps

**Service stops after phone restart?**
- Enable auto-start in app
- Disable battery optimization for VolumeAssist

**Lock screen not working?**
- Some devices restrict this - it's a known Android limitation
- The app tries multiple methods

**Can't build in Android Studio?**
- File → Invalidate Caches and Restart
- Clean Project, Rebuild Project

## 📝 Development Notes

- Built for devices with broken hardware buttons
- Clean, modern Material Design 3 UI
- No analytics, ads, or tracking
- 100% offline functionality
- Open source architecture

## 🔄 Future Enhancements

- [ ] Custom gestures (shake to lock, etc.)
- [ ] Volume profiles (media, ringtone, alarm)
- [ ] Screenshot button
- [ ] Customizable overlay position memory
- [ ] Multiple theme options
- [ ] Widget support

## 📄 License

This project is for personal use. Feel free to fork and modify for your own needs.

---

**Platform**: Android 13+  
**Version**: 1.0  
**Release Date**: October 2025
