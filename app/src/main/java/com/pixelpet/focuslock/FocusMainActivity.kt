package com.pixelpet.focuslock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.ComponentName
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.pixelpet.R
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

class FocusMainActivity : AppCompatActivity() {
    private var selectedTodoId: Long? = null
    private lateinit var btnStartFocus: Button
    private lateinit var tvStats: TextView
    private lateinit var tvAllowedApps: TextView
    private lateinit var layoutPermissions: LinearLayout
    private var isPermissionsCollapsed = true
    private val todoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val todoId = data?.getLongExtra(TodoActivity.EXTRA_TODO_ID, -1L) ?: -1L
            val todoText = data?.getStringExtra(TodoActivity.EXTRA_TODO_TEXT)
            if (todoId > 0 && !todoText.isNullOrBlank()) {
                selectedTodoId = todoId
                findViewById<EditText>(R.id.etTaskName).setText(todoText)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.focuslock_activity_main)

        FocusManager.init(this)
        setupPermissions()
        setupPresets()

        btnStartFocus = findViewById(R.id.btnStartFocus)
        tvStats = findViewById(R.id.tvStats)
        tvAllowedApps = findViewById(R.id.tvAllowedApps)
        layoutPermissions = findViewById(R.id.layoutPermissionsContent)
        findViewById<ImageButton>(R.id.btnTogglePermissions).setOnClickListener {
            togglePermissions()
        }
        applyPermissionsCollapsed()
        btnStartFocus.setOnClickListener {
            if (FocusManager.currentState == FocusManager.State.RUNNING) {
                stopFocusSession()
            } else {
                startFocus()
            }
        }
        findViewById<Button>(R.id.btnAllowedApps).setOnClickListener { showAllowedAppsDialog() }

        updateFocusButton()
        updateStats()
        updateAllowedAppsSummary()
    }

    private fun setupPermissions() {
        findViewById<Button>(R.id.btnUsageStats).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
             }
        }
        
        findViewById<android.widget.ImageButton>(R.id.btnTodoList).setOnClickListener {
            todoLauncher.launch(Intent(this, TodoActivity::class.java))
        }
    }
    
    private fun setupPresets() {
        val etDuration = findViewById<EditText>(R.id.etDuration)
        findViewById<Button>(R.id.btnPreset25).setOnClickListener { etDuration.setText("25") }
        findViewById<Button>(R.id.btnPreset45).setOnClickListener { etDuration.setText("45") }
        findViewById<Button>(R.id.btnPreset50).setOnClickListener { etDuration.setText("50") }
    }

    private fun startFocus() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请开启 Todo 无障碍服务", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        val durationStr = findViewById<EditText>(R.id.etDuration).text.toString()
        val duration = durationStr.toIntOrNull() ?: 0
        if (duration <= 0) {
            Toast.makeText(this, "请输入有效时长", Toast.LENGTH_SHORT).show()
            return
        }
        
        val taskName = findViewById<EditText>(R.id.etTaskName).text.toString()
        
        FocusManager.startFocus(this, duration, taskName, selectedTodoId)
        
        // Start Service
        val serviceIntent = Intent(this, FocusCoreService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        Toast.makeText(this, "专注已开始", Toast.LENGTH_SHORT).show()
        updateFocusButton()
        updateStats()
    }

    private fun stopFocusSession() {
        FocusManager.stopFocus(this, completed = false)
        Toast.makeText(this, "已停止专注", Toast.LENGTH_SHORT).show()
        updateFocusButton()
        updateStats()
    }

    override fun onResume() {
        super.onResume()
        FocusManager.init(this)
        updateFocusButton()
        updateStats()
        updateAllowedAppsSummary()
    }

    private fun updateStats() {
        if (FocusManager.currentState == FocusManager.State.RUNNING) {
            val remain = FocusManager.getRemainingTimeSeconds()
            val min = remain / 60
            val sec = remain % 60
            val taskSuffix = FocusManager.currentTaskName?.let { " - $it" } ?: ""
            tvStats.text = "专注中：%02d:%02d%s".format(min, sec, taskSuffix)
            return
        }
        val stats = FocusManager.getTodayStats(this)
        tvStats.text = "今日：${stats.totalMinutes} 分钟（${stats.pomodoroCount} 次）"
    }

    private fun updateFocusButton() {
        if (!::btnStartFocus.isInitialized) return
        if (FocusManager.currentState == FocusManager.State.RUNNING) {
            btnStartFocus.text = "停止专注"
        } else {
            btnStartFocus.text = getString(R.string.btn_start_focus)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, FocusCoreService::class.java)
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val service = ComponentName.unflattenFromString(splitter.next())
            if (service == expected) {
                return true
            }
        }
        return false
    }

    private fun showAllowedAppsDialog() {
        val pm = packageManager
        val apps = queryLaunchableApps(pm)
        if (apps.isEmpty()) {
            Toast.makeText(this, "未找到可用应用", Toast.LENGTH_SHORT).show()
            return
        }

        val allowed = FocusManager.getAllowedPackages(this)
        val labels = apps.map { it.label }.toTypedArray()
        val selected = BooleanArray(apps.size) { idx ->
            allowed.contains(apps[idx].packageName)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_allowed_apps))
            .setMultiChoiceItems(labels, selected) { _, which, isChecked ->
                selected[which] = isChecked
            }
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val chosen = apps.indices
                    .filter { selected[it] }
                    .map { apps[it].packageName }
                    .toSet()
                FocusManager.saveAllowedPackages(this, chosen)
                updateAllowedAppsSummary()
                Toast.makeText(this, "已更新可用应用白名单", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun updateAllowedAppsSummary() {
        val pkgs = FocusManager.getAllowedPackages(this).toList()
        val labelMap = loadPackageLabels(packageManager, pkgs)
        val labels = pkgs.mapNotNull { labelMap[it] }.sorted().take(6)
        val suffix = if (pkgs.size > 6) " ..." else ""
        tvAllowedApps.text = if (pkgs.isEmpty()) {
            getString(R.string.allowed_apps_hint)
        } else {
            "可用应用: ${labels.joinToString("、")}$suffix"
        }
    }

    private fun togglePermissions() {
        isPermissionsCollapsed = !isPermissionsCollapsed
        applyPermissionsCollapsed()
    }

    private fun applyPermissionsCollapsed() {
        layoutPermissions.visibility = if (isPermissionsCollapsed) View.GONE else View.VISIBLE
        val icon = if (isPermissionsCollapsed) R.drawable.ic_pixel_add else R.drawable.ic_nav_settings
        findViewById<ImageButton>(R.id.btnTogglePermissions).setImageResource(icon)
    }

    private data class AppEntry(
        val packageName: String,
        val label: String
    )

    private fun queryLaunchableApps(pm: PackageManager): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val list: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        return list
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isBlank()) null else AppEntry(pkg, label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun loadPackageLabels(pm: PackageManager, pkgs: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        pkgs.forEach { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                map[pkg] = label
            } catch (_: Exception) {
            }
        }
        return map
    }
}

