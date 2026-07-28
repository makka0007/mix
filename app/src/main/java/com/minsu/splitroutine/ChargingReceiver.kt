package com.minsu.splitroutine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return

        val prefs = Prefs(context)
        if (!prefs.autoRunEnabled || !prefs.isConfigured()) return

        if (SplitScreenLauncher.isRootAvailable()) {
            SplitScreenLauncher.launch(context)
        } else {
            NotificationHelper.showChargingTrigger(context)
        }
    }
}
