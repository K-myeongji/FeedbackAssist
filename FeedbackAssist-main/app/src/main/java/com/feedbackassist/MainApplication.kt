package com.feedbackassist

import android.app.Application
import android.os.Build

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 앱 시작 시 필요한 초기화 작업 (필요 시 추가)
        }
    }
}
