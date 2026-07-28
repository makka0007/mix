package com.minsu.splitroutine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 충전 시작(ACTION_POWER_CONNECTED)을 감지하는 리시버.
 * 이 브로드캐스트는 안드로이드 8+ 암시적 브로드캐스트 제한의 예외 항목이라
 * 앱이 꺼져 있어도 매니페스트 등록만으로 정상 수신됩니다.
 */
class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return

        val prefs = Prefs(context)
        if (!prefs.autoRunEnabled || !prefs.isConfigured()) return

        SplitScreenLauncher.launch(context)
    }
}
