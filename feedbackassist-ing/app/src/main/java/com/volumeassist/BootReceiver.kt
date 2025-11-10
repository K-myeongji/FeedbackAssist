package com.volumeassist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if auto-start is enabled
            val preferences = context.getSharedPreferences("VolumeAssistPrefs", Context.MODE_PRIVATE)
            val autoStart = preferences.getBoolean("auto_start", false)
            
            // Only start if auto-start enabled AND overlay permission granted
            if (autoStart && Settings.canDrawOverlays(context)) {
                try {
                    // Start the overlay service
                    val serviceIntent = Intent(context, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
