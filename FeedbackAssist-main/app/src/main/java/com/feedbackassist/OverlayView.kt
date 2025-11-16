package com.feedbackassist

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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

    private val appPrefs: SharedPreferences =
        context.getSharedPreferences("VolumeAssistPrefs", Context.MODE_PRIVATE)
    private val overlayPrefs: SharedPreferences =
        context.getSharedPreferences("VolumeAssistOverlay", Context.MODE_PRIVATE)

    private var bubble: View? = null
    private var isRecording: Boolean = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var dragging = false
    private val touchSlop by lazy { (10 * context.resources.displayMetrics.density).toInt() }
    private var originalSize: Int = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    fun show() {
        if (bubble != null) return

        @Suppress("InflateParams")
        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.overlay_record_bubble, null, false)

        originalSize = try {
            context.resources.getDimensionPixelSize(R.dimen.overlay_bubble_size)
        } catch (e: Exception) {
            (64 * context.resources.displayMetrics.density).toInt()
        }

        view.setOnClickListener {
            if (dragging) return@setOnClickListener
            onToggleRecord()
        }
        view.setOnTouchListener { v, ev -> handleDragTouch(v, ev) }

        bubble = view
        wm.addView(view, createLayoutParams())

        applyStyle()

        renderState()
    }

    fun hide() {
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
            // 투명도는 이제 applyStyle에서 뷰 전체에 적용하므로, 아이콘 개별 알파는 제거합니다.
            // icon.alpha = if (isRecording) 1.0f else 0.9f
        }
    }

    // 이 함수는 외부(OverlayService)에서 호출되어야 하므로 public으로 유지합니다.
    fun applyStyle() {
        val color = appPrefs.getInt("overlay_color", 0xFFA8B0C0.toInt())
        val trans = appPrefs.getInt("overlay_transparency", 90)
        val sizePercent = appPrefs.getInt("overlay_size", 100)

        val view = bubble ?: return

        // ----- 크기 계산 -----
        val sizePx = (originalSize * (sizePercent / 100f)).toInt().coerceAtLeast(40)

        // 레이아웃 크기 반영
        val lp = view.layoutParams as WindowManager.LayoutParams
        if (lp.width != sizePx || lp.height != sizePx) {
            lp.width = sizePx
            lp.height = sizePx
            runCatching { wm.updateViewLayout(view, lp) }
        }

        // ----- 색 + 투명도 계산 -----
        val alpha = (trans * 255 / 100).coerceIn(0, 255)
        val argb = (alpha shl 24) or (color and 0x00FFFFFF.toInt())

        // ✅ 기존 배경은 버리고, 우리가 원하는 원형 배경을 새로 만든다.
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(argb)
        }
        view.background = bg
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
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
    }

    // handleDragTouch 함수는 수정할 필요가 없습니다.
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
                val wasDragging = dragging
                if (dragging) {
                    overlayPrefs.edit().putInt("overlay_x", params.x).putInt("overlay_y", params.y).apply()
                }
                dragging = false
                return wasDragging
            }
        }
        return false
    }

}
