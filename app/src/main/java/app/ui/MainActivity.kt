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

class MainActivity : AppCompatActivity() {
    private lateinit var repository: PetRepository
    private lateinit var activeAdapter: ActivePetAdapter
    private lateinit var assetAdapter: PetGridAdapter
    private lateinit var textServiceStatus: TextView
    private lateinit var btnToggleService: Button

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
        updateServiceStatus()
    }

    private fun setupViews() {
        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        
        // Service Control
        textServiceStatus = findViewById(R.id.textServiceStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnToggleService.setOnClickListener { toggleService() }

        updateServiceStatus()
    }
    
    private fun updateServiceStatus() {
        if (OverlayService.isServiceRunning) {
            textServiceStatus.text = "Status: Running"
            textServiceStatus.setTextColor(getColor(R.color.colorPrimary))
            btnToggleService.text = "Stop Service"
            btnToggleService.setBackgroundColor(Color.RED)
        } else {
            textServiceStatus.text = "Status: Stopped"
            textServiceStatus.setTextColor(Color.GRAY)
            btnToggleService.text = "Start Service"
            btnToggleService.setBackgroundColor(getColor(R.color.colorPrimary))
        }
    }

    private fun toggleService() {
        if (OverlayService.isServiceRunning) {
            OverlayService.stop(this)
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return
            }
            OverlayService.start(this)
        }
        // Small delay to allow service to start/stop
        textServiceStatus.postDelayed({ updateServiceStatus() }, 500)
    }
        findViewById<View>(R.id.fabRefresh).setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "Rescanning assets...", Toast.LENGTH_SHORT).show()
                AssetScanner.scanAndPopulate(applicationContext, repository)
            }
        }
        
        // Settings Button
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            padding = 50 // px, rough conversion
            setPadding(40, 40, 40, 40)
        }
        
        // Scale
        val labelScale = TextView(this).apply { text = "Scale: 1.5x" }
        val seekScale = SeekBar(this).apply { max = 15; progress = 5 } // 1.0 + (progress/10.0) -> 1.0 to 2.5
        dialogView.addView(labelScale)
        dialogView.addView(seekScale)
        
        // Alpha
        val labelAlpha = TextView(this).apply { text = "Opacity: 100%" }
        val seekAlpha = SeekBar(this).apply { max = 10; progress = 10 } // progress/10.0
        dialogView.addView(labelAlpha)
        dialogView.addView(seekAlpha)
        
        // Load current values
        lifecycleScope.launch {
            repository.getSetting("scale").collectLatest { 
                val scale = it?.toFloatOrNull() ?: 1.5f
                seekScale.progress = ((scale - 1.0f) * 10).toInt().coerceIn(0, 15)
                labelScale.text = "Scale: ${String.format("%.1f", scale)}x"
            }
        }
        lifecycleScope.launch {
             repository.getSetting("alpha").collectLatest {
                 val alpha = it?.toFloatOrNull() ?: 1.0f
                 seekAlpha.progress = (alpha * 10).toInt().coerceIn(1, 10)
                 labelAlpha.text = "Opacity: ${(alpha * 100).toInt()}%"
             }
        }

        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val scale = 1.0f + (p1 / 10.0f)
                labelScale.text = "Scale: ${String.format("%.1f", scale)}x"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val alpha = p1 / 10.0f
                labelAlpha.text = "Opacity: ${(alpha * 100).toInt()}%"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        android.app.AlertDialog.Builder(this)
            .setTitle("Global Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newScale = 1.0f + (seekScale.progress / 10.0f)
                val newAlpha = seekAlpha.progress / 10.0f
                lifecycleScope.launch {
                    repository.setSetting("scale", newScale.toString())
                    repository.setSetting("alpha", newAlpha.toString())
                }
            }
            .setNegativeButton("Cancel", null)
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
        recyclerAdoption.layoutManager = GridLayoutManager(this, 2)
        assetAdapter = PetGridAdapter(
            emptyList(),
            onAdopt = { asset ->
                lifecycleScope.launch {
                    repository.addInstance(asset.id, asset.name)
                    Toast.makeText(this@MainActivity, "Adopted ${asset.name}!", Toast.LENGTH_SHORT).show()
                }
            }
        )
        recyclerAdoption.adapter = assetAdapter
    }

    private fun updateServiceStatus() {
        val isRunning = OverlayService.isServiceRunning
        textServiceStatus.text = if (isRunning) "Status: Running" else "Status: Stopped"
        textServiceStatus.setTextColor(if (isRunning) Color.parseColor("#4CAF50") else Color.parseColor("#E64A19"))
        btnToggleService.text = if (isRunning) "Stop" else "Start"
        btnToggleService.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isRunning) Color.parseColor("#E64A19") else Color.parseColor("#4CAF50")
        )
    }

    private fun toggleService() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }
        if (OverlayService.isServiceRunning) OverlayService.stop(this) else OverlayService.start(this)
        // Delay update to allow service to start/stop
        window.decorView.postDelayed({ updateServiceStatus() }, 500)
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
}