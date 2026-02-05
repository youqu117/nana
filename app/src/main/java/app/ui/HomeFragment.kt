package app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import app.content.AssetLoader
import app.data.PetInstanceEntity
import app.data.PetRepository
import app.overlay.OverlayService
import app.pet.PetState
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pixelpet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var repository: PetRepository
    private lateinit var showcasePetView: app.pet.PetView
    private lateinit var switchOverlay: SwitchMaterial
    private lateinit var btnToggleService: Button
    private lateinit var textServiceStatus: TextView
    private var isUpdatingToggle = false
    private var wasRequestingPermission = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        repository = (requireActivity() as MainActivity).repository
        
        showcasePetView = view.findViewById(R.id.showcasePetView)
        textServiceStatus = view.findViewById(R.id.textServiceStatus)
        
        btnToggleService = view.findViewById(R.id.btnToggleService)
        btnToggleService.setOnClickListener { toggleService() }
        
        switchOverlay = view.findViewById(R.id.switchOverlay)
        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingToggle) return@setOnCheckedChangeListener
            if (isChecked != OverlayService.isServiceRunning) {
                toggleService()
            }
        }

        view.findViewById<Button>(R.id.btnConfigure).setOnClickListener {
            (requireActivity() as MainActivity).showSettingsDialog()
        }

        updateServiceStatus()

        // Observe active pet for showcase
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allInstances.collectLatest { pets ->
                if (pets.isNotEmpty()) {
                    val activePet = pets.firstOrNull { it.isEnabled } ?: pets.first()
                    updateShowcase(activePet)
                } else {
                    // Try to load first asset if no pets
                     repository.allAssets.firstOrNull()?.firstOrNull()?.let { asset ->
                         updateShowcaseByAssetId(asset.id)
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

    private fun updateServiceStatus() {
        if (!isAdded) return
        val isRunning = OverlayService.isServiceRunning
        
        textServiceStatus.text = if (isRunning) {
            getString(R.string.status_running_cn)
        } else {
            getString(R.string.status_stopped_cn)
        }

        isUpdatingToggle = true
        switchOverlay.isChecked = isRunning
        isUpdatingToggle = false

        if (isRunning) {
            btnToggleService.setBackgroundResource(R.drawable.bg_btn_secondary)
            btnToggleService.text = getString(R.string.action_stop_service)
            btnToggleService.setTextColor(requireContext().getColor(R.color.ui_text_main))
        } else {
            btnToggleService.setBackgroundResource(R.drawable.bg_btn_success)
            btnToggleService.text = getString(R.string.action_start_service)
            btnToggleService.setTextColor(requireContext().getColor(android.R.color.white))
        }
    }

    private fun toggleService() {
        val context = requireContext()
        if (!hasOverlayPermission()) {
            Toast.makeText(context, getString(R.string.msg_grant_permission), Toast.LENGTH_LONG).show()
            wasRequestingPermission = true
            requestOverlayPermission()
            return
        }
        
        try {
            if (OverlayService.isServiceRunning) {
                OverlayService.stop(context)
                Toast.makeText(context, getString(R.string.msg_stopping_service), Toast.LENGTH_SHORT).show()
            } else {
                OverlayService.start(context)
                Toast.makeText(context, getString(R.string.msg_starting_service), Toast.LENGTH_SHORT).show()
            }
            view?.postDelayed({ updateServiceStatus() }, 500)
            view?.postDelayed({ updateServiceStatus() }, 1500)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(requireContext())
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
            startActivity(intent)
        }
    }

    private fun updateShowcase(instance: PetInstanceEntity) {
        updateShowcaseByAssetId(instance.assetId)
    }

    private fun updateShowcaseByAssetId(assetId: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val context = context ?: return@launch
            val manifest = AssetLoader.loadManifest(context, assetId)
            if (manifest != null) {
                withContext(Dispatchers.Main) {
                    showcasePetView.loadAssets(manifest)
                    showcasePetView.updateState(PetState())
                    
                    // Fixed scale for showcase as requested (0.8x)
                    showcasePetView.setDisplayScale(0.8f)
                    showcasePetView.scaleX = 1.0f
                    showcasePetView.scaleY = 1.0f
                }
            }
        }
    }
}
