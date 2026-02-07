package com.pixelpet.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pixelpet.content.AssetLoader
import com.pixelpet.data.PetInstanceEntity
import com.pixelpet.data.PetRepository
import com.pixelpet.overlay.OverlayService
import com.pixelpet.pet.level.LevelSystem
import com.pixelpet.pet.model.PetState
import com.pixelpet.pet.view.PetView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pixelpet.R
import com.pixelpet.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var repository: PetRepository
    private lateinit var showcasePetView: PetView
    private lateinit var switchOverlay: SwitchMaterial
    private lateinit var textServiceStatus: TextView
    private lateinit var textMainSubtitle: TextView
    private lateinit var textLevelProgress: TextView
    private lateinit var textGuideTip: TextView
    private var isUpdatingToggle = false
    private var wasRequestingPermission = false
    private var tipCursor = 0

    private val guideTips = listOf(
        "点一下宠物可以提升亲密度和心情。",
        "长按并拖拽宠物可以调整位置。",
        "在小屋里试试小提琴和钢琴互动。",
        "专注模式可与 Todo 联动，减少刷手机。",
        "等级由亲密度、心情、精力、饱腹和陪伴天数共同决定。"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        repository = (requireActivity() as MainActivity).repository
        
        showcasePetView = view.findViewById(R.id.showcasePetView)
        textServiceStatus = view.findViewById(R.id.textServiceStatus)
        textMainSubtitle = view.findViewById(R.id.textMainSubtitle)
        textLevelProgress = view.findViewById(R.id.textLevelProgress)
        textGuideTip = view.findViewById(R.id.textGuideTip)
        view.findViewById<View>(R.id.cardGuideWindow).setOnClickListener { showGuideDialog() }
        updateGuideTip()
        
        switchOverlay = view.findViewById(R.id.switchOverlay)
        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingToggle) return@setOnCheckedChangeListener
            if (isChecked != OverlayService.isServiceRunning) {
                toggleService()
            }
        }

        // Focus Mode Entry
        view.findViewById<View>(R.id.cardFocusLock).setOnClickListener {
            val intent = Intent(requireContext(), com.pixelpet.focuslock.FocusMainActivity::class.java)
            startActivity(intent)
        }

        updateServiceStatus()

        // Observe active pet for showcase
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allInstances.collectLatest { pets ->
                if (pets.isNotEmpty()) {
                    val activePet = pets.firstOrNull { it.isEnabled } ?: pets.first()
                    updateShowcase(activePet)
                } else {
                    textMainSubtitle.text = "领养一只宠物开始冒险"
                    textLevelProgress.text = "成长进度 0%"
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

        // Hide showcase pet if service is running (Summoned)
        showcasePetView.visibility = if (isRunning) View.INVISIBLE else View.VISIBLE

        isUpdatingToggle = true
        switchOverlay.isChecked = isRunning
        isUpdatingToggle = false
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
        val levelInfo = LevelSystem.fromPet(instance)
        textMainSubtitle.text = "LV.${levelInfo.level} ${levelInfo.title} · ${instance.name}"
        textLevelProgress.text = "成长进度 ${levelInfo.progressPercent}%"
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

    private fun updateGuideTip() {
        textGuideTip.text = guideTips[tipCursor % guideTips.size]
        tipCursor += 1
    }

    private fun showGuideDialog() {
        updateGuideTip()
        val message = listOf(
            "1. 在商店领养宠物后，可在我的宠物里召唤到桌面。",
            "2. 互动会提升亲密度与心情，喂食会提升饱腹，休息会恢复精力。",
            "3. 等级经验来源：亲密度>心情>精力>饱腹>陪伴天数。",
            "4. 小屋左上角可查看当前等级与经验条进度。",
            "5. 指标含义：精力影响行动频率，心情影响互动收益，饱腹影响衰减速度。",
            "6. 专注模式搭配 Todo 可减少沉迷手机，可配置锁屏可用应用白名单。"
        ).joinToString("\n")

        AlertDialog.Builder(requireContext())
            .setTitle("玩家指引")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show()
    }
}

