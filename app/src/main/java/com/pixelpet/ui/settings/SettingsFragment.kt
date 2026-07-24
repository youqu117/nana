package com.pixelpet.ui.settings

import android.app.AlertDialog
import android.graphics.Bitmap
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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pixelpet.R
import com.pixelpet.data.AppDatabase
import com.pixelpet.data.AssetScanner
import com.pixelpet.data.PetRepository
import com.pixelpet.util.BitmapUtils
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
                Toast.makeText(context, "已恢复默认设置", Toast.LENGTH_SHORT).show()
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
                    val scale = 0.05f + (p1 / 100f * 2.45f)
                    textScaleValue.text = String.format("%.2fx", scale)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                val scale = 0.05f + (seekScale.progress / 100f * 2.45f)
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.setSetting("scale", scale.toString())
                }
            }
        })

        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, fromUser: Boolean) {
                if (fromUser) {
                    val alpha = (p1.coerceAtLeast(1) / 10f)
                    textAlphaValue.text = "${(alpha * 100).toInt()}%"
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                val alpha = (seekAlpha.progress.coerceAtLeast(1) / 10f)
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
                        json.put("default_scale", 1)

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
                        Toast.makeText(context, "无效宠物包：缺少 manifest.json", Toast.LENGTH_SHORT).show()
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
        val editText = EditText(context).apply {
            hint = "宠物名称"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
            addView(editText)
        }

        AlertDialog.Builder(context)
            .setTitle("输入桌宠名称")
            .setView(container)
            .setPositiveButton("导入") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    processImageImport(uri, name)
                } else {
                    Toast.makeText(context, "名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
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

                val imageFileName = "idle.png"
                val imageFile = File(petDir, imageFileName)

                // Use unified BitmapUtils to safely decode and scale sampled image
                val decodedBitmap = BitmapUtils.decodeSampledFromStream(
                    streamFactory = { context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open uri") },
                    maxDimPx = 128
                ) ?: throw IOException("Failed to decode image")

                FileOutputStream(imageFile).use { output ->
                    decodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                val manifestJson = JSONObject()
                manifestJson.put("id", folderName)
                manifestJson.put("name", name)
                manifestJson.put("version", 1)

                val relativePath = "pets/$folderName/$imageFileName"

                manifestJson.put("preview", relativePath)
                manifestJson.put("static_normal", relativePath)
                manifestJson.put("static_tongue", relativePath)
                manifestJson.put("idle_sheet", "")
                manifestJson.put("idle_anim", "")

                val baseScale = if (decodedBitmap.width <= 64 && decodedBitmap.height <= 64) 3 else 1
                manifestJson.put("default_scale", baseScale)

                val hitbox = JSONObject()
                hitbox.put("x", 0)
                hitbox.put("y", 0)
                hitbox.put("w", decodedBitmap.width)
                hitbox.put("h", decodedBitmap.height)
                manifestJson.put("hitbox", hitbox)

                val anchors = JSONObject()
                anchors.put("root_x", decodedBitmap.width / 2)
                anchors.put("root_y", decodedBitmap.height)
                manifestJson.put("anchors", anchors)

                val manifestFile = File(petDir, "manifest.json")
                manifestFile.writeText(manifestJson.toString())

                AssetScanner.scanAndPopulate(context, repository)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.msg_import_success), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.msg_import_failed) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showManageAssetsDialog() {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val petsDir = File(context.filesDir, "pets")
            val customPets = if (petsDir.exists()) {
                petsDir.listFiles()?.filter { it.isDirectory }?.map { dir ->
                    val manifestFile = File(dir, "manifest.json")
                    var name = dir.name
                    if (manifestFile.exists()) {
                        try {
                            val json = JSONObject(manifestFile.readText())
                            name = json.optString("name", dir.name)
                        } catch (_: Exception) {}
                    }
                    Pair(name, dir)
                } ?: emptyList()
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                if (customPets.isEmpty()) {
                    Toast.makeText(context, "没有可管理的自定义素材", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val names = customPets.map { it.first }.toTypedArray()
                AlertDialog.Builder(context)
                    .setTitle("删除自定义素材")
                    .setItems(names) { _, which ->
                        val targetDir = customPets[which].second
                        val petName = customPets[which].first

                        AlertDialog.Builder(context)
                            .setTitle("确认删除")
                            .setMessage("确定删除自定义素材 '$petName' 吗？")
                            .setPositiveButton("删除") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    targetDir.deleteRecursively()
                                    AssetScanner.scanAndPopulate(context, repository)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "已删除 '$petName'", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    .setNegativeButton("关闭", null)
                    .show()
            }
        }
    }
}
