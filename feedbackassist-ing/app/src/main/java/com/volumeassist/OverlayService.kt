package com.volumeassist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import com.volumeassist.ServiceBus.ACTION_SERVICE_STATE
import com.volumeassist.ServiceBus.ACTION_STOP_SERVICE
import com.volumeassist.ServiceBus.EXTRA_RUNNING
import java.io.File

class OverlayService : Service() {

    private var overlayView: OverlayView? = null
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    private lateinit var appPrefs: SharedPreferences
    private lateinit var mainHandler: Handler

    private val channelId = "VolumeAssistChannel"
    private val notifId = 1

    // MediaStore(Downloads) 항목 URI
    private var currentUri: Uri? = null
    private var pendingDisplayName: String = ""

    override fun onCreate() {
        super.onCreate()
        mainHandler = Handler(Looper.getMainLooper())
        appPrefs = getSharedPreferences("VolumeAssistPrefs", MODE_PRIVATE)

        // 오버레이 권한 없으면 즉시 종료
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // 알림 채널 + 포그라운드 시작
        createNotificationChannel()
        startForeground(notifId, buildNotification("Ready"))

        // 오버레이 표시 (버튼 탭 → 녹음 토글)
        overlayView = OverlayView(this) { toggleRecording() }.also { it.show() }

        // 상태 저장 + 브로드캐스트
        appPrefs.edit().putBoolean("service_running", true).apply()
        sendStateBroadcast(true)
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

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            // 삑- 소리 후 약간의 지연 뒤 녹음 시작
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

    /** Downloads/FeedbackAssist 로 저장 (API29+는 MediaStore, 그 미만은 앱 전용 Download 폴더) */
    private fun startRecording() {
        try {
            recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()

            val base = "VA_${System.currentTimeMillis()}"
            pendingDisplayName = "$base.m4a"

            if (Build.VERSION.SDK_INT >= 29) {
                // ✅ 공개 저장소: Downloads/FeedbackAssist
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, pendingDisplayName)
                    put(MediaStore.Downloads.MIME_TYPE, "audio/mp4")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/FeedbackAssist"
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: throw IllegalStateException("Failed to create MediaStore item")
                currentUri = uri

                val pfd = contentResolver.openFileDescriptor(uri, "w")!!
                recorder!!.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                    setOutputFile(pfd.fileDescriptor)
                    prepare()
                    start()
                }
                pfd.close()
            } else {
                // ✅ Android 9 이하: 앱 전용 Download/FeedbackAssist
                val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "FeedbackAssist")
                if (!dir.exists()) dir.mkdirs()
                val out = File(dir, pendingDisplayName)
                currentUri = null

                recorder!!.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                    setOutputFile(out.absolutePath)
                    prepare()
                    start()
                }
            }

            isRecording = true
            overlayView?.setRecordingState(true)
            updateNotification("Recording…")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
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

        // MediaStore 마무리 (API29+): IS_PENDING → 0
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
            // ✅ 파일명 입력 화면 호출 (API29+만: MediaStore 항목명 변경)
            currentUri?.let { uri ->
                val i = Intent(this, RenameActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(RenameActivity.EXTRA_URI, uri.toString())
                    putExtra(RenameActivity.EXTRA_DEFAULT_NAME, pendingDisplayName)
                }
                startActivity(i)
            }
        }

        currentUri = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording(force = true)
        overlayView?.hide()
        overlayView = null

        // 상태 저장 + 브로드캐스트
        appPrefs.edit().putBoolean("service_running", false).apply()
        sendStateBroadcast(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 상태 브로드캐스트
    private fun sendStateBroadcast(running: Boolean) {
        sendBroadcast(Intent(ACTION_SERVICE_STATE).putExtra(EXTRA_RUNNING, running))
    }

    // ---------- Notification ----------
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
        private const val ACTION_TOGGLE_RECORD = "TOGGLE_RECORD"
    }
}