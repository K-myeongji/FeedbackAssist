package com.feedbackassist.store

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object Store {

    /**
     * Downloads/FeedbackAssist 에 [displayName] 파일을 만든다.
     * API 29+ : MediaStore.Downloads (권한 불필요)
     * API 28- : 공용 Downloads/FeedbackAssist 폴더에 직접 기록 (WRITE_EXTERNAL_STORAGE 필요)
     *
     * @return (uri, outputStream) — 하위버전에서는 uri 가 null 일 수 있다.
     */
    fun createInDownloads(
        context: Context,
        displayName: String,
        mime: String
    ): Pair<Uri?, OutputStream?> {

        val dirName = "FeedbackAssist"

        return if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/$dirName"
                )
                // 작성 중인 항목 표시
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values)

            // 'w' 모드로 꼭 지정 (일부 기기에서 기본 모드가 읽기 전용으로 열릴 수 있음)
            val os = uri?.let { context.contentResolver.openOutputStream(it, "w") }
            Pair(uri, os)
        } else {
            // API 28 이하: 퍼블릭 Downloads/FeedbackAssist
            @Suppress("DEPRECATION")
            val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(base, dirName).apply { mkdirs() }
            val file = File(dir, displayName)
            val os = runCatching { FileOutputStream(file) }.getOrNull()
            Pair(null, os) // uri 없음
        }
    }

    /** API 29+ 에서 IS_PENDING 해제 (하위 버전은 무시) */
    fun finalizePending(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= 29) {
            val v = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            runCatching { context.contentResolver.update(uri, v, null, null) }
        }
    }
}