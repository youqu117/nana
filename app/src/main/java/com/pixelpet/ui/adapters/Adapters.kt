package com.pixelpet.ui.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pixelpet.content.AssetLoader
import com.pixelpet.data.PetAssetEntity
import com.pixelpet.data.PetInstanceEntity
import com.pixelpet.pet.level.LevelSystem
import com.pixelpet.R
import kotlin.math.max
import kotlin.math.min

class PetGridAdapter(
    private var assets: List<PetAssetEntity>,
    private val onAdopt: (PetAssetEntity) -> Unit
) : RecyclerView.Adapter<PetGridAdapter.PetViewHolder>() {

    class PetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgPetPreview)
        val name: TextView = view.findViewById(R.id.textPetName)
    }

    fun updateData(newAssets: List<PetAssetEntity>) {
        assets = newAssets
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet_grid, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val asset = assets[position]
        holder.name.text = asset.name
        loadPreview(holder.itemView.context, holder.image, asset.previewPath)

        holder.itemView.setOnClickListener {
            onAdopt(asset)
        }
    }

    override fun getItemCount() = assets.size
}

class ActivePetAdapter(
    private var pets: List<PetInstanceEntity>,
    private val onCardClick: (PetInstanceEntity) -> Unit,
    private val onToggle: (PetInstanceEntity) -> Unit,
    private val onDelete: (PetInstanceEntity) -> Unit,
    private val onRename: (PetInstanceEntity) -> Unit,
    private val onFeed: (PetInstanceEntity) -> Unit
) : RecyclerView.Adapter<ActivePetAdapter.ActiveViewHolder>() {
    private var previewPaths: Map<String, String> = emptyMap()

    class ActiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textName)
        val levelBadge: TextView = view.findViewById(R.id.textLevelBadge)
        val preview: ImageView = view.findViewById(R.id.imgActivePetPreview)
        val toggleBtn: Button = view.findViewById(R.id.btnToggle)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDelete)
        val renameBtn: ImageButton = view.findViewById(R.id.btnRename)
        val feedBtn: ImageButton = view.findViewById(R.id.btnFeed)
        
        val progressEnergy: android.widget.ProgressBar = view.findViewById(R.id.progressEnergy)
        val progressMood: android.widget.ProgressBar = view.findViewById(R.id.progressMood)
        val progressHunger: android.widget.ProgressBar = view.findViewById(R.id.progressHunger)
    }
    
    fun updateData(newPets: List<PetInstanceEntity>) {
        pets = newPets
        notifyDataSetChanged()
    }

    fun updateAssets(assets: List<PetAssetEntity>) {
        previewPaths = assets.associate { it.id to it.previewPath }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_pet, parent, false)
        return ActiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActiveViewHolder, position: Int) {
        val pet = pets[position]
        holder.name.text = pet.name
        val levelInfo = LevelSystem.fromPet(pet)
        holder.levelBadge.text = "LV.${levelInfo.level} ${levelInfo.title}"
        loadPreview(holder.itemView.context, holder.preview, previewPaths[pet.assetId])

        holder.itemView.setOnClickListener { onCardClick(pet) }

        holder.progressEnergy.progress = pet.energy.coerceIn(0, 100)
        holder.progressMood.progress = pet.mood.coerceIn(0, 100)
        holder.progressHunger.progress = pet.hunger.coerceIn(0, 100)
        
        if (pet.isEnabled) {
            holder.toggleBtn.text = holder.itemView.context.getString(R.string.action_go_home)
            holder.toggleBtn.setBackgroundResource(R.drawable.bg_btn_primary)
            holder.toggleBtn.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
        } else {
            holder.toggleBtn.text = holder.itemView.context.getString(R.string.action_resume)
            holder.toggleBtn.setBackgroundResource(R.drawable.bg_btn_secondary)
            holder.toggleBtn.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.ui_text_main))
        }

        holder.toggleBtn.setOnClickListener { onToggle(pet) }
        holder.deleteBtn.setOnClickListener { onDelete(pet) }
        holder.renameBtn.setOnClickListener { onRename(pet) }
        holder.feedBtn.setOnClickListener { onFeed(pet) }
        holder.preview.setOnClickListener { onCardClick(pet) }
    }

    override fun getItemCount() = pets.size
}

private fun loadPreview(context: Context, imageView: ImageView, path: String?) {
    if (path.isNullOrBlank()) {
        imageView.setImageResource(android.R.drawable.ic_menu_help)
        return
    }
    try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        AssetLoader.openStream(context, path).use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val sourceW = boundsOptions.outWidth
        val sourceH = boundsOptions.outHeight
        if (sourceW <= 0 || sourceH <= 0) {
            imageView.setImageResource(android.R.drawable.ic_menu_help)
            return
        }

        val reqW = PREVIEW_MAX_DIM_PX
        val reqH = PREVIEW_MAX_DIM_PX
        val sampleSize = calculateInSampleSize(sourceW, sourceH, reqW, reqH)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inScaled = false
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }

        AssetLoader.openStream(context, path).use { stream ->
            val decoded = BitmapFactory.decodeStream(stream, null, decodeOptions)
            if (decoded != null) {
                val bitmap = if (decoded.width <= reqW && decoded.height <= reqH) {
                    decoded
                } else {
                    val ratio = min(reqW.toFloat() / decoded.width, reqH.toFloat() / decoded.height)
                    val scaledW = max(1, (decoded.width * ratio).toInt())
                    val scaledH = max(1, (decoded.height * ratio).toInt())
                    val scaled = android.graphics.Bitmap.createScaledBitmap(decoded, scaledW, scaledH, false)
                    if (scaled !== decoded) decoded.recycle()
                    scaled
                }
                imageView.setImageBitmap(bitmap)
                (imageView.drawable as? android.graphics.drawable.BitmapDrawable)?.isFilterBitmap = false
                return
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    imageView.setImageResource(android.R.drawable.ic_menu_help)
}

private fun calculateInSampleSize(sourceW: Int, sourceH: Int, reqW: Int, reqH: Int): Int {
    var sampleSize = 1
    val halfW = sourceW / 2
    val halfH = sourceH / 2
    while (halfW / sampleSize >= reqW && halfH / sampleSize >= reqH) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}

private const val PREVIEW_MAX_DIM_PX = 256

