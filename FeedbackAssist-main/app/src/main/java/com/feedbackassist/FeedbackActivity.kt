package com.feedbackassist

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.feedbackassist.store.Store
import java.io.IOException

class FeedbackActivity : AppCompatActivity() {

    private val TAG = "FeedbackActivity"

    private lateinit var screenshotImageView: ImageView
    private lateinit var titleEditText: EditText
    private lateinit var feedbackEditText: EditText
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var tempScreenshotUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        screenshotImageView = findViewById(R.id.screenshotImageView)
        titleEditText = findViewById(R.id.titleEditText)
        feedbackEditText = findViewById(R.id.feedbackEditText)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        loadScreenshotInfo(intent)

        // 키보드 호출 로직 (정상 동작)
        Handler(Looper.getMainLooper()).postDelayed({
            titleEditText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 150)


        // ▼▼▼▼▼ 여기가 모든 문제를 해결하는 코드입니다 (저장/취소 기능 복원) ▼▼▼▼▼
        saveButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val content = feedbackEditText.text.toString().trim()

            if (title.isBlank()) {
                Toast.makeText(this, "피드백 제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 텍스트 파일과 스크린샷 이미지 저장
            saveTextToFile(title, content)
            renameScreenshot(title)

            Toast.makeText(this, "'$title' 관련 파일이 저장되었습니다.", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_OK) // 성공 결과 설정
            finish() // 액티비티 종료
        }

        cancelButton.setOnClickListener {
            // 임시로 생성했던 스크린샷 파일 삭제
            tempScreenshotUri?.let {
                try {
                    contentResolver.delete(it, null, null)
                    Log.d(TAG, "임시 스크린샷 파일 삭제 성공: $it")
                } catch (e: Exception) {
                    Log.e(TAG, "임시 스크린샷 파일 삭제 실패", e)
                }
            }
            Toast.makeText(this, "피드백 작성을 취소했습니다.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED) // 취소 결과 설정
            finish() // 액티비티 종료
        }
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
    }

    private fun loadScreenshotInfo(intent: Intent?) {
        intent?.getStringExtra("temp_screenshot_uri")?.let {
            val uri = Uri.parse(it)
            tempScreenshotUri = uri
            screenshotImageView.setImageURI(uri)
        }
    }

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
