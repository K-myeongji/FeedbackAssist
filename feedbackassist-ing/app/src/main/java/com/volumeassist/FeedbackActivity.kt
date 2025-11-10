package com.volumeassist

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log // Log 임포트는 이미 되어있습니다.
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.volumeassist.store.Store
import java.io.IOException

class FeedbackActivity : AppCompatActivity() {

    // ▼▼▼ 로그를 필터링하기 쉽도록 TAG를 명확하게 정의합니다. ▼▼▼
    private val TAG = "KBD_DEBUG" // Keyboard Debug

    private lateinit var screenshotImageView: ImageView
    private lateinit var titleEditText: EditText
    private lateinit var feedbackEditText: EditText
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var tempScreenshotUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "FeedbackActivity: onCreate 시작") // 1. 액티비티 시작 확인
        setContentView(R.layout.activity_feedback)
        Log.d(TAG, "FeedbackActivity: setContentView 완료") // 2. 레이아웃 로드 확인

        screenshotImageView = findViewById(R.id.screenshotImageView)
        titleEditText = findViewById(R.id.titleEditText)
        feedbackEditText = findViewById(R.id.feedbackEditText)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        Log.d(TAG, "FeedbackActivity: 뷰 초기화 완료") // 3. 뷰 ID 찾기 확인

        loadScreenshotInfo(intent)

        titleEditText.isFocusable = true
        titleEditText.isFocusableInTouchMode = true
        titleEditText.visibility = View.VISIBLE
        Log.d(TAG, "FeedbackActivity: EditText 포커스 속성 설정 완료") // 4. 포커스 속성 설정 확인

        // ▼▼▼ 키보드 호출 로직에 로그를 집중 추가합니다. ▼▼▼
        Log.d(TAG, "FeedbackActivity: Handler.postDelayed 호출 직전")
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "FeedbackActivity: postDelayed 내부 실행 시작 (150ms 후)") // 5. 지연된 코드가 실행되는지 확인

            titleEditText.requestFocus()
            val hasFocus = titleEditText.hasFocus()
            Log.d(TAG, "FeedbackActivity: requestFocus() 호출. 포커스 상태: $hasFocus") // 6. 실제로 포커스를 받았는지 확인

            if (hasFocus) {
                Log.d(TAG, "FeedbackActivity: 포커스 확인, imm.showSoftInput 호출 시도")
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val result = imm.showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
                Log.d(TAG, "FeedbackActivity: imm.showSoftInput 결과: $result") // 7. 키보드 표시 함수의 결과 확인 (true/false)
            } else {
                Log.e(TAG, "FeedbackActivity: EditText가 포커스를 받지 못했습니다! 키보드를 호출할 수 없습니다.")
            }
        }, 150)

        saveButton.setOnClickListener { /* ... 기존 코드 ... */ }
        cancelButton.setOnClickListener { /* ... 기존 코드 ... */ }
    }

    private fun loadScreenshotInfo(intent: Intent?) {
        Log.d(TAG, "FeedbackActivity: loadScreenshotInfo 시작")
        intent?.getStringExtra("temp_screenshot_uri")?.let {
            val uri = Uri.parse(it)
            tempScreenshotUri = uri
            screenshotImageView.setImageURI(uri)
            Log.d(TAG, "FeedbackActivity: 스크린샷 이미지 로드 완료, URI: $uri")
        }
    }

    // renameScreenshot, saveTextToFile 함수는 기존 코드와 동일합니다.
    private fun renameScreenshot(finalName: String) {
        val imageUri = tempScreenshotUri ?: return
        val newDisplayName = "$finalName.jpg"

        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
                put(MediaStore.Images.Media.DISPLAY_NAME, newDisplayName)
            }
            val updatedRows = contentResolver.update(imageUri, values, null, null)
            if (updatedRows > 0) {
                Log.d(TAG, "스크린샷 이름 변경 성공: $newDisplayName")
            } else {
                Log.e(TAG, "스크린샷 이름 변경 실패.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "스크린샷 이름 변경 중 오류", e)
        }
    }

    private fun saveTextToFile(fileName: String, content: String) {
        val displayName = "$fileName.txt"
        val mimeType = "text/plain"
        val fullText = "제목: $fileName\n\n내용:\n$content"

        val (uri, outputStream) = Store.createInDownloads(this, displayName, mimeType)
        if (outputStream == null) {
            Log.e(TAG, "텍스트 파일 저장 실패 (Stream=null)")
            return
        }

        try {
            outputStream.use { it.write(fullText.toByteArray()) }
            uri?.let { Store.finalizePending(this, it) }
            Log.d(TAG, "텍스트 파일 저장 성공: $displayName")
        } catch (e: IOException) {
            Log.e(TAG, "텍스트 파일 저장 중 오류", e)
        }
    }
}
