package com.volumeassist

object ServiceBus {
    const val ACTION_SERVICE_STATE = "com.volumeassist.SERVICE_STATE"  // 서비스 상태 방송
    const val EXTRA_RUNNING = "running"                                 // Boolean
    const val ACTION_STOP_SERVICE = "com.volumeassist.ACTION_STOP"      // 강제 중지
}
