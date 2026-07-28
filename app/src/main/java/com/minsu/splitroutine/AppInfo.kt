package com.minsu.splitroutine

/** 런처에 노출되는 설치 앱 하나를 표현. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String
) {
    override fun toString(): String = label
}
