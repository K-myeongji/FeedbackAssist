////package com.feedbackassist
////
////import android.content.Intent
////import android.net.Uri
////import android.os.Environment
////import android.os.FileObserver
////import android.os.Handler
////import android.os.Looper
////import android.util.Log
////import android.widget.Toast
////import androidx.lifecycle.LifecycleService
////import com.feedbackassist.store.Store
////import java.io.File
////import java.io.IOException
////
////class ScreenshotObserverService : LifecycleService() {
////
////    private val TAG = "ScreenshotService"
////    private var observer: FileObserver? = null
////    // 중복 실행을 막기 위해 최근 처리한 파일 경로를 저장
////    private var lastProcessedPath: String? = null
////    private var lastProcessedTime: Long = 0
////
////    override fun onCreate() {
////        super.onCreate()
////        val screenshotsDir = getScreenshotsDirectory()
////        if (screenshotsDir == null) {
////            Log.e(TAG, "스크린샷 폴더를 찾을 수 없어 서비스를 종료합니다.")
////            stopSelf()
////            return
////        }
////        Log.d(TAG, "감시 시작 폴더: ${screenshotsDir.absolutePath}")
////
////        val eventToWatch = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO
////
////        observer = object : FileObserver(screenshotsDir, eventToWatch) {
////            override fun onEvent(event: Int, path: String?) {
////                if (path == null) return
////
////                if (path.startsWith("thumbnail_")) return
////
////                if (path.startsWith(".pending-")) {
////                    return
////                }
////
////                val currentTime = System.currentTimeMillis()
////                if (path == lastProcessedPath && (currentTime - lastProcessedTime) < 2000) {
////                    return
////                }
////                lastProcessedPath = path
////                lastProcessedTime = currentTime
////
////                // 이벤트 타입에 따라 로그 출력
////                val eventName = when (event) {
////                    CLOSE_WRITE -> "CLOSE_WRITE"
////                    MOVED_TO -> "MOVED_TO"
////                    else -> "UNKNOWN"
////                }
////                Log.d(TAG, "감지된 이벤트: $eventName, 파일: $path")
////
////                // 스크린샷 파일 복사 실행
////                val screenshotFile = File(screenshotsDir, path)
////                if (screenshotFile.exists()) {
////                    copyScreenshotToAppDirectory(screenshotFile)
////                }
////            }
////        }
////        observer?.startWatching()
////    }
////
////
////    private fun copyScreenshotToAppDirectory(originalFile: File) {
////        val tempDisplayName = "temp_screenshot_${System.currentTimeMillis()}.jpg"
////        val mimeType = "image/jpeg"
////
////        val (uri, outputStream) = Store.createInDownloads(this, tempDisplayName, mimeType)
////
////        if (outputStream == null) {
////            Log.e(TAG, "스크린샷 복사 실패: 출력 스트림을 열 수 없습니다.")
////            return
////        }
////
////        try {
////            originalFile.inputStream().use { input ->
////                outputStream.use { output ->
////                    input.copyTo(output)
////                }
////            }
////
////            Log.d(TAG, "스크린샷을 임시 파일로 복사 완료: $tempDisplayName")
////
////            // ▼▼▼▼▼ 핵심 수정: FeedbackActivity에 '임시 URI'를 전달 ▼▼▼▼▼
////            if (uri != null) {
////                // 이제 launchFeedbackActivity는 URI만 전달합니다.
////                launchFeedbackActivity(uri)
////            }
////
////        } catch (e: IOException) {
////            Log.e(TAG, "스크린샷 복사 중 오류 발생", e)
////        }
////    }
////
////    private fun launchFeedbackActivity(tempScreenshotUri: Uri) {
////        Handler(Looper.getMainLooper()).post {
////            Toast.makeText(applicationContext, "스크린샷 감지! 피드백을 기록해주세요.", Toast.LENGTH_LONG).show()
////
////            val intent = Intent(this, FeedbackActivity::class.java).apply {
////                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
////                putExtra("temp_screenshot_uri", tempScreenshotUri.toString())
////            }
////            startActivity(intent)
////        }
////    }
////
////
////    private fun getScreenshotsDirectory(): File? {
////        val publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
////        val screenshotsPath = File(publicPicturesDir, "Screenshots")
////        if (screenshotsPath.exists() && screenshotsPath.isDirectory) return screenshotsPath
////
////        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
////        val screenshotsDcimPath = File(dcimDir, "Screenshots")
////        if (screenshotsDcimPath.exists() && screenshotsDcimPath.isDirectory) return screenshotsDcimPath
////
////        return null
////    }
////
////    override fun onDestroy() {
////        super.onDestroy()
////        observer?.stopWatching()
////        Log.d(TAG, "ScreenshotObserverService: onDestroy")
////    }
////}


// 이게 4번 뜨는 버전


//package com.feedbackassist
//
//import android.content.ContentResolver
//import android.content.ContentValues
//import android.content.Intent
//import android.database.ContentObserver
//import android.net.Uri
//import android.os.Handler
//import android.os.Looper
//import android.provider.MediaStore
//import android.util.Log
//import android.widget.Toast
//import androidx.lifecycle.LifecycleService
//
//class ScreenshotObserverService : LifecycleService() {
//
//    private val TAG = "ScreenshotObserverService"
//    private var contentObserver: ContentObserver? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        startScreenshotObserver()
//    }
//
//    /**
//     * MediaStore 기반 스크린샷 감지 (모든 갤럭시에 대응)
//     */
//    private fun startScreenshotObserver() {
//        val resolver: ContentResolver = contentResolver
//
//        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
//            override fun onChange(selfChange: Boolean, uri: Uri?) {
//                super.onChange(selfChange, uri)
//
//                if (uri == null) return
//
//                Log.d(TAG, "📡 MediaStore 변경 감지됨: $uri")
//
//                val projection = arrayOf(
//                    MediaStore.Images.Media.DISPLAY_NAME,
//                    MediaStore.Images.Media.DATE_ADDED,
//                    MediaStore.Images.Media._ID
//                )
//
//                resolver.query(uri, projection, null, null, null)?.use { cursor ->
//                    if (!cursor.moveToFirst()) return
//
//                    val name = cursor.getString(0) ?: return
//                    val timestamp = cursor.getLong(1)
//
//                    // 파일명으로 스크린샷 판정
//                    if (!isScreenshotName(name)) {
//                        return
//                    }
//
//                    Log.d(
//                        TAG,
//                        "📸 스크린샷 감지됨 → name=$name, date=$timestamp"
//                    )
//
//                    launchFeedbackActivity(uri)
//                }
//            }
//        }
//
//        resolver.registerContentObserver(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            true,
//            contentObserver!!
//        )
//
//        Log.d(TAG, "📡 MediaStore Screenshot 감시 시작됨")
//    }
//
//    /**
//     * 스크린샷 파일명 패턴 매칭
//     */
//    private fun isScreenshotName(name: String): Boolean {
//        val lower = name.lowercase()
//        return lower.contains("screenshot") ||
//                lower.contains("capture") ||
//                lower.contains("스크린샷") ||
//                lower.contains("screen_shot") ||
//                lower.contains("screen-shot")
//    }
//
//    /**
//     * FeedbackActivity 실행
//     */
//    private fun launchFeedbackActivity(uri: Uri) {
//        Handler(Looper.getMainLooper()).post {
//            Toast.makeText(
//                applicationContext,
//                "스크린샷 감지! 피드백을 기록해주세요.",
//                Toast.LENGTH_LONG
//            ).show()
//
//            val intent = Intent(this, FeedbackActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                putExtra("temp_screenshot_uri", uri.toString())
//            }
//            startActivity(intent)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        contentObserver?.let {
//            contentResolver.unregisterContentObserver(it)
//        }
//        Log.d(TAG, "🛑 ScreenshotObserverService 종료됨")
//    }
//}
package com.feedbackassist

import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import com.feedbackassist.store.Store
import java.io.IOException

class ScreenshotObserverService : LifecycleService() {

    private val TAG = "ScreenshotObserverService"
    private var contentObserver: ContentObserver? = null

    // 중복 호출 방지
    private var lastProcessedUri: String? = null
    private var lastProcessedTime: Long = 0
    private val DEBOUNCE_TIME_MS = 3000L  // 🔥 3초로 증가

    // 🔥 처리 중 플래그 추가
    private var isProcessing = false

    override fun onCreate() {
        super.onCreate()
        startScreenshotObserver()
    }

    private fun startScreenshotObserver() {
        val resolver: ContentResolver = contentResolver

        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                if (uri == null) return

                // 🔥 이미 처리 중이면 무시
                if (isProcessing) {
                    Log.d(TAG, "⏭️ 이미 처리 중이므로 무시: $uri")
                    return
                }

                Log.d(TAG, "📡 MediaStore 변경 감지됨: $uri")

                // 중복 호출 체크
                val currentTime = System.currentTimeMillis()
                if (uri.toString() == lastProcessedUri &&
                    currentTime - lastProcessedTime < DEBOUNCE_TIME_MS) {
                    Log.d(TAG, "⏭️ 중복 호출 무시: $uri")
                    return
                }

                val projection = arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media._ID
                )

                resolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return

                    val name = cursor.getString(0) ?: return
                    val timestamp = cursor.getLong(1)

                    // 파일명으로 스크린샷 판정
                    if (!isScreenshotName(name)) {
                        return
                    }

                    Log.d(TAG, "📸 스크린샷 감지됨 → name=$name, date=$timestamp")

                    // 🔥 처리 시작
                    isProcessing = true
                    lastProcessedUri = uri.toString()
                    lastProcessedTime = currentTime

                    // 파일이 완전히 생성될 때까지 대기 후 복사
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            copyScreenshotToAppDirectory(uri)
                        } finally {
                            // 🔥 처리 완료 후 플래그 해제
                            Handler(Looper.getMainLooper()).postDelayed({
                                isProcessing = false
                                Log.d(TAG, "✅ 처리 완료, 다음 스크린샷 대기 중")
                            }, 1000)
                        }
                    }, 500)
                }
            }
        }

        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )

        Log.d(TAG, "📡 MediaStore Screenshot 감시 시작됨")
    }

    private fun isScreenshotName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("screenshot") ||
                lower.contains("capture") ||
                lower.contains("스크린샷") ||
                lower.contains("screen_shot") ||
                lower.contains("screen-shot")
    }

    private fun copyScreenshotToAppDirectory(originalUri: Uri) {
        val tempDisplayName = "temp_screenshot_${System.currentTimeMillis()}.jpg"
        val mimeType = "image/jpeg"

        val (uri, outputStream) = Store.createInDownloads(this, tempDisplayName, mimeType)

        if (outputStream == null) {
            Log.e(TAG, "❌ 스크린샷 복사 실패: 출력 스트림을 열 수 없습니다.")
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(originalUri)

            if (inputStream == null) {
                Log.e(TAG, "❌ 원본 스크린샷을 열 수 없습니다: $originalUri")
                outputStream.close()
                return
            }

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "✅ 스크린샷을 임시 파일로 복사 완료: $tempDisplayName")

            if (uri != null) {
                launchFeedbackActivity(uri)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ 권한 오류: 스크린샷 접근 불가", e)
        } catch (e: IOException) {
            Log.e(TAG, "❌ 스크린샷 복사 중 오류 발생", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 예상치 못한 오류", e)
        }
    }

    private fun launchFeedbackActivity(tempScreenshotUri: Uri) {
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(
                    applicationContext,
                    "스크린샷 감지! 피드백을 기록해주세요.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(this, FeedbackActivity::class.java).apply {
                    // 🔥 FLAG 수정: 기존 Activity가 있으면 재사용
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("temp_screenshot_uri", tempScreenshotUri.toString())
                }
                startActivity(intent)

                Log.d(TAG, "✅ FeedbackActivity 실행 완료")
            } catch (e: Exception) {
                Log.e(TAG, "❌ FeedbackActivity 실행 실패", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        isProcessing = false
        Log.d(TAG, "🛑 ScreenshotObserverService 종료됨")
    }
}