package app.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.data.AppDatabase
import app.data.AssetScanner
import app.data.PetAssetEntity
import app.data.PetInstanceEntity
import app.data.PetRepository
import app.overlay.OverlayService
import com.pixelpet.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import app.content.AssetLoader
import app.pet.PetState

class MainActivity : AppCompatActivity() {
    private lateinit var repository: PetRepository
    private lateinit var activeAdapter: ActivePetAdapter
    private lateinit var assetAdapter: PetGridAdapter
    private lateinit var textServiceStatus: TextView
    private lateinit var btnToggleService: Button
    private lateinit var showcasePetView: app.pet.PetView // Add showcase view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Database Init
        val db = AppDatabase.getDatabase(this)
        repository = PetRepository(db.petDao(), db.settingsDao())

        // UI Setup
        setupViews()
        setupAdapters()
        
        // Data Observation
        lifecycleScope.launch {
            AssetScanner.scanAndPopulate(applicationContext, repository)
            
            launch {
                repository.allInstances.collectLatest { pets ->
                    activeAdapter.updateData(pets)
                    
                    // Update Showcase with the first enabled pet (or just first one)
                    if (pets.isNotEmpty()) {
                        val activePet = pets.firstOrNull { it.isEnabled } ?: pets.first()
                        updateShowcase(activePet)
                    }
                }
            }
            
            launch {
                repository.allAssets.collectLatest { assets ->
                    assetAdapter.updateData(assets)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-start if permission was just granted
        if (hasOverlayPermission() && wasRequestingPermission) {
            wasRequestingPermission = false
            toggleService()
        }
        updateServiceStatus()
    }

    private fun setupViews() {
        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        
        // Showcase View
        showcasePetView = findViewById(R.id.showcasePetView)
        // Note: scaleX/Y removed here as we use setDisplayScale in updateShowcase
        
        // Service Control
        textServiceStatus = findViewById(R.id.textServiceStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnToggleService.setOnClickListener { toggleService() }

        // Initial check
        updateServiceStatus()

        // Bottom Navigation
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    findViewById<androidx.core.widget.NestedScrollView>(R.id.appBar).parent.requestChildFocus(findViewById(R.id.appBar), findViewById(R.id.appBar))
                    true
                }
                R.id.nav_pets -> {
                    findViewById<androidx.core.widget.NestedScrollView>(R.id.recyclerActivePets).parent.requestChildFocus(findViewById(R.id.recyclerActivePets), findViewById(R.id.recyclerActivePets))
                    true
                }
                R.id.nav_shop -> {
                     findViewById<androidx.core.widget.NestedScrollView>(R.id.recyclerAdoption).parent.requestChildFocus(findViewById(R.id.recyclerAdoption), findViewById(R.id.recyclerAdoption))
                     true
                }
                R.id.nav_settings -> {
                    showSettingsDialog()
                    false 
                }
                else -> false
            }
        }
    }
    
    private var wasRequestingPermission = false

    private fun updateShowcase(instance: PetInstanceEntity) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val manifest = AssetLoader.loadManifest(applicationContext, instance.assetId)
            if (manifest != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showcasePetView.loadAssets(manifest)
                    // Optional: Reset state to idle
                    showcasePetView.updateState(PetState())
                    
                    // Apply a slightly larger scale than default for showcase visibility
                    val baseScale = manifest.defaultScale.toFloat()
                    val showcaseScale = baseScale * 1.8f
                    showcasePetView.setDisplayScale(showcaseScale)
                    // Reset scaleX/Y because setDisplayScale handles sizing now
                    showcasePetView.scaleX = 1.0f
                    showcasePetView.scaleY = 1.0f
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        
        // Scale
        val labelScale = TextView(this).apply { text = getString(R.string.label_scale, 1.5f) }
        val seekScale = SeekBar(this).apply { max = 15; progress = 5 } // 1.0 + (progress/10.0) -> 1.0 to 2.5
        dialogView.addView(labelScale)
        dialogView.addView(seekScale)
        
        // Alpha
        val labelAlpha = TextView(this).apply { text = getString(R.string.label_opacity, 100) }
        val seekAlpha = SeekBar(this).apply { max = 10; progress = 10 } // progress/10.0
        dialogView.addView(labelAlpha)
        dialogView.addView(seekAlpha)
        
        // Load current values
        lifecycleScope.launch {
            repository.getSetting("scale").collectLatest { 
                val scale = it?.toFloatOrNull() ?: 1.5f
                seekScale.progress = ((scale - 1.0f) * 10).toInt().coerceIn(0, 15)
                labelScale.text = getString(R.string.label_scale, scale)
            }
        }
        lifecycleScope.launch {
             repository.getSetting("alpha").collectLatest {
                 val alpha = it?.toFloatOrNull() ?: 1.0f
                 seekAlpha.progress = (alpha * 10).toInt().coerceIn(1, 10)
                 labelAlpha.text = getString(R.string.label_opacity, (alpha * 100).toInt())
             }
        }

        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val scale = 1.0f + (p1 / 10.0f)
                labelScale.text = getString(R.string.label_scale, scale)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val alpha = p1 / 10.0f
                labelAlpha.text = getString(R.string.label_opacity, (alpha * 100).toInt())
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_settings_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val newScale = 1.0f + (seekScale.progress / 10.0f)
                val newAlpha = seekAlpha.progress / 10.0f
                lifecycleScope.launch {
                    repository.setSetting("scale", newScale.toString())
                    repository.setSetting("alpha", newAlpha.toString())
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun setupAdapters() {
        // Active Pets
        val recyclerActive = findViewById<RecyclerView>(R.id.recyclerActivePets)
        recyclerActive.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        activeAdapter = ActivePetAdapter(
            emptyList(),
            onToggle = { pet -> 
                lifecycleScope.launch { repository.toggleInstance(pet.copy(isEnabled = !pet.isEnabled)) }
            },
            onDelete = { pet ->
                lifecycleScope.launch { repository.deleteInstance(pet) }
            }
        )
        recyclerActive.adapter = activeAdapter

        // Adoption Grid
        val recyclerAdoption = findViewById<RecyclerView>(R.id.recyclerAdoption)
        recyclerAdoption.layoutManager = GridLayoutManager(this, 3) // Changed to 3 columns per React UI
        assetAdapter = PetGridAdapter(
            emptyList(),
            onAdopt = { asset ->
                lifecycleScope.launch {
                    repository.addInstance(asset.id, asset.name)
                    Toast.makeText(this@MainActivity, getString(R.string.msg_adopted, asset.name), Toast.LENGTH_SHORT).show()
                }
            }
        )
        recyclerAdoption.adapter = assetAdapter

        // Adoption Collapsible Logic Removed (Now always visible grid as per React UI)
    }

    private fun updateServiceStatus() {
        val isRunning = OverlayService.isServiceRunning
        
        // Update Text
        findViewById<TextView>(R.id.textServiceStatus).text = if (isRunning) "状态：运行中" else "状态：已关闭"
        
        // Update Icon & Background
        val iconBg = findViewById<FrameLayout>(R.id.statusIconBg)
        val icon = findViewById<ImageView>(R.id.statusIcon)
        val btnToggle = findViewById<Button>(R.id.btnToggleService)
        
        if (isRunning) {
            iconBg.setBackgroundResource(R.drawable.bg_status_icon_on)
            icon.setImageResource(android.R.drawable.ic_lock_idle_charging) // Zap icon equivalent
            icon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.ui_green))
            btnToggle.setBackgroundResource(R.drawable.bg_toggle_on)
        } else {
            iconBg.setBackgroundResource(R.drawable.bg_status_icon_off)
            icon.setImageResource(android.R.drawable.ic_lock_power_off)
            icon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.ui_text_mute))
            btnToggle.setBackgroundResource(R.drawable.bg_toggle_off)
        }
    }

    private fun toggleService() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, getString(R.string.msg_grant_permission), Toast.LENGTH_LONG).show()
            wasRequestingPermission = true
            requestOverlayPermission()
            return
        }
        
        try {
            if (OverlayService.isServiceRunning) {
                OverlayService.stop(this)
                Toast.makeText(this, getString(R.string.msg_stopping_service), Toast.LENGTH_SHORT).show()
            } else {
                OverlayService.start(this)
                Toast.makeText(this, getString(R.string.msg_starting_service), Toast.LENGTH_SHORT).show()
            }
            // Delay update to allow service to start/stop
            window.decorView.postDelayed({ updateServiceStatus() }, 500)
            window.decorView.postDelayed({ updateServiceStatus() }, 1500)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                lifecycleScope.launch {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_rescanning), Toast.LENGTH_SHORT).show()
                    AssetScanner.scanAndPopulate(applicationContext, repository)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}