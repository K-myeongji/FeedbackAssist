package com.feedbackassist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.feedbackassist.ServiceBus.ACTION_SERVICE_STATE
import com.feedbackassist.ServiceBus.ACTION_STOP_SERVICE
import com.feedbackassist.ServiceBus.ACTION_UPDATE_OVERLAY
import com.feedbackassist.ServiceBus.EXTRA_RUNNING
import java.io.File


class OverlayService : Service() {

    private var overlayView: OverlayView? = null
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    private lateinit var appPrefs: SharedPreferences
    private lateinit var mainHandler: Handler

    private val channelId = "VolumeAssistChannel"
    private val notifId = 1

    private var currentUri: Uri? = null
    private var pendingDisplayName: String = ""

    private val overlayUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 액션 체크까지 넣고 싶으면:
            if (intent?.action == ACTION_UPDATE_OVERLAY) {
                overlayView?.applyStyle()
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        mainHandler = Handler(Looper.getMainLooper())
        appPrefs = getSharedPreferences("VolumeAssistPrefs", MODE_PRIVATE)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // ✅ 1) 오버레이 버블 생성
        overlayView = OverlayView(this) { toggleRecording() }.also { it.show() }
        // 처음 한 번 현재 설정값 적용
        overlayView?.applyStyle()

        // ✅ 2) 브로드캐스트 리시버 등록
        val filter = IntentFilter(ACTION_UPDATE_OVERLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(overlayUpdateReceiver, filter)
        }

        appPrefs.registerOnSharedPreferenceChangeListener(prefListener)

        createNotificationChannel()
        startForeground(notifId, buildNotification("Ready"))

        appPrefs.edit().putBoolean("service_running", true).apply()
        sendStateBroadcast(true)

        val screenshotServiceIntent = Intent(this, ScreenshotObserverService::class.java)
        startService(screenshotServiceIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_RECORD -> toggleRecording()
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording(force = true)
        overlayView?.hide()
        overlayView = null

        appPrefs.edit().putBoolean("service_running", false).apply()
        sendStateBroadcast(false)

        runCatching { unregisterReceiver(overlayUpdateReceiver) }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            playTriggerBeep { startRecording() }
        }
    }

    private fun playTriggerBeep(after: () -> Unit) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            mainHandler.postDelayed({
                runCatching { tg.release() }
                after()
            }, 200)
        } catch (_: Exception) {
            after()
        }
    }

    private fun startRecording() {
        try {
            recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()

            val base = "VA_${System.currentTimeMillis()}"
            pendingDisplayName = "$base.m4a"

            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, pendingDisplayName)
                    put(MediaStore.Downloads.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FeedbackAssist")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Failed to create MediaStore item for recording.")
                currentUri = uri

                val pfd = contentResolver.openFileDescriptor(uri, "w")
                    ?: throw IllegalStateException("Failed to open ParcelFileDescriptor.")

                recorder!!.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(pfd.fileDescriptor)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                    prepare()
                    start()
                }
            } else {
                val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "FeedbackAssist")
                if (!dir.exists()) dir.mkdirs()
                val outputFile = File(dir, pendingDisplayName)
                currentUri = null

                recorder!!.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(outputFile.absolutePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                    prepare()
                    start()
                }
            }

            isRecording = true
            overlayView?.setRecordingState(true)
            updateNotification("Recording…")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음 시작 실패: " + e.message, Toast.LENGTH_LONG).show()
            stopRecording(force = true)
        }
    }

    private fun stopRecording(force: Boolean = false) {
        runCatching { recorder?.stop() }
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null

        val wasRecording = isRecording
        isRecording = false
        overlayView?.setRecordingState(false)
        updateNotification("Ready")

        if (!force && Build.VERSION.SDK_INT >= 29) {
            currentUri?.let { uri ->
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    contentResolver.update(uri, values, null, null)
                }
            }
        }

        if (wasRecording && !force) {
            Toast.makeText(this, "녹음이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            currentUri?.let { uri ->
                val i = Intent(this, RenameActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(RenameActivity.EXTRA_URI, uri.toString())
                    putExtra(RenameActivity.EXTRA_DEFAULT_NAME, "")
                }
                startActivity(i)
            }
        }

        currentUri = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendStateBroadcast(running: Boolean) {
        sendBroadcast(Intent(ACTION_SERVICE_STATE).putExtra(EXTRA_RUNNING, running))
    }

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "overlay_color" ||
                key == "overlay_transparency" ||
                key == "overlay_size"
            ) {
                // 설정이 바뀔 때마다 버블 스타일 다시 적용
                overlayView?.applyStyle()
            }
        }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            "VolumeAssist",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Recording overlay"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val toggleRecord = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).apply { action = ACTION_TOGGLE_RECORD },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopService = PendingIntent.getService(
            this, 2,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(if (isRecording) R.drawable.ic_mic_rec else R.drawable.ic_mic_idle)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp)
            .addAction(
                if (isRecording) R.drawable.ic_stop else R.drawable.ic_mic_idle,
                if (isRecording) "Stop Rec" else "Record",
                toggleRecord
            )
            .addAction(R.drawable.ic_close, "Stop Service", stopService)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, buildNotification(text))
    }

    companion object {
        // MainActivity의 알림 클릭 시 전달되는 Action과 구분하기 위해 이름을 다르게 사용
        private const val ACTION_TOGGLE_RECORD = "com.feedbackassist.TOGGLE_RECORD_FROM_NOTIF"
    }
}
