package app.data

import android.content.Context
import app.content.AssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AssetScanner {
    suspend fun scanAndPopulate(context: Context, repository: PetRepository) {
        withContext(Dispatchers.IO) {
            try {
                val assetPets = context.assets.list("pets")?.toList() ?: emptyList()
                val localPetsDir = java.io.File(context.filesDir, "pets")
                val localPets = if (localPetsDir.exists()) localPetsDir.list()?.toList() ?: emptyList() else emptyList()
                
                val petsList = (assetPets + localPets).distinct()
                
                val assets = petsList.mapNotNull { folderName ->
                    // Try to load manifest for each folder in pets/
                    val manifest = AssetLoader.loadManifest(context, folderName)
                    if (manifest != null) {
                        val hasPreview = AssetLoader.assetExists(context, manifest.preview)
                        val hasNormal = AssetLoader.assetExists(context, manifest.staticNormal)
                        val hasIdleSheet = manifest.idleSheet.isBlank() || AssetLoader.assetExists(context, manifest.idleSheet)
                        if (!hasPreview || !hasNormal || !hasIdleSheet) {
                            return@mapNotNull null
                        }
                        PetAssetEntity(
                            id = manifest.id,
                            name = manifest.name,
                            previewPath = manifest.preview,
                            defaultScale = manifest.defaultScale,
                            width = manifest.hitbox.w,
                            height = manifest.hitbox.h
                        )
                    } else {
                        null
                    }
                }
                
                if (assets.isNotEmpty()) {
                    repository.clearAssets() // Clear old assets first
                    repository.insertAllAssets(assets)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
