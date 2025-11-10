package com.feedbackassist

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.abs


@SuppressLint("ClickableViewAccessibility")
class OverlayView(
    private val context: Context,
    private val onToggleRecord: () -> Unit
) {

    private val wm: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val overlayPrefs: SharedPreferences =
        context.getSharedPreferences("VolumeAssistOverlay", Context.MODE_PRIVATE)
    private val appPrefs: SharedPreferences =
        context.getSharedPreferences("VolumeAssistPrefs", Context.MODE_PRIVATE)

    private var bubble: View? = null
    private var isRecording: Boolean = false

    // drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var dragging = false
    private val touchSlop by lazy { (10 * context.resources.displayMetrics.density).toInt() }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val overlayUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (i?.action == "com.volumeassist.UPDATE_OVERLAY") applyStyle()
        }
    }

    fun show() {
        if (bubble != null) return

        @Suppress("InflateParams")
        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.overlay_record_bubble, null, false)

        view.setOnClickListener {
            if (dragging) return@setOnClickListener
            onToggleRecord()
        }

        view.setOnTouchListener { v, ev -> handleDragTouch(v, ev) }

        // ▼▼▼▼▼ 여기가 수정되었습니다 ▼▼▼▼▼
        wm.addView(view, createLayoutParams())
        bubble = view

        applyStyle()
        context.registerReceiver(
            overlayUpdateReceiver,
            IntentFilter("com.volumeassist.UPDATE_OVERLAY"),
            Context.RECEIVER_NOT_EXPORTED
        )

        renderState()
    }

    fun hide() {
        runCatching { context.unregisterReceiver(overlayUpdateReceiver) }
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
    }

    fun setRecordingState(recording: Boolean) {
        isRecording = recording
        renderState()
    }

    private fun renderState() {
        mainHandler.post {
            val icon = bubble?.findViewById<ImageView>(R.id.micIcon) ?: return@post
            icon.setImageResource(if (isRecording) R.drawable.ic_mic_rec else R.drawable.ic_mic_idle)
            icon.alpha = if (isRecording) 1.0f else 0.9f
        }
    }

    private fun applyStyle() {
        val color = appPrefs.getInt("overlay_color", 0xFFA8B0C0.toInt())
        val trans = appPrefs.getInt("overlay_transparency", 90)
        val size = appPrefs.getInt("overlay_size", 100)

        val alpha = (trans * 255 / 100)
        val colorWithAlpha = (color and 0x00FFFFFF) or (alpha shl 24)

        bubble?.apply {
            background?.mutate()?.setTint(colorWithAlpha)
            val scale = size / 100f
            scaleX = scale
            scaleY = scale
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val savedX = overlayPrefs.getInt("overlay_x", 800)
        val savedY = overlayPrefs.getInt("overlay_y", 500)

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags, // 수정된 플래그 적용
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
    }

    private fun handleDragTouch(view: View, event: MotionEvent): Boolean {
        val params = view.layoutParams as WindowManager.LayoutParams

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialX = params.x
                initialY = params.y
                dragging = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) dragging = true
                if (dragging) {
                    params.x = (initialX + dx).toInt()
                    params.y = (initialY + dy).toInt()
                    val dm = context.resources.displayMetrics
                    params.x = params.x.coerceIn(0, dm.widthPixels - view.width)
                    params.y = params.y.coerceIn(0, dm.heightPixels - view.height)
                    runCatching { wm.updateViewLayout(view, params) }
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    overlayPrefs.edit().putInt("overlay_x", params.x)
                        .putInt("overlay_y", params.y).apply()
                    dragging = false
                    return true
                }
                return false
            }
        }
        return false
    }
}
