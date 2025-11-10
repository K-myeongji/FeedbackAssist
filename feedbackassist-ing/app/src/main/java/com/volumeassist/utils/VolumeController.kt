package com.volumeassist.utils

import android.content.Context
import android.media.AudioManager

class VolumeController(context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val streamType = AudioManager.STREAM_MUSIC

    /**
     * Get current volume as percentage (0-100)
     */
    fun getCurrentVolume(): Int {
        val currentVolume = audioManager.getStreamVolume(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        return if (maxVolume > 0) {
            (currentVolume * 100) / maxVolume
        } else {
            0
        }
    }

    /**
     * Set volume as percentage (0-100)
     */
    fun setVolume(volumePercent: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val volume = (volumePercent * maxVolume) / 100
        audioManager.setStreamVolume(
            streamType,
            volume.coerceIn(0, maxVolume),
            0 // No UI flags
        )
    }

    /**
     * Increase volume by one step
     */
    fun increaseVolume() {
        audioManager.adjustStreamVolume(
            streamType,
            AudioManager.ADJUST_RAISE,
            0 // No UI flags
        )
    }

    /**
     * Increase volume with system UI shown
     */
    fun increaseVolumeWithUI() {
        audioManager.adjustStreamVolume(
            streamType,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI // Show system volume UI
        )
    }

    /**
     * Decrease volume by one step
     */
    fun decreaseVolume() {
        audioManager.adjustStreamVolume(
            streamType,
            AudioManager.ADJUST_LOWER,
            0 // No UI flags
        )
    }

    /**
     * Decrease volume with system UI shown
     */
    fun decreaseVolumeWithUI() {
        audioManager.adjustStreamVolume(
            streamType,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI // Show system volume UI
        )
    }

    /**
     * Mute/unmute volume
     */
    fun toggleMute() {
        val currentVolume = audioManager.getStreamVolume(streamType)
        if (currentVolume > 0) {
            // Mute
            audioManager.setStreamVolume(streamType, 0, 0)
        } else {
            // Unmute to 50%
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            audioManager.setStreamVolume(streamType, maxVolume / 2, 0)
        }
    }

    /**
     * Get maximum volume level
     */
    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(streamType)
    }

    /**
     * Check if volume is muted
     */
    fun isMuted(): Boolean {
        return audioManager.getStreamVolume(streamType) == 0
    }
}
