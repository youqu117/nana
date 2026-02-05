package app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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
        
        try {
            holder.itemView.context.assets.open(asset.previewPath).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                holder.image.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            holder.image.setImageResource(android.R.drawable.ic_menu_help)
        }

        holder.itemView.setOnClickListener {
            onAdopt(asset)
        }
    }

    override fun getItemCount() = assets.size
}

class ActivePetAdapter(
    private var pets: List<PetInstanceEntity>,
    private val onToggle: (PetInstanceEntity) -> Unit,
    private val onDelete: (PetInstanceEntity) -> Unit
) : RecyclerView.Adapter<ActivePetAdapter.ActiveViewHolder>() {

    class ActiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textName)
        val status: TextView = view.findViewById(R.id.textStatus)
        val toggleBtn: Button = view.findViewById(R.id.btnToggle)
        val deleteBtn: ImageView = view.findViewById(R.id.btnDelete)
    }
    
    fun updateData(newPets: List<PetInstanceEntity>) {
        pets = newPets
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
        holder.status.text = "Energy: ${pet.energy} | Mood: ${pet.mood} | Affection: ${pet.affection}"
        holder.toggleBtn.text = if (pet.isEnabled) "Hide" else "Show"
        
        holder.toggleBtn.setOnClickListener { onToggle(pet) }
        holder.deleteBtn.setOnClickListener { onDelete(pet) }
    }

    override fun getItemCount() = pets.size
}
