package app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import app.data.AppDatabase
import app.data.AssetScanner
import app.data.PetRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pixelpet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    lateinit var repository: PetRepository

    private val importLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val db = AppDatabase.getDatabase(this)
        repository = PetRepository(db.petDao(), db.settingsDao())

        setupViewPager()
        
        lifecycleScope.launch {
            AssetScanner.scanAndPopulate(applicationContext, repository)
        }
    }

    private fun setupViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2

        // Sync ViewPager -> BottomNav
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
            }
        })

        // Sync BottomNav -> ViewPager
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.nav_pets -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.nav_shop -> {
                    viewPager.currentItem = 2
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

    fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val seekScale = dialogView.findViewById<SeekBar>(R.id.seekScale)
        val textScaleValue = dialogView.findViewById<TextView>(R.id.textScaleValue)
        val seekAlpha = dialogView.findViewById<SeekBar>(R.id.seekAlpha)
        val textAlphaValue = dialogView.findViewById<TextView>(R.id.textAlphaValue)
        val switchVertical = dialogView.findViewById<android.widget.Switch>(R.id.switchVerticalMove)
        val btnImport = dialogView.findViewById<android.widget.Button>(R.id.btnImportPet)

        lifecycleScope.launch {
            repository.getSetting("scale").collectLatest { 
                val scale = it?.toFloatOrNull() ?: 1.0f
                val p = ((scale - 0.05f) / 2.45f * 100).toInt().coerceIn(0, 100)
                seekScale.progress = p
                textScaleValue.text = String.format("%.2fx", scale)
            }
        }
        
        lifecycleScope.launch {
            repository.getSetting("alpha").collectLatest {
                val alpha = it?.toFloatOrNull() ?: 1.0f
                seekAlpha.progress = (alpha * 10).toInt().coerceIn(1, 10)
                textAlphaValue.text = "${(alpha * 100).toInt()}%"
            }
        }
        
        lifecycleScope.launch {
            repository.getSetting("vertical_move").collectLatest {
                val enabled = it?.toBoolean() ?: false
                switchVertical.isChecked = enabled
            }
        }

        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val scale = 0.05f + (p1 / 100.0f * 2.45f)
                textScaleValue.text = String.format("%.2fx", scale)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val alpha = p1 / 10.0f
                textAlphaValue.text = "${(alpha * 100).toInt()}%"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        }

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_settings_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val newScale = 0.05f + (seekScale.progress / 100.0f * 2.45f)
                val newAlpha = seekAlpha.progress / 10.0f
                val newVertical = switchVertical.isChecked
                lifecycleScope.launch {
                    repository.setSetting("scale", newScale.toString())
                    repository.setSetting("alpha", newAlpha.toString())
                    repository.setSetting("vertical_move", newVertical.toString())
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setNeutralButton("Reset") { _, _ ->
                 lifecycleScope.launch {
                    repository.setSetting("scale", "1.0")
                    repository.setSetting("alpha", "1.0")
                    repository.setSetting("vertical_move", "false")
                }
            }
            .show()
    }

    private fun handleImport(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@launch
                val tempDir = File(filesDir, "temp_import_${System.currentTimeMillis()}")
                tempDir.mkdirs()
                
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val file = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { fos ->
                                val buffer = ByteArray(4096)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
                
                val manifestFile = tempDir.walk().find { it.name == "manifest.json" }
                if (manifestFile != null) {
                     val json = org.json.JSONObject(manifestFile.readText())
                     val id = json.getString("id")
                     
                     val targetDir = File(filesDir, "pets/$id")
                     if (targetDir.exists()) targetDir.deleteRecursively()
                     targetDir.mkdirs()
                     
                     val sourceDir = manifestFile.parentFile ?: tempDir
                     sourceDir.copyRecursively(targetDir, overwrite = true)
                     
                     tempDir.deleteRecursively()
                     
                     withContext(Dispatchers.Main) {
                         Toast.makeText(this@MainActivity, getString(R.string.msg_import_success), Toast.LENGTH_SHORT).show()
                         AssetScanner.scanAndPopulate(applicationContext, repository)
                     }
                } else {
                     tempDir.deleteRecursively()
                     withContext(Dispatchers.Main) {
                         Toast.makeText(this@MainActivity, "Invalid pet package: no manifest.json", Toast.LENGTH_SHORT).show()
                     }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_import_failed) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class MainPagerAdapter(fa: androidx.fragment.app.FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> MyPetsFragment()
                2 -> ShopFragment()
                else -> HomeFragment()
            }
        }
    }
}
