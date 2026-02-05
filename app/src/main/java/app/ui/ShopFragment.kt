package app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.data.PetRepository
import com.pixelpet.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShopFragment : Fragment(R.layout.fragment_shop) {
    private lateinit var repository: PetRepository
    private lateinit var assetAdapter: PetGridAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        repository = (requireActivity() as MainActivity).repository
        
        setupAdapter(view)
        
        val textCount = view.findViewById<TextView>(R.id.textAssetsCount)

        viewLifecycleOwner.lifecycleScope.launch {
            repository.allAssets.collectLatest { assets ->
                assetAdapter.updateData(assets)
                textCount.text = getString(R.string.label_all_pets_count).replace("%d", assets.size.toString()) // Assuming string has %d or just append
            }
        }
    }

    private fun setupAdapter(view: View) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerAdoption)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        
        assetAdapter = PetGridAdapter(
            emptyList(),
            onAdopt = { asset ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.addInstance(asset.id, asset.name)
                    Toast.makeText(requireContext(), getString(R.string.msg_adopted, asset.name), Toast.LENGTH_SHORT).show()
                }
            }
        )
        recycler.adapter = assetAdapter
    }
}
