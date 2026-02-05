package app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.data.PetInstanceEntity
import app.data.PetRepository
import com.pixelpet.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyPetsFragment : Fragment(R.layout.fragment_my_pets) {
    private lateinit var repository: PetRepository
    private lateinit var activeAdapter: ActivePetAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        repository = (requireActivity() as MainActivity).repository
        
        setupAdapter(view)
        
        val emptyView = view.findViewById<View>(R.id.textEmptyState)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerActivePets)
        
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allInstances.collectLatest { pets ->
                activeAdapter.updateData(pets)
                emptyView.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
                recycler.visibility = if (pets.isEmpty()) View.GONE else View.VISIBLE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allAssets.collectLatest { assets ->
                activeAdapter.updateAssets(assets)
            }
        }
    }

    private fun setupAdapter(view: View) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerActivePets)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        
        activeAdapter = ActivePetAdapter(
            emptyList(),
            onToggle = { pet -> 
                viewLifecycleOwner.lifecycleScope.launch { repository.toggleInstance(pet) }
            },
            onDelete = { pet ->
                showDeleteConfirmDialog(pet)
            },
            onRename = { pet ->
                showRenameDialog(pet)
            },
            onFeed = { pet ->
                // Feed: +10 Hunger, +5 Mood
                val newHunger = (pet.hunger + 10).coerceAtMost(100)
                val newMood = (pet.mood + 5).coerceAtMost(100)
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.updateInstance(pet.copy(hunger = newHunger, mood = newMood))
                    // Optional: Toast "Fed [Name]!"
                }
            }
        )
        recycler.adapter = activeAdapter
    }

    private fun showRenameDialog(pet: PetInstanceEntity) {
        val context = requireContext()
        val editText = EditText(context).apply {
            setText(pet.name)
            setSelection(pet.name.length)
            hint = getString(R.string.hint_enter_name)
        }
        
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
            addView(editText)
        }

        AlertDialog.Builder(context)
            .setTitle(getString(R.string.action_rename))
            .setView(container)
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        repository.updateInstance(pet.copy(name = newName))
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showDeleteConfirmDialog(pet: PetInstanceEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.action_delete))
            .setMessage(getString(R.string.msg_confirm_delete, pet.name))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteInstance(pet)
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }
}
