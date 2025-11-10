package com.feedbackassist

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import com.feedbackassist.store.Store
import java.io.File
import java.io.IOException

class ScreenshotObserverService : LifecycleService() {

    private val TAG = "ScreenshotService"
    private var observer: FileObserver? = null
    // 중복 실행을 막기 위해 최근 처리한 파일 경로를 저장
    private var lastProcessedPath: String? = null
    private var lastProcessedTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        val screenshotsDir = getScreenshotsDirectory()
        if (screenshotsDir == null) {
            Log.e(TAG, "스크린샷 폴더를 찾을 수 없어 서비스를 종료합니다.")
            stopSelf()
            return
        }
        Log.d(TAG, "감시 시작 폴더: ${screenshotsDir.absolutePath}")

        val eventToWatch = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO

        observer = object : FileObserver(screenshotsDir, eventToWatch) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return

                if (path.startsWith(".pending-")) {
                    return
                }

                val currentTime = System.currentTimeMillis()
                if (path == lastProcessedPath && (currentTime - lastProcessedTime) < 2000) {
                    return
                }
                lastProcessedPath = path
                lastProcessedTime = currentTime

                // 이벤트 타입에 따라 로그 출력
                val eventName = when (event) {
                    CLOSE_WRITE -> "CLOSE_WRITE"
                    MOVED_TO -> "MOVED_TO"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "감지된 이벤트: $eventName, 파일: $path")

                // 스크린샷 파일 복사 실행
                val screenshotFile = File(screenshotsDir, path)
                if (screenshotFile.exists()) {
                    copyScreenshotToAppDirectory(screenshotFile)
                }
            }
        }
        observer?.startWatching()
    }


    private fun copyScreenshotToAppDirectory(originalFile: File) {
        val tempDisplayName = "temp_screenshot_${System.currentTimeMillis()}.jpg"
        val mimeType = "image/jpeg"

        val (uri, outputStream) = Store.createInDownloads(this, tempDisplayName, mimeType)

        if (outputStream == null) {
            Log.e(TAG, "스크린샷 복사 실패: 출력 스트림을 열 수 없습니다.")
            return
        }

        try {
            originalFile.inputStream().use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "스크린샷을 임시 파일로 복사 완료: $tempDisplayName")

            // ▼▼▼▼▼ 핵심 수정: FeedbackActivity에 '임시 URI'를 전달 ▼▼▼▼▼
            if (uri != null) {
                // 이제 launchFeedbackActivity는 URI만 전달합니다.
                launchFeedbackActivity(uri)
            }

        } catch (e: IOException) {
            Log.e(TAG, "스크린샷 복사 중 오류 발생", e)
        }
    }

    private fun launchFeedbackActivity(tempScreenshotUri: Uri) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "스크린샷 감지! 피드백을 기록해주세요.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, FeedbackActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("temp_screenshot_uri", tempScreenshotUri.toString())
            }
            startActivity(intent)
        }
    }


    private fun getScreenshotsDirectory(): File? {
        val publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val screenshotsPath = File(publicPicturesDir, "Screenshots")
        if (screenshotsPath.exists() && screenshotsPath.isDirectory) return screenshotsPath

        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val screenshotsDcimPath = File(dcimDir, "Screenshots")
        if (screenshotsDcimPath.exists() && screenshotsDcimPath.isDirectory) return screenshotsDcimPath

        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        observer?.stopWatching()
        Log.d(TAG, "ScreenshotObserverService: onDestroy")
    }
}

