package com.volumeassist

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
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class RenameActivity : AppCompatActivity() {

    private val TAG = "RenameActivity"

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_DEFAULT_NAME = "default_name"
        const val EXTRA_HINT_MESSAGE = "hint_message"
        const val RESULT_EXTRA_NAME = "file_name"
    }

    private var fileUri: Uri? = null
    private lateinit var nameEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rename)

        nameEditText = findViewById(R.id.editName)
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)
        val cancelButton = findViewById<MaterialButton>(R.id.cancelButton)

        intent.getStringExtra(EXTRA_URI)?.let {
            fileUri = Uri.parse(it)
        }

        val defaultName = intent.getStringExtra(EXTRA_DEFAULT_NAME)
        val hint = intent.getStringExtra(EXTRA_HINT_MESSAGE) ?: "파일 이름을 입력하세요"

        val cleanName = defaultName?.substringBeforeLast('.')
        nameEditText.setText(cleanName)
        nameEditText.hint = hint

        // ▼▼▼▼▼ 최후의 해결책 ▼▼▼▼▼
        // EditText가 포커스를 받을 수 있도록 모든 속성을 코드로 강제하고,
        // Handler를 사용해 약간의 지연 후 키보드를 확실하게 호출합니다.
        nameEditText.isFocusable = true
        nameEditText.isFocusableInTouchMode = true
        nameEditText.visibility = View.VISIBLE // 혹시 숨겨져 있을 경우를 대비

        Handler(Looper.getMainLooper()).postDelayed({
            nameEditText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(nameEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 150) // 150ms 지연
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        saveButton.setOnClickListener {
            val finalName = nameEditText.text.toString().trim()
            if (finalName.isBlank()) {
                Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fileUri != null) {
                val extension = defaultName?.substringAfterLast('.', "") ?: "m4a"
                val finalDisplayName = "$finalName.$extension"
                renameMediaStoreFile(fileUri!!, finalDisplayName)
            } else {
                val resultIntent = Intent().apply {
                    putExtra(RESULT_EXTRA_NAME, finalName)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        cancelButton.setOnClickListener {
            if (fileUri != null) {
                try {
                    contentResolver.delete(fileUri!!, null, null)
                    Toast.makeText(this, "녹음이 취소되어 파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "임시 파일 삭제 실패", e)
                }
            }
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun renameMediaStoreFile(uri: Uri, newName: String) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName)
            }
            val updatedRows = contentResolver.update(uri, values, null, null)

            if (updatedRows > 0) {
                Toast.makeText(this, "'$newName'(으)로 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "이름 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore 파일 이름 변경 중 오류", e)
            Toast.makeText(this, "오류가 발생하여 이름을 변경할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
