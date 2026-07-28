package com.minsu.splitroutine

import android.content.Context

/** 선택한 앱 정보와 설정값을 저장하는 SharedPreferences 래퍼. */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("split_charge_prefs", Context.MODE_PRIVATE)

    var packageA: String?
        get() = sp.getString("pkgA", null)
        set(v) = sp.edit().putString("pkgA", v).apply()

    var activityA: String?
        get() = sp.getString("actA", null)
        set(v) = sp.edit().putString("actA", v).apply()

    var packageB: String?
        get() = sp.getString("pkgB", null)
        set(v) = sp.edit().putString("pkgB", v).apply()

    var activityB: String?
        get() = sp.getString("actB", null)
        set(v) = sp.edit().putString("actB", v).apply()

    var autoRunEnabled: Boolean
        get() = sp.getBoolean("autoRun", false)
        set(v) = sp.edit().putBoolean("autoRun", v).apply()

    var preferRoot: Boolean
        get() = sp.getBoolean("preferRoot", true)
        set(v) = sp.edit().putBoolean("preferRoot", v).apply()

    fun isConfigured(): Boolean =
        !packageA.isNullOrEmpty() && !activityA.isNullOrEmpty() &&
        !packageB.isNullOrEmpty() && !activityB.isNullOrEmpty()
}
