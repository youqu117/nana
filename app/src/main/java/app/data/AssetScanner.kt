package app.data

import android.content.Context
import app.content.AssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AssetScanner {
    suspend fun scanAndPopulate(context: Context, repository: PetRepository) {
        withContext(Dispatchers.IO) {
            try {
                val petsList = context.assets.list("pets") ?: return@withContext
                
                val assets = petsList.mapNotNull { folderName ->
                    // Try to load manifest for each folder in pets/
                    val manifest = AssetLoader.loadManifest(context, folderName)
                    if (manifest != null) {
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
                    repository.insertAllAssets(assets)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
