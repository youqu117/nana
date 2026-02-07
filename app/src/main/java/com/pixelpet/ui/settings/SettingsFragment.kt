package com.pixelpet.ui.settings

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pixelpet.data.AppDatabase
import com.pixelpet.data.AssetScanner
import com.pixelpet.data.PetRepository
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pixelpet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class SettingsFragment : Fragment() {
    private lateinit var repository: PetRepository
    private lateinit var seekScale: SeekBar
    private lateinit var textScaleValue: TextView
    private lateinit var seekAlpha: SeekBar
    private lateinit var textAlphaValue: TextView
    private lateinit var switchVertical: SwitchMaterial
    private lateinit var switchDrift: SwitchMaterial
    private lateinit var seekDriftSpeed: SeekBar
    private lateinit var textDriftSpeedValue: TextView

    private val importZipLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleZipImport(it) }
    }

    private val importImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showNameInputDialog(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val db = AppDatabase.getDatabase(requireContext())
        repository = PetRepository(db.petDao(), db.settingsDao())

        seekScale = view.findViewById(R.id.seekScale)
        textScaleValue = view.findViewById(R.id.textScaleValue)
        seekAlpha = view.findViewById(R.id.seekAlpha)
        textAlphaValue = view.findViewById(R.id.textAlphaValue)
        switchVertical = view.findViewById(R.id.switchVerticalMove)
        switchDrift = view.findViewById(R.id.switchDrift)
        seekDriftSpeed = view.findViewById(R.id.seekDriftSpeed)
        textDriftSpeedValue = view.findViewById(R.id.textDriftSpeedValue)
        
        val btnImportZip = view.findViewById<Button>(R.id.btnImportPet)
        val btnImportImage = view.findViewById<Button>(R.id.btnImportImage)
        val btnManageAssets = view.findViewById<Button>(R.id.btnManageAssets)
        val btnReset = view.findViewById<Button>(R.id.btnReset)

        setupObservers()
        setupListeners()

        btnImportZip.setOnClickListener {
            importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        }

        btnImportImage.setOnClickListener {
            importImageLauncher.launch("image/*")
        }

        btnManageAssets.setOnClickListener {
            showManageAssetsDialog()
        }

        btnReset.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                repository.setSetting("scale", "1.0")
                repository.setSetting("alpha", "1.0")
                repository.setSetting("vertical_move", "false")
                repository.setSetting("drift_enabled", "true")
                repository.setSetting("drift_speed", "50")
                Toast.makeText(context, "Reset to defaults", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getSetting("scale").collectLatest { 
                val scale = it?.toFloatOrNull() ?: 1.0f
                val p = ((scale - 0.05f) / 2.45f * 100).toInt().coerceIn(0, 100)
                if (seekScale.progress != p) {
                    seekScale.progress = p
                }
                textScaleValue.text = String.format("%.2fx", scale)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getSetting("alpha").collectLatest {
                val alpha = it?.toFloatOrNull() ?: 1.0f
                val p = (alpha * 10).toInt().coerceIn(1, 10)
                if (seekAlpha.progress != p) {
                    seekAlpha.progress = p
                }
                textAlphaValue.text = "${(alpha * 100).toInt()}%"
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getSetting("vertical_move").collectLatest {
                val enabled = it?.toBoolean() ?: false
                if (switchVertical.isChecked != enabled) {
                    switchVertical.isChecked = enabled
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getSetting("drift_enabled").collectLatest {
                val enabled = it?.toBoolean() ?: true
                if (switchDrift.isChecked != enabled) {
                    switchDrift.isChecked = enabled
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getSetting("drift_speed").collectLatest {
                val speed = it?.toIntOrNull() ?: 50
                if (seekDriftSpeed.progress != speed) {
                    seekDriftSpeed.progress = speed
                }
                textDriftSpeedValue.text = speed.toString()
            }
        }
    }

    private fun setupListeners() {
        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, fromUser: Boolean) {
                if (fromUser) {
                    val scale = 0.05f + (p1 / 100.0f * 2.45f)
                    textScaleValue.text = String.format("%.2fx", scale)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                val scale = 0.05f + (seekScale.progress / 100.0f * 2.45f)
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.setSetting("scale", scale.toString())
                }
            }
        })

        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, fromUser: Boolean) {
                if (fromUser) {
                    val alpha = p1 / 10.0f
                    textAlphaValue.text = "${(alpha * 100).toInt()}%"
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                val alpha = seekAlpha.progress / 10.0f
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.setSetting("alpha", alpha.toString())
                }
            }
        })

        switchVertical.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                repository.setSetting("vertical_move", isChecked.toString())
            }
        }
        
        switchDrift.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                repository.setSetting("drift_enabled", isChecked.toString())
            }
        }
        
        seekDriftSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, fromUser: Boolean) {
                if (fromUser) {
                    textDriftSpeedValue.text = p1.toString()
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.setSetting("drift_speed", seekDriftSpeed.progress.toString())
                }
            }
        })
    }

    private fun handleZipImport(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val tempDir = File(context.filesDir, "temp_import_${System.currentTimeMillis()}")
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
                
                var manifestFile = tempDir.walk().find { it.name == "manifest.json" }
                
                // If no manifest found, try to generate one if images exist
                if (manifestFile == null) {
                    val imageFile = tempDir.walk().find { 
                        it.extension.lowercase() in listOf("png", "jpg", "jpeg") && !it.name.startsWith(".") 
                    }
                    
                    if (imageFile != null) {
                        val generatedManifest = File(tempDir, "manifest.json")
                        val petId = "imported_${System.currentTimeMillis()}"
                        val relPath = imageFile.relativeTo(tempDir).path.replace("\\", "/")
                        
                        val json = JSONObject()
                        json.put("id", petId)
                        json.put("name", imageFile.nameWithoutExtension)
                        json.put("version", 1)
                        json.put("preview", relPath)
                        json.put("static_normal", relPath)
                        json.put("static_tongue", relPath)
                        json.put("idle_sheet", "")
                        json.put("idle_anim", "")
                        json.put("default_scale", 1) // Default to 1 for safety
                        
                        // Hitbox (simple default)
                        val hitbox = JSONObject()
                        hitbox.put("x", 0)
                        hitbox.put("y", 0)
                        hitbox.put("w", 100)
                        hitbox.put("h", 100)
                        json.put("hitbox", hitbox)
                        
                        val anchors = JSONObject()
                        anchors.put("root_x", 50)
                        anchors.put("root_y", 100)
                        json.put("anchors", anchors)
                        
                        generatedManifest.writeText(json.toString())
                        manifestFile = generatedManifest
                    }
                }

                if (manifestFile != null) {
                     val resolvedManifest = manifestFile
                     val json = JSONObject(resolvedManifest.readText())
                     val id = json.getString("id")
                     
                     val targetDir = File(context.filesDir, "pets/$id")
                     if (targetDir.exists()) targetDir.deleteRecursively()
                     targetDir.mkdirs()
                     
                     val sourceDir = resolvedManifest.parentFile ?: tempDir
                     sourceDir.copyRecursively(targetDir, overwrite = true)
                     
                     // Update manifest paths to be absolute relative to assets root
                     val keysToUpdate = listOf("preview", "static_normal", "static_tongue", "idle_sheet", "idle_anim")
                     val updatedJson = JSONObject(File(targetDir, "manifest.json").readText())
                     var modified = false
                     
                     for (key in keysToUpdate) {
                         val path = updatedJson.optString(key, "")
                         if (path.isNotEmpty() && !path.startsWith("pets/$id/")) {
                             updatedJson.put(key, "pets/$id/$path")
                             modified = true
                         }
                     }
                     
                     if (modified) {
                         File(targetDir, "manifest.json").writeText(updatedJson.toString())
                     }
                     
                     tempDir.deleteRecursively()
                     
                     AssetScanner.scanAndPopulate(context, repository)
                     
                     withContext(Dispatchers.Main) {
                         Toast.makeText(context, getString(R.string.msg_import_success), Toast.LENGTH_SHORT).show()
                     }
                } else {
                     tempDir.deleteRecursively()
                     withContext(Dispatchers.Main) {
                         Toast.makeText(context, "Invalid pet package: no manifest.json", Toast.LENGTH_SHORT).show()
                     }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.msg_import_failed) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showNameInputDialog(uri: Uri) {
        val context = requireContext()
        val editText = EditText(context)
        editText.hint = "Pet Name"
        editText.inputType = InputType.TYPE_CLASS_TEXT

        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 40)
        container.addView(editText)

        AlertDialog.Builder(context)
            .setTitle("Enter Pet Name")
            .setView(container)
            .setPositiveButton("Import") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    processImageImport(uri, name)
                } else {
                    Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processImageImport(uri: Uri, name: String) {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val folderName = "custom_$timestamp"
                val petsDir = File(context.filesDir, "pets")
                if (!petsDir.exists()) petsDir.mkdirs()
                
                val petDir = File(petsDir, folderName)
                petDir.mkdirs()
                
                // Resize and copy image
                val contentResolver = context.contentResolver
                
                // Define output file (always png for resized images)
                val imageFileName = "idle.png"
                val imageFile = File(petDir, imageFileName)
                
                // First pass: Decode bounds
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }

                // Calculate sample size
                val targetSize = 100
                var sampleSize = 1
                if (options.outHeight > targetSize || options.outWidth > targetSize) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while ((halfHeight / sampleSize) >= targetSize && (halfWidth / sampleSize) >= targetSize) {
                        sampleSize *= 2
                    }
                }

                // Second pass: Decode with sample size
                val decodeOptions = BitmapFactory.Options()
                decodeOptions.inSampleSize = sampleSize
                
                val scaledBitmap = contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                } ?: throw IOException("Failed to decode image")
                
                // Additional precise scaling if needed
                val maxDim = 128
                val finalBitmap = if (scaledBitmap.width > maxDim || scaledBitmap.height > maxDim) {
                    val ratio = Math.min(maxDim.toFloat() / scaledBitmap.width, maxDim.toFloat() / scaledBitmap.height)
                    val newW = (scaledBitmap.width * ratio).toInt()
                    val newH = (scaledBitmap.height * ratio).toInt()
                    Bitmap.createScaledBitmap(scaledBitmap, newW, newH, true)
                } else {
                    scaledBitmap
                }

                // Save as PNG
                FileOutputStream(imageFile).use { output ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                
                // Create Manifest
                val manifestJson = JSONObject()
                manifestJson.put("id", folderName)
                manifestJson.put("name", name)
                manifestJson.put("version", 1)
                
                // For paths in manifest.json, AssetLoader expects them relative to "pets/$id/" OR absolute path.
                // But wait, AssetLoader.openStream uses `File(context.filesDir, path)`.
                // So if path is "pets/$folderName/$imageFileName", it looks for "files/pets/$folderName/$imageFileName".
                // This is correct.
                val relativePath = "pets/$folderName/$imageFileName"
                
                manifestJson.put("preview", relativePath)
                manifestJson.put("static_normal", relativePath)
                manifestJson.put("static_tongue", relativePath)
                manifestJson.put("idle_sheet", "")
                manifestJson.put("idle_anim", "")
                
                // Smart Scale: If image is small (pixel art), scale up. If large, keep as is.
                val baseScale = if (finalBitmap.width <= 64 && finalBitmap.height <= 64) 3 else 1
                manifestJson.put("default_scale", baseScale)
                
                val hitbox = JSONObject()
                hitbox.put("x", 0)
                hitbox.put("y", 0)
                
                val savedImageOptions = BitmapFactory.Options()
                savedImageOptions.inJustDecodeBounds = true
                BitmapFactory.decodeFile(imageFile.absolutePath, savedImageOptions)
                val width = if (savedImageOptions.outWidth > 0) savedImageOptions.outWidth else 100
                val height = if (savedImageOptions.outHeight > 0) savedImageOptions.outHeight else 100
                
                hitbox.put("w", width)
                hitbox.put("h", height)
                manifestJson.put("hitbox", hitbox)
                
                val anchors = JSONObject()
                anchors.put("root_x", width / 2)
                anchors.put("root_y", height)
                anchors.put("head_x", width / 2)
                anchors.put("head_y", height / 3)
                anchors.put("face_x", width / 2)
                anchors.put("face_y", height / 3)
                manifestJson.put("anchors", anchors)
                
                File(petDir, "manifest.json").writeText(manifestJson.toString())
                
                // Ensure the file is readable
                imageFile.setReadable(true, false)
                File(petDir, "manifest.json").setReadable(true, false)
                
                // Refresh
                AssetScanner.scanAndPopulate(context, repository)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.msg_import_success) + ": $name", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showManageAssetsDialog() {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val petsDir = File(context.filesDir, "pets")
            val customPets = petsDir.listFiles()?.filter { 
                it.isDirectory && File(it, "manifest.json").exists() 
            } ?: emptyList()
            
            val petNames = customPets.map { dir ->
                try {
                    val json = JSONObject(File(dir, "manifest.json").readText())
                    json.optString("name", dir.name)
                } catch (e: Exception) {
                    dir.name
                }
            }
            
            withContext(Dispatchers.Main) {
                if (customPets.isEmpty()) {
                    Toast.makeText(context, "No custom assets found", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                
                AlertDialog.Builder(context)
                    .setTitle("Manage Custom Assets")
                    .setItems(petNames.toTypedArray()) { _, which ->
                        showDeleteConfirmation(customPets[which], petNames[which])
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showDeleteConfirmation(file: File, name: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Asset")
            .setMessage("Are you sure you want to delete '$name'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAsset(file)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteAsset(file: File) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            file.deleteRecursively()
            AssetScanner.scanAndPopulate(requireContext(), repository)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
