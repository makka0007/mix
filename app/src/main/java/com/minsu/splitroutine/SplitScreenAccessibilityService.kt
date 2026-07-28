package com.minsu.splitroutine

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 루트가 없는 기기에서 "최근 앱 화면 -> 분할화면 버튼 탭 -> 두번째 앱 실행" 과정을
 * 자동으로 흉내내는 접근성 서비스.
 *
 * 제조사/런처별로 분할화면 버튼의 문구(content-description)나 리소스 id가 달라서
 * 100% 기기에서 동작을 보장하지 않습니다. 실기기에서 버튼을 못 찾으면
 * SPLIT_BUTTON_KEYWORDS에 실제 문구를 추가하고 다시 빌드하세요.
 * (설정 > 개발자 옵션 > 레이아웃 경계 표시, 또는 접근성 검사기로 실제 문구 확인 가능)
 */
class SplitScreenAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SplitScreenA11y"
        var instance: SplitScreenAccessibilityService? = null

        private val SPLIT_BUTTON_KEYWORDS = listOf(
            "split screen", "split-screen", "분할 화면", "화면 분할", "분할화면"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "접근성 서비스 연결됨")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 이벤트 기반 로직은 사용하지 않음 (performSplitLaunch의 지연 시퀀스로 처리)
    }

    override fun onInterrupt() {}

    /**
     * 앱A를 전체화면으로 실행 -> 최근 앱 화면 열기 -> 분할화면 버튼 탐색/클릭
     * -> 앱B 실행(대기 중인 분할 슬롯에 배치됨) 순서로 진행합니다.
     */
    fun performSplitLaunch(pkgA: String, actA: String, pkgB: String, actB: String) {
        SplitScreenLauncher.launchApp(this, pkgA, actA)

        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_RECENTS)

            mainHandler.postDelayed({
                val tapped = findAndClickSplitButton()
                if (tapped) {
                    Log.i(TAG, "분할화면 버튼 클릭 성공, 앱B 실행 예정")
                    mainHandler.postDelayed({
                        SplitScreenLauncher.launchApp(this, pkgB, actB)
                    }, 700)
                } else {
                    Log.w(TAG, "분할화면 버튼을 찾지 못했습니다. 이 기기의 실제 버튼 문구를 " +
                        "SPLIT_BUTTON_KEYWORDS에 추가해야 할 수 있습니다.")
                }
            }, 600)
        }, 1200)
    }

    private fun findAndClickSplitButton(): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByKeywords(root, SPLIT_BUTTON_KEYWORDS)
        return if (node != null) {
            val target = findClickableAncestorOrSelf(node)
            target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        } else {
            false
        }
    }

    private fun findClickableAncestorOrSelf(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return node
    }

    private fun findNodeByKeywords(
        node: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase()
        val text = node.text?.toString()?.lowercase()
        if (keywords.any { kw -> desc?.contains(kw) == true || text?.contains(kw) == true }) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByKeywords(child, keywords)
            if (found != null) return found
        }
        return null
    }
}
