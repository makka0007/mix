package com.minsu.splitroutine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 실제로 두 앱을 분할화면으로 띄우는 로직.
 *
 * 1) 루트가 있으면: `su`로 `am start-activity --windowingMode <n>` 셸 명령을 실행.
 *    windowingMode 3/4는 AOSP의 WindowConfiguration.WINDOWING_MODE_SPLIT_SCREEN_PRIMARY/SECONDARY 값으로,
 *    adb shell 또는 su 권한(shell/root uid)에서만 허용되는 파라미터입니다.
 * 2) 루트가 없으면: 접근성 서비스로 "분할화면" 버튼을 자동으로 눌러 흉내냅니다.
 */
object SplitScreenLauncher {

    private const val TAG = "SplitScreenLauncher"

    fun launch(context: Context) {
        val prefs = Prefs(context)
        if (!prefs.isConfigured()) {
            Log.w(TAG, "앱 A/B가 아직 설정되지 않았습니다.")
            return
        }
        val pkgA = prefs.packageA!!
        val actA = prefs.activityA!!
        val pkgB = prefs.packageB!!
        val actB = prefs.activityB!!

        if (prefs.preferRoot && isRootAvailable()) {
            Log.i(TAG, "루트 방식으로 실행합니다.")
            val ok = launchViaRoot(pkgA, actA, pkgB, actB)
            if (ok) return
            Log.w(TAG, "루트 방식 실패, 접근성 방식으로 대체합니다.")
        }

        launchViaAccessibility(context, pkgA, actA, pkgB, actB)
    }

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exit = process.waitFor()
            exit == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun launchViaRoot(pkgA: String, actA: String, pkgB: String, actB: String): Boolean {
        return try {
            val script = """
                am start-activity --windowingMode 3 -n $pkgA/$actA
                sleep 0.6
                am start-activity --windowingMode 4 -n $pkgB/$actB
            """.trimIndent()
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            val exit = process.waitFor()
            exit == 0
        } catch (e: Exception) {
            Log.e(TAG, "루트 실행 실패: ${e.message}")
            false
        }
    }

    private fun launchViaAccessibility(
        context: Context, pkgA: String, actA: String, pkgB: String, actB: String
    ) {
        val service = SplitScreenAccessibilityService.instance
        if (service == null) {
            Log.w(TAG, "접근성 서비스가 꺼져 있어 앱 A만 전체화면으로 실행합니다.")
            launchApp(context, pkgA, actA)
            return
        }
        service.performSplitLaunch(pkgA, actA, pkgB, actB)
    }

    fun launchApp(context: Context, pkg: String, act: String) {
        try {
            val intent = Intent().apply {
                component = ComponentName(pkg, act)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "앱 실행 실패 ($pkg/$act): ${e.message}")
        }
    }
}
