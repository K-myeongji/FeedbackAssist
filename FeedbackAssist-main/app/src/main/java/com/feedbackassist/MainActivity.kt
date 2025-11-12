package com.feedbackassist

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.feedbackassist.ServiceBus.ACTION_SERVICE_STATE
import com.feedbackassist.ServiceBus.EXTRA_RUNNING

class MainActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    private lateinit var statusText: TextView
    private lateinit var permissionStatusText: TextView
    private lateinit var permissionButton: MaterialButton
    private lateinit var toggleServiceButton: MaterialButton
    private lateinit var settingsIcon: ImageView

    private val REQ_NOTIF = 101
    private val REQ_AUDIO = 201
    private val REQ_STORAGE = 301

    private var serviceRunning: Boolean = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "오버레이 권한이 허용되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "오버레이 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_SERVICE_STATE) {
                serviceRunning = intent.getBooleanExtra(EXTRA_RUNNING, false)
                preferences.edit().putBoolean("service_running", serviceRunning).apply()
                updateUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = getSharedPreferences("VolumeAssistPrefs", MODE_PRIVATE)

        initViews()
        setupListeners()

        serviceRunning = preferences.getBoolean("service_running", false)
        updateUI()

    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ACTION_SERVICE_STATE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stateReceiver, filter)
        }
        // 앱이 다시 보일 때마다 UI 상태를 최신화합니다.
        serviceRunning = preferences.getBoolean("service_running", false)
        updateUI()
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(stateReceiver) }
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        permissionStatusText = findViewById(R.id.permissionStatusText)
        permissionButton = findViewById(R.id.permissionButton)
        toggleServiceButton = findViewById(R.id.toggleServiceButton)
        settingsIcon = findViewById(R.id.settingsIcon)
    }

    private fun setupListeners() {
        settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        permissionButton.setOnClickListener {
            if (!ensureStoragePermission()) return@setOnClickListener
            if (!ensureOverlay()) return@setOnClickListener
            if (!ensureAudio()) return@setOnClickListener
            ensureNotificationPermission()
            updateUI()
        }

        toggleServiceButton.setOnClickListener {
            if (serviceRunning) {
                stopAllServices() // 이제 모든 서비스를 중지합니다.
            } else {
                startAllServicesIfReady() // 이제 모든 서비스를 시작합니다.
            }
        }
    }

    // --- 권한 관련 함수들은 기존과 동일합니다 ---
    private fun ensureStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val ok = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!ok) {
            requestPermissions(arrayOf(permission), REQ_STORAGE)
        }
        return ok
    }

    private fun ensureOverlay(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return false
        }
        return true
    }

    private fun ensureAudio(): Boolean {
        val ok = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!ok) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        }
        return ok
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
        }
    }
    // --- 여기까지 권한 관련 함수 ---


    // ▼▼▼▼▼ 핵심 수정: 서비스 시작/종료 로직 변경 ▼▼▼▼▼

    /**
     * 모든 권한이 준비되었을 때, 오버레이와 스크린샷 감지 서비스를 모두 시작합니다.
     */
    private fun startAllServicesIfReady() {
        if (!hasAllPermissions()) {
            Toast.makeText(this, "모든 권한을 먼저 허용해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        // 1. 오버레이 서비스 시작
        val overlayIntent = Intent(this, OverlayService::class.java)
        ContextCompat.startForegroundService(this, overlayIntent)

        // 2. 스크린샷 감지 서비스 시작
        val screenshotIntent = Intent(this, ScreenshotObserverService::class.java)
        startService(screenshotIntent)

        serviceRunning = true
        preferences.edit().putBoolean("service_running", serviceRunning).apply()
        updateUI()
        Toast.makeText(this, "피드백 서비스가 시작되었습니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * 실행 중인 모든 서비스를 중지합니다.
     */
    private fun stopAllServices() {
        // 1. 오버레이 서비스 중지
        val overlayIntent = Intent(this, OverlayService::class.java)
        stopService(overlayIntent)

        // 2. 스크린샷 감지 서비스 중지
        val screenshotIntent = Intent(this, ScreenshotObserverService::class.java)
        stopService(screenshotIntent)

        serviceRunning = false
        preferences.edit().putBoolean("service_running", serviceRunning).apply()
        updateUI()
        Toast.makeText(this, "피드백 서비스가 중지되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    private fun hasAllPermissions(): Boolean {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        return hasStorage && hasOverlay && hasAudio && hasNotif
    }

    private fun updateUI() {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        statusText.text = if (serviceRunning) getString(R.string.service_running) else getString(R.string.service_stopped)

        val statusSummary = buildString {
            append("저장소 (스크린샷 감지): "); append(if (hasStorage) "✓" else "✗"); append('\n')
            append("다른 앱 위에 표시: "); append(if (hasOverlay) "✓" else "✗"); append('\n')
            append("마이크: "); append(if (hasAudio) "✓" else "✗"); append('\n')
            append("알림: "); append(if (hasNotif) "✓" else "✗")
        }
        permissionStatusText.text = statusSummary

        val allGranted = hasStorage && hasOverlay && hasAudio && hasNotif
        permissionButton.isEnabled = !allGranted
        permissionButton.text = if (allGranted) "✓ 모든 권한이 허용되었습니다" else getString(R.string.setup_permissions)

        toggleServiceButton.text = if (serviceRunning) getString(R.string.stop_service) else getString(R.string.start_service)
        toggleServiceButton.backgroundTintList = getColorStateList(if(serviceRunning) R.color.red else R.color.primary)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQ_STORAGE -> Toast.makeText(this, "저장소 권한이 거부되었습니다. 스크린샷 감지 기능이 동작하지 않습니다.", Toast.LENGTH_SHORT).show()
                REQ_AUDIO -> Toast.makeText(this, "마이크 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                REQ_NOTIF -> Toast.makeText(this, "알림 권한이 거부되었습니다 (API 33+).", Toast.LENGTH_SHORT).show()
            }
            updateUI()
            return
        }

        when (requestCode) {
            REQ_STORAGE -> Toast.makeText(this, "저장소 권한이 허용되었습니다!", Toast.LENGTH_SHORT).show()
            REQ_AUDIO -> Toast.makeText(this, "마이크 권한이 허용되었습니다!", Toast.LENGTH_SHORT).show()
            REQ_NOTIF -> Toast.makeText(this, "알림 권한이 허용되었습니다!", Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }
}
