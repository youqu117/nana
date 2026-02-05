package app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.data.AppDatabase
import app.data.PetRepository

class MainActivity : AppCompatActivity() {
    private lateinit var repository: PetRepository
    private lateinit var container: LinearLayout
    private lateinit var statusBar: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var serviceToggleButton: Button
    private lateinit var petListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        repository = PetRepository(db.petDao(), db.settingsDao())

        // Setup UI
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }
        scrollView.addView(container)
        setContentView(scrollView)

        buildHomeConsoleUI()

        // Observe Data
        lifecycleScope.launch {
            repository.allInstances.collectLatest { instances ->
                refreshPetList(instances)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusBar()
    }

    private fun buildHomeConsoleUI() {
        // Title
        val title = TextView(this).apply {
            text = "Pixel Pet Console"
            textSize = 24f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 }
        }
        container.addView(title)

        // Status Bar
        statusBar = createCard().apply {
            orientation = LinearLayout.VERTICAL
            setOnClickListener {
                // Handle clicks on status bar to request permissions
                if (!hasOverlayPermission()) requestOverlayPermission()
                else if (!isIgnoringBatteryOptimizations()) requestBatteryOptimizations()
            }
        }
        statusText = TextView(this).apply {
            textSize = 14f
            setLineSpacing(0f, 1.5f)
        }
        statusBar.addView(statusText)
        container.addView(statusBar)

        // Quick Toggles
        val quickTogglesCard = createCard().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
        }

        serviceToggleButton = Button(this).apply {
            text = "启动服务"
            setOnClickListener { toggleService() }
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginEnd = 16 }
        }
        quickTogglesCard.addView(serviceToggleButton)

        // Placeholder for other toggles (e.g., Pause All, Lock Pets)
        val pauseButton = Button(this).apply {
            text = "暂停所有"
            isEnabled = false // Not implemented yet
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        quickTogglesCard.addView(pauseButton)
        container.addView(quickTogglesCard)

        // Pet List Section
        val petListLabel = TextView(this).apply {
            text = "我的桌宠"
            textSize = 18f
            setTextColor(Color.DKGRAY)
            setPadding(0, 32, 0, 16)
        }
        container.addView(petListLabel)

        petListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(petListContainer)

        val addPetButton = Button(this).apply {
            text = "+ 领养柴犬 (Shiba)"
            setOnClickListener {
                lifecycleScope.launch {
                    repository.addInstance("dog_shiba", "柴柴 ${System.currentTimeMillis() % 100}")
                }
            }
        }
        container.addView(addPetButton)
    }

    private fun formatAge(adoptionTime: Long): String {
        val diff = System.currentTimeMillis() - adoptionTime
        val days = diff / (1000 * 60 * 60 * 24)
        return when {
            days < 7 -> "${days}天 (幼崽)"
            days < 28 -> "${days / 7}周 (少年)"
            else -> "${days / 30}月 (成年)"
        }
    }

    private fun refreshPetList(instances: List<PetInstanceEntity>) {
        petListContainer.removeAllViews()
        if (instances.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "暂无桌宠，点击下方按钮领养"
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
            }
            petListContainer.addView(emptyView)
            return
        }

        instances.forEach { instance ->
            val item = createCard().apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }

            // Header: Name + Switch
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val nameText = TextView(this).apply {
                text = "${instance.name} (${formatAge(instance.adoptionTime)})"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val toggleBtn = Switch(this).apply {
                isChecked = instance.isEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch { repository.toggleInstance(instance.copy(isEnabled = isChecked)) }
                }
            }
            header.addView(nameText)
            header.addView(toggleBtn)
            item.addView(header)

            // Stats Bars
            item.addView(createStatBar("Energy", instance.energy, Color.parseColor("#FFC107"))) // Amber
            item.addView(createStatBar("Mood", instance.mood + 100, Color.parseColor("#03A9F4"), 200)) // Blue, range 0-200
            item.addView(createStatBar("Affection", instance.affection, Color.parseColor("#E91E63"))) // Pink

            // Delete Button
            val deleteBtn = Button(this).apply {
                text = "删除"
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END
                    topMargin = 16
                }
                setOnClickListener {
                    lifecycleScope.launch { repository.deleteInstance(instance) }
                }
            }
            item.addView(deleteBtn)

            petListContainer.addView(item)
        }
    }

    private fun createStatBar(label: String, value: Int, color: Int, max: Int = 100): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }
        val labelView = TextView(this).apply {
            text = label
            width = 200
            textSize = 12f
        }
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            this.max = max
            this.progress = value
            progressTintList = android.content.res.ColorStateList.valueOf(color)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        layout.addView(labelView)
        layout.addView(progress)
        return layout
    }


    private fun updateStatusBar() {
        val overlayPerm = if (hasOverlayPermission()) "✅ 悬浮窗权限" else "❌ 悬浮窗权限 (点击授予)"
        val batteryPerm = if (isIgnoringBatteryOptimizations()) "✅ 电池优化已忽略" else "⚠️ 电池优化未忽略 (建议开启)"
        val serviceState = if (OverlayService.isServiceRunning) "RUNNING" else "STOPPED"

        statusText.text = "$overlayPerm\n$batteryPerm\n后台服务状态: $serviceState"
        serviceToggleButton.text = if (OverlayService.isServiceRunning) "停止服务" else "启动服务"
    }

    private fun toggleService() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        if (OverlayService.isServiceRunning) {
            OverlayService.stop(this)
        } else {
            OverlayService.start(this)
        }
        // Delay slightly to allow service to start/stop
        container.postDelayed({ updateStatusBar() }, 500)
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, android.R.drawable.dialog_holo_light_frame) // Simple frame
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24 }
            elevation = 4f
        }
    }

    // --- Permissions ---

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开电池优化设置", Toast.LENGTH_SHORT).show()
            }
        }
    }
}