package app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.data.AppDatabase
import app.data.AssetScanner
import app.data.PetInstanceEntity
import app.data.PetRepository
import app.overlay.OverlayService
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pixelpet.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import app.content.AssetLoader
import app.pet.PetState

class MainActivity : AppCompatActivity() {
    private lateinit var repository: PetRepository
    private lateinit var activeAdapter: ActivePetAdapter
    private lateinit var assetAdapter: PetGridAdapter
    private lateinit var btnToggleService: Button
    private lateinit var switchOverlay: SwitchMaterial
    private lateinit var showcasePetView: app.pet.PetView
    private var isUpdatingToggle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val db = AppDatabase.getDatabase(this)
        repository = PetRepository(db.petDao(), db.settingsDao())

        setupViews()
        setupAdapters()

        lifecycleScope.launch {
            AssetScanner.scanAndPopulate(applicationContext, repository)
            
            launch {
                repository.allInstances.collectLatest { pets ->
                    activeAdapter.updateData(pets)
                    
                    if (pets.isNotEmpty()) {
                        val activePet = pets.firstOrNull { it.isEnabled } ?: pets.first()
                        updateShowcase(activePet)
                    }
                }
            }
            
            launch {
                repository.allAssets.collectLatest { assets ->
                    assetAdapter.updateData(assets)
                    activeAdapter.updateAssets(assets)
                    if (assets.isNotEmpty() && activeAdapter.itemCount == 0) {
                        updateShowcaseByAssetId(assets.first().id)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasOverlayPermission() && wasRequestingPermission) {
            wasRequestingPermission = false
            toggleService()
        }
        updateServiceStatus()
    }

    private fun setupViews() {
        setSupportActionBar(findViewById(R.id.toolbar))
        
        showcasePetView = findViewById(R.id.showcasePetView)

        btnToggleService = findViewById(R.id.btnToggleService)
        btnToggleService.setOnClickListener { toggleService() }
        switchOverlay = findViewById(R.id.switchOverlay)
        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingToggle) return@setOnCheckedChangeListener
            if (isChecked != OverlayService.isServiceRunning) {
                toggleService()
            }
        }

        updateServiceStatus()

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)
        val activeSection = findViewById<View>(R.id.recyclerActivePets)
        val adoptionSection = findViewById<View>(R.id.recyclerAdoption)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    scrollView.smoothScrollTo(0, 0)
                    true
                }
                R.id.nav_pets -> {
                    scrollView.smoothScrollTo(0, activeSection.top)
                    true
                }
                R.id.nav_shop -> {
                     scrollView.smoothScrollTo(0, adoptionSection.top)
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
                    showcasePetView.updateState(PetState())
                    
                    val baseScale = manifest.defaultScale.toFloat()
                    val showcaseScale = baseScale * 1.8f
                    showcasePetView.setDisplayScale(showcaseScale)
                    showcasePetView.scaleX = 1.0f
                    showcasePetView.scaleY = 1.0f
                }
            }
        }
    }

    private fun updateShowcaseByAssetId(assetId: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val manifest = AssetLoader.loadManifest(applicationContext, assetId)
            if (manifest != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showcasePetView.loadAssets(manifest)
                    showcasePetView.updateState(PetState())
                    val baseScale = manifest.defaultScale.toFloat()
                    val showcaseScale = baseScale * 1.8f
                    showcasePetView.setDisplayScale(showcaseScale)
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
        
        val labelScale = TextView(this).apply { text = getString(R.string.label_scale, 1.5f) }
        val seekScale = SeekBar(this).apply { max = 15; progress = 5 }
        dialogView.addView(labelScale)
        dialogView.addView(seekScale)
        
        val labelAlpha = TextView(this).apply { text = getString(R.string.label_opacity, 100) }
        val seekAlpha = SeekBar(this).apply { max = 10; progress = 10 }
        dialogView.addView(labelAlpha)
        dialogView.addView(seekAlpha)
        
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

        val recyclerAdoption = findViewById<RecyclerView>(R.id.recyclerAdoption)
        recyclerAdoption.layoutManager = GridLayoutManager(this, 3)
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
    }

    private fun updateServiceStatus() {
        val isRunning = OverlayService.isServiceRunning
        
        findViewById<TextView>(R.id.textServiceStatus).text = if (isRunning) {
            getString(R.string.status_running_cn)
        } else {
            getString(R.string.status_stopped_cn)
        }

        isUpdatingToggle = true
        switchOverlay.isChecked = isRunning
        isUpdatingToggle = false

        val btnToggle = findViewById<Button>(R.id.btnToggleService)
        if (isRunning) {
            btnToggle.setBackgroundResource(R.drawable.bg_btn_secondary)
            btnToggle.text = getString(R.string.action_stop_service)
            btnToggle.setTextColor(getColor(R.color.ui_text_main))
        } else {
            btnToggle.setBackgroundResource(R.drawable.bg_btn_success)
            btnToggle.text = getString(R.string.action_start_service)
            btnToggle.setTextColor(getColor(android.R.color.white))
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
