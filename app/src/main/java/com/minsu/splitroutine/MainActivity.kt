package com.minsu.splitroutine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.minsu.splitroutine.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private var apps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        apps = loadLaunchableApps()
        val labels = apps.map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        binding.spinnerAppA.adapter = adapter
        binding.spinnerAppB.adapter = adapter

        restoreSelection()

        binding.spinnerAppA.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                val app = apps.getOrNull(pos) ?: return
                prefs.packageA = app.packageName
                prefs.activityA = app.activityName
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.spinnerAppB.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                val app = apps.getOrNull(pos) ?: return
                prefs.packageB = app.packageName
                prefs.activityB = app.activityName
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.switchAutoRun.isChecked = prefs.autoRunEnabled
        binding.switchAutoRun.setOnCheckedChangeListener { _, checked ->
            prefs.autoRunEnabled = checked
        }

        binding.switchPreferRoot.isChecked = prefs.preferRoot
        binding.switchPreferRoot.setOnCheckedChangeListener { _, checked ->
            prefs.preferRoot = checked
        }

        binding.btnTestRun.setOnClickListener {
            if (!prefs.isConfigured()) {
                Toast.makeText(this, "먼저 앱 A와 앱 B를 선택하세요", Toast.LENGTH_SHORT).show()
            } else {
                SplitScreenLauncher.launch(this)
            }
        }

        binding.btnAccessibilitySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnNotificationSettings.setOnClickListener {
            openFullScreenNotificationSettings()
        }

        updateRootStatus()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    private fun openFullScreenNotificationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= 34) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                }
            } else {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "설정 화면을 열 수 없습니다. 설정 > 앱 > 이 앱 > 알림에서 직접 허용해주세요.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateRootStatus() {
        val hasRoot = SplitScreenLauncher.isRootAvailable()
        binding.tvRootStatus.text = if (hasRoot) {
            "루트 상태: 사용 가능 (루트 방식 우선 사용)"
        } else {
            "루트 상태: 없음 (접근성 서비스 + 알림 방식 사용, 아래에서 권한을 켜주세요)"
        }
    }

    private fun restoreSelection() {
        val idxA = apps.indexOfFirst { it.packageName == prefs.packageA && it.activityName == prefs.activityA }
        if (idxA >= 0) binding.spinnerAppA.setSelection(idxA)
        val idxB = apps.indexOfFirst { it.packageName == prefs.packageB && it.activityName == prefs.activityB }
        if (idxB >= 0) binding.spinnerAppB.setSelection(idxB)
    }

    private fun loadLaunchableApps(): List<AppInfo> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(mainIntent, 0)
        return resolved
            .mapNotNull { ri ->
                val activityInfo = ri.activityInfo ?: return@mapNotNull null
                val label = ri.loadLabel(pm)?.toString()
                if (TextUtils.isEmpty(label)) return@mapNotNull null
                AppInfo(
                    label = label!!,
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name
                )
            }
            .distinctBy { "${it.packageName}/${it.activityName}" }
            .sortedBy { it.label.lowercase() }
    }
}
