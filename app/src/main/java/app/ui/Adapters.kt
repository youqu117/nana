package app.ui

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
import app.data.PetAssetEntity
import app.data.PetInstanceEntity
import com.pixelpet.R

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
    private val onToggle: (PetInstanceEntity) -> Unit,
    private val onDelete: (PetInstanceEntity) -> Unit,
    private val onRename: (PetInstanceEntity) -> Unit,
    private val onFeed: (PetInstanceEntity) -> Unit
) : RecyclerView.Adapter<ActivePetAdapter.ActiveViewHolder>() {
    private var previewPaths: Map<String, String> = emptyMap()

    class ActiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textName)
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
        loadPreview(holder.itemView.context, holder.preview, previewPaths[pet.assetId])

        holder.progressEnergy.progress = pet.energy.coerceIn(0, 100)
        holder.progressMood.progress = pet.mood.coerceIn(0, 100)
        holder.progressHunger.progress = pet.hunger.coerceIn(0, 100)
        
        if (pet.isEnabled) {
            holder.toggleBtn.text = holder.itemView.context.getString(R.string.action_pause)
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
    }

    override fun getItemCount() = pets.size
}

private fun loadPreview(context: Context, imageView: ImageView, path: String?) {
    if (path.isNullOrBlank()) {
        imageView.setImageResource(android.R.drawable.ic_menu_help)
        return
    }
    val options = BitmapFactory.Options().apply {
        inScaled = false
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    try {
        context.assets.open(path).use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream, null, options)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                (imageView.drawable as? android.graphics.drawable.BitmapDrawable)?.isFilterBitmap = false
                return
            }
        }
    } catch (e: Exception) {
        try {
            val b64Path = "$path.base64"
            val b64String = context.assets.open(b64Path).bufferedReader().use { it.readText() }
            val bytes = android.util.Base64.decode(b64String, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                (imageView.drawable as? android.graphics.drawable.BitmapDrawable)?.isFilterBitmap = false
                return
            }
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
    imageView.setImageResource(android.R.drawable.ic_menu_help)
}
